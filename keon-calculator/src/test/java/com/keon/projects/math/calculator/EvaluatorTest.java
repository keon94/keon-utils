package com.keon.projects.math.calculator;

import org.junit.jupiter.api.Test;

import static com.keon.projects.calculator.logic.Calculator.eval;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EvaluatorTest {

    @Test
    public void testMonoid() {
        assertEquals(1.0, eval("(1)"));
        assertEquals(1.0, eval("1"));
    }

    @Test
    public void testSigned() {
        assertEquals(1.0, eval("+1"));
        assertEquals(-1.0, eval("-1"));
    }

    @Test
    public void testBasicAdd() {
        assertEquals(3.0, eval("(1 + 2)"));
        assertEquals(3.0, eval("1 + 2"));
    }

    @Test
    public void testBasicMult() {
        assertEquals(2.0, eval("(1 * 2)"));
        assertEquals(2.0, eval("1 * 2"));
    }

    @Test
    public void testNested1() {
        assertEquals(8.0, eval("(1 + (2*3) + 1)"));
        assertEquals(1.0, eval("2 * (1 + (3/6)) - 2"));
    }
    
    @Test
    public void testNested2() {
        assertEquals(38.0, eval("(1 + (2*3)^2 + 1)"));
        assertEquals(129.0, eval("1 + (2^3)^2 * 2"));
    }
    
    @Test
    public void testExponent() {
        assertEquals(256.0, eval("2^(3-1)^3"));
        assertEquals(512.0, eval("2^3^2"));
        assertEquals(256.0, eval("2^(3^2-1)"));
    }
    
    @Test
    public void testFunction() {
        assertEquals(0.0, eval("sin(0)"));
        assertEquals(1.0, eval("cos(0)"));
        assertEquals(2.0, eval("cos(0) + cos(sin(0))"));
        assertEquals(9.0 , eval("1 + 3*avg(2, avg(3,5), 1) + 1"));
        assertEquals(16.0 , eval("9*(avg(1+2^1, avg(1, (sin(0)), 1/5*avg((avg(5))^2)), -1))^2*1"));
    }
}
