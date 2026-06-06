package com.zzzlew.server.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.zzzlew.domain.dto.FileChunkInfoDTO;
import com.zzzlew.domain.dto.FileMessageDTO;
import com.zzzlew.domain.dto.MessageDTO;
import com.zzzlew.domain.entity.Message;
import com.zzzlew.domain.vo.MessageVO;
import com.zzzlew.mapper.ConversationMapper;
import com.zzzlew.mapper.MessageMapper;
import com.zzzlew.properties.MinIOConfigProperties;
import com.zzzlew.server.MessageService;
import com.zzzlew.utils.MinIOFileStorgeUtil;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zzzlew.constant.RedisConstant.*;


/**
 * @Auther: zzzlew
 * @Date: 2025/11/15 - 11 - 15 - 23:52
 * @Description: com.zzzlew.zzzimserver.server.impl
 * @version: 1.0
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Resource
    private MessageMapper messageMapper;
    @Resource
    private ConversationMapper conversationMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MinIOFileStorgeUtil minIOFileStorgeUtil;
    @Resource
    private MinIOConfigProperties minIOConfigProperties;

    /**
     * 热数据预加载消息列表，当前限额100条
     *
     * @param conversationIds 会话id列表
     * @return 消息列表
     */
    @Override
    public List<MessageVO> initMessageList(String conversationIds, Boolean isInit) {
        // 获取当前登录用户的id
        Long userId = UserHolder.getUser().getId();
        // 从redis中获取当前用户的离线时间
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(USER_OFFLINE_INFO_KEY);
        String quitTime;
        // 如果redis中不存在该值的信息，或者已经标记了是初始化，那么就直接返回空列表
        if (entries.get(userId.toString()) == null || isInit) {
            // redis中不存在该值的信息，或者已经标记了是初始化
            quitTime = null;
        } else {
            quitTime = entries.get(userId.toString()).toString();
        }
        log.info("需要初始化加载的会话id为：{}，离线时间为：{}", conversationIds, quitTime);
        List<String> conversationIdList = List.of(conversationIds.split(","));
        // 查询会话内的消息列表
        List<Message> messageList = messageMapper.initMessageList(conversationIdList, quitTime);

        List<MessageVO> messageVOList = new ArrayList<>();

        // 转换为消息VO列表
        for (Message message : messageList) {
            log.info("初始化加载的消息为：{}", message);
            MessageVO messageVO = BeanUtil.copyProperties(message, MessageVO.class);
            messageVOList.add(messageVO);
        }

        return messageVOList;
    }

    @Override
    public List<MessageVO> pullMessageList(String conversationId, Long maxMessageId) {
        // 查询会话内的消息列表
        List<Message> messageList = messageMapper.pullMessageList(conversationId, maxMessageId);

        // 转换为消息VO列表
        List<MessageVO> messageVOList = messageList.stream().map(Message -> BeanUtil.copyProperties(Message, MessageVO.class)).collect(Collectors.toList());

        log.info("从数据库中查询到的消息列表为：{}", messageVOList);

        return messageVOList;
    }

    @Transactional
    @Override
    public MessageVO sendMessage(MessageDTO messageDTO) {
        messageDTO.setSendTime(null);

        Integer msgType = messageDTO.getMsgType();
        boolean isSystem = java.util.Objects.equals(msgType, 99);
        boolean isText = java.util.Objects.equals(msgType, 1);
        boolean isFile = !isText && !isSystem;
        messageDTO.setSendStatus(1);

        if (isFile) {
            String bucketName = minIOFileStorgeUtil.getBucketName(msgType);
            String remotePath = messageDTO.getRemotePath();
            messageDTO.setBucket(bucketName);
            messageDTO.setSendStatus(0);
            messageDTO.setRemoteUrl(minIOConfigProperties.getEndpoint() + "/" + bucketName + "/" + remotePath);
        }

        LocalDateTime sendTime = LocalDateTime.now();
        // 入库
        messageMapper.saveMessage(messageDTO);

        // 系统消息：不更新会话最新消息/时间
        if (!isSystem) {
            String conversationId = messageDTO.getConversationId();
            if (conversationId.startsWith("g_")) {
                List<String> receiverIds = conversationMapper.selectGroupNumber(conversationId);
                conversationMapper.updateGroupConversationStatus(conversationId, messageDTO.getContent(), sendTime, receiverIds);
            } else {
                String receiverId = messageDTO.getReceiverId();
                conversationMapper.updateConversationStatus(conversationId, messageDTO.getContent(), sendTime, receiverId);
            }
        }
        MessageVO vo = isFile ? BeanUtil.copyProperties(messageDTO, MessageVO.class)
                : new MessageVO();
        vo.setId(messageDTO.getId());
        vo.setConversationId(messageDTO.getConversationId());
        vo.setSenderId(messageDTO.getSenderId());
        vo.setReceiverId(messageDTO.getReceiverId());
        vo.setMsgType(msgType);
        vo.setSubType(messageDTO.getSubType());
        vo.setSendStatus(messageDTO.getSendStatus());
        vo.setSendTime(sendTime);
        if (isText || isSystem) {
            vo.setContent(messageDTO.getContent());
        }
        return vo;
    }

    /**
     * 生成上传凭证
     *
     * @param fileId 文件id
     * @return 凭证id
     */
    @Override
    public Map<String, Object> verifyFileUploadToken(String fileId) {
        // 生成雪花id，用来当作凭证
        String verify = String.valueOf(IdUtil.getSnowflakeNextId());
        // 创建凭证的redis目录
        String verifyKey = FILE_UPLOAD_VERIFY_KEY + fileId;
        stringRedisTemplate.opsForValue().set(verifyKey, verify);
        stringRedisTemplate.expire(verifyKey, FILE_UPLOAD_VERIFY_KEY_TTL, TimeUnit.MINUTES);
        // 创建该文件id的redis目录结构
        String fileIdKey = FILE_CHUNK_INDEX_KEY + fileId;
        // 分块索引不可能存在负数，因此使用-1创建索引
        stringRedisTemplate.opsForZSet().add(fileIdKey, "-1", -1);
        // 设置过期时间
        stringRedisTemplate.expire(fileIdKey, FILE_CHUNK_INDEX_KEY_TTL, TimeUnit.MINUTES);
        // 创建存入minio的文件路径
        String minioFilePath = minIOFileStorgeUtil.buildFilePath(fileId);
        return Map.of("verify", verify, "minioFilePath", minioFilePath);
    }

    @Override
    public void updateSendStatus(String fileId, Integer sendStatus) {
        messageMapper.updateSendStatus(fileId, sendStatus);
    }

    @Override
    public void recallMessage(String conversationId, Long messageId) {
        // 直接删除这条消息
        messageMapper.deleteMessage(conversationId, messageId);
    }

    @Override
    public void uploadFileChunk(MultipartFile chunkBlob, FileChunkInfoDTO fileChunkInfoDTO) {
        Integer chunkIndex = fileChunkInfoDTO.getChunkIndex();
        String chunkHash = fileChunkInfoDTO.getChunkHash();
        // 文件的唯一标识
        String fileHash = fileChunkInfoDTO.getFileId();
        Integer fileType = fileChunkInfoDTO.getFileType();
        String verify = fileChunkInfoDTO.getVerify();

        // 根据文件hash，查询该文件的上传凭证
        String verifyKey = FILE_UPLOAD_VERIFY_KEY + fileHash;
        String realVerify = stringRedisTemplate.opsForValue().get(verifyKey);
        if (realVerify.isBlank() || !verify.equals(realVerify)) {
            // 没有该凭证
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "verify invalid");
        }

        byte[] bytes = null;
        try {
            bytes = chunkBlob.getBytes();
        } catch (IOException e) {
            log.error("上传文件分块消息失败: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "read chunk failed");
        }
        // 计算接收分片的md5值
        String md5Digest = DigestUtils.md5DigestAsHex(bytes);
        if (!chunkHash.equals(md5Digest)) {
            log.error("md5计算值{}与前端传递的{}不相同", md5Digest, chunkHash);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "chunk hash mismatch");
        } else {
            log.info("第{}个分片校验成功", chunkIndex);
        }

        // 构建minio文件分块路径
        String minioFileChunkPath = "file-chunk/" + fileHash + "/" + chunkIndex;

        // 将文件分块文件存入minio中
        minIOFileStorgeUtil.uploadFileChunk(minioFileChunkPath, chunkBlob, fileType);

        String key = FILE_CHUNK_INDEX_KEY + fileHash;
        // 分片文件写入成功之后，将分片索引信息写入redis
        stringRedisTemplate.opsForZSet().add(key, chunkIndex.toString(), chunkIndex);
        // 设置过期时间
        stringRedisTemplate.expire(key, FILE_CHUNK_INDEX_KEY_TTL, TimeUnit.MINUTES);
    }

    @Override
    public List<Integer> checkUploaded(String verify, String fileId) {
        // 根据文件hash，查询该文件的上传凭证
        String verifyKey = FILE_UPLOAD_VERIFY_KEY + fileId;
        String realVerify = stringRedisTemplate.opsForValue().get(verifyKey);
        if (realVerify.isBlank() || !verify.equals(realVerify)) {
            // 没有该凭证
            throw new RuntimeException("凭证不对");
        }
        String key = FILE_CHUNK_INDEX_KEY + fileId;
        Set<String> chunkIndices = stringRedisTemplate.opsForZSet().rangeByScore(key, 0, Double.POSITIVE_INFINITY);
        if (chunkIndices == null || chunkIndices.isEmpty()) {
            // TODO 后期添加从minio查询已上传的分片索引
            return new ArrayList<>();
        }
        // 将 Set<String> 转换为 List<Integer>
        List<Integer> uploadedIndices = chunkIndices.stream().map(Integer::parseInt).collect(Collectors.toList());
        return uploadedIndices;
    }

    @Override
    public void mergeFile(FileMessageDTO fileMessageDTO) {
        String fileHash = fileMessageDTO.getFileHash();
        String fileName = fileMessageDTO.getFileName();
        Integer fileType = fileMessageDTO.getFileType();
        Integer chunkCount = fileMessageDTO.getChunkCount();
        // 创建存入minio的文件路径
        String minioFilePath = fileMessageDTO.getMinioFilePath();
        // 分块文件所在路径
        String minioFileChunkPath = "file-chunk/" + fileHash + "/";
        // 合并文件分块
        minIOFileStorgeUtil.mergeFileChunks(minioFilePath, minioFileChunkPath, chunkCount, fileType);
        // 清除分块文件
        minIOFileStorgeUtil.clearChunkFlies(minioFileChunkPath, chunkCount, fileType);
    }

    @Override
    public void clearHistoryMessage(String conversationId) {
        Long senderId = UserHolder.getUser().getId();
        messageMapper.clearHistoryMessage(senderId, conversationId);
    }
}
