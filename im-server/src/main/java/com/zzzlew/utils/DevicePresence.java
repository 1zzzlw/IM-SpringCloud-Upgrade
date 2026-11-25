package com.zzzlew.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zzzlew.domain.response.DevicePresenceResponseVO;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zzzlew.config.NettyConfig.NETTY_CLUSTER_ID;
import static com.zzzlew.constant.RedisConstant.*;

@Slf4j
public class DevicePresence {
    public static final int CONNECTION_IDLE_SECONDS = 90;
    private static final String CONNECTION_KEY_PREFIX = "im:device:presence:";
    private static final AttributeKey<DevicePresenceResponseVO> LAST_SNAPSHOT =
            AttributeKey.valueOf("im.devicePresence");
    private static final DefaultRedisScript<Long> REGISTER = script("register_device_presence.lua");
    private static final DefaultRedisScript<Long> REMOVE = script("remove_device_presence.lua");

    public enum Disconnect {
        STALE, LAST_DEVICE, OTHER_DEVICES_REMAIN
    }

    private static DefaultRedisScript<Long> script(String name) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/" + name));
        script.setResultType(Long.class);
        return script;
    }

    public static Long register(StringRedisTemplate redis, ChannelManageUtil.Session session, Channel channel) {
        String connectionId = channel.id().asLongText();
        return redis.execute(REGISTER,
                List.of(USER_ONLINE_STATUS_KEY, USER_CLUSTER_MAPPING_KEY, session.accessKey(), session.redisKey(),
                        CONNECTION_KEY_PREFIX + connectionId),
                session.user().getId().toString(), session.deviceId(), NETTY_CLUSTER_ID, connectionId,
                USER_ONLINE_STATUS_KEY_TTL.toString(), session.sessionId(), String.valueOf(CONNECTION_IDLE_SECONDS));
    }

    public static Disconnect remove(StringRedisTemplate redis, ChannelManageUtil.Session session, Channel channel) {
        String connectionId = channel.id().asLongText();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Long removed = redis.execute(REMOVE,
                List.of(USER_ONLINE_STATUS_KEY, USER_CLUSTER_MAPPING_KEY, USER_OFFLINE_INFO_KEY,
                        CONNECTION_KEY_PREFIX + connectionId),
                session.user().getId().toString(), session.deviceId(), connectionId, time);
        if (Long.valueOf(1).equals(removed)) return Disconnect.LAST_DEVICE;
        if (Long.valueOf(2).equals(removed)) return Disconnect.OTHER_DEVICES_REMAIN;
        return Disconnect.STALE;
    }

    public static void onHeartbeat(StringRedisTemplate redis, Channel channel) {
        try {
            redis.opsForValue().set(CONNECTION_KEY_PREFIX + channel.id().asLongText(), "1",
                    Duration.ofSeconds(CONNECTION_IDLE_SECONDS));
            pushSnapshot(redis, channel);
        } catch (RuntimeException e) {
            log.warn("更新设备在线状态失败", e);
        }
    }

    public static void pushSnapshot(StringRedisTemplate redis, Channel channel) {
        // 跨节点通知只触发重查。同一连接串行读取并发送，避免迟到的通知恢复旧状态。
        channel.eventLoop().execute(() -> {
            try {
                if (!channel.isWritable() || !ChannelManageUtil.isCurrent(channel, redis)) return;
                Long userId = ChannelManageUtil.getUser(channel).getId();
                DevicePresenceResponseVO snapshot = snapshot(redis, userId);
                var last = channel.attr(LAST_SNAPSHOT);
                if (snapshot.equals(last.get())) return;
                last.set(snapshot);
                channel.writeAndFlush(snapshot).addListener(future -> {
                    if (!future.isSuccess()) last.compareAndSet(snapshot, null);
                });
            } catch (RuntimeException e) {
                log.warn("推送设备在线状态失败", e);
            }
        });
    }

    private static DevicePresenceResponseVO snapshot(StringRedisTemplate redis, Long userId) {
        DevicePresenceResponseVO snapshot = new DevicePresenceResponseVO();
        Object encodedRoutes = redis.opsForHash().get(USER_CLUSTER_MAPPING_KEY, userId.toString());
        if (encodedRoutes == null || !encodedRoutes.toString().startsWith("{")) return snapshot;
        JSONObject routes = JSON.parseObject(encodedRoutes.toString());
        List<String> devices = routes.keySet().stream()
                .filter(id -> id.startsWith("desktop_") || id.startsWith("app_"))
                .toList();
        if (devices.isEmpty()) return snapshot;

        List<String> keys = devices.stream()
                .map(id -> CONNECTION_KEY_PREFIX + routes.getJSONArray(id).getString(1))
                .toList();
        List<String> live = redis.opsForValue().multiGet(keys);
        for (int i = 0; i < devices.size(); i++) {
            if (live.get(i) == null) continue;
            if (devices.get(i).startsWith("desktop_")) snapshot.setDesktopOnline(true);
            else snapshot.setAppOnline(true);
        }
        return snapshot;
    }

    public static Map<String, List<String>> groupByNode(Object encodedRoutes, String excludedConnection) {
        Map<String, List<String>> nodes = new LinkedHashMap<>();
        if (encodedRoutes == null || !encodedRoutes.toString().startsWith("{")) return nodes;
        JSONObject routes = JSON.parseObject(encodedRoutes.toString());
        for (String deviceId : routes.keySet()) {
            var route = routes.getJSONArray(deviceId);
            String connectionId = route.getString(1);
            if (!connectionId.equals(excludedConnection)) {
                nodes.computeIfAbsent(route.getString(0), node -> new ArrayList<>()).add(connectionId);
            }
        }
        return nodes;
    }
}
