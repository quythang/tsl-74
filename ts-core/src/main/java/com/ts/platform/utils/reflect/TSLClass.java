package com.ts.platform.utils.reflect;



import com.ts.platform.utils.collect.Lists;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ts.platform.utils.reflect.TSLClasses.isAnnotationPresentIncludeSuper;

@Getter
@SuppressWarnings("rawtypes")
public class TSLClass implements TSLReflectElement {

    protected final Class clazz;
    protected final List<TSLField> fields;
    protected final List<TSLMethod> methods;
    protected final List<TSLField> declaredFields;
    protected final List<TSLMethod> declaredMethods;
    protected final Map<String, TSLField> fieldsByName;
    protected final Map<String, TSLMethod> methodsByName;

    public TSLClass(Class clazz) {
        this.clazz = clazz;
        this.methods = newMethods(clazz);
        this.fields = newFields(clazz);
        this.declaredFields = newDeclaredFields(clazz);
        this.declaredMethods = newDeclaredMethods(clazz);
        this.fieldsByName = mapFieldsByName();
        this.methodsByName = mapMethodsByName();
    }

    @SuppressWarnings("unchecked")
    public <T> T newInstance() {
        return (T) TSLClasses.newInstance(clazz);
    }

    @Override
    public String getName() {
        return clazz.getName();
    }

    public int getModifiers() {
        return clazz.getModifiers();
    }

    public TSLField getField(String name) {
        return fieldsByName.get(name);
    }

    public Optional<TSLField> getField(Predicate<TSLField> predicate) {
        return fields.stream().filter(predicate).findFirst();
    }

    public TSLMethod getMethod(String name) {
        return methodsByName.get(name);
    }

