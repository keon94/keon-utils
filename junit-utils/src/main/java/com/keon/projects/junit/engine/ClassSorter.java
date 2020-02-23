package com.keon.projects.junit.engine;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

//TODO
class ClassSorter {

    private final Collection<Class<?>> classes;

    ClassSorter(final Collection<Class<?>> classes) {
        this.classes = classes;
    }

    Set<Class<?>> getSorted() {
        return new HashSet<>(classes);
    }
}
