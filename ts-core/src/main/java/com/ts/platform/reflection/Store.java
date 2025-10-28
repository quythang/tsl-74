package com.ts.platform.reflection;

import com.ts.platform.reflection.util.*;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


public class Store {

    private transient boolean concurrent;
    private final Map<String, Multimap<String, String>> storeMap;

    //used via reflection
    protected Store() {
        storeMap = new HashMap<String, Multimap<String, String>>();
        concurrent = false;
    }

    public Store(Configuration configuration) {
        storeMap = new HashMap<String, Multimap<String, String>>();
        concurrent = configuration.getExecutorService() != null;
    }

    /** return all indices */
    public Set<String> keySet() {
        return storeMap.keySet();
    }

    /** get or create the multimap object for the given {@code index} */
    public Multimap<String, String> getOrCreate(String index) {
        Multimap<String, String> mmap = storeMap.get(index);
        if (mmap == null) {
            SetMultimap<String, String> multimap =
                    Multimaps.newSetMultimap(new HashMap<String, Collection<String>>(),
                            new Supplier<Set<String>>() {
                                public Set<String> get() {
                                    return Sets.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
                                }
                            });
            mmap = concurrent ? Multimaps.synchronizedSetMultimap(multimap) : multimap;
            storeMap.put(index,mmap);
        }
        return mmap;
    }

    public Multimap<String, String> get(String index) {
        Multimap<String, String> mmap = storeMap.get(index);
        if (mmap == null) {
            throw new ReflectionsException("Scanner " + index + " was not configured");
        }
        return mmap;
    }

    public Iterable<String> get(String index, String... keys) {
        return get(index, Arrays.asList(keys));
    }

    public Iterable<String> get(String index, Iterable<String> keys) {
        Multimap<String, String> mmap = get(index);
        IterableChain<String> result = new IterableChain<String>();
        for (String key : keys) {
            result.addAll(mmap.get(key));
        }
        return result;
    }

    private Iterable<String> getAllIncluding(String index, Iterable<String> keys, IterableChain<String> result) {
        result.addAll(keys);
        for (String key : keys) {
            Iterable<String> values = get(index, key);
            if (values.iterator().hasNext()) {
                getAllIncluding(index, values, result);
            }
        }
        return result;
    }

    public Iterable<String> getAll(String index, String key) {
        return getAllIncluding(index, get(index, key), new IterableChain<String>());
    }

    public Iterable<String> getAll(String index, Iterable<String> keys) {
    		Iterable<String> storedValues = get(index, keys);
    		IterableChain<String> result = new IterableChain<String>();
        return getAllIncluding(index, storedValues, result);
    }

    @SuppressWarnings("unchecked")
    private static class IterableChain<T> implements Iterable<T> {
		private final List<Iterable<T>> chain = Lists.newArrayList();

        private void addAll(Iterable<T> iterable) { chain.add(iterable); }

        public Iterator<T> iterator() { return Iterables.concat(chain).iterator(); }
    }
}
