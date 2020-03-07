package com.keon.projects.jassist;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class CtClassTemplate {

    private static final ClassPool POOL = ClassPool.getDefault();


    public static volatile String CLASS_DEBUG_PATH = null;
    private final String name;
    private boolean autoImportDiscovery;
    private String extendsClass;
    private final List<String> implementsInterfaces = new ArrayList<>();
    private final Set<String> imports = new HashSet<>();
    private final List<CtFieldTemplate> fields = new ArrayList<>();
    private final List<CtMethodTemplate> methods = new ArrayList<>();

    public CtClassTemplate(final String name) {
        this.name = name;
    }

    public CtClassTemplate autoImportDiscovery(final boolean auto) {
        autoImportDiscovery = auto;
        return this;
    }

    public CtClassTemplate addImports(final String... imps) {
        imports.addAll(asList(imps));
        return this;
    }

    public CtClassTemplate addImports(final Class<?>... classes) {
        imports.addAll(Arrays.stream(classes).map(Class::getName).collect(Collectors.toSet()));
        return this;
    }

    public CtClassTemplate setExtends(final String className) {
        this.extendsClass = className;
        return this;
    }

    public CtClassTemplate addImplements(final String ifcName) {
        implementsInterfaces.add(ifcName);
        return this;
    }

    public CtClassTemplate addField(final String code) {
        final CtFieldTemplate result = new CtFieldTemplate(code);
        fields.add(result);
        return this;
    }

    public CtClassTemplate addMethod(final String code) {
        final CtMethodTemplate result = new CtMethodTemplate(code);
        methods.add(result);
        return this;
    }

    public Class<?> createClass() throws CannotCompileException {
        return createClass(POOL, CtClassTemplate.class.getClassLoader());
    }

    public Class<?> createClass(final ClassPool pool) throws CannotCompileException {
        return createClass(pool, CtClassTemplate.class.getClassLoader());
    }

    public Class<?> createClass(final ClassPool pool, final ClassLoader cl) throws CannotCompileException {
        try {

            final ImportDiscoverer discoverer = autoImportDiscovery ? new ImportDiscovererImpl(pool) : new NoOpImportDiscoverer();

            //explicit imports
            for(final String imp : imports) {
                pool.importPackage(imp);
            }

            final CtClass ctClass = pool.makeClass(name);

            if (extendsClass != null && !extendsClass.isEmpty()) {
                discoverer.withImportDiscovery(() -> ctClass.setSuperclass(pool.get(extendsClass)));
            }

            for (String ifc : implementsInterfaces) {
                discoverer.withImportDiscovery(() -> ctClass.addInterface(pool.get(ifc)));
            }

            for (CtFieldTemplate field : fields) {
                final CtField ctField = discoverer.withImportDiscovery(() -> CtField.make(field.getCode(), ctClass));
                ctClass.addField(ctField);
            }

            for (CtMethodTemplate method : methods) {
                final CtMethod ctMethod = discoverer.withImportDiscovery(() -> CtMethod.make(method.getCode(), ctClass));
                ctClass.addMethod(ctMethod);
            }

            final String cdp = CLASS_DEBUG_PATH;

            if (cdp != null) {
                ctClass.writeFile(cdp);
            }

            return ctClass.toClass(cl, null);

        } catch (NotFoundException | IOException e) {
            throw new CannotCompileException(e);
        }
    }



    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (final String imp : imports) {
            builder.append("import ").append(imp).append('\n');
        }
        builder.append("class ").append(name).append(" ");
        if (extendsClass != null) {
            builder.append(extendsClass).append(" ");
        }
        if (!implementsInterfaces.isEmpty()) {
            builder.append("implements ");
            for(final String ifc : implementsInterfaces) {
                builder.append(ifc).append(", ");
            }
            builder.deleteCharAt(builder.length()-2);
        }
        builder.append("{\n");
        //fields
        for(final CtFieldTemplate f : fields) {
            builder.append("    ").append(f.getCode()).append('\n');
        }
        //methods
        for(final CtMethodTemplate m : methods) {
            builder.append("    ").append(m.toString().replaceAll("\n", "\n    ")).append('\n');
        }
        builder.append("}");
        return builder.toString();
    }

    public static class CtMethodTemplate {

        private String code;

        private CtMethodTemplate(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code
                    .replaceAll(";\\s*", ";\n")
                    .replaceAll("\\{\\s*", "{\n")
                    .replaceAll("}\\s*", "}\n");
        }
    }

    public static class CtFieldTemplate {
        private String code;

        private CtFieldTemplate(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
