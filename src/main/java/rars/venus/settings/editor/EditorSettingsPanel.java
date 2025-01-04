package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;
import rars.venus.settings.editor.views.PanelWithTextAreaView;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

public final class EditorSettingsPanel extends JPanel {

    /**
     * Custom divider UI that removes the dots from the divider.
     */
    private static final @NotNull BasicSplitPaneUI plainSplitPaneDividerUI = new BasicSplitPaneUI() {
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
                @Override
                public void paint(final Graphics g) {
                    // Remove painting of any dots by leaving this empty
                    g.setColor(splitPane.getBackground());
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
        }
    };
    public final @NotNull PanelWithTextAreaView panelWithTextAreaView;
    public final @NotNull BottomRowComponent bottomRowComponent;

    public EditorSettingsPanel(
        final @NotNull TreePanel treePanel,
        final @NotNull PanelWithTextAreaView panelWithTextAreaView
    ) {
        super(new BorderLayout());
        this.panelWithTextAreaView = panelWithTextAreaView;
        this.bottomRowComponent = new BottomRowComponent();
        this.bottomRowComponent.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.add(buildMainPart(treePanel, panelWithTextAreaView), BorderLayout.CENTER);
        this.add(bottomRowComponent, BorderLayout.SOUTH);

    }

    private static @NotNull JSplitPane buildMainPart(
        final @NotNull TreePanel treePanel,
        final @NotNull PanelWithTextAreaView panelWithTextAreaView
    ) {
        final var result = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, panelWithTextAreaView);
        result.setDividerLocation(0.5);
        result.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        result.setDividerSize(2);
        result.setUI(plainSplitPaneDividerUI);
        return result;
    }

}
