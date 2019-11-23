package com.keon.projects.ipc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ExtendWith(MultiJVMTestSuite.class)
public class MultiJVMTestSuite implements TestExecutionExceptionHandler {

    protected static final Logger log = LogManager.getLogger(MultiJVMTestSuite.class);

    protected final JvmComm comm = new JvmComm("comm_channel");
    private final List<JavaProcess> jvmPool = new ArrayList<>();

    @BeforeEach
    public void before() throws IOException {
        Files.deleteIfExists(Paths.get(comm.commChannelPath)); //just to be safe
    }

    @AfterEach
    public void after() throws IOException {
        for (final JavaProcess jvm : jvmPool) {
            try {
                comm.writeKey(JvmComm.SHUTDOWN_JVM, "true");
            } finally {
                System.gc(); //removes mmaped file descriptor from memory so it can be deleted. Needs to be called as this is a JDK bug: https://bugs.openjdk.java.net/browse/JDK-4715154
                if (jvm != null && !jvm.awaitTermination()) {
                    log.severe("Failed to properly terminate JVM running " + jvm.getMainClass().getName());
                }
            }
        }
        Files.delete(Paths.get(comm.commChannelPath));
    }

    protected JavaProcess start(final Class<? extends SubJvm> clazz) throws Exception {
        final JavaProcess jvm = new JavaProcess(clazz);
        jvm.exec(clazz.getName());
        jvmPool.add(jvm);
        return jvm;
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        comm.writeKey(JvmComm.TERMINATE_JVM, "true");
    }

    @JavaProcess.Remote
    public static abstract class SubJvm {

        protected final JvmComm comm = new JvmComm("comm_channel");
        protected static final Logger log = LogManager.getLogger(SubJvm.class);

        public static void main(String[] args) {
            log.info("Main...");
            int exitCode = 0; // zero means success
            final SubJvm jvm;
            try {
                jvm = (SubJvm) Class.forName(args[0]).newInstance();
            } catch (Exception e) {
                log.log(Level.SEVERE, "JVM could not instantiate " + args[0], e);
                System.exit(-1);
                return;
            }
            try {
                jvm.run();
                jvm.comm.waitUntilAvailable(JvmComm.SHUTDOWN_JVM);
            } catch (final Throwable t) {
                log.log(Level.SEVERE, jvm.getClass().getName() + ": " + t.getMessage(), t);
                exitCode = 1;
            } finally {
                log.info(jvm.getClass().getName() + " shutting down...");
                System.exit(exitCode);
            }
        }

        protected abstract void run() throws Exception;

    }

}
