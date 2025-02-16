package rars.io

import rars.settings.BoolSetting
import rars.settings.BoolSettingsImpl
import rars.venus.MessagesPane
import java.nio.charset.StandardCharsets
import kotlin.math.min

class VenusIO(
    private val messagesPane: MessagesPane,
    private val boolSettings: BoolSettingsImpl
) : AbstractIO {
    private val fileHandler = FileHandler(SYSCALL_MAXFILES - 3, this.boolSettings)
    private var buffer = ""
    private var lastTime = 0L

    override fun readImpl(
        initialValue: String,
        prompt: String,
        maxLength: Int
    ): String {
        val isPopup = this.boolSettings.getSetting(BoolSetting.POPUP_SYSCALL_INPUT)
        return if (isPopup) messagesPane.getInputStringFromDialog(prompt)
        else messagesPane.getInputString(maxLength)
    }

    override fun printString(message: String) {
        this.printToGui(message)
    }

    override fun openFile(filename: String, flags: Int): Int {
        val fd = this.fileHandler.openFile(filename, flags)
        return if (fd == -1) -1 else fd + 3
    }

    override fun closeFile(fd: Int) {
        this.fileHandler.closeFile(fd - 3)
    }

    fun resetFiles() {
        this.fileHandler.closeAll()
    }

    override fun writeToFile(fd: Int, myBuffer: ByteArray, lengthRequested: Int): Int = when (fd) {
        STDOUT, STDERR -> {
            val string = String(myBuffer, StandardCharsets.UTF_8) // decode the bytes using UTF-8 
            this.printToGui(string)
            myBuffer.size
        }
        else -> this.fileHandler.writeToFile(fd - 3, myBuffer, lengthRequested)
    }

    override fun seek(fd: Int, offset: Int, base: Int): Int = if (fd !in STDERR..SYSCALL_MAXFILES)
        -1
    else
        this.fileHandler.seek(fd - 3, offset, base)

    override fun readFromFile(fd: Int, myBuffer: ByteArray, lengthRequested: Int): Int = if (fd == STDIN) {
        val input = this.messagesPane.getInputString(lengthRequested)
        val bytesRead = input.toByteArray()

        for (i in myBuffer.indices) {
            myBuffer[i] = if (i < bytesRead.size) bytesRead[i] else 0
        }
        min(myBuffer.size.toDouble(), bytesRead.size.toDouble()).toInt()
    } else
        this.fileHandler.readFromFile(fd - 3, myBuffer, lengthRequested)

    override fun flush() {
        this.messagesPane.postRunMessage(this.buffer)
        this.buffer = ""
        this.lastTime = System.currentTimeMillis() + 100
    }

    private fun printToGui(message: String) {
        val time = System.currentTimeMillis()
        if (time > this.lastTime) {
            this.messagesPane.postRunMessage(this.buffer + message)
            this.buffer = ""
            this.lastTime = time + 100
        } else {
            this.buffer += message
        }
    }
}
