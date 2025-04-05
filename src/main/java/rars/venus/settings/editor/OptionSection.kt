package rars.venus.settings.editor

import rars.venus.util.BoxLayout
import java.awt.Color
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

class OptionSection(
    label: String,
    checkBoxState: Boolean?,
    colorPickerButtonState: Color?
) : JPanel() {

    val colorPickerButton: ColorPickerButton? = colorPickerButtonState?.let { color ->
        ColorPickerButton(color).apply {
            isEnabled = (checkBoxState != false)
        }
    }

    val checkBox: JCheckBox? = checkBoxState?.let { state ->
        JCheckBox().apply {
            isSelected = state
            if (colorPickerButton != null) addChangeListener {
                colorPickerButton.isEnabled = isSelected
            }
        }
    }

    init {
        BoxLayout(BoxLayout.X_AXIS) {
            +JLabel(label)
            if (checkBox != null) {
                horizontalStrut(10)
                +checkBox
            }
            if (colorPickerButton != null) {
                val width = if (checkBox != null) 5 else 10
                horizontalStrut(width)
                +colorPickerButton
            }
        }
    }
}
