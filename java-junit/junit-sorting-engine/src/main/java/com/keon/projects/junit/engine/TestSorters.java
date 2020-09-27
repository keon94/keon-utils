package com.keon.projects.junit.engine;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

public interface TestSorters {

    interface MethodSorter {

        Set<Method> sort(final Collection<Method> methods);

    }

    interface SuiteSorter {

        Set<Class<?>> sort(final Collection<Class<?>> suites);

    }
}
