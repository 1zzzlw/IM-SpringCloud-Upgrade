package com.zzzlew.server.impl;

import cn.hutool.core.util.IdUtil;
import com.zzzlew.domain.dto.AIMessageDTO;
import com.zzzlew.domain.dto.AIPersonalityDTO;
import com.zzzlew.domain.vo.AIMessageVO;
import com.zzzlew.domain.vo.AIPersonalityVO;
import com.zzzlew.mapper.AIMessageMapper;
import com.zzzlew.properties.MinIOConfigProperties;
import com.zzzlew.server.AIMessageService;
import com.zzzlew.utils.MinIOFileStorgeUtil;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/5 - 03 - 05 - 13:51
 * @Description: com.zzzlew.server.impl
 * @version: 1.0
 */
@Slf4j
@Service
public class AIMessageServiceImpl implements AIMessageService {

    @Resource(name = "ollamaChatClient")
    private ChatClient ollamaChatClient;
    @Resource(name = "zhipuChatClient")
    private ChatClient zhipuChatClient;
    @Resource
    private AIMessageMapper aiMessageMapper;
    @Resource
    private MinIOFileStorgeUtil minIOFileStorgeUtil;
    @Resource
    private MinIOConfigProperties minIOConfigProperties;

    @Override
    public List<AIMessageVO> loadMessage() {
        // 获得当前用户的id
        Long userId = UserHolder.getUser().getId();
        List<AIMessageVO> history = aiMessageMapper.selectHistory(userId);
        return history;
    }

    @Override
    public Flux<String> sendMessage(AIMessageDTO aiMessageDTO) {
        // 获得用户发送的内容
        String content = aiMessageDTO.getContent();
        StringBuilder fullContent = new StringBuilder();
        aiMessageDTO.setId(IdUtil.getSnowflakeNextId());

        // 获得历史消息
        List<AIMessageVO> history = aiMessageMapper.selectHistory(aiMessageDTO.getUserId());

        log.info("历史消息为: {}", history);
        List<Message> historyMessages = new ArrayList<>();
        for (AIMessageVO msg : history) {
            if ("user".equals(msg.getRole())) {
                historyMessages.add(new UserMessage(msg.getContent()));
            } else {
                historyMessages.add(new AssistantMessage(msg.getContent()));
            }
        }

        // 获取ai智能体信息
        AIPersonalityVO aiPersonality = aiMessageMapper.getActivePersonality(aiMessageDTO.getUserId());

        String personalityPrompt = "当前用户没有ai个性化";

        if (aiPersonality != null) {
            log.info("当前用户有ai个性化");
            // 添加ai个性化信息
            personalityPrompt = String.format("""
                    你的角色：%s
                    性格设定：%s
                    """, aiPersonality.getName(), aiPersonality.getSystemPrompt());
        } else {
            log.info("当前用户没有ai个性化");
        }

        // 返回ai思考的消息，并保存到数据库
        return zhipuChatClient.prompt().system(personalityPrompt).messages(historyMessages).user(content).stream().content().doOnNext(chunk -> {
            // 每个chunk追加到完整内容
            fullContent.append(chunk);
        }).doOnComplete(() -> {
            // 流结束后保存到数据库
            log.info("保存消息到数据库：{}", fullContent);
            AIMessageDTO aiResponseDTO = new AIMessageDTO();
            aiResponseDTO.setId(IdUtil.getSnowflakeNextId());
            aiResponseDTO.setUserId(aiMessageDTO.getUserId());
            aiResponseDTO.setRole("assistant");
            aiResponseDTO.setMsgType(1);
            aiResponseDTO.setContent(fullContent.toString());
            // 保存ai回复的消息
            aiMessageMapper.saveMessage(aiResponseDTO);
            // 保存用户发送的消息
            aiMessageMapper.saveMessage(aiMessageDTO);
        });
    }

    @Override
    public void updatePersonality(AIPersonalityDTO aiPersonalityDTO) {
        Long id = aiPersonalityDTO.getId();
        String roleName = aiPersonalityDTO.getName();
        String content = aiPersonalityDTO.getSystemPrompt();

        aiMessageMapper.updatePersonality(id, roleName, content);
    }

    @Transient
    @Override
    public String createPersonality(AIPersonalityDTO aiPersonalityDTO, MultipartFile avatarFile) {
        // 获得当前用户的id
        Long userId = UserHolder.getUser().getId();
        aiPersonalityDTO.setUserId(userId);

        String randomId = IdUtil.getSnowflakeNextId() + "";

        String avatarName = userId + "/" + randomId + "AIAvatar." + avatarFile.getContentType().split("/")[1];
        // 生成远端的存储路径
        String baseUrl = minIOFileStorgeUtil.buildFilePath(avatarName);
        // 上传用户头像到minio服务端
        minIOFileStorgeUtil.uploadAvatar(baseUrl, avatarFile);
        // 生成本地存储远程路径
        String avatar = minIOConfigProperties.getEndpoint() + "/" + minIOConfigProperties.getAvatarBucket() + "/" + baseUrl;
        aiPersonalityDTO.setAvatar(avatar);

        aiMessageMapper.createPersonality(aiPersonalityDTO);

        Long id = aiPersonalityDTO.getId();

        log.info("id为 {}", id);

        return id.toString();
    }

    @Transactional
    @Override
    public String switchPersonality(Long id) {
        Long userId = UserHolder.getUser().getId();

        // 取消当前用户的所有ai智能体个性化
        aiMessageMapper.cancelAllAIPersonality(userId);

        // 切换ai智能体的个性化
        aiMessageMapper.switchPersonality(id, userId);

        // 获取当前ai智能体的头像地址
        String avatar = aiMessageMapper.getAiAvatar(id, userId);

        return avatar;
    }

    @Override
    public List<AIPersonalityVO> listPersonality() {
        log.info("获取ai个性化配置列表");
        // 获得当前用户的id
        Long userId = UserHolder.getUser().getId();
        List<AIPersonalityVO> personality = aiMessageMapper.getPersonality(userId);
        return personality;
    }

    @Override
    public void deletePersonality(Long id) {
        Long userId = UserHolder.getUser().getId();
        aiMessageMapper.deletePersonality(id, userId);
    }
}
