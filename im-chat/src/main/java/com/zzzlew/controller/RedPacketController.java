package com.zzzlew.controller;

import com.zzzlew.domain.dto.RedPacketDTO;
import com.zzzlew.domain.vo.RedPacketVO;
import com.zzzlew.result.Result;
import com.zzzlew.server.RedPacketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 红包接口
 */
@Slf4j
@RestController
@RequestMapping("/redPacket")
@Tag(name = "红包接口")
public class RedPacketController {

    @Resource
    private RedPacketService redPacketService;

    /**
     * 发送红包
     * 返回红包VO（含红包ID），前端用此ID发送WS消息通知对方/群成员
     */
    @Operation(summary = "发送红包")
    @PostMapping("/send")
    public Result<RedPacketVO> sendRedPacket(@RequestBody RedPacketDTO redPacketDTO) {
        log.info("发送红包：{}", redPacketDTO);
        RedPacketVO vo = redPacketService.sendRedPacket(redPacketDTO);
        return Result.success(vo);
    }

    /**
     * 抢红包
     * 返回领取的金额
     */
    @Operation(summary = "抢红包")
    @PostMapping("/grab/{redPacketId}")
    public Result<BigDecimal> grabRedPacket(@PathVariable Long redPacketId) {
        log.info("抢红包：redPacketId={}", redPacketId);
        BigDecimal amount = redPacketService.grabRedPacket(redPacketId);
        return Result.success(amount);
    }

    /**
     * 查看红包详情（含领取记录）
     */
    @Operation(summary = "查看红包详情")
    @GetMapping("/detail/{redPacketId}")
    public Result<RedPacketVO> getRedPacketDetail(@PathVariable Long redPacketId) {
        log.info("查看红包详情：redPacketId={}", redPacketId);
        RedPacketVO vo = redPacketService.getRedPacketDetail(redPacketId);
        return Result.success(vo);
    }
}
