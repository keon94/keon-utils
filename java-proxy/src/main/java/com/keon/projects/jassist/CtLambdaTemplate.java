package com.keon.projects.jassist;

import javassist.CannotCompileException;

import java.lang.reflect.Method;
import java.util.Arrays;

public class CtLambdaTemplate<T> implements CtTemplate<CtLambdaTemplate<T>> {

    private static int LAMBDA_COUNTER = 0;

    private final CtClassTemplate template;
    private final Class<T> type;

    public CtLambdaTemplate(final Class<T> type) {
        template = new CtClassTemplate("LAMBDA_" + LAMBDA_COUNTER);
        this.type = type;
        LAMBDA_COUNTER++;
    }

    @Override
    public Class<T> createClass() throws CannotCompileException {
        return (Class<T>) template.createClass();
    }

    @Override
    public CtLambdaTemplate<T> autoImportDiscovery(boolean auto) {
        template.autoImportDiscovery(auto);
        return this;
    }

    @Override
    public CtLambdaTemplate<T> addImports(String... imps) {
        template.addImports(imps);
        return this;
    }

    @Override
    public CtLambdaTemplate<T> addImports(Class<?>... classes) {
        template.addImports(classes);
        return this;
    }

    @Override
    public CtLambdaTemplate<T> addImplements(String ifcName) {
        template.addImplements(ifcName);
        return this;
    }

    public FunctionBuilder function() {
        template.addImplements(type.getName());
        return new FunctionBuilder(type);
    }

    public class FunctionBuilder {

        private Class<?> returnType;
        private Class<?>[] argTypes = {};
        private Class<? extends Throwable> exceptionClass;
        private String functionName;
        private String body;

        private FunctionBuilder() {
        }

        private FunctionBuilder(final Class<?> ifc) {
            final Method m = ifc.getMethods()[0];
            if (!m.getReturnType().equals(Void.class)) {
                returnType = m.getReturnType();
            }
            if (m.getParameterCount() > 0) {
                argTypes = m.getParameterTypes();
            }
            functionName = m.getName();
        }

        public FunctionBuilder returnType(final Class<?> returnType) {
            this.returnType = returnType;
            return this;
        }

        public FunctionBuilder args(final Class<?>... argTypes) {
            this.argTypes = argTypes;
            return this;
        }

        public FunctionBuilder exceptionType(final Class<? extends Throwable> exceptionClass) {
            this.exceptionClass = exceptionClass;
            return this;
        }

        public FunctionBuilder body(final String body) {
            this.body = body;
            return this;
        }

        public CtLambdaTemplate<T> build(final String... args) {
            final StringBuilder b = new StringBuilder();
            b.append("public ").append(returnType.getName()).append(" ").append(functionName).append("(");
            if (args != null && args.length != argTypes.length) {
                throw new RuntimeException("Arg type count and args length do not match! " + Arrays.toString(argTypes) + " vs. " + Arrays.toString(args));
            }
            if (args != null && args.length > 0) {
                for (int i = 0; i < args.length; ++i) {
                    b.append(argTypes[i].getName()).append(" ").append(args[i]).append(", ");
                }
                b.delete(b.length() - 2, b.length());
            }
            b.append(")");
            if (exceptionClass != null) {
                b.append("throws ").append(exceptionClass.getName()).append(" ");
            }
            b.append("{");
            b.append(body);
            b.append("}");
            final String method = b.toString();
            template.addMethod(method);
            return CtLambdaTemplate.this;
        }
    }

    public interface Function {
        Object apply(Object... o) throws Exception;
    }
}
