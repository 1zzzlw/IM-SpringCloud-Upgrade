package com.zzzlew.client;

import com.zzzlew.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/2 - 06 - 02 - 0:24
 * @Description: com.zzzlew.client
 * @version: 1.0
 */
@FeignClient(value = "im-chat")
public interface ChatClient {
    @PostMapping("/conversation/create")
    Result<Object> createConversation(@RequestParam("conversationId") String conversationId,
                                     @RequestParam("toUserId") Long toUserId,
                                     @RequestParam("fromUserId") String fromUserId,
                                     @RequestParam("type") Integer type);
}
