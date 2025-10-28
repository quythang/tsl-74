package com.ts.platform.reflection.scanners;



import com.ts.platform.reflection.Configuration;
import com.ts.platform.reflection.ReflectionsException;
import com.ts.platform.reflection.adapters.MetadataAdapter;
import com.ts.platform.reflection.util.Multimap;
import com.ts.platform.reflection.util.Predicates;
import com.ts.platform.reflection.vfs.Vfs;

import java.util.function.Predicate;


@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractScanner implements Scanner {

    private Configuration configuration;
    private Multimap<String, String> store;
    private Predicate<String> resultFilter = Predicates.alwaysTrue(); //accept all by default

    public boolean acceptsInput(String file) {
        return getMetadataAdapter().acceptsInput(file);
    }

    public Object scan(Vfs.File file, Object classObject) {
        if (classObject == null) {
            try {
                classObject = configuration.getMetadataAdapter().getOrCreateClassObject(file);
            } catch (Exception e) {
                throw new ReflectionsException("could not create class object from file " + file.getRelativePath(), e);
            }
        }
        scan(classObject);
        return classObject;
    }

    public abstract void scan(Object cls);

    //
    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    public Multimap<String, String> getStore() {
        return store;
    }

    public void setStore(final Multimap<String, String> store) {
        this.store = store;
    }

    public Predicate<String> getResultFilter() {
        return resultFilter;
    }

    public void setResultFilter(Predicate<String> resultFilter) {
        this.resultFilter = resultFilter;
    }

    public Scanner filterResultsBy(Predicate<String> filter) {
        this.setResultFilter(filter); return this;
    }

    public boolean acceptResult(final String fqn) {
        return fqn != null && resultFilter.test(fqn);
    }

    protected MetadataAdapter getMetadataAdapter() {
        return configuration.getMetadataAdapter();
    }

    //
    @Override public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() {
        return getClass().hashCode();
    }
}
