package compressedInstructions;

import org.junit.jupiter.api.Test;

public class CEBREAK extends AbstractCompressedInstructionTest {
    @Test
    void validCall() {
        assertCompiles("c.ebreak");
    }

    @Test
    void invalidCall_Operand() {
        assertFails("c.ebreak, 3");
    }
}
