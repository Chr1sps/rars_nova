package rars.venus.settings.editor.views

import rars.venus.Editor
import rars.venus.settings.editor.GenericOptionSection
import rars.venus.util.BoxLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class OtherSettingsView(
    initialCaretBlinkRate: Int,
    initialEditorTabSize: Int,
) : JPanel() {
    val blinkRateSpinner = JSpinner(
        SpinnerNumberModel(
            initialCaretBlinkRate,
            Editor.MIN_BLINK_RATE,
            Editor.MAX_BLINK_RATE,
            1
        )
    )
    val tabSizeSpinner: JSpinner = JSpinner(
        SpinnerNumberModel(
            initialEditorTabSize,
            Editor.MIN_TAB_SIZE,
            Editor.MAX_TAB_SIZE,
            1
        )
    )

    init {
        BoxLayout(BoxLayout.Y_AXIS) {
            +buildRow(
                addMargins = true,
                GenericOptionSection(
                    "Caret blink rate (ms, 0 to disable)",
                    blinkRateSpinner
                ),
                GenericOptionSection("Tab size", tabSizeSpinner)
            )
            verticalGlue()
        }
    }
}
