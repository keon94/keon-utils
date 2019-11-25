package com.keon.projects.ipc;

import com.keon.projects.ipc.test.MultiJVMTestSuite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.keon.projects.ipc.LogManager.log;

public class MultiJvmTest extends MultiJVMTestSuite {

    @Test
    public void testLambdaCommunication() throws Throwable {
        super.start(comm -> {
            final JvmComm.XJvmSupplier<String> greeting = comm.get("Greet", 3, TimeUnit.SECONDS);
            log(log, "Received {0}", greeting.get());
            comm.putLambda("Reply", () -> "I'm fine, thanks!");
        }, 10, TimeUnit.SECONDS);

        comm.putLambda("Greet", () -> "Hi, How are you?");
        final JvmComm.XJvmSupplier<String> reply = comm.get("Reply", 5, TimeUnit.SECONDS);
        Assertions.assertEquals("I'm fine, thanks!", reply.get());
    }

    @Test
    public void test3WayLambdaCommunication() throws Throwable {
        super.start(comm -> {
            comm.putLambda("Greet1", extension -> "I'm Remote1" + extension);
            final JvmComm.XJvmFunction<String,String> greeting = comm.get("Greet2", 5, TimeUnit.SECONDS);
            Assertions.assertEquals("I'm Remote2!", greeting.apply("!"));
            comm.putLambda("Finished1", () -> "Remote1 Done");
        }, 10, TimeUnit.SECONDS);
        super.start(comm -> {
            comm.putLambda("Greet2", extension -> "I'm Remote2" + extension);
            final JvmComm.XJvmFunction<String,String> greeting = comm.get("Greet1", 5, TimeUnit.SECONDS);
            Assertions.assertEquals("I'm Remote1!", greeting.apply("!"));
            comm.putLambda("Finished2", () -> "Remote2 Done");
        }, 10, TimeUnit.SECONDS);
        final JvmComm.XJvmSupplier<String> finished1 = comm.get("Finished1", 15, TimeUnit.SECONDS);
        Assertions.assertEquals("Remote1 Done", finished1.get());
        final JvmComm.XJvmSupplier<String> finished2 = comm.get("Finished2", 15, TimeUnit.SECONDS);
        Assertions.assertEquals("Remote2 Done", finished2.get());
    }

    @Test
    public void testComplexLambdas() throws Throwable {
    }

}