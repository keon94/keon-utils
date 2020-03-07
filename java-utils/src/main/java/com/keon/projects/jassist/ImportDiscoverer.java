package com.keon.projects.jassist;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.compiler.CompileError;
import javassist.compiler.MemberResolver;
import org.junit.platform.commons.util.ReflectionUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

interface ImportDiscoverer {

    <T> T withImportDiscovery(final CompilableSupplier<T> compilable) throws CannotCompileException, NotFoundException;

    void withImportDiscovery(final CompilableRunnable compilable) throws CannotCompileException, NotFoundException;

    interface CompilableSupplier<R> {
        R get() throws CannotCompileException, NotFoundException;
    }

    interface CompilableRunnable {
        void run() throws CannotCompileException, NotFoundException;
    }

}

class NoOpImportDiscoverer implements ImportDiscoverer {

    @Override
    public <T> T withImportDiscovery(CompilableSupplier<T> compilable) throws CannotCompileException, NotFoundException {
        return compilable.get();
    }

    @Override
    public void withImportDiscovery(CompilableRunnable compilable) throws CannotCompileException, NotFoundException {
        compilable.run();
    }
}

class ImportDiscovererImpl implements ImportDiscoverer {

    private final ClassPool pool;
    private Set<String> allClasses;

    ImportDiscovererImpl(final ClassPool pool) {
        this.pool = pool;
    }

    @Override
    public <T> T withImportDiscovery(final CompilableSupplier<T> compilable) throws CannotCompileException, NotFoundException {
        while(true) {
            try {
                return compilable.get();
            } catch (final CannotCompileException e) {
                final String clazz = parseMsg(e);
                final String fqnClazz = discover(clazz);
                if (fqnClazz == null) {
                    throw e;
                }
                clearInvalidCache();
                pool.importPackage(fqnClazz);
            }
        }
    }

    @Override
    public void withImportDiscovery(final CompilableRunnable compilable) throws CannotCompileException, NotFoundException {
        withImportDiscovery(() -> {
            compilable.run();
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private void clearInvalidCache() {
        final Map<ClassPool, ?> invalidMap = (Map<ClassPool, ?>) ReflectionUtils.tryToReadFieldValue(MemberResolver.class, "invalidNamesMap", null).getOrThrow(RuntimeException::new);
        invalidMap.remove(pool);
    }

    private static String parseMsg(final CannotCompileException e) {
        assert e.getCause().getClass() == CompileError.class;
        final String msg = e.getCause().getMessage();
        return msg.replaceAll(".*no such class: (.*)", "$1");
    }

    private String discover(final String clazz) {
        lazyLoadClasses();
        final Set<String> types = allClasses.stream().filter(type -> type.endsWith("." + clazz)).collect(Collectors.toSet());
        if (types.isEmpty()) {
            return null;
        }
        if (types.size() > 1) {
            throw new RuntimeException("Multiple FQNs found for " + clazz + ": " + types);
        }
        return types.iterator().next();
    }

    @SuppressWarnings("UnstableApiUsage")
    private void lazyLoadClasses() {
        if (allClasses != null) {
            return;
        }
        final ClassLoader cl = ImportDiscoverer.class.getClassLoader();
        try {
            allClasses = ClassPath.from(cl).getAllClasses().stream().map(ClassInfo::getName).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
