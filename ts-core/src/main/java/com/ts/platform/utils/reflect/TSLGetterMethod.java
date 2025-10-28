package com.ts.platform.utils.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class TSLGetterMethod extends TSLByFieldMethod {

    public TSLGetterMethod(Method method) {
        this(new TSLMethod(method));
    }

    public TSLGetterMethod(TSLMethod method) {
        super(method.getMethod());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class getType() {
        return getReturnType();
    }

    @Override
    public Type getGenericType() {
        return getGenericReturnType();
    }

    @Override
    public String getFieldName() {
        return TSLMethods.getFieldNameOfGetter(method);
    }
}
