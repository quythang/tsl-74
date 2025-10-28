package com.ts.platform.utils.reflect;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

public abstract class TSLByFieldMethod
    extends TSLMethod
    implements TSLGenericElement, TSLKnownTypeElement {

    public TSLByFieldMethod(Method method) {
        super(method);
    }

    public String getFieldName() {
        return TSLMethods.getFieldName(method, 3);
    }

    public boolean isMapType() {
        return Map.class.isAssignableFrom(getType());
    }

    public boolean isCollection() {
        return Collection.class.isAssignableFrom(getType());
    }
}
