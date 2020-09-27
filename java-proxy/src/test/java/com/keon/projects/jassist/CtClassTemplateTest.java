package com.keon.projects.jassist;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SuppressWarnings("all")
//Does not work with maven
public class CtClassTemplateTest {

    @Test
    public void testList() throws Exception {
        final CtClassTemplate tmp = new CtClassTemplate("Keon")
                .autoImportDiscovery(true)
                .addImports(List.class, ArrayList.class)
                .addMethod(
                        "public Collection testAdd() { " +
                                "List l = new ArrayList();" +
                                "l.add(\"Hi \");" +
                                "l.add(\"Keon\");" +
                                "System.out.println(l.stream().collect(Collectors.toList())); " +
                                "return l;" +
                                "}");
        final Object o = tmp.createClass().newInstance();
        final Collection<String> c = (Collection<String>) ReflectionUtils.invokeMethod(o.getClass().getMethod("testAdd"), o);
        assertEquals(Arrays.asList("Hi ", "Keon"), c);
    }

    @Test
    public void testRunnableLambda() throws Exception {
        final CtLambdaTemplate<Runnable> template = new CtLambdaTemplate<>(Runnable.class)
                .autoImportDiscovery(false)
                .addImports(DummyException.class)
                .function().body("int x = 1;" +
                        "int y = 2;" +
                        "System.out.println(x+y);" +
                        "throw new DummyException();"
                ).build();
        final Runnable r = template.createClass().newInstance();
        assertThrows(DummyException.class, () -> r.run());
    }

    @Test
    public void testFunctionLambda() throws Exception {
        final CtLambdaTemplate<Function> template = new CtLambdaTemplate<>(Function.class)
                .autoImportDiscovery(true)
                .function()
                .body("return str + \"!\";")
                .build("str");
        final Function<String, String> f = template.createClass().newInstance();
        assertEquals("Hi Keon!", f.apply("Hi Keon"));
    }

    public static class DummyException extends RuntimeException {

        public DummyException() {
            super();
        }

    }
}
