package com.zzzlew.listener;

import com.zzzlew.domain.ClusterMessageWrapper;
import com.zzzlew.domain.dto.MessageDTO;
import com.zzzlew.server.MessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;

import static com.zzzlew.constant.RabbitMQConstant.QUEUE_STORGE_PREFIX;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/24 - 03 - 24 - 23:07
 * @Description: com.zzzlew.listener
 * @version: 1.0
 */
@Slf4j
@Configuration
public class MQMessageListener {

    @Resource
    private MessageService messageService;

    /**
     * 监听本集群的消息队列
     * 所有消息类型均持久化（INSERT IGNORE 自动处理已存在的记录，如文件消息经REST API预存的情况）
     */
    @RabbitListener(queues = QUEUE_STORGE_PREFIX)
    public void handleClusterMessage(ClusterMessageWrapper<MessageDTO> wrapper) {
        log.info("收到集群消息: {}", wrapper.getMessage());
        MessageDTO messageDTO = wrapper.getMessage();
        messageService.sendMessage(messageDTO);
    }

}
