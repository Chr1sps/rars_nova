package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.venus.settings.editor.EditorSettingsDialog;
import rars.venus.settings.editor.EditorSettingsPanel;

public final class EditorSettingsController {
    public final @NotNull EditorSettingsPanel editorSettingsView;

    public final @NotNull OtherSettingsController otherSettingsController;


    public EditorSettingsController(final @NotNull EditorSettingsPanel editorSettingsView,
                                    final @NotNull EditorSettingsDialog dialog) {
        this.editorSettingsView = editorSettingsView;
        final var pickerCardView = editorSettingsView.panelWithTextAreaView.pickerCardView;
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
        this.otherSettingsController.applySettings();
    }
}
