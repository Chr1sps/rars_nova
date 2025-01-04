package instructions.r32;

import instructions.AbstractInstructionTest;
import org.junit.jupiter.api.Test;

public final class AND extends AbstractInstructionTest {
    @Test
    public void test1() {
        runAndTest("0xff00ff00", "0x0f0f0f0f", "0x0f000f00");
    }

    @Test
    public void test2() {
        runAndTest("0x0ff00ff0", "0xf0f0f0f0", "0x00f000f0");
    }

    @Test
    public void test3() {
        runAndTest("0x00ff00ff", "0x0f0f0f0f", "0x000f000f");
    }

    @Test
    public void test4() {
        runAndTest("0xf00ff00f", "0xf0f0f0f0", "0xf000f000");
    }

    @Test
    public void destinationTest1() {
        final var code = """
            li x1, 0xff00ff00
            li x2, 0x0f0f0f0f
            and x1, x1, x2
            li x29, 0x0f000f00
            bne x1, x29, fail
            """;
        runTest32(code);
    }

    @Test
    public void destinationTest2() {
        final var code = """
            li x1, 0x0ff00ff0
            li x2, 0xf0f0f0f0
            and x2, x1, x2
            li x29, 0x00f000f0
            bne x2, x29, fail
            """;
        runTest32(code);
    }

    @Test
    public void destinationTest3() {
        final var code = """
            li x1, 0xff00ff00
            and x1, x1, x1
            li x29, 0xff00ff00
            bne x1, x29, fail
            """;
        runTest32(code);
    }

    private void runAndTest(final String first, final String second, final String result) {
        runArithmeticTest32("and", first, second, result);
    }
}
