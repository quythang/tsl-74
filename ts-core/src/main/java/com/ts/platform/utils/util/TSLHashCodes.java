package com.ts.platform.utils.util;

public class TSLHashCodes {

    protected final int initial;
    protected final int prime;
    protected int hashCode;

    public TSLHashCodes() {
        this(1, 31);
    }

    public TSLHashCodes(int initial, int prime) {
        this.initial = initial;
        this.prime = prime;
        this.hashCode = initial;
    }

    public int toHashCode() {
        return hashCode;
    }

    public TSLHashCodes append(Object value) {
        this.hashCode = hashCode * prime + (value == null ? 43 : value.hashCode());
        return this;
    }
}
