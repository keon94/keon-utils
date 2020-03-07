package com.keon.projects.junit.engine;

import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomDiscoveryEngine extends HierarchicalTestEngine<JupiterEngineExecutionContext> {

    private final TestEngine engine = new JupiterTestEngine();

    @Override
    public String getId() {
        return "KEON-ID";
    }

    @Override
    public TestDescriptor discover(final EngineDiscoveryRequest discoveryRequest, final UniqueId uniqueId) {
        final TestDescriptor descriptor = engine.discover(discoveryRequest, uniqueId);
        final DiscoverySelector[] selectors = sortMethods(discoverMethods(descriptor));
        final LauncherDiscoveryRequest modifiedRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectors)
                .build();
        return new DecoratedEngineDescriptor((JupiterEngineDescriptor) engine.discover(modifiedRequest, uniqueId));
    }

    @Override
    protected JupiterEngineExecutionContext createExecutionContext(final ExecutionRequest request) {
        return new JupiterEngineExecutionContext(request.getEngineExecutionListener(),
                ((DecoratedEngineDescriptor) request.getRootTestDescriptor()).getConfiguration());
    }

    //=================================Internal=====================================================================

    private static List<Method> discoverMethods(final TestDescriptor descriptor) {
        final List<Method> methods = new ArrayList<>();
        for(final TestDescriptor test : descriptor.getDescendants()) {
            if (test instanceof TestMethodTestDescriptor) {
                final TestMethodTestDescriptor methodTestDescriptor = (TestMethodTestDescriptor) test;
                methods.add(methodTestDescriptor.getTestMethod());
            }
        }
        return methods;
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
}
