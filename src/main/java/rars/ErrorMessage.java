package rars;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
/*
Copyright (c) 2003-2012,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Represents occurrence of an error detected during tokenizing, assembly or
 * simulation.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public final class ErrorMessage {
    public final boolean isWarning; // allow for warnings too (added Nov 2006)
    public final @Nullable File file; // name of source file (added Oct 2006)
    public final int lineNumber; // line in source code where error detected
    public final int position; // position in source line where error detected
    public final @NotNull String message;
    private final @NotNull String macroExpansionHistory;

    /**
     * Constructor for ErrorMessage. Assumes line number is calculated after any
     * .include files expanded, and
     * if there were, it will adjust file and line number so message reflects
     * original file and line number.
     *
     * @param isWarning
     *     set to WARNING if message is a warning not error, else
     *     set to ERROR or omit.
     * @param sourceProgram
     *     RISCVprogram object of source file in which this error
     *     appears.
     * @param lineNumber
     *     Line number in source program being processed when error
     *     occurred.
     * @param position
     *     Position within line being processed when error
     *     occurred. Normally is starting
     *     position of source token.
     * @param message
     *     String containing appropriate error message.
     */
    private ErrorMessage(
        final boolean isWarning,
        final @Nullable RISCVProgram sourceProgram,
        final int lineNumber,
        final int position,
        final @NotNull String message
    ) {
        this.isWarning = isWarning;
        this.file = (sourceProgram == null) ? null : (
            (sourceProgram.getSourceLineList() == null) ? sourceProgram.getFile() : (
                sourceProgram.getSourceLineList().get(lineNumber - 1).file()
            )
        );
        this.lineNumber = (sourceProgram == null) ? -1 : (
            (sourceProgram.getSourceLineList() == null) ? lineNumber : (
                sourceProgram.getSourceLineList().get(lineNumber - 1).lineNumber()
            )
        );
        this.position = position;
        this.message = message;
        this.macroExpansionHistory = ErrorMessage.getExpansionHistory(sourceProgram);
    }

    public static @NotNull ErrorMessage error(
        final @Nullable RISCVProgram sourceProgram,
        final int line,
        final int position,
        final @NotNull String message
    ) {
        return new ErrorMessage(false, sourceProgram, line, position, message);
    }

    public static @NotNull ErrorMessage warning(
        final @Nullable RISCVProgram sourceProgram,
        final int line,
        final int position,
        final @NotNull String message
    ) {
        return new ErrorMessage(true, sourceProgram, line, position, message);
    }

    private static @NotNull String getExpansionHistory(final RISCVProgram sourceProgram) {
        // Added by Mohammad Sekavat Dec 2012
        if (sourceProgram == null || sourceProgram.getLocalMacroPool() == null) {
            return "";
        }
        return sourceProgram.getLocalMacroPool().getExpansionHistory();
    }

    public @NotNull String generateReport() {
        final var builder = new StringBuilder();
        builder.append((this.isWarning ? "Warning" : "Error"))
            .append(" in ");
        if (this.file != null) {
            builder.append(this.file.getPath()).append(" ");
        }
        builder.append("line ")
            .append(this.getMacroExpansionHistory())
            .append(this.lineNumber);
        builder.append(" column ")
            .append(this.position);
        builder.append(":\n")
            .append(this.message)
            .append("\n");
        return builder.toString();
    }

    /**
     * Returns string describing macro expansion. Empty string if none.
     *
     * @return string describing macro expansion
     */
    @Contract(pure = true)
    @NotNull
    private String getMacroExpansionHistory() {
        // Method added by Mohammad Sekavat Dec 2012
        if (this.macroExpansionHistory.isEmpty()) {
            return "";
        }
        return this.macroExpansionHistory + "->";
    }

} // ErrorMessage
