package rars.venus.settings.editor.views

import rars.util.FontUtilities
import rars.util.FontWeight
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class FontSettingsView : JPanel() {
    val fontSizeSpinner: JSpinner
    val fontSelector: JComboBox<String>
    val fontWeightSelector: JComboBox<FontWeight>
    val ligaturesCheckbox: JCheckBox

    init {
        this.layout = GridBagLayout()
        val gbc = GridBagConstraints()

        gbc.fill = GridBagConstraints.BOTH
        gbc.insets = Insets(5, 5, 5, 5)

        // font family
        gbc.gridy = 0
        gbc.gridx = 0
        add(FONT_LABEL, gbc)
        val fontFamilies = FontUtilities.allFontFamilies
        this.fontSelector = JComboBox(fontFamilies)
        this.fontSelector.setEditable(false)
        this.fontSelector.setMaximumRowCount(20)
        gbc.gridx = 1
        this.add(fontSelector, gbc)

        // font size
        gbc.gridy = 1
        gbc.gridx = 0
        this.add(SIZE_LABEL, gbc)
        val fontSizeModel = SpinnerNumberModel(
            12,
            1,
            100,
            1
        )
        this.fontSizeSpinner = JSpinner(fontSizeModel)
        this.fontSizeSpinner.setToolTipText("Current font size in points.")
        gbc.gridx = 1
        this.add(fontSizeSpinner, gbc)

        // font weight
        gbc.gridy = 2
        gbc.gridx = 0
        this.add(WEIGHT_LABEL, gbc)
        this.fontWeightSelector = JComboBox<FontWeight>(FontWeight.entries.toTypedArray())
        this.fontWeightSelector.setEditable(false)
        this.fontWeightSelector.setMaximumRowCount(FontWeight.entries.size)
        gbc.gridx = 1
        this.add(fontWeightSelector, gbc)

        // ligatures
        gbc.gridy = 3
        gbc.gridx = 0
        this.add(LIGATURES_LABEL, gbc)
        this.ligaturesCheckbox = JCheckBox()
        this.ligaturesCheckbox.setToolTipText("Enable or disable ligatures.")
        gbc.gridx = 1
        this.add(ligaturesCheckbox, gbc)
    }

    companion object {
        private val FONT_LABEL = JLabel("Font family")
        private val WEIGHT_LABEL = JLabel("Font weight")
        private val SIZE_LABEL = JLabel("Font size")
        private val LIGATURES_LABEL = JLabel("Ligatures")
    }
}
