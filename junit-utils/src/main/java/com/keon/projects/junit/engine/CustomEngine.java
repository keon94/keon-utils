package com.keon.projects.junit.engine;

import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;

public class CustomEngine implements TestEngine {

    private final TestEngine engine = new JupiterTestEngine();

    @Override
    public String getId() {
        return "KEON_ID";
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        final RequestFilters filters = new RequestFilters(discoveryRequest);
        final DiscoverySelector[] selectors = flatten(
                sortPackages(filters.packages),
                sortClasses(filters.classes),
                sortMethods(filters.methods));
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectors)
                .build();
        final TestDescriptor descriptor = engine.discover(request, uniqueId);
        return descriptor;
    }

    @Override
    public void execute(ExecutionRequest request) {
        engine.execute(request);
    }

    private static ClassSelector[] sortPackages(final Collection<String> packages) {
        if (packages.isEmpty()) {
            return new ClassSelector[0];
        }
        try {
            final URL testClassesURL = Paths.get("target/test-classes").toUri().toURL();
            final Set<Class<?>> types = new Reflections(new ConfigurationBuilder()
                    .addClassLoader(URLClassLoader.newInstance(new URL[]{testClassesURL}, ClasspathHelper.staticClassLoader()))
                    .forPackages(packages.toArray(new String[0]))
                    .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner()))
                    .getTypesAnnotatedWith(CustomRunner.class, true);
            return sortClasses(types);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ClassSelector[] sortClasses(final Collection<Class<?>> types) {
        if (types.isEmpty()) {
            return new ClassSelector[0];
        }
        final Set<Class<?>> sorted = new SuiteSorter(types).getSorted();
        return sorted.stream().map(DiscoverySelectors::selectClass).toArray(ClassSelector[]::new);
    }

    private static MethodSelector[] sortMethods(final Collection<Method> methods) {
        if (methods.isEmpty()) {
            return new MethodSelector[0];
        }
        final Map<Class<?>, List<Method>> methodMap = new HashMap<>();
        for(final Method m : methods) {
            List<Method> ms = methodMap.get(m.getDeclaringClass());
            if (ms == null) {
                ms = new ArrayList<>();
            }
            ms.add(m);
            methodMap.put(m.getDeclaringClass(), ms);
        }
        final Set<Class<?>> sortedClasses = new SuiteSorter(methodMap.keySet()).getSorted();
        final List<Method> sortedMethods = new ArrayList<>();
        sortedClasses.forEach(c -> sortedMethods.addAll(methodMap.get(c)));
        return sortedMethods.stream().map(m -> DiscoverySelectors.selectMethod(m.getDeclaringClass(), m)).toArray(MethodSelector[]::new);
    }

    private static <T extends DiscoverySelector> DiscoverySelector[] flatten(final T[]... arrays) {
        final List<T> flattened = new ArrayList<>();
        for(final T[] array : arrays) {
            flattened.addAll(asList(array));
        }
        return flattened.toArray(new DiscoverySelector[0]);
    }

    private static class RequestFilters {

        final List<String> packages = new ArrayList<>();
        final List<Class<?>> classes = new ArrayList<>();
        final List<Method> methods = new ArrayList<>();

        RequestFilters(final EngineDiscoveryRequest request) {
            //request.getSelectorsByType(DirectorySelector.class).forEach(d -> d.getDirectory()); //Maybe TODO
            request.getSelectorsByType(ClasspathRootSelector.class).forEach(r -> {});
            request.getSelectorsByType(PackageSelector.class).forEach(p -> packages.add(p.getPackageName()));
            request.getSelectorsByType(ClassSelector.class).forEach(c -> classes.add(c.getJavaClass()));
            request.getSelectorsByType(MethodSelector.class).forEach(m -> {
                if (!classes.contains(m.getJavaClass())) { //probably unnecessary check
                    methods.add(m.getJavaMethod());
                }
            });
        }
    }
}
