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
        return Result.success(wallet.getBalance());
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


    /**
     * 查询账单流水接口
     * 根据用户ID、分页参数和账单类型查询用户的账单记录
     *
     * @param page     当前页码，默认值为1
     * @param pageSize 每页记录数，默认值为20
     * @param type     账单类型，0表示查询所有类型，其他值表示特定类型
     * @return 返回包含账单记录列表和总数的结果对象
     */
    @Operation(summary = "查询账单流水")
    @GetMapping("/records")
    public Result<PageResult<WalletRecordVO>> getRecords(
            @RequestParam(defaultValue = "1") int page,          // 当前页码，默认从1开始
            @RequestParam(defaultValue = "20") int pageSize,    // 每页显示记录数，默认为20
            @RequestParam(defaultValue = "0") int type) {        // 账单类型，0表示全部
        Long userId = UserHolder.getUser().getId();             // 获取当前登录用户的ID
        int offset = (page - 1) * pageSize;                   // 计算偏移量
        Integer typeParam = type == 0 ? null : type;          // 处理类型参数，0转换为null
        List<WalletRecordVO> records = walletService.listRecords(userId, typeParam, offset, pageSize);  // 查询账单记录
        long total = walletService.countRecords(userId, typeParam);  // 查询记录总数
        records.forEach(r -> r.setTypeDesc(typeDesc(r.getType())));  // 设置类型描述
        return Result.success(new PageResult<>(total, records));      // 返回分页结果
    }


    /**
     * 内部API：用于处理发红包的扣款操作
     * 该接口通过POST请求接收扣款信息，并调用钱包服务完成扣款
     *
     * @param dto 扣款数据传输对象，包含用户ID、金额、业务ID和备注信息
     * @return 返回操作结果，成功时返回成功状态
     */
    @Operation(summary = "发红包扣款（内部）")
    @PostMapping("/deduct")
    public Result<Void> deduct(@RequestBody DeductDTO dto) {
        // 调用钱包服务执行扣款操作
        // 参数包括：用户ID、扣款金额、业务ID和备注信息
        walletService.deduct(dto.getUserId(), dto.getAmount(), dto.getBusinessId(), dto.getRemark());
        // 返回操作成功的结果
        return Result.success();
    }


    /**
     * 初始化钱包的API接口方法
     * 该接口用于为指定用户初始化钱包信息
     *
     * @param userId 用户ID，用于标识需要初始化钱包的用户
     * @return 返回操作结果，成功时返回success状态
     */
    @Operation(summary = "初始化钱包（内部）")  // API接口描述，表示这是一个初始化钱包的内部接口
    @PostMapping("/init")  // HTTP POST请求映射，指定请求路径为"/init"
    public Result<Void> initWallet(@RequestParam Long userId) {  // 方法声明，接收用户ID参数并返回操作结果
        walletService.initWallet(userId);  // 调用钱包服务层的初始化方法
        return Result.success();  // 返回成功结果
    }


    /**
     * 退款接口（内部调用）
     * 该接口用于处理退款请求，需要传入用户ID、退款金额、业务ID和备注信息
     *
     * @param dto 包含退款信息的DTO对象，包含用户ID、退款金额、业务ID和备注
     * @return 返回操作结果，成功时返回success
     */
    @Operation(summary = "退款（内部）")
    @PostMapping("/refund")
    public Result<Void> refund(@RequestBody DeductDTO dto) {
        // 调用钱包服务处理退款逻辑
        // 参数包括：用户ID、退款金额、业务ID、备注信息
        walletService.refund(dto.getUserId(), dto.getAmount(), dto.getBusinessId(), dto.getRemark());
        // 返回成功结果
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
