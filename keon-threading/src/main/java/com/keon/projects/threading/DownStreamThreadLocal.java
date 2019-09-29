package com.keon.projects.threading;

public class DownStreamThreadLocal<T> extends SharedThreadLocal<T> {

    @Override
    protected Long getRoot(long tid) {
        return tid;
    }

    @Override
    protected boolean inheritLocals() {
        return false;
    }

}
