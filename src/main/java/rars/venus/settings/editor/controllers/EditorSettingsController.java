package rars.venus.settings.editor.controllers;

import org.jetbrains.annotations.NotNull;
import rars.settings.EditorThemeSettings;
import rars.settings.FontSettings;
import rars.settings.OtherSettings;
import rars.settings.SettingsTheme;
import rars.venus.settings.editor.EditorSettingsDialog;
import rars.venus.settings.editor.EditorSettingsPanel;
import rars.venus.settings.editor.TreeNode;
import rars.venus.settings.editor.TreePanel;

import javax.swing.tree.DefaultMutableTreeNode;

public final class EditorSettingsController {
    private final @NotNull FontSettingsController fontSettingsController;
    private final @NotNull BaseStyleSettingsController baseStyleSettingsController;
    private final @NotNull SyntaxStyleSettingsController syntaxStyleSettingsController;
    private final @NotNull OtherSettingsController otherSettingsController;
    private final @NotNull EditorThemeSettings editorThemeSettings;
    public @NotNull SettingsTheme settingsTheme;

    public EditorSettingsController(
        final @NotNull EditorSettingsPanel editorSettingsView,
        final @NotNull EditorSettingsDialog dialog,
        final @NotNull TreePanel treePanel,
        final @NotNull FontSettings fontSettings,
        final @NotNull EditorThemeSettings editorThemeSettings,
        final @NotNull OtherSettings otherSettings
    ) {
        this.settingsTheme = editorThemeSettings.getCurrentTheme();
        this.editorThemeSettings = editorThemeSettings;
        final var pickerCardView = editorSettingsView.panelWithTextAreaView.pickerCardView;
        final var textArea = editorSettingsView.panelWithTextAreaView.textArea;

        this.fontSettingsController = new FontSettingsController(
            pickerCardView.fontSettingsView,
            textArea,
            fontSettings
        );
        new PresetsController(
            pickerCardView.presetsView,
            textArea,
            this
        );
        this.baseStyleSettingsController = new BaseStyleSettingsController(
            pickerCardView.baseStyleView,
            textArea,
            this
        );
        this.syntaxStyleSettingsController = new SyntaxStyleSettingsController(
            pickerCardView.syntaxStyleView,
            this,
            textArea
        );
        this.otherSettingsController = new OtherSettingsController(
            pickerCardView.otherSettingsView,
            textArea,
            otherSettings
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
                switch ((TreeNode) (selectedNode.getUserObject())) {
                    case null -> { /* ignore */ }
                    case final TreeNode.Normal node -> {
                        if (node == treePanel.fontSettingsNode) {
                            pickerCardView.showFontView();
                        } else if (node == treePanel.generalSchemeSettingsNode) {
                            pickerCardView.showBaseStyleView();
                        } else if (node == treePanel.syntaxSettingsNode) {
                            pickerCardView.showSyntaxStyleView();
                        } else if (node == treePanel.otherSettingsNode) {
                            pickerCardView.showOtherSettings();
                        } else if (node == treePanel.presetsNode) {
                            pickerCardView.showPresets();
                        } else {
                            pickerCardView.showEmpty();
                        }
                    }
                    case final TreeNode.Syntax node -> { /* deprecated: no per-token nodes in tree */ }
                }
            }
        );
    }

    private void discardSettings() {
        this.settingsTheme = this.editorThemeSettings.getCurrentTheme();
        this.fontSettingsController.resetButtonValues();
        this.baseStyleSettingsController.resetButtonValues();
        this.syntaxStyleSettingsController.resetButtonValues();
    }

    public void updateThemeControllers() {
        this.fontSettingsController.resetButtonValues();
        this.baseStyleSettingsController.resetButtonValues();
    }

    private void applySettings() {
        this.fontSettingsController.applySettings();

        this.editorThemeSettings.setCurrentTheme(this.settingsTheme);
        this.editorThemeSettings.saveSettingsToPreferences();

        this.otherSettingsController.applySettings();
    }
}
