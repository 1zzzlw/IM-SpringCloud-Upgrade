package com.zzzlew.domain.request;

import com.zzzlew.domain.Message;

/** 客户端按需请求完整在线好友列表。 */
public class OnlineStatusListRequestDTO extends Message {
    @Override
    public int getMessageType() {
        return OnlineStatusListRequestDTO;
    }
}
