package cli;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import rars.api.ProgramOptions;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliTest {
    private static void doTest(
        final @NotNull String @NotNull [] args,
        final @NotNull Consumer<@NotNull ProgramOptions> assertionsFunc
    ) {
        System.out.printf("Args: %s%n", Arrays.toString(args));
        final var programArgs = new ProgramOptions();
        new CommandLine(programArgs).execute(args);
        System.out.println("Result: " + programArgs);
        assertionsFunc.accept(programArgs);
    }

    @Test
    void testFiles() {
        doTest(
            new String[]{"--files", "file1", "file2"},
            programArgs -> {
                final var expected = List.of(new File("file1"), new File("file2"));
                assertEquals(expected, programArgs.files);
            }
        );
    }
}
