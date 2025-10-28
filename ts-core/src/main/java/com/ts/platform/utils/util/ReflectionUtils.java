package com.ts.platform.utils.util;

import com.ts.platform.utils.collect.Lists;
import com.ts.platform.utils.collect.Sets;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ReflectionUtils {

    public static boolean includeObject = false;

    public static List<Method> getAllMethodList(Class<?> type, Predicate<? super Method>... predicates) {
        List<Method> result = Lists.newArrayList(new Method[0]);

        for(Class<?> t : getAllSuperTypes(type)) {
            result.addAll(getMethodList(t, predicates));
        }

        return result;
    }

    @SafeVarargs
    public static List<Method> getMethodList(Class<?> t, Predicate<? super Method>... predicates) {
        return filterToList(t.isInterface() ? t.getMethods() : t.getDeclaredMethods(), predicates);
    }


    public static Set<Class<?>> getAllSuperTypes(Class<?> type, Predicate<? super Class<?>>... predicates) {
        Set result = Sets.newLinkedHashSet(new Class[0]);
        if (type != null && (includeObject || !type.equals(Object.class))) {
            result.add(type);

            for(Class<?> supertype : getSuperTypes(type)) {
                result.addAll(getAllSuperTypes(supertype));
            }
        }

        return filter(result, predicates);
    }

    static <T> List<T> filterToList(T[] elements, Predicate<? super T>... predicates) {
        return Utils.isEmpty(predicates) ? Lists.newArrayList(elements) : Lists.newArrayList(Iterables.filter(Arrays.asList(elements), Predicates.and(predicates)));
    }

    static <T> Set<T> filter(T[] elements, Predicate<? super T>... predicates) {
        return Utils.isEmpty(predicates) ? Sets.newHashSet(elements) : Sets.newHashSet(Iterables.filter(Arrays.asList(elements), Predicates.and(predicates)));
    }

    static <T> Set<T> filter(Iterable<T> elements, Predicate<? super T>... predicates) {
        return Utils.isEmpty(predicates) ? Sets.newHashSet(elements) : Sets.newHashSet(Iterables.filter(elements, Predicates.and(predicates)));
    }

    public static Set<Class<?>> getSuperTypes(Class<?> type) {
        Set<Class<?>> result = new LinkedHashSet<>();
        Class<?> superclass = type.getSuperclass();
        Class<?>[] interfaces = type.getInterfaces();
        if (superclass != null && (includeObject || !superclass.equals(Object.class))) {
            result.add(superclass);
        }

        if (interfaces != null && interfaces.length > 0) {
            result.addAll(Arrays.asList(interfaces));
        }

        return result;
    }
}
