package com.keon.projects.ipc;

import com.keon.projects.ipc.test.MultiJVMTestSuite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.keon.projects.ipc.misc.LogManager.log;

public class MultiJvmTest extends MultiJVMTestSuite {

    @Test
    public void testLambdaCommunication() throws Throwable {
        super.start(comm -> {
            final JvmComm.XJvmSupplier<String> greeting = comm.get("Greet", 3, TimeUnit.SECONDS);
            log(log, "Received {0}", greeting.get());
            comm.putLambda("Reply", () -> "I'm fine, thanks!");
        }, 10, TimeUnit.SECONDS);

        comm.putLambda("Greet", () -> "Hi, How are you?");
        final JvmComm.XJvmSupplier<String> reply = comm.get("Reply", 5, TimeUnit.SECONDS);
        Assertions.assertEquals("I'm fine, thanks!", reply.get());
    }

    @Test
    public void test3WayLambdaCommunication() throws Throwable {
        super.start(comm -> {
            comm.putLambda("Greet1", extension -> "I'm Remote1" + extension);
            final JvmComm.XJvmFunction<String, String> greeting = comm.get("Greet2", 5, TimeUnit.SECONDS);
            Assertions.assertEquals("I'm Remote2!", greeting.apply("!"));
            comm.putLambda("Finished1", () -> "Remote1 Done");
        }, 10, TimeUnit.SECONDS);
        super.start(comm -> {
            comm.putLambda("Greet2", extension -> "I'm Remote2" + extension);
            final JvmComm.XJvmFunction<String, String> greeting = comm.get("Greet1", 5, TimeUnit.SECONDS);
            Assertions.assertEquals("I'm Remote1!", greeting.apply("!"));
            comm.putLambda("Finished2", () -> "Remote2 Done");
        }, 10, TimeUnit.SECONDS);
        final JvmComm.XJvmSupplier<String> finished1 = comm.get("Finished1", 15, TimeUnit.SECONDS);
        Assertions.assertEquals("Remote1 Done", finished1.get());
        final JvmComm.XJvmSupplier<String> finished2 = comm.get("Finished2", 15, TimeUnit.SECONDS);
        Assertions.assertEquals("Remote2 Done", finished2.get());
    }

    @Test
    public void testContention() throws Throwable {
        super.start(comm -> {
            comm.put("Start!", true);
            for (int i = 0; i < 150; ++i) {
                Thread.sleep(50);
                comm.put("" + i, i);
            }
            comm.remove("Finished!", 20, TimeUnit.SECONDS);
        }, 5, TimeUnit.SECONDS);
        comm.remove("Start!", 3, TimeUnit.SECONDS);
        for (int i = 150; i < 200; ++i) {
            Thread.sleep(50);
            comm.put("" + i, i);
        }
        comm.put("Finished!", true);
        final Set<String> range = IntStream.rangeClosed(0, 199).mapToObj(i -> "" + i).collect(Collectors.toSet());
        final Map<String, Integer> m = comm.remove(range);
        Assertions.assertEquals(200, m.size());
        for(final Map.Entry<String, Integer> e : m.entrySet()) {
            Assertions.assertEquals(e.getKey(), "" + e.getValue());
        }
    }

    @Test
    public void testComplexObjectLambda() throws Throwable {
        super.start(comm -> {
            {
                final List<String> list = Arrays.asList("1", "2", "3");
                final Integer i = 10;
                final Map<String, Object> m = new HashMap<>();
                m.put("list", list);
                m.put("int", i);
                comm.put(m);
            }
            final Map<String, String> m = new HashMap<>();
            m.put("A", "A");
            m.put("B", "B");
            comm.put(m);
        }, 3, TimeUnit.SECONDS);
        {
            final List<String> received = comm.get("list", 5, TimeUnit.SECONDS);
            Assertions.assertEquals(Arrays.asList("1", "2", "3"), received);
        }
        {
            final Integer received = comm.get("int", 1, TimeUnit.SECONDS);
            Assertions.assertEquals(10, received);
        }
        final Map<String, Object> m = comm.get(Arrays.asList("list", "int", "A", "B"));
        Assertions.assertEquals(Arrays.asList("1", "2", "3"), m.get("list"));
        Assertions.assertEquals(10, m.get("int"));
        Assertions.assertEquals("A", m.get("A"));
        Assertions.assertEquals("B", m.get("B"));
        Assertions.assertEquals(m, comm.remove(Arrays.asList("list", "int", "A", "B")));
    }

}