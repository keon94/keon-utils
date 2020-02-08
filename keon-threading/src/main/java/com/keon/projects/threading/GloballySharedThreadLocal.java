package com.keon.projects.threading;

public class GloballySharedThreadLocal<T> extends SharedThreadLocal<T> {
    
    @Override
    protected Long getRoot(long tid) {
        Long root = null;
        while (true) {
            root = HierarchicalThreads.getParent(tid);
            if (root == -1) {
                return tid;
            } else if(root == null) {
                throw new IllegalStateException("Root Thread is null, meaning that it is not registered.");
            }
            tid = root;
        }
    }

    @Override
    protected boolean inheritLocals() {
        return true;
    }
}
