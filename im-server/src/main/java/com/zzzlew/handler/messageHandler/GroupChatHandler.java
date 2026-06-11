package com.zzzlew.handler.messageHandler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.zzzlew.domain.request.GroupChatRequestDTO;
import com.zzzlew.domain.response.ACKMessageResponseVO;
import com.zzzlew.domain.response.GroupChatResponseVO;
import com.zzzlew.handler.impl.MessageHandler;
import com.zzzlew.result.MessageResult;
import com.zzzlew.utils.ACKMessagePackUtil;
import com.zzzlew.utils.ChannelManageUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/19 - 11 - 19 - 21:25
 * @Description: com.zzzlew.zzzimserver.handler.messageHandler
 * @version: 1.0
 */
@Slf4j
@ChannelHandler.Sharable
public class GroupChatHandler implements MessageHandler<GroupChatRequestDTO> {

    @Override
    public MessageResult handle(ChannelHandlerContext ctx, GroupChatRequestDTO groupChatRequestDTO) {
        // 处理群聊消息
        log.info("收到群聊消息: {}", groupChatRequestDTO);
        // 获得当前登录用户id
        Long userId = ChannelManageUtil.getUser(ctx.channel()).getId();
        groupChatRequestDTO.setSenderId(userId);
        // sendTime 由客户端提供，保证 WS 推送、DB 存储、ACK 回传三者时间一致
        // 仅在客户端未提供时才由服务端填充
        // 获得接收者id列表
        List<Long> receiverIds = groupChatRequestDTO.getReceiverIds();
        String receiverId = groupChatRequestDTO.getReceiverId();
        GroupChatResponseVO groupChatResponseVO =
                BeanUtil.copyProperties(groupChatRequestDTO, GroupChatResponseVO.class);
        groupChatResponseVO.setReceiverId(receiverId);
        // 优先使用客户端的 sendTime，保证 WS 推送和 DB 存储时间一致；仅在未提供时由服务端填充
        if (groupChatResponseVO.getSendTime() == null) {
            groupChatResponseVO.setSendTime(LocalDateTime.now());
        }
        // 如果客户端已提供ID（文件消息经REST API预存），复用该ID；否则生成新的雪花ID
        String messageId;
        if (groupChatRequestDTO.getId() != null && groupChatRequestDTO.getId() != 0) {
            messageId = String.valueOf(groupChatRequestDTO.getId());
        } else {
            messageId = String.valueOf(IdUtil.getSnowflakeNextId());
        }
        groupChatResponseVO.setId(messageId);
        log.info("群聊回应的消息为: {}", groupChatResponseVO);
        // 返回发送消息成功发送服务端的ACK消息
        ACKMessageResponseVO successACK = ACKMessagePackUtil.createSuccessACK(String.valueOf(groupChatRequestDTO.getId()), groupChatResponseVO.getId());
        log.info("ACK消息为：{}", successACK);
        ctx.channel().writeAndFlush(successACK);
        return MessageResult.multiple(groupChatResponseVO, receiverIds);
    }

}
