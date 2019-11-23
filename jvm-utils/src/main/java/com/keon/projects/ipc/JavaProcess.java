package com.keon.projects.ipc;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaProcess {

    private final Class<?> klass;
    private StartedProcess process;
    private static final long DEFAULT_PROCESS_TIMEOUT_SECONDS = 30;
    private static int DEBUG_PORT = 8100;

    private static final Logger log = LogManager.getLogger();

    /**
     * @param klass The class that the new JVM will execute (must have a main method)
     */
    public JavaProcess(final Class<?> klass) {
        this.klass = klass;
    }

    public Class<?> getMainClass() {
        return klass;
    }

    /**
     * Starts the JVM in the background.
     *
     * @param args
     * @throws Exception
     */
    public void exec(final String... args) throws Exception {
        final ProcessExecutor process = initJvm(Arrays.asList(args));
        this.process = process.destroyOnExit().start();
    }


    public boolean awaitTermination() {
        return awaitTermination(DEFAULT_PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Waits for the JVM to terminate up to a certain timeout.
     *
     * @return true if the JVM successfully terminated, false otherwise.
     */
    public boolean awaitTermination(final long duration, final TimeUnit unit) {
        final Future<ProcessResult> future = process.getFuture();
        try {
            final ProcessResult result = future.get(duration, unit);
            if (result.getExitValue() != 0) {
                log.severe("Received exit value: " + result.getExitValue());
                return false;
            }
            return true;
        } catch (final TimeoutException e) {
            log.severe(klass + " is still active after timeout period. Force killing it.");
            future.cancel(true);
            return false;
        } catch (ExecutionException | InterruptedException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            future.cancel(true);
            return false;
        } finally {
            log.info(Thread.currentThread().getName() + " finished executing JVM " + klass);
        }
    }

    private OutputStream getStream(final boolean isError) {
        if (isError) {
            return new LogOutputStream() {
                @Override
                protected void processLine(String line) {
                    System.err.println(line);
                }
            };
        } else {
            return new LogOutputStream() {
                @Override
                protected void processLine(String line) {
                    System.out.println(line);
                }
            };
        }
    }

    private ProcessExecutor initJvm(final List<String> args) {
        final String javaHome = System.getProperty("java.home");
        final String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        final String classpath = System.getProperty("java.class.path");
        final String className = klass.getName();
        final List<String> command = new ArrayList<>(Arrays.asList(javaBin, ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + DEBUG_PORT++), "-cp", classpath, className));
        command.addAll(args);
        log.info("Launching JVM with commands: " + command.toString());
        return new ProcessExecutor().command(command).redirectOutput(getStream(false)).redirectError(getStream(true));
    }
}
