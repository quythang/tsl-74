package com.ts.platform.utils.reflect;


import com.ts.platform.utils.collect.Lists;
import com.ts.platform.utils.functional.TSLBuilder;
import com.ts.platform.utils.util.TSLEquals;
import com.ts.platform.utils.util.TSLHashCodes;
import lombok.Getter;
import lombok.Setter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.ts.platform.utils.reflect.TSLReflections.*;


@Getter
public class TSLMethod implements TSLReflectElement {

    protected final Method method;

    @Setter
    protected String displayName;

    public TSLMethod(Method method) {
        this.method = method;
        this.displayName = method.getName();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Object invoke(Object obj, Object... args) {
        return TSLMethods.invoke(method, obj, args);
    }

    @Override
    public String getName() {
        return method.getName();
    }

    public boolean isSetter() {
        return isPublic()
            && method.getName().startsWith(METHOD_PREFIX_SET)
            && method.getParameterCount() == 1;
    }

    public boolean isGetter() {
        String methodName = method.getName();
        return isPublic()
            && (
                methodName.startsWith(METHOD_PREFIX_GET)
                    || methodName.startsWith(METHOD_PREFIX_IS)
            )
            && method.getParameterCount() == 0
            && method.getReturnType() != void.class;
    }

    public boolean isPublic() {
        return Modifier.isPublic(method.getModifiers());
    }

    @SuppressWarnings("rawtypes")
    public Class getReturnType() {
        return method.getReturnType();
    }

    public Type getGenericReturnType() {
        return method.getGenericReturnType();
    }

    public Parameter[] getParameters() {
        return method.getParameters();
    }

    @SuppressWarnings("rawtypes")
    public Class[] getParameterTypes() {
        return method.getParameterTypes();
    }

    public int getParameterCount() {
        return method.getParameterCount();
    }

    public Type[] getGenericParameterTypes() {
        return method.getGenericParameterTypes();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass) {
        return method.getAnnotation(annClass);
    }

    @Override
    public boolean isAnnotated(Class<? extends Annotation> annClass) {
        return method.isAnnotationPresent(annClass);
    }

    public String getFieldName() {
        String name = getName();
        if (!name.startsWith(METHOD_PREFIX_SET)
            && !name.startsWith(METHOD_PREFIX_GET)
            && !name.startsWith(METHOD_PREFIX_NEW)
        ) {
            return name;
        }
        if (name.length() <= 3) {
            return name;
        }
        String remain = name.substring(3);
        return remain.substring(0, 1).toLowerCase() + remain.substring(1);
    }

    public String getDeclaration() {
        int modifiers = method.getModifiers();
        return getDeclaration(Modifier.toString(modifiers));
    }

    public String getDeclaration(String modifierName) {
        Parameter[] params = method.getParameters();
        Class<?> returnType = method.getReturnType();
        StringBuilder builder = new StringBuilder();
        builder
            .append(modifierName)
            .append(" ")
            .append(returnType.getTypeName())
            .append(" ")
            .append(displayName)
            .append("(");
        for (int i = 0; i < params.length; ++i) {
            builder.append(params[i].getType().getTypeName())
                .append(" ")
                .append(params[i].getName());
            if (i < params.length - 1) {
                builder.append(", ");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    public String getPublicDeclaration() {
        return getDeclaration(MODIFIER_PUBLIC);
    }

    public void setAccessible(boolean flag) {
        method.setAccessible(flag);
    }

    @Override
    public boolean equals(Object obj) {
        return new TSLEquals<TSLMethod>()
            .function(m -> m.method)
            .isEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return new TSLHashCodes()
            .append(method)
            .toHashCode();
    }

    @Override
    public String toString() {
        return method.toString();
    }

    @SuppressWarnings("rawtypes")
    public static class Builder implements TSLBuilder<TSLMethod> {
        protected Class clazz;
        protected String methodName;
        protected List<Class> parameterTypes = new ArrayList<>();

        public Builder clazz(Class clazz) {
            this.clazz = clazz;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder parameterTypes(Class... parameterTypes) {
            this.parameterTypes.addAll(Lists.newArrayList(parameterTypes));
            return this;
        }

        @Override
        public TSLMethod build() {
            return new TSLMethod(getMethod());
        }

        protected Method getMethod() {
            return TSLMethods.getMethod(clazz, methodName, getParameterTypes());
        }

        protected Class[] getParameterTypes() {
            return parameterTypes.toArray(new Class[0]);
        }
    }
}
