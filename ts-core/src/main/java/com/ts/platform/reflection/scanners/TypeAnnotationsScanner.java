package com.ts.platform.reflection.scanners;

import java.lang.annotation.Inherited;
import java.util.List;

@SuppressWarnings({"unchecked"})
public class TypeAnnotationsScanner extends AbstractScanner {
    public void scan(final Object cls) {
		final String className = getMetadataAdapter().getClassName(cls);

        for (String annotationType : (List<String>) getMetadataAdapter().getClassAnnotationNames(cls)) {

            if (acceptResult(annotationType) ||
                annotationType.equals(Inherited.class.getName())) { //as an exception, accept Inherited as well
                getStore().put(annotationType, className);
            }
        }
    }

}
