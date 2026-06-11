package com.zzzlew.server;

import com.zzzlew.domain.dto.RedPacketDTO;
import com.zzzlew.domain.vo.RedPacketVO;

/**
 * 红包Service
 */
public interface RedPacketService {

    /**
     * 发送红包
     * 1. 生成红包ID（雪花ID）
     * 2. 写入 red_packet 表
     * 3. 返回红包VO（前端用此ID发送WS消息）
     *
     * @param redPacketDTO 红包信息
     * @return 红包VO（含ID和基本信息）
     */
    RedPacketVO sendRedPacket(RedPacketDTO redPacketDTO);

    /**
     * 抢红包
     * TODO: Server层实现需要 Redis 缓存 + 分布式锁
     * 大致流程：
     * 1. Redis 中检查红包是否存在、是否已领完
     * 2. 分布式锁保护扣减操作
     * 3. 计算金额（拼手气：二倍均值算法；普通：均分）
     * 4. 扣减红包剩余金额/个数
     * 5. 写入领取记录
     * 6. 检查是否领完，更新红包状态
     *
     * @param redPacketId 红包ID
     * @return 领取的金额
     */
    java.math.BigDecimal grabRedPacket(Long redPacketId);

    /**
     * 查看红包详情（含领取记录）
     *
     * @param redPacketId 红包ID
     * @return 红包详情VO
     */
    RedPacketVO getRedPacketDetail(Long redPacketId);
}
