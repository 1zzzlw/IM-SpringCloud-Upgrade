package com.zzzlew.mapper;

import com.zzzlew.domain.entity.Wallet;
import com.zzzlew.domain.vo.WalletRecordVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface WalletMapper {

    /**
     * 查询钱包（加行锁，for update）
     */
    Wallet selectByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * 乐观锁扣款：balance >= amount 时才更新
     */
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 增加余额
     */
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 写钱包流水
     */
    void insertRecord(@Param("userId") Long userId,
                      @Param("amount") BigDecimal amount,
                      @Param("type") int type,
                      @Param("businessId") Long businessId,
                      @Param("before") BigDecimal before,
                      @Param("after") BigDecimal after,
                      @Param("remark") String remark);

    /**
     * 初始化钱包（首次使用时插入）
     */
    void initWallet(@Param("userId") Long userId);

    /**
     * 分页查询流水（按 create_time 降序，type=0 查全部）
     */
    List<WalletRecordVO> selectRecords(
            @Param("userId") Long userId,
            @Param("type") Integer type,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 查询流水总数（type=0 查全部）
     */
    long countRecords(@Param("userId") Long userId, @Param("type") Integer type);
}
