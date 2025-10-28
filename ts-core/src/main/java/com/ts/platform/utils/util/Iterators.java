package com.ts.platform.utils.util;

import java.util.Iterator;
import java.util.function.Predicate;

public class Iterators {
    public static Iterator filter(final Iterator unfiltered, final Predicate retainIfTrue) {
        return new AbstractIterator() {
            protected Object computeNext() {
                while(true) {
                    if (unfiltered.hasNext()) {
                        Object element = unfiltered.next();
                        if (!retainIfTrue.test(element)) {
                            continue;
                        }

                        return element;
                    }

                    return this.endOfData();
                }
            }
        };
    }
}
