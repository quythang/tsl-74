package com.ts.platform.reflection.scanners;

import com.ts.platform.reflection.adapters.MetadataAdapter;
import com.ts.platform.reflection.util.Joiner;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MethodParameterNamesScanner extends AbstractScanner {

    @Override
    public void scan(Object cls) {
        final MetadataAdapter md = getMetadataAdapter();

        for (Object method : md.getMethods(cls)) {
            String key = md.getMethodFullKey(cls, method);
            if (acceptResult(key)) {
                LocalVariableAttribute table = (LocalVariableAttribute) ((MethodInfo) method).getCodeAttribute().getAttribute(LocalVariableAttribute.tag);
                int length = table.tableLength();
                int i = Modifier.isStatic(((MethodInfo) method).getAccessFlags()) ? 0 : 1; //skip this
                if (i < length) {
                    List<String> names = new ArrayList<String>(length - i);
                    while (i < length) names.add(((MethodInfo) method).getConstPool().getUtf8Info(table.nameIndex(i++)));
                    getStore().put(key, Joiner.on(", ").join(names));
                }
            }
        }
    }
}
