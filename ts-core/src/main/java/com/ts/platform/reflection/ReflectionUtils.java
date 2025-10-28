package com.ts.platform.reflection;


import com.ts.platform.reflection.util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.ts.platform.reflection.util.Utils.isEmpty;



@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class ReflectionUtils {

    public static boolean includeObject = false;


    public static Set<Class<?>> getAllSuperTypes(final Class<?> type, Predicate<? super Class<?>>... predicates) {
        Set<Class<?>> result = Sets.newLinkedHashSet();
        if (type != null && (includeObject || !type.equals(Object.class))) {
            result.add(type);
            for (Class<?> supertype : getSuperTypes(type)) {
                result.addAll(getAllSuperTypes(supertype));
            }
        }
        return filter(result, predicates);
    }

    public static Set<Class<?>> getSuperTypes(Class<?> type) {
        Set<Class<?>> result = new LinkedHashSet<>();
        Class<?> superclass = type.getSuperclass();
        Class<?>[] interfaces = type.getInterfaces();
        if (superclass != null && (includeObject || !superclass.equals(Object.class))) result.add(superclass);
        if (interfaces.length > 0) result.addAll(Arrays.asList(interfaces));
        return result;
    }

    public static Set<Method> getAllMethods(final Class<?> type, Predicate<? super Method>... predicates) {
        Set<Method> result = Sets.newHashSet();
        for (Class<?> t : getAllSuperTypes(type)) {
            result.addAll(getMethods(t, predicates));
        }
        return result;
    }
    
    public static List<Method> getAllMethodList(final Class<?> type, Predicate<? super Method>... predicates) {
    		List<Method> result = Lists.newArrayList();
        for (Class<?> t : getAllSuperTypes(type)) {
            result.addAll(getMethodList(t, predicates));
        }
        return result;
    }

    public static Set<Method> getMethods(Class<?> t, Predicate<? super Method>... predicates) {
        return filter(t.isInterface() ? t.getMethods() : t.getDeclaredMethods(), predicates);
    }
    
    public static List<Method> getMethodList(Class<?> t, Predicate<? super Method>... predicates) {
        return filterToList(t.isInterface() ? t.getMethods() : t.getDeclaredMethods(), predicates);
    }

	public static Set<Constructor> getAllConstructors(final Class<?> type, Predicate<? super Constructor>... predicates) {
        Set<Constructor> result = Sets.newHashSet();
        for (Class<?> t : getAllSuperTypes(type)) {
            result.addAll(getConstructors(t, predicates));
        }
        return result;
    }

    public static Set<Constructor> getConstructors(Class<?> t, Predicate<? super Constructor>... predicates) {
        return ReflectionUtils.<Constructor>filter(t.getDeclaredConstructors(), predicates); //explicit needed only for jdk1.5
    }

    public static Set<Field> getAllFields(final Class<?> type, Predicate<? super Field>... predicates) {
        Set<Field> result = Sets.newHashSet();
        for (Class<?> t : getAllSuperTypes(type)) result.addAll(getFields(t, predicates));
        return result;
    }

    public static Set<Field> getFields(Class<?> type, Predicate<? super Field>... predicates) {
        return filter(type.getDeclaredFields(), predicates);
    }

    public static <T extends AnnotatedElement> Set<Annotation>  getAllAnnotations(T type, Predicate<Annotation>... predicates) {
        Set<Annotation> result = Sets.newHashSet();
        if (type instanceof Class) {
            for (Class<?> t : getAllSuperTypes((Class<?>) type)) {
                result.addAll(getAnnotations(t, predicates));
            }
        } else {
            result.addAll(getAnnotations(type, predicates));
        }
        return result;
    }

    public static <T extends AnnotatedElement> Set<Annotation> getAnnotations(T type, Predicate<Annotation>... predicates) {
        return filter(type.getDeclaredAnnotations(), predicates);
    }

    /** filter all given {@code elements} with {@code predicates}, if given */
    public static <T extends AnnotatedElement> Set<T> getAll(final Set<T> elements, Predicate<? super T>... predicates) {
        return isEmpty(predicates) ? elements : Sets.newHashSet(Iterables.filter(elements, Predicates.and(predicates)));
    }


    public static <T extends Member> Predicate<T> withName(final String name) {
        return new Predicate<T>() {
            public boolean test(T input) {
                return input != null && input.getName().equals(name);
            }
        };
    }

    public static <T extends Member> Predicate<T> withPrefix(final String prefix) {
        return new Predicate<T>() {
            public boolean test(T input) {
                return input != null && input.getName().startsWith(prefix);
            }
        };
    }


    public static <T extends AnnotatedElement> Predicate<T> withPattern(final String regex) {
        return new Predicate<T>() {
            public boolean test(T input) {
                return Pattern.matches(regex, input.toString());
            }
        };
    }

    public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Class<? extends Annotation> annotation) {
        return new Predicate<T>() {
            public boolean test(T input) {
                return input != null && input.isAnnotationPresent(annotation);
            }
        };
    }

    public static <T extends AnnotatedElement> Predicate<T> withAnnotations(final Class<? extends Annotation>... annotations) {
        return new Predicate<T>() {
            public boolean test(T input) {
                return input != null && Arrays.equals(annotations, annotationTypes(input.getAnnotations()));
            }
        };
    }

    public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Annotation annotation) {
        return new Predicate<T>() {
            public boolean test(T input) {
                return input != null && input.isAnnotationPresent(annotation.annotationType()) &&
                        areAnnotationMembersMatching(input.getAnnotation(annotation.annotationType()), annotation);
            }
        };
    }

    public static <T extends AnnotatedElement> Predicate<T> withAnnotations(final Annotation... annotations) {
        return new Predicate<T>() {
            public boolean test(T input) {
                if (input != null) {
                    Annotation[] inputAnnotations = input.getAnnotations();
                    if (inputAnnotations.length == annotations.length) {
                        for (int i = 0; i < inputAnnotations.length; i++) {
                            if (!areAnnotationMembersMatching(inputAnnotations[i], annotations[i])) return false;
                        }
                    }
                }
                return true;
            }
        };
    }

    public static Predicate<Member> withParameters(final Class<?>... types) {
        return input -> Arrays.equals(parameterTypes(input), types);
    }

    public static Predicate<Member> withParametersAssignableTo(final Class... types) {
        return input -> isAssignable(types, parameterTypes(input));
    }

    public static Predicate<Member> withParametersAssignableFrom(final Class... types) {
        return input -> isAssignable(parameterTypes(input), types);
    }

    public static Predicate<Member> withParametersCount(final int count) {
        return input -> input != null && parameterTypes(input).length == count;
    }

    public static Predicate<Member> withAnyParameterAnnotation(final Class<? extends Annotation> annotationClass) {
        return input -> input != null && Iterables.any(annotationTypes(parameterAnnotations(input)), (Predicate<Class<? extends Annotation>>) input1 -> input1.equals(annotationClass));
    }

    public static Predicate<Member> withAnyParameterAnnotation(final Annotation annotation) {
        return input -> input != null && Iterables.any(parameterAnnotations(input), new Predicate<Annotation>() {
            public boolean test(Annotation input) {
                return areAnnotationMembersMatching(annotation, input);
            }
        });
    }

    public static <T> Predicate<Field> withType(final Class<T> type) {
        return input -> input != null && input.getType().equals(type);
    }

    /** when field type assignable to given {@code type} */
    public static <T> Predicate<Field> withTypeAssignableTo(final Class<T> type) {
        return input -> input != null && type.isAssignableFrom(input.getType());
    }

    /** when method return type equal given {@code type} */
    public static <T> Predicate<Method> withReturnType(final Class<T> type) {
        return input -> input != null && input.getReturnType().equals(type);
    }

    /** when method return type assignable from given {@code type} */
    public static <T> Predicate<Method> withReturnTypeAssignableTo(final Class<T> type) {
        return input -> input != null && type.isAssignableFrom(input.getReturnType());
    }


    public static <T extends Member> Predicate<T> withModifier(final int mod) {
        return input -> input != null && (input.getModifiers() & mod) != 0;
    }


    public static Predicate<Class<?>> withClassModifier(final int mod) {
        return input -> input != null && (input.getModifiers() & mod) != 0;
    }


    public static Class<?> forName(String typeName, ClassLoader... classLoaders) {
        if (getPrimitiveNames().contains(typeName)) {
            return getPrimitiveTypes().get(getPrimitiveNames().indexOf(typeName));
        } else {
            String type;
            if (typeName.contains("[")) {
                int i = typeName.indexOf("[");
                type = typeName.substring(0, i);
                String array = typeName.substring(i).replace("]", "");

                if (getPrimitiveNames().contains(type)) {
                    type = getPrimitiveDescriptors().get(getPrimitiveNames().indexOf(type));
                } else {
                    type = "L" + type + ";";
                }

                type = array + type;
            } else {
                type = typeName;
            }

            List<ReflectionsException> reflectionsExceptions = Lists.newArrayList();
            for (ClassLoader classLoader : ClasspathHelper.classLoaders(classLoaders)) {
                if (type.contains("[")) {
                    try { return Class.forName(type, false, classLoader); }
                    catch (Throwable e) {
                        reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, e));
                    }
                }
                try { return classLoader.loadClass(type); }
                catch (Throwable e) {
                    reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, e));
                }
            }

            if (Reflections.log != null) {
                for (ReflectionsException reflectionsException : reflectionsExceptions) {
                    Reflections.log.warn("could not get type for name {} from any class loader", typeName, reflectionsException);
                }
            }

            return null;
        }
    }

    /** try to resolve all given string representation of types to a list of java types */
    public static <T> List<Class<? extends T>> forNames(final Iterable<String> classes, ClassLoader... classLoaders) {
        List<Class<? extends T>> result = new ArrayList<Class<? extends T>>();
        for (String className : classes) {
            Class<?> type = forName(className, classLoaders);
            if (type != null) {
                result.add((Class<? extends T>) type);
            }
        }
        return result;
    }

    private static Class[] parameterTypes(Member member) {
        return member != null ?
                member.getClass() == Method.class ? ((Method) member).getParameterTypes() :
                        member.getClass() == Constructor.class ? ((Constructor) member).getParameterTypes() : null : null;
    }

    private static Set<Annotation> parameterAnnotations(Member member) {
        Set<Annotation> result = Sets.newHashSet();
        Annotation[][] annotations =
                member instanceof Method ? ((Method) member).getParameterAnnotations() :
                member instanceof Constructor ? ((Constructor) member).getParameterAnnotations() : null;
        for (Annotation[] annotation : annotations) Collections.addAll(result, annotation);
        return result;
    }

    private static Set<Class<? extends Annotation>> annotationTypes(Iterable<Annotation> annotations) {
        Set<Class<? extends Annotation>> result = Sets.newHashSet();
        for (Annotation annotation : annotations) result.add(annotation.annotationType());
        return result;
    }

    private static Class<? extends Annotation>[] annotationTypes(Annotation[] annotations) {
        Class<? extends Annotation>[] result = new Class[annotations.length];
        for (int i = 0; i < annotations.length; i++) result[i] = annotations[i].annotationType();
        return result;
    }

    //
    private static List<String> primitiveNames;
    private static List<Class> primitiveTypes;
    private static List<String> primitiveDescriptors;

    private static void initPrimitives() {
        if (primitiveNames == null) {
            primitiveNames = Lists.newArrayList("boolean", "char", "byte", "short", "int", "long", "float", "double", "void");
            primitiveTypes = Lists.<Class>newArrayList(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);
            primitiveDescriptors = Lists.newArrayList("Z", "C", "B", "S", "I", "J", "F", "D", "V");
        }
    }

    private static List<String> getPrimitiveNames() { initPrimitives(); return primitiveNames; }
    private static List<Class> getPrimitiveTypes() { initPrimitives(); return primitiveTypes; }
    private static List<String> getPrimitiveDescriptors() { initPrimitives(); return primitiveDescriptors; }

    //
    static <T> Set<T> filter(final T[] elements, Predicate<? super T>... predicates) {
        return isEmpty(predicates) ? Sets.newHashSet(elements) :
                Sets.newHashSet(Iterables.filter(Arrays.asList(elements), Predicates.and(predicates)));
    }

    static <T> Set<T> filter(final Iterable<T> elements, Predicate<? super T>... predicates) {
        return isEmpty(predicates) ? Sets.newHashSet(elements) :
                Sets.newHashSet(Iterables.<T>filter(elements, Predicates.and(predicates)));
    }
    
    static <T> List<T> filterToList(final T[] elements, Predicate<? super T>... predicates) {
        return isEmpty(predicates) ? Lists.newArrayList(elements) :
                Lists.newArrayList(Iterables.filter(Arrays.asList(elements), Predicates.and(predicates)));
    }

    private static boolean areAnnotationMembersMatching(Annotation annotation1, Annotation annotation2) {
        if (annotation2 != null && annotation1.annotationType() == annotation2.annotationType()) {
            for (Method method : annotation1.annotationType().getDeclaredMethods()) {
                try {
                    if (!method.invoke(annotation1).equals(method.invoke(annotation2))) return false;
                } catch (Exception e) {
                    throw new ReflectionsException(String.format("could not invoke method %s on annotation %s", method.getName(), annotation1.annotationType()), e);
                }
            }
            return true;
        }
        return false;
    }


    private static boolean isAssignable(Class[] childClasses, Class[] parentClasses) {
        if (childClasses == null) {
            return parentClasses == null || parentClasses.length == 0;
        }
        if (childClasses.length != parentClasses.length) {
            return false;
        }
        for (int i = 0; i < childClasses.length; i++) {
            if (!parentClasses[i].isAssignableFrom(childClasses[i]) ||
                    (parentClasses[i] == Object.class && childClasses[i] != Object.class)) {
                return false;
            }
        }
        return true;
    }
}
