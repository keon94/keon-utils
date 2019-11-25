package com.keon.projects.ipc;

import java.io.IOException;
import java.io.Serializable;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static com.keon.projects.ipc.LogManager.error;
import static com.keon.projects.ipc.LogManager.log;

public class JvmComm {

    public static final String SHUTDOWN_JVM = "Shutdown JVM";
    public static final String TERMINATE_JVM = "Terminate JVM";
    public static final String LAMBDA_RUNNER = "Lambda Runner";

    private static final long MAX_BUFFER_MEMORY_BYTES = 4096;
    private static final long DEFAULT_AWAIT_TIMEOUT_SECONDS = 30;

    public final String commChannelName;
    public final Path commChannelPath;

    private static final Logger log = LogManager.getLogger();

    public JvmComm(final String sharedChannelName) throws IOException {
        this.commChannelName = sharedChannelName;
        commChannelPath = Paths.get(System.getProperty("java.io.tmpdir") + "/" + sharedChannelName + ".tmp");
    }

    //========================= Cross JVM communication methods ======================================================

    public XJvmFunction<?, ?> putLambda(final String key, final XJvmFunction<?, ?> function) throws IOException {
        return put(key, function);
    }

    public XJvmConsumer<?> putLambda(final String key, final XJvmConsumer<?> function) throws IOException {
        return put(key, function);
    }

    public XJvmSupplier<?> putLambda(final String key, final XJvmSupplier<?> function) throws IOException {
        return put(key, function);
    }

    public XJvmRunnable putLambda(final String key, final XJvmRunnable function) throws IOException {
        return put(key, function);
    }

    /**
     * Writes the key-value pair to the "store", retrieves the previous value if it exists, else null.
     *
     * @throws IOException
     */
    public <T> T put(final String key, final T value) throws IOException {
        return put(Collections.singletonMap(key, value)).get(key);
    }

