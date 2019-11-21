package com.keon.projects.ipc;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JavaProcess {

    private final Class<?> klass;
    private Process process;
    private static final long PROCESS_TIMEOUT_MILLIS = 30000;
    private static int DEBUG_PORT = 8100;

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
     * @param args
     * @throws Exception
     */
    public void exec(final String... args) throws Exception {
        final ProcessBuilder processBuilder = initJvm(Arrays.asList(args));
        processBuilder.redirectErrorStream(true);
        final Thread streamReaderThread = new Thread(streamReader(), "streamReader");
        process = processBuilder.start();
        streamReaderThread.start();
    }

    /**
     * Waits for the JVM to terminate up to a certain timeout. That JVM's logs are also redirected to this JVM's standard output upon its completion.
     * @return true if the JVM successfully terminated, false otherwise.
     */
    public boolean awaitTermination() {
        try {
            if (!process.waitFor(PROCESS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                System.err.println(klass + " is still active after timeout period. Force killing it.");
                process.destroy();
                return false;
            } else if (process.exitValue() != 0) {
                return false;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null && process.isAlive()) {
                System.err.println(klass + " is still active. Force killing it.");
                process.destroyForcibly();
            }
            System.out.println(Thread.currentThread().getName() + " finished executing JVM " + klass);
        }
        return true;
    }

    private Runnable streamReader() {
        return () -> {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            final StringBuilder builder = new StringBuilder();
            String line = null;

            try {
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                    builder.append(System.getProperty("line.separator"));
                }
                System.out.println("-----------------[REMOTE - " + klass + "]---------------------");
                System.out.println(builder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    private ProcessBuilder initJvm(final List<String> args) {
        final String javaHome = System.getProperty("java.home");
        final String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        final String classpath = System.getProperty("java.class.path");
        final String className = klass.getName();
        final List<String> command = new ArrayList<>(Arrays.asList(javaBin, ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + DEBUG_PORT++), "-cp", classpath, className));
        command.addAll(args);
        System.out.println("Launching JVM with commands: " + command.toString());
        return new ProcessBuilder(command);
    }
}
