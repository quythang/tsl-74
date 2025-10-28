package com.ts.platform.utils.asm;



import com.ts.platform.utils.io.TSLStrings;
import com.ts.platform.utils.reflect.TSLMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TSLFunction {

    protected final TSLMethod method;
    protected final TSLBody body;
    protected String modifier = "public";
    protected Class<?> exceptionClass = null;

    public TSLFunction(Method method) {
        this(new TSLMethod(method));
    }

    public TSLFunction(TSLMethod method) {
        this.method = method;
        this.body = new TSLBody(this);
    }

    public TSLFunction modifier(String modifier) {
        this.modifier = modifier;
        return this;
    }

    public TSLFunction throwsException() {
        return throwsException(Exception.class);
    }

    public TSLFunction throwsException(Class<?> exceptionClass) {
        this.exceptionClass = exceptionClass;
        return this;
    }

    public TSLBody body() {
        return body;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
            .append(method.getDeclaration(modifier));
        if (exceptionClass != null) {
            builder.append(" throws ")
                .append(Exception.class.getTypeName());
        }
        builder.append(" {\n")
            .append(body)
            .append("}");
        return builder.toString();
    }

    public static class TSLBody {
        protected TSLFunction function;
        protected List<TSLInstruction> instructions = new ArrayList<>();

        public TSLBody(TSLFunction function) {
            this.function = function;
        }

        public TSLBody append(TSLInstruction instruction) {
            this.instructions.add(instruction);
            return this;
        }

        public TSLFunction function() {
            return function;
        }

        @Override
        public String toString() {
            return TSLStrings.join(instructions, "");
        }
    }
}
