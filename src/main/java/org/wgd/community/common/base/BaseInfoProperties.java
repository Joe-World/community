package org.wgd.community.common.base;

public class BaseInfoProperties {
    // 主题事件种类
    public static final String TOPIC_COMMENT = "comment";
    public static final String TOPIC_LIKE = "like";
    public static final String TOPIC_FOLLOW = "follow";
    public static final String TOPIC_UPDATE_ES = "update_es";
    public static final String TOPIC_DELETE_ES = "delete_es";

    // 系统id
    public static final int SYSTEM_USER_ID = 1;

    // 用户权限：普通用户，管理员，版主
    public static final String AUTHORIZATION_USER = "user";
    public static final String AUTHORIZATION_ADMIN = "admin";
    public static final String AUTHORIZATION_MODERATOR = "moderator";
}
