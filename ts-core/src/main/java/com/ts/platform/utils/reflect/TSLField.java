package com.ts.platform.utils.reflect;


import com.ts.platform.utils.functional.TSLBuilder;
import com.ts.platform.utils.util.TSLEquals;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class TSLField
    implements TSLReflectElement, TSLGenericElement, TSLKnownTypeElement {

    @Getter
    protected final Field field;

    public TSLField(Field field) {
        this.field = field;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Object get(Object obj) {
        return TSLFields.get(field, obj);
    }

    public void set(Object obj, Object value) {
        TSLFields.set(field, obj, value);
    }

    @Override
    public String getName() {
        return field.getName();
    }

    public boolean isPublic() {
        return Modifier.isPublic(field.getModifiers());
    }

    public boolean isWritable() {
        return !Modifier.isFinal(field.getModifiers());
    }

    public boolean isMapType() {
        return Map.class.isAssignableFrom(getType());
    }

    public boolean isCollection() {
        return Collection.class.isAssignableFrom(getType());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class getType() {
        return field.getType();
    }

    public String getGetterMethod() {
        return TSLFields.getGetterMethod(field);
    }

    public String getSetterMethod() {
        return TSLFields.getSetterMethod(field);
    }

    @Override
    public Type getGenericType() {
        return field.getGenericType();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass) {
        return field.getAnnotation(annClass);
    }

    @Override
    public boolean isAnnotated(Class<? extends Annotation> annClass) {
        return field.isAnnotationPresent(annClass);
    }

    public void setAccessible(boolean flag) {
        field.setAccessible(flag);
    }

    @Override
    public boolean equals(Object obj) {
        return new TSLEquals<TSLField>()
            .function(f -> f.field)
            .isEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return new EzyHashCodes()
            .append(field)
            .toHashCode();
    }

    @Override
    public String toString() {
        return field.toString();
    }

    @SuppressWarnings("rawtypes")
    public static class Builder implements TSLBuilder<TSLField> {
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
        public TSLField build() {
            return new TSLField(getField());
        }

        protected Field getField() {
            return TSLFields.getField(clazz, fieldName);
        }
    }
}
