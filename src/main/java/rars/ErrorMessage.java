package rars;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.assembler.SourceLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * Represents occurrance of an error detected during tokenizing, assembly or
 * simulation.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public final class ErrorMessage {
    private final boolean isWarning; // allow for warnings too (added Nov 2006)
    private final @NotNull String filename; // name of source file (added Oct 2006)
    private final int line; // line in source code where error detected
    private final int position; // position in source line where error detected
    private final @NotNull String message;
    private final @NotNull String macroExpansionHistory;

    /**
     * Constructor for ErrorMessage. Assumes line number is calculated after any
     * .include files expanded, and
     * if there were, it will adjust filename and line number so message reflects
     * original file and line number.
     *
     * @param isWarning
     *     set to WARNING if message is a warning not error, else
     *     set to ERROR or omit.
     * @param sourceProgram
     *     RISCVprogram object of source file in which this error
     *     appears.
     * @param line
     *     Line number in source program being processed when error
     *     occurred.
     * @param position
     *     Position within line being processed when error
     *     occurred. Normally is starting
     *     position of source token.
     * @param message
     *     String containing appropriate error message.
     */
    public ErrorMessage(
        final boolean isWarning,
        final @Nullable RISCVProgram sourceProgram,
        final int line,
        final int position,
        final @NotNull String message
    ) {
        this.isWarning = isWarning;
        if (sourceProgram == null) {
            this.filename = "";
            this.line = line;
        } else {
            if (sourceProgram.getSourceLineList() == null) {
                this.filename = sourceProgram.getFilename();
                this.line = line;
            } else {
                final SourceLine sourceLine = sourceProgram.getSourceLineList()
                    .get(line - 1);
                this.filename = sourceLine.filename();
                this.line = sourceLine.lineNumber();
            }
        }
        this.position = position;
        this.message = message;
        this.macroExpansionHistory = ErrorMessage.getExpansionHistory(sourceProgram);
    }

    /**
     * Constructor for ErrorMessage, to be used for runtime exceptions.
     *
     * @param statement
     *     The ProgramStatement object for the instruction causing the
     *     runtime error
     * @param message
     *     String containing appropriate error message.
     */
    // Added January 2013
    public ErrorMessage(
        final @NotNull ProgramStatement statement,
        final @NotNull String message
    ) {
        this.isWarning = false;
        this.filename = (statement.getSourceProgram() == null)
            ? ""
            : statement.getSourceProgram().getFilename();
        this.position = 0;
        this.message = message;
        // Somewhere along the way we lose the macro history, but can
        // normally recreate it here. The line number for macro use (in the
        // expansion) comes with the ProgramStatement.getSourceLine().
        // The line number for the macro definition comes embedded in
        // the source code from ProgramStatement.getSource(), which is
        // displayed in the Text Segment display. It would previously
        // have had the macro definition line prepended in brackets,
        // e.g. "<13> syscall # finished". So I'll extract that
        // bracketed number here and include it in the error message.
        // Looks bass-ackwards, but to get the line numbers to display correctly
        // for runtime error occurring in macro expansion (expansion->definition), need
        // to assign to the opposite variables.
        final var defineLine = ErrorMessage.parseMacroHistory(statement.getSource());
        if (defineLine.isEmpty()) {
            this.line = statement.getSourceLine();
            this.macroExpansionHistory = "";
        } else {
            this.line = defineLine.getFirst();
            this.macroExpansionHistory = "" + statement.getSourceLine();
        }
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

    private static @NotNull List<Integer> parseMacroHistory(final String string) {
        final Pattern pattern = Pattern.compile("<\\d+>");
        final Matcher matcher = pattern.matcher(string);
        String verify = string.trim();
        final var macroHistory = new ArrayList<Integer>();
        while (matcher.find()) {
            final String match = matcher.group();
            if (verify.indexOf(match) == 0) {
                try {
                    final int line = Integer.parseInt(match.substring(1, match.length() - 1));
                    macroHistory.add(line);
                } catch (final NumberFormatException e) {
                    break;
                }
                verify = verify.substring(match.length()).trim();
            } else {
                break;
            }
        }
        return macroHistory;
    }

    // Added by Mohammad Sekavat Dec 2012
    private static @NotNull String getExpansionHistory(final RISCVProgram sourceProgram) {
        if (sourceProgram == null || sourceProgram.getLocalMacroPool() == null) {
            return "";
        }
        return sourceProgram.getLocalMacroPool().getExpansionHistory();
    }

    /**
     * Produce name of file containing error.
     *
     * @return Returns String containing name of source file containing the error.
     */
    // Added October 2006
    public @NotNull String getFilename() {
        return this.filename;
    }

    /**
     * Produce line number of error.
     *
     * @return Returns line number in source program where error occurred.
     */
    public int getLine() {
        return this.line;
    }

    /**
     * Produce position within erroneous line.
     *
     * @return Returns position within line of source program where error occurred.
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * Produce error message.
     *
     * @return Returns String containing textual error message.
     */
    public @NotNull String getMessage() {
        return this.message;
    }

    /**
     * Determine whether this message represents error or warning.
     *
     * @return Returns true if this message reflects warning, false if error.
     */
    // Method added 28 Nov 2006
    public boolean isWarning() {
        return this.isWarning;
    }

    public @NotNull String generateReport() {
        final var builder = new StringBuilder();
        builder.append((this.isWarning ? "Warning" : "Error"))
            .append(" in ");
        if (!this.getFilename().isEmpty()) {
            builder.append(new File(this.getFilename()).getPath());
        }
        if (this.getLine() > 0) {
            builder.append(" line ")
                .append(this.getMacroExpansionHistory())
                .append(this.getLine());
        }
        if (this.getPosition() > 0) {
            builder.append(" column ")
                .append(this.getPosition());
        }
        builder.append(": ")
            .append(this.getMessage())
            .append("\n");
        return builder.toString();
    }

    /**
     * Returns string describing macro expansion. Empty string if none.
     *
     * @return string describing macro expansion
     */
    // Method added by Mohammad Sekavat Dec 2012
    @Contract(pure = true)
    public @NotNull String getMacroExpansionHistory() {
        if (this.macroExpansionHistory.isEmpty()) {
            return "";
        }
        return this.macroExpansionHistory + "->";
    }

} // ErrorMessage
