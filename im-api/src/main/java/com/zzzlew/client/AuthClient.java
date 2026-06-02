package com.zzzlew.client;

import com.zzzlew.domain.entity.UserAuth;
import com.zzzlew.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/2 - 06 - 02 - 16:22
 * @Description: com.zzzlew.client
 * @version: 1.0
 */
@FeignClient(value = "im-auth", path = "/user")
public interface AuthClient {
    @PostMapping("/list/ids")
    Result<List<UserAuth>> getUserListByIds(@RequestBody List<Long> userIds);

}
