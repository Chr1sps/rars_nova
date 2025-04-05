package rars.venus.settings.editor.controllers

import rars.settings.TokenSettingKey
import rars.settings.TokenSettingKey.Companion.getTokenTypesForSetting
import rars.venus.editors.TextEditingArea
import rars.venus.editors.TokenStyle
import rars.venus.settings.editor.views.SyntaxStyleView
import javax.swing.AbstractButton
import javax.swing.event.ChangeListener

class SyntaxStyleSettingsController(
    private val view: SyntaxStyleView,
    private val parentController: EditorSettingsController,
    private val textArea: TextEditingArea
) {
    private var currentKey: TokenSettingKey

    init {
        this.currentKey = TokenSettingKey.COMMENT // dummy value
        view.initializeView()
    }

    private fun SyntaxStyleView.initializeView() {
        val tokenStyleChangeListener = ChangeListener {
            val tokenStyle = tokenStyleFromView
            parentController.settingsTheme.tokenStyles.put(currentKey, tokenStyle)
            getTokenTypesForSetting(currentKey).forEach { key ->
                textArea.setTokenStyle(key, tokenStyle)
            }
        }
        arrayOf<AbstractButton>(
            useForeground,
            foregroundColorButton,
            useBackground,
            backgroundColorButton,
            isBold,
            isItalic,
            isUnderline
        ).forEach { component ->
            component.addChangeListener(tokenStyleChangeListener)
        }
    }

    private val tokenStyleFromView: TokenStyle
        get() = view.run {
            val foreground = if (useForeground.isSelected) foregroundColorButton.color else null
            val background = if (useBackground.isSelected) backgroundColorButton.color else null
            val isBold = isBold.isSelected
            val isItalic = isItalic.isSelected
            val isUnderline = isUnderline.isSelected
            TokenStyle(foreground, background, isBold, isItalic, isUnderline)
        }

    fun setCurrentKey(key: TokenSettingKey) {
        this.currentKey = key
        view.setView(key)
    }

    fun resetButtonValues() {
        view.setView(this.currentKey)
    }

    private fun SyntaxStyleView.setView(key: TokenSettingKey) {
        val settingsTheme = parentController.settingsTheme
        val tokenStyle = settingsTheme.tokenStyles[key]!!
        val newForeground = tokenStyle.foreground
        if (newForeground != null) {
            useForeground.isSelected = true
            foregroundColorButton.color = newForeground
        } else {
            useForeground.isSelected = false
            foregroundColorButton.color = settingsTheme.foregroundColor
        }
        val background = tokenStyle.background
        if (background != null) {
            useBackground.isSelected = true
            backgroundColorButton.color = background
        } else {
            useBackground.isSelected = false
            backgroundColorButton.color = settingsTheme.backgroundColor
        }
        isBold.isSelected = tokenStyle.isBold
        isItalic.isSelected = tokenStyle.isItalic
        isUnderline.isSelected = tokenStyle.isUnderline
    }
}
