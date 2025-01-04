package instructions.r32;

import instructions.AbstractInstructionTest;
import org.junit.jupiter.api.Test;

public final class SLLI extends AbstractInstructionTest {
    @Test
    public void test1() {
        runSlliTest("0x00000001", "0", "0x00000001");
    }

    @Test
    public void test2() {
        runSlliTest("0x00000001", "1", "0x00000002");
    }

    @Test
    public void test3() {
        runSlliTest("0x00000001", "7", "0x00000080");
    }

    @Test
    public void test4() {
        runSlliTest("0x00000001", "14", "0x00004000");
    }

    @Test
    public void test5() {
        runSlliTest("0x00000001", "31", "0x80000000");
    }

    @Test
    public void test6() {
        runSlliTest("0xffffffff", "0", "0xffffffff");
    }

    @Test
    public void test7() {
        runSlliTest("0xffffffff", "1", "0xfffffffe");
    }

    @Test
    public void test8() {
        runSlliTest("0xffffffff", "7", "0xffffff80");
    }

    @Test
    public void test9() {
        runSlliTest("0xffffffff", "14", "0xffffc000");
    }

    @Test
    public void test10() {
        runSlliTest("0xffffffff", "31", "0x80000000");
    }

    @Test
    public void test11() {
        runSlliTest("0x21212121", "0", "0x21212121");
    }

    @Test
    public void test12() {
        runSlliTest("0x21212121", "1", "0x42424242");
    }

    @Test
    public void test13() {
        runSlliTest("0x21212121", "7", "0x90909080");
    }

    @Test
    public void test14() {
        runSlliTest("0x21212121", "14", "0x48484000");
    }

    @Test
    public void test15() {
        runSlliTest("0x21212121", "31", "0x80000000");
    }

    @Test
    public void srcDestTest() {
        final var code = """
            li x1, 0x00000001
            slli x1, x1, 7
            li x29, 0x00000080
            bne x1, x29, fail
            """;
        runTest32(code);
    }

    private void runSlliTest(String first, String intermediate, String result) {
        runArithmeticImmediateTest32("slli", first, intermediate, result);
    }
}
