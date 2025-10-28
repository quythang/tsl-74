package com.ts.platform.utils.reflect;

import com.ts.platform.utils.asm.TSLFunction;
import com.ts.platform.utils.asm.TSLInstruction;
import com.ts.platform.utils.functional.TSLBuilder;
import com.ts.platform.utils.util.TSLLoggable;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public class TSLGetterBuilder extends TSLLoggable implements TSLBuilder<Function> {

    protected static final AtomicInteger COUNT = new AtomicInteger(0);
    @Setter
    protected static boolean debug;
    protected TSLField field;
    protected TSLMethod method;
    protected Class<?> declaringClass;

    public TSLGetterBuilder field(TSLField field) {
        this.field = field;
        this.declaringClass = field.getField().getDeclaringClass();
        return this;
    }

    public TSLGetterBuilder method(TSLMethod method) {
        this.method = method;
        this.declaringClass = method.getMethod().getDeclaringClass();
        return this;
    }

    @Override
    public Function build() {
        try {
            return doBuild();
        } catch (Exception e) {
            throw new IllegalArgumentException("build getter: " + field + " error", e);
        }
    }

    protected Function doBuild() throws Exception {
        String implClassName = getImplClassName();
        ClassPool pool = ClassPool.getDefault();
        CtClass implClass = pool.makeClass(implClassName);
        String acceptMethodContent = makeApplyMethodContent();
        printMethodContent(acceptMethodContent);
        implClass.addMethod(CtNewMethod.make(acceptMethodContent, implClass));
        implClass.setInterfaces(new CtClass[]{pool.get(Function.class.getName())});
        Class<?> answerClass = implClass.toClass();
        implClass.detach();
        return TSLClasses.newInstance(answerClass);
    }

    protected String makeApplyMethodContent() {
        Class<?> type;
        String methodName;
        if (field != null) {
            type = field.getType();
            methodName = field.getGetterMethod();
        } else {
            type = method.getReturnType();
            methodName = method.getName();
        }
        return new TSLFunction(getEntityTypeMethod())
            .body()
            .append(new TSLInstruction("\t", "\n")
                .append(type.getTypeName())
                .append(" answer = ")
                .cast(declaringClass, "arg0")
                .dot()
                .append(methodName)
                .bracketopen()
                .bracketclose())
            .append(new TSLInstruction("\t", "\n")
                .answer()
                .valueOf(type, "answer"))
            .function()
            .toString();
    }

    protected TSLMethod getEntityTypeMethod() {
        Method method = TSLMethods.getMethod(Function.class, "apply", Object.class);
        return new TSLMethod(method);
    }

    protected String getImplClassName() {
        String name = field != null ? field.getName() : method.getFieldName();
        return declaringClass.getName() + "$" + name + "$EzyObjectProxy$GetterImpl$" + COUNT.incrementAndGet();
    }

    protected void printMethodContent(String methodContent) {
        if (debug) {
            logger.info("method content \n{}", methodContent);
        }
    }
}
