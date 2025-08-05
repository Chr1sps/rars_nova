package rars.venus.settings.editor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import java.awt.*;

public final class TreePanel extends JScrollPane {
    public final @NotNull TreeNode fontSettingsNode, generalSchemeSettingsNode, syntaxSettingsNode, otherSettingsNode, presetsNode;

    public final @NotNull JTree tree;

    public TreePanel() {
        super();
        this.fontSettingsNode = new TreeNode.Normal("Font");
        this.generalSchemeSettingsNode = new TreeNode.Normal("General");
        this.syntaxSettingsNode = new TreeNode.Normal("RISC-V Syntax");
        this.otherSettingsNode = new TreeNode.Normal("Other settings");
        this.presetsNode = new TreeNode.Normal("Presets");
        this.tree = buildTree(fontSettingsNode, presetsNode, generalSchemeSettingsNode, syntaxSettingsNode, otherSettingsNode);
        this.setViewportView(this.tree);

        final var renderer = (DefaultTreeCellRenderer) this.tree.getCellRenderer();
        renderer.setLeafIcon(null); // Remove icons for leaf nodes
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);

        // Keep the navigation panel compact vertically
        this.setPreferredSize(new Dimension(200, 300));
    }

    private static @NotNull JTree buildTree(
        final @NotNull TreeNode fontSettingsNode,
        final @NotNull TreeNode presetsNode,
        final @NotNull TreeNode generalSchemeSettingsNode,
        final @NotNull TreeNode syntaxSettingsNode,
        final @NotNull TreeNode otherSettingsNode
    ) {
        final var newRoot = root(
            presetNode(fontSettingsNode),
            normalNode("Color scheme").children(
                presetNode(presetsNode),
                presetNode(generalSchemeSettingsNode),
                presetNode(syntaxSettingsNode)
            ),
            presetNode(otherSettingsNode)
        ).collect();
        final var tree = new JTree(newRoot);
        tree.setRootVisible(false); // Hide the root node
        tree.setShowsRootHandles(true); // Show expand/collapse icons
        return tree;
    }

    @Contract("_ -> new")
    private static @NotNull NodeBuilder normalNode(final @NotNull String name) {
        return new NodeBuilder(new TreeNode.Normal(name));
    }

    private static @NotNull NodeBuilder presetNode(final @NotNull TreeNode node) {
        return new NodeBuilder(node);
    }

    private static @NotNull NodeBuilder root(final @NotNull NodeBuilder... children) {
        return new NodeBuilder(null).children(children);
    }

    private static final class NodeBuilder {
        private final DefaultMutableTreeNode node;

        public NodeBuilder(final Object data) {
            this.node = new DefaultMutableTreeNode(data);
        }

        public NodeBuilder children(final NodeBuilder @NotNull ... children) {
            for (final var child : children) {
                this.node.add(child.collect());
            }
            return this;
        }

        public MutableTreeNode collect() {
            return node;
        }
    }
}
