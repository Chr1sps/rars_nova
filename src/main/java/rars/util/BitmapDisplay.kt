package rars.util

import arrow.core.Either
import rars.assembler.DataTypes
import rars.logging.RARSLogging
import rars.logging.error
import rars.notices.AccessType
import rars.notices.MemoryAccessNotice
import rars.riscv.hardware.memory.Memory
import rars.riscv.hardware.memory.MemoryListenerHandle
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
    private var upperAddressBound: Int =
        baseAddress + (displayWidth * displayHeight * DataTypes.WORD_SIZE)
    private val grid: Grid = Grid(displayHeight, displayWidth)
    private val panel: GraphicsPanel =
        GraphicsPanel(Dimension(displayWidth, displayHeight), this.grid)
    private val accessNoticeCallback: (MemoryAccessNotice) -> Unit = { notice ->
        if (notice.accessType == AccessType.WRITE) {
            this.updateDisplay(notice.address, notice.length)
        }
    }
    private var handle: MemoryListenerHandle<Int>

    init {
        title = "Syscall: DisplayBitmap"
        defaultCloseOperation = DISPOSE_ON_CLOSE
        add(panel)
        isResizable = false

        fillGrid()
        pack()

        handle = memory
            .subscribe(accessNoticeCallback, baseAddress, upperAddressBound)
            .unwrap()
    }

    fun changeBaseAddress(newBaseAddress: Int) {
        memory.unsubscribe(handle)
        baseAddress = newBaseAddress
        upperAddressBound =
            newBaseAddress + (displayWidth * displayHeight * DataTypes.WORD_SIZE)
        handle = memory.subscribe(
            accessNoticeCallback,
            baseAddress,
            upperAddressBound
        ).unwrap()
    }

    fun unsubscribeFromMemory() {
        memory.unsubscribe(handle)
    }

    private fun fillGrid() {
        var currentOffset = 0
        for (row in 0..<displayHeight) {
            for (col in 0..<displayWidth) {
                val address = baseAddress + currentOffset
                val word =
                    memory.silentMemoryView.getWord(address).unwrap { e ->
                        LOGGER.error {
                            """Error updating color for address $address in bitmap display: $e"""
                        }
                        return
                    }
                val color = Color(word)
                grid.setColor(row, col, color)
                currentOffset += DataTypes.WORD_SIZE
            }
        }
    }

    private fun updateDisplay(memoryAddress: Int, writeLength: Int) {
        // figure out which pixels were changed

        val endAddress = memoryAddress + writeLength
        // clamp the range to the display bounds
        if (endAddress >= baseAddress && memoryAddress <= upperAddressBound) {
            // the memory written may not be aligned by 4 bytes, so we round
            // the start and end to the nearest 4 byte boundary
            val start = (max(memoryAddress, baseAddress) / 4) * 4
            val end = ((min(endAddress, upperAddressBound) + 3) / 4) * 4
            // these values are already nicely aligned, so all that's left to
            // do is to update the grid
            var row = (start - baseAddress) / (displayWidth * 4)
            var col = (start - baseAddress) % (displayWidth * 4) / 4
            var i = start
            while (i < end) {
                when (val word = memory.silentMemoryView.getWord(i)) {
                    is Either.Left -> {
                        LOGGER.error {
                            "Error updating color for address $i in bitmap display: ${word.value}"
                        }
                        break
                    }
                    is Either.Right -> {
                        val color = Color(word.value)
                        grid.setColor(row, col, color)
                        col++
                        if (col == displayWidth) {
                            col = 0
                            row++
                        }
                        i += 4
                    }
                }
            }
        }
        panel.repaint()
    }

    companion object {
        private val LOGGER = RARSLogging.forClass(BitmapDisplay::class)
    }
}
