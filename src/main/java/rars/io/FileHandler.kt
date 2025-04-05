package rars.io

import rars.Globals
import rars.settings.BoolSetting
import rars.settings.BoolSettingsImpl
import java.io.*
import java.nio.channels.FileChannel

class FileHandler(
    private val fdCount: Int,
    private val boolSettings: BoolSettingsImpl
) {
    private val entries: Array<FileEntry?> = arrayOfNulls<FileEntry>(fdCount)

    /**
     * Open a file for either reading or writing. Note that read/write flag is NOT
     * IMPLEMENTED. Also note that file permission modes are also NOT IMPLEMENTED.
     *
     * @param filename
     * string containing file
     * @param flags
     * 0 for read, 1 for write
     * @return file descriptor in the range 0 to SYSCALL_MAXFILES-1, or -1 if error
     * @author Ken Vollmar
     */
    fun openFile(filename: String, flags: Int): Int {
        // Check internal plausibility of opening this file
        val fdToUse = this.tryFindEmptyFd(filename, flags)
        if (fdToUse < 0) return -1

        var filepath = File(filename)
        if (!filepath.isAbsolute && Globals.PROGRAM != null) {
            if (this.boolSettings.getSetting(BoolSetting.DERIVE_CURRENT_WORKING_DIRECTORY)) {
                val parent = Globals.PROGRAM!!.file!!.getParentFile()
                filepath = File(parent, filename)
            }
        }
        val stream = if (flags == O_RDONLY) try {
            FileInputStream(filepath)
        } catch (_: FileNotFoundException) {
            return -1
        } else if ((flags and O_WRONLY) != 0) try {
            FileOutputStream(filepath, (flags and O_APPEND) != 0)
        } catch (_: FileNotFoundException) {
            return -1
        } else return -1

        val newEntry = FileEntry(filename, flags, stream)
        this.entries[fdToUse] = newEntry

        return fdToUse
    }

    /**
     * Read bytes from file.
     *
     * @param fd
     * file descriptor
     * @param buffer
     * byte array to contain bytes read
     * @param length
     * number of bytes to read
     * @return number of bytes read, 0 on EOF, or -1 on error
     */
    fun readFromFile(fd: Int, buffer: ByteArray, length: Int): Int {
        // Check the existence of the "read" fd
        if (this.fdNotInUse(fd, O_RDONLY)) {
            return -1
        }
        // retrieve FileInputStream from storage
        val inputStream = this.entries[fd]!!.stream as InputStream
        var retValue: Int
        try {
            // Reads up to lengthRequested bytes of data from this Input stream into an
            // array of bytes.
            retValue = inputStream.read(buffer, 0, length)
            // This method will return -1 upon EOF, but our spec says that negative
            // value represents an error, so we return 0 for EOF.
            if (retValue == -1) {
                retValue = 0
            }
        } catch (_: IOException) {
            return -1
        } catch (_: IndexOutOfBoundsException) {
            return -1
        }
        return retValue
    }

    /**
     * Write bytes to file.
     *
     * @param fd
     * file descriptor
     * @param buffer
     * byte array containing characters to write
     * @param length
     * number of bytes to write
     * @return number of bytes written, or -1 on error
     */
    fun writeToFile(fd: Int, buffer: ByteArray, length: Int): Int {
        // Check the existence of the "write" fd
        if (this.fdNotInUse(fd, O_WRONLY)) {
            return -1
        }
        // retrieve FileOutputStream from storage
        val outputStream = this.entries[fd]!!.stream as OutputStream
        try {
            // Observation: made a call to outputStream.write(myBuffer, 0, lengthRequested)
            // with myBuffer containing 6(ten) 32-bit-words <---> 24(ten) bytes, where the
            // words are MIPS integers with values such that many of the bytes are ZEROES.
            // The effect is apparently that the write stops after encountering a
            // zero-valued
            // byte. (The method write does not return a value and so this can't be verified
            // by the return value.)
            // Writes up to lengthRequested bytes of data to this output stream from an
            // array of bytes.
            // outputStream.write(myBuffer, 0, lengthRequested);
            // write is a void method
            // -- no verification value returned

            // Force the write statement to write exactly
            // the number of bytes requested, even though those bytes include many ZERO
            // values.

            for (i in 0..<length) {
                outputStream.write(buffer[i].toInt())
            }
            outputStream.flush()
        } catch (_: IOException) {
            return -1
        } catch (_: IndexOutOfBoundsException) {
            return -1
        }

        return length
    }

    /**
     * Set a position in a file to read or write from.
     *
     * @param fd
     * file descriptor
     * @param offset
     * where in the file to seek to
     * @param base
     * the point to reference 0 for start of file, 1 for current
     * position, 2 for end of the file
     * @return -1 on error
     */
    fun seek(fd: Int, offset: Int, base: Int): Int {
        // Check the existence of the "read" fd
        if (this.fdNotInUse(fd, 0)) return -1
        if (fd !in 0..<this.fdCount) return -1
        val stream = this.entries[fd]!!.stream
        try {
            val channel: FileChannel = when (stream) {
                is FileInputStream -> stream.getChannel()
                is FileOutputStream -> stream.getChannel()
                else -> return -1
            }
            val newOffset = offset + when (base) {
                SEEK_CUR -> channel.position().toInt()
                SEEK_END -> channel.size().toInt()
                SEEK_SET -> 0
                else -> return -1
            }
            return if (newOffset < 0) {
                -1
            } else {
                channel.position(newOffset.toLong())
                offset
            }
        } catch (_: IOException) {
            return -1
        }
    }

    fun closeAll() {
        for (i in 0..<this.fdCount) {
            if (this.entries[i] != null) {
                try {
                    this.entries[i]!!.stream.close()
                } catch (_: IOException) {
                    // ignore
                }
                this.entries[i] = null
            }
        }
    }

    fun closeFile(fd: Int) {
        if (fd !in 0..<this.fdCount) return
        if (this.entries[fd] != null) {
            try {
                this.entries[fd]!!.stream.close()
            } catch (_: IOException) {
            }
            this.entries[fd] = null
        }
    }

    /**
     * Determine whether a given fd is already in use with the given flag.
     */
    private fun fdNotInUse(fd: Int, flag: Int): Boolean = if (fd !in 0..<fdCount) {
        true
    } else {
        val entry = entries[fd]
        if (entry != null && entry.flags == 0 && flag == 0) {
            false
        } else {
            entry == null || ((entry.flags and flag and O_WRONLY) != O_WRONLY)
        }
    }

    private fun isFilenameInUse(filename: String): Boolean = entries.any { it != null && it.filename == filename }

    private fun tryFindEmptyFd(filename: String, flag: Int): Int {
        if (this.isFilenameInUse(filename)) {
            return -1
        }
        // Only read and write are implemented
        if (flag != O_RDONLY && flag != O_WRONLY && flag != (O_WRONLY or O_APPEND)) {
            return -1
        }
        for (i in 0..<this.fdCount) {
            if (this.entries[i] == null) {
                return i
            }
        }
        return -1
    }

    @JvmRecord
    private data class FileEntry(val filename: String, val flags: Int, val stream: Closeable)

}
