package com.ts.platform.utils.asm;

import com.tvd12.ezyfox.io.EzyStrings;
import com.tvd12.ezyfox.reflect.EzyClasses;
import com.tvd12.ezyfox.reflect.EzyTypes;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class TSLInstruction {

    protected final String end;
    protected final boolean semicolon;
    protected final StringBuilder builder = new StringBuilder();

    protected static final Map<Class, Class> PRIMITIVE_WRAPPER_TYPES =
        EzyTypes.PRIMITIVE_WRAPPER_TYPES_MAP;
    private static final Map<Class, String> FETCH_PRIMITIVE_METHODS =
        fetchPrimitiveMethods();

    public TSLInstruction() {
        this("");
    }

    public TSLInstruction(String begin) {
        this(begin, "");
    }

    public TSLInstruction(String begin, String end) {
        this(begin, end, true);
    }

    public TSLInstruction(String begin, String end, boolean semicolon) {
        this.end = end;
        this.builder.append(begin);
        this.semicolon = semicolon;
    }

    private static Map<Class, String> fetchPrimitiveMethods() {
        Map<Class, String> map = new HashMap<>();
        map.put(Boolean.class, "booleanValue()");
        map.put(Byte.class, "byteValue()");
        map.put(Character.class, "charValue()");
        map.put(Double.class, "doubleValue()");
        map.put(Float.class, "floatValue()");
        map.put(Integer.class, "intValue()");
        map.put(Long.class, "longValue()");
        map.put(Short.class, "shortValue()");
        return map;
    }

    public TSLInstruction equal() {
        builder.append(" = ");
        return this;
    }

    public TSLInstruction finish() {
        builder.append(";");
        return this;
    }

    public TSLInstruction constructor(Class clazz) {
        return clazz(clazz).append("()");
    }

    public TSLInstruction append(Object value) {
        builder.append(value.toString());
        return this;
    }

    public TSLInstruction append(String str) {
        builder.append(str);
        return this;
    }

    public TSLInstruction append(TSLInstruction instruction) {
        return append(instruction.toString());
    }

    public TSLInstruction string(String str) {
        builder.append("\"").append(str).append("\"");
        return this;
    }

    public TSLInstruction clazz(Class clazz) {
        return clazz(clazz, false);
    }

    public TSLInstruction clazz(Class clazz, boolean extension) {
        append(clazz.getTypeName());
        return extension ? append(".class") : this;
    }

    public TSLInstruction dot() {
        return append(".");
    }

    public TSLInstruction comma() {
        return append(", ");
    }

    public TSLInstruction bracketopen() {
        return append("(");
    }

    public TSLInstruction bracketclose() {
        return append(")");
    }

    public TSLInstruction brackets(Class clazz) {
        builder.append("(").append(clazz.getTypeName()).append(")");
        return this;
    }

    public TSLInstruction brackets(String expression) {
        builder.append("(").append(expression).append(")");
        return this;
    }

    public TSLInstruction answer() {
        return append("return ");
    }

    public TSLInstruction variable(Class type) {
        return variable(type, EzyClasses.getVariableName(type));
    }

    public TSLInstruction variable(Class type, String name) {
        builder.append(type.getTypeName()).append(" ").append(name);
        return this;
    }

    public TSLInstruction cast(Class type, String expression) {
        if (PRIMITIVE_WRAPPER_TYPES.containsKey(type)) {
            return castPrimitive(type, expression);
        }
        return castNormal(type, expression);
    }

    protected TSLInstruction castNormal(Class type, String expression) {
        builder
            .append("(")
            .append("(").append(type.getTypeName()).append(")")
            .append("(").append(expression).append(")")
            .append(")");
        return this;
    }

    protected TSLInstruction castPrimitive(Class type, String expression) {
        Class wrapperType = PRIMITIVE_WRAPPER_TYPES.get(type);
        String primitiveValueMethod = FETCH_PRIMITIVE_METHODS.get(wrapperType);
        castNormal(wrapperType, expression);
        dot();
        append(primitiveValueMethod);
        return this;
    }

    public TSLInstruction function(String method, String... args) {
        return append(method)
            .brackets(EzyStrings.join(args, ", "));
    }

    public TSLInstruction invoke(String object, String method, String... args) {
        return append(object)
            .dot()
            .function(method, args);
    }

    public TSLInstruction valueOf(Class type, String expression) {
        return valueOf(type, expression, false);
    }

    public TSLInstruction valueOf(Class type, String expression, boolean forceCast) {
        Class wrapperType = PRIMITIVE_WRAPPER_TYPES.get(type);
        if (wrapperType != null) {
            return clazz(wrapperType).append(".valueOf").brackets(expression);
        }
        if (forceCast) {
            return castNormal(type, expression);
        }
        return append(expression);
    }

    public TSLInstruction defaultValue(Class type) {
        if (type == boolean.class) {
            return append("false");
        }
        if (type == byte.class) {
            return append("(byte)0");
        }
        if (type == char.class) {
            return append("(char)0");
        }
        if (type == double.class) {
            return append("0D");
        }
        if (type == float.class) {
            return append("0F");
        }
        if (type == int.class) {
            return append("0");
        }
        if (type == long.class) {
            return append("0L");
        }
        if (type == short.class) {
            return append("(short)0");
        }
        return append("null");
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean autoFinish) {
        String string = builder.toString();
        if (autoFinish && semicolon) {
            string = string.endsWith(";") ? string : string + ";";
        }
        return string + end;
    }
}
