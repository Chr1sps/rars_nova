package rars.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.settings.BoolSetting;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import static rars.Globals.BOOL_SETTINGS;


/*
Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

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
 * Provides standard i/o services needed to simulate the RISCV syscall
 * routines. These methods will detect whether the simulator is being
 * run from the command line or through the GUI, then do I/O to
 * System.in and System.out in the former situation, and interact with
 * the GUI in the latter.
 *
 * @author Pete Sanderson and Ken Vollmar
 * @version August 2003-2005
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class SystemIO {
    /** Buffer size for syscalls for file I/O */
    public static final int SYSCALL_BUFSIZE = 128;
    /** Maximum number of files that can be open */
    private static final int SYSCALL_MAXFILES = 32;
    private static final int O_RDONLY = 0x00000000;
    private static final int O_WRONLY = 0x00000001;
    private static final int O_RDWR = 0x00000002;
    private static final int O_APPEND = 0x00000008;
    private static final int O_CREAT = 0x00000200; // 512
    private static final int O_TRUNC = 0x00000400; // 1024
    private static final int O_EXCL = 0x00000800; // 2048
    private static final int SEEK_SET = 0;
    private static final int SEEK_CUR = 1;
    private static final int SEEK_END = 2;

    // standard I/O channels

    private static final int STDIN = 0;
    private static final int STDOUT = 1;
    private static final int STDERR = 2;

    // The GUI doesn't handle lots of small messages well so I added this hacky way
    // of buffering
    // Currently it checks to flush every instruction run

    private static String buffer = "";
    private static long lasttime = 0;

    private SystemIO() {
    }

    /**
     * Implements syscall to read an integer value.
     * Client is responsible for catching NumberFormatException.
     *
     * @param serviceNumber
     *     the number assigned to Read Int syscall (default 5)
     * @return int value corresponding to user input
     */
    public static int readInteger(final int serviceNumber) {
        final String input = SystemIO.readStringInternal(
            "0", "Enter an integer value (syscall " + serviceNumber +
                ")", -1
        );
        // Client is responsible for catching NumberFormatException
        return Integer.parseInt(input.trim());
    }

    private static String readStringInternal(final String init, final String prompt, final int maxlength) {
        String input = init;
        if (Globals.GUI == null) {
            try {
                input = FileIOData.data.inputReader.get().readLine();
                if (input == null) {
                    input = "";
                }
            } catch (final IOException ignored) {
            }
        } else {
            if (BOOL_SETTINGS.getSetting(BoolSetting.POPUP_SYSCALL_INPUT)) {
                input = Globals.GUI.messagesPane.getInputStringFromDialog(prompt);
            } else {
                input = Globals.GUI.messagesPane.getInputString(maxlength);
            }
        }
        return input;
    }

    /**
     * Implements syscall to read a float value.
     * Client is responsible for catching NumberFormatException.
     *
     * @param serviceNumber
     *     the number assigned to Read Float syscall (default 6)
     * @return float value corresponding to user input
     * Feb 14 2005 Ken Vollmar
     */
    public static float readFloat(final int serviceNumber) {
        final String input = SystemIO.readStringInternal(
            "0", "Enter a float value (syscall " + serviceNumber + ")",
            -1
        );
        return Float.parseFloat(input.trim());
    }

    /**
     * Implements syscall to read a double value.
     * Client is responsible for catching NumberFormatException.
     *
     * @param serviceNumber
     *     the number assigned to Read Duoble syscall (default 7)
     * @return double value corresponding to user input
     * Feb 14 2005 Ken Vollmar
     */
    public static double readDouble(final int serviceNumber) {
        final String input = SystemIO.readStringInternal(
            "0", "Enter a Double value (syscall " + serviceNumber + ")"
            , -1
        );
        return Double.parseDouble(input.trim());
    }

    /**
     * Implements syscall having 4 in $v0, to print a string.
     *
     * @param string
     *     a {@link java.lang.String} object
     */
    public static void printString(final String string) {
        if (Globals.GUI == null) {
            try {
                FileIOData.data.outputWriter.get().write(string);
                FileIOData.data.outputWriter.get().flush();
            } catch (final IOException ignored) {
            }
        } else {
            SystemIO.print2Gui(string);
        }
    }

    /**
     * Implements syscall to read a string.
     *
     * @param serviceNumber
     *     the number assigned to Read String syscall (default 8)
     * @param maxLength
     *     the maximum string length
     * @return the entered string, truncated to maximum length if necessary
     */
    public static @NotNull String readString(final int serviceNumber, final int maxLength) {
        String input = SystemIO.readStringInternal(
            "", "Enter a string of maximum length " + maxLength
                + " (syscall " + serviceNumber + ")", maxLength
        );
        if (input.endsWith("\n")) {
            input = input.substring(0, input.length() - 1);
        }
        if (input.length() > maxLength) {
            // Modified DPS 13-July-2011. Originally: return input.substring(0, maxLength);
            return (maxLength <= 0) ? "" : input.substring(0, maxLength);
        } else {
            return input;
        }
    }

    /**
     * Implements syscall having 12 in $v0, to read a char value.
     *
     * @param serviceNumber
     *     the number assigned to Read Char syscall (default 12)
     * @return int value with lowest byte corresponding to user input
     */
    public static char readChar(final int serviceNumber) {

        final String input = SystemIO.readStringInternal(
            "0", "Enter a character value (syscall " + serviceNumber +
                ")", 1
        );
        // The whole try-catch is not really necessary in this case since I'm
        // just propagating the runtime exception (the default behavior), but
        // I want to make it explicit. The client needs to catch it.
        // first character input
        final var returnValue = input.charAt(0);

        return returnValue;

    }

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
    public static int writeToFile(final int fd, final byte[] myBuffer, final int lengthRequested) {
        // DPS 8-Jan-2013
        // Write to STDOUT or STDERR file descriptor while using IDE - write to
        // Messages pane.
        if ((fd == SystemIO.STDOUT || fd == SystemIO.STDERR)) {
            if (Globals.GUI != null) {
                final String data = new String(myBuffer, StandardCharsets.UTF_8); // decode the bytes using UTF-8 
                // charset
                SystemIO.print2Gui(data);
                return myBuffer.length; // data.length would not count multi-byte characters
            }
        }
        // When running in command mode, code below works for either regular file or STDOUT/STDERR

        if (FileIOData.fdNotInUse(fd, 1)) // Check the existence of the "write" fd
        {
            return -1;
        }
        // retrieve FileOutputStream from storage
        final OutputStream outputStream = (OutputStream) FileIOData.getStreamInUse(fd);
        try {
            // Oct. 9 2005 Ken Vollmar
            // Observation: made a call to outputStream.write(myBuffer, 0, lengthRequested)
            // with myBuffer containing 6(ten) 32-bit-words <---> 24(ten) bytes, where the
            // words are MIPS integers with values such that many of the bytes are ZEROES.
            // The effect is apparently that the write stops after encountering a
            // zero-valued
            // byte. (The method write does not return a value and so this can't be verified
            // by the return value.)
            // Writes up to lengthRequested bytes of data to this output stream from an
            // array of bytes.
            // outputStream.write(myBuffer, 0, lengthRequested); // write is a void method
            // -- no verification value returned

            // Oct. 9 2005 Ken Vollmar Force the write statement to write exactly
            // the number of bytes requested, even though those bytes include many ZERO
            // values.
            for (int ii = 0; ii < lengthRequested; ii++) {
                outputStream.write(myBuffer[ii]);
            }
            outputStream.flush();// DPS 7-Jan-2013
        } catch (final IOException e) {
            return -1;
        } catch (final IndexOutOfBoundsException e) {
            return -1;
        }

        return lengthRequested;

    } // end writeToFile

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
    public static int readFromFile(final int fd, final byte[] myBuffer, final int lengthRequested) {
        // DPS 8-Jan-2013
        // Read from STDIN file descriptor while using IDE - get input from Messages
        // pane.
        if (fd == SystemIO.STDIN) {
            if (Globals.GUI != null) {
                final String input = Globals.GUI.messagesPane.getInputString(lengthRequested);
                final byte[] bytesRead = input.getBytes();

                for (int i = 0; i < myBuffer.length; i++) {
                    myBuffer[i] = (i < bytesRead.length) ? bytesRead[i] : 0;
                }
                return Math.min(myBuffer.length, bytesRead.length);
            }
        }
        // When running in command mode, code below works for either regular file or STDIN
        if (FileIOData.fdNotInUse(fd, 0)) // Check the existence of the "read" fd
        {
            return -1;
        }
        // retrieve FileInputStream from storage
        final InputStream InputStream = (InputStream) FileIOData.getStreamInUse(fd);
        int retValue;
        try {
            // Reads up to lengthRequested bytes of data from this Input stream into an
            // array of bytes.
            retValue = InputStream.read(myBuffer, 0, lengthRequested);
            // This method will return -1 upon EOF, but our spec says that negative
            // value represents an error, so we return 0 for EOF. DPS 10-July-2008.
            if (retValue == -1) {
                retValue = 0;
            }
        } catch (final IOException e) {
            return -1;
        } catch (final IndexOutOfBoundsException e) {
            return -1;
        }
        return retValue;

    } // end readFromFile

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
    public static int seek(final int fd, int offset, final int base) {
        if (FileIOData.fdNotInUse(fd, 0)) // Check the existence of the "read" fd
        {
            return -1;
        }
        if (fd < 0 || fd >= SystemIO.SYSCALL_MAXFILES) {
            return -1;
        }
        final var stream = FileIOData.getStreamInUse(fd);
        if (stream == null) {
            return -1;
        }
        try {
            final FileChannel channel;
            if (stream instanceof FileInputStream) {
                channel = ((FileInputStream) stream).getChannel();
            } else if (stream instanceof FileOutputStream) {
                channel = ((FileOutputStream) stream).getChannel();
            } else {
                return -1;
            }

            switch (base) {
                case SystemIO.SEEK_CUR -> offset += (int) channel.position();
                case SystemIO.SEEK_END -> offset += (int) channel.size();
                case SystemIO.SEEK_SET -> {
                }
                default -> {
                    return -1;
                }
            }
            if (offset < 0) {
                return -1;
            }
            channel.position(offset);
            return offset;
        } catch (final IOException io) {
            return -1;
        }
    }

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
    public static int openFile(final String filename, final int flags) {
        // Internally, a "file descriptor" is an index into a table
        // of the file, flag, and the File???putStream associated with
        // that file descriptor.

        // Check internal plausibility of opening this file
        final int fdToUse = FileIOData.nowOpening(filename, flags);
        // return value is the fd
        int retValue = fdToUse;
        if (fdToUse < 0) {
            return -1;
        } // fileErrorString would have been set

        File filepath = new File(filename);
        if (!filepath.isAbsolute() && Globals.program != null) {
            if (BOOL_SETTINGS.getSetting(BoolSetting.DERIVE_CURRENT_WORKING_DIRECTORY)) {
                final var parent = Globals.program.getFile().getParentFile();
                filepath = new File(parent, filename);
            }
        }
        if (flags == SystemIO.O_RDONLY) // Open for reading only
        {
            try {
                // Set up input stream from disk file
                final FileInputStream inputStream = new FileInputStream(filepath);
                FileIOData.setStreamInUse(fdToUse, inputStream); // Save stream for later use
            } catch (final FileNotFoundException e) {
                retValue = -1;
            }
        } else if ((flags & SystemIO.O_WRONLY) != 0) // Open for writing only
        {
            // Set up output stream to disk file
            try {
                final FileOutputStream outputStream = new FileOutputStream(
                    filepath,
                    ((flags & SystemIO.O_APPEND) != 0)
                );
                FileIOData.setStreamInUse(fdToUse, outputStream); // Save stream for later use
            } catch (final FileNotFoundException e) {
                retValue = -1;
            }
        }
        return retValue; // return the "file descriptor"

    }

    /**
     * Close the file with specified file descriptor
     *
     * @param fd
     *     the file descriptor of an open file
     */
    public static void closeFile(final int fd) {
        FileIOData.close(fd);
    }

    /**
     * Reset all files -- clears out the file descriptor table.
     */
    public static void resetFiles() {
        FileIOData.resetFiles();
    }

    private static void print2Gui(final String output) {
        final long time = System.currentTimeMillis();
        if (time > SystemIO.lasttime) {
            Globals.GUI.messagesPane.postRunMessage(SystemIO.buffer + output);
            SystemIO.buffer = "";
            SystemIO.lasttime = time + 100;
        } else {
            SystemIO.buffer += output;
        }
    }

    /**
     * Flush stdout cache
     * Makes sure that messages don't get stuck in the print2Gui buffer for too
     * long.
     */
    public static void flush() {
        if (Globals.GUI != null) {
            Globals.GUI.messagesPane.postRunMessage(SystemIO.buffer);
            SystemIO.buffer = "";
            final long time = System.currentTimeMillis();
            SystemIO.lasttime = time + 100;
        } else {
            try {
                FileIOData.data.outputWriter.get().flush();
            } catch (final IOException ignored) {
            }
        }
    }

    public static Data swapData(final Data in) {
        final var previousData = FileIOData.data;
        FileIOData.data = in;
        return previousData;
    }

    public static class Data {
        public @Nullable Closeable @NotNull [] streams;
        public @NotNull Cached<@NotNull BufferedReader> inputReader;
        public @NotNull Cached<@NotNull BufferedWriter> outputWriter, errorWriter;
        private @Nullable String @NotNull [] fileNames; // The filenames in use. Null if file descriptor i is not in use.
        private int @NotNull [] fileFlags; // The flags of this file, 0=READ, 1=WRITE. Invalid if this file descriptor is

        public Data() {
            this(System.in, System.out, System.err);
        }

        public Data(final InputStream in, final OutputStream out, final OutputStream err) {
            this.fileNames = new String[SystemIO.SYSCALL_MAXFILES];
            this.fileFlags = new int[SystemIO.SYSCALL_MAXFILES];
            this.streams = new Closeable[SystemIO.SYSCALL_MAXFILES];
            this.fileNames[SystemIO.STDIN] = "STDIN";
            this.fileNames[SystemIO.STDOUT] = "STDOUT";
            this.fileNames[SystemIO.STDERR] = "STDERR";
            this.fileFlags[SystemIO.STDIN] = SystemIO.O_RDONLY;
            this.fileFlags[SystemIO.STDOUT] = SystemIO.O_WRONLY;
            this.fileFlags[SystemIO.STDERR] = SystemIO.O_WRONLY;
            this.streams[SystemIO.STDIN] = in;
            this.streams[SystemIO.STDOUT] = out;
            this.streams[SystemIO.STDERR] = err;
            this.inputReader = Cached.of(() -> new BufferedReader(new InputStreamReader(
                (InputStream) Data.this.streams[SystemIO.STDIN]
            )));
            this.outputWriter = Cached.of(() -> new BufferedWriter(new OutputStreamWriter(
                (OutputStream) Data.this.streams[SystemIO.STDOUT]
            )));
            this.errorWriter = Cached.of(() -> new BufferedWriter(new OutputStreamWriter(
                (OutputStream) Data.this.streams[SystemIO.STDERR]
            )));
        }
    }

    // Maintain information on files in use. The index to the arrays is the "file
    // descriptor."
    // Ken Vollmar, August 2005

    private static final class FileIOData {
        public static @NotNull Data data;

        static {
            data = new Data();
            data.fileNames = new String[SystemIO.SYSCALL_MAXFILES];
            data.fileFlags = new int[SystemIO.SYSCALL_MAXFILES];
            data.streams = new Closeable[SystemIO.SYSCALL_MAXFILES];
        }

        /**
         * Reset all file information. Closes any open files and resets the arrays
         */
        private static void resetFiles() {
            for (int i = 0; i < SystemIO.SYSCALL_MAXFILES; i++) {
                FileIOData.close(i);
            }
            if (FileIOData.data.outputWriter.isInitialized()) {
                try {
                    FileIOData.data.outputWriter.get().close();
                    FileIOData.data.outputWriter.invalidate();
                } catch (final IOException ignored) {
                }
            }
            if (FileIOData.data.errorWriter.isInitialized()) {
                try {
                    FileIOData.data.errorWriter.get().close();
                    FileIOData.data.errorWriter.invalidate();
                } catch (final IOException ignored) {
                }
            }
            FileIOData.setupStdio();
        }

        private static void setupStdio() {
            FileIOData.data.fileNames[SystemIO.STDIN] = "STDIN";
            FileIOData.data.fileNames[SystemIO.STDOUT] = "STDOUT";
            FileIOData.data.fileNames[SystemIO.STDERR] = "STDERR";
            FileIOData.data.fileFlags[SystemIO.STDIN] = SystemIO.O_RDONLY;
            FileIOData.data.fileFlags[SystemIO.STDOUT] = SystemIO.O_WRONLY;
            FileIOData.data.fileFlags[SystemIO.STDERR] = SystemIO.O_WRONLY;
            FileIOData.data.streams[SystemIO.STDIN] = System.in;
            FileIOData.data.streams[SystemIO.STDOUT] = System.out;
            FileIOData.data.streams[SystemIO.STDERR] = System.err;
            System.out.flush();
            System.err.flush();
        }

        /**
         * Preserve a stream that is in use
         */
        private static void setStreamInUse(final int fd, final Closeable s) {
            FileIOData.data.streams[fd] = s;
        }

        /**
         * Retrieve a stream for use
         */
        private static Closeable getStreamInUse(final int fd) {
            return FileIOData.data.streams[fd];
        }

        /**
         * Determine whether a given file is already in use.
         */
        private static boolean filenameInUse(final @NotNull String requestedFilename) {
            for (final var optFilename : FileIOData.data.fileNames) {
                if (optFilename != null && optFilename.equals(requestedFilename)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Determine whether a given fd is already in use with the given flag.
         */
        private static boolean fdNotInUse(final int fd, final int flag) {
            if (fd < 0 || fd >= SystemIO.SYSCALL_MAXFILES) {
                return true;
            } else if (FileIOData.data.fileNames[fd] != null && FileIOData.data.fileFlags[fd] == 0 && flag == 0) { // O_RDONLY 
                // O_WRONLY
                // write-only
                // read-only
                return false;
            } else {
                return FileIOData.data.fileNames[fd] == null || ((FileIOData.data.fileFlags[fd] & flag & SystemIO.O_WRONLY) != SystemIO.O_WRONLY);
            }

        }

        /**
         * Close the file with file descriptor fd. No errors are recoverable -- if the
         * user's
         * made an error in the call, it will come back to him.
         */
        private static void close(final int fd) {
            // Can't close STDIN, STDOUT, STDERR, or invalid fd
            if (fd <= SystemIO.STDERR || fd >= SystemIO.SYSCALL_MAXFILES) {
                return;
            }

            FileIOData.data.fileNames[fd] = null;
            // All this code will be executed only if the descriptor is open.
            if (FileIOData.data.streams[fd] != null) {
                final var keepStream = FileIOData.data.streams[fd];
                FileIOData.data.fileFlags[fd] = -1;
                FileIOData.data.streams[fd] = null;
                try {
                    keepStream.close();
                } catch (final IOException ioe) {
                    // not concerned with this exception
                }
            } else {
                FileIOData.data.fileFlags[fd] = -1; // just to be sure... streams[fd] known to be null
            }
        }

        /**
         * Attempt to open a new file with the given flag, using the lowest available
         * file descriptor.
         * Check that file is not in use, flag is reasonable, and there is an
         * available file descriptor.
         * Return: file descriptor in 0...(SYSCALL_MAXFILES-1), or -1 if error
         */
        private static int nowOpening(final String filename, final int flag) {
            if (FileIOData.filenameInUse(filename)) {
                return -1;
            }

            if (flag != SystemIO.O_RDONLY
                && flag != SystemIO.O_WRONLY
                && flag != (SystemIO.O_WRONLY | SystemIO.O_APPEND)) // Only read and write are implemented
            {
                return -1;
            }

            int i = 0;
            while (FileIOData.data.fileNames[i] != null && i < SystemIO.SYSCALL_MAXFILES) {
                i++;
            } // Attempt to find available file descriptor

            if (i >= SystemIO.SYSCALL_MAXFILES) // no available file descriptors
            {
                return -1;
            }

            // Must be OK -- put file in table
            FileIOData.data.fileNames[i] = filename; // our table has its own copy of file
            FileIOData.data.fileFlags[i] = flag;
            return i;
        }

    } // end private class FileIOData
}
