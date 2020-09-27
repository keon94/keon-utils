package com.keon.projects.algorithms.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Timeout(5)
public class TimedScheduledExecutorTest {

    private TimedScheduledExecutor executor;

    @AfterEach
    public void afterEach() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void testSingleThreaded() throws Exception {
        executor = new TimedScheduledExecutor(1);
        final AtomicInteger count = new AtomicInteger(5);
        final long start = System.nanoTime();
        final Future<?> future = executor.scheduleAtFixedRate(count::set, 0, 200, TimeUnit.MILLISECONDS, count.get());
        future.get();
        final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        Assertions.assertTrue(duration > 1000, "Elapsed: " + duration + " ms");
        Assertions.assertEquals(0, count.get());
        assertEquals(0, executor.getQueue().size());
    }

    @Test
    public void testSingleThreaded_completeBeforeTimeout() throws Exception {
        executor = new TimedScheduledExecutor(1);
        final AtomicInteger count = new AtomicInteger(5);
        final Future<?> future = executor.scheduleAtFixedRate(count::set, 0, 100, TimeUnit.MILLISECONDS, count.get());
        future.get(1, TimeUnit.SECONDS);
        Assertions.assertTrue(future.isDone());
        Assertions.assertEquals(0, count.get());
        assertEquals(0, executor.getQueue().size());
    }

    @Test
    public void testSingleThreaded_timeout() throws Exception {
        executor = new TimedScheduledExecutor(1);
        final AtomicInteger count = new AtomicInteger(5);
        final Future<?> future = executor.scheduleAtFixedRate(count::set, 0, 200, TimeUnit.MILLISECONDS, count.get());
        Thread.sleep(25);
        Assertions.assertFalse(future.isDone());
        Assertions.assertThrows(TimeoutException.class, () -> future.get(125, TimeUnit.MILLISECONDS));
        Assertions.assertNotEquals(0, count.get());
        future.get();
        Assertions.assertTrue(future.isDone());
        Assertions.assertEquals(0, count.get());
        assertEquals(0, executor.getQueue().size());
    }

    @Test
    public void testMultiThreaded() throws Exception {
        executor = new TimedScheduledExecutor(2);
        final AtomicInteger count = new AtomicInteger(10);
        final long start = System.nanoTime();
        final Future<?> f1 = executor.scheduleAtFixedRate(count::set, 0, 200, TimeUnit.MILLISECONDS, count.get());
        final Future<?> f2 = executor.scheduleAtFixedRate(count::set, 0, 200, TimeUnit.MILLISECONDS, count.get());
        f1.get();
        f2.get();
        final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        Assertions.assertTrue(duration > 1000, "Elapsed: " + duration + " ms");
        Assertions.assertTrue(f1.isDone());
        Assertions.assertTrue(f2.isDone());
        Assertions.assertEquals(0, count.get());
        assertEquals(0, executor.getQueue().size());
    }

    @Test
    public void testSelfCompletion() throws Exception {
        executor = new TimedScheduledExecutor(1);
        final AtomicInteger count = new AtomicInteger(5);
        final Future<?> future = executor.scheduleAtFixedRate(remaining -> {
            count.set(remaining);
            return remaining == 2;
        }, 0, 200, TimeUnit.MILLISECONDS, count.get());
        future.get();
        Assertions.assertTrue(future.isDone());
        Assertions.assertEquals(2, count.get());
        assertEquals(0, executor.getQueue().size());

    }

    @Test
    public void testSelfCompletion_indefiniteTask() throws Exception {
        executor = new TimedScheduledExecutor(1);
        final AtomicInteger count = new AtomicInteger(5);
        final Future<?> future = executor.scheduleAtFixedRate(r -> {
            count.decrementAndGet();
            return count.get() == 2;
        }, 0, 200, TimeUnit.MILLISECONDS, Integer.MAX_VALUE);
        future.get();
        Assertions.assertTrue(future.isDone());
        Assertions.assertEquals(2, count.get());
        assertEquals(0, executor.getQueue().size());
    }

    @Test
    public void testExternalCancellation() throws Exception {
        executor = new TimedScheduledExecutor(2);
        final AtomicInteger count = new AtomicInteger(5);
        final Future<?> future = executor.scheduleAtFixedRate(remaining -> {
            System.out.println("Setting remainder to " + remaining);
            count.set(remaining);
        }, 0, 200, TimeUnit.MILLISECONDS, count.get());
        Thread.sleep(400);
        executor.submit(() -> future.cancel(true));
        Assertions.assertThrows(CancellationException.class, future::get);
        Assertions.assertTrue(future.isDone());
        Assertions.assertTrue(count.get() > 0, "value was " + count.get());
        while (!executor.getQueue().isEmpty()) {
            Thread.sleep(25);
        }
    }

    @Test
    public void testExceptionalTask() {
        executor = new TimedScheduledExecutor(1);
        final Future<?> future = executor.scheduleAtFixedRate(remaining -> {
            if (remaining == 2) {
                throw new RuntimeException("Exceptional test task throwing an exception.");
            }
        }, 0, 200, TimeUnit.MILLISECONDS, 5);
        Assertions.assertThrows(ExecutionException.class, future::get);
        Assertions.assertTrue(future.isDone());
    }
}