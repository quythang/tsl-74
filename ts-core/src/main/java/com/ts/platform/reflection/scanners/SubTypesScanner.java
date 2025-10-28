package com.ts.platform.reflection.scanners;


import com.ts.platform.reflection.util.FilterBuilder;

import java.util.List;

public class SubTypesScanner extends AbstractScanner {

    public SubTypesScanner() {
        this(true); //exclude direct Object subtypes by default
    }


    public SubTypesScanner(boolean excludeObjectClass) {
        if (excludeObjectClass) {
            filterResultsBy(new FilterBuilder().exclude(Object.class.getName())); //exclude direct Object subtypes
        }
    }

    @SuppressWarnings({"unchecked"})
    public void scan(final Object cls) {
		String className = getMetadataAdapter().getClassName(cls);
		String superclass = getMetadataAdapter().getSuperclassName(cls);

        if (acceptResult(superclass)) {
            getStore().put(superclass, className);
        }

		for (String anInterface : (List<String>) getMetadataAdapter().getInterfacesNames(cls)) {
			if (acceptResult(anInterface)) {
                getStore().put(anInterface, className);
            }
        }
    }
}
