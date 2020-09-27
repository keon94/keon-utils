package com.keon.projects.jassist;

import javassist.CannotCompileException;

public interface CtTemplate<T extends CtTemplate<T>> {

    Class<?> createClass() throws CannotCompileException;

    default CtTemplate<T> autoImportDiscovery(final boolean auto) {
        return this;
    }

    ;

    default CtTemplate<T> addImports(final String... imps) {
        return this;
    }

    default CtTemplate<T> addImports(final Class<?>... classes) {
        return this;
    }

    default CtTemplate<T> addImplements(final String ifcName) {
        return this;
    }
}
