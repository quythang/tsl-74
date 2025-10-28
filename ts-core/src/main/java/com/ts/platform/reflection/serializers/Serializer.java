package com.ts.platform.reflection.serializers;


import com.ts.platform.reflection.Reflections;

import java.io.File;
import java.io.InputStream;

public interface Serializer {
    Reflections read(InputStream inputStream);

    File save(Reflections reflections, String filename);

    String toString(Reflections reflections);
}
