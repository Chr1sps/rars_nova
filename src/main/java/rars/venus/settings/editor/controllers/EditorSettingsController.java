package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.settings.SettingsTheme;
import rars.venus.settings.editor.EditorSettingsDialog;
import rars.venus.settings.editor.EditorSettingsPanel;
import rars.venus.settings.editor.TreeNode;
import rars.venus.settings.editor.TreePanel;

import javax.swing.tree.DefaultMutableTreeNode;

import static rars.settings.EditorThemeSettings.EDITOR_THEME_SETTINGS;

public final class EditorSettingsController {
    public final @NotNull EditorSettingsPanel editorSettingsView;
    private final @NotNull FontSettingsController fontSettingsController;
    private final @NotNull BaseStyleSettingsController baseStyleSettingsController;
    private final @NotNull SyntaxStyleSettingsController syntaxStyleSettingsController;
    private final @NotNull OtherSettingsController otherSettingsController;
    private @NotNull SettingsTheme settingsTheme;


    public EditorSettingsController(
        final @NotNull EditorSettingsPanel editorSettingsView,
        final @NotNull EditorSettingsDialog dialog,
        final @NotNull TreePanel treePanel
    ) {
        this.editorSettingsView = editorSettingsView;
        this.settingsTheme = EDITOR_THEME_SETTINGS.currentTheme.clone();
        final var pickerCardView = editorSettingsView.panelWithTextAreaView.pickerCardView;
        final var textArea = editorSettingsView.panelWithTextAreaView.textArea;

        this.fontSettingsController = new FontSettingsController(
            pickerCardView.fontSettingsView,
            textArea
        );
        this.baseStyleSettingsController = new BaseStyleSettingsController(
            pickerCardView.baseStylePicker,
            textArea,
            this.settingsTheme
        );
        this.syntaxStyleSettingsController = new SyntaxStyleSettingsController(
            pickerCardView.syntaxStylePicker,
            this.settingsTheme,
            textArea
        );
        this.otherSettingsController = new OtherSettingsController(
            pickerCardView.otherSettingsView,
            textArea
        );
        final var bottomRow = editorSettingsView.bottomRowComponent;
        bottomRow.applyButton.addActionListener(e -> applySettings());
        bottomRow.applyAndCloseButton.addActionListener(e -> {
            applySettings();
            dialog.setVisible(false);
            dialog.dispose();
        });
        bottomRow.cancelButton.addActionListener(e -> {
            discardSettings();
            dialog.setVisible(false);
            dialog.dispose();
        });
        treePanel.tree.addTreeSelectionListener(
            event -> {
                final var selectedNode = (DefaultMutableTreeNode) treePanel.tree.getLastSelectedPathComponent();
                if (selectedNode == null) {
                    return;
                }
                switch (selectedNode.getUserObject()) {
                    case final TreeNode.Syntax node -> {
                        syntaxStyleSettingsController.setCurrentKey(node.type);
                        pickerCardView.showSyntaxStylePicker();
                    }
                    case final TreeNode node -> {
                        if (node == treePanel.fontSettingsNode) {
                            pickerCardView.showFontPicker();
                        } else if (node == treePanel.generalSchemeSettingsNode) {
                            pickerCardView.showBasePicker();
                        } else if (node == treePanel.otherSettingsNode) {
                            pickerCardView.showOtherSettings();
                        } else if (node == treePanel.presetsNode) {
                            // TODO: Implement presets
                            pickerCardView.showEmpty();
                        } else {
                            pickerCardView.showEmpty();
                        }
                    }
                    default -> {
                    }
                }
            }
        );
    }

    private void discardSettings() {
        this.settingsTheme = EDITOR_THEME_SETTINGS.currentTheme.clone();
        this.fontSettingsController.resetButtonValues();
        this.baseStyleSettingsController.resetButtonValues();
        this.syntaxStyleSettingsController.resetButtonValues();
    }

    private void applySettings() {
        this.fontSettingsController.applySettings();

        EDITOR_THEME_SETTINGS.currentTheme = this.settingsTheme;
        EDITOR_THEME_SETTINGS.commitChanges();

        this.otherSettingsController.applySettings();
    }
}
