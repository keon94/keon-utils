package com.keon.projects.threading;

public class LocallySharedThreadLocal<T> extends SharedThreadLocal<T> {

    @Override
    protected Long getRoot(long tid) {
        final Long root = HierarchicalThreads.getParent(tid);
        if (root == -1) {
            return tid;
        } else if (root == null) {
            throw new IllegalStateException("Root Thread is null, meaning that it is not registered.");
        }
        return root;
    }

    @Override
    protected boolean inheritLocals() {
        return true;
    }
}
