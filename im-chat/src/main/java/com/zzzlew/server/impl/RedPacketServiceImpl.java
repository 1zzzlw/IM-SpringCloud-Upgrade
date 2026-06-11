package com.zzzlew.server.impl;

import cn.hutool.core.util.IdUtil;
import com.zzzlew.client.AuthClient;
import com.zzzlew.domain.dto.RedPacketDTO;
import com.zzzlew.domain.entity.RedPacket;
import com.zzzlew.domain.entity.RedPacketRecord;
import com.zzzlew.domain.entity.UserAuth;
import com.zzzlew.domain.vo.RedPacketVO;
import com.zzzlew.mapper.RedPacketMapper;
import com.zzzlew.server.RedPacketService;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 红包Service实现
 */
@Slf4j
@Service
public class RedPacketServiceImpl implements RedPacketService {

    @Resource
    private RedPacketMapper redPacketMapper;

    @Resource
    private AuthClient authClient;

    /**
     * 发送红包
     * 已实现：数据库写入，返回红包VO供前端发送WS消息
     */
    @Transactional
    @Override
    public RedPacketVO sendRedPacket(RedPacketDTO redPacketDTO) {
        Long userId = UserHolder.getUser().getId();

        // 生成红包ID（雪花ID，与消息ID一致）
        long redPacketId = IdUtil.getSnowflakeNextId();

        // 构建红包实体
        RedPacket redPacket = new RedPacket();
        redPacket.setId(redPacketId);
        redPacket.setConversationId(redPacketDTO.getConversationId());
        redPacket.setSenderId(userId);
        redPacket.setTotalAmount(redPacketDTO.getTotalAmount());
        redPacket.setTotalCount(redPacketDTO.getTotalCount());
        redPacket.setRemainAmount(redPacketDTO.getTotalAmount());
        redPacket.setRemainCount(redPacketDTO.getTotalCount());
        redPacket.setType(redPacketDTO.getType() != null ? redPacketDTO.getType() : 0);
        redPacket.setGreeting(redPacketDTO.getGreeting());
        redPacket.setStatus(0);

        // 写入数据库
        redPacketMapper.saveRedPacket(redPacket);

        // 构建返回VO
        RedPacketVO vo = new RedPacketVO();
        vo.setId(redPacketId);
        vo.setSenderId(userId);
        vo.setTotalAmount(redPacketDTO.getTotalAmount());
        vo.setTotalCount(redPacketDTO.getTotalCount());
        vo.setType(redPacket.getType());
        vo.setGreeting(redPacketDTO.getGreeting());
        vo.setStatus(0);
        vo.setCreatedAt(redPacket.getCreatedAt());

        return vo;
    }

    /**
     * 抢红包
     * TODO: 需要 Redis 缓存 + 分布式锁的高并发实现
     *
     * 当前为简单实现，生产环境需要：
     * 1. 将红包信息预加载到 Redis（Hash结构：remain_amount, remain_count, total_count）
     * 2. 抢红包时用 Redis Lua 脚本原子扣减（避免超卖）
     * 3. 扣减成功后异步写入 MySQL（MQ 或直接写入）
     * 4. 分布式锁（Redisson）保护并发场景
     *
     * 拼手气红包算法（二倍均值法）：
     *   每次随机金额 = random(0.01, remainAmount / remainCount * 2)
     *   最后一个人领取剩余全部金额
     *
     * 普通红包算法：
     *   每人金额 = totalAmount / totalCount（最后一个补差）
     */
    @Override
    public BigDecimal grabRedPacket(Long redPacketId) {
        Long userId = UserHolder.getUser().getId();

        // 查询红包是否存在
        RedPacket redPacket = redPacketMapper.selectRedPacketById(redPacketId);
        if (redPacket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "红包不存在");
        }

