package rars.venus

import rars.settings.EditorThemeSettings
import rars.settings.FontSettings
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

/**
 * Use this to render Monospaced and right-aligned data in JTables.
 * I am using it to render integer addresses and values that are stored as
 * Strings containing either the decimal or hexidecimal version
 * of the integer value.
 */
class MonoRightCellRenderer(
    private val fontSettings: FontSettings,
    private val editorThemeSettings: EditorThemeSettings
) : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val theme = editorThemeSettings.currentTheme
        return (super.getTableCellRendererComponent(
            table, value,
            isSelected, hasFocus, row, column
        ) as JLabel).apply {
            setFont(fontSettings.currentFont)
            setHorizontalAlignment(RIGHT)
            setForeground(theme.foregroundColor)
            setBackground(theme.backgroundColor)
        }
    }
}
