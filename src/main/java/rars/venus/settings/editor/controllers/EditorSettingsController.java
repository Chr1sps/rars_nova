package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.venus.settings.editor.EditorSettingsDialog;
import rars.venus.settings.editor.EditorSettingsPanel;

import static rars.settings.EditorThemeSettings.EDITOR_THEME_SETTINGS;

public final class EditorSettingsController {
    public final @NotNull EditorSettingsPanel editorSettingsView;

    private final @NotNull FontSettingsController fontSettingsController;
    private final @NotNull BaseStyleSettingsController baseStyleSettingsController;
    private final @NotNull SyntaxStyleSettingsController syntaxStyleSettingsController;
    private final @NotNull OtherSettingsController otherSettingsController;


    public EditorSettingsController(final @NotNull EditorSettingsPanel editorSettingsView,
                                    final @NotNull EditorSettingsDialog dialog) {
        this.editorSettingsView = editorSettingsView;
        final var pickerCardView = editorSettingsView.panelWithTextAreaView.pickerCardView;

        this.fontSettingsController = new FontSettingsController(pickerCardView.fontSettingsView,
            editorSettingsView.panelWithTextAreaView.textArea);
        this.baseStyleSettingsController = new BaseStyleSettingsController(pickerCardView.baseStylePicker,
            editorSettingsView.panelWithTextAreaView.textArea);
        this.syntaxStyleSettingsController = new SyntaxStyleSettingsController(pickerCardView.syntaxStylePicker);
        this.otherSettingsController = new OtherSettingsController(pickerCardView.otherSettingsView, this);


        this.editorSettingsView.bottomRowComponent.applyButton.addActionListener(e -> applySettings());
        this.editorSettingsView.bottomRowComponent.applyAndCloseButton.addActionListener(e -> {
            applySettings();
            dialog.setVisible(false);
            dialog.dispose();
        });
        this.editorSettingsView.bottomRowComponent.cancelButton.addActionListener(e -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
    }

    public void applySettings() {
        this.fontSettingsController.applySettings();
        this.baseStyleSettingsController.writeSettings();
        EDITOR_THEME_SETTINGS.commitChanges();
        this.otherSettingsController.applySettings();
    }
}
