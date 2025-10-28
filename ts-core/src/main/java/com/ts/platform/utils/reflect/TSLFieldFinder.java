package com.ts.platform.utils.reflect;


import lombok.AllArgsConstructor;

import java.lang.reflect.Field;

@SuppressWarnings("rawtypes")
@AllArgsConstructor
public class TSLFieldFinder {
    protected Class clazz;
    protected String fieldName;

    public static Builder builder() {
        return new Builder();
    }

    public Field find() {
        return getField(clazz);
    }

    protected Field getField(Class clazz) {
        Field field = tryGetField(clazz);
        if (field != null) {
            return field;
        }
        Class[] interfaces = getInterfaces(clazz);
        for (Class itf : interfaces) {
            field = getField(itf);
            if (field != null) {
                return field;
            }
        }
        Class superClass = getSupperClasses(clazz);
        return superClass != null ? getField(superClass) : null;
    }

    protected Field tryGetField(Class clazz) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (Exception e) {
            return null;
        }
    }

    protected Class getSupperClasses(Class clazz) {
        return clazz.getSuperclass();
    }

    protected Class[] getInterfaces(Class clazz) {
        return clazz.getInterfaces();
    }

    public static class Builder implements EzyBuilder<TSLFieldFinder> {
        protected Class clazz;
        protected String fieldName;

        public Builder clazz(Class clazz) {
            this.clazz = clazz;
            return this;
        }

        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        @Override
        public TSLFieldFinder build() {
            return new TSLFieldFinder(clazz, fieldName);
        }
    }
}
