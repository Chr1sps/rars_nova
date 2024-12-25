package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

public final class EditorSettingsPanel extends JSplitPane {
    public EditorSettingsPanel(final @NotNull TreePanel left, final @NotNull JComponent right) {
        super(JSplitPane.HORIZONTAL_SPLIT, left, right);
        setDividerLocation(0.5);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        setDividerSize(2);
        setUI(new PlainSplitPaneDividerUI());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Custom divider UI that removes the dots from the divider.
     */
    private static class PlainSplitPaneDividerUI extends BasicSplitPaneUI {
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
                @Override
                public void paint(Graphics g) {
                    // Remove painting of any dots by leaving this empty
                    g.setColor(splitPane.getBackground());
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
        }
    }
}
