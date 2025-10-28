package com.ts.platform.utils.functional;

public interface TSLBuilder<T> {

    /**
     * build a product.
     *
     * @return the constructed product
     */
    T build();
}
