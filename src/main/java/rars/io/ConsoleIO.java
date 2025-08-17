package rars.io;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.ExitingException;
import rars.settings.BoolSettings;
import rars.util.Lazy;

import java.io.*;

public final class ConsoleIO implements AbstractIO {
    private final @NotNull Lazy<@NotNull BufferedReader> inputReader;
    private final @NotNull Lazy<@NotNull BufferedWriter> outputWriter, errorWriter;
    private final @NotNull InputStream stdin;
    private final @NotNull FileHandler fileHandler;

    public ConsoleIO(
        final @NotNull InputStream stdin,
        final @NotNull OutputStream stdout,
        final @NotNull OutputStream stderr,
        final @NotNull BoolSettings boolSettings
    ) {
        this.stdin = stdin;
        this.fileHandler = new FileHandler(SYSCALL_MAXFILES - 3, boolSettings);
        this.outputWriter = Lazy.of(() ->
            new BufferedWriter(new OutputStreamWriter(stdout))
        );
        this.errorWriter = Lazy.of(() ->
            new BufferedWriter(new OutputStreamWriter(stderr))
        );
        this.inputReader = Lazy.of(() ->
            new BufferedReader(new InputStreamReader(stdin))
        );
    }

    @Override
    public @NotNull String read(
        @NotNull final String initialValue,
        @NotNull final String prompt,
        final int maxLength
    ) {
        try {
            final var readLine = this.inputReader.get().readLine();
            return readLine == null ? "" : readLine;
        } catch (final IOException e) {
            return "";
        }
    }

    @Override
    public void printString(final @NotNull String message) {
        try {
            this.outputWriter.get().write(message);
            this.outputWriter.get().flush();
        } catch (final IOException ignored) {
        }
    }

    @Override
    public int openFile(final @NotNull String filename, final int flags) {
        final var fd = this.fileHandler.openFile(filename, flags);
        if (fd == -1) {
            return -1;
        } else {
            return fd + 3;
        }
    }

    @Override
    public void closeFile(final int fd) {
        this.fileHandler.closeFile(fd - 3);
    }

    @Override
    public int writeToFile(final int fd, final byte[] myBuffer, final int lengthRequested) {
        switch (fd) {
            case STDOUT -> {
                try {
                    this.outputWriter.get().write(new String(myBuffer));
                    this.outputWriter.get().flush();
                    return myBuffer.length;
                } catch (final IOException e) {
                    return -1;
                }
            }
            case STDERR -> {
                try {
                    this.errorWriter.get().write(new String(myBuffer));
                    this.errorWriter.get().flush();
                    return myBuffer.length;
                } catch (final IOException e) {
                    return -1;
                }
            }
            default -> {
                return this.fileHandler.writeToFile(fd - 3, myBuffer, lengthRequested);
            }
        }
    }

    @Override
    public int seek(final int fd, final int offset, final int base) {
        if (fd <= STDERR || fd >= SYSCALL_MAXFILES) {
            return -1;
        }
        return this.fileHandler.seek(fd - 3, offset, base);
    }

    @Override
    public int readFromFile(final int fd, final byte[] myBuffer, final int lengthRequested) {
        if (fd == STDIN) {
            try {
                return this.stdin.read(myBuffer, 0, lengthRequested);
            } catch (final IOException e) {
                return -1;
            }
        }
        return this.fileHandler.readFromFile(fd - 3, myBuffer, lengthRequested);
    }

    @Override
    public void flush() {

    }

    @Override
    public void showDisplay(
        int baseAddress,
        int width,
        int height,
        @NotNull ProgramStatement stmt
    ) throws ExitingException {
        throw new ExitingException(
            stmt,
            "the BitmapDisplay syscall (syscall 61) is not supported in the console"
        );
    }
}
