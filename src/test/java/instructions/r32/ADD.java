package instructions.r32;

import instructions.AbstractInstructionTest;
import org.junit.jupiter.api.Test;

public class ADD extends AbstractInstructionTest {
    @Test
    public void test1() {
        runAddTest("0x00000000", "0x00000000", "0x00000000");
    }

    @Test
    public void test2() {
        runAddTest("0x00000001", "0x00000001", "0x00000002");
    }

    @Test
    public void test3() {
        runAddTest("0x00000003", "0x00000007", "0x0000000a");
    }

    @Test
    public void test4() {
        runAddTest("0x00000000", "0xffff8000", "0xffff8000");
    }

    @Test
    public void test5() {
        runAddTest("0x80000000", "0x00000000", "0x80000000");
    }

    @Test
    public void test6() {
        runAddTest("0x80000000", "0xffff8000", "0x7fff8000");
    }

    @Test
    public void test7() {
        runAddTest("0x00000000", "0x00007fff", "0x00007fff");
    }

    @Test
    public void test8() {
        runAddTest("0x7fffffff", "0x00000000", "0x7fffffff");
    }

    @Test
    public void test9() {
        runAddTest("0x7fffffff", "0x00007fff", "0x80007ffe");
    }

    @Test
    public void test10() {
        runAddTest("0x80000000", "0x00007fff", "0x80007fff");
    }

    @Test
    public void test11() {
        runAddTest("0x7fffffff", "0xffff8000", "0x7fff7fff");
    }

    @Test
    public void test12() {
        runAddTest("0x00000000", "0xffffffff", "0xffffffff");
    }

    @Test
    public void test13() {
        runAddTest("0xffffffff", "0x00000001", "0x00000000");
    }

    @Test
    public void test14() {
        runAddTest("0xffffffff", "0xffffffff", "0xfffffffe");
    }

    @Test
    public void test15() {
        runAddTest("0x00000001", "0x7fffffff", "0x80000000");
    }

    @Test
    public void test16() {
        runAddTest("13", "11", "24");
    }

    @Test
    public void destinationTest1() {
        final var code = """
                li x1, 13
                li x2, 11
                add x1, x1, x2
                li x29, 24
                bne x1, x29, fail
                """;
        runTest32(code);
    }

    @Test
    public void destinationTest2() {
        final var code = """
                li x1, 14
                li x2, 11
                add x2, x1, x2
                li x29, 25
                bne x2, x29, fail
                """;
        runTest32(code);
    }

    @Test
    public void destinationTest3() {
        final var code = """
                li x1, 13
                add x1, x1, x1
                li x29, 26
                bne x1, x29, fail
                """;
        runTest32(code);
    }


    private void runAddTest(final String first, final String second, final String result) {
        runArithmeticTest32("add", first, second, result);
    }
}
