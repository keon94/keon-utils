import com.keon.projects.ipc.JavaProcess;
import com.keon.projects.ipc.JvmComm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MultiJVMTestSuite.class)
public class MultiJVMTestSuite implements TestExecutionExceptionHandler {

    protected final JvmComm comm = new JvmComm("comm_channel");
    private final List<JavaProcess> jvmPool = new ArrayList<>();

    @BeforeEach
    public void before() throws IOException {
        Files.deleteIfExists(Paths.get(comm.commChannelPath));
    }

    @AfterEach
    public void after() throws IOException {
        for (final JavaProcess jvm : jvmPool) {
            try {
                comm.writeKey(JvmComm.SHUTDOWN_JVM, "true");
            } finally {
                if (jvm != null && !jvm.awaitTermination()) {
                    System.err.println("Failed to properly terminate JVM running " + jvm.getMainClass().getName());
                }
            }
        }
        //comm.clearFileChannel(); //Just to be safe...
        Files.delete(Paths.get(comm.commChannelPath));
    }

    protected JavaProcess start(final Class<? extends SubJvm> clazz) throws Exception {
        final JavaProcess jvm = new JavaProcess(SubJvm.class);
        jvm.exec(clazz.getName());
        jvmPool.add(jvm);
        return jvm;
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        comm.writeKey(JvmComm.TERMINATE_JVM, "true");
    }

    public static abstract class SubJvm {

        protected final JvmComm comm = new JvmComm("comm_channel");

        public static void main(String[] args) {
            System.out.println("Main...");
            int exitCode = 0; // zero means success
            final SubJvm jvm;
            try {
                jvm = (SubJvm) Class.forName(args[0]).newInstance();
            } catch (Exception e) {
                System.err.println("JVM could not instantiate " + args[0]);
                e.printStackTrace();
                System.exit(-1);
                return;
            }
            try {
                jvm.run();
                String x = jvm.comm.waitUntilAvailable(JvmComm.SHUTDOWN_JVM);
                System.out.println("Got " + x);
            } catch (final Throwable t) {
                t.printStackTrace();
                exitCode = 1;
            } finally {
                System.out.println(jvm.getClass().getName() + " shutting down...");
                System.exit(exitCode);
            }
        }

        protected abstract void run() throws Exception;

    }

}
