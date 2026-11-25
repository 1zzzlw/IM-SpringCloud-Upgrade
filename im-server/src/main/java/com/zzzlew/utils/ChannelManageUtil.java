package com.zzzlew.utils;

import com.zzzlew.domain.dto.UserBaseDTO;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.zzzlew.constant.RedisConstant.SESSION_ID_CLAIM;
import static com.zzzlew.constant.RedisConstant.deviceSessionKey;

public class ChannelManageUtil {
    private static final AttributeKey<Session> SESSION = AttributeKey.valueOf("im.session");
    private static final AttributeKey<Boolean> REGISTERED = AttributeKey.valueOf("im.registered");
    private static final ConcurrentHashMap<Long, Set<Channel>> CHANNELS = new ConcurrentHashMap<>();

    public record Session(UserBaseDTO user, String deviceId, String sessionId, String accessKey) {
        public String redisKey() {
            return deviceSessionKey(user.getId(), deviceId);
        }
    }

    public static void authenticate(Channel channel, Session session) {
        channel.attr(SESSION).set(session);
    }

    public static Session getSession(Channel channel) {
        return channel.attr(SESSION).get();
    }

    public static UserBaseDTO getUser(Channel channel) {
        Session session = getSession(channel);
        return session == null ? null : session.user();
    }

    public static void register(Channel channel) {
        Session session = getSession(channel);
        channel.attr(REGISTERED).set(true);
        CHANNELS.compute(session.user().getId(), (userId, devices) -> {
            if (devices == null) devices = ConcurrentHashMap.newKeySet();
            devices.add(channel);
            return devices;
        });
    }

    public static List<Channel> getChannels(Long userId) {
        var devices = CHANNELS.get(userId);
        return devices == null ? List.of() : List.copyOf(devices);
    }

    public static boolean removeChannel(Channel channel) {
        Session session = channel.attr(SESSION).getAndSet(null);
        if (session == null || !Boolean.TRUE.equals(channel.attr(REGISTERED).getAndSet(false))) return false;
        CHANNELS.computeIfPresent(session.user().getId(), (userId, devices) -> {
            devices.remove(channel);
            return devices.isEmpty() ? null : devices;
        });
        return true;
    }

    public static boolean isCurrent(Channel channel, StringRedisTemplate redis) {
        Session session = getSession(channel);
        if (session == null || !channel.isActive() || !Boolean.TRUE.equals(channel.attr(REGISTERED).get())) return false;
        if (session.sessionId().isEmpty()) return Boolean.TRUE.equals(redis.hasKey(session.accessKey()));
        List<Object> current = redis.opsForHash().multiGet(session.redisKey(), List.of(SESSION_ID_CLAIM, "connectionId"));
        return current != null && current.size() == 2 && session.sessionId().equals(current.get(0))
                && channel.id().asLongText().equals(current.get(1));
    }
}
