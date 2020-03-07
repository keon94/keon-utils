package com.keon.projects.jassist;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

public class CtClassTemplateTest {

    @Test
    public void test() throws Exception {
        final CtClassTemplate tmp = new CtClassTemplate("Keon")
                .autoImportDiscovery(true)
               // .addImports(List.class, ArrayList.class)
                .addMethod(
                        "public void testAdd() { " +
                            "java.util.List l = new ArrayList();" +
                            "l.add(\"Hi \");" +
                            "l.add(\"Keon\");" +
                            "System.out.println(l.stream().collect(Collectors.toList())); " +
                        "}");
        final Object o = tmp.createClass().newInstance();
        ReflectionUtils.invokeMethod(o.getClass().getMethod("testAdd"), o);
    }
}
