package com.zzzlew.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @Auther: zzzlew
 * @Date: 2025/10/13 - 10 - 13 - 23:49
 * @Description: itcast.protocol
 * @version: 1.0
 */
public class ProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {

    /** 完整应用消息上限，包含16字节协议头；与WebSocket载荷上限共用。 */
    public static final int MAX_FRAME_LENGTH = 64 * 1024;

    public ProtocolFrameDecoder() {
        /**
         * maxFrameLength 允许解码的「最大帧长度」，单条消息的最大大小
         * lengthFieldOffset 「长度字段」在整个报文中的起始偏移量，也就是说自定义解码器协议的长度和正文的距离
         * lengthFieldLength 长度字段本身占用的字节数，4个字节存储一个数字，用来告诉解码器后面正文部分的长度大小
         * LengthAdjustment 长度字段的值和「实际整帧长度」的差值补偿
         * initialBytesToStrip 解码后「要跳过 / 剥离的字节数」
         * */
        this(MAX_FRAME_LENGTH, 12, 4, 0, 0);
    }

    public ProtocolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment,
        int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }
}
