package com.ts.platform.reflection.util;


import com.ts.platform.reflection.Reflections;
import com.ts.platform.reflection.scanners.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * a garbage can of convenient methods
 */
@SuppressWarnings("rawtypes")
public abstract class Utils {

    public static String repeat(String string, int times) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < times; i++) {
            sb.append(string);
        }

        return sb.toString();
    }

    /**
     * isEmpty compatible with Java 5
     */
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean isEmpty(Object[] objects) {
        return objects == null || objects.length == 0;
    }

    public static File prepareFile(String filename) {
        File file = new File(filename);
        File parent = file.getAbsoluteFile().getParentFile();
        if (!parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        return file;
    }




    public static void close(InputStream closeable) {
        try { if (closeable != null) closeable.close(); }
        catch (IOException e) {
            if (Reflections.log != null) {
                Reflections.log.warn("Could not close InputStream", e);
            }
        }
    }

    public static Logger findLogger(Class<?> aClass) {
        try {
            // This is to check whether an optional SLF4J binding is available. While SLF4J recommends that libraries
            // "should not declare a dependency on any SLF4J binding but only depend on slf4j-api", doing so forces
            // users of the library to either add a binding to the classpath (even if just slf4j-nop) or to set the
            // "slf4j.suppressInitError" system property in order to avoid the warning, which both is inconvenient.
            Class.forName("org.slf4j.impl.StaticLoggerBinder");
            return LoggerFactory.getLogger(aClass);
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isConstructor(String fqn) {
        return fqn.contains("init>");
    }

    public static String name(Class type) {
        if (!type.isArray()) {
            return type.getName();
        } else {
            int dim = 0;
            while (type.isArray()) {
                dim++;
                type = type.getComponentType();
            }
            return type.getName() + repeat("[]", dim);
        }
    }


    public static List<String> names(Iterable<Class<?>> types) {
        List<String> result = new ArrayList<String>();
        for (Class<?> type : types) result.add(name(type));
        return result;
    }

    public static List<String> names(Class<?>... types) {
        return names(Arrays.asList(types));
    }




    public static String name(Field field) {
        return field.getDeclaringClass().getName() + "." + field.getName();
    }

    public static String index(Class<? extends Scanner> scannerClass) { return scannerClass.getSimpleName(); }
}
