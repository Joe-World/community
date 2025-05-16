package org.wgd.community.common.enums;

public enum MessageStatusEnum {
    UNREAD(0, "私信未读"),
    READ(1, "私信已读"),
    ;
    private final int type;
    private final String value;

    MessageStatusEnum(int type, String value) {
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
