package com.keon.projects.differ;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class Differ {

    public static <T> Diff<?> diff(final T obj1, final T obj2) throws Exception {
        return diff0(obj1, obj2);
    }

    private static <T, U> Diff<?> diff0(final T o1, final T o2) throws Exception {
        if (o1 == o2)
            return Diff.EMPTY;
        final Diff<?> totalDiff = new Diff<>(o1, o2);
        if (o1 == null || o2 == null) {
            return totalDiff;
        }
        //neither is null
        for (final Field f : o1.getClass().getDeclaredFields()) {
            final U of1 = (U) f.get(o1);
            final U of2 = (U) f.get(o2);
            final Diff diff = diff0(of1, of2);
            if (diff != Diff.EMPTY)
                totalDiff.add(diff.forField(f));
        }
        return totalDiff.getEffective();
    }
}

class Diff<T> {

    static final Diff<?> EMPTY = null;

    private final Set<Diff<?>> diffs = new HashSet<>();

    private Field field;
    private final T obj1;
    private final T obj2;

    Diff(final T obj1, final T obj2) {
        this.obj1 = obj1;
        this.obj2 = obj2;
    }

    Diff forField(final Field field) {
        this.field = field;
        return this;
    }

    Diff add(final Diff<?> diff) {
        diffs.add(diff);
        return this;
    }

    Diff<?> getEffective() {
        return diffs.isEmpty() ? EMPTY : this;
    }
}

