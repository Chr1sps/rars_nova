package rars;

import org.jetbrains.annotations.NotNull;
import rars.assembler.Token;

import java.util.ArrayList;
import java.util.List;
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
 * Maintains list of generated error messages, regardless of source (tokenizing,
 * parsing,
 * assembly, execution).
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public final class ErrorList {
    public static final @NotNull String ERROR_MESSAGE_PREFIX = "Error";
    public static final @NotNull String WARNING_MESSAGE_PREFIX = "Warning";
    public static final @NotNull String FILENAME_PREFIX = " in ";
    public static final @NotNull String LINE_PREFIX = " line ";
    public static final @NotNull String POSITION_PREFIX = " column ";
    public static final @NotNull String MESSAGE_SEPARATOR = ": ";
    private final @NotNull ArrayList<@NotNull ErrorMessage> messages;
    private int errorCount;
    private int warningCount;

    /**
     * Constructor for ErrorList
     */
    public ErrorList() {
        this.messages = new ArrayList<>();
        this.errorCount = 0;
        this.warningCount = 0;
    }

    /**
     * Get limit on number of error messages to be generated
     * by one assemble operation.
     *
     * @return error limit.
     */
    @SuppressWarnings("SameReturnValue")
    public static int getErrorLimit() {
        return Globals.maximumErrorMessages;
    }

    /**
     * Get ArrayList of error messages.
     *
     * @return ArrayList of ErrorMessage objects
     */
    public @NotNull List<ErrorMessage> getErrorMessages() {
        return this.messages;
    }

    /**
     * Determine whether error has occurred or not.
     *
     * @return {@code true} if an error has occurred (does not include warnings),
     * {@code false} otherwise.
     */
    public boolean errorsOccurred() {
        return (this.errorCount != 0);
    }

    /**
     * Determine whether warning has occurred or not.
     *
     * @return {@code true} if an warning has occurred, <code>false</code> otherwise.
     */
    public boolean warningsOccurred() {
        return (this.warningCount != 0);
    }

    /**
     * Add new error message to end of list.
     *
     * @param mess
     *     ErrorMessage object to be added to end of error list.
     */
    public void add(final @NotNull ErrorMessage mess) {
        if (this.errorCount > ErrorList.getErrorLimit()) {
            return;
        }
        if (this.errorCount == ErrorList.getErrorLimit()) {
            this.messages.add(ErrorMessage.error(
                null,
                mess.lineNumber,
                mess.position,
                "Error Limit of %d exceeded."
                    .formatted(ErrorList.getErrorLimit())
            ));
            this.errorCount++; // subsequent errors will not be added; see if statement above
            return;
        }
        this.messages.add(this.messages.size(), mess);
        if (mess.isWarning) {
            this.warningCount++;
        } else {
            this.errorCount++;
        }
    }

    public void addTokenError(final @NotNull Token token, final @NotNull String message) {
        final var errorMessage = ErrorMessage.error(
            token.getSourceProgram(),
            token.getSourceLine(),
            token.getStartPos(),
            message
        );
        this.add(errorMessage);
    }

    public void addWarning(final @NotNull Token token, final @NotNull String message) {
        final var errorMessage = ErrorMessage.warning(
            token.getSourceProgram(),
            token.getSourceLine(),
            token.getStartPos(),
            message
        );
        this.add(errorMessage);
    }

    /**
     * Check to see if error limit has been exceeded.
     *
     * @return True if error limit exceeded, false otherwise.
     */
    public boolean errorLimitExceeded() {
        return this.errorCount > ErrorList.getErrorLimit();
    }

    /**
     * Produce error report.
     *
     * @return String containing report.
     */
    public @NotNull String generateErrorReport() {
        return this.generateReport(false);
    }

    /**
     * Produce warning report.
     *
     * @return String containing report.
     */
    public @NotNull String generateWarningReport() {
        return this.generateReport(true);
    }

    /**
     * Produce report containing both warnings and errors, warnings first.
     *
     * @return String containing report.
     */
    public @NotNull String generateErrorAndWarningReport() {
        return this.generateWarningReport() + this.generateErrorReport();
    }

    // Produces either error or warning report.
    private @NotNull String generateReport(final boolean isWarning) {
        final StringBuilder report = new StringBuilder();
        for (final ErrorMessage m : this.messages) {
            if ((isWarning && m.isWarning) || (!isWarning && !m.isWarning)) {
                report.append(m.generateReport());
            }
        }
        return report.toString();
    }
} // ErrorList
