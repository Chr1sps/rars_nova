package rars.venus.settings.editor

import rars.venus.settings.editor.views.PanelWithTextAreaView
import rars.venus.util.BorderLayout
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI

class EditorSettingsPanel(
    treePanel: TreePanel,
    val panelWithTextAreaView: PanelWithTextAreaView
) : JPanel() {
    val bottomRowComponent = BottomRowComponent().apply {
        border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
    }

    init {
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        BorderLayout {
            this[BorderLayout.CENTER] = buildMainPart(treePanel, panelWithTextAreaView)
            this[BorderLayout.SOUTH] = bottomRowComponent
        }
    }
}

private fun buildMainPart(
    treePanel: TreePanel,
    panelWithTextAreaView: PanelWithTextAreaView
): JSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, panelWithTextAreaView).apply {
    setDividerLocation(0.5)
    maximumSize = Dimension(Int.Companion.MAX_VALUE, Int.Companion.MAX_VALUE)
    dividerSize = 2
    setUI(PlainSplitPaneDividerUI)
}

/**
 * Custom divider UI that removes the dots from the divider.
 */
object PlainSplitPaneDividerUI : BasicSplitPaneUI() {
    override fun createDefaultDivider(): BasicSplitPaneDivider = PlainSplitPaneDivider(this)
}

private class PlainSplitPaneDivider(ui: BasicSplitPaneUI) : BasicSplitPaneDivider(ui) {
    override fun paint(g: Graphics) {
        // Remove painting of any dots by leaving this empty
        g.color = splitPane.background
        g.fillRect(0, 0, width, height)
    }
}