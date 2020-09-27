package com.keon.projects.junit.engine;

import com.keon.projects.junit.engine.TestSorters.MethodSorter;
import com.keon.projects.junit.engine.TestSorters.SuiteSorter;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TestSorterExecutor {

    private final Map<Class<?>, Set<Method>> sortedSuiteMap = new LinkedHashMap<>();

    private static Reflections getReflector() {
        final long start = System.currentTimeMillis();
        final Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forJavaClassPath())
                .setScanners(new SubTypesScanner())
                .useParallelExecutor(4));
        System.out.println("Reflections took " + (System.currentTimeMillis() - start) + " ms");
        return reflections;
    }

    TestSorterExecutor(final Map<Class<?>, List<Method>> suiteMap) throws Exception {
        final Reflections reflections = getReflector();
        final Set<Class<? extends SuiteSorter>> suiteSorters = reflections.getSubTypesOf(SuiteSorter.class);
        Set<Class<?>> sortedSuites = suiteMap.keySet();
        for (Class<? extends SuiteSorter> suiteSorter : suiteSorters) {
            final Constructor<? extends SuiteSorter> constructor = suiteSorter.getDeclaredConstructor();
            constructor.setAccessible(true);
            sortedSuites = constructor.newInstance().sort(sortedSuites);
        }
        sortedSuites.forEach(c -> sortedSuiteMap.put(c, new LinkedHashSet<>()));

        final Set<Class<? extends MethodSorter>> methodSorters = reflections.getSubTypesOf(MethodSorter.class);
        for (final Class<?> suite : sortedSuites) {
            Set<Method> sortedMethods = new LinkedHashSet<>(suiteMap.get(suite));
            for (Class<? extends MethodSorter> methodSorter : methodSorters) {
                final Constructor<? extends MethodSorter> constructor = methodSorter.getDeclaredConstructor();
                constructor.setAccessible(true);
                sortedMethods = constructor.newInstance().sort(sortedMethods);
            }
            sortedSuiteMap.get(suite).addAll(sortedMethods);
        }
    }

    Set<Class<?>> getSortedClasses() {
        return sortedSuiteMap.keySet();
    }

    Set<Method> getSortedMethods(Class<?> c) {
        return sortedSuiteMap.get(c);
    }
}