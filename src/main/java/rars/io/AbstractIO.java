package rars.io;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.ExitingException;
import rars.riscv.Syscall;

public interface AbstractIO {
    int SYSCALL_MAXFILES = 32;

    int STDIN = 0;
    int STDOUT = 1;
    int STDERR = 2;

    int O_RDONLY = 0x00000000;
    int O_WRONLY = 0x00000001;
    int O_RDWR = 0x00000002;
    int O_APPEND = 0x00000008;
    int O_CREAT = 0x00000200; // 512
    int O_TRUNC = 0x00000400; // 1024
    int O_EXCL = 0x00000800; // 2048

    int SEEK_SET = 0;
    int SEEK_CUR = 1;
    int SEEK_END = 2;

    /**
     * Implements syscall to read a double value.
     * Client is responsible for catching NumberFormatException.
     *
     * @return double value corresponding to user input
     */
    default double readDouble() {
        final var input = this.read(
            "0",
            "Enter a Double value (syscall %d)".formatted(
                Syscall.ReadDouble.serviceNumber
            ), -1
        );
        return Double.parseDouble(input.trim());
    }

    /**
     * Implements syscall to read a float value.
     * Client is responsible for catching NumberFormatException.
     *
     * @return float value corresponding to user input
     * Feb 14 2005 Ken Vollmar
     */
    default float readFloat() {
        final var input = this.read(
            "0", "Enter a Float value (syscall %d)".formatted(
                Syscall.ReadFloat.serviceNumber
            ), -1
        );
        return Float.parseFloat(input.trim());
    }

    /**
     * Implements syscall to read an integer value.
     * Client is responsible for catching NumberFormatException.
     *
     * @return int value corresponding to user input
     */
    default int readInt() {
        final var input = this.read(
            "0", "Enter an Integer value (syscall %d)".formatted(
                Syscall.ReadInt.serviceNumber
            ), -1
        );
        return Integer.parseInt(input.trim());
    }

    /**
     * Implements syscall to read a string.
     *
     * @param maxLength
     *     the maximum string length
     * @return the entered string, truncated to maximum length if necessary
     */
    default @NotNull String readString(final int maxLength) {
        var input = this.read(
            "",
            "Enter a string of maximum length %d (syscall %d)".formatted(
                maxLength,
                Syscall.ReadString.serviceNumber
            ),
            maxLength
        );
        if (input.endsWith("\n")) {
            input = input.substring(0, input.length() - 1);
        }
        if (input.length() > maxLength) {
            return (maxLength <= 0) ? "" : input.substring(0, maxLength);
        } else {
            return input;
        }
    }

    /**
     * Implements syscall to read a char value.
     *
     * @return char value corresponding to user input
     */
    default char readChar() {
        final var input = this.read(
            "0",
            "Enter a character value (syscall %d)".formatted(
                Syscall.ReadChar.serviceNumber
            ), 1
        );
        // The whole try-catch is not really necessary in this case since I'm
        // just propagating the runtime exception (the default behavior), but
        // I want to make it explicit. The client needs to catch it.
        // first character input

        return input.charAt(0);
    }

    String read(
        final @NotNull String initialValue,
        final @NotNull String prompt,
        final int maxLength
    );

    /**
     * Implements syscall to print a string.
     */
    void printString(final @NotNull String message);

    /**
     * Open a file for either reading or writing. Note that read/write flag is NOT
     * IMPLEMENTED. Also note that file permission modes are also NOT IMPLEMENTED.
     *
     * @param filename
     *     string containing file
     * @param flags
     *     0 for read, 1 for write
     * @return file descriptor in the range 0 to SYSCALL_MAXFILES-1, or -1 if error
     * @author Ken Vollmar
     */
    int openFile(final String filename, final int flags);

    /**
     * Close the file with specified file descriptor
     *
     * @param fd
     *     the file descriptor of an open file
     */
    void closeFile(final int fd);

    /**
     * Write bytes to file.
     *
     * @param fd
     *     file descriptor
     * @param myBuffer
     *     byte array containing characters to write
     * @param lengthRequested
     *     number of bytes to write
     * @return number of bytes written, or -1 on error
     */
    int writeToFile(final int fd, final byte[] myBuffer, final int lengthRequested);

    /**
     * Read bytes from file.
     *
     * @param fd
     *     file descriptor
     * @param offset
     *     where in the file to seek to
     * @param base
     *     the point to reference 0 for start of file, 1 for current
     *     position, 2 for end of the file
     * @return -1 on error
     */
    int seek(final int fd, int offset, final int base);

    /**
     * Read bytes from file.
     *
     * @param fd
     *     file descriptor
     * @param myBuffer
     *     byte array to contain bytes read
     * @param lengthRequested
     *     number of bytes to read
     * @return number of bytes read, 0 on EOF, or -1 on error
     */
    int readFromFile(final int fd, final byte[] myBuffer, final int lengthRequested);

    /**
     * Given a base address and display dimensions, displays memory context in
     * a new window. When a window is present:
     * <ul>
     *     <li>
     *         Calling the function with the same arguments doesn't affect the
     *         window.
     *     </li>
     *     <li>
     *         Calling the function with the same dimensions, but a different
     *         base address updates the existing window with new contents.
     *     </li>
     *     <li>
     *         Calling the function with different window dimensions disposes
     *         of the current window and replaces it with a new one. To avoid
     *         creating a window on each resize, the implementation may limit
     *         the rate of such window disposals according to its own needs.
     *     </li>
     * </ul>
     * 
     * In the case of not supporting the display (i.e., in console mode), the
     * implementation may throw an according notice to the simulator to notify
     * of such a case.
     * 
     * @throws ExitingException in case the display is unsupported
     */
    void showDisplay(final int baseAddress, final int width, final int height, @NotNull ProgramStatement stmt) throws ExitingException;

    void flush();
}
