package org.wgd.community.common.enums;

public enum YesOrNo {
    YES(1, "是"),
    NO(0,"否")
    ;
    private final int type;
    private final String value;

    YesOrNo(int type, String value) {
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
