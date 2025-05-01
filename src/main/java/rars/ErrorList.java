package rars;

import org.jetbrains.annotations.NotNull;
import rars.assembler.Token;

import java.util.ArrayList;
import java.util.List;

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
    public static final int ERROR_LIMIT = 200;
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
     * Get ArrayList of error messages.
     *
     * @return ArrayList of ErrorMessage objects
     */
    public @NotNull List<@NotNull ErrorMessage> getErrorMessages() {
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
     * @return {@code true} if an warning has occurred, {@code false} otherwise.
     */
    public boolean warningsOccurred() {
        return (this.warningCount != 0);
    }

    /**
     * Add new error message to end of list.
     *
     * @param mess ErrorMessage object to be added to end of error list.
     */
    public void add(final @NotNull ErrorMessage mess) {
        if (this.errorCount > ERROR_LIMIT) {
            return;
        }
        if (this.errorCount == ERROR_LIMIT) {
            this.messages.add(ErrorMessage.error(
                    null,
                    mess.lineNumber,
                    mess.position,
                    "Error Limit of %d exceeded."
                            .formatted(ERROR_LIMIT)
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
        return this.errorCount > ERROR_LIMIT;
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

    /**
     * Produces either error or warning report.
     */
    private @NotNull String generateReport(final boolean isWarning) {
        final StringBuilder report = new StringBuilder();
        for (final ErrorMessage m : this.messages) {
            if ((isWarning && m.isWarning) || (!isWarning && !m.isWarning)) {
                report.append(m.generateReport());
            }
        }
        return report.toString();
    }

    @Override
    public String toString() {
        return "ErrorList{" +
                "messages=" + messages +
                '}';
    }
}
