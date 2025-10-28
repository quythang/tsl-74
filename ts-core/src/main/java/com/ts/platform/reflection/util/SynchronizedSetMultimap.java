package com.ts.platform.reflection.util;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public record SynchronizedSetMultimap<K, V>(SetMultimap<K, V> realMultimap) implements SetMultimap<K, V> {

    @Override
    public boolean put(K key, V item) {
        synchronized (realMultimap) {
            return realMultimap.put(key, item);
        }
    }

    @Override
    public boolean putAll(Multimap<K, V> multimap) {
        synchronized (realMultimap) {
            return this.realMultimap.putAll(multimap);
        }
    }

    @Override
    public Collection<V> get(K key) {
        synchronized (realMultimap) {
            return realMultimap.get(key);
        }
    }

    @Override
    public Set<K> keySet() {
        synchronized (realMultimap) {
            return realMultimap.keySet();
        }
    }

    @Override
    public Collection<V> values() {
        synchronized (realMultimap) {
            return realMultimap.values();
        }
    }

    @Override
    public Iterable<Entry<K, V>> entries() {
        synchronized (realMultimap) {
            return realMultimap.entries();
        }
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        synchronized (realMultimap) {
            return realMultimap.asMap();
        }
    }

    @Override
    public int size() {
        synchronized (realMultimap) {
            return realMultimap.size();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (realMultimap) {
            return realMultimap.isEmpty();
        }
    }
}
