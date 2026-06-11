package com.zzzlew.mapper;

import com.zzzlew.domain.dto.RedPacketDTO;
import com.zzzlew.domain.entity.RedPacket;
import com.zzzlew.domain.entity.RedPacketRecord;
import com.zzzlew.domain.vo.RedPacketVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 红包Mapper
 */
public interface RedPacketMapper {

    /**
     * 创建红包
     */
    void saveRedPacket(RedPacket redPacket);

    /**
     * 查询红包详情
     */
    RedPacket selectRedPacketById(Long id);

    /**
     * 扣减红包（剩余金额和个数）
     * 返回受影响行数：1=成功，0=已领完或不存在
     */
    int deductRedPacket(@Param("redPacketId") Long redPacketId,
                        @Param("amount") BigDecimal amount);

    /**
     * 查询某用户是否已领取该红包
     */
    Integer countGrabRecord(@Param("redPacketId") Long redPacketId,
                            @Param("userId") Long userId);

    /**
     * 插入领取记录
     */
    void saveRedPacketRecord(RedPacketRecord record);

    /**
     * 查询红包领取记录列表
     */
    List<RedPacketRecord> selectRecordsByRedPacketId(Long redPacketId);

    /**
     * 更新红包状态
     */
    void updateRedPacketStatus(@Param("id") Long id,
                               @Param("status") Integer status);
}
