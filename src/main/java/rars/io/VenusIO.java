package rars.io;

import org.jetbrains.annotations.NotNull;
import rars.settings.BoolSetting;
import rars.settings.BoolSettings;
import rars.venus.MessagesPane;

import java.nio.charset.StandardCharsets;

public final class VenusIO implements AbstractIO {

    private final @NotNull MessagesPane messagesPane;

    private final @NotNull BoolSettings boolSettings;
    private final @NotNull FileHandler fileHandler;
    private @NotNull String buffer;
    private long lastTime;

    public VenusIO(
        final @NotNull MessagesPane messagesPane,
        final @NotNull BoolSettings boolSettings
    ) {
        super();
        this.messagesPane = messagesPane;
        this.boolSettings = boolSettings;
        this.fileHandler = new FileHandler(SYSCALL_MAXFILES - 3, this.boolSettings);
        this.buffer = "";
        this.lastTime = 0;
    }

    @Override
    public @NotNull String readImpl(
        @NotNull final String initialValue,
        @NotNull final String prompt,
        final int maxLength
    ) {
        final var isPopup = this.boolSettings.getSetting(BoolSetting.POPUP_SYSCALL_INPUT);
        return (isPopup) ? messagesPane.getInputStringFromDialog(prompt) : messagesPane.getInputString(maxLength);
    }

    @Override
    public void printString(final @NotNull String message) {
        this.printToGui(message);
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

    public void resetFiles() {
        this.fileHandler.closeAll();
    }

    @Override
    public int writeToFile(final int fd, final byte[] myBuffer, final int lengthRequested) {
        if (fd == STDOUT || fd == STDERR) {
            final var string = new String(myBuffer, StandardCharsets.UTF_8); // decode the bytes using UTF-8 
            this.printToGui(string);
            return myBuffer.length;
        } else {
            return this.fileHandler.writeToFile(fd - 3, myBuffer, lengthRequested);
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
            final var input = this.messagesPane.getInputString(lengthRequested);
            final var bytesRead = input.getBytes();

            for (int i = 0; i < myBuffer.length; i++) {
                myBuffer[i] = (i < bytesRead.length) ? bytesRead[i] : 0;
            }
            return Math.min(myBuffer.length, bytesRead.length);
        }
        return this.fileHandler.readFromFile(fd - 3, myBuffer, lengthRequested);
    }

    @Override
    public void flush() {
        this.messagesPane.postRunMessage(this.buffer);
        this.buffer = "";
        this.lastTime = System.currentTimeMillis() + 100;
    }

    private void printToGui(final @NotNull String message) {
        final long time = System.currentTimeMillis();
        if (time > this.lastTime) {
            this.messagesPane.postRunMessage(this.buffer + message);
            this.buffer = "";
            this.lastTime = time + 100;
        } else {
            this.buffer += message;
        }
    }
}
