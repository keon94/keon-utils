package com.keon.projects.ipc;

import com.keon.projects.ipc.test.MultiJVMTestSuite;
import org.junit.jupiter.api.Test;

public class MultiJvmTest extends MultiJVMTestSuite {

    @Test
    public void test() throws Exception {
            super.start(P1.class);
            super.start(P2.class);
    }

    public static class P1 extends RemoteJvm {

        @Override
        protected void run() throws Exception {
            log.info("P1");
            throw new Exception("kfhfh");
        }
    }

    public static class P2 extends RemoteJvm {

        @Override
        protected void run() throws Exception {
            log.info("P2");
        }
    }

}