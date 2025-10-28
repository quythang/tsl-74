package com.ts.platform.utils.reflect;

public final class TSLPackages {

    private TSLPackages() {}

    public static TSLReflection scanPackage(String packet) {
        return new TSLReflectionProxy(packet);
    }

    public static TSLReflection scanPackages(Iterable<String> packages) {
        return new TSLReflectionProxy(packages);
    }
}
