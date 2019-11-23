package com.keon.projects.ipc;

public class JvmContext {

    private static boolean IS_REMOTE = false;

    /**
     * Once a single class subclasses this the entire JVM will have a Remote Context going forward.
     */
    public static abstract class RemoteContext {
        static {
            IS_REMOTE = true;
        }
    }

    public static boolean isRemoteContext() {
        return IS_REMOTE;
    }
}
