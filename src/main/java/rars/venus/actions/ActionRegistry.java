package rars.venus.actions;

import rars.settings.AllSettings;
import rars.settings.BoolSetting;
import rars.settings.BoolSettingsImpl;
import rars.settings.OtherSettingsImpl;
import rars.venus.EditFindReplaceAction;
import rars.venus.FileDumpMemoryAction;
import rars.venus.HelpHelpAction;
import rars.venus.VenusUI;
import rars.venus.run.*;
import rars.venus.settings.SettingsAction;
import rars.venus.settings.SettingsEditorAction;
import rars.venus.settings.SettingsExceptionHandlerAction;
import rars.venus.settings.SettingsMemoryConfigurationAction;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.*;

import static rars.venus.util.IconLoading.loadIcon;

/**
 * Central registry for all actions in the application.
 * This class is responsible for creating and managing actions.
 */
public class ActionRegistry {
    private final VenusUI mainUI;
    private final AllSettings allSettings;
    private final Map<String, Action> actions = new HashMap<>();
    private final Map<ActionCategory, List<Action>> actionsByCategory = new EnumMap<>(ActionCategory.class);

    /**
     * Creates a new ActionRegistry for the given UI and settings.
     *
     * @param mainUI
     *     The main UI instance
     * @param allSettings
     *     The settings instance
     */
    public ActionRegistry(final VenusUI mainUI, final AllSettings allSettings) {
        this.mainUI = mainUI;
        this.allSettings = allSettings;
        initializeActions();
    }

    /**
     * Creates a shortcut KeyStroke for the given key.
     *
     * @param key
     *     The key code
     * @return A KeyStroke for the key with the platform's menu shortcut modifier
     */
    private static KeyStroke makeShortcut(final int key) {
        return KeyStroke.getKeyStroke(key, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * Gets an action by its name.
     *
     * @param name
     *     The name of the action
     * @return The action, or null if not found
     */
    public Action getAction(final String name) {
        return actions.get(name);
    }

    /**
     * Gets all actions in a specific category.
     *
     * @param category
     *     The category of actions to get
     * @return A list of actions in the category
     */
    public List<Action> getActions(final ActionCategory category) {
        return actionsByCategory.getOrDefault(category, Collections.emptyList());
    }

    /**
     * Enables all actions in the specified categories.
     *
     * @param categories
     *     The categories to enable
     */
    public void enableActions(final ActionCategory... categories) {
        for (final ActionCategory category : categories) {
            for (final Action action : getActions(category)) {
                action.setEnabled(true);
            }
        }
    }

    /**
     * Disables all actions in the specified categories.
     *
     * @param categories
     *     The categories to disable
     */
    public void disableActions(final ActionCategory... categories) {
        for (final ActionCategory category : categories) {
            for (final Action action : getActions(category)) {
                action.setEnabled(false);
            }
        }
    }

    /**
     * Enables the specified actions.
     *
     * @param actionNames
     *     The names of the actions to enable
     */
    public void enableActions(final String... actionNames) {
        for (final String name : actionNames) {
            final Action action = getAction(name);
            if (action != null) {
                action.setEnabled(true);
            }
        }
    }

    /**
     * Disables the specified actions.
     *
     * @param actionNames
     *     The names of the actions to disable
     */
    public void disableActions(final String... actionNames) {
        for (final String name : actionNames) {
            final Action action = getAction(name);
            if (action != null) {
                action.setEnabled(false);
            }
        }
    }

    /**
     * Initializes all actions and organizes them by category.
     */
    private void initializeActions() {
        // Initialize all actions
        initializeFileActions();
        initializeEditActions();
        initializeRunActions();
        initializeSettingsActions();
        initializeHelpActions();
    }

    /**
     * Initializes file-related actions.
     */
    private void initializeFileActions() {
        final List<Action> fileActions = new ArrayList<>();

        // File -> New
        final Action fileNewAction = new GuiAction(
            "New",
            "Create a new file for editing", loadIcon("New22.png"),
            KeyEvent.VK_N, makeShortcut(KeyEvent.VK_N),
            mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                mainUI.editor.newFile();
            }
        };
        registerAction("fileNewAction", fileNewAction, ActionCategory.FILE, fileActions);

        // File -> Open
        final Action fileOpenAction = new GuiAction(
            "Open ...", "Open a file for editing", loadIcon("Open22.png"),
            KeyEvent.VK_O, makeShortcut(KeyEvent.VK_O),
            mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                mainUI.editor.openFile();
            }
        };
        registerAction("fileOpenAction", fileOpenAction, ActionCategory.FILE, fileActions);

        // File -> Close
        final Action fileCloseAction = new GuiAction(
            "Close", "Close the current file", null, KeyEvent.VK_C,
            makeShortcut(KeyEvent.VK_W),
            mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                mainUI.editor.close();
            }
        };
        registerAction("fileCloseAction", fileCloseAction, ActionCategory.FILE, fileActions);

