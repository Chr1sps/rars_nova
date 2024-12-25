package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;
import rars.venus.settings.editor.controllers.EditorSettingsController;
import rars.venus.settings.editor.views.PanelWithTextAreaView;
import rars.venus.settings.editor.views.PickerCardView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class EditorSettingsDialog extends JDialog {
    public EditorSettingsDialog(
        final @NotNull Frame owner,
        final @NotNull String title,
        final boolean modality
    ) {
        super(owner, title, modality);
        final var pickerCardView = new PickerCardView();
        final var panelWithTextAreaView = new PanelWithTextAreaView(pickerCardView);
        final var treePanel = new TreePanel(pickerCardView);
        final var mainPanel = new EditorSettingsPanel(treePanel, panelWithTextAreaView);
        final var topLevelController = new EditorSettingsController(mainPanel, this);
        this.setContentPane(mainPanel);
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.pack();
        this.addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(final WindowEvent we) {
                    EditorSettingsDialog.this.setVisible(false);
                    EditorSettingsDialog.this.dispose();
                }
            });
        this.setLocationRelativeTo(owner);
    }
}
