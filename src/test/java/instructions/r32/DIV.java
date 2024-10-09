package instructions.r32;

import instructions.AbstractInstructionTest;
import org.junit.jupiter.api.Test;

public final class DIV extends AbstractInstructionTest {
    @Test
    public void test1() {
        runDivTest("20", "6", "3");
    }

    @Test
    public void test2() {
        runDivTest("-20", "6", "-3");
    }

    @Test
    public void test3() {
        runDivTest("20", "-6", "-3");
    }

    @Test
    public void test4() {
        runDivTest("-20", "-6", "3");
    }

    @Test
    public void test5() {
        runDivTest("0x80000000", "1", "0x80000000");
    }

    @Test
    public void test6() {
        runDivTest("0x80000000", "-1", "0x80000000");
    }

    @Test
    public void test7() {
        runDivTest("0x80000000", "0", "-1");
    }

    @Test
    public void test8() {
        runDivTest("1", "0", "-1");
    }

    @Test
    public void test9() {
        runDivTest("0", "0", "-1");
    }

    private void runDivTest(final String first, final String second, final String result) {
        runArithmeticTest32("div", first, second, result);
    }
}
