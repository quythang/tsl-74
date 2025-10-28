package com.ts.platform.utils.naming;


import lombok.Getter;

public enum TSLNamingCase {
    NATURE(""),
    UPPER(""),
    LOWER(""),
    CAMEL(""),
    DASH("-"),
    DOT("."),
    UNDERSCORE("_");

    @Getter
    private final String sign;

    TSLNamingCase(String sign) {
        this.sign = sign;
    }

    public static TSLNamingCase of(String value) {
        if (value == null) {
            return NATURE;
        }
        return valueOf(value);
    }
}
