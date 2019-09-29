package com.keon.projects.math.calculator;

import static com.keon.projects.math.calculator.Calculator.calculate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CalculatorTest {

    @Test
    public void testAdd() {
        assertEquals(1.0, calculate(new Object[] { 1.0 }));
        assertEquals(3.0, calculate(new Object[] { 1.0, '+', 2.0 }));
        assertEquals(6.0, calculate(new Object[] { 1.0, '+', 2.0, '+', 3.0 }));
        assertEquals(4.0, calculate(new Object[] { 1.0, '+', 2.0, '-', 3.0, '+', 4.0 }));
    }

    @Test
    public void testMult() {
        assertEquals(2.0, calculate(new Object[] { 1.0, '*', 2.0 }));
        assertEquals(1.5, calculate(new Object[] { 1.0, '/', 2.0, '*', 3.0 }));
    }

    @Test
    public void testAddMult() {
        assertEquals(7.0, calculate(new Object[] { 1.0, '+', 2.0, '*', 3.0 }));
        assertEquals(11.0, calculate(new Object[] { 1.0, '+', 2.0, '*', 3.0, '+', 4.0 }));
        assertEquals(17.0,
                calculate(new Object[] { 1.0, '+', 2.0, '*', 3.0, '+', 4.0, '*', 5.0, '/', 2.0 }));
        assertEquals(16.0,
                calculate(new Object[] { 1.0, '+', 2.0, '*', 3.0, '+', 4.0, '*', 5.0, '/', 2.0, '-', 1.0 }));
    }
    
    @Test
    public void testExp() {
        assertEquals(8.0, calculate(new Object[] { 2.0, '^', 3.0 }));
        assertEquals(512.0, calculate(new Object[] { 2.0, '^', 3.0, '^', 2.0 }));
        assertEquals(4.0, calculate(new Object[] { 2.0, '^', 3.0, '/', 2.0 }));
        assertEquals(10.0, calculate(new Object[] { 2.0, '^', 3.0, '+', 4.0, '^', 0.5 }));
    }
}
