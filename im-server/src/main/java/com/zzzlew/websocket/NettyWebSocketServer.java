package com.zzzlew.websocket;


import com.zzzlew.handler.*;
import com.zzzlew.protocol.MessageCodecSharable;
import com.zzzlew.protocol.ProtocolFrameDecoder;
import com.zzzlew.utils.NettyPlatformOptimizer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.zzzlew.config.NettyConfig.NETTY_CLUSTER_ID;
import static com.zzzlew.config.NettyConfig.NETTY_PORT;
import static com.zzzlew.constant.RabbitMQConstant.QUEUE_NETTY_PREFIX;
import static com.zzzlew.constant.RabbitMQConstant.QUEUE_NETTY_ROUTING_KEY;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/17 - 11 - 17 - 20:46
 * @Description: com.zzzlew.zzzimserver.websocket
 * @version: 1.0
 */
@Slf4j
@Service
public class NettyWebSocketServer {

    private final EventLoopGroup bossGroup = NettyPlatformOptimizer.createOptimalBossEventLoopGroup();
    private final EventLoopGroup workerGroup = NettyPlatformOptimizer.createOptimalEventLoopGroup();

    @Resource
    private RabbitAdmin rabbitAdmin;
    @Resource
    @Qualifier("imTopicExchange")
    private TopicExchange imTopicExchange;

    // 启动Netty服务器
    @PostConstruct
    public void start() throws InterruptedException {
        // 打印简要平台信息
        log.info(NettyPlatformOptimizer.getPlatformOptimizationTips());

        // 初始化MQ消息队列
        initNettyRabbitMQ();
        // 启动服务
        run();
        log.info("Netty服务器启动成功，端口：{}", NETTY_PORT);
    }

    // 结束前销毁Netty服务器
    @PreDestroy
    public void destroy() {
        Future<?> future1 = bossGroup.shutdownGracefully();
        Future<?> future2 = workerGroup.shutdownGracefully();
        future1.syncUninterruptibly();
        future2.syncUninterruptibly();
        log.info("Netty服务器销毁成功");
    }

    public void run() throws InterruptedException {
        // 创建Netty服务器启动器
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.group(bossGroup, workerGroup).channel(NettyPlatformOptimizer.getOptimalServerChannelClass())
                // 用于设置 TCP 连接的 半连接队列（backlog）大小
                .option(ChannelOption.SO_BACKLOG, NettyPlatformOptimizer.getOptimalBacklog())
                // 禁用 Nagle 算法，减少延迟
                .childOption(ChannelOption.TCP_NODELAY, true)
                // 用于设置 TCP 连接的 保活时间（keep-alive），单位为秒
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                // 允许端口快速重用
                .childOption(ChannelOption.SO_REUSEADDR, true)
                // 显式使用池化分配器
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        // 用于记录 Netty 服务器的日志
        serverBootstrap.handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline pipeline = socketChannel.pipeline();
                // websocket协议本身是基于http协议的，所以这边也要使用http解编码器
                pipeline.addLast(new HttpServerCodec());
                // 将HTTP消息的多个部分聚合成完整的FullHttpRequest 确保收到完整的HTTP请求再处理 ，避免处理不完整的请求
                // 入站请求，这个处理器只在入站的时候处理
                pipeline.addLast(new HttpObjectAggregator(128 * 1024));
                // 出战请求，用于处理 HTTP 消息的分块写入，确保数据按块发送 这个处理器在消息入站的时候不使用，因为消息入站的只是请求
                pipeline.addLast(new ChunkedWriteHandler());
                // 自定义 Http 处理头，用于处理 Http 头信息
                pipeline.addLast(new HttpHeadersHandler());
                // 用于处理 WebSocket 协议的握手、升级和关闭，会自动处理 ping/pong 帧
                pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true, 131072, true, true, 30000L));
                // 用于将 BinaryWebSocketFrame 转换为 ByteBuf ，因为后续的处理器都需要 ByteBuf 类型的消息
                pipeline.addLast(new BinaryWebSocketFrameToByteBufHandler());
                // 自定义协议处理，WebSocket 升级完成后使用
                pipeline.addLast(new ProtocolFrameDecoder());
                // 出站转换处理器 - 必须放在所有出站消息处理之后，但在WebSocketServerProtocolHandler之前
                pipeline.addLast(new ByteBufToBinaryWebSocketFrameHandler());
                // 消息编码器，用于将消息编码为 ByteBuf ，因为后续的处理器都需要 ByteBuf 类型的消息
                pipeline.addLast(new MessageCodecSharable());
                // 先统计解码后的入站消息，再交给会消费消息的处理器，避免心跳被提前拦截。
                // 连续 90 秒没有读到完整的应用层消息时触发 READER_IDLE。
                pipeline.addLast(new IdleStateHandler(90, 0, 0, TimeUnit.SECONDS));
                pipeline.addLast(new HeartBeatHandler());
                // 消息分发处理器，用于分发消息到不同的处理器
                pipeline.addLast(new MessageDispatcherHandler());
                // 连接成功处理器，用于处理连接成功事件，此时用户上线
                pipeline.addLast(new ConnectSuccessMessageHandler());
                // 退出登录处理器，用于处理退出登录请求
                pipeline.addLast(new QuitLoginHandler());
            }
        });
        // 启动Netty服务器
        serverBootstrap.bind(NETTY_PORT).sync();
    }

    public void initNettyRabbitMQ() {
        // 创建当前Netty节点的专属队列
        Queue queue = QueueBuilder.durable(QUEUE_NETTY_PREFIX + NETTY_CLUSTER_ID).lazy().build();
        rabbitAdmin.declareQueue(queue);

        // 绑定到Topic交换机
        Binding binding = BindingBuilder.bind(queue).to(imTopicExchange).with(QUEUE_NETTY_ROUTING_KEY + NETTY_CLUSTER_ID);
        rabbitAdmin.declareBinding(binding);
    }

}
