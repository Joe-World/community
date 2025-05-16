package org.wgd.community.common.enums;

public enum EntityTypeEnum {
    POST(1, "帖子"),
    COMMENT(2, "评论"),
    USER(3, "用户"),

    ;

    private final int type;
    private final String value;

    EntityTypeEnum(int type, String value) {
        this.type = type;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
