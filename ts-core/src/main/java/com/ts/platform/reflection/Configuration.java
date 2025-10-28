package com.ts.platform.reflection;



import com.ts.platform.reflection.adapters.MetadataAdapter;
import com.ts.platform.reflection.scanners.Scanner;
import com.ts.platform.reflection.serializers.Serializer;


import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;


public interface Configuration {
    Set<Scanner> getScanners();

    Set<URL> getUrls();

    MetadataAdapter getMetadataAdapter();


    Predicate<String> getInputsFilter();

    ExecutorService getExecutorService();

    Serializer getSerializer();


    ClassLoader[] getClassLoaders();

    boolean shouldExpandSuperTypes();
}
