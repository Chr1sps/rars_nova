package instructions.r32;

import instructions.AbstractInstructionTest;
import org.junit.jupiter.api.Test;

public final class DIVU extends AbstractInstructionTest {
    @Test
    public void test1() {
        runDivuTest("20", "6", "3");
    }

    @Test
    public void test2() {
        runDivuTest("-20", "6", "715827879");
    }

    @Test
    public void test3() {
        runDivuTest("20", "-6", "0");
    }

    @Test
    public void test4() {
        runDivuTest("-20", "-6", "0");
    }

    @Test
    public void test5() {
        runDivuTest("0x80000000", "1", "0x80000000");
    }

    @Test
    public void test6() {
        runDivuTest("0x80000000", "-1", "0");
    }

    @Test
    public void test7() {
        runDivuTest("0x80000000", "0", "-1");
    }

    @Test
    public void test8() {
        runDivuTest("1", "0", "-1");
    }

    @Test
    public void test9() {
        runDivuTest("0", "0", "-1");
    }

    private void runDivuTest(final String first, final String second, final String result) {
        runArithmeticTest32("divu", first, second, result);
    }
}
