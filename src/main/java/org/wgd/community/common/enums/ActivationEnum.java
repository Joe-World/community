package org.wgd.community.common.enums;

public enum ActivationEnum {
    SUCCESS(0,"激活成功"),
    REPEAT(1,"重复激活"),
    FAILURE(2,"激活失败");

    private final int type;
    private final String value;

    ActivationEnum(int type, String value) {
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
