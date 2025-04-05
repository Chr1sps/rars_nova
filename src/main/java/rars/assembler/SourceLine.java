package rars.assembler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.RISCVProgram;

import java.io.File;

/**
 * Handy class to represent, for a given line of source code, the code
 * itself, the program containing it, and its line number within that program.
 * This is used to separately keep track of the original file/position of
 * a given line of code. When .include is used, it will migrate to a different
 * line and possibly different program but the migration should not be visible
 * to the user.
 */
public record SourceLine(
    @NotNull String source,
    @NotNull RISCVProgram program,
    @Nullable File file,
    int lineNumber
) {
    /**
     * SourceLine constructor
     *
     * @param source
     *     The source code itself
     * @param program
     *     The program (object representing source file) containing
     *     that line
     * @param lineNumber
     *     The line number within that program where source appears.
     */
    public SourceLine(@NotNull final String source, final @NotNull RISCVProgram program, final int lineNumber) {
        this(source, program, program.getFile(), lineNumber);
    }
}
