package rars.simulator

import rars.notices.SimulatorNotice
import rars.util.ListenerDispatcher
import rars.venus.VenusUI
import rars.venus.run.RunSpeedPanel
import javax.swing.SwingUtilities

class GuiSimThread(
    pc: Int,
    maxSteps: Int,
    breakPoints: IntArray,
    simulatorNoticeDispatcher: ListenerDispatcher<SimulatorNotice>,
    private val mainUI: VenusUI
) : SimThread(
    pc,
    maxSteps,
    breakPoints,
    mainUI.venusIO,
    simulatorNoticeDispatcher
) {
    override val runSpeed: Double
        get() = mainUI.runSpeedPanel.runSpeed

    override fun onEndLoop() {
        if (maxSteps != 1 && runSpeed < RunSpeedPanel.UNLIMITED_SPEED) {
            SwingUtilities.invokeLater { updateUI() }
            synchronized(this) {
                try {
                    (this as Object).wait((1000 / runSpeed).toLong())
                } catch (_: InterruptedException) {
                }
            }
//            lock.withLock {
//                try {
//                    condition.await((1000 / runSpeed).toLong(), TimeUnit.MILLISECONDS)
//                } catch (_: InterruptedException) {
//                }
//            }
        }
    }

    private fun updateUI() {
        mainUI.mainPane.executePane.apply {
            if (mainUI.registersPane.selectedComponent === registerValues) {
                registerValues.updateRegisters()
            } else {
                fpRegValues.updateRegisters()
            }
            dataSegment.updateValues()
            textSegment.codeHighlighting = true
            textSegment.highlightStepAtPC()
        }
    }
}
