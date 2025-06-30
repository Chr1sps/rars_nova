package rars.venus.settings.editor

import rars.venus.util.BoxLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class BottomRowComponent : JPanel() {
    val applyButton = JButton("Apply")
    val applyAndCloseButton = JButton("Apply and close")
    val cancelButton = JButton("Cancel")

    init {
        BoxLayout(BoxLayout.X_AXIS) {
            horizontalGlue()
            +cancelButton
            horizontalStrut(10)
            +applyButton
            +applyAndCloseButton
        }
    }
}
