package com.ts.platform.reflection;


import com.ts.platform.reflection.scanners.Scanner;
import com.ts.platform.reflection.serializers.Serializer;
import com.ts.platform.reflection.serializers.XmlSerializer;
import com.ts.platform.reflection.util.ClasspathHelper;
import com.ts.platform.reflection.util.ConfigurationBuilder;
import com.ts.platform.reflection.util.FilterBuilder;
import com.ts.platform.reflection.util.Multimap;
import com.ts.platform.reflection.vfs.Vfs;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.ts.platform.reflection.util.Utils.findLogger;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Reflections {
     public static Logger log = findLogger(Reflections.class);

    protected final transient Configuration configuration;
    protected Store store;


    public Reflections(final Configuration configuration) {
        this.configuration = configuration;
        store = new Store(configuration);

        if (configuration.getScanners() != null && !configuration.getScanners().isEmpty()) {
            //inject to scanners
            for (Scanner scanner : configuration.getScanners()) {
                scanner.setConfiguration(configuration);
                scanner.setStore(store.getOrCreate(index(scanner.getClass())));
            }

            scan();

            if (configuration.shouldExpandSuperTypes()) {
                expandSuperTypes();
            }
        }
    }


    public Reflections(final String prefix, @Nullable final Scanner... scanners) {
        this((Object) prefix, scanners);
    }


    public Reflections(final Object... params) {
        this(ConfigurationBuilder.build(params));
    }

    protected Reflections() {
        configuration = new ConfigurationBuilder();
        store = new Store(configuration);
    }


    protected void scan() {
        if (configuration.getUrls() == null || configuration.getUrls().isEmpty()) {
            if (log != null) log.warn("given scan urls are empty. set urls in the configuration");
            return;
        }

        if (log != null && log.isDebugEnabled()) {
            log.debug("going to scan these urls:\n{}", Joiner.on("\n").join(configuration.getUrls()));
        }

        long time = System.currentTimeMillis();
        int scannedUrls = 0;
        ExecutorService executorService = configuration.getExecutorService();
        List<Future<?>> futures = Lists.newArrayList();

        for (final URL url : configuration.getUrls()) {
            try {
                if (executorService != null) {
                    futures.add(executorService.submit(new Runnable() {
                        public void run() {
                            if (log != null) {
                                log.debug("[{}] scanning {}", Thread.currentThread().toString(), url);
                            }
                            scan(url);
                        }
                    }));
                } else {
                    scan(url);
                }
                scannedUrls++;
            } catch (ReflectionsException e) {
                if (log != null) {
                    log.debug("could not create Vfs.Dir from url. ignoring the exception and continuing", e);
                }
            }
        }

        //todo use CompletionService
        if (executorService != null) {
            for (Future future : futures) {
                try { future.get(); } catch (Exception e) { throw new RuntimeException(e); }
            }
        }

        time = System.currentTimeMillis() - time;

        //gracefully shutdown the parallel scanner executor service.
        if (executorService != null) {
            executorService.shutdown();
        }

        if (log != null) {
            int keys = 0;
            int values = 0;
            for (String index : store.keySet()) {
                keys += store.get(index).keySet().size();
                values += store.get(index).size();
            }

            log.debug(format("Reflections took %d ms to scan %d urls, producing %d keys and %d values %s",
                    time, scannedUrls, keys, values,
                    executorService != null && executorService instanceof ThreadPoolExecutor ?
                            format("[using %d cores]", ((ThreadPoolExecutor) executorService).getMaximumPoolSize()) : ""));
        }
    }

    protected void scan(URL url) {
        Vfs.Dir dir = Vfs.fromURL(url);
        try {
            for (final Vfs.File file : dir.getFiles()) {
                // scan if inputs filter accepts file relative path or fqn
                Predicate<String> inputsFilter = configuration.getInputsFilter();
                String path = file.getRelativePath();
                String fqn = path.replace('/', '.');
                if (inputsFilter == null || inputsFilter.test(path) || inputsFilter.test(fqn)) {
                    Object classObject = null;
                    for (Scanner scanner : configuration.getScanners()) {
                        try {
                            if (scanner.acceptsInput(path) || scanner.acceptsInput(fqn)) {
                                classObject = scanner.scan(file, classObject);
                            }
                        } catch (Exception e) {
                            if (log != null) {
                                // SLF4J will filter out Throwables from the format string arguments.
                                log.debug("could not scan file {} in url {} with scanner {}", file.getRelativePath(), url.toExternalForm(), scanner.getClass().getSimpleName(), e);
                            }
                        }
                    }
                }
            }
        } finally {
            dir.close();
        }
    }


    public static Reflections collect() {
        return collect("META-INF/reflections/", new FilterBuilder().include(".*-reflections.xml"));
    }


    public static Reflections collect(final String packagePrefix, final Predicate<String> resourceNameFilter,  Serializer... optionalSerializer) {
        Serializer serializer = optionalSerializer != null && optionalSerializer.length == 1 ? optionalSerializer[0] : new XmlSerializer();

        Collection<URL> urls = ClasspathHelper.forPackage(packagePrefix);
        if (urls.isEmpty()) return null;
        long start = System.currentTimeMillis();
        final Reflections reflections = new Reflections();
        Iterable<Vfs.File> files = Vfs.findFiles(urls, packagePrefix, resourceNameFilter);
        for (final Vfs.File file : files) {
            InputStream inputStream = null;
            try {
                inputStream = file.openInputStream();
                reflections.merge(serializer.read(inputStream));
            } catch (IOException e) {
                throw new ReflectionsException("could not merge " + file, e);
            } finally {
                close(inputStream);
            }
        }

        if (log != null) {
            Store store = reflections.getStore();
            int keys = 0;
            int values = 0;
            for (String index : store.keySet()) {
                keys += store.get(index).keySet().size();
                values += store.get(index).size();
            }

            log.debug(format("Reflections took %d ms to collect %d url%s, producing %d keys and %d values [%s]",
                    System.currentTimeMillis() - start, urls.size(), urls.size() > 1 ? "s" : "", keys, values, Joiner.on(", ").join(urls)));
        }
        return reflections;
    }


    public Reflections collect(final InputStream inputStream) {
        try {
            merge(configuration.getSerializer().read(inputStream));
            if (log != null) log.debug("Reflections collected metadata from input stream using serializer " + configuration.getSerializer().getClass().getName());
        } catch (Exception ex) {
            throw new ReflectionsException("could not merge input stream", ex);
        }

        return this;
    }

    /** merges saved Reflections resources from the given file, using the serializer configured in this instance's Configuration
     * <p> useful if you know the serialized resource location and prefer not to look it up the classpath
     * */
    public Reflections collect(final File file) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return collect(inputStream);
        } catch (FileNotFoundException e) {
            throw new ReflectionsException("could not obtain input stream from file " + file, e);
        } finally {
            Utils.close(inputStream);
        }
    }

    /**
     * merges a Reflections instance metadata into this instance
     */
    public Reflections merge(final Reflections reflections) {
        if (reflections.store != null) {
            for (String indexName : reflections.store.keySet()) {
                Multimap<String, String> index = reflections.store.get(indexName);
                for (String key : index.keySet()) {
                    for (String string : index.get(key)) {
                        store.getOrCreate(indexName).put(key, string);
                    }
                }
            }
        }
        return this;
    }

    /**
     * expand super types after scanning, for super types that were not scanned.
     * this is helpful in finding the transitive closure without scanning all 3rd party dependencies.
     * it uses {@link ReflectionUtils#getSuperTypes(Class)}.
     * <p>
     * for example, for classes A,B,C where A supertype of B, B supertype of C:
     * <ul>
     *     <li>if scanning C resulted in B (B->C in store), but A was not scanned (although A supertype of B) - then getSubTypes(A) will not return C</li>
     *     <li>if expanding supertypes, B will be expanded with A (A->B in store) - then getSubTypes(A) will return C</li>
     * </ul>
     */
    public void expandSuperTypes() {
        if (store.keySet().contains(index(SubTypesScanner.class))) {
            Multimap<String, String> mmap = store.get(index(SubTypesScanner.class));
            Sets.SetView<String> keys = Sets.difference(mmap.keySet(), Sets.newHashSet(mmap.values()));
            Multimap<String, String> expand = HashMultimap.create();
            for (String key : keys) {
                final Class<?> type = forName(key, loaders());
                if (type != null) {
                    expandSupertypes(expand, key, type);
                }
            }
            mmap.putAll(expand);
        }
    }

    private void expandSupertypes(Multimap<String, String> mmap, String key, Class<?> type) {
        for (Class<?> supertype : ReflectionUtils.getSuperTypes(type)) {
            if (mmap.put(supertype.getName(), key)) {
                if (log != null) log.debug("expanded subtype {} -> {}", supertype.getName(), key);
                expandSupertypes(mmap, supertype.getName(), supertype);
            }
        }
    }

    //query
    /**
     * gets all sub types in hierarchy of a given type
     * <p/>depends on SubTypesScanner configured
     */
    public <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type) {
    		String index = index(SubTypesScanner.class);
    		List<String> typeNames = Arrays.asList(type.getName());
    		Iterable<String> all = store.getAll(index, typeNames);
    		ClassLoader[] loaders = loaders();
    		List<Class<? extends T>> classes = ReflectionUtils.<T>forNames(all, loaders);
        return Sets.newHashSet(classes);
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link Inherited} is not honored by default.
     * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and its sub types
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation) {
        return getTypesAnnotatedWith(annotation, false);
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link Inherited} is honored according to given honorInherited.
     * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and it's sub types
     * <p>when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation, boolean honorInherited) {
        Iterable<String> annotated = store.get(index(TypeAnnotationsScanner.class), annotation.getName());
        Iterable<String> classes = getAllAnnotated(annotated, annotation.isAnnotationPresent(Inherited.class), honorInherited);
        return Sets.newHashSet(Iterables.concat(forNames(annotated, loaders()), forNames(classes, loaders())));
    }

    /**
     * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
     * <p>{@link Inherited} is not honored by default
     * <p/>depends on TypeAnnotationsScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation) {
        return getTypesAnnotatedWith(annotation, false);
    }

    /**
     * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
     * <p>{@link Inherited} is honored according to given honorInherited
     * <p/>depends on TypeAnnotationsScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation, boolean honorInherited) {
        Iterable<String> annotated = store.get(index(TypeAnnotationsScanner.class), annotation.annotationType().getName());
        Iterable<Class<?>> filter = Iterables.filter(forNames(annotated, loaders()), withAnnotation(annotation));
        Iterable<String> classes = getAllAnnotated(names(filter), annotation.annotationType().isAnnotationPresent(Inherited.class), honorInherited);
        return Sets.newHashSet(concat(filter, forNames(filter(classes, not(in(Sets.newHashSet(annotated)))), loaders())));
    }

    protected Iterable<String> getAllAnnotated(Iterable<String> annotated, boolean inherited, boolean honorInherited) {
        if (honorInherited) {
            if (inherited) {
                Iterable<String> subTypes = store.get(index(SubTypesScanner.class), filter(annotated, new Predicate<String>() {
                    public boolean test(@Nullable String input) {
                        final Class<?> type = forName(input, loaders());
                        return type != null && !type.isInterface();
                    }
                }));
                return Iterables.concat(subTypes, store.getAll(index(SubTypesScanner.class), subTypes));
            } else {
                return annotated;
            }
        } else {
            Iterable<String> subTypes = Iterables.concat(annotated, store.getAll(index(TypeAnnotationsScanner.class), annotated));
            return Iterables.concat(subTypes, store.getAll(index(SubTypesScanner.class), subTypes));
        }
    }

    /**
     * get all methods annotated with a given annotation
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Method> getMethodsAnnotatedWith(final Class<? extends Annotation> annotation) {
        Iterable<String> methods = store.get(index(MethodAnnotationsScanner.class), annotation.getName());
        return getMethodsFromDescriptors(methods, loaders());
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Method> getMethodsAnnotatedWith(final Annotation annotation) {
    		Set<Method> methods = getMethodsAnnotatedWith(annotation.annotationType());
    		Predicate<AnnotatedElement> predicate = withAnnotation(annotation);
    		Set<Method> answer = filter(methods, predicate);
    		return answer;
    }

    /** get methods with parameter types matching given {@code types}*/
    public Set<Method> getMethodsMatchParams(Class<?>... types) {
        return getMethodsFromDescriptors(store.get(index(MethodParameterScanner.class), names(types).toString()), loaders());
    }

    /** get methods with return type match given type */
    public Set<Method> getMethodsReturn(Class returnType) {
        return getMethodsFromDescriptors(store.get(index(MethodParameterScanner.class), names(returnType)), loaders());
    }

    /** get methods with any parameter annotated with given annotation */
    public Set<Method> getMethodsWithAnyParamAnnotated(Class<? extends Annotation> annotation) {
        return getMethodsFromDescriptors(store.get(index(MethodParameterScanner.class), annotation.getName()), loaders());

    }

    /** get methods with any parameter annotated with given annotation, including annotation member values matching */
    public Set<Method> getMethodsWithAnyParamAnnotated(Annotation annotation) {
        return filter(getMethodsWithAnyParamAnnotated(annotation.annotationType()), withAnyParameterAnnotation(annotation));
    }

    /**
     * get all constructors annotated with a given annotation
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Constructor> getConstructorsAnnotatedWith(final Class<? extends Annotation> annotation) {
        Iterable<String> methods = store.get(index(MethodAnnotationsScanner.class), annotation.getName());
        return getConstructorsFromDescriptors(methods, loaders());
    }

    /**
     * get all constructors annotated with a given annotation, including annotation member values matching
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Constructor> getConstructorsAnnotatedWith(final Annotation annotation) {
    		Set<Constructor> constructors = getConstructorsAnnotatedWith(annotation.annotationType());
    		Predicate<AnnotatedElement> predicate = withAnnotation(annotation);
    		Set<Constructor> answer = filter(constructors, predicate);
    		return answer;
    }

    /** get constructors with parameter types matching given {@code types}*/
    public Set<Constructor> getConstructorsMatchParams(Class<?>... types) {
        return getConstructorsFromDescriptors(store.get(index(MethodParameterScanner.class), names(types).toString()), loaders());
    }

    /** get constructors with any parameter annotated with given annotation */
    public Set<Constructor> getConstructorsWithAnyParamAnnotated(Class<? extends Annotation> annotation) {
        return getConstructorsFromDescriptors(store.get(index(MethodParameterScanner.class), annotation.getName()), loaders());
    }

    /** get constructors with any parameter annotated with given annotation, including annotation member values matching */
    public Set<Constructor> getConstructorsWithAnyParamAnnotated(Annotation annotation) {
        return filter(getConstructorsWithAnyParamAnnotated(annotation.annotationType()), withAnyParameterAnnotation(annotation));
    }

    /**
     * get all fields annotated with a given annotation
     * <p/>depends on FieldAnnotationsScanner configured
     */
    public Set<Field> getFieldsAnnotatedWith(final Class<? extends Annotation> annotation) {
        final Set<Field> result = Sets.newHashSet();
        for (String annotated : store.get(index(FieldAnnotationsScanner.class), annotation.getName())) {
            result.add(getFieldFromString(annotated, loaders()));
        }
        return result;
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on FieldAnnotationsScanner configured
     */
    public Set<Field> getFieldsAnnotatedWith(final Annotation annotation) {
    		Set<Field> fields = getFieldsAnnotatedWith(annotation.annotationType());
    		Predicate<AnnotatedElement> predicate = withAnnotation(annotation);
        return filter(fields, predicate);
    }

    /** get resources relative paths where simple name (key) matches given namePredicate
     * <p>depends on ResourcesScanner configured
     * */
    public Set<String> getResources(final Predicate<String> namePredicate) {
        Iterable<String> resources = Iterables.filter(store.get(index(ResourcesScanner.class)).keySet(), namePredicate);
        return Sets.newHashSet(store.get(index(ResourcesScanner.class), resources));
    }

    /** get resources relative paths where simple name (key) matches given regular expression
     * <p>depends on ResourcesScanner configured
     * <pre>Set<String> xmls = reflections.getResources(".*\\.xml");</pre>
     */
    public Set<String> getResources(final Pattern pattern) {
        return getResources(new Predicate<String>() {
            public boolean test(String input) {
                return pattern.matcher(input).matches();
            }
        });
    }

    /** get parameter names of given {@code method}
     * <p>depends on MethodParameterNamesScanner configured
     */
    public List<String> getMethodParamNames(Method method) {
        Iterable<String> names = store.get(index(MethodParameterNamesScanner.class), name(method));
        return !Iterables.isEmpty(names) ? Arrays.asList(Iterables.getOnlyElement(names).split(", ")) : Arrays.<String>asList();
    }

    /** get parameter names of given {@code constructor}
     * <p>depends on MethodParameterNamesScanner configured
     */
    public List<String> getConstructorParamNames(Constructor constructor) {
        Iterable<String> names = store.get(index(MethodParameterNamesScanner.class), Utils.name(constructor));
        return !Iterables.isEmpty(names) ? Arrays.asList(Iterables.getOnlyElement(names).split(", ")) : Arrays.<String>asList();
    }

    /** get all given {@code field} usages in methods and constructors
     * <p>depends on MemberUsageScanner configured
     */
    public Set<Member> getFieldUsage(Field field) {
        return getMembersFromDescriptors(store.get(index(MemberUsageScanner.class), name(field)));
    }

    /** get all given {@code method} usages in methods and constructors
     * <p>depends on MemberUsageScanner configured
     */
    public Set<Member> getMethodUsage(Method method) {
        return getMembersFromDescriptors(store.get(index(MemberUsageScanner.class), name(method)));
    }

    /** get all given {@code constructors} usages in methods and constructors
     * <p>depends on MemberUsageScanner configured
     */
    public Set<Member> getConstructorUsage(Constructor constructor) {
        return getMembersFromDescriptors(store.get(index(MemberUsageScanner.class), name(constructor)));
    }

    /** get all types scanned. this is effectively similar to getting all subtypes of Object.
     * <p>depends on SubTypesScanner configured with {@code SubTypesScanner(false)}, otherwise {@code ReflectionsException} is thrown
     * <p><i>note using this might be a bad practice. it is better to get types matching some criteria,
     * such as {@link #getSubTypesOf(Class)} or {@link #getTypesAnnotatedWith(Class)}</i>
     * @return Set of String, and not of Class, in order to avoid definition of all types in PermGen
     */
    public Set<String> getAllTypes() {
        Set<String> allTypes = Sets.newHashSet(store.getAll(index(SubTypesScanner.class), Object.class.getName()));
        if (allTypes.isEmpty()) {
            throw new ReflectionsException("Couldn't find subtypes of Object. " +
                    "Make sure SubTypesScanner initialized to include Object class - new SubTypesScanner(false)");
        }
        return allTypes;
    }

    /** returns the {@link com.tvd12.reflections.Store} used for storing and querying the metadata */
    public Store getStore() {
        return store;
    }

    /** returns the {@link com.tvd12.reflections.Configuration} object of this instance */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * serialize to a given directory and filename
     * <p>* it is preferred to specify a designated directory (for example META-INF/reflections),
     * so that it could be found later much faster using the load method
     * <p>see the documentation for the save method on the configured {@link com.tvd12.reflections.serializers.Serializer}
     */
    public File save(final String filename) {
        return save(filename, configuration.getSerializer());
    }

    /**
     * serialize to a given directory and filename using given serializer
     * <p>* it is preferred to specify a designated directory (for example META-INF/reflections),
     * so that it could be found later much faster using the load method
     */
    public File save(final String filename, final Serializer serializer) {
        File file = serializer.save(this, filename);
        if (log != null) //noinspection ConstantConditions
            log.debug("Reflections successfully saved in " + file.getAbsolutePath() + " using " + serializer.getClass().getSimpleName());
        return file;
    }

    private ClassLoader[] loaders() { return configuration.getClassLoaders(); }
}
