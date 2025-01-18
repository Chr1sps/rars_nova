package rars.io;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.settings.BoolSetting;
import rars.settings.BoolSettings;

import java.io.*;
import java.nio.channels.FileChannel;

public final class FileHandler {
    private static final int SYSCALL_MAXFILES = 32;

    private static final int STDIN = 0;
    private static final int STDOUT = 1;
    private static final int STDERR = 2;

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

    private final int fdCount;
    private final FileEntry[] entries;

    private final @NotNull BoolSettings boolSettings;

    public FileHandler(final int fdCount, final @NotNull BoolSettings boolSettings) {
        this.fdCount = fdCount;
        this.entries = new FileEntry[fdCount];
        this.boolSettings = boolSettings;
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
    public int openFile(final @NotNull String filename, final int flags) {
        // Check internal plausibility of opening this file
        final int fdToUse = this.tryFindEmptyFd(filename, flags);
        if (fdToUse < 0) {
            return -1;
        }

        File filepath = new File(filename);
        if (!filepath.isAbsolute() && Globals.program != null) {
            if (this.boolSettings.getSetting(BoolSetting.DERIVE_CURRENT_WORKING_DIRECTORY)) {
                final var parent = Globals.program.getFile().getParentFile();
                filepath = new File(parent, filename);
            }
        }
        final Closeable stream;
        if (flags == O_RDONLY) {
            try {
                stream = new FileInputStream(filepath);
            } catch (final FileNotFoundException e) {
                return -1;
            }
        } else if ((flags & O_WRONLY) != 0) {
            try {
                stream = new FileOutputStream(filepath, (flags & O_APPEND) != 0);
            } catch (final FileNotFoundException e) {
                return -1;
            }
        } else {
            return -1;
        }

        final var newEntry = new FileEntry(filename, flags, stream);
        this.entries[fdToUse] = newEntry;

        return fdToUse;
    }

    /**
     * Read bytes from file.
     *
     * @param fd
     *     file descriptor
     * @param buffer
     *     byte array to contain bytes read
     * @param length
     *     number of bytes to read
     * @return number of bytes read, 0 on EOF, or -1 on error
     */
    public int readFromFile(final int fd, final byte[] buffer, final int length) {
        // Check the existence of the "read" fd
        if (this.fdNotInUse(fd, O_RDONLY)) {
            return -1;
        }
        // retrieve FileInputStream from storage
        final var inputStream = (InputStream) this.entries[fd].stream;
        int retValue;
        try {
            // Reads up to lengthRequested bytes of data from this Input stream into an
            // array of bytes.
            retValue = inputStream.read(buffer, 0, length);
            // This method will return -1 upon EOF, but our spec says that negative
            // value represents an error, so we return 0 for EOF. DPS 10-July-2008.
            if (retValue == -1) {
                retValue = 0;
            }
        } catch (final IOException | IndexOutOfBoundsException e) {
            return -1;
        }
        return retValue;
    }

    /**
     * Write bytes to file.
     *
     * @param fd
     *     file descriptor
     * @param buffer
     *     byte array containing characters to write
     * @param length
     *     number of bytes to write
     * @return number of bytes written, or -1 on error
     */
    public int writeToFile(final int fd, final byte[] buffer, final int length) {
        // Check the existence of the "write" fd
        if (this.fdNotInUse(fd, O_WRONLY)) {
            return -1;
        }
        // retrieve FileOutputStream from storage
        final var outputStream = (OutputStream) this.entries[fd].stream;
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
            for (int i = 0; i < length; i++) {
                outputStream.write(buffer[i]);
            }
            outputStream.flush();// DPS 7-Jan-2013
        } catch (final IOException | IndexOutOfBoundsException e) {
            return -1;
        }

        return length;
    }

    /**
     * Set a position in a file to read or write from.
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
    public int seek(final int fd, int offset, final int base) {
        // Check the existence of the "read" fd
        if (this.fdNotInUse(fd, 0)) {
            return -1;
        }
        if (fd < 0 || fd >= this.fdCount) {
            return -1;
        }
        final var stream = this.entries[fd].stream;
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
                case SEEK_CUR -> offset += (int) channel.position();
                case SEEK_END -> offset += (int) channel.size();
                case SEEK_SET -> {
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

    public void closeAll() {
        for (var i = 0; i < this.fdCount; ++i) {
            if (this.entries[i] != null) {
                try {
                    this.entries[i].stream.close();
                } catch (final IOException e) {
                    // ignore
                }
                this.entries[i] = null;
            }
        }
    }

    public void closeFile(final int fd) {
        if (fd < 0 || fd >= this.fdCount) {
            return;
        }
        if (this.entries[fd] != null) {
            try {
                this.entries[fd].stream.close();
            } catch (final IOException e) {
                // ignore
            }
            this.entries[fd] = null;
        }
    }

    /**
     * Determine whether a given fd is already in use with the given flag.
     */
    private boolean fdNotInUse(final int fd, final int flag) {
        if (fd < 0 || fd >= fdCount) {
            return true;
        } else if (entries[fd] != null && entries[fd].flags == 0 && flag == 0) {
            // O_RDONLY 
            // O_WRONLY
            // write-only
            // read-only
            return false;
        } else {
            return entries[fd] == null || ((entries[fd].flags & flag & O_WRONLY) != O_WRONLY);
        }
    }

    private boolean isFilenameInUse(final @NotNull String filename) {
        for (final var entry : entries) {
            if (entry != null && entry.filename.equals(filename)) {
                return true;
            }
        }
        return false;
    }

    private int tryFindEmptyFd(final @NotNull String filename, final int flag) {
        if (this.isFilenameInUse(filename)) {
            return -1;
        }
        // Only read and write are implemented
        if (flag != O_RDONLY && flag != O_WRONLY && flag != (O_WRONLY | O_APPEND)) {
            return -1;
        }
        for (var i = 0; i < this.fdCount; ++i) {
            if (this.entries[i] == null) {
                return i;
            }
        }
        return -1;
    }

    private record FileEntry(@NotNull String filename, int flags, @NotNull Closeable stream) {
    }
}
