package rars.venus.run

import org.jetbrains.annotations.Contract
import rars.venus.util.BorderLayout
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSlider

/**
 * Class for the Run speed slider control.
 *
 * @author Pete Sanderson
 * @version August 2005
 */
class RunSpeedPanel : JPanel() {
    private val sliderLabel: JLabel

    @Volatile
    private var runSpeedIndex: Int = SPEED_INDEX_MAX

    init {
        val runSpeedSlider = createSlider()
        sliderLabel = JLabel(this.createLabel(this.runSpeedIndex)).apply {
            horizontalAlignment = JLabel.CENTER
            alignmentX = CENTER_ALIGNMENT
        }
        BorderLayout {
            this[BorderLayout.NORTH] = sliderLabel
            this[BorderLayout.CENTER] = runSpeedSlider
        }
        val speed = SPEEDS[SPEED_INDEX_INTERACTION_LIMIT].toInt()
        toolTipText =
            """Simulation speed for "Go". At $speed inst/sec or less, tables updated after each instruction."""
    }

    private fun createSlider(): JSlider = JSlider(
        JSlider.HORIZONTAL, SPEED_INDEX_MIN,
        SPEED_INDEX_MAX, SPEED_INDEX_INIT
    ).apply {
        size = Dimension(100, size.height.toInt())
        maximumSize = size
        majorTickSpacing = 5
        paintTicks = true // Create the label table
        addChangeListener {
            if (!valueIsAdjusting) {
                runSpeedIndex = value
            } else {
                sliderLabel.text = createLabel(value)
            }
        }
    }

    /**
     * Current run speed setting, in instructions/second. Unlimited speed
     * setting is equal to RunSpeedPanel.UNLIMITED_SPEED
     */
    val runSpeed: Double get() = SPEEDS[this.runSpeedIndex]

    /**
     * Set label wording depending on current speed setting.
     */
    @Contract(pure = true)
    private fun createLabel(index: Int): String = buildString {
        append("Run speed ")
        if (index <= SPEED_INDEX_INTERACTION_LIMIT) {
            if (SPEEDS[index] < 1) {
                append(SPEEDS[index])
            } else {
                append(SPEEDS[index].toInt())
            }
            append(" inst/sec")
        } else {
            append("at max (no interaction)")
        }
    }

    companion object {
        /**
         * Constant that represents unlimited run speed. Compare with return value of
         * getRunSpeed() to determine if set to unlimited. At the unlimited setting, the GUI
         * will not attempt to update register and memory contents as each instruction
         * is executed. This is the only possible value for command-line use of Mars.
         */
        const val UNLIMITED_SPEED: Double = 40.0

        private const val SPEED_INDEX_MIN = 0
        private const val SPEED_INDEX_MAX = 40
        private const val SPEED_INDEX_INIT = 40
        private const val SPEED_INDEX_INTERACTION_LIMIT = 35

        private val SPEEDS = doubleArrayOf(
            0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0,  // 0-10
            6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0,  // 11-20
            16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0, 25.0,  // 21-30
            26.0, 27.0, 28.0, 29.0, 30.0, UNLIMITED_SPEED, UNLIMITED_SPEED,  // 31-37
            UNLIMITED_SPEED, UNLIMITED_SPEED, UNLIMITED_SPEED // 38-40
        )
    }
}

sealed interface RunSpeed {
    data class Limited(val speed: Double) : RunSpeed
    data object Unlimited : RunSpeed
}
