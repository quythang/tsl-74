package com.ts.platform.reflection.util;

import java.util.*;
import java.util.function.Supplier;

public class HashMultimap<K, V> extends AbstractMultimap<K, V> {

	public HashMultimap(
			Map<K, ? extends Collection<V>> map, 
			Supplier<? extends Collection<V>> factory) {
		super(map, factory);
	}

	public static <K, V> HashMultimap<K, V> create() {
		return new HashMultimap<K, V>(
				new HashMap<>(),
				new Supplier<Set<V>>() {
					@Override
					public Set<V> get() {
						return new HashSet<>();
					}
				});
	}
	
}
