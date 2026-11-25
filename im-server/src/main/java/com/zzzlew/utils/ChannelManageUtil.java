package com.zzzlew.utils;

import com.zzzlew.domain.dto.UserBaseDTO;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.zzzlew.constant.RedisConstant.LOGIN_USERINFO_ACCESSTOKEN_KEY;
import static com.zzzlew.constant.RedisConstant.deviceTokenKey;

public class ChannelManageUtil {
    private static final AttributeKey<UserBaseDTO> USER = AttributeKey.valueOf("im.user");
    private static final AttributeKey<String> DEVICE_ID = AttributeKey.valueOf("im.deviceId");
    private static final AttributeKey<String> ACCESS_TOKEN = AttributeKey.valueOf("im.accessToken");
    private static final AttributeKey<Boolean> REGISTERED = AttributeKey.valueOf("im.registered");
    private static final ConcurrentHashMap<Long, Set<Channel>> CHANNELS = new ConcurrentHashMap<>();

    public static void authenticate(Channel channel, UserBaseDTO user, String deviceId, String accessToken) {
        channel.attr(DEVICE_ID).set(deviceId);
        channel.attr(ACCESS_TOKEN).set(accessToken);
        channel.attr(USER).set(user);
    }

    public static UserBaseDTO getUser(Channel channel) {
        return channel.attr(USER).get();
    }

    public static String getDeviceId(Channel channel) {
        return channel.attr(DEVICE_ID).get();
    }

    static String getAccessToken(Channel channel) {
        return channel.attr(ACCESS_TOKEN).get();
    }

    public static void register(Channel channel) {
        UserBaseDTO user = getUser(channel);
        channel.attr(REGISTERED).set(true);
        CHANNELS.compute(user.getId(), (userId, devices) -> {
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
        UserBaseDTO user = channel.attr(USER).getAndSet(null);
        channel.attr(DEVICE_ID).set(null);
        channel.attr(ACCESS_TOKEN).set(null);
        if (user == null || !Boolean.TRUE.equals(channel.attr(REGISTERED).getAndSet(false))) return false;
        CHANNELS.computeIfPresent(user.getId(), (userId, devices) -> {
            devices.remove(channel);
            return devices.isEmpty() ? null : devices;
        });
        return true;
    }

    public static boolean isCurrent(Channel channel, StringRedisTemplate redis) {
        if (!channel.isActive() || !Boolean.TRUE.equals(channel.attr(REGISTERED).get())) return false;
        String deviceId = getDeviceId(channel);
        String accessToken = getAccessToken(channel);
        UserBaseDTO user = getUser(channel);
        if (user == null) return false;
        if (deviceId == null) return Boolean.TRUE.equals(redis.hasKey(LOGIN_USERINFO_ACCESSTOKEN_KEY + accessToken));
        Object current = redis.opsForHash().get(deviceTokenKey(user.getId(), deviceId), "connectionId");
        return channel.id().asLongText().equals(current);
    }
}
