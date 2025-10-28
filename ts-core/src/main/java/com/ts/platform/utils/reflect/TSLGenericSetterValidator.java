package com.ts.platform.utils.reflect;

import com.ts.platform.utils.functional.TSLValidator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Set;

public class TSLGenericSetterValidator implements TSLValidator<Type> {

    @Override
    public boolean validate(Type genericType) {
        if (genericType instanceof WildcardType) {
            return false;
        }
        if (genericType instanceof Class) {
            return validateClassType(genericType);
        }
        if (genericType instanceof ParameterizedType) {
            return validate((ParameterizedType) genericType);
        }
        return false;
    }

    protected boolean validate(ParameterizedType parameterizedType) {
        Type[] types = parameterizedType.getActualTypeArguments();
        for (Type type : types) {
            if (!validate(type)) {
                return false;
            }
        }
        return true;
    }

    protected boolean validateClassType(Type classType) {
        return !getCommonGenericTypes().contains(classType);
    }

    @SuppressWarnings("rawtypes")
    protected Set<Class> getCommonGenericTypes() {
        return TSLTypes.COMMON_GENERIC_TYPES;
    }
}
