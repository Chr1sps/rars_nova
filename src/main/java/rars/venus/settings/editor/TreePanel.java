package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;

public final class TreePanel {

    public TreePanel(final @NotNull PickerCardView pickerCardView) {
        final var tree = buildTree();

        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
        renderer.setLeafIcon(null); // Remove icons for leaf nodes
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
    }
    
    private static JTree buildTree() {
        final var newRoot = node()
                .children(
                        node(TreeNode.Font.INSTANCE),
                        node(TreeNode.Base.INSTANCE),
                        node(new TreeNode.Empty("RISC-V Syntax"))
                                .children(
                                        node(new TreeNode.Syntax(RVTokenType.WHITESPACE)),
                                        node(new TreeNode.Syntax(RVTokenType.DIRECTIVE)),
                                        node(new TreeNode.Syntax(RVTokenType.IDENTIFIER))
                                )
                )
                .build();
        final var tree = new JTree(newRoot);
        tree.setRootVisible(false); // Hide the root node
        tree.setShowsRootHandles(true); // Show expand/collapse icons
        return tree;
    }

    private static NodeBuilder node(Object data) {
        return new NodeBuilder(data);
    }

    private static NodeBuilder node() {
        return new NodeBuilder(null);
    }

    private static final class NodeBuilder {
        private final DefaultMutableTreeNode node;

        public NodeBuilder(Object data) {
            this.node = new DefaultMutableTreeNode(data);
        }

        public NodeBuilder children(NodeBuilder... children) {
            for (final var child : children) {
                this.node.add(child.build());
            }
            return this;
        }

        public MutableTreeNode build() {
            return node;
        }
    }
}
