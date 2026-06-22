package com.zzzlew.server.impl;

import cn.hutool.core.util.IdUtil;
import com.zzzlew.client.AuthClient;
import com.zzzlew.client.PayClient;
import com.zzzlew.domain.dto.DeductDTO;
import com.zzzlew.domain.dto.RedPacketDTO;
import com.zzzlew.domain.entity.RedPacket;
import com.zzzlew.domain.entity.RedPacketRecord;
import com.zzzlew.domain.entity.UserAuth;
import com.zzzlew.domain.vo.RedPacketVO;
import com.zzzlew.mapper.RedPacketMapper;
import com.zzzlew.result.Result;
import com.zzzlew.server.RedPacketService;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zzzlew.constant.RabbitMQConstant.EXCHANGE;
import static com.zzzlew.constant.RabbitMQConstant.ROUTING_KEY_REDPACKET_GRAB;

@Slf4j
@Service
public class RedPacketServiceImpl implements RedPacketService {

    @Resource
    private RedPacketMapper redPacketMapper;
    @Resource
    private AuthClient authClient;
    @Resource
    private PayClient payClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;

    private static final String RED_PACKET_HASH_PREFIX = "red_packet:";
    private static final String RED_PACKET_GRAB_SET_PREFIX = "red_packet:%s:grabbed";
    // 红包 Redis 缓存 24 小时
    private static final long RED_PACKET_TTL = 24 * 60 * 60L;

    private static final DefaultRedisScript<Long> GRAB_SCRIPT;

    static {
        GRAB_SCRIPT = new DefaultRedisScript<>();
        GRAB_SCRIPT.setLocation(new ClassPathResource("lua/grab_red_packet.lua"));
        GRAB_SCRIPT.setResultType(Long.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RedPacketVO sendRedPacket(RedPacketDTO redPacketDTO) {
        Long userId = UserHolder.getUser().getId();

        // 金额校验
        BigDecimal totalAmount = redPacketDTO.getTotalAmount();
        int totalCount = redPacketDTO.getTotalCount();
        if (totalAmount == null || totalAmount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "红包金额不合法");
        }
        BigDecimal perMin = totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.FLOOR);
        if (perMin.compareTo(new BigDecimal("0.01")) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "红包平均金额不能低于0.01元");
        }

        // 生成红包 ID
        long redPacketId = IdUtil.getSnowflakeNextId();