        // File -> Close All
        final Action fileCloseAllAction = new GuiAction(
            "Close All", "Close all open files", null,
            KeyEvent.VK_L, null,
            mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                mainUI.editor.closeAll();
            }
        };
        registerAction("fileCloseAllAction", fileCloseAllAction, ActionCategory.FILE, fileActions);

        // File -> Save
        final Action fileSaveAction = new GuiAction(
            "Save", "Save the current file", loadIcon("Save22.png"),
            KeyEvent.VK_S, makeShortcut(KeyEvent.VK_S), mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                mainUI.editor.save();
            }
        };
        registerAction("fileSaveAction", fileSaveAction, ActionCategory.FILE, fileActions);

        // File -> Save As
        final Action fileSaveAsAction = new GuiAction(
            "Save as ...", "Save current file with different name", loadIcon("SaveAs22.png"),
            KeyEvent.VK_A, null, mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                mainUI.editor.saveAs();
            }
        };
        registerAction("fileSaveAsAction", fileSaveAsAction, ActionCategory.FILE, fileActions);

        // File -> Save All
        final Action fileSaveAllAction = new GuiAction(
            "Save All", "Save all open files", null,
            KeyEvent.VK_V, null, mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                mainUI.editor.saveAll();
            }
        };
        registerAction("fileSaveAllAction", fileSaveAllAction, ActionCategory.FILE, fileActions);

        // File -> Dump Memory
        final Action fileDumpMemoryAction = new FileDumpMemoryAction(
            "Dump Memory ...", loadIcon("Dump22.png"),
            "Dump machine code or data in an available format", KeyEvent.VK_D,
            makeShortcut(KeyEvent.VK_D),
            mainUI
        );
        registerAction("fileDumpMemoryAction", fileDumpMemoryAction, ActionCategory.FILE, fileActions);

        // File -> Exit
        final Action fileExitAction = new GuiAction("Exit", "Exit Rars", null, KeyEvent.VK_X, null, mainUI) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                if (mainUI.editor.closeAll()) {
                    System.exit(0);
                }
            }
        };
        registerAction("fileExitAction", fileExitAction, ActionCategory.FILE, fileActions);

        actionsByCategory.put(ActionCategory.FILE, fileActions);
    }

    /**
     * Initializes edit-related actions.
     */
    private void initializeEditActions() {
        final List<Action> editActions = new ArrayList<>();

        // Edit -> Undo
        final Action editUndoAction = new GuiAction(
            "Undo", "Undo last edit", loadIcon("Undo22.png"),
            KeyEvent.VK_U, makeShortcut(KeyEvent.VK_Z), mainUI
        ) {
            {
                this.setEnabled(false);
            }

            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                final var editPane = mainUI.mainPane.getCurrentEditTabPane();
                if (editPane != null) {
                    editPane.undo();
                    // Note: VenusUI will update undo/redo state when needed
                }
            }
        };
        registerAction("editUndoAction", editUndoAction, ActionCategory.EDIT, editActions);

        // Edit -> Redo
        final Action editRedoAction = new GuiAction(
            "Redo", "Redo last edit", loadIcon("Redo22.png"),
            KeyEvent.VK_R, makeShortcut(KeyEvent.VK_Y), mainUI
        ) {
            {
                this.setEnabled(false);
            }

            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                final var editPane = mainUI.mainPane.getCurrentEditTabPane();
                if (editPane != null) {
                    editPane.redo();
                    // Note: VenusUI will update undo/redo state when needed
                }
            }
        };
        registerAction("editRedoAction", editRedoAction, ActionCategory.EDIT, editActions);

        // Edit -> Cut
        final Action editCutAction = new GuiAction(
            "Cut", "Cut", loadIcon("Cut22.gif"), KeyEvent.VK_C,
            makeShortcut(KeyEvent.VK_X), mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                mainUI.mainPane.getCurrentEditTabPane().cutText();
            }
        };
        registerAction("editCutAction", editCutAction, ActionCategory.EDIT, editActions);

        // Edit -> Copy
        final Action editCopyAction = new GuiAction(
            "Copy", "Copy", loadIcon("Copy22.png"), KeyEvent.VK_O,
            makeShortcut(KeyEvent.VK_C), mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                mainUI.mainPane.getCurrentEditTabPane().copyText();
            }
        };
        registerAction("editCopyAction", editCopyAction, ActionCategory.EDIT, editActions);

        // Edit -> Paste
        final Action editPasteAction = new GuiAction(
            "Paste", "Paste", loadIcon("Paste22.png"), KeyEvent.VK_P,
            makeShortcut(KeyEvent.VK_V), mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                mainUI.mainPane.getCurrentEditTabPane().pasteText();
            }
        };
        registerAction("editPasteAction", editPasteAction, ActionCategory.EDIT, editActions);

        // Edit -> Find/Replace
        final Action editFindReplaceAction = new EditFindReplaceAction(
            "Find/Replace", loadIcon("Find22.png"),
            "Find/Replace", KeyEvent.VK_F, makeShortcut(KeyEvent.VK_F), mainUI
        );
        registerAction("editFindReplaceAction", editFindReplaceAction, ActionCategory.EDIT, editActions);

        actionsByCategory.put(ActionCategory.EDIT, editActions);
    }

    /**
     * Initializes run-related actions.
     */
    private void initializeRunActions() {
        final List<Action> runActions = new ArrayList<>();

        // Run -> Assemble
        final Action runAssembleAction = new RunAssembleAction(
            "Assemble", loadIcon("Assemble22.png"),
            "Assemble the current file and clear breakpoints", KeyEvent.VK_A,
            KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), mainUI
        );
        registerAction("runAssembleAction", runAssembleAction, ActionCategory.RUN, runActions);

        // Run -> Go
        final Action runGoAction = new RunGoAction(
            "Go", loadIcon("Play22.png"), "Run the current program",
            KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), mainUI
        );
        registerAction("runGoAction", runGoAction, ActionCategory.RUN, runActions);

        // Run -> Step
        final Action runStepAction = new RunStepAction(
            "Step", loadIcon("StepForward22.png"),
            "Run one step at a time", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), mainUI
        );
        registerAction("runStepAction", runStepAction, ActionCategory.RUN, runActions);

        // Run -> Backstep
        final Action runBackstepAction = new RunBackstepAction(
            "Backstep", loadIcon("StepBack22.png"),
            "Undo the last step", KeyEvent.VK_B, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), mainUI
        );
        registerAction("runBackstepAction", runBackstepAction, ActionCategory.RUN, runActions);

        // Run -> Pause
        final Action runPauseAction = new GuiAction(
            "Pause", "Pause the currently running program", loadIcon("Pause22.png"),
            KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0),
            mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                rars.Globals.SIMULATOR.pauseExecution();
                // RunGoAction's "paused" method will do the cleanup.
            }
        };
        registerAction("runPauseAction", runPauseAction, ActionCategory.RUN, runActions);

        // Run -> Stop
        final Action runStopAction = new GuiAction(
            "Stop", "Stop the currently running program", loadIcon("Stop22.png"),
            KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),
            mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                rars.Globals.SIMULATOR.stopExecution();
                // RunGoAction's "stopped" method will take care of the cleanup.
            }
        };
        registerAction("runStopAction", runStopAction, ActionCategory.RUN, runActions);

        // Run -> Reset
        final Action runResetAction = new RunResetAction(
            "Reset", loadIcon("Reset22.png"), "Reset memory and registers",
            KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), mainUI
        );
        registerAction("runResetAction", runResetAction, ActionCategory.RUN, runActions);

        // Run -> Clear Breakpoints
        final Action runClearBreakpointsAction = new RunClearBreakpointsAction(
            KeyEvent.VK_K,
            makeShortcut(KeyEvent.VK_K), mainUI
        );
        registerAction("runClearBreakpointsAction", runClearBreakpointsAction, ActionCategory.RUN, runActions);

        // Run -> Toggle Breakpoints
        final Action runToggleBreakpointsAction = new GuiAction(
            "Toggle all breakpoints",
            "Disable/enable all breakpoints without clearing (can also click Bkpt column header)",
            null,
            KeyEvent.VK_T, makeShortcut(KeyEvent.VK_T), mainUI
        ) {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                mainUI.mainPane.executePane.getTextSegment().toggleBreakpoints();
            }
        };
        registerAction("runToggleBreakpointsAction", runToggleBreakpointsAction, ActionCategory.RUN, runActions);

        actionsByCategory.put(ActionCategory.RUN, runActions);
    }

    /**
     * Initializes settings-related actions.
     */
    private void initializeSettingsActions() {
        final List<Action> settingsActions = new ArrayList<>();
        final BoolSettingsImpl boolSettings = allSettings.boolSettings;
        final OtherSettingsImpl otherSettings = allSettings.otherSettings;

        // Settings -> Label Window
        final Action settingsLabelAction = new SettingsAction(
            "Show Labels Window (symbol table)",
            "Toggle visibility of Labels window (symbol table) in the Execute tab",
            boolSettings,
            BoolSetting.LABEL_WINDOW_VISIBILITY,
            mainUI,
            (value) -> mainUI.mainPane.executePane.setLabelWindowVisibility(value)
        );
        registerAction("settingsLabelAction", settingsLabelAction, ActionCategory.SETTINGS, settingsActions);

        // Settings -> Dark Mode
        final Action settingsDarkModeAction = new SettingsAction(
            "Dark mode", "Toggle between light and dark mode",
            boolSettings,
            BoolSetting.DARK_MODE, mainUI, (value) -> {
            // Note: We can't call setDarkModeState directly because it's private
            // The UI will handle the dark mode change through other means
        }
        );
        registerAction("settingsDarkModeAction", settingsDarkModeAction, ActionCategory.SETTINGS, settingsActions);

        // Settings -> Popup Input
        final Action settingsPopupInputAction = new SettingsAction(
            "Popup dialog for input syscalls (5,6,7,8,12)",
            "If set, use popup dialog for input syscalls (5,6,7,8,12) instead of cursor in Run I/O window",
            boolSettings,
            BoolSetting.POPUP_SYSCALL_INPUT, mainUI
        );
        registerAction("settingsPopupInputAction", settingsPopupInputAction, ActionCategory.SETTINGS, settingsActions);

        // Settings -> Value Display Base
        final Action settingsValueDisplayBaseAction = new SettingsAction(
            "Values displayed in hexadecimal",
            "Toggle between hexadecimal and decimal display of memory/register values",
            boolSettings,
            BoolSetting.DISPLAY_VALUES_IN_HEX, mainUI,
            mainUI.mainPane.executePane.getValueDisplayBaseChooser()::setSelected
        );
        registerAction(
            "settingsValueDisplayBaseAction",
            settingsValueDisplayBaseAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        // Settings -> Address Display Base
        final Action settingsAddressDisplayBaseAction = new SettingsAction(
            "Addresses displayed in hexadecimal",
            "Toggle between hexadecimal and decimal display of memory addresses",
            boolSettings,
            BoolSetting.DISPLAY_ADDRESSES_IN_HEX,
            mainUI,
            mainUI.mainPane.executePane.getAddressDisplayBaseChooser()::setSelected
        );
        registerAction(
            "settingsAddressDisplayBaseAction",
            settingsAddressDisplayBaseAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        // Settings -> Extended
        final Action settingsExtendedAction = new SettingsAction(
            "Permit extended (usePseudoInstructions) instructions and formats",
            "If set, extended (usePseudoInstructions) instructions are formats are permitted.",
            boolSettings,
            BoolSetting.EXTENDED_ASSEMBLER_ENABLED, mainUI
        );
        registerAction("settingsExtendedAction", settingsExtendedAction, ActionCategory.SETTINGS, settingsActions);

        // Settings -> Assemble On Open
        final Action settingsAssembleOnOpenAction = new SettingsAction(
            "Assemble file upon opening",
            "If set, a file will be automatically assembled as soon as it is opened. File Open dialog will show most recently opened file.",
            boolSettings,
            BoolSetting.ASSEMBLE_ON_OPEN,
            mainUI
        );
        registerAction(
            "settingsAssembleOnOpenAction",
            settingsAssembleOnOpenAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        // Settings -> Assemble All
        final Action settingsAssembleAllAction = new SettingsAction(
            "Assemble all files in directory",
            "If set, all files in current directory will be assembled when Assemble operation is selected.",
            boolSettings,
            BoolSetting.ASSEMBLE_ALL, mainUI
        );
        registerAction(
            "settingsAssembleAllAction",
            settingsAssembleAllAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        // Settings -> Assemble Open
        final Action settingsAssembleOpenAction = new SettingsAction(
            "Assemble all files currently open",
            "If set, all files currently open for editing will be assembled when Assemble operation is selected.",
            boolSettings,
            BoolSetting.ASSEMBLE_OPEN, mainUI
        );
        registerAction(
            "settingsAssembleOpenAction",
            settingsAssembleOpenAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        // Settings -> Warnings Are Errors
        final Action settingsWarningsAreErrorsAction = new SettingsAction(
            "Assembler warnings are considered errors",
            "If set, assembler warnings will be interpreted as errors and prevent successful assembly.",
            boolSettings,
            BoolSetting.WARNINGS_ARE_ERRORS, mainUI
        );
        registerAction(
            "settingsWarningsAreErrorsAction",
            settingsWarningsAreErrorsAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        // Settings -> Start At Main
        final Action settingsStartAtMainAction = new SettingsAction(
            "Initialize Program Counter to global 'main' if defined",
            "If set, assembler will initialize Program Counter to text address globally labeled 'main', if defined.",
            boolSettings,
            BoolSetting.START_AT_MAIN, mainUI
        );
        registerAction(
            "settingsStartAtMainAction",
            settingsStartAtMainAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        // Settings -> Program Arguments
        final Action settingsProgramArgumentsAction = new SettingsAction(
            "Program arguments provided to program",
            "If set, program arguments for the program can be entered in border of Text Segment window.",
            boolSettings,
            BoolSetting.PROGRAM_ARGUMENTS, mainUI, (selected) -> {
            if (selected) {
                mainUI.mainPane.executePane.getTextSegment().addProgramArgumentsPanel();
            } else {
                mainUI.mainPane.executePane.getTextSegment().removeProgramArgumentsPanel();
            }
        }
        );
        registerAction(
            "settingsProgramArgumentsAction",
            settingsProgramArgumentsAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        // Settings -> Self-Modifying Code
        final Action settingsSelfModifyingCodeAction = new SettingsAction(
            "Self-modifying code",
            "If set, the program can write and branch to both text and data segments.",
            boolSettings,
            BoolSetting.SELF_MODIFYING_CODE_ENABLED, mainUI
        );
        registerAction(
            "settingsSelfModifyingCodeAction",
            settingsSelfModifyingCodeAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        // Settings -> RV64
        final Action settingsRV64Action = new SettingsAction(
            "64 bit",
            "If set, registers are 64 bits wide and new instructions are available",
            boolSettings,
            BoolSetting.RV64_ENABLED, mainUI, (isRV64) -> {
            rars.riscv.InstructionsRegistry.RV64_MODE_FLAG = isRV64;
            mainUI.registersTab.updateRegisters();
            mainUI.floatingPointTab.updateRegisters();
            mainUI.csrTab.updateRegisters();
        }
        );
        registerAction("settingsRV64Action", settingsRV64Action, ActionCategory.SETTINGS, settingsActions);

        // Settings -> Derive Current Working Directory
        final Action settingsDeriveCurrentWorkingDirectoryAction = new SettingsAction(
            "Derive current working directory",
            "If set, the working directory is derived from the main file instead of the RARS executable directory.",
            boolSettings,
            BoolSetting.DERIVE_CURRENT_WORKING_DIRECTORY, mainUI
        );
        registerAction(
            "settingsDeriveCurrentWorkingDirectoryAction",
            settingsDeriveCurrentWorkingDirectoryAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        // Settings -> Editor
        final Action settingsEditorAction = new SettingsEditorAction(
            mainUI,
            allSettings
        );
        registerAction("settingsEditorAction", settingsEditorAction, ActionCategory.SETTINGS, settingsActions);

        // Settings -> Exception Handler
        final Action settingsExceptionHandlerAction = new SettingsExceptionHandlerAction(
            "Exception Handler...",
            "If set, the specified exception handler file will be included in all Assemble operations.",
            mainUI,
            boolSettings,
            otherSettings
        );
        registerAction(
            "settingsExceptionHandlerAction",
            settingsExceptionHandlerAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        // Settings -> Memory Configuration
        final Action settingsMemoryConfigurationAction = new SettingsMemoryConfigurationAction(
            "Memory Configuration...",
            null, "View and modify memory segment base addresses for the simulated processor",
            null, null, mainUI
        );
        registerAction(
            "settingsMemoryConfigurationAction",
            settingsMemoryConfigurationAction,
            ActionCategory.SETTINGS,
            settingsActions
        );

        actionsByCategory.put(ActionCategory.SETTINGS, settingsActions);
    }

    /**
     * Initializes help-related actions.
     */
    private void initializeHelpActions() {
        final List<Action> helpActions = new ArrayList<>();

        // Help -> Help
        final Action helpHelpAction = new HelpHelpAction(
            mainUI,
            allSettings.fontSettings
        );
        registerAction("helpHelpAction", helpHelpAction, ActionCategory.HELP, helpActions);

        // Help -> About
        final Action helpAboutAction = new HelpAboutAction(mainUI);
        registerAction("helpAboutAction", helpAboutAction, ActionCategory.HELP, helpActions);

        actionsByCategory.put(ActionCategory.HELP, helpActions);
    }

    /**
     * Registers an action with the registry.
     *
     * @param name
     *     The name to register the action under
     * @param action
     *     The action to register
     * @param category
     *     The category of the action
     * @param categoryList
     *     The list to add the action to
     */
    private void registerAction(
        final String name,
        final Action action,
        final ActionCategory category,
        final List<Action> categoryList
    ) {
        actions.put(name, action);
        categoryList.add(action);
    }

    /**
     * Categories of actions for organization and bulk operations.
     */
    public enum ActionCategory {
        FILE,
        EDIT,
        RUN,
        SETTINGS,
        HELP
    }
}
