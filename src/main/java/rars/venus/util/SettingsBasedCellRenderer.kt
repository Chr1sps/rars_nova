package rars.venus.util

import rars.settings.AllSettings
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

open class SettingsBasedCellRenderer(
    protected val settings: AllSettings,
    horizontalAlignment: Int,
    protected val table: JTable,
) : DefaultTableCellRenderer() {
    init {
        this.horizontalAlignment = horizontalAlignment
        settings.fontSettings.onChangeListenerHook.subscribe { table.repaint() }
        settings.editorThemeSettings.onChangeListenerHook.subscribe { table.repaint() }
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val cell = super.getTableCellRendererComponent(
            table,
            value,
            isSelected,
            hasFocus,
            row,
            column
        ) as DefaultTableCellRenderer
        return cell.apply {
            font = settings.fontSettings.currentFont
            horizontalAlignment =
                this@SettingsBasedCellRenderer.horizontalAlignment
            val theme = settings.editorThemeSettings.currentTheme
            background = theme.backgroundColor
            foreground = theme.foregroundColor
        }
    }
}
