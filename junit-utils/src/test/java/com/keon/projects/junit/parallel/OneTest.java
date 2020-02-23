package com.keon.projects.junit.parallel;

import com.keon.projects.junit.engine.CustomRunner;
import com.keon.projects.junit.engine.client.Resources;
import org.junit.jupiter.api.Test;

@CustomRunner(resources = {Resources.A, Resources.B, Resources.E})
public class OneTest {

    @Test
    public void test() {

    }
}
