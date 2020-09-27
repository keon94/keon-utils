package com.keon.projects.junit.engine.client;

import com.keon.projects.junit.engine.TestSorters.MethodSorter;
import com.keon.projects.junit.engine.TestSorters.SuiteSorter;
import com.keon.projects.junit.engine.client.ResourceGraph.Resource;
import com.keon.projects.junit.engine.client.ResourceGraph.Resource.Builder;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.keon.projects.junit.engine.client.ResourceGraph.totalWeight;

class ResourceSuiteSorter implements SuiteSorter {

    @Override
    public Set<Class<?>> sort(final Collection<Class<?>> classes) {
        final Map<Class<?>, String[]> suiteMappings = getResourceMappings(classes);
        final ResourceGraph<Class<?>> graph = new ResourceGraph<>();
        for (final Entry<Class<?>, String[]> e : suiteMappings.entrySet()) {
            final float weight = totalWeight(new Resources().add(e.getValue()).getResources());
            graph.add(Builder.<Class<?>>id(e.getKey()).weight(weight).build());
        }
        return graph.getResources().stream().map(Resource::getId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Map<Class<?>, String[]> getResourceMappings(final Collection<Class<?>> classes) {
        final Map<Class<?>, String[]> map = new HashMap<>();
        for (final Class<?> clazz : classes) {
            final CustomRunner[] runners = clazz.getDeclaredAnnotationsByType(CustomRunner.class);
            if (runners.length == 0)
                continue;
            if (runners.length > 1)
                throw new IllegalStateException(runners.length + " instances of annotation " + CustomRunner.class.getName() + " found on " + clazz.getName() + ". Expected 1.");
            final CustomRunner runner = runners[0];
            map.put(clazz, runner.resources());
        }
        return map;
    }
}

class MethodSorterImpl implements MethodSorter {

    @Override
    public Set<Method> sort(Collection<Method> methods) {
        final Set<Method> set = new TreeSet<>((o1, o2) -> o2.getName().compareTo(o1.getName()));
        set.addAll(methods);
        return set;
    }
}