        // 1. 先通过 Feign 扣款（确保余额足够）
        DeductDTO deductDTO = new DeductDTO();
        deductDTO.setUserId(userId);
        deductDTO.setAmount(totalAmount);
        deductDTO.setBusinessId(redPacketId);
        deductDTO.setRemark("发送红包");
        Result<Void> deductResult = payClient.deduct(deductDTO);
        if (deductResult == null || deductResult.getCode() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, deductResult != null ? deductResult.getMsg() : "扣款失败");
        }

        // 2. 写 DB + 预热 Redis（如果失败则退款补偿）
        try {
            // 写 red_packet 表
            RedPacket redPacket = new RedPacket();
            redPacket.setId(redPacketId);
            redPacket.setConversationId(redPacketDTO.getConversationId());
            redPacket.setSenderId(userId);
            redPacket.setTotalAmount(totalAmount);
            redPacket.setTotalCount(totalCount);
            redPacket.setRemainAmount(totalAmount);
            redPacket.setRemainCount(totalCount);
            redPacket.setType(redPacketDTO.getType() != null ? redPacketDTO.getType() : 0);
            redPacket.setGreeting(redPacketDTO.getGreeting());
            redPacket.setStatus(0);
            redPacketMapper.saveRedPacket(redPacket);

            // 预热 Redis Hash（金额存分，避免浮点精度问题）
            String hashKey = RED_PACKET_HASH_PREFIX + redPacketId;
            long totalAmountFen = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();
            Map<String, String> hashData = new HashMap<>();
            hashData.put("remain_count", String.valueOf(totalCount));
            hashData.put("remain_amount", String.valueOf(totalAmountFen));
            hashData.put("total_count", String.valueOf(totalCount));
            hashData.put("total_amount", String.valueOf(totalAmountFen));
            hashData.put("status", "0");
            hashData.put("type", String.valueOf(redPacket.getType()));
            stringRedisTemplate.opsForHash().putAll(hashKey, hashData);
            stringRedisTemplate.expire(hashKey, RED_PACKET_TTL, TimeUnit.SECONDS);

            // 构建返回 VO
            RedPacketVO vo = new RedPacketVO();
            vo.setId(redPacketId);
            vo.setSenderId(userId);
            vo.setTotalAmount(totalAmount);
            vo.setTotalCount(totalCount);
            vo.setType(redPacket.getType());
            vo.setGreeting(redPacketDTO.getGreeting());
            vo.setStatus(0);
            vo.setCreatedAt(redPacket.getCreatedAt());
            return vo;
        } catch (Exception e) {
            // 补偿：DB 或 Redis 写入失败时退款
            log.error("红包创建失败（扣款已成功），尝试退款：userId={}, amount={}, redPacketId={}", userId, totalAmount, redPacketId, e);
            try {
                DeductDTO refundDTO = new DeductDTO();
                refundDTO.setUserId(userId);
                refundDTO.setAmount(totalAmount);
                refundDTO.setBusinessId(redPacketId);
                refundDTO.setRemark("红包创建失败退款");
                Result<Void> refundResult = payClient.refund(refundDTO);
                if (refundResult != null && refundResult.getCode() == 1) {
                    log.info("退款成功：userId={}, amount={}", userId, totalAmount);
                } else {
                    log.error("退款失败！！需人工处理：userId={}, amount={}, redPacketId={}", userId, totalAmount, redPacketId);
                }
            } catch (Exception refundEx) {
                log.error("退款调用异常！！需人工处理：userId={}, amount={}, redPacketId={}", userId, totalAmount, redPacketId, refundEx);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "红包创建失败，已退款");
        }
    }

    @Override
    public BigDecimal grabRedPacket(Long redPacketId) {
        Long userId = UserHolder.getUser().getId();
        String hashKey = RED_PACKET_HASH_PREFIX + redPacketId;
        String grabSetKey = String.format(RED_PACKET_GRAB_SET_PREFIX, redPacketId);

        // 如果 Redis 缓存过期，加分布式锁后从 DB 回灌（防并发覆盖）
        Boolean exists = stringRedisTemplate.hasKey(hashKey);
        if (Boolean.FALSE.equals(exists)) {
            String warmupLockKey = "red_packet:warmup_lock:" + redPacketId;
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(warmupLockKey, "1", 10, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(locked)) {
                try {
                    warmupRedisFromDB(redPacketId, hashKey);
                } finally {
                    stringRedisTemplate.delete(warmupLockKey);
                }
            } else {
                // 其他线程正在回灌，短暂等待后重试
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(hashKey))) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "红包缓存加载中，请稍后重试");
                }
            }
        }

        // 执行 Lua 抢红包（原子）
        Long result = stringRedisTemplate.execute(GRAB_SCRIPT, Arrays.asList(hashKey, grabSetKey),
                String.valueOf(userId), String.valueOf(getTypeFromRedis(hashKey)));

        if (result == null || result == -2L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "红包已领完或不存在");
        }
        if (result == -1L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "你已经领取过了");
        }

        // 金额（分）转元
        BigDecimal amount = BigDecimal.valueOf(result).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // 1. 先通过 MQ 异步到账（Lua 已提交，必须保证到账）
        try {
            Map<String, Object> grabEvent = new HashMap<>();
            grabEvent.put("redPacketId", redPacketId);
            grabEvent.put("userId", userId);
            grabEvent.put("amount", amount.toPlainString());
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_REDPACKET_GRAB, grabEvent);
        } catch (Exception e) {
            // MQ 发送失败：金额已在 Redis 扣减但用户钱包未到账
            // 记录日志并抛异常，由运维补偿或死信队列处理
            log.error("红包MQ到账消息发送失败！需人工补偿：redPacketId={}, userId={}, amount={}", redPacketId, userId, amount, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后重试");
        }

        // 2. 同步写领取记录 + 同步更新 DB remain（让 warmup 安全）
        RedPacketRecord record = new RedPacketRecord();
        record.setRedPacketId(redPacketId);
        record.setUserId(userId);
        record.setAmount(amount);
        redPacketMapper.saveRedPacketRecord(record);
        redPacketMapper.deductRedPacket(redPacketId, amount);

        // 3. 检查是否领完，更新 DB 状态
        String remainCount = (String) stringRedisTemplate.opsForHash().get(hashKey, "remain_count");
        if ("0".equals(remainCount)) {
            redPacketMapper.updateRedPacketStatus(redPacketId, 1);
            stringRedisTemplate.opsForHash().put(hashKey, "status", "1");
        }

        log.info("用户 {} 领取红包 {} 金额 {}元", userId, redPacketId, amount);
        return amount;
    }

    @Override
    public RedPacketVO getRedPacketDetail(Long redPacketId) {
        Long userId = UserHolder.getUser().getId();
        RedPacket redPacket = redPacketMapper.selectRedPacketById(redPacketId);
        if (redPacket == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "红包不存在");

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

        Integer count = redPacketMapper.countGrabRecord(redPacketId, userId);
        vo.setGrabbed(count != null && count > 0);

        List<RedPacketRecord> records = redPacketMapper.selectRecordsByRedPacketId(redPacketId);
        if (records != null && !records.isEmpty()) {
            List<Long> userIds = records.stream().map(RedPacketRecord::getUserId).collect(Collectors.toList());
            List<UserAuth> userAuthList = authClient.getUserListByIds(userIds).getData();
            Map<Long, UserAuth> userMap = userAuthList == null ? new HashMap<>() : userAuthList.stream().collect(Collectors.toMap(UserAuth::getUserId, u -> u));

            List<RedPacketVO.GrabRecordVO> recordVOs = new ArrayList<>();
            for (RedPacketRecord r : records) {
                RedPacketVO.GrabRecordVO recordVO = new RedPacketVO.GrabRecordVO();
                recordVO.setUserId(r.getUserId());
                recordVO.setAmount(r.getAmount());
                recordVO.setCreatedAt(r.getCreatedAt());
                UserAuth user = userMap.get(r.getUserId());
                if (user != null) {
                    recordVO.setUsername(user.getUsername());
                    recordVO.setAvatar(user.getAvatar());
                }
                recordVOs.add(recordVO);
            }
            vo.setRecords(recordVOs);

            if (Boolean.TRUE.equals(vo.getGrabbed())) {
                records.stream().filter(r -> r.getUserId().equals(userId)).findFirst().ifPresent(r -> vo.setGrabbedAmount(r.getAmount()));
            }
        } else {
            vo.setRecords(new ArrayList<>());
        }
        return vo;
    }

    private void warmupRedisFromDB(Long redPacketId, String hashKey) {
        RedPacket rp = redPacketMapper.selectRedPacketById(redPacketId);
        if (rp == null) return;
        long remainFen = rp.getRemainAmount().multiply(BigDecimal.valueOf(100)).longValue();
        long totalFen = rp.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        Map<String, String> data = new HashMap<>();
        data.put("remain_count", String.valueOf(rp.getRemainCount()));
        data.put("remain_amount", String.valueOf(remainFen));
        data.put("total_count", String.valueOf(rp.getTotalCount()));
        data.put("total_amount", String.valueOf(totalFen));
        data.put("status", String.valueOf(rp.getStatus()));
        data.put("type", String.valueOf(rp.getType()));
        stringRedisTemplate.opsForHash().putAll(hashKey, data);
        stringRedisTemplate.expire(hashKey, RED_PACKET_TTL, TimeUnit.SECONDS);
    }

    private int getTypeFromRedis(String hashKey) {
        Object t = stringRedisTemplate.opsForHash().get(hashKey, "type");
        return t != null ? Integer.parseInt(t.toString()) : 0;
    }
}
