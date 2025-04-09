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

    override fun read(
        initialValue: String,
        prompt: String,
        maxLength: Int
    ): String = try {
        inputReader.readLine() ?: ""
    } catch (_: IOException) {
        ""
    }

    override fun printString(message: String) {
        try {
            outputWriter.write(message)
            outputWriter.flush()
        } catch (_: IOException) {
        }
    }

    override fun openFile(filename: String, flags: Int): Int {
        val fd = fileHandler.openFile(filename, flags)
        return if (fd == -1) {
            -1
        } else {
            fd + 3
        }
    }

    override fun closeFile(fd: Int) {
        fileHandler.closeFile(fd - 3)
    }

    override fun writeToFile(fd: Int, myBuffer: ByteArray, lengthRequested: Int): Int = when (fd) {
        STDOUT -> try {
            outputWriter.write(String(myBuffer))
            outputWriter.flush()
            myBuffer.size
        } catch (_: IOException) {
            -1
        }

        STDERR -> try {
            errorWriter.write(String(myBuffer))
            errorWriter.flush()
            myBuffer.size
        } catch (_: IOException) {
            -1
        }

        else -> fileHandler.writeToFile(fd - 3, myBuffer, lengthRequested)
    }

    override fun seek(fd: Int, offset: Int, base: Int): Int =
        if (fd !in (STDERR + 1..<SYSCALL_MAXFILES)) {
            -1
        } else fileHandler.seek(fd - 3, offset, base)

    override fun readFromFile(fd: Int, myBuffer: ByteArray, lengthRequested: Int): Int = if (fd == STDIN) try {
        stdin.read(myBuffer, 0, lengthRequested)
    } catch (_: IOException) {
        -1
    } else fileHandler.readFromFile(fd - 3, myBuffer, lengthRequested)

    override fun flush() {}
}
