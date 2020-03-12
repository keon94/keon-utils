package com.keon.projects.ipc;

import com.keon.projects.ipc.misc.KryoSerializer;
import com.keon.projects.ipc.misc.LogManager;

import java.io.IOException;
import java.io.Serializable;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.keon.projects.ipc.misc.LogManager.error;
import static com.keon.projects.ipc.misc.LogManager.log;

public class JvmComm {

    public static final String SHUTDOWN_JVM = "Shutdown JVM";
    public static final String TERMINATE_JVM = "Terminate JVM";
    public static final String LAMBDA_RUNNER = "Lambda Runner";

    private static final long MAX_BUFFER_MEMORY_BYTES = 4096;
    private static final long DEFAULT_AWAIT_TIMEOUT_SECONDS = 15;
    private static final Logger log = LogManager.getLogger(JvmComm.class);

    private final Path commChannelPath;

    public JvmComm(final String sharedChannelName) {
        this.commChannelPath = Paths.get(System.getProperty("java.io.tmpdir") + "/" + sharedChannelName + ".tmp");
    }

    public Path getChannelPath() {
        return this.commChannelPath;
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
        boolean success = false;
        try (final FileChannel channel = getChannel()) {
            channel.lock();
            //log(log, "Beginning to write key-values in {0}", Arrays.toString(entries.entrySet().toArray()));
            final MappedByteBuffer mmapedBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_BUFFER_MEMORY_BYTES);
            byte[] existingBytes = new byte[mmapedBuffer.limit()];
            mmapedBuffer.get(existingBytes);
            existingMap = serializer.deserialize(existingBytes);
            if (existingMap == null) {
                existingMap = new HashMap<>();
            }
            log(log, "put: Memory map has {0} keys: {1}", existingMap.size(), existingMap.keySet());
            for (final Map.Entry<String, T> e : entries.entrySet()) {
                final T previous = existingMap.put(e.getKey(), e.getValue());
                previousMap.put(e.getKey(), previous);
            }
            log(log, "put: Memory map will now have {0} keys: {1}", existingMap.size(), existingMap.keySet());
            mmapedBuffer.position(0);
            mmapedBuffer.put(new byte[mmapedBuffer.limit()]); //reset the buffer
            mmapedBuffer.position(0);
            mmapedBuffer.put(serializer.serialize(existingMap));
            success = true;
        } catch (final IOException e) {
            error(log, "Existing Keys: {0}", e, toString(existingMap == null ? null : existingMap.keySet()));
            throw e;
        } finally {
            if (success) {
                log(log, "Finished writing keys in {0}", toString(entries.keySet()));
            }
        }
        return previousMap;
    }

    private static <T> String toString(final Collection<T> c) {
        return c == null ? Collections.EMPTY_SET.toString() : new TreeSet<>(c).toString();
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
            return (T) getOrRemove(Collections.singleton(key), timeout, unit, false).get(key);
        } finally {
            log(log, "Finished waiting to receive key: \"{0}\"", key);
        }
    }

    public <T> Map<String, T> get(final Collection<String> keys) throws IOException, TimeoutException, InterruptedException {
        log(log, "Beginning waiting to receive {0} keys \"{1}\"", keys.size(), keys);
        try {
            return getOrRemove(new HashSet<>(keys), DEFAULT_AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS, false);
        } finally {
            log(log, "Finished waiting to receive {0} keys: \"{1}\"", keys.size(), keys);
        }
    }

    public <T> Map<String, T> get(final Collection<String> keys, final long timeout, final TimeUnit unit) throws IOException, TimeoutException, InterruptedException {
        log(log, "Beginning waiting to receive {0} keys \"{1}\"", keys.size(), keys);
        try {
            return getOrRemove(new HashSet<>(keys), timeout, unit, false);
        } finally {
            log(log, "Finished waiting to receive {0} keys: \"{1}\"", keys.size(), keys);
        }
    }

    public <T> T remove(final String key) throws IOException, TimeoutException, InterruptedException {
        return remove(key, DEFAULT_AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public <T> Map<String, T> remove(final Collection<String> keys) throws IOException, TimeoutException, InterruptedException {
        return remove(keys, DEFAULT_AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public <T> T remove(final String key, final long timeout, final TimeUnit unit) throws IOException, TimeoutException, InterruptedException {
        log(log, "Beginning to remove key: {0}", key);
        final T result = (T) getOrRemove(Collections.singleton(key), timeout, unit, true).get(key);
        log(log, "Finished removing key: \"{0}\"", key);
        return result;
    }

    public <T> Map<String, T> remove(final Collection<String> keys, final long timeout, final TimeUnit unit) throws IOException, TimeoutException, InterruptedException {
        log(log, "Beginning to remove {0} keys: {1}", keys.size(), keys);
        final Map<String, T> result = getOrRemove(new HashSet<>(keys), timeout, unit, true);
        log(log, "Finished removing {0} keys: \"{1}\"", keys.size(), keys);
        return result;
    }

    private <T> Map<String, T> getOrRemove(final Set<String> keys, final long timeout, final TimeUnit unit, final boolean remove) throws TimeoutException, InterruptedException, IOException {
        if (keys.isEmpty())
            return new HashMap<>();
        final KryoSerializer serializer = new KryoSerializer();
        final Map<String, T> results = new HashMap<>();
        final long begin = System.currentTimeMillis();
        while (true) {
            byte[] existingBytes = null;
            try (final FileChannel channel = getChannel()) {
                final MappedByteBuffer mmapedBuffer = remove ? channel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_BUFFER_MEMORY_BYTES) : channel.map(FileChannel.MapMode.READ_ONLY, 0, MAX_BUFFER_MEMORY_BYTES);
                existingBytes = new byte[mmapedBuffer.limit()];
                mmapedBuffer.get(existingBytes);
                final Map<String, T> existingMap;
                {
                    Map<String, T> temp = serializer.deserialize(existingBytes);
                    existingMap = temp == null ? new HashMap<>() : temp;
                }
                log(log, "getOrRemove: Memory map has {0} keys: {1}", existingMap.size(), existingMap.keySet());
                if (!existingMap.isEmpty()) {
                    final Set<String> newKeys = new HashSet<>();
                    for (final Entry<String, T> e : existingMap.entrySet()) {
                        if (keys.contains(e.getKey()) && results.put(e.getKey(), e.getValue()) == null) {
                            newKeys.add(e.getKey());
                        }
                    }
                    if (remove) {
                        channel.lock();
                        existingMap.entrySet().removeIf(e -> results.containsKey(e.getKey()));
                        mmapedBuffer.position(0);
                        mmapedBuffer.put(new byte[mmapedBuffer.limit()]); //reset the buffer
                        mmapedBuffer.position(0);
                        mmapedBuffer.put(serializer.serialize(existingMap));
                        channel.close();
                        if (!newKeys.isEmpty()) {
                            log(log, "Removed keys {0}. So far collected {1} keys ({2})", newKeys, results.size(), results.keySet());
                        }
                    } else {
                        channel.close();
                        if (!newKeys.isEmpty()) {
                            log(log, "Received keys {0}. So far received {1} keys ({2})", newKeys, results.size(), results.keySet());
                        }
                    }
                    if (results.keySet().equals(keys)) {
                        return results;
                    }
                }
                if (existingMap.containsKey(TERMINATE_JVM)) {
                    if (!keys.contains(SHUTDOWN_JVM)) {
                        error(log, "Existing Keys: {0}", toString(existingMap.keySet()));
                        throw new IllegalStateException("Key \"" + TERMINATE_JVM + "\" detected while not waiting for key \"" + SHUTDOWN_JVM + "\"!");
                    }
                    error(log, "Key \"{0}\" detected while waiting for key \"{1}\"! Exiting wait...", TERMINATE_JVM, SHUTDOWN_JVM);
                    error(log, "Existing Keys: {0}", toString(existingMap.keySet()));
                    return null;
                }
                if (results.size() < keys.size() && System.currentTimeMillis() - begin > unit.toMillis(timeout)) {
                    error(log, "Existing Keys in channel: {0}", toString(existingMap.keySet()));
                    final Set<String> remaining = keys.stream().filter(k -> !results.containsKey(k)).collect(Collectors.toSet());
                    throw new TimeoutException("Timed out waiting for " + remaining.size() + " remaining keys in \"" + toString(remaining) + "\" to become available");
                }
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
