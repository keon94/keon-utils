package com.keon.projects.ipc;

import com.keon.projects.ipc.test.MultiJVMTestSuite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MultiJvmTest extends MultiJVMTestSuite {

    @Test
    public void testLambdaCommunication() throws Throwable {
        super.start(comm -> {
            final JvmComm.XJvmSupplier<String> greeting = comm.waitFor("Greet", 3, TimeUnit.SECONDS);
            log.log(Level.INFO, "Received {0}", greeting.get());
            comm.writeLambda("Reply", () -> "I'm fine, thanks!");
        }, 10, TimeUnit.SECONDS);
        comm.writeLambda("Greet", () -> "Hi, How are you?");
        final JvmComm.XJvmSupplier<String> reply = comm.waitFor("Reply", 5, TimeUnit.SECONDS);
        Assertions.assertEquals("I'm fine, thanks!", reply.get());
    }

    @Test
    public void test3WayLambdaCommunication() throws Throwable {

    }
}