package rars.venus.settings.editor

import rars.settings.TokenSettingKey
import java.awt.Dimension
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeNode

class TreePanel : JScrollPane() {
    val fontSettingsNode: TreeNodeData = TreeNodeData("Font")
    val generalSchemeSettingsNode: TreeNodeData = TreeNodeData("General")
    val otherSettingsNode: TreeNodeData = TreeNodeData("Other settings")
    val presetsNode: TreeNodeData = TreeNodeData("Presets")

    val tree = buildTree(
        fontSettingsNode,
        presetsNode,
        generalSchemeSettingsNode,
        otherSettingsNode
    )

    init {
        setViewportView(tree)

        (tree.cellRenderer as DefaultTreeCellRenderer).apply {
            leafIcon = null
            closedIcon = null
            openIcon = null
        }

        preferredSize = Dimension(200, 400)
    }
}

private fun buildTree(
    fontSettingsNode: TreeNodeData,
    presetsNode: TreeNodeData,
    generalSchemeSettingsNode: TreeNodeData,
    otherSettingsNode: TreeNodeData
): JTree {
    val newRoot = buildRoot {
        presetNode(fontSettingsNode)
        normalNode("Color scheme") {
            presetNode(presetsNode)
            presetNode(generalSchemeSettingsNode)
            normalNode("RISC-V Syntax") {
                TokenSettingKey.entries.forEach { key ->
                    syntaxNode(key, key.description)
                }
            }
        }
        presetNode(otherSettingsNode)
    }

    return JTree(newRoot).apply {
        isRootVisible = false
        showsRootHandles = true
    }
}

private fun buildRoot(builder: DefaultMutableTreeNode.() -> Unit): TreeNode =
    DefaultMutableTreeNode(null).apply(builder)

private fun DefaultMutableTreeNode.normalNode(
    name: String,
    builderFunc: DefaultMutableTreeNode.() -> Unit
) {
    DefaultMutableTreeNode(TreeNodeData(name)).apply(builderFunc)
        .also { add(it) }
}

private fun DefaultMutableTreeNode.presetNode(node: TreeNodeData) {
    DefaultMutableTreeNode(node).also { add(it) }
}

private fun DefaultMutableTreeNode.syntaxNode(
    type: TokenSettingKey,
    name: String
) {
    DefaultMutableTreeNode(TreeNodeData.Syntax(type, name)).also { add(it) }
}

