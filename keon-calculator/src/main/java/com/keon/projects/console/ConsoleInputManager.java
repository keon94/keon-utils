package com.keon.projects.console;

import java.io.Closeable;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class ConsoleInputManager implements Closeable {

    public static final String LINE_BEGIN = "> ";

    @SuppressWarnings("serial")
    private final List<String> inputHistory = new ArrayList<>() {
        public boolean add(final String str) {
            if (isEmpty() || !super.get(size() - 1).equals(str)) {
                super.add(str);
            }
            return true;
        }
    };
    private final Lock inputLock = new ReentrantLock();
    private final Scanner scanner;

    // mutable
    private String requestedPreviousInput;
    private int index = 0;

    public ConsoleInputManager(final InputStream in, final PrintStream out) {
        GlobalScreen.addNativeKeyListener(this.new ConsoleKeyListener(out));
        this.scanner = new Scanner(in);
    }

    public String getRequestedInput() {
        String cmd = null;
        boolean read;
        while (true) {
            read = false;
            try {
                cmd = scanner.nextLine().replaceAll("[^\\x20-\\x7E]", "");
                read = true;
                inputLock.lock();
                if (requestedPreviousInput != null) {
                    cmd = requestedPreviousInput + cmd;
                    inputHistory.add(cmd);
                    return cmd;
                } else if (cmd != null && !cmd.isEmpty()) {
                    inputHistory.add(cmd);
                    return cmd;
                }
            } finally {
                index = inputHistory.size() - 1;
                requestedPreviousInput = null;
                // System.out.println("\ncmd:" + cmd + ";index:" + index + ";history:" +
                // inputHistory);
                if (read)
                    inputLock.unlock();
            }
        }
    }

    @Override
    public void close() {
        this.scanner.close();
    }

    // ===========================================================

    private class ConsoleKeyListener implements NativeKeyListener {

        private final PrintStream out;

        public ConsoleKeyListener(final PrintStream out) {
            this.out = out;
        }

        @Override
        public void nativeKeyPressed(NativeKeyEvent e) {
            final int key = e.getKeyCode();
            try {
                inputLock.lock();
                if (!inputHistory.isEmpty()) {
                    // System.out.print(";Pressed: " + NativeKeyEvent.getKeyText(key));
                    if (key == NativeKeyEvent.VC_UP) {
                        requestedPreviousInput = inputHistory.get(index == 0 ? 0 : index--);
                        clearLine();
                        out.print(requestedPreviousInput);
                    } else if (key == NativeKeyEvent.VC_DOWN) {
                        requestedPreviousInput = inputHistory.get(index == inputHistory.size() - 1 ? index : index++);
                        clearLine();
                        out.print(requestedPreviousInput);
                    }
                }
            } finally {
                inputLock.unlock();
            }
        }

        @Override
        public void nativeKeyTyped(NativeKeyEvent nativeEvent) {
            //
        }

        @Override
        public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
            //
        }

        private void clearLine() {
            // out.println();
            // out.print("\033[F\r" + LINE_BEGIN);
            out.print("\r" + LINE_BEGIN);
            out.flush();
        }
    }

    static {
        try {
            Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(Level.OFF);
            GlobalScreen.registerNativeHook();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }, "NativeHookShutDown"));
        } catch (NativeHookException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}