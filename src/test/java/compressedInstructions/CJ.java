package compressedInstructions;

import org.junit.jupiter.api.Test;

public class CJ extends AbstractCompressedInstructionTest {
    @Test
    void validCall() {
        assertCompiles("""
            some_label:
            c.j, some_label
            """);
    }

    @Test
    void invalid_noArg() {
        assertFails("c.j");
    }

    @Test
    void invalid_wrongArgType() {
        assertFails("c.j, 3");
    }

    @Test
    void invalid_twoArgs() {
        assertFails("""
            some:
            label:
            c.j, some, label""");
    }
}
