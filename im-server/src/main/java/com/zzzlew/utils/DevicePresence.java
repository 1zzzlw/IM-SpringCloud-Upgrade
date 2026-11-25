package com.zzzlew.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zzzlew.config.NettyConfig.NETTY_CLUSTER_ID;
import static com.zzzlew.constant.RedisConstant.*;

public class DevicePresence {
    private static final DefaultRedisScript<Long> REGISTER = script("register_device_presence.lua");
    private static final DefaultRedisScript<Long> REMOVE = script("remove_device_presence.lua");

    private static DefaultRedisScript<Long> script(String name) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/" + name));
        script.setResultType(Long.class);
        return script;
    }

    public static Long register(StringRedisTemplate redis, ChannelManageUtil.Session session, Channel channel) {
        return redis.execute(REGISTER,
                List.of(USER_ONLINE_STATUS_KEY, USER_CLUSTER_MAPPING_KEY, session.accessKey(), session.redisKey()),
                session.user().getId().toString(), session.deviceId(), NETTY_CLUSTER_ID, channel.id().asLongText(),
                USER_ONLINE_STATUS_KEY_TTL.toString(), session.sessionId());
    }

    public static boolean wentOffline(StringRedisTemplate redis, ChannelManageUtil.Session session, Channel channel) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Long removed = redis.execute(REMOVE,
                List.of(USER_ONLINE_STATUS_KEY, USER_CLUSTER_MAPPING_KEY, USER_OFFLINE_INFO_KEY),
                session.user().getId().toString(), session.deviceId(), channel.id().asLongText(), time);
        return Long.valueOf(1).equals(removed);
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
