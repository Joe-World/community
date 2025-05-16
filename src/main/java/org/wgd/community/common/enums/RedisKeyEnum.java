package org.wgd.community.common.enums;

public enum RedisKeyEnum {
    USER_LIKE_ENTITY("user:like:entity", "用户点赞帖子/评论"),
    ENTITY_LIKED_COUNT("entity:liked:count", "帖子/评论获赞数量"),
    USER_LIKED_COUNT("user:liked:count", "用户获赞数量"),
    USER_FOLLOW("user:follow", "用户关注"),
    USER_FAN("user:fan", "用户被关注/用户粉丝"),
    CODE("code", "验证码"),
    TICKET_KEY("ticket", "用户凭证"),
    USER_INFO("user:info", "用户信息缓存"),
    UNIQUE_VISITOR("uv", "独立访客"),
    DAILY_ACTIVE_USER("dau", "每日活跃用户"),
    POST_SCORE("post:score", "帖子热度分"),
    ;
    private final String RedisKey;
    private final String value;

    RedisKeyEnum(String redisKey, String value) {
        RedisKey = redisKey;
        this.value = value;
    }

    public String getRedisKey() {
        return RedisKey;
    }

    public String getValue() {
        return value;
    }
}
