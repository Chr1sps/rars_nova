package rars.riscv.syscalls

import rars.Globals
import rars.notices.SimulatorNotice
import rars.riscv.hardware.memory.Memory
import rars.util.BitmapDisplay

// TODO: move this to IO
class DisplayBitmapImpl(
    private val memory: Memory
) {
    private var display: BitmapDisplay? = null

    init {
        Globals.SIMULATOR.simulatorNoticeHook.subscribe { notice ->
            if (notice.action == SimulatorNotice.Action.START) {
                display?.dispose()
                display = null
            }
        }
    }

    fun show(baseAddress: Int, width: Int, height: Int) {
        when {
            display == null -> {
                display = BitmapDisplay(memory, baseAddress, width, height)
            }
            display!!.displayWidth != width || display!!.displayHeight != height -> {
                display!!.unsubscribeFromMemory()
                display!!.dispose()
                display = BitmapDisplay(memory, baseAddress, width, height)
            }
            display!!.baseAddress != baseAddress -> {
                display!!.changeBaseAddress(baseAddress)
                display!!.repaint()
            }
        }
        if (!display!!.isVisible) {
            display!!.isVisible = true
        }
    }

    companion object {
        val INSTANCE: DisplayBitmapImpl = DisplayBitmapImpl(Globals.MEMORY_INSTANCE)
    }
}
