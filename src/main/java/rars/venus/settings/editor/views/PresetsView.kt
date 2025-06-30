package rars.venus.settings.editor.views

import rars.venus.util.BoxLayout
import rars.venus.util.JPanel
import javax.swing.*

class PresetsView : JScrollPane() {
    val sections = mutableListOf<PresetSection>()
    private val mainPanel = JPanel {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    init {
        setViewportView(mainPanel)
    }

    fun addSection(section: PresetSection) {
        sections.add(section)
        mainPanel.add(section)
    }

    class PresetSection(themeName: String) : JPanel() {
        val button: JButton = JButton("Apply")

        init {
            BoxLayout(BoxLayout.X_AXIS) {
                +JLabel(themeName)
                horizontalGlue()
                +button
            }
        }
    }
}
