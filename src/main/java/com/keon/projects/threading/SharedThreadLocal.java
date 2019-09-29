package com.keon.projects.threading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("rawtypes")
public abstract class SharedThreadLocal<T> {

    static final Map<Long, Map<SharedThreadLocal, Object>> LOCALS = new ConcurrentHashMap<>();

    protected SharedThreadLocal() {
//        final long tid = Thread.currentThread().getId();
//        final long ptid = HierrarchialThreads.getParent(tid);
        //inheritLocals(tid, ptid);
    }

    public void set(final T t) {

        final long tid = Thread.currentThread().getId();

        Map<SharedThreadLocal, Object> m = LOCALS.get(tid);
        if (m == null) {
            m = new HashMap<>();
        }
        m.put(this, t);
        for (final var htid : getThreadHierrarchy(tid)) {
            LOCALS.put(htid, m);
        }
    }

    @SuppressWarnings("unchecked")
    public T get() {
        final Map<SharedThreadLocal, Object> m = LOCALS.get(Thread.currentThread().getId());
        return m == null ? null : (T) m.get(this);
    }

    protected abstract Long getRoot(final long tid);

    protected abstract boolean inheritLocals();

    protected void getThreadHierrarchy(final Long root, final List<Long> all) {
        final List<Long> children = HierrarchialThreads.getChildren(root);
        if (children.isEmpty()) {
            all.add(root);
        } else {
            for (final Long child : children) {
                getThreadHierrarchy(child, all);
            }
        }
    }
    
    private List<Long> getThreadHierrarchy(final long tid) {
        final List<Long> hierrarchy = new ArrayList<>();
        final Long root = getRoot(tid);
        if (root != null)
            getThreadHierrarchy(root, hierrarchy);
        return hierrarchy;
    }

    static void inheritLocals(final long tid, final long parent) {
        final Map<SharedThreadLocal, Object> parentMap = LOCALS.get(parent);
        if (parentMap != null) {
            for (final var local : parentMap.entrySet()) {
                if (local.getKey().inheritLocals()) {
                    LOCALS.put(tid, parentMap);
                }
            }
        }
    }

}
