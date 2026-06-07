package com.zzzlew.server.impl;

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSON;
import com.zzzlew.domain.dto.MomentsDTO;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.domain.vo.MomentsVO;
import com.zzzlew.mapper.MomentsMapper;
import com.zzzlew.properties.MinIOConfigProperties;
import com.zzzlew.server.MomentsService;
import com.zzzlew.utils.MinIOFileStorgeUtil;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.zzzlew.constant.RedisConstant.*;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/5 - 06 - 05 - 12:05
 * @Description: com.zzzlew.server.impl
 * @version: 1.0
 */
@Slf4j
@Service
public class MomentsServiceImpl implements MomentsService {

    @Resource
    private MomentsMapper momentsMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MinIOFileStorgeUtil minIOFileStorgeUtil;
    @Resource
    private MinIOConfigProperties minIOConfigProperties;

    /**
     * 上传图片
     *
     * @param images 图片
     * @return 图片url
     */
    @Override
    public List<String> uploadImage(List<MultipartFile> images) {
        Long userId = UserHolder.getUser().getId();
        List<String> urlList = new ArrayList<>();

        // 上传图片到minio中
        for (MultipartFile image : images) {
            String imageName = userId + "/" + image.getOriginalFilename();
            String minioUserFavoritePath = minIOFileStorgeUtil.buildFilePath(imageName);
            log.info("minioUserFavoritePath: {}", minioUserFavoritePath);
            minIOFileStorgeUtil.uploadMomentsImage(minioUserFavoritePath, image);
            String url = minIOConfigProperties.getEndpoint() + "/" + minIOConfigProperties.getMomentsBucket() + "/" + minioUserFavoritePath;
            urlList.add(url);
        }

        return urlList;
    }

    /**
     * 发布朋友圈
     *
     * @param momentsDTO 内容
     */
    @Override
    public void publish(MomentsDTO momentsDTO) {
        UserBaseDTO userBaseDTO = UserHolder.getUser();
        momentsDTO.setUserId(userBaseDTO.getId());
        momentsDTO.setUsername(userBaseDTO.getUsername());
        momentsDTO.setAvatar(userBaseDTO.getAvatar());
        // 插入数据库
        momentsMapper.insert(momentsDTO);
        // 维护Redis中存储最新帖子的排行榜
        long score = momentsDTO.getId();
        String momentsId = String.valueOf(momentsDTO.getId());
        stringRedisTemplate.opsForZSet().add(MOMENTS_LIST_NEW_KEY, momentsId, score);
    }

    /**
     * 查看朋友圈，采用游标分页来查询
     *
     * @param sortWay 排序方式
     * @param lastId  最后一条数据的id
     * @return 朋友圈列表
     */
    @Override
    public List<MomentsVO> list(Integer sortWay, Long lastId) {
        String momentsHotSetKey = MOMENTS_LIST_HOT_KEY;
        List<MomentsVO> resultVO = new ArrayList<>();
        // 固定每次查询的条数
        int pageSize = 20;

        if (sortWay == 0) {
            // 按照最热排序


        } else if (sortWay == 1) {
            double max = (lastId == 0) ? Double.MAX_VALUE : (lastId - 1);
            // 先从 ZSet 拿 ID 列表
            Set<String> idSet = stringRedisTemplate.opsForZSet().reverseRangeByScore(MOMENTS_LIST_NEW_KEY, 0, max, 0, pageSize);
            if (idSet == null || idSet.isEmpty()) {
                // ZSet 没数据，直接去 MySQL 查旧数据
                log.info("ZSet 没数据，直接去 MySQL 查旧数据");
                List<MomentsVO> mysqlData = momentsMapper.list(sortWay, lastId, pageSize);
                if (!mysqlData.isEmpty()) {
                    replenishRedis(mysqlData);
                }
                resultVO.addAll(mysqlData);
                return resultVO;
            }

            List<String> idList = new ArrayList<>(idSet);
            List<String> keys = idList.stream().map(id -> MOMENTS_INFO_LIST_KEY + id).toList();

            // 批量查 String 缓存
            List<String> jsonList = stringRedisTemplate.opsForValue().multiGet(keys);

            List<Long> missingIds = new ArrayList<>();
            Map<Long, MomentsVO> resultMap = new HashMap<>();

            // 找出缺失的帖子
            for (int i = 0; i < idList.size(); i++) {
                Long id = Long.valueOf(idList.get(i));
                String json = jsonList.get(i);
                if (json == null) {
                    missingIds.add(id); // 记录缓存失效的 ID
                } else {
                    resultMap.put(id, JSON.parseObject(json, MomentsVO.class));
                }
            }

            // 精准回填：去数据库查缺失的 ID
            if (!missingIds.isEmpty()) {
                List<MomentsVO> dbMissingData = momentsMapper.selectByIds(missingIds);
                for (MomentsVO vo : dbMissingData) {
                    resultMap.put(vo.getId(), vo);
                    // 顺手回填到 Redis String 缓存，下次就有了
                    stringRedisTemplate.opsForValue().set(MOMENTS_INFO_LIST_KEY + vo.getId(), JSON.toJSONString(vo),
                            MOMENTS_INFO_LIST_KEY_TTL, TimeUnit.SECONDS);
                }
            }

            // 5. 按照 ZSet 的顺序把结果拼好
            for (String id : idList) {
                MomentsVO vo = resultMap.get(Long.valueOf(id));
                if (vo != null) resultVO.add(vo);
            }

            // 判断是否需要查“更旧”的数据
            // 只有当 ZSet 给出的 ID 数量不足 pageSize 时，说明 ZSet 已经见底了
            if (idList.size() < pageSize) {
                int needSize = pageSize - idList.size();
                Long mysqlLastId = idList.isEmpty() ? lastId : Long.valueOf(idList.get(idList.size() - 1));
                List<MomentsVO> olderData = momentsMapper.list(sortWay, mysqlLastId, needSize);
                if (!olderData.isEmpty()) {
                    resultVO.addAll(olderData);
                    replenishRedis(olderData);
                }
            }
        } else {
            log.error("错误的排序类型: {}", sortWay);
        }

        isMomentLiked(resultVO);
        updateCount(resultVO);

        return resultVO;
    }

