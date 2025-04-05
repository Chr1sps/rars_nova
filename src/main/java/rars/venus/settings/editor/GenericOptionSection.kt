package rars.venus.settings.editor

import rars.venus.util.BoxLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class GenericOptionSection<T : JComponent>(
    title: String,
    val component: T
) : JPanel() {
    init {
        BoxLayout(BoxLayout.X_AXIS) {
            +JLabel(title)
            horizontalStrut(10)
            +component
        }
    }
}
