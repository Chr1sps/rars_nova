package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;
import rars.settings.SettingsTheme;

import javax.swing.*;
import java.util.ArrayList;

public final class PresetsView extends JScrollPane {
    public final @NotNull ArrayList<@NotNull PresetSection> sections;
    private final @NotNull JPanel mainPanel;

    public PresetsView() {
        super();
        this.sections = new ArrayList<>();
        this.mainPanel = new JPanel();
        this.mainPanel.setLayout(new BoxLayout(this.mainPanel, BoxLayout.Y_AXIS));
        this.setViewportView(this.mainPanel);
    }

    public void addSection(final @NotNull PresetSection section) {
        this.sections.add(section);
        this.mainPanel.add(section);
    }

    public static class PresetSection extends JPanel {
        public final @NotNull JButton button;

        public PresetSection(
            final @NotNull String themeName,
            final @NotNull SettingsTheme theme
        ) {
            super();
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            this.button = new JButton("Apply");
            this.add(new JLabel(themeName));
            this.add(Box.createHorizontalGlue());
            this.add(this.button);
        }
    }
}
