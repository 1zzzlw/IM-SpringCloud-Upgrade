package com.zzzlew.server.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zzzlew.domain.dto.MomentCommentsDTO;
import com.zzzlew.domain.dto.MomentCommentsPageQueryDTO;
import com.zzzlew.domain.dto.MomentsDTO;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.domain.vo.MomentsCommentsVO;
import com.zzzlew.domain.vo.MomentsVO;
import com.zzzlew.mapper.MomentsMapper;
import com.zzzlew.properties.MinIOConfigProperties;
import com.zzzlew.result.PageResult;
import com.zzzlew.server.MomentsService;
import com.zzzlew.utils.MinIOFileStorgeUtil;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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
    @Resource
    private DefaultRedisScript<Long> momentsLikeScript;
    @Resource
    private DefaultRedisScript<Long> momentsWarmupCacheScript;
    @Resource
    private DefaultRedisScript<Long> momentsCommentLikeScript;
    @Resource
    private DefaultRedisScript<Long> momentsCommentCountIncrScript;

    // ZSet 最新帖子列表最大保留条数
    private static final long MOMENTS_LIST_NEW_MAX_SIZE = 2000L;

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
        // 只保留最新的 N 条，超出的旧帖子降级到 DB 查询
        Long size = stringRedisTemplate.opsForZSet().zCard(MOMENTS_LIST_NEW_KEY);
        if (size != null && size > MOMENTS_LIST_NEW_MAX_SIZE) {
            stringRedisTemplate.opsForZSet().removeRange(MOMENTS_LIST_NEW_KEY, 0, size - MOMENTS_LIST_NEW_MAX_SIZE - 1);
        }
    }

    /**
     * 查看朋友圈，采用游标分页来查询
     *
     * @param lastId 最后一条数据的id
     * @return 朋友圈列表
     */
    @Override
    public List<MomentsVO> listByNew(Long lastId) {
        List<MomentsVO> resultVO = new ArrayList<>();
        // 固定每次查询的条数
        int pageSize = 20;
        double max = (lastId == 0) ? Double.MAX_VALUE : (lastId - 1);
        // 先从 ZSet 拿 ID 列表
        Set<String> idSet = stringRedisTemplate.opsForZSet().reverseRangeByScore(MOMENTS_LIST_NEW_KEY, 0, max, 0, pageSize);
        // 第一种情况，redis里面本身就是空的，可能发生在刚启动的时候
        if (idSet == null || idSet.isEmpty()) {
            // ZSet 没数据，直接去 MySQL 查旧数据
            log.info("ZSet 没数据，直接去 MySQL 查旧数据");
            List<MomentsVO> mysqlData = momentsMapper.listByNew(lastId, pageSize);
            if (!mysqlData.isEmpty()) {
                replenishRedis(mysqlData);
            }
            resultVO.addAll(mysqlData);
            // 直接返回就好了，这里相当于给刚启动的redis实例进行了缓存预热，后续再次查询就会快很多
            return resultVO;
        }

        // 如果 idSet 集合有值，就说明 redis 里面已经预热过了，但是还有可能出现预热不完全的情况，也就是说还有旧数据在mysql里，所以需要去查
        List<String> idList = new ArrayList<>(idSet);
        // 帖子详细信息的缓存 key 集合
        List<String> keys = idList.stream().map(id -> MOMENTS_INFO_LIST_KEY + id).toList();

        // 批量查 String 缓存，拿到帖子的详细信息
        List<String> jsonList = stringRedisTemplate.opsForValue().multiGet(keys);
        if (jsonList == null) {
            log.warn("multiGet 返回 null");
            jsonList = Collections.emptyList();
        }

        // 找出缺失的帖子，有的帖子详细信息可能会过期，每个帖子的添加时间不一样，所以需要找到缺失的帖子
        List<Long> missingIds = new ArrayList<>();
        Map<Long, MomentsVO> resultMap = new HashMap<>();

        // 找出缺失的帖子
        for (int i = 0; i < idList.size(); i++) {
            Long id = Long.valueOf(idList.get(i));
            String json = (i < jsonList.size()) ? jsonList.get(i) : null;

            if (json == null) {
                missingIds.add(id); // 记录缓存失效的 ID
            } else {
                try {
                    resultMap.put(id, JSON.parseObject(json, MomentsVO.class));
                } catch (Exception e) {
                    log.error("解析 JSON 失败，momentId: {}, json: {}", id, json, e);
                    missingIds.add(id);
                }
            }
        }

        // 精准回填：去数据库查缺失的 ID
        if (!missingIds.isEmpty()) {
            List<MomentsVO> dbMissingData = momentsMapper.selectByIds(missingIds);
            for (MomentsVO vo : dbMissingData) {
                resultMap.put(vo.getId(), vo);
                // 顺手回填到 Redis String 缓存，添加随机 TTL
                long ttl = calculateRandomTTL(MOMENTS_INFO_LIST_KEY_TTL);
                stringRedisTemplate.opsForValue().set(MOMENTS_INFO_LIST_KEY + vo.getId(), JSON.toJSONString(vo), ttl, TimeUnit.SECONDS);
            }
        }

        // 按照 ZSet 的顺序把结果拼好
        for (String id : idList) {
            MomentsVO vo = resultMap.get(Long.valueOf(id));
            if (vo != null) resultVO.add(vo);
        }

        // 判断是否需要查“更旧”的数据
        // 只有当 ZSet 给出的 ID 数量不足 pageSize 时，说明 ZSet 已经见底了
        if (idList.size() < pageSize) {
            int needSize = pageSize - idList.size();
            Long mysqlLastId = idList.isEmpty() ? lastId : Long.valueOf(idList.get(idList.size() - 1));
            List<MomentsVO> olderData = momentsMapper.listByNew(mysqlLastId, needSize);
            if (!olderData.isEmpty()) {
                resultVO.addAll(olderData);
                replenishRedis(olderData);
            }
        }

        isMomentLiked(resultVO);
        updateCount(resultVO);

        return resultVO;
    }

    /**
     * 按热度查询朋友圈，使用 Redis ZSet 热度排行榜
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Override
    public PageResult<MomentsVO> listByHot(int page, int pageSize) {
        List<MomentsVO> resultList = new ArrayList<>();

        try {
            // 计算 ZSet 的偏移量
            int offset = (page - 1) * pageSize;

            // 从热度排行榜 ZSet 中获取帖子 ID，按分数降序
            Set<String> idSet = stringRedisTemplate.opsForZSet()
                    .reverseRange(MOMENTS_LIST_HOT_KEY, offset, offset + pageSize - 1);

            // 如果 Redis 没数据，降级到数据库查询
            if (idSet == null || idSet.isEmpty()) {
                log.info("热度排行榜为空，降级查询数据库");
                PageHelper.startPage(page, pageSize);
                Page<MomentsVO> pageData = momentsMapper.listByHot();

                // 预热热度排行榜
                if (!pageData.isEmpty()) {
                    replenishHotRank(pageData.getResult());
                }

                isMomentLiked(pageData.getResult());
                updateCount(pageData.getResult());

                return new PageResult<>(pageData.getTotal(), pageData.getResult());
            }

            // 从 Redis 批量查询帖子详情
            List<String> idList = new ArrayList<>(idSet);
            List<String> keys = idList.stream()
                    .map(id -> MOMENTS_INFO_LIST_KEY + id)
                    .toList();

            List<String> jsonList = stringRedisTemplate.opsForValue().multiGet(keys);
            if (jsonList == null) {
                log.warn("multiGet 返回 null");
                jsonList = Collections.emptyList();
            }

            // 找出缓存失效的帖子
            List<Long> missingIds = new ArrayList<>();
            Map<Long, MomentsVO> resultMap = new HashMap<>();

            for (int i = 0; i < idList.size(); i++) {
                Long id = Long.valueOf(idList.get(i));
                String json = (i < jsonList.size()) ? jsonList.get(i) : null;

                if (json == null) {
                    missingIds.add(id);
                } else {
                    try {
                        resultMap.put(id, JSON.parseObject(json, MomentsVO.class));
                    } catch (Exception e) {
                        log.error("解析 JSON 失败，momentId: {}, json: {}", id, json, e);
                        missingIds.add(id);
                    }
                }
            }

            // 从数据库查询缺失的帖子
            if (!missingIds.isEmpty()) {
                List<MomentsVO> dbMissingData = momentsMapper.selectByIds(missingIds);
                for (MomentsVO vo : dbMissingData) {
                    resultMap.put(vo.getId(), vo);
                    // 回写到 Redis
                    long ttl = calculateRandomTTL(MOMENTS_INFO_LIST_KEY_TTL);
                    stringRedisTemplate.opsForValue().set(
                            MOMENTS_INFO_LIST_KEY + vo.getId(),
                            JSON.toJSONString(vo),
                            ttl,
                            TimeUnit.SECONDS
                    );
                }
            }

            // 按照 ZSet 的顺序组装结果
            for (String id : idList) {
                MomentsVO vo = resultMap.get(Long.valueOf(id));
                if (vo != null) {
                    resultList.add(vo);
                }
            }

            // 填充点赞状态和计数器
            isMomentLiked(resultList);
            updateCount(resultList);

            // 获取总数，ZSet 的大小
            Long total = stringRedisTemplate.opsForZSet().zCard(MOMENTS_LIST_HOT_KEY);

            return new PageResult<>(total != null ? total : 0L, resultList);

        } catch (Exception e) {
            log.error("查询热门帖子失败，page: {}, pageSize: {}", page, pageSize, e);
            // 降级到数据库
            PageHelper.startPage(page, pageSize);
            Page<MomentsVO> pageData = momentsMapper.listByHot();
            isMomentLiked(pageData.getResult());
            updateCount(pageData.getResult());
            return new PageResult<>(pageData.getTotal(), pageData.getResult());
        }
    }

    /**
     * 预热热度排行榜（用于 Redis 为空时的初始化）
     */
    private void replenishHotRank(List<MomentsVO> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            for (MomentsVO moment : data) {
                String momentId = String.valueOf(moment.getId());
                double score = moment.getLikeCount(); // 使用点赞数作为热度分数
                stringRedisTemplate.opsForZSet().add(MOMENTS_LIST_HOT_KEY, momentId, score);
            }
            log.info("预热热度排行榜成功，数据量: {}", data.size());
        } catch (Exception e) {
            log.error("预热热度排行榜失败", e);
        }
    }

    /**
     * 补充 Redis 缓存，排行榜、详情、计数器
     * 添加 TTL 随机偏移防止缓存雪崩，使用 SETEX 代替 SET+EXPIRE
     */
    private void replenishRedis(List<MomentsVO> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (MomentsVO moment : data) {
                    String idStr = String.valueOf(moment.getId());
                    byte[] idBytes = idStr.getBytes();

                    // 计算随机 TTL
                    long infoTTL = calculateRandomTTL(MOMENTS_INFO_LIST_KEY_TTL);
                    long countTTL = calculateRandomTTL(MOMENTS_COUNT_KEY_TTL);

                    // ZSet 最新排行榜
                    connection.zAdd(MOMENTS_LIST_NEW_KEY.getBytes(), (double) moment.getId(), idBytes);

                    // String 详情，使用 SETEX 原子性设置值和过期时间
                    byte[] infoKey = (MOMENTS_INFO_LIST_KEY + idStr).getBytes();
                    connection.setEx(infoKey, infoTTL, JSON.toJSONString(moment).getBytes());

                    // Hash 计数器，使用 HSET + EXPIRE
                    byte[] countKey = (MOMENTS_COUNT_KEY + idStr).getBytes();
                    connection.hashCommands().hSet(countKey, "like".getBytes(), String.valueOf(moment.getLikeCount()).getBytes());
                    connection.hashCommands().hSet(countKey, "comment".getBytes(), String.valueOf(moment.getCommentCount()).getBytes());
                    connection.expire(countKey, countTTL);
                }
                return null;
            });

            log.info("批量回填缓存成功，数据量: {}", data.size());

        } catch (Exception e) {
            log.error("批量回填缓存失败", e);
        }
    }

    /**
     * 计算随机 TTL，添加 ±10% 的随机偏移，防止缓存雪崩
     *
     * @param baseTTL 基础 TTL（秒）
     * @return 随机 TTL（秒）
     */
    private long calculateRandomTTL(Long baseTTL) {
        if (baseTTL == null || baseTTL <= 0) {
            return 3600L; // 默认 1 小时
        }
        // 添加 ±10% 的随机偏移
        long offset = (long) (baseTTL * 0.1 * ThreadLocalRandom.current().nextDouble(-1, 1));
        return baseTTL + offset;
    }


    /**
     * 点赞/取消点赞，需要使用 Lua 脚本保证原子性
     *
     * @param momentId 帖子ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(Long momentId) {
        if (momentId == null || momentId <= 0) {
            log.warn("无效的 momentId: {}", momentId);
            return;
        }

        Long userId = UserHolder.getUser().getId();
        // 点赞列表
        String likeSetKey = MOMENTS_LIKE_KEY + momentId;
        // 点赞和评论的计数器
        String countKey = MOMENTS_COUNT_KEY + momentId;
        // 热度排行榜
        String hotRankKey = MOMENTS_LIST_HOT_KEY;

        try {
            // 执行 Lua 脚本，保证 Redis 操作的原子性
            // 点赞Set不设置过期时间，保证点赞关系永久准确
            Long result = stringRedisTemplate.execute(
                    momentsLikeScript,
                    Arrays.asList(likeSetKey, countKey, hotRankKey),
                    String.valueOf(userId),
                    String.valueOf(momentId),
                    String.valueOf(MOMENTS_COUNT_KEY_TTL)
            );

            if (result == null) {
                log.error("Lua 脚本执行失败，momentId: {}, userId: {}", momentId, userId);
                return;
            }

            // 处理 Lua 脚本返回结果
            if (result == -1) {
                // 缓存未预热，需要从数据库加载数据并预热缓存
                warmupCacheAndRetry(momentId, userId);
            } else if (result == 1) {
                // 点赞成功，同步更新数据库
                momentsMapper.like(momentId, 1);
                log.info("用户 {} 点赞帖子 {} 成功", userId, momentId);
            } else if (result == 0) {
                // 取消点赞成功，同步更新数据库
                momentsMapper.like(momentId, -1);
                log.info("用户 {} 取消点赞帖子 {} 成功", userId, momentId);
            }

        } catch (Exception e) {
            log.error("点赞操作失败，momentId: {}, userId: {}", momentId, userId, e);
            // 事务回滚会自动处理
            throw new RuntimeException("点赞操作失败", e);
        }
    }

    /**
     * 原子性更新 Redis 评论计数，key 不存在时先从 DB 预热再重试
     */
    private void updateCommentCountInRedis(Long momentId, String countKey, int delta) {
        Long result = stringRedisTemplate.execute(
                momentsCommentCountIncrScript,
                Collections.singletonList(countKey),
                String.valueOf(delta)
        );

        if (result != null && result == -1L) {
            // key 不存在，先预热再重试一次
            MomentsVO moment = momentsMapper.getById(momentId);
            if (moment == null) return;

            long ttl = calculateRandomTTL(MOMENTS_COUNT_KEY_TTL);
            Long warmupResult = stringRedisTemplate.execute(
                    momentsWarmupCacheScript,
                    Collections.singletonList(countKey),
                    String.valueOf(moment.getLikeCount()),
                    String.valueOf(moment.getCommentCount()),
                    String.valueOf(ttl)
            );
            if (warmupResult != null && warmupResult >= 0) {
                // 预热成功后再自增
                stringRedisTemplate.execute(
                        momentsCommentCountIncrScript,
                        Collections.singletonList(countKey),
                        String.valueOf(delta)
                );
            }
        }
    }

    /**
     * 缓存预热并重试点赞操作
     */
    private void warmupCacheAndRetry(Long momentId, Long userId) {
        try {
            // 从数据库查询帖子信息
            MomentsVO momentsVO = momentsMapper.getById(momentId);
            if (momentsVO == null) {
                log.warn("帖子不存在，momentId: {}", momentId);
                return;
            }

            // 使用 Lua 脚本原子性地预热缓存
            String countKey = MOMENTS_COUNT_KEY + momentId;
            Long warmupResult = stringRedisTemplate.execute(
                    momentsWarmupCacheScript,
                    Collections.singletonList(countKey),
                    String.valueOf(momentsVO.getLikeCount()),
                    String.valueOf(momentsVO.getCommentCount()),
                    String.valueOf(MOMENTS_COUNT_KEY_TTL)
            );

            if (warmupResult != null && warmupResult == 1) {
                log.info("缓存预热成功，momentId: {}", momentId);
            }

            // 重试点赞操作（递归调用，但只会执行一次，因为缓存已预热）
            like(momentId);

        } catch (Exception e) {
            log.error("缓存预热失败，momentId: {}", momentId, e);
            throw new RuntimeException("缓存预热失败", e);
        }
    }

    /**
     * 根据id查询帖子（先查缓存，再查数据库，并刷新实时计数）
     *
     * @param momentId 帖子ID
     * @return 帖子信息
     */
    @Override
    public MomentsVO getById(Long momentId) {
        if (momentId == null || momentId <= 0) {
            log.warn("无效的 momentId: {}", momentId);
            return null;
        }

        try {
            // 先从 Redis String 缓存查询
            String cacheKey = MOMENTS_INFO_LIST_KEY + momentId;
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);

            MomentsVO momentsVO;

            if (cachedJson != null) {
                momentsVO = JSON.parseObject(cachedJson, MomentsVO.class);
            } else {
                // 缓存未命中，查询数据库
                momentsVO = momentsMapper.getById(momentId);

                if (momentsVO != null) {
                    // 回写缓存，添加随机 TTL 防止雪崩
                    long ttl = calculateRandomTTL(MOMENTS_INFO_LIST_KEY_TTL);
                    stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(momentsVO), ttl, TimeUnit.SECONDS);
                }
            }

            if (momentsVO != null) {
                // 刷新实时计数器（点赞数、评论数）
                updateCount(Collections.singletonList(momentsVO));
                // 刷新当前用户的点赞状态
                isMomentLiked(Collections.singletonList(momentsVO));
            }

            return momentsVO;

        } catch (Exception e) {
            log.error("查询帖子失败，momentId: {}", momentId, e);
            // 降级：直接查数据库
            return momentsMapper.getById(momentId);
        }
    }

    /**
     * 发布评论
     *
     * @param momentCommentsDTO 评论内容
     * @return 评论信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MomentsCommentsVO publishComment(MomentCommentsDTO momentCommentsDTO) {
        UserBaseDTO user = UserHolder.getUser();
        Long momentId = momentCommentsDTO.getMomentId();

        // 构建评论对象
        MomentsCommentsVO momentsCommentsVO = new MomentsCommentsVO();
        momentsCommentsVO.setUserId(user.getId());
        momentsCommentsVO.setUsername(user.getUsername());
        momentsCommentsVO.setAvatar(user.getAvatar());
        momentsCommentsVO.setContent(momentCommentsDTO.getContent());
        momentsCommentsVO.setMomentId(momentId);

        // 插入评论到数据库
        momentsMapper.publishComment(momentsCommentsVO);

        // 更新数据库中的评论计数
        momentsMapper.updateCommentCount(momentId, 1);

        // 更新 Redis 中的评论计数器（原子性，避免 check-then-act 竞态）
        String countKey = MOMENTS_COUNT_KEY + momentId;
        try {
            updateCommentCountInRedis(momentId, countKey, 1);
        } catch (Exception e) {
            log.error("更新评论计数器失败，momentId: {}", momentId, e);
            // 不抛异常，数据库已更新即可
        }

        return momentsCommentsVO;
    }

    @Override
    public PageResult<MomentsCommentsVO> comments(MomentCommentsPageQueryDTO queryDTO) {
        // 设置分页参数
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        // 查询数据
        Page<MomentsCommentsVO> pageResult = momentsMapper.comments(queryDTO);
        // 封装分页结果
        long total = pageResult.getTotal();
        List<MomentsCommentsVO> result = pageResult.getResult();
        // 填充点赞状态
        fillCommentLikeStatus(result);
        return new PageResult<>(total, result);
    }

    /**
     * 查看评论的下级回复列表
     *
     * @param commentId 评论ID
     * @param page      页码
     * @param pageSize  每页大小
     * @return 回复列表
     */
    @Override
    public PageResult<MomentsCommentsVO> commentReplies(Long commentId, int page, int pageSize) {
        // 设置分页参数
        PageHelper.startPage(page, pageSize);
        // 查询下级回复
        Page<MomentsCommentsVO> pageResult = momentsMapper.commentReplies(commentId);
        // 封装分页结果
        long total = pageResult.getTotal();
        List<MomentsCommentsVO> result = pageResult.getResult();
        // 填充点赞状态
        fillCommentLikeStatus(result);
        return new PageResult<>(total, result);
    }

    /**
     * 填充评论点赞状态
     */
    private void fillCommentLikeStatus(List<MomentsCommentsVO> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        try {
            Long userId = UserHolder.getUser().getId();
            if (userId == null) {
                log.warn("用户ID为空，无法判断评论点赞状态");
                return;
            }

            // 批量查询 Redis
            List<Object> result = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (MomentsCommentsVO comment : comments) {
                    String key = MOMENTS_COMMENT_LIKE_KEY + comment.getId();
                    connection.sIsMember(key.getBytes(), userId.toString().getBytes());
                }
                return null;
            });

            if (result != null && result.size() == comments.size()) {
                for (int i = 0; i < comments.size(); i++) {
                    comments.get(i).setLiked(Boolean.TRUE.equals(result.get(i)));
                }
            }
        } catch (Exception e) {
            log.error("判断评论点赞状态失败", e);
            // 异常时默认设置为未点赞
            comments.forEach(comment -> comment.setLiked(false));
        }
    }

    /**
     * 发布评论下的回复
     *
     * @param momentCommentsDTO 回复内容
     * @return 回复信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MomentsCommentsVO publishCommentReply(MomentCommentsDTO momentCommentsDTO) {
        UserBaseDTO user = UserHolder.getUser();

        // 构建回复对象
        MomentsCommentsVO reply = new MomentsCommentsVO();
        reply.setUserId(user.getId());
        reply.setUsername(user.getUsername());
        reply.setAvatar(user.getAvatar());
        reply.setContent(momentCommentsDTO.getContent());
        reply.setMomentId(momentCommentsDTO.getMomentId());
        reply.setParentId(momentCommentsDTO.getParentId());
        reply.setReplyToUserId(momentCommentsDTO.getReplyToUserId());
        reply.setReplyToUsername(momentCommentsDTO.getReplyToUsername());

        // 插入回复到数据库
        momentsMapper.publishCommentReply(reply);

        // 更新数据库中的评论计数
        Long momentId = momentCommentsDTO.getMomentId();
        momentsMapper.updateCommentCount(momentId, 1);

        // 更新 Redis 中的评论计数器（原子性，避免 check-then-act 竞态）
        String countKey = MOMENTS_COUNT_KEY + momentId;
        try {
            updateCommentCountInRedis(momentId, countKey, 1);
        } catch (Exception e) {
            log.error("更新评论计数器失败，momentId: {}", momentId, e);
        }

        return reply;
    }

    /**
     * 点赞评论
     *
     * @param commentId 评论ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId) {
        if (commentId == null || commentId <= 0) {
            log.warn("无效的 commentId: {}", commentId);
            return;
        }

        Long userId = UserHolder.getUser().getId();
        String likeSetKey = MOMENTS_COMMENT_LIKE_KEY + commentId;

        try {
            Long result = stringRedisTemplate.execute(
                    momentsCommentLikeScript,
                    Collections.singletonList(likeSetKey),
                    userId.toString()
            );

            if (result == null) {
                log.error("评论点赞 Lua 脚本执行失败，commentId: {}, userId: {}", commentId, userId);
                return;
            }

            if (result == 1) {
                momentsMapper.likeComment(commentId, 1);
                log.info("用户 {} 点赞评论 {} 成功", userId, commentId);
            } else {
                momentsMapper.likeComment(commentId, -1);
                log.info("用户 {} 取消点赞评论 {} 成功", userId, commentId);
            }
        } catch (Exception e) {
            log.error("点赞评论操作失败，commentId: {}, userId: {}", commentId, userId, e);
            throw new RuntimeException("点赞评论操作失败", e);
        }
    }

    @Override
    public PageResult<MomentsVO> queryUserMoments(int page, int pageSize) {
        Long userId = UserHolder.getUser().getId();
        PageHelper.startPage(page, pageSize);
        Page<MomentsVO> pageResult = momentsMapper.queryUserMoments(userId);
        return new PageResult<>(pageResult.getTotal(), pageResult.getResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long momentId) {
        if (momentId == null || momentId <= 0) {
            log.warn("无效的 momentId: {}", momentId);
            return;
        }
        // 删除数据库
        momentsMapper.delete(momentId);

        // 直接清理所有相关 Redis 缓存，DEL 本身是原子的，无需 Lua 脚本
        try {
            String idStr = String.valueOf(momentId);
            stringRedisTemplate.delete(Arrays.asList(
                    MOMENTS_INFO_LIST_KEY + idStr,
                    MOMENTS_COUNT_KEY + idStr,
                    MOMENTS_LIKE_KEY + idStr
            ));
            stringRedisTemplate.opsForZSet().remove(MOMENTS_LIST_NEW_KEY, idStr);
            stringRedisTemplate.opsForZSet().remove(MOMENTS_LIST_HOT_KEY, idStr);
            log.info("删除帖子 {} 的 Redis 缓存完成", momentId);
        } catch (Exception e) {
            log.error("删除帖子 {} 的 Redis 缓存失败", momentId, e);
        }
    }

    /**
     * 更新帖子内容，缓存失效由 Canal 监听 binlog 异步处理
     */
    @Override
    public void update(MomentsDTO momentsDTO) {
        momentsMapper.update(momentsDTO);
    }

    @Override
    public void reward(Long momentId, Integer count) {
        // 打赏者身份与余额校验,确认登录用户,查询钱包余额是否足够

        // 打赏者钱包扣减

        // 异步操作
        // 被打赏者钱包余额增加

        // 生成打赏凭证,也就是生成记录到数据库
    }

    /**
     * 判断当前用户是否点赞了该帖子，添加 NPE 防护 + 数据库回源
     */
    public void isMomentLiked(List<MomentsVO> momentsVOList) {
        if (momentsVOList == null || momentsVOList.isEmpty()) {
            return;
        }

        try {
            Long userId = UserHolder.getUser().getId();
            if (userId == null) {
                log.warn("用户ID为空，无法判断点赞状态");
                return;
            }

            // 批量查询 Redis
            List<Object> result = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (MomentsVO vo : momentsVOList) {
                    String key = MOMENTS_LIKE_KEY + vo.getId();
                    connection.sIsMember(key.getBytes(), userId.toString().getBytes());
                }
                return null;
            });

            if (result != null && result.size() == momentsVOList.size()) {
                for (int i = 0; i < momentsVOList.size(); i++) {
                    momentsVOList.get(i).setLiked(Boolean.TRUE.equals(result.get(i)));
                }
            }
        } catch (Exception e) {
            log.error("判断点赞状态失败", e);
            // 异常时默认设置为未点赞
            momentsVOList.forEach(vo -> vo.setLiked(false));
        }
    }

    /**
     * 更新点赞数和评论数，添加 NPE 防护和默认值处理
     */
    public void updateCount(List<MomentsVO> momentsVOList) {
        if (momentsVOList == null || momentsVOList.isEmpty()) {
            return;
        }

        try {
            List<Object> result = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (MomentsVO vo : momentsVOList) {
                    String key = MOMENTS_COUNT_KEY + vo.getId();
                    connection.hashCommands().hMGet(key.getBytes(), "like".getBytes(), "comment".getBytes());
                }
                return null;
            });

            if (result != null && result.size() == momentsVOList.size()) {
                for (int i = 0; i < momentsVOList.size(); i++) {
                    MomentsVO vo = momentsVOList.get(i);
                    List<Object> row = (List<Object>) result.get(i);

                    if (row != null && !row.isEmpty()) {
                        // 更新点赞数
                        if (row.get(0) != null) {
                            try {
                                vo.setLikeCount(Integer.parseInt(row.get(0).toString()));
                            } catch (NumberFormatException e) {
                                log.warn("点赞数格式错误，momentId: {}, value: {}", vo.getId(), row.get(0));
                            }
                        }

                        // 更新评论数
                        if (row.size() > 1 && row.get(1) != null) {
                            try {
                                vo.setCommentCount(Integer.parseInt(row.get(1).toString()));
                            } catch (NumberFormatException e) {
                                log.warn("评论数格式错误，momentId: {}, value: {}", vo.getId(), row.get(1));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("更新计数器失败", e);
        }
    }

}
