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
    val blinkRateSpinner: JSpinner
    val tabSizeSpinner: JSpinner

    init {
        val blinkRateModel = SpinnerNumberModel(
            initialCaretBlinkRate,
            Editor.MIN_BLINK_RATE,
            Editor.MAX_BLINK_RATE,
            1
        )
        val blinkRatePicker = GenericOptionSection(
            "Caret blink rate (ms, 0 to disable)",
            JSpinner(blinkRateModel)
        )
        val tabSizeModel = SpinnerNumberModel(
            initialEditorTabSize,
            Editor.MIN_TAB_SIZE,
            Editor.MAX_TAB_SIZE,
            1
        )
        val tabSizePicker = GenericOptionSection(
            "Tab size",
            JSpinner(tabSizeModel)
        )
        blinkRateSpinner = blinkRatePicker.component
        tabSizeSpinner = tabSizePicker.component
        BoxLayout(BoxLayout.Y_AXIS) {
            +buildRow(addMargins = true, blinkRatePicker, tabSizePicker)
            verticalGlue()
        }
    }
}
