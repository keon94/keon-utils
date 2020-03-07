package com.keon.projects.ipc.misc;

import com.keon.projects.ipc.JvmContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

public class LogManager {

    private LogManager() {
    }

    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

    public static Logger getLogger(final Class<?> clazz) {
        final Logger logger = java.util.logging.LogManager.getLogManager().getLogger(clazz.getName());
        if (logger != null) {
            return logger;
        }
        if (JvmContext.isRemoteContext()) {
            return getLogger(clazz.getName(), new CustomFormatter("REMOTE"));
        }
        return getLogger(clazz.getName(), new CustomFormatter("LOCAL"));
    }

    private static Logger getLogger(final String clazz, final CustomFormatter formatter) {

        final Logger logger = Logger.getLogger(clazz);
        logger.setUseParentHandlers(false);

        final StreamHandler handler = new StreamHandler(System.out, formatter) {
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
                stderrHandler.setFormatter(formatter);
            }
        };
        logger.addHandler(handler);
        return logger;
    }

    private static class CustomFormatter extends Formatter {

        private final String context;

        private CustomFormatter(final String context) {
            this.context = context;
        }

        @Override
        public String format(LogRecord record) {
            final StackTraceElement e = getCallSite();
            final String clazz = e == null ? record.getSourceClassName() : e.getClassName();
            final String method = e == null ? record.getSourceMethodName() : e.getMethodName();
            final Integer line = e == null ? null : e.getLineNumber();
            return MessageFormat.format(
                    "[" + context + "][{0}][PID-{1}][Thread-{2}][{3}.{4}({5})]" +
                            ":\n  {6} - {7}{8}\n",
                    DF.format(new Date(record.getMillis())),
                    PID,
                    Thread.currentThread().getName(),
                    clazz,
                    method,
                    formatArguments(clazz, method, line),
                    record.getLevel(),
                    super.formatMessage(record),
                    formatThrowable(record.getThrown()));
        }

        private StackTraceElement getCallSite() {
            final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int i = 0; i < stack.length; ++i) {
                if (stack[i].getClassName().equals(Logger.class.getName())) {
                    if (stack[i + 1].getClassName().equals(LogManager.class.getName()))
                        return stack[i + 2];
                    else if (!stack[i + 1].getClassName().equals(Logger.class.getName()))
                        return stack[i + 1];
                }
            }
            return null;
        }

        private static String formatThrowable(final Throwable t) {
            if (t == null) {
                return "";
            }
            final StringWriter sw = new StringWriter();
            try (final PrintWriter pw = new PrintWriter(sw) {
                @Override
                public void write(final String s) {
                    super.write("  " + s);
                }
            }) {
                pw.println();
                t.printStackTrace(pw);
            }
            return sw.toString();
        }

        private static String formatArguments(final String clazz, final String method, final Integer line) {
            return line == null ? "*" : "L" + line; //Nothing we can do to infer the arg signature
        }
    }

    //====================== Helper Log functions =====================================

    public static void log(final Logger log, final String message, final Object... o) {
        log.log(Level.INFO, message, o);
    }

    public static void error(final Logger log, final String message, final Object... o) {
        log.log(Level.SEVERE, message, o);
    }

    public static void error(final Logger log, final String message, final Throwable t, final Object... o) {
        log.log(Level.SEVERE, MessageFormat.format(message, o), t);
    }

}
