package com.ts.platform.utils.reflect;

import java.lang.annotation.Annotation;

public interface TLSAnnotatedElement {

    <T extends Annotation> T getAnnotation(Class<T> annClass);

    boolean isAnnotated(Class<? extends Annotation> annClass);
}
