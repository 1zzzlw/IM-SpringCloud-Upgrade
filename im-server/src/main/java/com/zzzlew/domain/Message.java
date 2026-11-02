package com.zzzlew.domain;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.zzzlew.domain.request.*;
import com.zzzlew.domain.response.*;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/19 - 11 - 19 - 15:46
 * @Description: com.zzzlew.zzzimserver.pojo
 * @version: 1.0
 */
@Data
// 传递消息给MQ之间的序列化注解声明
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, // 用名字标识类型
        include = JsonTypeInfo.As.PROPERTY,  // 把类型当成一个字段
        property = "messageType" // 这个字段名叫：messageType
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HeartRequestDTO.class, name = "0"),
        @JsonSubTypes.Type(value = PrivateChatRequestDTO.class, name = "1"),
        @JsonSubTypes.Type(value = PrivateChatResponseVO.class, name = "2"),
        @JsonSubTypes.Type(value = GroupChatRequestDTO.class, name = "3"),
        @JsonSubTypes.Type(value = GroupChatResponseVO.class, name = "4"),
        @JsonSubTypes.Type(value = FriendApplyRequestDTO.class, name = "5"),
        @JsonSubTypes.Type(value = FriendApplyResponseVO.class, name = "6"),
        @JsonSubTypes.Type(value = GroupApplyRequestDTO.class, name = "7"),
        @JsonSubTypes.Type(value = GroupApplyResponseVO.class, name = "8"),
        @JsonSubTypes.Type(value = LoginSuccessResponseVO.class, name = "9"),
        @JsonSubTypes.Type(value = OnlineStatusListResponseVO.class, name = "10"),
        @JsonSubTypes.Type(value = QuitLoginResponseVO.class, name = "11"),
        @JsonSubTypes.Type(value = SystemMessageRequestDTO.class, name = "12"),
        @JsonSubTypes.Type(value = SystemMessageResponseVO.class, name = "13"),
        @JsonSubTypes.Type(value = FriendApplyDealRequestDTO.class, name = "14"),
        @JsonSubTypes.Type(value = FriendApplyDealResponseVO.class, name = "15"),
        @JsonSubTypes.Type(value = ACKMessageResponseVO.class, name = "16"),
        @JsonSubTypes.Type(value = GroupApplyDealRequestDTO.class, name = "17"),
        @JsonSubTypes.Type(value = GroupApplyDealResponseVO.class, name = "18"),
        @JsonSubTypes.Type(value = OnlineStatusListRequestDTO.class, name = "19"),
})
public abstract class Message implements Serializable {
    private static final Map<Integer, Class<? extends Message>> messageClasses = new HashMap<>();

    private int sequenceId;

    private int messageType;

    public abstract int getMessageType();

    public static final int HeartRequestDTO = 0;

    public static final int PrivateChatRequestDTO = 1;
    public static final int PrivateChatResponseVO = 2;

    public static final int GroupChatRequestDTO = 3;
    public static final int GroupChatResponseVO = 4;

    public static final int FriendApplyRequestDTO = 5;
    public static final int FriendApplyResponseVO = 6;

    public static final int GroupApplyRequestDTO = 7;
    public static final int GroupApplyResponseVO = 8;

    public static final int LoginSuccessResponseVO = 9;

    public static final int OnlineStatusListResponseVO = 10;
    public static final int QuitLoginResponseVO = 11;

    public static final int SystemMessageRequestDTO = 12;
    public static final int SystemMessageResponseVO = 13;

    public static final int FriendApplyDealRequestDTO = 14;
    public static final int FriendApplyDealResponseVO = 15;

    public static final int ACKMessageResponseVO = 16;

    public static final int GroupApplyDealRequestDTO = 17;
    public static final int GroupApplyDealResponseVO = 18;
    public static final int OnlineStatusListRequestDTO = 19;

    /**
     * 根据消息类型字节，获得对应的消息 class
     *
     * @param messageType 消息类型字节
     * @return 消息 class
     */
    public static Class<? extends Message> getMessageClass(int messageType) {
        return messageClasses.get(messageType);
    }

    static {
        messageClasses.put(HeartRequestDTO, HeartRequestDTO.class);
        messageClasses.put(PrivateChatRequestDTO, PrivateChatRequestDTO.class);
        messageClasses.put(PrivateChatResponseVO, PrivateChatResponseVO.class);
        messageClasses.put(GroupChatRequestDTO, GroupChatRequestDTO.class);
        messageClasses.put(GroupChatResponseVO, GroupChatResponseVO.class);
        messageClasses.put(FriendApplyRequestDTO, FriendApplyRequestDTO.class);
        messageClasses.put(FriendApplyResponseVO, FriendApplyResponseVO.class);
        messageClasses.put(GroupApplyRequestDTO, GroupApplyRequestDTO.class);
        messageClasses.put(GroupApplyResponseVO, GroupApplyResponseVO.class);
        messageClasses.put(LoginSuccessResponseVO, LoginSuccessResponseVO.class);
        messageClasses.put(OnlineStatusListResponseVO, OnlineStatusListResponseVO.class);
        messageClasses.put(SystemMessageRequestDTO, SystemMessageRequestDTO.class);
        messageClasses.put(SystemMessageResponseVO, SystemMessageResponseVO.class);
        messageClasses.put(FriendApplyDealRequestDTO, FriendApplyDealRequestDTO.class);
        messageClasses.put(FriendApplyDealResponseVO, FriendApplyDealResponseVO.class);
        messageClasses.put(ACKMessageResponseVO, ACKMessageResponseVO.class);
        messageClasses.put(GroupApplyDealRequestDTO, GroupApplyDealRequestDTO.class);
        messageClasses.put(GroupApplyDealResponseVO, GroupApplyDealResponseVO.class);
        messageClasses.put(OnlineStatusListRequestDTO, OnlineStatusListRequestDTO.class);
    }
}
