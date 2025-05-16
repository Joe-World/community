package org.wgd.community.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.wgd.community.common.enums.EntityTypeEnum;
import org.wgd.community.common.enums.RedisKeyEnum;
import org.wgd.community.common.enums.YesOrNo;
import org.wgd.community.model.pojo.User;
import org.wgd.community.service.RedisService;
import org.wgd.community.util.RedisKeyUtils;
import org.wgd.community.util.RedisUtil;

import java.util.*;

@Service
public class RedisServiceImpl implements RedisService {
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserServiceImpl userService;

    /**
     * 将点赞信息存入redis，使用hash数据结构
     *
     * @param userId
     * @param entityType
     * @param entityId
     */
    @Override
    public void like(int userId, int entityType, int entityId, int likedUserId) {
        redisUtil.getRedisTemplate().execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                // key
                String statusKey = RedisKeyEnum.USER_LIKE_ENTITY.getRedisKey();
                String entityCountKey = RedisKeyEnum.ENTITY_LIKED_COUNT.getRedisKey();
                String userCountKey = RedisKeyEnum.USER_LIKED_COUNT.getRedisKey();
                // field
                String statusField = RedisKeyUtils.getUserLikedKey(userId, entityType, entityId);
                String entityCountField = RedisKeyUtils.getEntityLikedCountKey(entityType, entityId);
                String userCountField = String.valueOf(likedUserId);

                Object isLikedHash = redisUtil.hget(statusKey, statusField);

                // 开启事务
                operations.multi();

                // 不存在hash
                if (isLikedHash == null) {
                    // 用户是否点赞该帖子的状态
                    redisUtil.hset(statusKey, statusField, YesOrNo.YES.getType());
                    // 不存在，直接新建计数键值对
                    redisUtil.hincrNoReturn(entityCountKey, entityCountField, 1);

                    redisUtil.hincrNoReturn(userCountKey, userCountField, 1);
                } else if ((Integer) isLikedHash == (YesOrNo.YES.getType())) {
                    redisUtil.hset(statusKey, statusField, YesOrNo.NO.getType());
                    // 存在，直接 - 1
                    // 帖子总点赞计数
                    redisUtil.hincrNoReturn(entityCountKey, entityCountField, -1);

                    // 用户总被赞计数
                    redisUtil.hincrNoReturn(userCountKey, userCountField, -1);
                } else {
                    redisUtil.hset(statusKey, statusField, YesOrNo.YES.getType());
                    // 存在，直接 + 1
                    redisUtil.hincrNoReturn(entityCountKey, entityCountField, 1);

                    redisUtil.hincrNoReturn(userCountKey, userCountField, 1);
                }

                return operations.exec();
            }
        });
    }

    /**
     * 查询帖子/评论点赞数
     *
     * @param entityType
     * @param entityId
     * @return
     */
    @Override
    public long countEntityLike(int entityType, int entityId) {
        String key = RedisKeyEnum.ENTITY_LIKED_COUNT.getRedisKey();
        String filed = RedisKeyUtils.getEntityLikedCountKey(entityType, entityId);

        return redisUtil.hHasKey(key, filed) ? (Integer) redisUtil.hget(key, filed) : 0;
    }

    @Override
    public int getEntityLikedStatus(int userId, int entityType, int entityId) {
        String key = RedisKeyEnum.USER_LIKE_ENTITY.getRedisKey();
        String filed = RedisKeyUtils.getUserLikedKey(userId, entityType, entityId);

        return redisUtil.hHasKey(key, filed) ? (int) redisUtil.hget(key, filed) : 0;
    }

    @Override
    public long countUserLiked(int userId) {
        String key = RedisKeyEnum.USER_LIKED_COUNT.getRedisKey();
        String field = String.valueOf(userId);

        return redisUtil.hHasKey(key, field) ? (Integer) redisUtil.hget(key, field) : 0;
    }


    @Override
    public void follow(int userId, int entityType, int entityId) {
        redisUtil.getRedisTemplate().execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                // 组装key
                String followKey = RedisKeyUtils.getUserFollowKey(userId, entityType);
                String fanKey = RedisKeyUtils.getUserFanKey(entityType, entityId);

                // 开启事务
                operations.multi();

                // key-val-score
                operations.opsForZSet().add(followKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(fanKey, userId, System.currentTimeMillis());

                return operations.exec();
            }
        });
    }


    public void unfollow(int userId, int entityType, int entityId) {
        redisUtil.getRedisTemplate().execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followKey = RedisKeyUtils.getUserFollowKey(userId, entityType);
                String fanKey = RedisKeyUtils.getUserFanKey(entityType, entityId);

                operations.multi();

                operations.opsForZSet().remove(followKey, entityId);
                operations.opsForZSet().remove(fanKey, userId);

                return operations.exec();
            }
        });
    }

    @Override
    public long countUserFollow(int userId, int entityType) {
        String followKey = RedisKeyUtils.getUserFollowKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followKey);
    }

    @Override
    public long countUserFan(int entityType, int entityId) {
        String fanKey = RedisKeyUtils.getUserFanKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(fanKey);
    }

    @Override
    public boolean isFollowed(int userId, int entityType, int entityId) {
        String followKey = RedisKeyUtils.getUserFollowKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followKey, entityId) != null;
    }

    @Override
    public List<Map<String, Object>> getFollows(int userId, int begin, int limit) {
        String followKey = RedisKeyUtils.getUserFollowKey(userId, EntityTypeEnum.USER.getType());

        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followKey, begin, begin + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.getUser(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

    @Override
    public List<Map<String, Object>> getFans(int userId, int begin, int limit) {
        String fanKeys = RedisKeyUtils.getUserFanKey(EntityTypeEnum.USER.getType(), userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(fanKeys,begin, begin + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.getUser(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(fanKeys, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

}
