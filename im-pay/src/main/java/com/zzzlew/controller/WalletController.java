package com.zzzlew.controller;

import com.zzzlew.domain.dto.DeductDTO;
import com.zzzlew.domain.dto.RechargeDTO;
import com.zzzlew.domain.dto.WithdrawDTO;
import com.zzzlew.domain.entity.Wallet;
import com.zzzlew.domain.vo.WalletRecordVO;
import com.zzzlew.result.PageResult;
import com.zzzlew.result.Result;
import com.zzzlew.service.WalletService;
import com.zzzlew.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/wallet")
@Tag(name = "钱包接口")
public class WalletController {

    @Resource
    private WalletService walletService;

    @Operation(summary = "查询钱包信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> getWalletInfo() {
        Long userId = UserHolder.getUser().getId();
        Wallet wallet = walletService.getWallet(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("balance", wallet.getBalance());
        data.put("freezeBalance", wallet.getFreezeBalance());
        return Result.success(data);
    }

    @Operation(summary = "查询余额")
    @GetMapping("/balance")
    public Result<BigDecimal> getBalance() {
        Long userId = UserHolder.getUser().getId();
        Wallet wallet = walletService.getWallet(userId);
        return Result.success(wallet == null ? BigDecimal.ZERO : wallet.getBalance());
    }

    @Operation(summary = "充值")
    @PostMapping("/recharge")
    public Result<Void> recharge(@RequestBody RechargeDTO dto) {
        Long userId = UserHolder.getUser().getId();
        walletService.recharge(userId, dto.getAmount(), dto.getRemark());
        return Result.success();
    }

    @Operation(summary = "提现")
    @PostMapping("/withdraw")
    public Result<Void> withdraw(@RequestBody WithdrawDTO dto) {
        Long userId = UserHolder.getUser().getId();
        walletService.withdraw(userId, dto.getAmount(), dto.getRemark());
        return Result.success();
    }

    @Operation(summary = "查询账单流水")
    @GetMapping("/records")
    public Result<PageResult<WalletRecordVO>> getRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "0") int type) {
        Long userId = UserHolder.getUser().getId();
        int offset = (page - 1) * pageSize;
        Integer typeParam = type == 0 ? null : type;
        List<WalletRecordVO> records = walletService.listRecords(userId, typeParam, offset, pageSize);
        long total = walletService.countRecords(userId, typeParam);
        records.forEach(r -> r.setTypeDesc(typeDesc(r.getType())));
        return Result.success(new PageResult<>(total, records));
    }

    @Operation(summary = "发红包扣款（内部）")
    @PostMapping("/deduct")
    public Result<Void> deduct(@RequestBody DeductDTO dto) {
        walletService.deduct(dto.getUserId(), dto.getAmount(), dto.getBusinessId(), dto.getRemark());
        return Result.success();
    }

    @Operation(summary = "初始化钱包（内部）")
    @PostMapping("/init")
    public Result<Void> initWallet(@RequestParam Long userId) {
        walletService.initWallet(userId);
        return Result.success();
    }

    @Operation(summary = "退款（内部）")
    @PostMapping("/refund")
    public Result<Void> refund(@RequestBody DeductDTO dto) {
        walletService.refund(dto.getUserId(), dto.getAmount(), dto.getBusinessId(), dto.getRemark());
        return Result.success();
    }

    private String typeDesc(Integer type) {
        if (type == null) return "其他";
        return switch (type) {
            case 1 -> "充值";
            case 2 -> "提现";
            case 3 -> "打赏支出";
            case 4 -> "打赏收入";
            case 5 -> "红包支出";
            case 6 -> "红包收入";
            case 7 -> "转账支出";
            case 8 -> "转账收入";
            default -> "其他";
        };
    }
}
