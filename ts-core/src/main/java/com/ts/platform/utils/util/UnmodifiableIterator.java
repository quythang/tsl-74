package com.ts.platform.utils.util;

import java.util.Iterator;

public abstract class UnmodifiableIterator<E> implements Iterator<E> {
    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
