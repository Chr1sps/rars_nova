package rars.io

import rars.settings.BoolSettingsImpl
import java.io.*

class ConsoleIO(
    private val stdin: InputStream,
    stdout: OutputStream,
    stderr: OutputStream,
    boolSettings: BoolSettingsImpl
) : AbstractIO {
    private val inputReader by lazy { BufferedReader(InputStreamReader(stdin)) }
    private val outputWriter by lazy { BufferedWriter(OutputStreamWriter(stdout)) }
    private val errorWriter by lazy { BufferedWriter(OutputStreamWriter(stderr)) }
    private val fileHandler = FileHandler(SYSCALL_MAXFILES - 3, boolSettings)

    override fun readImpl(
        initialValue: String,
        prompt: String,
        maxLength: Int
    ): String = try {
        val readLine = this.inputReader.readLine()
        readLine ?: ""
    } catch (_: IOException) {
        ""
    }

    override fun printString(message: String) {
        try {
            this.outputWriter.write(message)
            this.outputWriter.flush()
        } catch (_: IOException) {
        }
    }

    override fun openFile(filename: String, flags: Int): Int {
        val fd = this.fileHandler.openFile(filename, flags)
        return if (fd == -1) {
            -1
        } else {
            fd + 3
        }
    }

    override fun closeFile(fd: Int) {
        this.fileHandler.closeFile(fd - 3)
    }

    override fun writeToFile(fd: Int, myBuffer: ByteArray, lengthRequested: Int): Int {
        when (fd) {
            STDOUT -> {
                try {
                    this.outputWriter.write(String(myBuffer))
                    this.outputWriter.flush()
                    return myBuffer.size
                } catch (_: IOException) {
                    return -1
                }
            }

            STDERR -> {
                try {
                    this.errorWriter.write(String(myBuffer))
                    this.errorWriter.flush()
                    return myBuffer.size
                } catch (_: IOException) {
                    return -1
                }
            }

            else -> {
                return this.fileHandler.writeToFile(fd - 3, myBuffer, lengthRequested)
            }
        }
    }

    override fun seek(fd: Int, offset: Int, base: Int): Int {
        if (fd <= STDERR || fd >= SYSCALL_MAXFILES) {
            return -1
        }
        return this.fileHandler.seek(fd - 3, offset, base)
    }

    override fun readFromFile(fd: Int, myBuffer: ByteArray, lengthRequested: Int): Int = if (fd == STDIN) try {
        this.stdin.read(myBuffer, 0, lengthRequested)
    } catch (_: IOException) {
        -1
    } else this.fileHandler.readFromFile(fd - 3, myBuffer, lengthRequested)

    override fun flush() {}
}
