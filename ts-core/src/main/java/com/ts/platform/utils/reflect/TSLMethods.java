package com.ts.platform.utils.reflect;

import com.ts.platform.utils.collect.Lists;
import com.ts.platform.utils.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class TSLMethods {

    private TSLMethods() {}

    public static String getFieldNameOfGetter(Method method) {
        String name = method.getName();
        if (name.length() <= 3) {
            return name;
        }
        if (name.startsWith("get")) {
            return getFieldName(method, 3);
        }
        if (name.startsWith("is")) {
            return getFieldName(method, 2);
        }
        return name;
    }

    public static String getFieldNameOfSetter(Method method) {
        String name = method.getName();
        if (name.length() <= 3) {
            return name;
        }
        if (name.startsWith("set")) {
            return getFieldName(method, 3);
        }
        return name;
    }

    public static String getFieldName(Method method, int prefixLength) {
        String name = method.getName();
        name = name.substring(prefixLength);
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    public static Object invoke(Method method, Object obj, Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            throw new IllegalArgumentException("can not call method " + method.getName(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Method> getMethods(Class clazz) {
        return ReflectionUtils.getAllMethodList(clazz);
    }

    @SuppressWarnings("rawtypes")
    public static Method getMethod(
        Class clazz, String methodName, Class... parameterTypes) {
        return new TSLMethodFinder(clazz, methodName, parameterTypes).find();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Method getPublicMethod(
        Class clazz, String methodName, Class... parameterTypes) {
        if (clazz == null) {
            return null;
        }
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            if (Modifier.isPublic(method.getModifiers())) {
                return method;
            }
            return null;
        } catch (NoSuchMethodException e) {
            Class[] interfaces = clazz.getInterfaces();
            for (Class itf : interfaces) {
                Method method = getPublicMethod(itf, methodName, parameterTypes);
                if (method != null) {
                    return method;
                }
            }
            Class supperClass = clazz.getSuperclass();
            return getPublicMethod(supperClass, methodName, parameterTypes);
        }
    }

    @SuppressWarnings("rawtypes")
    public static List<Method> getAnnotatedMethods(
        Class clazz, Class<? extends Annotation> annClass) {
        List<Method> answer = new ArrayList<>();
        getAnnotatedMethods(clazz, annClass, answer);
        return answer;
    }

    @SuppressWarnings("rawtypes")
    private static void getAnnotatedMethods(
        Class clazz,
        Class<? extends Annotation> annClass,
        List<Method> output) {
        if (clazz == null || clazz.equals(Object.class)) {
            return;
        }
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(annClass)) {
                output.add(method);
            }
        }
        getAnnotatedMethods(clazz.getSuperclass(), annClass, output);
        Class[] interfaces = clazz.getInterfaces();
        for (Class itf : interfaces) {
            getAnnotatedMethods(itf, annClass, output);
        }
    }

    @SuppressWarnings("rawtypes")
    public static List<Method> getDeclaredMethods(Class clazz) {
        return Lists.newArrayList(clazz.getDeclaredMethods());
    }

    @SuppressWarnings("rawtypes")
    public static List<Method> getPublicMethods(Class clazz) {
        List<Method> methods = getMethods(clazz);
        List<Method> answer = new ArrayList<>();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers())) {
                answer.add(method);
            }
        }
        return answer;
    }

    public static List<Method> filterOverriddenMethods(List<Method> allMethods) {
        List<Method> methods = new ArrayList<>();
        for (Method i : allMethods) {
            boolean valid = true;
            for (Method k : methods) {
                if (isOverriddenMethod(i, k)) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                methods.add(i);
            }
        }
        return methods;
    }

    public static List<TSLMethod> filterOverriddenMethods(Collection<TSLMethod> allMethods) {
        List<TSLMethod> methods = new ArrayList<>();
        for (TSLMethod i : allMethods) {
            boolean valid = true;
            for (TSLMethod k : methods) {
                if (isOverriddenMethod(i, k)) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                methods.add(i);
            }
        }
        return methods;
    }

    public static boolean isOverriddenMethod(TSLMethod a, TSLMethod b) {
        return isOverriddenMethod(a.getMethod(), b.getMethod());
    }

    public static boolean isOverriddenMethod(Method a, Method b) {
        try {
            if (a.equals(b)) {
                return false;
            }
            if (a.getName().equals(b.getName())) {
                boolean answer = false;
                Class<?> dca = a.getDeclaringClass();
                Class<?> dcb = b.getDeclaringClass();
                if (dca.equals(dcb)) {
                    return false;
                }
                if (dca.isAssignableFrom(dcb)) {
                    try {
                        dcb.getDeclaredMethod(a.getName(), a.getParameterTypes());
                        answer = true;
                    } catch (NoSuchMethodException ignored) {
                        // do nothing
                    }
                } else if (dcb.isAssignableFrom(dca)) {
                    try {
                        dca.getDeclaredMethod(b.getName(), b.getParameterTypes());
                        answer = true;
                    } catch (NoSuchMethodException ignored) {
                        // do nothing
                    }
                }
                return answer;
            }
            return false;
        } catch (Exception e) {
            throw new IllegalArgumentException("can't check overridden of method: " + a + ", " + b, e);
        }
    }
}
