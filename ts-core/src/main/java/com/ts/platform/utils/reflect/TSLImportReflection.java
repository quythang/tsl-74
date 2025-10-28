package com.ts.platform.utils.reflect;

import com.ts.platform.utils.annotation.TSLImport;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TSLImportReflection implements TSLReflection {

    private final Set<Class<?>> classes = new HashSet<>();

    public TSLImportReflection(Set<Class<?>> annotatedClasses) {
        for (Class<?> clazz : annotatedClasses) {
            TSLImport ann = clazz.getAnnotation(TSLImport.class);
            if (ann != null) {
                classes.addAll(Arrays.asList(ann.value()));
            }
        }
    }

    @Override
    public Set<Class<?>> getExtendsClasses(Class<?> parentClass) {
        Set<Class<?>> answer = new HashSet<>();
        for (Class<?> clazz : classes) {
            if (parentClass.isAssignableFrom(clazz)) {
                answer.add(clazz);
            }
        }
        return answer;
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
        Set<Class<?>> answer = new HashSet<>();
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(annotationClass)) {
                answer.add(clazz);
            }
        }
        return answer;
    }
}
