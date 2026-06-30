package com.zzzlew.utils;

import com.zzzlew.domain.dto.UserBaseDTO;
import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/18 - 11 - 18 - 22:16
 * @Description: com.zzzlew.zzzimserver.utils
 * @version: 1.0
 */
public class ChannelManageUtil {

    public static final ConcurrentHashMap<Long, Channel> Online_user = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Channel, UserBaseDTO> Online_channel = new ConcurrentHashMap<>();

    public static void addChannel(UserBaseDTO userBaseDTO, Channel channel) {
        Online_user.put(userBaseDTO.getId(), channel);
        Online_channel.put(channel, userBaseDTO);
    }

    public static UserBaseDTO getUser(Channel channel) {
        return Online_channel.get(channel);
    }

    public static Channel getChannel(Long userId) {
        return Online_user.get(userId);
    }

    public static void removeChannel(Channel channel) {
        UserBaseDTO userBaseDTO = Online_channel.remove(channel);
        if (userBaseDTO != null) {
            Online_user.remove(userBaseDTO.getId());
        }
    }

}
