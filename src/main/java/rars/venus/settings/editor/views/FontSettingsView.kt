package rars.venus.settings.editor.views

import rars.util.FontUtilities
import rars.util.FontWeight
import rars.venus.util.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Insets
import javax.swing.*

class FontSettingsView : JPanel() {
    val fontSizeSpinner = JSpinner().apply {
        model = SpinnerNumberModel(
            /* value = */ 12,
            /* minimum = */ 1,
            /* maximum = */ 100,
            /* stepSize = */ 1
        )
        toolTipText = "Current font size in points."
    }
    val fontSelector: JComboBox<String> =
        JComboBox(FontUtilities.allFontFamilies).apply {
            isEditable = false
            maximumRowCount = 20
        }
    val fontWeightSelector: JComboBox<FontWeight> =
        JComboBox(FontWeight.entries.toTypedArray()).apply {
            isEditable = false
            maximumRowCount = FontWeight.entries.size
        }
    val ligaturesCheckbox = JCheckBox().apply {
        toolTipText = "Enable or disable ligatures."
    }

    init {
        val baseConstraints = GridBagConstraints().apply {
            fill = GridBagConstraints.BOTH
            insets = Insets(5, 5, 5, 5)
        }
        GridBagLayout(baseConstraints) {
            add(FONT_LABEL, gridx = 0, gridy = 0)
            add(fontSelector, gridx = 1, gridy = 0)

            add(SIZE_LABEL, gridx = 0, gridy = 1)
            add(fontSizeSpinner, gridx = 1, gridy = 1)

            add(WEIGHT_LABEL, gridx = 0, gridy = 2)
            add(fontWeightSelector, gridx = 1, gridy = 2)

            add(LIGATURES_LABEL, gridx = 0, gridy = 3)
            add(ligaturesCheckbox, gridx = 1, gridy = 3)
        }
    }

    companion object {
        private val FONT_LABEL = JLabel("Font family")
        private val WEIGHT_LABEL = JLabel("Font weight")
        private val SIZE_LABEL = JLabel("Font size")
        private val LIGATURES_LABEL = JLabel("Ligatures")
    }
}