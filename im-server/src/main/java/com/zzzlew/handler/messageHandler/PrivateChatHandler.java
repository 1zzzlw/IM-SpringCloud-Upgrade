package com.zzzlew.handler.messageHandler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.zzzlew.domain.request.PrivateChatRequestDTO;
import com.zzzlew.domain.response.ACKMessageResponseVO;
import com.zzzlew.domain.response.PrivateChatResponseVO;
import com.zzzlew.handler.impl.MessageHandler;
import com.zzzlew.result.MessageResult;
import com.zzzlew.utils.ACKMessagePackUtil;
import com.zzzlew.utils.ChannelManageUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/19 - 11 - 19 - 21:25
 * @Description: com.zzzlew.zzzimserver.handler.messageHandler
 * @version: 1.0
 */
@Slf4j
@ChannelHandler.Sharable
public class PrivateChatHandler implements MessageHandler<PrivateChatRequestDTO> {

    @Override
    public MessageResult handle(ChannelHandlerContext ctx, PrivateChatRequestDTO privateChatRequestDTO) {
        // 处理私聊消息
        log.info("收到私聊消息：{}", privateChatRequestDTO);
        // 获得当前登录用户id
        Long userId = ChannelManageUtil.getUser(ctx.channel()).getId();
        privateChatRequestDTO.setSenderId(userId);
        // sendTime 由客户端提供（雪花ID对应的时间戳），保证 WS 推送、DB 存储、ACK 回传三者时间一致
        // 仅在客户端未提供时才由服务端填充
        // 获得接收者id
        Long receiverId = privateChatRequestDTO.getReceiverId();
        log.info("私信消息:{}", privateChatRequestDTO);
        // 封装回应消息
        PrivateChatResponseVO privateChatResponseVO = BeanUtil.copyProperties(privateChatRequestDTO, PrivateChatResponseVO.class);
        // 优先使用客户端的 sendTime，保证 WS 推送和 DB 存储时间一致；仅在未提供时由服务端填充
        if (privateChatResponseVO.getSendTime() == null) {
            privateChatResponseVO.setSendTime(LocalDateTime.now());
        }
        // 如果客户端已提供ID（文件消息经REST API预存），复用该ID；否则生成新的雪花ID
        String messageId;
        if (privateChatRequestDTO.getId() != null && privateChatRequestDTO.getId() != 0) {
            messageId = String.valueOf(privateChatRequestDTO.getId());
        } else {
            messageId = String.valueOf(IdUtil.getSnowflakeNextId());
        }
        privateChatResponseVO.setId(messageId);
        log.info("已向接收者{}的channel写入私聊消息:{}", receiverId, privateChatResponseVO);
        // 返回发送消息成功发送服务端的ACK消息
        ACKMessageResponseVO successACK = ACKMessagePackUtil.createSuccessACK(String.valueOf(privateChatRequestDTO.getId()), privateChatResponseVO.getId());
        log.info("ACK消息为：{}", successACK);
        ctx.channel().writeAndFlush(successACK);
        return MessageResult.single(privateChatResponseVO, receiverId);
    }

}
