package com.keon.projects.ipc;

public class JvmContext {

    private static boolean IS_REMOTE = false;

    public static abstract class RemoteContext {
        static {
            IS_REMOTE = true;
        }
    }

    public static boolean isRemoteContext() {
        return IS_REMOTE;
    }
}
