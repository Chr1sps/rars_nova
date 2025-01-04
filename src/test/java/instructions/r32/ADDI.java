package instructions.r32;

import instructions.AbstractInstructionTest;
import org.junit.jupiter.api.Test;

public final class ADDI extends AbstractInstructionTest {
    @Test
    public void test1() {
        runAddiTest("0x00000000", "0x000", "0x00000000");
    }

    @Test
    public void test2() {
        runAddiTest("0x00000001", "0x001", "0x00000002");
    }

    @Test
    public void test3() {
        runAddiTest("0x00000003", "0x007", "0x0000000a");
    }

    @Test
    public void test4() {
        runAddiTest("0x00000000", "0xfffff800", "0xfffff800");
    }

    @Test
    public void test5() {
        runAddiTest("0x80000000", "0x000", "0x80000000");
    }

    @Test
    public void test6() {
        runAddiTest("0x80000000", "0xfffff800", "0x7ffff800");
    }

    @Test
    public void test7() {
        runAddiTest("0x00000000", "0x7ff", "0x000007ff");
    }

    @Test
    public void test8() {
        runAddiTest("0x7fffffff", "0x000", "0x7fffffff");
    }

    @Test
    public void test9() {
        runAddiTest("0x7fffffff", "0x7ff", "0x800007fe");
    }

    @Test
    public void test10() {
        runAddiTest("0x80000000", "0x7ff", "0x800007ff");
    }

    @Test
    public void test11() {
        runAddiTest("0x7fffffff", "0xfffff800", "0x7ffff7ff");
    }

    @Test
    public void test12() {
        runAddiTest("0x00000000", "0xffffffff", "0xffffffff");
    }

    @Test
    public void test13() {
        runAddiTest("0xffffffff", "0x001", "0x00000000");
    }

    @Test
    public void test14() {
        runAddiTest("0xffffffff", "0xffffffff", "0xfffffffe");
    }

    @Test
    public void test15() {
        runAddiTest("0x7fffffff", "0x001", "0x80000000");
    }

    @Test
    public void destinationTest1() {
        final String code = """
            li x1, 13
            addi x1, x1, 11
            li x29, 24
            bne x1, x29, fail
            """;
        runTest32(code);
    }

    @Test
    public void destinationTest2() {
        final String code = """
            addi x1, x0, 32
            li x29, 32
            bne x1, x29, fail
            """;
        runTest32(code);
    }

    @Test
    public void destinationTest3() {
        final String code = """
            li x1, 33
            addi x0, x1, 50
            li x29, 0
            bne x0, x29, fail
            """;
        runTest32(code);
    }

    private void runAddiTest(final String first, final String second, final String result) {
        runArithmeticImmediateTest32("addi", first, second, result);
    }
}