    public <T> Map<String, T> put(final Map<String, T> entries) throws IOException {
        for (final T value : entries.values()) { //Sanitize
            if (value.getClass().isSynthetic() && !(value instanceof Serializable)) {
                throw new IllegalArgumentException(value.getClass() + " must implement Serializable");
            }
        }
        final KryoSerializer serializer = new KryoSerializer();
        Map<String, T> existingMap = null;
        final Map<String, T> previousMap = new HashMap<>();
        try (final FileChannel channel = getChannel()) {
            log(log, "Beginning to write key-values in {0}", Arrays.toString(entries.entrySet().toArray()));
            final MappedByteBuffer mmapedBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_BUFFER_MEMORY_BYTES);
            byte[] existingBytes = new byte[mmapedBuffer.limit()];
            mmapedBuffer.get(existingBytes);
            try {
                existingMap = serializer.deserialize(existingBytes);
                if (existingMap == null) {
                    existingMap = new HashMap<>();
                }
            } catch (final Exception e) {
                existingMap = new HashMap<>(); //thrown when there's nothing to deserialize. can improve this later
            }
            for (final Map.Entry<String, T> e : entries.entrySet()) {
                final T previous = existingMap.put(e.getKey(), e.getValue());
                previousMap.put(e.getKey(), previous);
            }
            mmapedBuffer.position(0);
            mmapedBuffer.put(new byte[mmapedBuffer.limit()]); //reset the buffer
            mmapedBuffer.position(0);
            mmapedBuffer.put(serializer.serialize(existingMap));
        } catch (final IOException e) {
            error(log, "Existing Keys: {0}", e, Arrays.toString(existingMap == null ? new Object[]{""} : existingMap.keySet().toArray()));
            throw e;
        }
        return previousMap;
    }

    /**
     * Removes the given keys from the "store"
     * TODO: Merge this into getInternal and modify its API
     *
     * @param keys
     * @throws IOException
     */
    public <T> void removeKeys(final List<String> keys) throws IOException {
        log(log, "Beginning to remove keys: {0}", keys);
        final KryoSerializer serializer = new KryoSerializer();
        Map<String, T> existingMap = null;
        try (final FileChannel channel = getChannel()) {
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
            error(log, "Existing Keys: {0}", e, Arrays.toString(existingMap == null ? new Object[]{""} : existingMap.keySet().toArray()));
        }
        log(log, "Finished removing keys: \"{0}\"", keys);
    }

    /**
     * Waits until a key is available in the "store", up to a fixed timeout period
     *
     * @param key
     * @return the value associated with that key
     * @throws TimeoutException
     * @throws IOException
     */
    public <T> T get(final String key) throws IOException, TimeoutException, InterruptedException {
        return get(key, DEFAULT_AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public <T> T get(final String key, final long timeout, final TimeUnit unit) throws IOException, TimeoutException, InterruptedException {
        log(log, "Beginning waiting to receive key \"{0}\"", key);
        try {
            return getInternal(key, timeout, unit, false);
        } finally {
            log(log, "Finished waiting to receive key: \"{0}\"", key);
        }
    }

    public <T> T remove(final String key) throws IOException, TimeoutException, InterruptedException {
        return remove(key, DEFAULT_AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public <T> T remove(final String key, final long timeout, final TimeUnit unit) throws IOException, TimeoutException, InterruptedException {
        log(log, "Beginning to remove key: {0}", key);
        final T result = getInternal(key, timeout, unit, true);
        log(log, "Finished removing key: \"{0}\"", key);
        return result;
    }

    private <T> T getInternal(final String key, final long timeout, final TimeUnit unit, final boolean remove) throws TimeoutException, InterruptedException, IOException {
        final KryoSerializer serializer = new KryoSerializer();
        final long begin = System.currentTimeMillis();
        while (true) {
            byte[] existingBytes = null;
            try (final FileChannel channel = getChannel()) {
                final MappedByteBuffer mmapedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, MAX_BUFFER_MEMORY_BYTES);
                existingBytes = new byte[mmapedBuffer.limit()];
                mmapedBuffer.get(existingBytes);
            }
            Map<String, T> existingMap = null;
            existingMap = serializer.deserialize(existingBytes);
            if (existingMap == null) {
                existingMap = new HashMap<>();
            } else if (existingMap.containsKey(key)) {
                if (remove) {
                    return existingMap.remove(key);
                }
                return existingMap.get(key);
            }
            if (existingMap != null && existingMap.containsKey(TERMINATE_JVM)) {
                if (!SHUTDOWN_JVM.equals(key)) {
                    error(log, "Existing Keys: {0}", Arrays.toString(existingMap == null ? new Object[]{""} : existingMap.keySet().toArray()));
                    throw new IllegalStateException("Key \"" + TERMINATE_JVM + "\" detected while not waiting for key \"" + SHUTDOWN_JVM + "\"!");
                }
                error(log, "Key \"{0}\" detected while waiting for key \"{1}\"! Exiting wait...", TERMINATE_JVM, SHUTDOWN_JVM);
                error(log, "Existing Keys: {0}", Arrays.toString(existingMap == null ? new Object[]{""} : existingMap.keySet().toArray()));
                return null;
            }
            if (System.currentTimeMillis() - begin > unit.toMillis(timeout)) {
                error(log, "Existing Keys: {0}", Arrays.toString(existingMap == null ? new Object[]{""} : existingMap.keySet().toArray()));
                throw new TimeoutException("Timed out waiting for key \"" + key + "\" to become available");
            }
            Thread.sleep(100);
        }
    }

    //=================================Helpers===================================================================

    private FileChannel getChannel() throws IOException {
        return FileChannel.open(commChannelPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
    }

    //===============================Cross Jvm Lambdas===========================================================

    public interface XJvmFunction<T, R> extends Serializable {
        R apply(T t) throws Throwable;
    }

    public interface XJvmConsumer<T> extends Serializable {
        void accept(T t) throws Throwable;
    }

    public interface XJvmSupplier<T> extends Serializable {
        T get() throws Throwable;
    }

    public interface XJvmRunnable extends Serializable {
        void run() throws Throwable;
    }
}
