package com.zzzlew.client;

import com.zzzlew.config.DefaultFeignConfig;
import com.zzzlew.domain.dto.DeductDTO;
import com.zzzlew.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * im-pay 服务 Feign 客户端
 */
@FeignClient(value = "im-pay", configuration = DefaultFeignConfig.class)
public interface PayClient {

    /**
     * 发红包扣款（内部调用）
     */
    @PostMapping("/wallet/deduct")
    Result<Void> deduct(@RequestBody DeductDTO dto);

    /**
     * 退款（发红包失败补偿，内部调用）
     */
    @PostMapping("/wallet/refund")
    Result<Void> refund(@RequestBody DeductDTO dto);

    /**
     * 初始化钱包（注册时调用，内部接口）
     */
    @PostMapping("/wallet/init")
    Result<Void> initWallet(@RequestParam("userId") Long userId);
}
