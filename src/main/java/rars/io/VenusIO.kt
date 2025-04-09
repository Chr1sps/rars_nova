package rars.io

import rars.settings.BoolSetting
import rars.settings.BoolSettingsImpl
import rars.venus.MessagesPane
import kotlin.math.min

class VenusIO(
    private val messagesPane: MessagesPane,
    private val boolSettings: BoolSettingsImpl
) : AbstractIO {
    private val fileHandler = FileHandler(SYSCALL_MAXFILES - 3, boolSettings)
    private var buffer = ""
    private var lastTime = 0L

    override fun read(
        initialValue: String,
        prompt: String,
        maxLength: Int
    ): String {
        val isPopup = boolSettings.getSetting(BoolSetting.POPUP_SYSCALL_INPUT)
        return if (isPopup) messagesPane.getInputStringFromDialog(prompt)
        else messagesPane.getInputString(maxLength)
    }

    override fun printString(message: String) {
        printToGui(message)
    }

    override fun openFile(filename: String, flags: Int): Int {
        val fd = fileHandler.openFile(filename, flags)
        return if (fd == -1) -1 else fd + 3
    }

    override fun closeFile(fd: Int) {
        fileHandler.closeFile(fd - 3)
    }

    fun resetFiles() {
        fileHandler.closeAll()
    }

    override fun writeToFile(fd: Int, myBuffer: ByteArray, lengthRequested: Int): Int = when (fd) {
        STDOUT, STDERR -> {
            // decode the bytes using UTF-8 
            val string = String(myBuffer, Charsets.UTF_8)
            printToGui(string)
            myBuffer.size
        }
        else -> fileHandler.writeToFile(fd - 3, myBuffer, lengthRequested)
    }

    override fun seek(fd: Int, offset: Int, base: Int): Int = if (fd !in (STDERR + 1)..<SYSCALL_MAXFILES)
        -1
    else
        fileHandler.seek(fd - 3, offset, base)

    override fun readFromFile(fd: Int, myBuffer: ByteArray, lengthRequested: Int): Int = if (fd == STDIN) {
        val input = messagesPane.getInputString(lengthRequested)
        val bytesRead = input.toByteArray()

        for (i in myBuffer.indices) {
            myBuffer[i] = if (i < bytesRead.size) bytesRead[i] else 0
        }
        min(myBuffer.size.toDouble(), bytesRead.size.toDouble()).toInt()
    } else
        fileHandler.readFromFile(fd - 3, myBuffer, lengthRequested)

    override fun flush() {
        messagesPane.postRunMessage(buffer)
        buffer = ""
        lastTime = System.currentTimeMillis() + 100
    }

    private fun printToGui(message: String) {
        val time = System.currentTimeMillis()
        if (time > lastTime) {
            messagesPane.postRunMessage(buffer + message)
            buffer = ""
            lastTime = time + 100
        } else {
            buffer += message
        }
    }
}
