package com.keon.projects.threading;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import com.keon.projects.threading.DownStreamThreadLocal;

import org.junit.jupiter.api.Test;

import com.keon.projects.threading.HierrarchialThreads;

public class DownStreamThreadLocalTest {
    @Test
    public void test() throws Exception {
        final var exec = Executors.newFixedThreadPool(3, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return HierrarchialThreads.register(Executors.defaultThreadFactory().newThread(r));
            }
        });

        HierrarchialThreads.register(Thread.currentThread());

        final var t = new DownStreamThreadLocal<String>();
        final var t2 = new DownStreamThreadLocal<String>();
        t.set("id1");
        t2.set("id2");
        final Runnable r = () -> System.out.println(Thread.currentThread().getName() + ": " + t.get() + " " + t2.get());
        var f1 = exec.submit(r);
        var f2 = exec.submit(r);

        f1.get();
        f2.get();

        t.set("id2");

        exec.submit(r);
        exec.submit(r);
        exec.submit(r);

        final Runnable r2 = () -> {
            t2.set("id3");
            System.out.println(Thread.currentThread().getName() + ": " + t.get() + " " + t2.get());
        };

        exec.submit(r2).get();
        t2.get();
        System.out.println(Thread.currentThread().getName() + ": " + t.get() + " " + t2.get());
        exec.shutdown();
    }
}