package com.keon.projects.concurrency;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Predicate;


/**
 * An extension to the {@link ScheduledThreadPoolExecutor} that lets users specify an upper bound on the number of times
 * periodic tasks should be run. Furthermore, the extension API gives the running tasks the ability to self-manage; that is,
 * by keeping a reference to their own Futures they can prematurely complete/terminate or cancel out of their schedule.
 *
 * @author KA046117
 */
public class TimedScheduledExecutor extends ScheduledThreadPoolExecutor {

    public TimedScheduledExecutor(final int corePoolSize) {
        super(corePoolSize);
    }

    public TimedScheduledExecutor(final int corePoolSize, final String threadGroup) {
        super(corePoolSize, r -> new Thread(r, threadGroup));
    }

    /**
     * Schedules a task which reruns for specified periods of time. The task may be prematurely completed or cancelled.
     * @param function the task: input is the number of cycles remaining. return true to indicate completion, thereby preventing further rescheduling.
     * @param initialDelay the initial delay before kicking off the task
     * @param period the length of each period
     * @param unit the units of time
     * @param cycles the number of periods to run the task
     * @return A {@link ScheduledFuture} to the consumer.
     */
    public ScheduledFuture<?> scheduleAtFixedRate(final Predicate<Integer> function, final long initialDelay, final long period, final TimeUnit unit, final int cycles) {
        final CompletableScheduledFuture<Void> future = new CompletableScheduledFuture<>();
        final SelfReschedulingRunnable runnable = new SelfReschedulingRunnable(function, future, period, unit, cycles);
        future.decoratedFuture = super.schedule(runnable, initialDelay, unit);
        runnable.startLatch.countDown();
        return future;
    }

    /**
     * Specialization of {@link #scheduleAtFixedRate(Predicate, long, long, TimeUnit, int)}, with no time-limited period.
     */
    public ScheduledFuture<?> scheduleAtFixedRate(final BooleanSupplier function, final long initialDelay, final long period, final TimeUnit unit) {
        return scheduleAtFixedRate(r -> {
            return function.getAsBoolean();
        }, initialDelay, period, unit, Integer.MAX_VALUE);
    }

    /**
     * Specialization of {@link #scheduleAtFixedRate(Predicate, long, long, TimeUnit, int)}, with no time-limited period, and no ability to self-complete,
     * but still having a reference to the remaining cycles.
     */
    public ScheduledFuture<?> scheduleAtFixedRate(final IntConsumer function, final long initialDelay, final long period, final TimeUnit unit, final int cycles) {
        return scheduleAtFixedRate(r -> {
            function.accept(r);
            return false;
        }, initialDelay, period, unit, cycles);
    }

    /**
     * A {@link ScheduledFuture} that can be explicitly completed by the task thread, similar to {@link java.util.concurrent.CompletableFuture},
     * but whose lifecycle is also managed by the underlying Executor Service.
     * @param <V> future type
     */
    private static class CompletableScheduledFuture<V> implements ScheduledFuture<V> {

        private volatile ScheduledFuture<V> decoratedFuture;
        private final CountDownLatch completionStatus = new CountDownLatch(1); // 0: complete, 1: not

        private CompletableScheduledFuture() {}

        @Override
        public long getDelay(final TimeUnit unit) {
            return decoratedFuture.getDelay(unit);
        }

        @Override
        public int compareTo(final Delayed d) {
            return decoratedFuture.compareTo(d);
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            final boolean cancel = decoratedFuture.cancel(mayInterruptIfRunning);
            completionStatus.countDown();
            return cancel;
        }

        @Override
        public boolean isCancelled() {
            return decoratedFuture.isCancelled();
        }

        @Override
        public boolean isDone() {
            return decoratedFuture.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            try {
                return get(Long.MAX_VALUE, NANOSECONDS); // -> 292 years
            } catch (final TimeoutException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public V get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            final long start = System.nanoTime();
            if (!completionStatus.await(timeout, unit)) {
                throw new TimeoutException("Timed out waiting for future to complete.");
            }
            //should be at least "almost" complete now
            return decoratedFuture.get(System.nanoTime() - start, NANOSECONDS);
        }

        void complete() {
            completionStatus.countDown();
        }

    }

    private class SelfReschedulingRunnable implements Callable<Void> {

        final Predicate<Integer> task;
        final CompletableScheduledFuture<?> future;
        final CountDownLatch startLatch = new CountDownLatch(1); //needed because otherwise the above future may be null when run() hits it at the beginning
        final long period;
        final TimeUnit unit;
        int remainingExecutions;

        SelfReschedulingRunnable(final Predicate<Integer> task, final CompletableScheduledFuture<Void> future, final long period, final TimeUnit unit, final int cycles) {
            this.remainingExecutions = cycles;
            this.future = future;
            this.task = task;
            this.period = period;
            this.unit = unit;
        }

        @Override
        public Void call() throws Exception {
            try {
                startLatch.await();
                runInternal();
                return null;
            } catch (final Exception e) {
                future.complete();
                throw e;
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void runInternal() {
            final boolean isDone = task.test(remainingExecutions);
            if (isDone || remainingExecutions == 0) {
                future.complete();
                return;
            }
            remainingExecutions--;
            final ScheduledFuture<?> nextFuture = schedule(this, period, unit);
            future.decoratedFuture = (ScheduledFuture) nextFuture;
        }

    }
}
