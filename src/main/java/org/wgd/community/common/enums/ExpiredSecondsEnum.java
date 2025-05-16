package org.wgd.community.common.enums;

public enum ExpiredSecondsEnum {
    DEFAULT(3600*12,"默认状态的登录凭证的超时时间"),
    REMEMBER(3600*24*100,"记住状态的登录凭证超时时间");

    private final int seconds;
    private final String value;

    ExpiredSecondsEnum(int seconds, String value) {
        this.seconds = seconds;
        this.value = value;
    }

    public int getSeconds() {
        return seconds;
    }

    public String getValue() {
        return value;
    }
}
