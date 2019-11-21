import org.junit.jupiter.api.Test;

public class MultiJvmTest extends MultiJVMTestSuite {

    @Test
    public void test() throws Exception {
            super.start(P1.class);
            super.start(P2.class);
    }

    public static class P1 extends SubJvm {

        @Override
        protected void run() throws Exception {
            System.out.println("P1...");
        }
    }

    public static class P2 extends SubJvm {

        @Override
        protected void run() throws Exception {
            System.out.println("P2...");
        }
    }

}