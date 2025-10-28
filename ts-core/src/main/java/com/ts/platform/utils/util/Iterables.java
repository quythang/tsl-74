package com.ts.platform.utils.util;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Iterables {

    public static <T> Iterable<T> filter(final Iterable<T> unfiltered, final Predicate<T> retainIfTrue) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return Iterators.filter(unfiltered.iterator(), retainIfTrue);
            }

            public void forEach(Consumer<? super T> action) {
                unfiltered.forEach((a) -> {
                    if (retainIfTrue.test(a)) {
                        action.accept(a);
                    }

                });
            }

            public Spliterator<T> spliterator() {
                return CollectSpliterators.filter(unfiltered.spliterator(), retainIfTrue);
            }
        };
    }
}
