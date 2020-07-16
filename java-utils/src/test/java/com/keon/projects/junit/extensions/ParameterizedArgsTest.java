package com.keon.projects.junit.extensions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;

import com.keon.projects.junit.annotations.BeforeEachArguments;

public class ParameterizedArgsTest {

    @ParameterizedTest
    @ArgumentsSource(ArgumentInterceptorImpl.class)
    public void test() {
        return;
    }

}

class ArgumentInterceptorImpl extends ArgumentInterceptor {

    @Override
    public Arguments[] provideArguments() {
        return new Arguments[] {
                Arguments.of(1, true), Arguments.of(2, false)
        };
    }
    @BeforeEachArguments
    public void onArgs(final int x, final boolean bb, final boolean[] b) {
        System.err.println("Got " + x + ",,, " + b[0]);
    }

    @BeforeEachArguments
    public void onArgs(final int x, final boolean... b) {
        System.err.println("Got " + x + ", " + b[0]);
    }
}
