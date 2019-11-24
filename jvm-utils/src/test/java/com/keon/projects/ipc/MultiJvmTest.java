package com.keon.projects.ipc;

import com.keon.projects.ipc.test.MultiJVMTestSuite;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

public class MultiJvmTest extends MultiJVMTestSuite {

    @Test
    public void test() throws Exception {
        super.start(P1.class, 90, TimeUnit.SECONDS);
        comm.write("Hi", (Runnable)(() -> System.out.println("REMOTE ------------ Hello World!")));
    }

    @Test
    public void test3() throws Exception {
        comm.write("Hi", (Supplier<String> & Serializable)(() -> "Hey!"));
        final Supplier<String> s =comm.waitUntilAvailable("Hi", 2, TimeUnit.SECONDS);
        log.log(Level.INFO, "Received {0}", s.get());
    }

    public static class P1 extends RemoteJvm {

        @Override
        protected void run() throws Throwable {
            log.info("P1");
            final Runnable s =comm.waitUntilAvailable("Hi", 60, TimeUnit.SECONDS);
            s.run();
        }
    }

    public static class P2 extends RemoteJvm {

        @Override
        protected void run() throws Exception {
            log.info("P2");
        }
    }

}