package com.keon.projects.ipc;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class JvmComm {

    public static final String SHUTDOWN_JVM = "Shutdown JVM";
    public static final String TERMINATE_JVM = "Terminate JVM";

    private static final long MAX_BUFFER_MEMORY_BYTES = 4096;
    private static final long AWAIT_TIMEOUT_MILLIS = 30000;

    public final String commChannelPath;

    public final String commChannelName;

    public JvmComm(final String sharedChannelName) {
        this.commChannelName = sharedChannelName;
        commChannelPath = Paths.get("src","test","resources", commChannelName).toAbsolutePath().toString();
    }

    //========================= Cross JVM communication methods ======================================================

    /**
     * Writes the key-value pair to the "store".
     * @throws IOException
     */
    public <T> void writeKey(final String key, final T value) throws IOException {
        debug("Beginning to write key: \"" + key + "\" value: \"" + value + "\"");
        final DataSerializer serializer = new DataSerializer();
        Map<String, T> existingMap = null;
        try (final FileChannel channel = getChannel()) {
            final MappedByteBuffer mmapedBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_BUFFER_MEMORY_BYTES);
            byte[] existingBytes = new byte[mmapedBuffer.limit()];
            mmapedBuffer.get(existingBytes);
            try {
                existingMap = serializer.deserialize(existingBytes);
                if(existingMap == null) {
                    existingMap = new HashMap<>();
                }
            } catch (final Exception e) {
                existingMap = new HashMap<>(); //thrown when there's nothing to deserialize. can improve this later
            }
            existingMap.put(key, value);
            mmapedBuffer.position(0);
            mmapedBuffer.put(new byte[mmapedBuffer.limit()]); //reset the buffer
            mmapedBuffer.position(0);
            mmapedBuffer.put(serializer.serialize(existingMap));
        } catch (final IOException e) {
            debugError("Existing Keys: " + Arrays.toString(existingMap == null ? new Object[] {""} : existingMap.keySet().toArray()));
            throw e;
        }
        debug("Finished writing key: \"" + key + "\" value: \"" + value + "\"");
    }

    /**
     * Removes the given keys from the "store"
     * @param keys
     * @throws IOException
     */
    public <T> void removeKeys(final List<String> keys) throws IOException {
        debug("Beginning to remove keys: " + keys);
        final DataSerializer serializer = new DataSerializer();
        Map<String, T> existingMap = null;
        try (final FileChannel channel = getChannel();) {
            final MappedByteBuffer mmapedBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_BUFFER_MEMORY_BYTES);
            byte[] existingBytes = new byte[mmapedBuffer.limit()];
            mmapedBuffer.get(existingBytes);
            try {
                existingMap = serializer.deserialize(existingBytes);
            } catch (final Exception e) {
                existingMap = new HashMap<>(); //thrown when there's nothing to deserialize. can improve this later
            }
            for (final String key : keys) {
                existingMap.remove(key);
            }
            mmapedBuffer.position(0);
            mmapedBuffer.put(new byte[mmapedBuffer.limit()]); //reset the buffer
            mmapedBuffer.position(0);
            mmapedBuffer.put(serializer.serialize(existingMap));
        } catch (final IOException e) {
            debugError("Existing Keys: " + Arrays.toString(existingMap == null ? new Object[] {""} : existingMap.keySet().toArray()));
            e.printStackTrace();
        }
        debug("Finished removing keys: " + keys);
    }

    /**
     * Removes a key from the "store"
     * @param key
     * @throws IOException
     */
    public void removeKey(final String key) throws IOException {
        removeKeys(Arrays.asList(key));
    }

    /**
     * Waits until a key is available in the "store", up to a fixed timeout period
     * @param key
     * @return the value associated with that key
     * @throws TimeoutException
     * @throws IOException
     */
    public <T> T waitUntilAvailable(final String key) throws IOException, TimeoutException, InterruptedException {
        return waitUntilAvailable(key, AWAIT_TIMEOUT_MILLIS);
    }

    <T> T waitUntilAvailable(final String key, final long timeoutMillis) throws IOException, TimeoutException, InterruptedException {
        debug("Beginning waiting to receive key \"" + key + "\"");
        try {
            final DataSerializer serializer = new DataSerializer();
            final long begin = System.currentTimeMillis();
            while (true) {
                try (final FileChannel channel = getChannel();) {
                    final MappedByteBuffer mmapedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, MAX_BUFFER_MEMORY_BYTES);
                    byte[] existingBytes = new byte[mmapedBuffer.limit()];
                    mmapedBuffer.get(existingBytes);
                    Map<String, T> existingMap = null;
                    try {
                        existingMap = serializer.deserialize(existingBytes);
                        if (existingMap.containsKey(key)) {
                            return existingMap.get(key);
                        }
                    } catch (final Exception e) {
                        //ignore - thrown when there's nothing to deserialize. can improve this later
                    }
                    Thread.sleep(200);
                    if (existingMap != null && existingMap.containsKey(TERMINATE_JVM)) {
                        if(!SHUTDOWN_JVM.equals(key)) {
                            debugError("Existing Keys: " + Arrays.toString(existingMap == null ? new Object[] {""} : existingMap.keySet().toArray()));
                            throw new IllegalStateException("Key \"" + TERMINATE_JVM + "\" detected while not waiting for key \"" + SHUTDOWN_JVM + "\"!");
                        }
                        debugError("Key \"" + TERMINATE_JVM + "\" detected while waiting for key \"" + SHUTDOWN_JVM + "\"! Exiting wait...");
                        debugError("Existing Keys: " + Arrays.toString(existingMap == null ? new Object[] {""} : existingMap.keySet().toArray()));
                        return null;
                    }
                    if (System.currentTimeMillis() - begin > timeoutMillis) {
                        debugError("Existing Keys: " + Arrays.toString(existingMap == null ? new Object[] {""} : existingMap.keySet().toArray()));
                        throw new TimeoutException("Timed out waiting for key \"" + key + "\" to become available");
                    }
                }
            }
        } finally {
            debug("Finished waiting to receive key \"" + key + "\"");
        }
    }

    public static void debug(final String msg) {
        System.out.println("[DEBUG]" + "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + "]: " + msg);
    }

    public static void debugError(final String msg) {
        System.err.println("[ERROR]" + "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + "]: " + msg);
    }

    //=================================Helpers===================================================================

    public void clearFileChannel() {
        try (final FileChannel channel = getChannel()) {
            final MappedByteBuffer mmapedBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_BUFFER_MEMORY_BYTES);
            byte[] existingBytes = new byte[mmapedBuffer.limit()];
            mmapedBuffer.get(existingBytes);
            mmapedBuffer.position(0);
            mmapedBuffer.put(new byte[mmapedBuffer.limit()]); //reset the buffer
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private FileChannel getChannel() throws IOException {
        final File file = new File(commChannelPath);
        file.setReadable(true, false);
        file.setWritable(true, false);
        file.setExecutable(true, false);
        return FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
    }
}
