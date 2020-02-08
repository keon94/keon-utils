package com.keon.projects.threading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HierarchicalThreads {

    private static final Map<Long, Relation> THREAD_RELATIONS = new HashMap<>();

    //TODO: Need to decorate the thread so that it's removed from the map once dead
    public static Thread register(final Thread t) {
        final Relation r = THREAD_RELATIONS.get(t.getId());
        if (r != null) {
            return t;
        } else {
            final long thisThreadId = Thread.currentThread().getId();
            final Relation r_parent = THREAD_RELATIONS.get(thisThreadId);
            if (r_parent != null) {
                r_parent.children.add(t.getId());
                THREAD_RELATIONS.put(t.getId(), new Relation(thisThreadId)); // child -> parent
                SharedThreadLocal.inheritLocals(t.getId(), thisThreadId);
            } else {
                THREAD_RELATIONS.put(t.getId(), new Relation(-1L));
            }
        }

        return t;
    }

    public static Long getParent(final long id) {
        final Relation r = THREAD_RELATIONS.get(id);
        return r == null ? null : r.parent;
    }

    public static List<Long> getChildren(final long id) {
        final Relation r = THREAD_RELATIONS.get(id);
        return r == null ? null : r.children;
    }

    private static class Relation {
        final Long parent;
        final List<Long> children = new ArrayList<>();

        Relation(final Long parent) {
            this.parent = parent;
        }
    }
}
