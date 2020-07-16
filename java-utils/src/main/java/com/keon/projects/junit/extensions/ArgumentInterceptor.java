package com.keon.projects.junit.extensions;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import com.google.common.primitives.Primitives;
import com.keon.projects.junit.annotations.BeforeEachArguments;

/**
 * Let's us run parameterized tests with beforeEach behavior for each set of arguments
 */
public abstract class ArgumentInterceptor implements ArgumentsProvider {

    public class DecoratedArguments implements Arguments {

        private Arguments delegate;

        //keep
        public DecoratedArguments() {}

        private DecoratedArguments(Object... args) {
            this.delegate = Arguments.of(args);
        }

        @Override
        public Object[] get() {
            final Object[] args = delegate.get();
            final Method interceptor = discoverInterceptor(args);
            if (interceptor != null ) {
                try {
                    interceptor.invoke(ArgumentInterceptor.this,args);
                } catch (final InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException("Error when calling method " + interceptor + " with args " + Arrays.toString(args), e);
                }
            }
            System.out.print("Running with args: " + Arrays.toString(args));
            return args;
        }

        private Method discoverInterceptor(final Object... args) {
            final Class<?>[] argTypes = Arrays.stream(args).map(a -> a == null ? Object.class : a.getClass()).toArray(Class[]::new);
            for (final Method m : ArgumentInterceptor.this.getClass().getMethods()) {
                if (m.isAnnotationPresent(BeforeEachArguments.class) &&
                        isCompatible(boxTypes(argTypes), boxTypes(m.getParameterTypes()), m.isVarArgs())) {
                    return m;
                }
            }
            return null;
        }

        private Class<?>[] boxTypes(final Class<?>[] types) {
            for(int i = 0 ; i < types.length; ++i) {
                final Class<?> type = types[i];
                if (type != null) {
                    if (type.isPrimitive()) {
                        types[i] = Primitives.wrap(type);
                    } else if (type.getComponentType() != null && type.getComponentType().isPrimitive()) {
                        types[i] = Array.newInstance(Primitives.wrap(type.getComponentType()), 0).getClass();
                    }
                }
            }
            return types;
        }

        private boolean isCompatible(final Class<?>[] args, final Class<?>[] params, final boolean isVarargs) {
            if (params.length == 0) {
                return args.length == 0;
            }
            final int nonVarParamLength = params.length - (isVarargs ? 1 : 0);

            for (int i = 0; i < nonVarParamLength; ++i) {
                if (i == args.length) {
                    return false;
                }
                if (!params[i].isAssignableFrom(args[i])) {
                    return false;
                }
            }
            if (isVarargs) {
                assert params[params.length - 1].isArray();
                if (params.length == 1 && args.length == 0)
                    return true;
                for (int i = params.length-1; i < args.length; ++i) {
                    if (!params[i].isAssignableFrom(args[i])) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private DecoratedArguments args(Object... args) {
        return new DecoratedArguments(args);
    }

    @Override
    public final Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
        return Arrays.stream(provideArguments()).map(args -> args(args.get()));
    }

    public abstract Arguments[] provideArguments();

}


