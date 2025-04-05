package rars.venus.settings.editor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import rars.settings.TokenSettingKey;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import java.awt.*;

import static kotlin.collections.CollectionsKt.map;

public final class TreePanel extends JScrollPane {
    public final @NotNull TreeNode fontSettingsNode, generalSchemeSettingsNode, otherSettingsNode, presetsNode;

    public final @NotNull JTree tree;

    public TreePanel() {
        super();
        this.fontSettingsNode = new TreeNode("Font");
        this.generalSchemeSettingsNode = new TreeNode("General");
        this.otherSettingsNode = new TreeNode("Other settings");
        this.presetsNode = new TreeNode("Presets");
        this.tree = buildTree(fontSettingsNode, presetsNode, generalSchemeSettingsNode, otherSettingsNode);
        this.setViewportView(this.tree);

        final var renderer = (DefaultTreeCellRenderer) this.tree.getCellRenderer();
        renderer.setLeafIcon(null); // Remove icons for leaf nodes
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);

        this.setPreferredSize(new Dimension(200, 400));
    }

    private static @NotNull JTree buildTree(
        final @NotNull TreeNode fontSettingsNode,
        final @NotNull TreeNode presetsNode,
        final @NotNull TreeNode generalSchemeSettingsNode,
        final @NotNull TreeNode otherSettingsNode
    ) {
        final var newRoot = root(
            presetNode(fontSettingsNode),
            normalNode("Color scheme").children(
                presetNode(presetsNode),
                presetNode(generalSchemeSettingsNode),
                normalNode("RISC-V Syntax").children(
                    map(
                        TokenSettingKey.getEntries(),
                        tokenSettingKey -> syntaxNode(tokenSettingKey, tokenSettingKey.description)
                    ).toArray(NodeBuilder[]::new)
                )
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
        return new NodeBuilder(new TreeNode(name));
    }

    @Contract("_, _ -> new")
    private static @NotNull NodeBuilder syntaxNode(final @NotNull TokenSettingKey type, final @NotNull String name) {
        return new NodeBuilder(new TreeNode.Syntax(type, name));
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
