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
import java.util.function.BiConsumer;

@SuppressWarnings("rawtypes")
public class TSLSetterBuilder extends TSLLoggable implements TSLBuilder<BiConsumer> {

    protected static final AtomicInteger COUNT = new AtomicInteger(0);
    @Setter
    protected static boolean debug;
    protected TSLField field;
    protected TSLMethod method;
    protected Class<?> declaringClass;

    public TSLSetterBuilder field(TSLField field) {
        this.field = field;
        this.declaringClass = field.getField().getDeclaringClass();
        return this;
    }

    public TSLSetterBuilder method(TSLMethod method) {
        this.method = method;
        this.declaringClass = method.getMethod().getDeclaringClass();
        return this;
    }

    @Override
    public BiConsumer build() {
        try {
            return doBuild();
        } catch (Exception e) {
            throw new IllegalArgumentException("build setter: " + field + " error", e);
        }
    }

    protected BiConsumer doBuild() throws Exception {
        String implClassName = getImplClassName();
        ClassPool pool = ClassPool.getDefault();
        CtClass implClass = pool.makeClass(implClassName);
        String acceptMethodContent = makeAcceptMethodContent();
        printMethodContent(acceptMethodContent);
        implClass.addMethod(CtNewMethod.make(acceptMethodContent, implClass));
        implClass.setInterfaces(new CtClass[]{pool.get(BiConsumer.class.getName())});
        Class answerClass = implClass.toClass();
        implClass.detach();
        return TSLClasses.newInstance(answerClass);
    }

    protected String makeAcceptMethodContent() {
        Class<?> type;
        String methodName;
        if (field != null) {
            type = field.getType();
            methodName = field.getSetterMethod();
        } else {
            type = method.getParameterTypes()[0];
            methodName = method.getName();
        }
        return new TSLFunction(getEntityTypeMethod())
            .body()
            .append(new TSLInstruction("\t", "\n")
                .cast(declaringClass, "arg0")
                .dot()
                .append(methodName)
                .bracketopen()
                .cast(type, "arg1")
                .bracketclose())
            .function()
            .toString();
    }

    protected TSLMethod getEntityTypeMethod() {
        Method method = TSLMethods.getMethod(BiConsumer.class, "accept", Object.class, Object.class);
        return new TSLMethod(method);
    }

    protected String getImplClassName() {
        String name = field != null ? field.getName() : method.getFieldName();
        return declaringClass.getName() + "$" + name + "$EzyObjectProxy$SetterImpl$" + COUNT.incrementAndGet();
    }

    protected void printMethodContent(String methodContent) {
        if (debug) {
            logger.info("method content \n{}", methodContent);
        }
    }
}
