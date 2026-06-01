package com.zzzlew.client;

import com.zzzlew.result.Result;
import com.zzzlew.vo.FriendRelationVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
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

}
