package com.keon.projects.ipc.test;

import com.keon.projects.ipc.JvmComm;
import com.keon.projects.ipc.JvmContext;
import com.keon.projects.ipc.misc.LogManager;
import com.keon.projects.ipc.process.JavaProcess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.keon.projects.ipc.misc.LogManager.error;
import static com.keon.projects.ipc.misc.LogManager.log;

@ExtendWith(MultiJVMTestSuite.class)
public class MultiJVMTestSuite implements TestExecutionExceptionHandler {

    private static final String COMM_CHANNEL = "comm_channel";
    protected static final Logger log = LogManager.getLogger();

    private final List<JavaProcess> jvmPool = new ArrayList<>();
    protected final JvmComm comm;

    protected MultiJVMTestSuite() {
        try {
            this.comm = new JvmComm(COMM_CHANNEL);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void before() throws IOException {
        Files.deleteIfExists(comm.commChannelPath); //just to be safe
    }

    @AfterEach
    public void after() throws IOException, InterruptedException {
        try {
            for (final JavaProcess jvm : jvmPool) {
                try {
                    comm.put(JvmComm.SHUTDOWN_JVM, "true");
                } finally {
                    if (jvm != null && !jvm.awaitTermination()) {
                        error(log, "Failed to properly terminate JVM running {0}", jvm.getMainClass().getName());
                    }
                }
            }
        } finally {
            System.gc(); //removes mmaped file descriptor from memory so it can be deleted. Needs to be called as this is a JDK bug: https://bugs.openjdk.java.net/browse/JDK-4715154
        }
        //Retry a few times because there will be a delay until all JVMs release their access
        for (int i = 0; ; ++i) {
            try {
                Files.deleteIfExists(comm.commChannelPath);
                log(log, "{0} deleted after {1} retries", comm.commChannelPath.toString(), i);
                break;
            } catch (final AccessDeniedException e) {
                if (i == 10) {
                    error(log, "{0} could not be deleted after {1} retries", e, comm.commChannelPath.toString(), i);
                    break;
                }
                Thread.sleep(100);
            }
        }
    }

    protected JavaProcess start(final Class<? extends RemoteJvm> clazz) throws Exception {
        return start(clazz, 30, TimeUnit.SECONDS);
    }

    protected JavaProcess start(final JvmComm.XJvmConsumer<JvmComm> runner) throws Exception {
        return start(runner, 30, TimeUnit.SECONDS);
    }

    protected JavaProcess start(final Class<? extends RemoteJvm> clazz, final long timeout, final TimeUnit unit) throws Exception {
        final JavaProcess jvm = new JavaProcess(clazz);
        jvm.timeout(timeout, unit).exec(clazz.getName());
        jvmPool.add(jvm);
        return jvm;
    }

    protected JavaProcess start(final JvmComm.XJvmConsumer<JvmComm> runner, final long timeout, final TimeUnit unit) throws Exception {
        final JavaProcess jvm = new JavaProcess(RemoteJvm.class);
        jvm.timeout(timeout, unit).exec(JvmComm.LAMBDA_RUNNER + (jvmPool.size() + 1));
        jvmPool.add(jvm);
        comm.putLambda(JvmComm.LAMBDA_RUNNER + jvmPool.size(), runner);
        return jvm;
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        comm.put(JvmComm.TERMINATE_JVM, "true");
        throw throwable;
    }

    /**
     * subclasses of this class will run on remote JVMs
     */
    public static abstract class RemoteJvm extends JvmContext.RemoteContext {

        public static void main(String[] args) {
            log(log, "JVM main method called");
            final JvmComm comm;
            final RemoteJvm jvm;
            if (args[0].startsWith(JvmComm.LAMBDA_RUNNER)) {
                try {
                    comm = new JvmComm(COMM_CHANNEL);
                    final JvmComm.XJvmConsumer<JvmComm> runner = comm.get(args[0]);
                    jvm = new RemoteJvm() {
                        @Override
                        protected void run(JvmComm comm) throws Throwable {
                            runner.accept(comm);
                        }
                    };
                } catch (final Throwable t) {
                    error(log, "Error getting a lambda executor", t);
                    System.exit(-2);
                    return;
                }
            } else {
                try {
                    comm = new JvmComm(COMM_CHANNEL);
                    jvm = (RemoteJvm) Class.forName(args[0]).newInstance();
                } catch (final LinkageError | ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
                    error(log, "JVM could not instantiate {0}", e, args[0]);
                    System.exit(-1);
                    return;
                }
            }
            int exitCode = 0; // zero means success
            try {
                jvm.run(comm);
                comm.get(JvmComm.SHUTDOWN_JVM);
            } catch (final Throwable t) {
                log.log(Level.SEVERE, jvm.getClass().getName() + ": " + t.getMessage(), t);
                exitCode = 1;
            } finally {
                log(log, "{0} shutting down...", jvm.getClass().getName());
            }
            System.exit(exitCode);
        }

        protected abstract void run(final JvmComm comm) throws Throwable;

    }

}
