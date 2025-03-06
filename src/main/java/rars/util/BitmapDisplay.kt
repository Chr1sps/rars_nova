package rars.util

import arrow.core.Either
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rars.assembler.DataTypes
import rars.notices.AccessNotice
import rars.notices.MemoryAccessNotice
import rars.riscv.hardware.Memory
import rars.riscv.hardware.MemoryListenerHandle
import java.awt.Color
import java.awt.Dimension
import javax.swing.JFrame
import kotlin.math.max
import kotlin.math.min

// TODO: move this class to the common SystemIO class
class BitmapDisplay(
    private val memory: Memory,
    @JvmField var baseAddress: Int,
    @JvmField val displayWidth: Int,
    @JvmField val displayHeight: Int
) : JFrame() {
    private var upperAddressBound: Int = baseAddress + (displayWidth * displayHeight * DataTypes.WORD_SIZE)
    private val grid: Grid = Grid(displayHeight, displayWidth)
    private val panel: GraphicsPanel = GraphicsPanel(Dimension(displayWidth, displayHeight), this.grid)
    private val accessNoticeCallback: (MemoryAccessNotice) -> Unit = { notice ->
        if (notice.accessType == AccessNotice.AccessType.WRITE) {
            this.updateDisplay(notice.address, notice.length)
        }
    }
    private var handle: MemoryListenerHandle<Int>

    init {
        this.setTitle("Syscall: DisplayBitmap")
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE)
        this.add(this.panel)
        this.setResizable(false)

        this.fillGrid()
        this.pack()

        handle = this.memory
            .subscribe(this.accessNoticeCallback, baseAddress, upperAddressBound)
            .unwrap()
    }

    fun changeBaseAddress(newBaseAddress: Int) {
        this.memory.unsubscribe(this.handle)
        this.baseAddress = newBaseAddress
        this.upperAddressBound = newBaseAddress + (this.displayWidth * this.displayHeight * DataTypes.WORD_SIZE)
        handle = this.memory.subscribe(
            this.accessNoticeCallback,
            this.baseAddress,
            this.upperAddressBound
        ).unwrap()
    }

    fun unsubscribeFromMemory() {
        this.memory.unsubscribe(handle)
    }

    private fun fillGrid() {
        var currentOffset = 0
        for (row in 0..<this.displayHeight) {
            for (col in 0..<this.displayWidth) {
                val address = this.baseAddress + currentOffset
                val word = this.memory.getWordNoNotify(address).unwrap { e ->
                    LOGGER.error("Error updating color for address {} in bitmap display: {}", address, e)
                    return
                }
                val color = Color(word)
                this.grid.setColor(row, col, color)
                currentOffset += DataTypes.WORD_SIZE
            }
        }
    }

    private fun updateDisplay(memoryAddress: Int, writeLength: Int) {
        // figure out which pixels were changed

        val endAddress = memoryAddress + writeLength
        // clamp the range to the display bounds
        if (endAddress >= this.baseAddress && memoryAddress <= this.upperAddressBound) {
            // the memory written may not be aligned by 4 bytes, so we round
            // the start and end to the nearest 4 byte boundary
            val start = (max(memoryAddress, this.baseAddress) / 4) * 4
            val end = ((min(endAddress, this.upperAddressBound) + 3) / 4) * 4
            // these values are already nicely aligned, so all that's left to
            // do is to update the grid
            var row = (start - this.baseAddress) / (this.displayWidth * 4)
            var col = (start - this.baseAddress) % (this.displayWidth * 4) / 4
            var i = start
            while (i < end) {
                when (val word = this.memory.getWordNoNotify(i)) {
                    is Either.Left -> {
                        LOGGER.error("Error updating color for address {} in bitmap display: {}", i, word.value)
                        break
                    }
                    is Either.Right -> {
                        val color = Color(word.value)
                        this.grid.setColor(row, col, color)
                        col++
                        if (col == this.displayWidth) {
                            col = 0
                            row++
                        }
                        i += 4
                    }
                }
            }
        }
        this.panel.repaint()
    }

    companion object {
        private val LOGGER: Logger = LogManager.getLogger(BitmapDisplay::class.java)
    }
}
