package rars.venus.settings.editor

import java.awt.Color
import javax.swing.JButton
import javax.swing.JColorChooser

class ColorPickerButton(color: Color) : JButton("Pick Color") {
    var color: Color get() = background
    set(newValue) {
        val newForeground = newValue.getOppositeLuminanceColour()
        background = newValue
        foreground = newForeground
        text = "#%02x%02x%02x".format(newValue.red, newValue.green, newValue.blue)
    }

    init {
        this.color = color
        addActionListener {
            val result = JColorChooser.showDialog(null, "Choose color", this.color, false)
            if (result != null) {
                this.color = result
            }
        }
    }


    companion object {
        private fun Color.getOppositeLuminanceColour(): Color {
            // Calculate luminance
            val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue

            // Return white or black based on luminance
            return if (luminance > 127.5) Color.BLACK else Color.WHITE
        }
    }
}
