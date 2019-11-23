package com.keon.projects.ipc;

import sun.reflect.Reflection;

import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class LogManager {

    private LogManager() {
    }

    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    ;

    public static Logger getCallbackLogger() {
        return getLocalLogger(Reflection.getCallerClass(2));
    }

    public static Logger getLogger() {
        return getLocalLogger(Reflection.getCallerClass(1));
    }

    public static Logger getLogger(final Class<?> clazz) {
        Class<?> klass = clazz;
        do {
            if (clazz.isAnnotationPresent(JavaProcess.Remote.class)) {
                return getRemoteLogger(clazz);
            }
            klass = klass.getSuperclass();
        } while (klass != null);
        return getLocalLogger(clazz);
    }

    private static Logger getLocalLogger(final Class<?> clazz) {
        return getLogger(clazz, record -> {
                    return MessageFormat.format(
                            "[{0}][Local: PID-{1}][Thread-{2}][{3}.{4}({5})]" +
                                    ":\n  {6} - {7}\n",
                            DF.format(new Date(record.getMillis())),
                            PID,
                            Thread.currentThread().getName(),
                            record.getSourceClassName(),
                            record.getSourceMethodName(),
                            record.getParameters(),
                            record.getLevel(),
                            record.getMessage());
                }
        );
    }

    private static Logger getRemoteLogger(final Class<?> clazz) {
        return getLogger(clazz, record -> {
                    return MessageFormat.format(
                            "[{0}][REMOTE: PID-{1}][Thread-{2}][{3}.{4}({5})]" +
                                    ":\n  {6} - {7}\n",
                            DF.format(new Date(record.getMillis())),
                            PID,
                            Thread.currentThread().getName(),
                            record.getSourceClassName(),
                            record.getSourceMethodName(),
                            record.getParameters(),
                            record.getLevel(),
                            record.getMessage());
                }
        );
    }

    private static Logger getLogger(final Class<?> clazz, final Function<LogRecord, String> formatterFunction) {

        final Logger logger = Logger.getLogger(clazz.getName());
        logger.setUseParentHandlers(false);

        final StreamHandler handler = new StreamHandler(System.out, new SimpleFormatter()) {
            {
                stderrHandler = new ConsoleHandler();
                init();
            }

            private final ConsoleHandler stderrHandler;

            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() <= Level.INFO.intValue()) {
                    super.publish(record);
                    super.flush();
                } else {
                    stderrHandler.publish(record);
                    stderrHandler.flush();
                }
            }

            private void init() {
                final Formatter formatter = new Formatter() {
                    @Override
                    public String format(LogRecord record) {
                        return formatterFunction.apply(record);
                    }
                };
                this.setFormatter(formatter);
                stderrHandler.setFormatter(formatter);
            }
        };
        logger.addHandler(handler);
        return logger;
    }
}
