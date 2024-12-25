package rars.venus.settings.editor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import java.awt.*;

public final class TreePanel extends JScrollPane {
    private final TreeNode fontSettingsNode, generalSchemeSettingsNode, otherSettingsNode;

    public TreePanel(final @NotNull PickerCardView pickerCardView) {
        super();
        this.fontSettingsNode = new TreeNode("Font");
        this.generalSchemeSettingsNode = new TreeNode("General");
        this.otherSettingsNode = new TreeNode("Other settings");
        final var tree = buildTree(fontSettingsNode, generalSchemeSettingsNode, otherSettingsNode);
        this.setViewportView(tree);

        final var renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
        renderer.setLeafIcon(null); // Remove icons for leaf nodes
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);

        tree.addTreeSelectionListener(event -> {
//            event.getOldLeadSelectionPath().getLastPathComponent();
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
        final @NotNull TreeNode generalSchemeSettingsNode,
        final @NotNull TreeNode otherSettingsNode
    ) {
        final var newRoot = root(
            presetNode(fontSettingsNode),
            normalNode("Color scheme").children(
                presetNode(generalSchemeSettingsNode),
                normalNode("RISC-V Syntax").children(
                    syntaxNode(TokenSettingKey.ERROR, "Errors"),
                    syntaxNode(TokenSettingKey.COMMENT, "Comments"),
                    syntaxNode(TokenSettingKey.DIRECTIVE, "Directives"),
                    syntaxNode(TokenSettingKey.REGISTER_NAME, "Registers"),
                    syntaxNode(TokenSettingKey.IDENTIFIER, "Identifiers"),
                    syntaxNode(TokenSettingKey.NUMBER, "Numbers"),
                    syntaxNode(TokenSettingKey.STRING, "Strings"),
                    syntaxNode(TokenSettingKey.LABEL, "Labels"),
                    syntaxNode(TokenSettingKey.INSTRUCTION, "Instructions"),
                    syntaxNode(TokenSettingKey.PUNCTUATION, "Punctuation"),
                    syntaxNode(TokenSettingKey.ROUNDING_MODE, "Rounding modes"),
                    syntaxNode(TokenSettingKey.MACRO_PARAMETER, "Macro parameters"),
                    syntaxNode(TokenSettingKey.HILO, "%hi/%lo offsets")
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
