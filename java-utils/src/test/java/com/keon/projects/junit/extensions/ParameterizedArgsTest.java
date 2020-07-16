package com.keon.projects.junit.extensions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;

import com.keon.projects.junit.annotations.BeforeEachArguments;

public class ParameterizedArgsTest extends ArgumentInterceptor {

    @ParameterizedTest
    @ArgumentsSource(ParameterizedArgsTest.class)
    public void test() {
        return;
    }

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
