package com.keon.projects.junit.parallel;

import com.keon.projects.junit.engine.JupiterSkipper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JupiterSkipper.class)
public class OneTest {

    @Test
    public void test() {

    }
}
