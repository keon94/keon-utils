package com.keon.projects.differ;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class DifferTest {

    @Test
    public void test() throws Exception {
        final Differ.Diff<?> d1 = Differ.diff(Arrays.asList("a", "b"), Arrays.asList("a"));
        System.out.println(d1);
        final Differ.Diff<?> d2 = Differ.diff(Arrays.asList("a", "b", Arrays.asList("x")), Arrays.asList("a"));
        System.out.println(d2);
    }

}
