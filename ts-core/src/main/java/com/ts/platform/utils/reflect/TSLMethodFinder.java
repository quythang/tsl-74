package com.ts.platform.utils.reflect;

import com.ts.platform.utils.collect.Lists;
import com.ts.platform.utils.functional.TSLBuilder;
import lombok.AllArgsConstructor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
@AllArgsConstructor
public class TSLMethodFinder {
    protected Class clazz;
    protected String methodName;
    protected Class[] parameterTypes;

    public static Builder builder() {
        return new Builder();
    }

    public Method find() {
        return getMethod(clazz);
    }

    protected Method getMethod(Class clazz) {
        Method method = tryGetMethod(clazz);
        if (method != null) {
            return method;
        }
        Class[] interfaces = getInterfaces(clazz);
        for (Class itf : interfaces) {
            method = getMethod(itf);
            if (method != null) {
                return method;
            }
        }
        Class superClass = getSupperClasses(clazz);
        return superClass != null ? getMethod(superClass) : null;
    }

    @SuppressWarnings({"unchecked"})
    protected Method tryGetMethod(Class clazz) {
        try {
            return clazz.getDeclaredMethod(methodName, parameterTypes);
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

    public static class Builder implements TSLBuilder<TSLMethodFinder> {
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
        public TSLMethodFinder build() {
            return new TSLMethodFinder(
                clazz,
                methodName,
                parameterTypes.toArray(new Class[0]));
        }
    }
}
