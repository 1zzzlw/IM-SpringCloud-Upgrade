package com.zzzlew.client;

import com.zzzlew.domain.dto.GroupApplyDTO;
import com.zzzlew.result.Result;
import com.zzzlew.domain.vo.FriendRelationVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/2 - 06 - 02 - 0:09
 * @Description: com.zzzlew.client
 * @version: 1.0
 */
@FeignClient("im-social")
public interface SocialClient {

    @GetMapping("/friend/init/list")
    Result<List<FriendRelationVO>> initFriendList(@RequestParam("isInit") Boolean isInit);

    @PostMapping("/apply/groupApply")
    Result<Object> sendGroupApply(@RequestParam("userId") Long userId,
                                  @RequestParam("friendIdList") List<Long> friendIdList,
                                  @RequestBody GroupApplyDTO groupApplyDTO);

}
