package com.ts.platform.utils.reflect;

import java.util.Set;

public final class TSLInterfaces {

    private TSLInterfaces() {}

    @SuppressWarnings("rawtypes")
    public static Class getInterface(Class clazz, Class interfaceClass) {
        Set<Class> interfaces = TSLClasses.flatInterfaces(clazz);
        return getInterface(interfaces, interfaceClass);
    }

    @SuppressWarnings("rawtypes")
    private static Class getInterface(Set<Class> classes, Class interfaceClass) {
        for (Class itf : classes) {
            if (itf.equals(interfaceClass)) {
                return itf;
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public static Class getInterfaceAnyway(Class clazz, Class interfaceClass) {
        Set<Class> classes = TSLClasses.flatSuperAndInterfaceClasses(clazz);
        return getInterface(classes, interfaceClass);
    }
}
