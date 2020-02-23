package com.keon.projects.junit.parallel;

import com.keon.projects.junit.engine.CustomRunner;
import com.keon.projects.junit.engine.client.Resources;
import org.junit.jupiter.api.Test;

@CustomRunner(resources = {Resources.A, Resources.F, Resources.E})
public class TwoTest {

    @Test
    public void test() {

    }
}
