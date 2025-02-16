package rars.io

import rars.riscv.Syscall.*

interface AbstractIO {
    /**
     * Implements syscall to read a double value.
     * Client is responsible for catching NumberFormatException.
     *
     * @return double value corresponding to user input
     */
    fun readDouble() = readImpl(
        "0",
        "Enter a Double value (syscall ${ReadDouble.serviceNumber})",
        -1
    ).trim { it <= ' ' }.toDouble()

    /**
     * Implements syscall to read a float value.
     * Client is responsible for catching NumberFormatException.
     *
     * @return float value corresponding to user input
     * Feb 14 2005 Ken Vollmar
     */
    fun readFloat() = readImpl(
        "0",
        "Enter a Float value (syscall ${ReadFloat.serviceNumber})",
        -1
    ).trim { it <= ' ' }.toFloat()

    /**
     * Implements syscall to read an integer value.
     * Client is responsible for catching NumberFormatException.
     *
     * @return int value corresponding to user input
     */
    fun readInt() = readImpl(
        "0",
        "Enter an Integer value (syscall ${ReadInt.serviceNumber})",
        -1
    ).trim { it <= ' ' }.toInt()

    /**
     * Implements syscall to read a string.
     *
     * @param maxLength
     * the maximum string length
     * @return the entered string, truncated to maximum length if necessary
     */
    fun readString(maxLength: Int): String {
        val input = this.readImpl(
            "",
            "Enter a string of maximum length $maxLength (syscall ${ReadString.serviceNumber})",
            maxLength
        ).removeSuffix("\n")
        return if (maxLength <= 0) "" else input.take(maxLength)
    }

    /**
     * Implements syscall to read a char value.
     *
     * @return char value corresponding to user input
     */
    fun readChar() = this.readImpl(
        "0",
        "Enter a character value (syscall ${ReadChar.serviceNumber})", 1
    )[0]

    fun readImpl(
        initialValue: String,
        prompt: String,
        maxLength: Int
    ): String

    /**
     * Implements syscall to print a string.
     */
    fun printString(message: String)

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
    fun openFile(filename: String, flags: Int): Int

    /**
     * Close the file with specified file descriptor
     *
     * @param fd
     * the file descriptor of an open file
     */
    fun closeFile(fd: Int)

    /**
     * Write bytes to file.
     *
     * @param fd
     * file descriptor
     * @param myBuffer
     * byte array containing characters to write
     * @param lengthRequested
     * number of bytes to write
     * @return number of bytes written, or -1 on error
     */
    fun writeToFile(fd: Int, myBuffer: ByteArray, lengthRequested: Int): Int

    /**
     * Read bytes from file.
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
    fun seek(fd: Int, offset: Int, base: Int): Int

    /**
     * Read bytes from file.
     *
     * @param fd
     * file descriptor
     * @param myBuffer
     * byte array to contain bytes read
     * @param lengthRequested
     * number of bytes to read
     * @return number of bytes read, 0 on EOF, or -1 on error
     */
    fun readFromFile(fd: Int, myBuffer: ByteArray, lengthRequested: Int): Int

    fun flush()
}
