package rars.venus.settings.editor.controllers

import rars.settings.FontSettingsImpl
import rars.util.FontWeight
import rars.venus.editors.TextEditingArea
import rars.venus.settings.editor.views.FontSettingsView
import java.awt.Font
import java.awt.font.TextAttribute

class FontSettingsController(
    view: FontSettingsView,
    private val textArea: TextEditingArea,
    private val settings: FontSettingsImpl
) {
    private var ligaturesEnabled = false
    private var fontSize = 0
    private var fontFamily: String? = null
    private var fontWeight: FontWeight? = null

    init {
        resetButtonValues()
        view.initializeControls()
    }

    private fun FontSettingsView.initializeControls() = apply {
        fontSelector.apply {
            selectedItem = fontFamily
            addItemListener {
                val selected = selectedItem as? String ?: return@addItemListener
                fontFamily = selected
                val currentFont = textArea.font
                val derivedFont = Font(fontFamily, currentFont.style, currentFont.size)
                textArea.font = derivedFont
            }
        }
        ligaturesCheckbox.apply {
            isSelected = ligaturesEnabled
            addItemListener {
                ligaturesEnabled = isSelected
                val currentFont = textArea.font
                val transformMap = mapOf(TextAttribute.LIGATURES to if (ligaturesEnabled) 1 else 0)
                val derivedFont = currentFont.deriveFont(transformMap)
                textArea.font = derivedFont
            }
        }
        fontSizeSpinner.apply {
            value = fontSize
            addChangeListener {
                fontSize = value as Int
                val currentFont = textArea.font
                val derivedFont = Font(currentFont.name, currentFont.style, fontSize)
                textArea.font = derivedFont
            }
        }
        fontWeightSelector.apply {
            selectedItem = fontWeight
            addItemListener {
                val selected = selectedItem as? FontWeight?
                if (selected == null) return@addItemListener
                fontWeight = selected
                val currentFont = textArea.font
                val transformMap = mapOf(TextAttribute.WEIGHT to fontWeight!!.weight)
                val derivedFont = currentFont.deriveFont(transformMap)
                textArea.font = derivedFont
            }
        }
    }

    fun applySettings() {
        settings.apply {
            fontFamily = this@FontSettingsController.fontFamily!!
            fontSize = this@FontSettingsController.fontSize
            isLigaturized = this@FontSettingsController.ligaturesEnabled
            fontWeight = this@FontSettingsController.fontWeight!!
            saveSettingsToPreferences()
        }
    }

    fun resetButtonValues() {
        ligaturesEnabled = settings.isLigaturized
        fontFamily = settings.fontFamily
        fontSize = settings.fontSize
        fontWeight = settings.fontWeight
    }
}
