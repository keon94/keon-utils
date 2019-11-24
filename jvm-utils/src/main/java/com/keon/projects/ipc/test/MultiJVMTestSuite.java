package com.keon.projects.ipc.test;

import com.keon.projects.ipc.JavaProcess;
import com.keon.projects.ipc.JvmComm;
import com.keon.projects.ipc.JvmContext;
import com.keon.projects.ipc.LogManager;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@ExtendWith(MultiJVMTestSuite.class)
public class MultiJVMTestSuite implements TestExecutionExceptionHandler {

    protected static final Logger log = LogManager.getLogger();
    private static final String COMM_CHANNEL = "comm_channel";

    protected final JvmComm comm = new JvmComm(COMM_CHANNEL);
    private final List<JavaProcess> jvmPool = new ArrayList<>();

    @BeforeEach
    public void before() throws IOException {
        Files.deleteIfExists(Paths.get(comm.commChannelPath)); //just to be safe
    }

    @AfterEach
    public void after() throws IOException {
        try {
            for (final JavaProcess jvm : jvmPool) {
                try {
                    comm.write(JvmComm.SHUTDOWN_JVM, "true");
                } finally {
                    if (jvm != null && !jvm.awaitTermination()) {
                        log.severe("Failed to properly terminate JVM running " + jvm.getMainClass().getName());
                    }
                }
            }
        } finally {
            System.gc(); //removes mmaped file descriptor from memory so it can be deleted. Needs to be called as this is a JDK bug: https://bugs.openjdk.java.net/browse/JDK-4715154
        }
        Files.deleteIfExists(Paths.get(comm.commChannelPath));
    }

    protected JavaProcess start(final Class<? extends RemoteJvm> clazz) throws Exception {
        return start(clazz, 30, TimeUnit.SECONDS);
    }

    protected JavaProcess start(final Class<? extends RemoteJvm> clazz, final long timeout, final TimeUnit unit) throws Exception {
        final JavaProcess jvm = new JavaProcess(clazz);
        jvm.timeout(timeout, unit).exec(clazz.getName());
        jvmPool.add(jvm);
        return jvm;
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        comm.write(JvmComm.TERMINATE_JVM, "true");
        throw throwable;
    }

    /**
     * subclasses of this class will run on remote JVMs
     */
    public static abstract class RemoteJvm extends JvmContext.RemoteContext {

        protected final JvmComm comm = new JvmComm(COMM_CHANNEL);
        protected static final Logger log = LogManager.getLogger();

        public static void main(String[] args) {
            log.info("Main...");
            final RemoteJvm jvm;
            try {
                jvm = (RemoteJvm) Class.forName(args[0]).newInstance();
            } catch (final LinkageError | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                log.log(Level.SEVERE, "JVM could not instantiate " + args[0], e);
                System.exit(-1);
                return;
            }
            int exitCode = 0; // zero means success
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


        protected abstract void run() throws Throwable;

    }

}
