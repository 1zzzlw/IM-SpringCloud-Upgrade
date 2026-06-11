package com.zzzlew.client;

import com.zzzlew.config.DefaultFeignConfig;
import com.zzzlew.domain.dto.GroupMemberDTO;
import com.zzzlew.domain.vo.ConversationVO;
import com.zzzlew.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/2 - 06 - 02 - 0:24
 * @Description: com.zzzlew.client
 * @version: 1.0
 */
@FeignClient(value = "im-chat", configuration = DefaultFeignConfig.class)
public interface ChatClient {
    @PostMapping("/conversation/create")
    Result<Object> createConversation(@RequestParam("conversationId") String conversationId,
                                      @RequestParam("toUserId") Long toUserId,
                                      @RequestParam("fromUserId") String fromUserId,
                                      @RequestParam("type") Integer type);

    @PostMapping("/conversation/inviteFriend")
    Result<Object> inviteFriends(@RequestBody GroupMemberDTO groupMemberDTO);

    @PostMapping("/conversation/internal/updateGroupAvatar")
    Result<Object> updateGroupInfo(@RequestParam("conversationId") String conversationId,
                                   @RequestParam("groupAvatar") String groupAvatar);

    @GetMapping("/conversation/query")
    Result<ConversationVO> queryConversation(@RequestParam("conversationId") String conversationId);
}
