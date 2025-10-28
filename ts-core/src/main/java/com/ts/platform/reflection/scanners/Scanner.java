package com.ts.platform.reflection.scanners;




import com.ts.platform.reflection.Configuration;
import com.ts.platform.reflection.util.Multimap;
import com.ts.platform.reflection.vfs.Vfs;

import java.util.function.Predicate;

/**
 *
 */
public interface Scanner {

    void setConfiguration(Configuration configuration);

    Multimap<String, String> getStore();

    void setStore(Multimap<String, String> store);

    Scanner filterResultsBy(Predicate<String> filter);

    boolean acceptsInput(String file);

    Object scan(Vfs.File file,  Object classObject);

    boolean acceptResult(String fqn);
}
