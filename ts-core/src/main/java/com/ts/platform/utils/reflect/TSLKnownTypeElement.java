package com.ts.platform.utils.reflect;

public interface TSLKnownTypeElement {

    @SuppressWarnings("rawtypes")
    Class getType();

    default String getTypeName() {
        return getType().getTypeName();
    }
}
