package org.wgd.community.util;

import org.wgd.community.common.enums.RedisKeyEnum;

public class RedisKeyUtils {
    public static final String SPLIT = ":";

    public static String getUserLikedKey(int userId, int entityType, int entityId) {
        StringBuilder builder = new StringBuilder();
        builder.append(userId);
        builder.append(SPLIT);
        builder.append(entityType);
        builder.append(SPLIT);
        builder.append(entityId);

        return builder.toString();
    }

    public static String getEntityLikedCountKey(int entityType, int entityId) {
        StringBuilder builder = new StringBuilder();
        builder.append(entityType);
        builder.append(SPLIT);
        builder.append(entityId);

        return builder.toString();
    }

    public static String getUserFollowKey(int userId, int entityType) {
        StringBuilder builder = new StringBuilder();
        builder.append(RedisKeyEnum.USER_FOLLOW.getRedisKey());
        builder.append(SPLIT);
        builder.append(userId);
        builder.append(SPLIT);
        builder.append(entityType);

        return builder.toString();
    }

    public static String getUserFanKey(int entityType, int userId) {
        StringBuilder builder = new StringBuilder();
        builder.append(RedisKeyEnum.USER_FAN.getRedisKey());
        builder.append(SPLIT);
        builder.append(entityType);
        builder.append(SPLIT);
        builder.append(userId);

        return builder.toString();
    }

    public static String getCodeKey(String owner) {
        StringBuilder builder = new StringBuilder();
        builder.append(RedisKeyEnum.CODE.getRedisKey());
        builder.append(SPLIT);
        builder.append(owner);

        return builder.toString();
    }

    public static String getTicketKey(String ticket) {
        StringBuilder builder = new StringBuilder();
        builder.append(RedisKeyEnum.TICKET_KEY.getRedisKey());
        builder.append(SPLIT);
        builder.append(ticket);

        return builder.toString();
    }

    public static String getUserInfoKey(Integer userId) {
        StringBuilder builder = new StringBuilder();
        builder.append(RedisKeyEnum.USER_INFO.getRedisKey());
        builder.append(SPLIT);
        builder.append(userId);

        return builder.toString();
    }

    /**
     * 单日独立访客key
     * @param date
     * @return
     */
    public static String getUVKey(String date) {
        StringBuilder builder = new StringBuilder();
        builder.append(RedisKeyEnum.UNIQUE_VISITOR.getRedisKey());
        builder.append(SPLIT);
        builder.append(date);

        return builder.toString();
    }

    /**
     * 区间独立访客key
     * @param startDate
     * @param endDate
     * @return
     */
    public static String getUVKey(String startDate, String endDate) {
        StringBuilder builder = new StringBuilder();
        builder.append(RedisKeyEnum.UNIQUE_VISITOR.getRedisKey());
        builder.append(SPLIT);
        builder.append(startDate);
        builder.append(SPLIT);
        builder.append(endDate);

        return builder.toString();
    }

    /**
     * 单日活跃用户key
     * @param date
     * @return
     */
    public static String getDAUKey(String date) {
        StringBuilder builder = new StringBuilder();
        builder.append(RedisKeyEnum.DAILY_ACTIVE_USER.getRedisKey());
        builder.append(SPLIT);
        builder.append(date);

        return builder.toString();
    }

    /**
     * 区间活跃用户key
     * @param startDate
     * @param endDate
     * @return
     */
    public static String getDAUKey(String startDate, String endDate) {
        StringBuilder builder = new StringBuilder();
        builder.append(RedisKeyEnum.DAILY_ACTIVE_USER.getRedisKey());
        builder.append(SPLIT);
        builder.append(startDate);
        builder.append(SPLIT);
        builder.append(endDate);

        return builder.toString();
    }

    public static String getPostScoreKey() {
        return RedisKeyEnum.POST_SCORE.getRedisKey();
    }
}