    public Optional<TSLMethod> getMethod(Predicate<TSLMethod> predicate) {
        return methods.stream().filter(predicate).findFirst();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isAnnotated(Class<? extends Annotation> annClass) {
        return clazz.isAnnotationPresent(annClass);
    }

    public boolean isAnnotatedIncludeSuper(
        Class<? extends Annotation> annClass
    ) {
        return isAnnotationPresentIncludeSuper(clazz, annClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass) {
        return (T) clazz.getAnnotation(annClass);
    }

    public List<Constructor> getDeclaredConstructors() {
        return Lists.newArrayList(clazz.getDeclaredConstructors());
    }

    @SuppressWarnings("unchecked")
    public Constructor getDeclaredConstructor(Class... parameterTypes) {
        try {
            return clazz.getDeclaredConstructor(parameterTypes);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Constructor getNoArgsDeclaredConstructor() {
        for (Constructor constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 0) {
                return constructor;
            }
        }
        return null;
    }

    public Constructor getMaxArgsDeclaredConstructor() {
        Constructor[] constructors = clazz.getDeclaredConstructors();
        Constructor max = constructors[0];
        for (int i = 1; i < constructors.length; ++i) {
            if (constructors[i].getParameterCount() > max.getParameterCount()) {
                max = constructors[i];
            }
        }
        return max;
    }

    public TSLMethod getGetterMethod(String methodName) {
        Optional<TSLMethod> optional =
            getGetterMethod(m -> m.getName().equals(methodName));
        return optional.orElse(null);
    }

    public Optional<TSLMethod> getGetterMethod(Predicate<TSLMethod> predicate) {
        return methods
            .stream()
            .filter(m -> m.isGetter() && predicate.test(m))
            .findFirst();
    }

    public TSLMethod getSetterMethod(String methodName) {
        Optional<TSLMethod> optional =
            getSetterMethod(m -> m.getName().equals(methodName));
        return optional.orElse(null);
    }

    public Optional<TSLMethod> getSetterMethod(Predicate<TSLMethod> predicate) {
        return methods
            .stream()
            .filter(m -> m.isSetter() && predicate.test(m))
            .findFirst();
    }

    public List<TSLGetterMethod> getGetterMethods() {
        return getMethods(TSLMethod::isGetter, TSLGetterMethod::new);
    }

    public List<TSLGetterMethod> getGetterMethods(Predicate<TSLGetterMethod> predicate) {
        return getGetterMethods()
            .stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }

    public List<TSLSetterMethod> getSetterMethods() {
        return getMethods(TSLMethod::isSetter, TSLSetterMethod::new);
    }

    public List<TSLSetterMethod> getSetterMethods(Predicate<TSLSetterMethod> predicate) {
        return getSetterMethods()
            .stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }

    public Optional<TSLMethod> getAnnotatedGetterMethod(Class<? extends Annotation> annClass) {
        return getGetterMethod(m -> m.isAnnotated(annClass));
    }

    public Optional<TSLMethod> getAnnotatedSetterMethod(Class<? extends Annotation> annClass) {
        return getSetterMethod(m -> m.isAnnotated(annClass));
    }

    public Optional<TSLMethod> getPublicMethod(Predicate<TSLMethod> predicate) {
        return methods
            .stream()
            .filter(m -> m.isPublic() && predicate.test(m))
            .findFirst();
    }

    public List<TSLMethod> getPublicMethods() {
        return getMethods(TSLMethod::isPublic);
    }

    public List<TSLMethod> getPublicMethods(Predicate<TSLMethod> predicate) {
        return getMethods(m -> m.isPublic() && predicate.test(m));
    }

    public List<TSLMethod> getMethods(Predicate<TSLMethod> predicate) {
        return methods
            .stream()
            .filter(predicate)
            .distinct()
            .collect(Collectors.toList());
    }

    public <T extends TSLMethod> List<T> getMethods(
            Predicate<TSLMethod> predicate, Function<TSLMethod, T> creator) {
        return methods
            .stream()
            .filter(predicate)
            .flatMap(m -> Stream.of(creator.apply(m)))
            .distinct()
            .collect(Collectors.toList());
    }

    public List<TSLMethod> getDistinctMethods(Predicate<TSLMethod> predicate) {
        List<TSLMethod> allMethods = getMethods(predicate);
        return TSLMethods.filterOverriddenMethods(allMethods);
    }

    public List<TSLField> getWritableFields() {
        return getFields(TSLField::isWritable);
    }

    public List<TSLField> getPublicFields() {
        return getFields(TSLField::isPublic);
    }

    public List<TSLField> getPublicFields(Predicate<TSLField> predicate) {
        return getFields(f -> f.isPublic() && predicate.test(f));
    }

    public List<TSLField> getFields(Predicate<TSLField> predicate) {
        return fields
            .stream()
            .filter(predicate)
            .distinct()
            .collect(Collectors.toList());
    }

    public List<TSLSetterMethod> getDeclaredSetterMethods() {
        return getDeclaredMethods(TSLMethod::isSetter, TSLSetterMethod::new);
    }

    public List<TSLGetterMethod> getDeclaredGetterMethods() {
        return getDeclaredMethods(TSLMethod::isGetter, TSLGetterMethod::new);
    }

    public <T extends TSLMethod> List<T> getDeclaredMethods(
            Predicate<TSLMethod> predicate, Function<TSLMethod, T> creator) {
        return declaredMethods
            .stream()
            .filter(predicate)
            .flatMap(m -> Stream.of(creator.apply(m)))
            .distinct()
            .collect(Collectors.toList());
    }

    public List<TSLMethod> getDeclaredMethods(Predicate<TSLMethod> predicate) {
        return declaredMethods
            .stream()
            .filter(predicate)
            .distinct()
            .collect(Collectors.toList());
    }

    public Optional<TSLField> getAnnotatedField(Class<? extends Annotation> annClass) {
        return getField(m -> m.isAnnotated(annClass));
    }

    public List<TSLField> getAnnotatedFields(Class<? extends Annotation> annClass) {
        return getFields(m -> m.isAnnotated(annClass));
    }

    public Optional<TSLMethod> getAnnotatedMethod(Class<? extends Annotation> annClass) {
        return getMethod(m -> m.isAnnotated(annClass));
    }

    public List<TSLMethod> getAnnotatedMethods(Class<? extends Annotation> annClass) {
        return getMethods(m -> m.isAnnotated(annClass));
    }

    private List<TSLField> newFields(Class clazz) {
        List<TSLField> answer = new ArrayList<>();
        TSLFields.getFields(clazz).forEach(f -> answer.add(new TSLField(f)));
        return answer;
    }

    private List<TSLMethod> newMethods(Class clazz) {
        List<TSLMethod> answer = new ArrayList<>();
        TSLMethods.getMethods(clazz).forEach(m -> answer.add(new TSLMethod(m)));
        return answer;
    }

    private List<TSLField> newDeclaredFields(Class clazz) {
        List<TSLField> answer = new ArrayList<>();
        TSLFields.getDeclaredFields(clazz).forEach(f -> answer.add(new TSLField(f)));
        return answer;
    }

    private List<TSLMethod> newDeclaredMethods(Class clazz) {
        List<TSLMethod> answer = new ArrayList<>();
        TSLMethods.getDeclaredMethods(clazz).forEach(m -> answer.add(new TSLMethod(m)));
        return answer;
    }

    private Map<String, TSLField> mapFieldsByName() {
        Map<String, TSLField> map = new HashMap<>();
        fields.forEach(f -> map.put(f.getName(), f));
        return map;
    }

    private Map<String, TSLMethod> mapMethodsByName() {
        Map<String, TSLMethod> map = new HashMap<>();
        methods.forEach(m -> map.put(m.getName(), m));
        return map;
    }

    @Override
    public String toString() {
        return clazz.toString();
    }
}
