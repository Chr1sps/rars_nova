package rars.venus.settings.editor.views;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public final class PickerCardView extends JPanel {
    public final @NotNull BaseStyleView baseStyleView;
    public final @NotNull PresetsView presetsView;
    public final @NotNull SyntaxStyleView syntaxStyleView;
    public final @NotNull FontSettingsView fontSettingsView;
    public final @NotNull OtherSettingsView otherSettingsView;
    private final @NotNull JPanel upperPanel;
    private final @NotNull CardLayout upperLayout;
    
    private static final @NotNull  String EMPTY = "empty",
            FONT = "font",
            PRESETS = "presets",
            BASE = "base",
            SYNTAX = "syntax",
            OTHER = "other";

    public PickerCardView() {
        super();
        setLayout(new BorderLayout());
        upperLayout = new CardLayout();
        upperPanel = new JPanel(upperLayout);

        setPreferredSize(new Dimension(450, 450));
        setMinimumSize(new Dimension(50, 50));

        final var emptyPanel = new JPanel();
        baseStyleView = new BaseStyleView();
        presetsView = new PresetsView();
        syntaxStyleView = new SyntaxStyleView();
        fontSettingsView = new FontSettingsView();
        otherSettingsView = new OtherSettingsView();

        upperPanel.add(emptyPanel, EMPTY);
        upperPanel.add(fontSettingsView, FONT);
        upperPanel.add(presetsView, PRESETS);
        upperPanel.add(baseStyleView, BASE);
        upperPanel.add(syntaxStyleView, SYNTAX);
        upperPanel.add(otherSettingsView, OTHER);

        this.add(upperPanel, BorderLayout.NORTH);
    }

    public void showBaseStyleView() {
        upperLayout.show(upperPanel, "base");
    }

    public void showFontView() {
        upperLayout.show(upperPanel, "font");
    }

    public void showSyntaxStyleView() {
        upperLayout.show(upperPanel, "syntax");
    }

    public void showEmpty() {
        upperLayout.show(upperPanel, "empty");
    }

    public void showOtherSettings() {
        upperLayout.show(upperPanel, "other");
    }
    
    public void showPresets() {
        upperLayout.show(upperPanel, "presets");
    }
}
