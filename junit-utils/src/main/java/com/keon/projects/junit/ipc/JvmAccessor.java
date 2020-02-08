package com.keon.projects.junit.ipc;

import com.keon.projects.ipc.JvmComm;
import com.keon.projects.ipc.JvmContext;
import com.keon.projects.ipc.misc.LogManager;
import com.keon.projects.ipc.process.JavaProcess;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.lang.reflect.Field;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.keon.projects.ipc.misc.LogManager.error;
import static com.keon.projects.ipc.misc.LogManager.log;

public class JvmAccessor implements Extension {

    private static final String COMM_CHANNEL = "comm_channel";
    private static final Logger LOG = LogManager.getLogger(JvmAccessor.class);

    private final List<JavaProcess> jvmPool = new ArrayList<>();
    private final JvmComm comm = new JvmComm(COMM_CHANNEL);

    public JavaProcess start(final Class<? extends RemoteJvm> clazz) throws Exception {
        return start(clazz, 30, TimeUnit.SECONDS);
    }

    public JavaProcess start(final JvmComm.XJvmConsumer<JvmComm> runner) throws Exception {
        return start(runner, 30, TimeUnit.SECONDS);
    }

    public JavaProcess start(final Class<? extends RemoteJvm> clazz, final long timeout, final TimeUnit unit) throws Exception {
        final JavaProcess jvm = new JavaProcess(clazz);
        jvm.timeout(timeout, unit).exec(clazz.getName());
        jvmPool.add(jvm);
        return jvm;
    }

    public JavaProcess start(final JvmComm.XJvmConsumer<JvmComm> runner, final long timeout, final TimeUnit unit) throws Exception {
        final JavaProcess jvm = new JavaProcess(RemoteJvm.class);
        jvm.timeout(timeout, unit).exec(JvmComm.LAMBDA_RUNNER + (jvmPool.size() + 1));
        jvmPool.add(jvm);
        comm.putLambda(JvmComm.LAMBDA_RUNNER + jvmPool.size(), runner);
        return jvm;
    }

    public JvmComm comm() {
        return this.comm;
    }

    public static Logger logger() {
        return LOG;
    }

    /**
     * subclasses of this class will run on remote JVMs
     */
    public static abstract class RemoteJvm extends JvmContext.RemoteContext {

        public static void main(String[] args) {
            log(LOG, "JVM main method called");
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
                    error(LOG, "Error getting a lambda executor", t);
                    System.exit(-2);
                    return;
                }
            } else {
                try {
                    comm = new JvmComm(COMM_CHANNEL);
                    jvm = (RemoteJvm) Class.forName(args[0]).newInstance();
                } catch (final LinkageError | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    error(LOG, "JVM could not instantiate {0}", e, args[0]);
                    System.exit(-1);
                    return;
                }
            }
            int exitCode = 0; // zero means success
            try {
                jvm.run(comm);
                comm.get(JvmComm.SHUTDOWN_JVM);
            } catch (final Throwable t) {
                LOG.log(Level.SEVERE, jvm.getClass().getName() + ": " + t.getMessage(), t);
                exitCode = 1;
            } finally {
                log(LOG, "{0} shutting down...", jvm.getClass().getName());
            }
            System.exit(exitCode);
        }

        protected abstract void run(final JvmComm comm) throws Throwable;

    }

    public static class LifeCycleManager implements BeforeEachCallback, AfterEachCallback, TestExecutionExceptionHandler {

        private JvmAccessor accessor;

        private LifeCycleManager() {}

        private static JvmAccessor getAccessor(final Object testInstance) throws IllegalAccessException {
            for(final Field f : testInstance.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if(f.getType() == JvmAccessor.class && f.isAnnotationPresent(RegisterExtension.class)) {
                    return (JvmAccessor) f.get(testInstance);
                }
            }
            return null;
        }

        @Override
        public void beforeEach(final ExtensionContext context) throws Exception {
            final Object testInstance = context.getRequiredTestInstance();
            accessor = getAccessor(testInstance);
            if(accessor == null) {
                throw new RuntimeException("No annotated JvmAccessor field found in the test suite");
            }
            Files.deleteIfExists(accessor.comm.getChannelPath()); //just to be safe
        }

        @Override
        public void afterEach(final ExtensionContext context) throws Exception {
            try {
                for (final JavaProcess jvm : accessor.jvmPool) {
                    try {
                        accessor.comm.put(JvmComm.SHUTDOWN_JVM, "true");
                    } finally {
                        if (jvm != null && !jvm.awaitTermination()) {
                            error(LOG, "Failed to properly terminate JVM running {0}", jvm.getMainClass().getName());
                        }
                    }
                }
            } finally {
                System.gc(); //removes mmaped file descriptor from memory so it can be deleted. Needs to be called as this is a JDK bug: https://bugs.openjdk.java.net/browse/JDK-4715154
            }
            //Retry a few times because there will be a delay until all JVMs release their access
            for (int i = 0; ; ++i) {
                try {
                    Files.deleteIfExists(accessor.comm.getChannelPath());
                    log(LOG, "{0} deleted after {1} retries", accessor.comm.getChannelPath().toString(), i);
                    break;
                } catch (final AccessDeniedException e) {
                    if (i == 10) {
                        error(LOG, "{0} could not be deleted after {1} retries", e, accessor.comm.getChannelPath().toString(), i);
                        break;
                    }
                    Thread.sleep(100);
                }
            }
        }

        @Override
        public void handleTestExecutionException(final ExtensionContext context, final Throwable throwable) throws Throwable {
            accessor.comm.put(JvmComm.TERMINATE_JVM, "true");
            throw throwable;
        }

    }

}
