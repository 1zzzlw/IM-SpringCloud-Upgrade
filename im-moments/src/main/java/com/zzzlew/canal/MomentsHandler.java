package com.zzzlew.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.zzzlew.constant.RedisConstant.MOMENTS_INFO_LIST_KEY;

/**
 * Canal 监听 moments 表变更，帖子更新时删除对应 Redis 缓存
 */
@Slf4j
@Component
public class MomentsHandler {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${canal.server}")
    private String canalServer;

    @Value("${canal.destination}")
    private String destination;

    private CanalConnector connector;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = false;

    @PostConstruct
    public void start() {
        String host = canalServer.split(":")[0];
        int port = Integer.parseInt(canalServer.split(":")[1]);

        connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(host, port), destination, "", "");
        connector.connect();
        connector.subscribe("zzz-im-server\\.moments");
        connector.rollback();

        running = true;
        executor.submit(this::listen);
        log.info("Canal client started, destination: {}", destination);
    }

    private void listen() {
        while (running) {
            try {
                Message message = connector.getWithoutAck(100);
                long batchId = message.getId();
                if (batchId == -1 || message.getEntries().isEmpty()) {
                    TimeUnit.MILLISECONDS.sleep(500);
                    continue;
                }
                processEntries(message.getEntries());
                connector.ack(batchId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Canal listen error", e);
                connector.rollback();
            }
        }
    }

    private void processEntries(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) continue;

            CanalEntry.RowChange rowChange;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                log.error("Canal parse entry error", e);
                continue;
            }

            CanalEntry.EventType eventType = rowChange.getEventType();
            if (eventType != CanalEntry.EventType.UPDATE) continue;

            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                String id = null;
                for (CanalEntry.Column col : rowData.getAfterColumnsList()) {
                    if ("id".equals(col.getName())) {
                        id = col.getValue();
                        break;
                    }
                }
                if (id != null) {
                    String cacheKey = MOMENTS_INFO_LIST_KEY + id;
                    stringRedisTemplate.delete(cacheKey);
                    log.info("帖子更新，缓存已清除，momentId: {}", id);
                }
            }
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        executor.shutdown();
        if (connector != null) {
            connector.disconnect();
        }
        log.info("Canal client stopped");
    }
}
