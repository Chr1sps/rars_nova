package rars.venus.settings.editor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import rars.settings.TokenSettingKey;
import rars.venus.settings.editor.views.PickerCardView;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import java.awt.*;
import java.util.Arrays;

public final class TreePanel extends JScrollPane {
    private final TreeNode fontSettingsNode, generalSchemeSettingsNode, otherSettingsNode, presetsNode;

    public TreePanel(final @NotNull PickerCardView pickerCardView) {
        super();
        this.fontSettingsNode = new TreeNode("Font");
        this.generalSchemeSettingsNode = new TreeNode("General");
        this.otherSettingsNode = new TreeNode("Other settings");
        this.presetsNode = new TreeNode("Presets");
        final var tree = buildTree(fontSettingsNode, presetsNode, generalSchemeSettingsNode, otherSettingsNode);
        this.setViewportView(tree);

        final var renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
        renderer.setLeafIcon(null); // Remove icons for leaf nodes
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);

        tree.addTreeSelectionListener(event -> {
            final var selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (selectedNode == null) {
                return;
            }
            switch (selectedNode.getUserObject()) {
                case final TreeNode.Syntax node -> {
                    final var syntaxStylePicker = pickerCardView.syntaxStylePicker;
//                    pickerCardView.getSyntaxStylePicker().setFromTokenStyle();
                    pickerCardView.showSyntaxStylePicker();
                }
                case final TreeNode node -> {
                    if (node == fontSettingsNode) {
                        pickerCardView.showFontPicker();
                    } else if (node == generalSchemeSettingsNode) {
                        pickerCardView.showBasePicker();
                    } else if (node == otherSettingsNode) {
                        pickerCardView.showOtherSettings();
                    } else if (node == presetsNode) {
                        // TODO: Implement presets
                    } else {
                        pickerCardView.showEmpty();
                    }
                }
                default -> {
                }
            }
        });

        this.setPreferredSize(new Dimension(200, 400));
    }

    private static JTree buildTree(
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
                    Arrays.stream(TokenSettingKey.values())
                        .map(key -> syntaxNode(key, key.description))
                        .toArray(NodeBuilder[]::new)
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
