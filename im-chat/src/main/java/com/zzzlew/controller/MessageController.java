package com.zzzlew.controller;

import com.zzzlew.domain.dto.FileChunkInfoDTO;
import com.zzzlew.domain.dto.FileMessageDTO;
import com.zzzlew.domain.dto.MessageDTO;
import com.zzzlew.domain.vo.FileMessageVO;
import com.zzzlew.domain.vo.MessageVO;
import com.zzzlew.result.Result;
import com.zzzlew.server.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/15 - 11 - 15 - 22:41
 * @Description: com.zzzlew.zzzimserver.controller
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/message")
@Tag(name = "消息模块")
public class MessageController {

    @Resource
    private MessageService messageService;

    /**
     * 根据前端传来的需要初始化的会话id列表，增量更新并热数据预加载消息列表，当前限额100条
     *
     * @param conversationIds 会话id列表
     * @param isInit          是否初始化
     * @return 消息列表
     */
    @Operation(summary = "根据前端传来的需要初始化的会话id列表，增量更新并热数据预加载消息列表，当前限额100条")
    @GetMapping("/init/list/{conversationIds}")
    public Result<List<MessageVO>> initMessageList(@PathVariable String conversationIds, @RequestParam Boolean isInit) {
        log.info("需要初始化的会话id列表为：{}", conversationIds);
        List<MessageVO> messageVOList = messageService.initMessageList(conversationIds, isInit);
        log.info("初始化完成，返回消息列表大小为：{}", messageVOList);
        return Result.success(messageVOList);
    }

    /**
     * 根据前端最旧消息拉取旧消息到前端数据库中
     *
     * @return 消息列表
     */
    @Operation(summary = "根据前端最旧消息拉取旧消息到前端数据库中")
    @GetMapping("/pull/list")
    public Result<List<MessageVO>> pullMessageList(@RequestParam String conversationId,
                                                   @RequestParam Long maxMessageId) {
        log.info("需要拉取旧消息的会话id为：{}，最旧消息id为：{}", conversationId, maxMessageId);
        List<MessageVO> messageVOList = messageService.pullMessageList(conversationId, maxMessageId);
        return Result.success(messageVOList);
    }

    /**
     * 发送消息
     *
     * @return 消息id
     */
    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public Result<MessageVO> sendMessage(@RequestBody MessageDTO messageDTO) {
        log.info("发送消息：{}", messageDTO);
        MessageVO messageVO = messageService.sendMessage(messageDTO);
        return Result.success(messageVO);
    }

    /**
     * 生成上传凭证
     *
     * @param fileId 文件id
     * @return 凭证id
     */
    @Operation(summary = "获取文件的上传凭证")
    @GetMapping("/verifyUploadToken/{fileId}")
    public Result<Map<String, Object>> verifyFileUploadToken(@PathVariable("fileId") String fileId) {
        log.info("验证文件上传token是否有效：{}", fileId);
        Map<String, Object> map = messageService.verifyFileUploadToken(fileId);
        return Result.success(map);
    }

    /**
     * 上传文件消息分块
     *
     * @return 消息id
     */
    @Operation(summary = "上传文件消息分块")
    @PostMapping("/uploadChunk")
    public Result<FileMessageVO> uploadFile(@RequestParam("chunkBlob") MultipartFile chunkBlob,
                                            @ModelAttribute FileChunkInfoDTO fileChunkInfoDTO) {
        log.info("上传文件分块消息的索引：{}，分块哈希值：{}，文件md5值：{}", fileChunkInfoDTO.getChunkIndex(), fileChunkInfoDTO.getChunkHash(),
                fileChunkInfoDTO.getFileId());
        messageService.uploadFileChunk(chunkBlob, fileChunkInfoDTO);
        return Result.success();
    }

    /**
     * 检查文件分片是否上传完成
     *
     * @param verify 凭证id
     * @param fileId 文件哈希值
     * @return 已上传分片索引列表
     */
    @Operation(summary = "获取上传成功的文件分块索引列表")
    @GetMapping("/checkUploaded")
    public Result<List<Integer>> checkUploaded(@RequestParam("verify") String verify,
                                               @RequestParam("fileId") String fileId) {
        log.info("检查文件分块是否上传完成：{},{}", verify, fileId);
        List<Integer> uploadedChunkIndices = messageService.checkUploaded(verify, fileId);
        log.info("上传过的文件分块集合：{}", uploadedChunkIndices);
        return Result.success(uploadedChunkIndices);
    }

    /**
     * 合并文件分块
     *
     * @return 合并后的文件消息
     */
    @Operation(summary = "合并文件分块")
    @PostMapping("/merge")
    public Result<FileMessageVO> mergeFile(@RequestBody FileMessageDTO fileMessageDTO) {
        log.info("合并文件分块：{}", fileMessageDTO);
        messageService.mergeFile(fileMessageDTO);
        return Result.success();
    }

    /**
     * 更新文件消息发送状态
     *
     * @return 响应结果
     */
    @Operation(summary = "更新文件消息发送状态")
    @PutMapping("/updateFileSendStatus")
    public Result<Object> updateFileSendStatus(@RequestParam("fileId") String fileId,
                                               @RequestParam("sendStatus") Integer sendStatus) {
        log.info("更新消息发送状态：{}, {}", fileId, sendStatus);
        messageService.updateSendStatus(fileId, sendStatus);
        return Result.success();
    }

    @Operation(summary = "撤回消息")
    @DeleteMapping("/recallMessage")
    public Result<Object> recallMessage(String conversationId, Long messageId) {
        log.info("撤回消息的会话id：{}，和消息id：{}", conversationId, messageId);
        messageService.recallMessage(conversationId, messageId);
        return Result.success();
    }

    // TODO 清空历史消息应该只清空本地的就可以了，这个请求感觉没什么用
    @Operation(summary = "清空历史消息")
    @DeleteMapping("/clearHistoryMessage/{conversationId}")
    public Result<Object> clearHistoryMessage(@PathVariable String conversationId) {
        log.info("清空历史消息");
        messageService.clearHistoryMessage(conversationId);
        return Result.success();
    }

}
