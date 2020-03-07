package com.keon.projects.junit.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.asList;

public class ResourceGraph<T> {

    private final Map<T, Resource<T>> resources = new HashMap<>();

    public ResourceGraph<T> add(final Resource<T> resource) {
        //make sure it's not circular
        final Resource<T> circular = findCircularDependency(resource, resource);
        if (circular != null) {
            throw new IllegalArgumentException(resource.id + " has a circular dependency on " + circular.id);
        }
        resources.put(resource.id, resource);
        return this;
    }

    public Set<Resource<T>> getResources() {
        final Set<Resource<T>> resources = new TreeSet<>((r1, r2) -> {
            if (r1.equals(r2)) {
                return 0;
            }
            if (totalWeight(r1) >= totalWeight(r2)) {
                return 1;
            }
            return -1;
        });
        resources.addAll(this.resources.values());
        return resources;
    }

    public static <T> float totalWeight(final Collection<Resource<T>> resources) {
        float total = 0;
        for(final Resource<T> r : resources) {
            total += r.weight;
        }
        return total;
    }

    private float totalWeight(final Resource<T> resource) {
        float total = resource.weight;
        for(final T dep : resource.dependencies) {
            final Resource<T> rDep = resources.get(dep);
            if (rDep != null) {
                total += totalWeight(rDep);
            }
        }
        return total;
    }

    private Resource<T> findCircularDependency(final Resource<T> root, final Resource<T> resource) {
        for (final T dep : resource.dependencies) {
            Resource<T> rDep = resources.get(dep);
            if (rDep != null) {
                if (rDep.dependencies.contains(root.id)) {
                    return rDep;
                }
                rDep = findCircularDependency(root, rDep);
                if (rDep != null && rDep.dependencies.contains(root.id)) {
                    return rDep;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "[resources=" + resources.toString() + "]";
    }

    public static class Resource<T> {

        private final float weight;
        private final Set<T> dependencies;
        private final T id;

        private Resource(final T id, final float weight, final Set<T> dependencies) {
            this.id = id;
            this.weight = weight;
            this.dependencies = dependencies;
        }

        public T getId() {
            return id;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            final Resource<T> other = (Resource<T>) o;
            return dependencies.equals(other.dependencies) && id.equals(other.id);
        }

        @Override
        public String toString() {
            return "[id=" + id + ", weight=" + weight + ", dependencies=" + dependencies + "]";
        }

        public static class Builder<T> {

            private final T id;
            private float weight;
            private Set<T> dependencies;

            private Builder(final T id) {
                this.id = id;
                this.weight = 1.0f;
                this.dependencies = Collections.emptySet();
            }

            public static <T> Builder<T> id(final T name) {
                return new Builder<>(name);
            }

            public Builder<T> weight(final float weight) {
                if (weight < 0) {
                   throw new IllegalArgumentException("Negative weight supplied: " + weight);
                }
                this.weight = weight;
                return this;
            }

            @SafeVarargs
            public final Builder<T> dependencies(final T... dependencies) {
                this.dependencies = new HashSet<>(asList(dependencies));
                return this;
            }

            public Resource<T> build() {
                return new Resource<>(id, weight, dependencies);
            }

        }
    }
}
