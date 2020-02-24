package com.keon.projects.junit.parallel;

import com.keon.projects.junit.engine.client.CustomRunner;
import com.keon.projects.junit.engine.client.Resources;
import org.junit.jupiter.api.Test;

@CustomRunner(resources = {Resources.G, Resources.A})
public class ThreeTest {

    @Test
    public void test1() {

    }
    @Test
    public void test2() {

    }
    @Test
    public void test3() {

    }
}
