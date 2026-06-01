package com.zzzlew.controller;

import com.zzzlew.domain.dto.AIMessageDTO;
import com.zzzlew.domain.dto.AIPersonalityDTO;
import com.zzzlew.domain.vo.AIMessageVO;
import com.zzzlew.domain.vo.AIPersonalityVO;
import com.zzzlew.result.Result;
import com.zzzlew.server.AIMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/4 - 03 - 04 - 20:03
 * @Description: com.zzzlew.controller
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/ai-message")
@Tag(name = "ai对话接口")
public class AIMessageController {

    @Resource
    private AIMessageService aiMessageService;

    @Operation(summary = "加载ai聊天记录")
    @GetMapping("/loadMessage")
    public Result<List<AIMessageVO>> loadMessage() {
        List<AIMessageVO> history = aiMessageService.loadMessage();
        return Result.success(history);
    }

    @Operation(summary = "ai识别文本消息")
    @PostMapping(value = "/sendMessage", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> sendMessage(@RequestBody AIMessageDTO aiMessageDTO) {
        log.info("接收到文本消息为：{}", aiMessageDTO);
        return aiMessageService.sendMessage(aiMessageDTO);
    }

    @Operation(summary = "ai识别图像消息")
    @PostMapping(value = "/sendImageMessage", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> sendImageMessage(@RequestBody AIMessageDTO aiMessageDTO) {
        log.info("接收到图像消息为：{}", aiMessageDTO);
        return null;
    }

    @Operation(summary = "创建ai个性化配置")
    @PostMapping("/createPersonality")
    public Result<String> createPersonality(AIPersonalityDTO aiPersonalityDTO,
                                            @RequestParam("avatarFile") MultipartFile avatarFile) {
        log.info("创建ai个性化配置信息为 {}，头像信息为 {}", aiPersonalityDTO, avatarFile);
        String id = aiMessageService.createPersonality(aiPersonalityDTO, avatarFile);
        return Result.success(id);
    }

    @Operation(summary = "更新ai个性化内容")
    @PostMapping("/updatePersonality")
    public Result<Object> updatePersonality(@RequestBody AIPersonalityDTO aiPersonalityDTO) {
        log.info("更新后个性化ai信息为: {}", aiPersonalityDTO);
        aiMessageService.updatePersonality(aiPersonalityDTO);
        return Result.success();
    }

    @Operation(summary = "删除ai个性化配置")
    @DeleteMapping("/deletePersonality/{id}")
    public Result<Object> deletePersonality(@PathVariable Long id) {
        aiMessageService.deletePersonality(id);
        return Result.success();
    }

    @Operation(summary = "切换ai个性化配置")
    @PostMapping("/switchPersonality/{id}")
    public Result<String> switchPersonality(@PathVariable Long id) {
        String avatar = aiMessageService.switchPersonality(id);
        return Result.success(avatar);
    }

    @Operation(summary = "获取ai个性化配置列表")
    @GetMapping("/listPersonality")
    public Result<List<AIPersonalityVO>> listPersonality() {
        List<AIPersonalityVO> list = aiMessageService.listPersonality();
        log.info("获取ai个性化配置列表为：{}", list);
        return Result.success(list);
    }

}