    /**
     * 补充 Redis 缓存（排行榜、详情、计数器）
     */
    private void replenishRedis(List<MomentsVO> data) {
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (MomentsVO moment : data) {
                String idStr = String.valueOf(moment.getId());
                byte[] idBytes = idStr.getBytes();
                // 1. ZSet 排行榜
                connection.zAdd(MOMENTS_LIST_NEW_KEY.getBytes(), (double) moment.getId(), idBytes);
                // 2. String 详情
                byte[] infoKey = (MOMENTS_INFO_LIST_KEY + idStr).getBytes();
                connection.set(infoKey, JSON.toJSONString(moment).getBytes());
                connection.expire(infoKey, MOMENTS_INFO_LIST_KEY_TTL);
                // 3. Hash 计数器
                byte[] countKey = (MOMENTS_COUNT_KEY + idStr).getBytes();
                connection.hashCommands().hSet(countKey, "like".getBytes(), String.valueOf(moment.getLikeCount()).getBytes());
                connection.hashCommands().hSet(countKey, "comment".getBytes(), String.valueOf(moment.getCommentCount()).getBytes());
                connection.expire(countKey, MOMENTS_COUNT_KEY_TTL);
            }
            return null;
        });
    }


    /**
     * 点赞
     *
     */
    @Override
    public void like(Long momentId) {
        // 获取当前用户ID
        String userId = String.valueOf(UserHolder.getUser().getId());
        String key = MOMENTS_LIKE_KEY + momentId;
        String countKey = MOMENTS_COUNT_KEY + momentId;
        if (BooleanUtil.isFalse(stringRedisTemplate.hasKey(countKey))) {
            // 从数据库查出该 moment 的最新数据
            MomentsVO momentsVO = getById(momentId);
            stringRedisTemplate.opsForHash().putIfAbsent(countKey, "like", String.valueOf(momentsVO.getLikeCount()));
            stringRedisTemplate.opsForHash().putIfAbsent(countKey, "comment", String.valueOf(momentsVO.getCommentCount()));
            // 预热后设置过期时间
            stringRedisTemplate.expire(countKey, MOMENTS_COUNT_KEY_TTL, TimeUnit.SECONDS);
        }

        // 判断当前用户是否已经点赞了
        Boolean isLike = stringRedisTemplate.opsForSet().isMember(key, userId);
        if (BooleanUtil.isFalse(isLike)) {
            // 该用户还没有点赞，数据库加一
            momentsMapper.like(momentId, 1);
            // redis记录该用户
            stringRedisTemplate.opsForSet().add(key, userId);
            // 点赞记录设置过期时间，防止永久堆积
            stringRedisTemplate.expire(key, MOMENTS_LIKE_KEY_TTL, TimeUnit.SECONDS);
            // redis缓存点赞数量
            stringRedisTemplate.opsForHash().increment(countKey, "like", 1);
        } else {
            // 该用户已经点赞了，数据库减一
            momentsMapper.like(momentId, -1);
            // redis移除用户记录
            stringRedisTemplate.opsForSet().remove(key, userId);
            // redis缓存点赞数量
            stringRedisTemplate.opsForHash().increment(countKey, "like", -1);
        }
    }

    /**
     * 根据id查询帖子
     *
     */
    @Override
    public MomentsVO getById(Long momentId) {
        MomentsVO momentsVO = momentsMapper.getById(momentId);
        return momentsVO;
    }

    /**
     * 判断当前用户是否点赞了该帖子
     *
     */
    public void isMomentLiked(List<MomentsVO> momentsVOList) {
        // 获取当前用户ID
        Long userId = UserHolder.getUser().getId();

        List<Object> result = stringRedisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    for (MomentsVO vo : momentsVOList) {
                        String key = MOMENTS_LIKE_KEY + vo.getId();
                        connection.sIsMember(
                                key.getBytes(),
                                userId.toString().getBytes()
                        );
                    }
                    return null;
                });

        for (int i = 0; i < momentsVOList.size(); i++) {
            momentsVOList.get(i).setLiked(Boolean.TRUE.equals(result.get(i)));
        }
    }

    /**
     * 更新点赞数和评论数
     *
     */
    public void updateCount(List<MomentsVO> momentsVOList) {
        List<Object> result = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (MomentsVO vo : momentsVOList) {
                String key = MOMENTS_COUNT_KEY + vo.getId();
                connection.hashCommands().hMGet(
                        key.getBytes(),
                        "like".getBytes(),
                        "comment".getBytes()
                );
            }
            return null;
        });

        for (int i = 0; i < momentsVOList.size(); i++) {
            MomentsVO vo = momentsVOList.get(i);
            List<Object> row = (List<Object>) result.get(i);
            if (row != null) {
                if (row.get(0) != null) {
                    vo.setLikeCount(Integer.parseInt(row.get(0).toString()));
                }
                if (row.get(1) != null) {
                    vo.setCommentCount(Integer.parseInt(row.get(1).toString()));
                }
            }
        }
    }

}
