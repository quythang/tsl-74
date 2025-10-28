package com.ts.platform.utils.reflect;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TSLObjectProxyProvider {

    protected final Map<Class, TSLObjectProxy> objectProxies;

    public TSLObjectProxyProvider() {
        this.objectProxies = new ConcurrentHashMap<>();
    }

    public TSLObjectProxy getObjectProxy(Class<?> objectType) {
        return objectProxies.computeIfAbsent(
            objectType, this::newObjectProxy);
    }

    protected TSLObjectProxy newObjectProxy(Class<?> objectType) {
        TSLClass clazz = new TSLClass(objectType);
        Collection<TSLField> fields = getFields(clazz);
        Map<String, Function> getters = new HashMap<>();
        Map<String, BiConsumer> setters = new HashMap<>();
        Map<String, Class<?>> propertyTypes = new HashMap<>();
        for (TSLField field : fields) {
            getters.put(field.getName(), newGetter(field));
            setters.put(field.getName(), newSetter(field));
            propertyTypes.put(field.getName(), field.getType());
        }
        Map<String, String> fieldKeys = getFieldKeys(fields);
        TSLObjectProxy.Builder builder = newObjectProxyBuilder(clazz)
            .propertyKey(fieldKeys)
            .addSetters((Map) setters)
            .addGetters((Map) getters)
            .addPropertyTypes(propertyTypes);
        preBuildObjectProxy(clazz, builder);
        return builder.build();
    }

    protected void preBuildObjectProxy(
            TSLClass clazz, TSLObjectProxy.Builder builder) {
    }

    protected Collection<TSLField> getFields(TSLClass clazz) {
        return clazz.getFields(f -> isSettableField(f));
    }

    protected boolean isSettableField(TSLField field) {
        return field.isWritable()
            && !Modifier.isStatic(field.getField().getModifiers());
    }

    protected Function newGetter(TSLField field) {
        return new TSLGetterBuilder()
            .field(field)
            .build();
    }

    protected BiConsumer newSetter(TSLField field) {
        return new TSLSetterBuilder()
            .field(field)
            .build();
    }

    protected TSLObjectProxy.Builder newObjectProxyBuilder(TSLClass clazz) {
        return TSLObjectProxy.builder();
    }

    protected Map<String, String> getFieldKeys(Collection<TSLField> fields) {
        return Collections.emptyMap();
    }
}