        // 检查红包状态
        if (redPacket.getStatus() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "红包已结束");
        }

        // 检查是否已领取
        Integer count = redPacketMapper.countGrabRecord(redPacketId, userId);
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "你已经领取过了");
        }

        // 检查剩余个数
        if (redPacket.getRemainCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "红包已领完");
        }

        // TODO: 以下扣减逻辑需要 Redis + 分布式锁保护
        // 计算金额
        BigDecimal amount;
        if (redPacket.getType() == 1) {
            // 普通红包：均分
            amount = redPacket.getTotalAmount()
                    .divide(BigDecimal.valueOf(redPacket.getTotalCount()), 2, java.math.RoundingMode.HALF_UP);
            // 最后一个人补差
            if (redPacket.getRemainCount() == 1) {
                amount = redPacket.getRemainAmount();
            }
        } else {
            // 拼手气红包：二倍均值法
            if (redPacket.getRemainCount() == 1) {
                amount = redPacket.getRemainAmount();
            } else {
                // random(0.01, remainAmount / remainCount * 2)
                double max = redPacket.getRemainAmount()
                        .divide(BigDecimal.valueOf(redPacket.getRemainCount()), 2, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(2))
                        .doubleValue();
                double randomAmount = 0.01 + Math.random() * (max - 0.01);
                amount = BigDecimal.valueOf(randomAmount).setScale(2, java.math.RoundingMode.HALF_UP);
                // 确保不超过剩余金额
                if (amount.compareTo(redPacket.getRemainAmount()) > 0) {
                    amount = redPacket.getRemainAmount();
                }
            }
        }

        // 扣减红包
        int updated = redPacketMapper.deductRedPacket(redPacketId, amount);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "红包已被领完，请重试");
        }

        // 写入领取记录
        RedPacketRecord record = new RedPacketRecord();
        record.setRedPacketId(redPacketId);
        record.setUserId(userId);
        record.setAmount(amount);
        redPacketMapper.saveRedPacketRecord(record);

        // 检查是否领完
        RedPacket updatedPacket = redPacketMapper.selectRedPacketById(redPacketId);
        if (updatedPacket != null && updatedPacket.getRemainCount() <= 0) {
            redPacketMapper.updateRedPacketStatus(redPacketId, 1);
        }

        log.info("用户 {} 领取红包 {} 金额 {}", userId, redPacketId, amount);
        return amount;
    }

    /**
     * 查看红包详情（含领取记录）
     */
    @Override
    public RedPacketVO getRedPacketDetail(Long redPacketId) {
        Long userId = UserHolder.getUser().getId();

        RedPacket redPacket = redPacketMapper.selectRedPacketById(redPacketId);
        if (redPacket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "红包不存在");
        }

        RedPacketVO vo = new RedPacketVO();
        vo.setId(redPacket.getId());
        vo.setSenderId(redPacket.getSenderId());
        vo.setTotalAmount(redPacket.getTotalAmount());
        vo.setTotalCount(redPacket.getTotalCount());
        vo.setRemainAmount(redPacket.getRemainAmount());
        vo.setRemainCount(redPacket.getRemainCount());
        vo.setType(redPacket.getType());
        vo.setGreeting(redPacket.getGreeting());
        vo.setStatus(redPacket.getStatus());
        vo.setCreatedAt(redPacket.getCreatedAt());

        // 检查当前用户是否已领取
        Integer count = redPacketMapper.countGrabRecord(redPacketId, userId);
        vo.setGrabbed(count != null && count > 0);

        // 查询领取记录
        List<RedPacketRecord> records = redPacketMapper.selectRecordsByRedPacketId(redPacketId);
        if (records != null && !records.isEmpty()) {
            // 批量查询用户信息
            List<Long> userIds = records.stream().map(RedPacketRecord::getUserId).collect(Collectors.toList());
            List<UserAuth> userAuthList = authClient.getUserListByIds(userIds).getData();
            Map<Long, UserAuth> userMap = userAuthList.stream()
                    .collect(Collectors.toMap(UserAuth::getUserId, u -> u));

            List<RedPacketVO.GrabRecordVO> recordVOs = new ArrayList<>();
            for (RedPacketRecord record : records) {
                RedPacketVO.GrabRecordVO recordVO = new RedPacketVO.GrabRecordVO();
                recordVO.setUserId(record.getUserId());
                recordVO.setAmount(record.getAmount());
                recordVO.setCreatedAt(record.getCreatedAt());
                UserAuth user = userMap.get(record.getUserId());
                if (user != null) {
                    recordVO.setUsername(user.getUsername());
                    recordVO.setAvatar(user.getAvatar());
                }
                recordVOs.add(recordVO);
            }
            vo.setRecords(recordVOs);

            // 设置当前用户领取的金额
            if (vo.getGrabbed()) {
                records.stream()
                        .filter(r -> r.getUserId().equals(userId))
                        .findFirst()
                        .ifPresent(r -> vo.setGrabbedAmount(r.getAmount()));
            }
        } else {
            vo.setRecords(new ArrayList<>());
        }

        return vo;
    }
}
