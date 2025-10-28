package com.ts.platform.utils.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class TSLSetterMethod extends TSLByFieldMethod {

    public TSLSetterMethod(Method method) {
        this(new TSLMethod(method));
    }

    public TSLSetterMethod(TSLMethod method) {
        super(method.getMethod());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class getType() {
        return getParameterTypes()[0];
    }

    @Override
    public Type getGenericType() {
        return getGenericParameterTypes()[0];
    }

    @Override
    public String getFieldName() {
        return TSLMethods.getFieldNameOfSetter(method);
    }
}
