package instructions.r32;

import instructions.AbstractInstructionTest;
import org.junit.jupiter.api.Test;

/*
copy these:

test_2:
 li x1, 0xff00ff00
 andi x30, x1, 0xffffff0f
 li x29, 0xff00ff00
 li gp, 2
 bne x30, x29, fail


test_3:
 li x1, 0x0ff00ff0
 andi x30, x1, 0x0f0
 li x29, 0x000000f0
 li gp, 3
 bne x30, x29, fail


test_4:
 li x1, 0x00ff00ff
 andi x30, x1, 0x70f
 li x29, 0x0000000f
 li gp, 4
 bne x30, x29, fail


test_5:
 li x1, 0xf00ff00f
 andi x30, x1, 0x0f0
 li x29, 0x00000000
 li gp, 5
 bne x30, x29, fail



  #-------------------------------------------------------------
  # Source/Destination tests
  #-------------------------------------------------------------

test_6:
 li x1, 0xff00ff00
 andi x1, x1, 0x0f0
 li x29, 0x00000000
 li gp, 6
 bne x1, x29, fail

test_13:
 andi x1, x0, 0x0f0
 li x29, 0
 li gp, 13
 bne x1, x29, fail


test_14:
 li x1, 0x00ff00ff
 andi x0, x1, 0x70f
 li x29, 0
 li gp, 14
 bne x0, x29, fail

 */

public final class ANDI extends AbstractInstructionTest {
    @Test
    public void test1() {
        runAndiTest("0xff00ff00", "0xffffff0f", "0xff00ff00");
    }

    @Test
    public void test2() {
        runAndiTest("0x0ff00ff0", "0x0f0", "0x000000f0");
    }

    @Test
    public void test3() {
        runAndiTest("0x00ff00ff", "0x70f", "0x0000000f");
    }

    @Test
    public void test4() {
        runAndiTest("0xf00ff00f", "0x0f0", "0x00000000");
    }

    @Test
    public void destSrcTest1() {
        final var code = """
                li x1, 0xff00ff00
                andi x1, x1, 0x0f0
                li x29, 0x00000000
                bne x1, x29, fail
                """;
        runTest32(code);
    }

    @Test
    public void destSrcTest2() {
        final var code = """
                andi x1, x0, 0x0f0
                li x29, 0
                bne x1, x29, fail
                """;
        runTest32(code);
    }

    @Test
    public void destSrcTest3() {
        final var code = """
                li x1, 0x00ff00ff
                andi x0, x1, 0x70f
                li x29, 0
                bne x0, x29, fail
                """;
        runTest32(code);
    }

    private void runAndiTest(final String first, final String second, final String result) {
        runArithmeticImmediateTest32("andi", first, second, result);
    }
}
