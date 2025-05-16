package org.wgd.community.common.enums;

public enum ExpiredDayEnum {
    OEN_WEEK(7, "数字7，代表一周"),
    ONE_MONTH(30, "数字30，代表一个月"),
    ;
    private final int day;
    private final String value;

    ExpiredDayEnum(int day, String value) {
        this.day = day;
        this.value = value;
    }

    public int getDay() {
        return day;
    }

    public String getValue() {
        return value;
    }
}
