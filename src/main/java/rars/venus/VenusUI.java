package rars.venus;

// import com.formdev.flatlaf.extras.FlatSVGIcon;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.io.VenusIO;
import rars.riscv.InstructionsRegistry;
import rars.settings.BoolSetting;
import rars.settings.OtherSettings;
import rars.venus.registers.ControlAndStatusWindow;
import rars.venus.registers.FloatingPointWindow;
import rars.venus.registers.RegistersPane;
import rars.venus.registers.RegistersWindow;
import rars.venus.run.*;
import rars.venus.settings.SettingsAction;
import rars.venus.settings.SettingsEditorAction;
import rars.venus.settings.SettingsExceptionHandlerAction;
import rars.venus.settings.SettingsMemoryConfigurationAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static rars.Globals.BOOL_SETTINGS;


/*
Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Top level container for Venus GUI.
 *
 * @author Sanderson and Team JSpim
 */

/*
 * Heavily modified by Pete Sanderson, July 2004, to incorporate JSPIMMenu and
 * JSPIMToolbar
 * not as subclasses of JMenuBar and JToolBar, but as instances of them. They
 * are both
 * here primarily so both can share the Action objects.
 */
public final class VenusUI extends JFrame {
    private static final Logger LOGGER = LogManager.getLogger(VenusUI.class);
    public final @NotNull JMenuBar menu;
    public final @NotNull MainPane mainPane;
    public final @NotNull RegistersPane registersPane;
    public final @NotNull RegistersWindow registersTab;
    public final @NotNull FloatingPointWindow floatingPointTab;
    public final @NotNull ControlAndStatusWindow csrTab;
    public final @NotNull MessagesPane messagesPane;
    public final @NotNull Editor editor;
    public final @NotNull RunSpeedPanel runSpeedPanel;
    public final @NotNull VenusIO venusIO;
    private final @NotNull Action fileNewAction, fileOpenAction, fileCloseAction, fileCloseAllAction, fileSaveAction;
    private final @NotNull Action fileSaveAsAction, fileSaveAllAction, fileDumpMemoryAction, fileExitAction;
    // The "action" objects, which include action listeners. One of each will be
    // created then
    // shared between a menu item and its corresponding toolbar button. This is a
    // very cool
    // technique because it relates the button and menu item so closely
    private final @NotNull Action editUndoAction;
    private final @NotNull Action editRedoAction;
    private final @NotNull Action editCutAction, editCopyAction, editPasteAction, editFindReplaceAction;
    private final @NotNull Action runAssembleAction, runGoAction, runStepAction, runBackstepAction, runResetAction,
        runStopAction, runPauseAction, runClearBreakpointsAction, runToggleBreakpointsAction;
    private final @NotNull Action settingsLabelAction, settingsDarkModeAction, settingsPopupInputAction,
        settingsValueDisplayBaseAction,
        settingsAddressDisplayBaseAction,
        settingsExtendedAction, settingsAssembleOnOpenAction, settingsAssembleOpenAction, settingsAssembleAllAction,
        settingsWarningsAreErrorsAction, settingsStartAtMainAction, settingsProgramArgumentsAction,
        settingsExceptionHandlerAction, settingsEditorAction, settingsMemoryConfigurationAction,
        settingsSelfModifyingCodeAction, settingsRV64Action, settingsDeriveCurrentWorkingDirectoryAction;
    private final @NotNull Action helpHelpAction, helpAboutAction;
    /// registers/memory reset for execution
    public boolean isMemoryReset = true;
    public boolean isExecutionStarted = false;

    /**
     * Constructor for the Class. Sets up a window object for the UI
     *
     * @param name
     *     Name of the window to be created.
     * @param files
     *     Files to open
     */
    public VenusUI(final @NotNull String name, final @NotNull List<? extends @NotNull File> files) {
        super(name);
        setDarkModeState(BOOL_SETTINGS.getSetting(BoolSetting.DARK_MODE));
        this.editor = new Editor(this);

        final double screenWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        final double screenHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        // basically give up some screen space if running at 800 x 600
        final double messageWidthPct = (screenWidth < 1000.0) ? 0.67 : 0.73;
        final double messageHeightPct = (screenWidth < 1000.0) ? 0.12 : 0.15;
        final double mainWidthPct = (screenWidth < 1000.0) ? 0.67 : 0.73;
        final double mainHeightPct = (screenWidth < 1000.0) ? 0.60 : 0.65;
        final double registersWidthPct = (screenWidth < 1000.0) ? 0.18 : 0.22;
        final double registersHeightPct = (screenWidth < 1000.0) ? 0.72 : 0.80;

        final Dimension messagesPanePreferredSize = new Dimension(
            (int) (screenWidth * messageWidthPct),
            (int) (screenHeight * messageHeightPct)
        );
        final Dimension mainPanePreferredSize = new Dimension(
            (int) (screenWidth * mainWidthPct),
            (int) (screenHeight * mainHeightPct)
        );
        final Dimension registersPanePreferredSize = new Dimension(
            (int) (screenWidth * registersWidthPct),
            (int) (screenHeight * registersHeightPct)
        );

        final var imageUrl = this.getClass().getResource(Globals.imagesPath + "RISC-V.png");
        if (imageUrl == null) {
            VenusUI.LOGGER.fatal("Internal Error: images folder or file not found.");
            System.exit(0);
        }
        final var rvImage = Toolkit.getDefaultToolkit().getImage(imageUrl);
        this.setIconImage(rvImage);
        // Everything in frame will be arranged on JPanel "center", which is only frame
        // component.
        // "center" has BorderLayout and 2 major components:
        // -- panel (jp) on North with 2 components
        // 1. toolbar
        // 2. run speed slider.
        // -- split pane (horizonSplitter) in center with 2 components side-by-side
        // 1. split pane (splitter) with 2 components stacked
        // a. main pane, with 2 tabs (edit, execute)
        // b. messages pane with 2 tabs (rars, run I/O)
        // 2. registers pane with 3 tabs (register file, coproc 0, coproc 1)
        // I should probably run this breakdown out to full detail. The components are
        // created
        // roughly in bottom-up order; some are created in component constructors and
        // thus are
        // not visible here.
        this.registersTab = new RegistersWindow(Globals.REGISTER_FILE, this, Globals.ALL_SETTINGS);
        this.floatingPointTab = new FloatingPointWindow(Globals.FP_REGISTER_FILE, this, Globals.ALL_SETTINGS);
        this.csrTab = new ControlAndStatusWindow(Globals.CS_REGISTER_FILE, this, Globals.ALL_SETTINGS);
        this.registersPane = new RegistersPane(this.registersTab, this.floatingPointTab, this.csrTab);
        this.registersPane.setPreferredSize(registersPanePreferredSize);

        this.mainPane = new MainPane(this, this.editor, this.registersTab, this.floatingPointTab, this.csrTab);

        this.mainPane.setPreferredSize(mainPanePreferredSize);
        this.messagesPane = new MessagesPane(this);
        this.messagesPane.setPreferredSize(messagesPanePreferredSize);
        final JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.mainPane, this.messagesPane);
        splitter.setOneTouchExpandable(true);
        splitter.resetToPreferredSizes();
        final var horizontalLayout = new JPanel(new BorderLayout());
        horizontalLayout.add(splitter, BorderLayout.CENTER);
        horizontalLayout.add(this.registersPane, BorderLayout.EAST);

        // due to dependencies, do not set up menu/toolbar until now.

        // region Action objects

        this.fileNewAction = new GuiAction(
            "New",
            VenusUI.loadIcon("New22.png"),
            "Create a new file for editing", KeyEvent.VK_N, VenusUI.makeShortcut(KeyEvent.VK_N),
            VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VenusUI.this.editor.newFile();
            }
        };
        this.fileOpenAction = new GuiAction(
            "Open ...", VenusUI.loadIcon("Open22.png"),
            "Open a file for editing", KeyEvent.VK_O, VenusUI.makeShortcut(KeyEvent.VK_O),
            VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VenusUI.this.editor.openFile();
            }
        };
        this.fileCloseAction = new GuiAction(
            "Close", null, "Close the current file", KeyEvent.VK_C,
            VenusUI.makeShortcut(KeyEvent.VK_W),
            VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VenusUI.this.editor.close();
            }
        };
        this.fileCloseAllAction = new GuiAction(
            "Close All", null, "Close all open files",
            KeyEvent.VK_L, null,
            VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VenusUI.this.editor.closeAll();
            }
        };
        this.fileSaveAction = new GuiAction(
            "Save", VenusUI.loadIcon("Save22.png"), "Save the current file",
            KeyEvent.VK_S, VenusUI.makeShortcut(KeyEvent.VK_S), VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VenusUI.this.editor.save();
            }
        };
        this.fileSaveAsAction = new GuiAction(
            "Save as ...", VenusUI.loadIcon("SaveAs22.png"),
            "Save current file with different name", KeyEvent.VK_A, null, VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VenusUI.this.editor.saveAs();
            }
        };
        this.fileSaveAllAction = new GuiAction(
            "Save All", null, "Save all open files",
            KeyEvent.VK_V, null, VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VenusUI.this.editor.saveAll();
            }
        };
        this.fileDumpMemoryAction = new FileDumpMemoryAction(
            "Dump Memory ...", VenusUI.loadIcon("Dump22.png"),
            "Dump machine code or data in an available format", KeyEvent.VK_D,
            VenusUI.makeShortcut(KeyEvent.VK_D),
            this
        );
        this.fileExitAction = new GuiAction("Exit", null, "Exit Rars", KeyEvent.VK_X, null, VenusUI.this) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (VenusUI.this.editor.closeAll()) {
                    System.exit(0);
                }
            }
        };

        this.editUndoAction = new GuiAction(
            "Undo", VenusUI.loadIcon("Undo22.png"), "Undo last edit",
            KeyEvent.VK_U, VenusUI.makeShortcut(KeyEvent.VK_Z), VenusUI.this
        ) {
            {
                this.setEnabled(false);
            }

            @Override
            public void actionPerformed(final ActionEvent e) {
                final EditPane editPane = VenusUI.this.mainPane.getEditPane();
                if (editPane != null) {
                    editPane.undo();
                    VenusUI.this.updateUndoAndRedoState();
                }
            }
        };
        this.editRedoAction = new GuiAction(
            "Redo", VenusUI.loadIcon("Redo22.png"), "Redo last edit",
            KeyEvent.VK_R, VenusUI.makeShortcut(KeyEvent.VK_Y), VenusUI.this
        ) {
            {
                this.setEnabled(false);
            }

            @Override
            public void actionPerformed(final ActionEvent e) {
                final EditPane editPane = VenusUI.this.mainPane.getEditPane();
                if (editPane != null) {
                    editPane.redo();
                    VenusUI.this.updateUndoAndRedoState();
                }
            }
        };
        this.editCutAction = new GuiAction(
            "Cut", VenusUI.loadIcon("Cut22.gif"), "Cut", KeyEvent.VK_C,
            VenusUI.makeShortcut(KeyEvent.VK_X), VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VenusUI.this.mainPane.getEditPane().cutText();
            }
        };
        this.editCopyAction = new GuiAction(
            "Copy", VenusUI.loadIcon("Copy22.png"), "Copy", KeyEvent.VK_O,
            VenusUI.makeShortcut(KeyEvent.VK_C), VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VenusUI.this.mainPane.getEditPane().copyText();
            }
        };
        this.editPasteAction = new GuiAction(
            "Paste", VenusUI.loadIcon("Paste22.png"), "Paste", KeyEvent.VK_P,
            VenusUI.makeShortcut(KeyEvent.VK_V), VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VenusUI.this.mainPane.getEditPane().pasteText();
            }
        };
        this.editFindReplaceAction = new EditFindReplaceAction(
            "Find/Replace", VenusUI.loadIcon("Find22.png"),
            "Find/Replace", KeyEvent.VK_F, VenusUI.makeShortcut(KeyEvent.VK_F), this
        );

        this.runAssembleAction = new RunAssembleAction(
            "Assemble", VenusUI.loadIcon("Assemble22.png"),
            "Assemble the current file and clear breakpoints", KeyEvent.VK_A,
            KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), this
        );
        this.runGoAction = new RunGoAction(
            "Go", VenusUI.loadIcon("Play22.png"), "Run the current program",
            KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), this
        );
        this.runStepAction = new RunStepAction(
            "Step", VenusUI.loadIcon("StepForward22.png"),
            "Run one step at a time", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), this
        );
        this.runBackstepAction = new RunBackstepAction(
            "Backstep", VenusUI.loadIcon("StepBack22.png"),
            "Undo the last step", KeyEvent.VK_B, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), this
        );
        this.runPauseAction = new GuiAction(
            "Pause", VenusUI.loadIcon("Pause22.png"),
            "Pause the currently running program", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0),
            VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Globals.SIMULATOR.pauseExecution();
                // RunGoAction's "paused" method will do the cleanup.
            }
        };
        this.runStopAction = new GuiAction(
            "Stop", VenusUI.loadIcon("Stop22.png"),
            "Stop the currently running program", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),
            VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Globals.SIMULATOR.stopExecution();
                // RunGoAction's "stopped" method will take care of the cleanup.
            }
        };
        this.runResetAction = new RunResetAction(
            "Reset", VenusUI.loadIcon("Reset22.png"), "Reset memory and " +
            "registers",
            KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), this
        );
        this.runClearBreakpointsAction = new RunClearBreakpointsAction(
            "Clear all breakpoints", null,
            "Clears all execution breakpoints set since the last assemble.", KeyEvent.VK_K,
            VenusUI.makeShortcut(KeyEvent.VK_K), this
        );
        this.runToggleBreakpointsAction = new GuiAction(
            "Toggle all breakpoints", null,
            "Disable/enable all breakpoints without clearing (can also click Bkpt column header)",
            KeyEvent.VK_T, VenusUI.makeShortcut(KeyEvent.VK_T), VenusUI.this
        ) {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // settingsLabelAction.actionPerformed(e);
                VenusUI.this.mainPane.executePane.textSegment.toggleBreakpoints();
            }
        };
        this.settingsLabelAction = new SettingsAction(
            "Show Labels Window (symbol table)",
            "Toggle visibility of Labels window (symbol table) in the Execute tab",
            BoolSetting.LABEL_WINDOW_VISIBILITY, this, (value) -> {
            this.mainPane.executePane.setLabelWindowVisibility(value);
            VenusUI.LOGGER.info("ExecutePane reference 2");
        }
        );
        this.settingsDarkModeAction = new SettingsAction(
            "Dark mode", "Toggle between light and dark mode",
            BoolSetting.DARK_MODE, this, this::setDarkModeState
        );

        this.settingsPopupInputAction = new SettingsAction(
            "Popup dialog for input syscalls (5,6,7,8,12)",
            "If set, use popup dialog for input syscalls (5,6,7,8,12) instead of cursor in Run I/O window",
            BoolSetting.POPUP_SYSCALL_INPUT, this, (v) -> {
        }
        );

        this.settingsValueDisplayBaseAction = new SettingsAction(
            "Values displayed in hexadecimal",
            "Toggle between hexadecimal and decimal display of memory/register values",
            BoolSetting.DISPLAY_VALUES_IN_HEX, this,
            this.mainPane.executePane.valueDisplayBase::setSelected
        );
        this.settingsAddressDisplayBaseAction = new SettingsAction(
            "Addresses displayed in hexadecimal",
            "Toggle between hexadecimal and decimal display of memory addresses",
            BoolSetting.DISPLAY_ADDRESSES_IN_HEX, this, this.mainPane.executePane.addressDisplayBase::setSelected
        );
        this.settingsExtendedAction = new SettingsAction(
            "Permit extended (usePseudoInstructions) instructions " +
                "and formats",
            "If set, extended (usePseudoInstructions) instructions are formats are permitted.",
            BoolSetting.EXTENDED_ASSEMBLER_ENABLED, this
        );
        this.settingsAssembleOnOpenAction = new SettingsAction(
            "Assemble file upon opening",
            "If set, a file will be automatically assembled as soon as it is opened.  File Open dialog will " +
                "show most recently opened file.",
            BoolSetting.ASSEMBLE_ON_OPEN, this
        );
        this.settingsAssembleAllAction = new SettingsAction(
            "Assemble all files in directory",
            "If set, all files in current directory will be assembled when Assemble operation is selected.",
            BoolSetting.ASSEMBLE_ALL, this
        );
        this.settingsAssembleOpenAction = new SettingsAction(
            "Assemble all files currently open",
            "If set, all files currently open for editing will be assembled when Assemble operation is " +
                "selected.",
            BoolSetting.ASSEMBLE_OPEN, this
        );
        this.settingsWarningsAreErrorsAction = new SettingsAction(
            "Assembler warnings are considered errors",
            "If set, assembler warnings will be interpreted as errors and prevent successful assembly.",
            BoolSetting.WARNINGS_ARE_ERRORS, this
        );
        this.settingsStartAtMainAction = new SettingsAction(
            "Initialize Program Counter to global 'main' if " +
                "defined",
            "If set, assembler will initialize Program Counter to text address globally labeled 'main', if " +
                "defined.",
            BoolSetting.START_AT_MAIN, this
        );
        this.settingsProgramArgumentsAction = new SettingsAction(
            "Program arguments provided to program",
            "If set, program arguments for the program can be entered in border of Text Segment window.",
            BoolSetting.PROGRAM_ARGUMENTS, this, (selected) -> {
            if (selected) {
                this.mainPane.executePane.textSegment.addProgramArgumentsPanel();
            } else {
                this.mainPane.executePane.textSegment.removeProgramArgumentsPanel();
            }
        }
        )
        ;
        this.settingsSelfModifyingCodeAction = new SettingsAction(
            "Self-modifying code",
            "If set, the program can write and branch to both text and data segments.",
            BoolSetting.SELF_MODIFYING_CODE_ENABLED, this
        );

        this.settingsRV64Action = new SettingsAction(
            "64 bit",
            "If set, registers are 64 bits wide and new instructions are available",
            BoolSetting.RV64_ENABLED, this, (isRV64) -> {
            InstructionsRegistry.RV64_MODE_FLAG = isRV64;
            this.registersTab.updateRegisters();
            this.floatingPointTab.updateRegisters();
            this.csrTab.updateRegisters();
        }
        );
        this.settingsDeriveCurrentWorkingDirectoryAction = new SettingsAction(
            "Derive current working directory",
            "If set, the working directory is derived from the main file instead of the RARS executable " +
                "directory.",
            BoolSetting.DERIVE_CURRENT_WORKING_DIRECTORY, this
        );

        this.settingsEditorAction = new SettingsEditorAction(
            "Editor...", null,
            "View and modify text editor settings.", null, null, this
        );
        this.settingsExceptionHandlerAction = new SettingsExceptionHandlerAction(
            "Exception Handler...", null,
            "If set, the specified exception handler file will be included in all Assemble operations.",
            null, null, this
        );
        this.settingsMemoryConfigurationAction = new SettingsMemoryConfigurationAction(
            "Memory Configuration...",
            null, "View and modify memory segment base addresses for the simulated processor",
            null, null, this
        );

        this.helpHelpAction = new HelpHelpAction(
            "Help", VenusUI.loadIcon("Help22.png"),
            "Help", KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), this
        );
        this.helpAboutAction = new HelpAboutAction(
            "About ...", null,
            "Information about Rars", null, null, this
        );

        // endregion Action objects

        this.venusIO = new VenusIO(this.messagesPane, BOOL_SETTINGS);

        this.menu = this.setUpMenuBar();
        this.setJMenuBar(this.menu);

        final JToolBar toolbar = this.createToolbar();

        final JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp.add(toolbar);
        this.runSpeedPanel = new RunSpeedPanel();
        jp.add(this.runSpeedPanel);
        final JPanel center = new JPanel(new BorderLayout());
        center.add(jp, BorderLayout.NORTH);
        center.add(horizontalLayout);

        this.getContentPane().add(center);

        FileStatus.reset();
        // The following has side effect of establishing menu state
        FileStatus.setSystemState(FileStatus.State.NO_FILE);
        this.setMenuStateInitial();

        this.addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowOpened(final WindowEvent e) {
                    // This is invoked when opening the app. It will set the app to
                    // appear at full screen size.
                    VenusUI.this.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }

                @Override
                public void windowClosing(final WindowEvent e) {
                    // This is invoked when exiting the app through the X icon. It will in turn
                    // check for unsaved edits before exiting.
                    if (VenusUI.this.editor.closeAll()) {
                        System.exit(0);
                    }
                }
            });

        // The following will handle the windowClosing event properly in the
        // situation where user Cancels out of "save edits?" dialog. By default,
        // the GUI frame will be hidden but I want it to do nothing.
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        this.pack();
        this.setVisible(true);

        // Open files
        if (!this.editor.openFiles(files)) {
            VenusUI.LOGGER.fatal(
                "Internal Error: could not open files {}",
                files.stream().map(File::getName).collect(Collectors.joining(", "))
            );
            System.exit(1);
        }
    }

    /*
     * build the menus and connect them to action objects (which serve as action
     * listeners
     * shared between menu item and corresponding toolbar icon).
     */

    private static KeyStroke makeShortcut(final int key) {
        return KeyStroke.getKeyStroke(key, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /// Sets each of the [Action] objects to be enabled.
    ///
    /// @param actions
    ///     The actions to enable.
    private static void setEnabled(final @NotNull Action... actions) {
        Arrays.stream(actions).forEach((action) -> action.setEnabled(true));
    }

    /// Sets each of the [Action] objects to be enabled.
    ///
    /// @param actions
    ///     The actions to enable.
    private static void setDisabled(final @NotNull Action... actions) {
        Arrays.stream(actions).forEach((action) -> action.setEnabled(false));
    }

    private static @NotNull JButton createActionButton(final @NotNull Action action) {
        final JButton button = new JButton(action);
        button.setText("");
        return button;
    }

    private static @NotNull JMenuItem menuItem(
        final @NotNull Action action,
        final @NotNull String imageFile
    ) {
        final var menuItem = new JMenuItem(action);
        menuItem.setIcon(VenusUI.loadIcon(imageFile));
        return menuItem;
    }

    private static @NotNull JCheckBoxMenuItem checkBoxItem(
        final @NotNull Action action,
        final @NotNull BoolSetting setting
    ) {
        final var checkBoxMenuItem = new JCheckBoxMenuItem(action);
        checkBoxMenuItem.setSelected(BOOL_SETTINGS.getSetting(setting));
        return checkBoxMenuItem;
    }

    private static @NotNull ImageIcon loadIcon(final @NotNull String name) {
        final var resource = VenusUI.class.getResource(Globals.imagesPath + name);
        return new ImageIcon(Toolkit.getDefaultToolkit().getImage(resource));
    }

    private void setDarkModeState(final boolean isDarkMode) {
        final var lookAndFeel = isDarkMode ? new FlatMacDarkLaf() : new FlatMacLightLaf();
        try {
            UIManager.setLookAndFeel(lookAndFeel);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (final UnsupportedLookAndFeelException e) {
            LOGGER.error("Error when loading look and feel.", e);
        }
    }

    // region Menu bar

    // region Menu bar menus

    private @NotNull JMenu createFileMenu() {
        final var result = new JMenu("File");
        result.setMnemonic(KeyEvent.VK_F);

        List.of(
            menuItem(this.fileNewAction, "New16.png"),
            menuItem(this.fileOpenAction, "Open16.png"),
            menuItem(this.fileCloseAction, "MyBlank16.gif"),
            menuItem(this.fileCloseAllAction, "MyBlank16.gif"),
            new JPopupMenu.Separator(),
            menuItem(this.fileSaveAction, "Save16.png"),
            menuItem(this.fileSaveAsAction, "SaveAs16.png"),
            menuItem(this.fileSaveAllAction, "MyBlank16.gif"),
            menuItem(this.fileDumpMemoryAction, "Dump16.png"),
            new JPopupMenu.Separator(),
            menuItem(this.fileExitAction, "MyBlank16.gif")
        ).forEach(result::add);

        return result;
    }

    private @NotNull JMenu createEditMenu() {
        final var result = new JMenu("Edit");
        result.setMnemonic(KeyEvent.VK_E);

        List.of(
            menuItem(this.editUndoAction, "Undo16.png"),
            menuItem(this.editRedoAction, "Redo16.png"),
            new JPopupMenu.Separator(),
            menuItem(this.editCutAction, "Cut16.gif"),
            menuItem(this.editCopyAction, "Copy16.png"),
            menuItem(this.editPasteAction, "Paste16.png"),
            new JPopupMenu.Separator(),
            menuItem(this.editFindReplaceAction, "Find16.png")
        ).forEach(result::add);

        return result;
    }

    private @NotNull JMenu createRunMenu() {
        final var run = new JMenu("Run");
        run.setMnemonic(KeyEvent.VK_R);

        List.of(
            menuItem(this.runAssembleAction, "Assemble16.png"),
            menuItem(this.runGoAction, "Play16.png"),
            menuItem(this.runStepAction, "StepForward16.png"),
            menuItem(this.runBackstepAction, "StepBack16.png"),
            menuItem(this.runPauseAction, "Pause16.png"),
            menuItem(this.runStopAction, "Stop16.png"),
            menuItem(this.runResetAction, "Reset16.png"),
            new JPopupMenu.Separator(),
            menuItem(this.runClearBreakpointsAction, "MyBlank16.gif"),
            menuItem(this.runToggleBreakpointsAction, "MyBlank16.gif")
        ).forEach(run::add);

        return run;
    }

    private @NotNull JMenu createSettingsMenu() {
        final var settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic(KeyEvent.VK_S);

        final var settingsValueDisplayBase = checkBoxItem(
            this.settingsValueDisplayBaseAction,
            BoolSetting.DISPLAY_VALUES_IN_HEX
        );
        final var settingsAddressDisplayBase = checkBoxItem(
            this.settingsAddressDisplayBaseAction,
            BoolSetting.DISPLAY_ADDRESSES_IN_HEX
        );

        this.mainPane.executePane.valueDisplayBase.setSettingsMenuItem(settingsValueDisplayBase);
        this.mainPane.executePane.addressDisplayBase.setSettingsMenuItem(settingsAddressDisplayBase);

        List.of(
            checkBoxItem(this.settingsDarkModeAction, BoolSetting.DARK_MODE),
            new JPopupMenu.Separator(),
            checkBoxItem(this.settingsLabelAction, BoolSetting.LABEL_WINDOW_VISIBILITY),
            checkBoxItem(this.settingsProgramArgumentsAction, BoolSetting.PROGRAM_ARGUMENTS),
            checkBoxItem(this.settingsPopupInputAction, BoolSetting.POPUP_SYSCALL_INPUT),
            settingsAddressDisplayBase,
            settingsValueDisplayBase,
            new JPopupMenu.Separator(),
            checkBoxItem(this.settingsAssembleOnOpenAction, BoolSetting.ASSEMBLE_ON_OPEN),
            checkBoxItem(this.settingsAssembleAllAction, BoolSetting.ASSEMBLE_ALL),
            checkBoxItem(this.settingsAssembleOpenAction, BoolSetting.ASSEMBLE_OPEN),
            checkBoxItem(this.settingsWarningsAreErrorsAction, BoolSetting.WARNINGS_ARE_ERRORS),
            checkBoxItem(this.settingsStartAtMainAction, BoolSetting.START_AT_MAIN),
            checkBoxItem(
                this.settingsDeriveCurrentWorkingDirectoryAction,
                BoolSetting.DERIVE_CURRENT_WORKING_DIRECTORY
            ),
            new JPopupMenu.Separator(),
            checkBoxItem(this.settingsExtendedAction, BoolSetting.EXTENDED_ASSEMBLER_ENABLED),
            checkBoxItem(this.settingsSelfModifyingCodeAction, BoolSetting.SELF_MODIFYING_CODE_ENABLED),
            checkBoxItem(this.settingsRV64Action, BoolSetting.RV64_ENABLED),
            new JPopupMenu.Separator(),
            new JMenuItem(this.settingsEditorAction),
            new JMenuItem(this.settingsExceptionHandlerAction),
            new JMenuItem(this.settingsMemoryConfigurationAction)
        ).forEach(settingsMenu::add);

        return settingsMenu;
    }

    private @NotNull JMenu createHelpMenu() {
        final var helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        // slight bug: user typing alt-H activates help menu item directly, not help
        // menu

        List.of(
            menuItem(this.helpHelpAction, "Help16.png"),
            new JPopupMenu.Separator(),
            menuItem(this.helpAboutAction, "MyBlank16.gif")
        ).forEach(helpMenu::add);

        return helpMenu;
    }

    // endregion Menu bar menus

    private @NotNull JMenuBar setUpMenuBar() {
        UIManager.put("TitlePane.menuBarEmbedded", true);
        final var menuBar = new JMenuBar();

        List.of(
            createFileMenu(),
            createEditMenu(),
            createRunMenu(),
            createSettingsMenu(),
            ToolLoader.buildToolsMenu(this),
            createHelpMenu()
        ).forEach(menuBar::add);

        return menuBar;
    }

    // endregion Menu bar

    private @NotNull JToolBar createToolbar() {
        final var toolBar = new JToolBar();

        final var sections = List.of(
            List.of(
                fileNewAction, fileOpenAction, fileSaveAction,
                fileSaveAsAction, fileDumpMemoryAction
            ),
            List.of(
                editUndoAction, editRedoAction, editCutAction,
                editCopyAction, editPasteAction, editFindReplaceAction
            ),
            List.of(
                runGoAction, runAssembleAction, runStepAction,
                runBackstepAction, runPauseAction, runStopAction,
                runResetAction
            ),
            List.of(helpHelpAction)
        );
        sections.stream()
            // convert each action to a button
            .map(list -> list.stream()
                .map(VenusUI::createActionButton)
                .toList())
            // insert separators and flatten the lists
            .flatMap(list -> Stream.concat(list.stream(), Stream.of(new JToolBar.Separator())))
            // add everything to the toolbar
            .forEach(toolBar::add);

        return toolBar;
    }

    // region Menu states

    /*
     * Determine from FileStatus what the menu state (enabled/disabled)should
     * be then call the appropriate method to set it. Current states are:
     *
     * setMenuStateInitial: set upon startup and after File->Close
     * setMenuStateEditingNew: set upon File->New
     * setMenuStateEditing: set upon File->Open or File->Save or erroneous
     * Run->Assemble
     * setMenuStateRunnable: set upon successful Run->Assemble
     * setMenuStateRunning: set upon Run->Go
     * setMenuStateTerminated: set upon completion of simulated execution
     */
    public void setMenuState(final @NotNull FileStatus.State status) {
        switch (status) {
            case NO_FILE -> this.setMenuStateInitial();
            case NEW_NOT_EDITED, NEW_EDITED -> this.setMenuStateEditingNew();
            case NOT_EDITED -> this.setMenuStateNotEdited(); // was MenuStateEditing. DPS 9-Aug-2011
            case EDITED -> this.setMenuStateEditing();
            case RUNNABLE -> this.setMenuStateRunnable();
            case RUNNING -> this.setMenuStateRunning();
            case TERMINATED -> this.setMenuStateTerminated();
            case OPENING -> {
            }
        }
    }

    private void setMenuStateInitial() {
        setEnabled(fileNewAction, fileOpenAction);
        setDisabled(
            fileCloseAction, fileCloseAllAction, fileSaveAction, fileSaveAsAction, fileSaveAllAction,
            fileDumpMemoryAction
        );
        this.fileExitAction.setEnabled(true);
        setDisabled(
            editUndoAction, editRedoAction, editCutAction, editCopyAction, editPasteAction,
            editFindReplaceAction
        );
        this.settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        setDisabled(
            runAssembleAction, runGoAction, runStepAction, runBackstepAction, runResetAction,
            runStopAction, runPauseAction, runClearBreakpointsAction, runToggleBreakpointsAction
        );
        setEnabled(helpHelpAction, helpAboutAction);
        this.updateUndoAndRedoState();
    }

    /*
     * Added DPS 9-Aug-2011, for newly-opened files. Retain
     * existing Run menu state (except Assemble, which is always true).
     * Thus if there was a valid assembly it is retained.
     */
    private void setMenuStateNotEdited() {
        /* Note: undo and redo are handled separately by the undo manager */
        setEnabled(
            fileNewAction, fileOpenAction, fileCloseAction, fileCloseAllAction, fileSaveAction,
            fileSaveAsAction, fileSaveAllAction
        );
        this.fileDumpMemoryAction.setEnabled(false);
        setEnabled(
            fileDumpMemoryAction, fileExitAction, editCutAction, editCopyAction, editPasteAction,
            editFindReplaceAction, settingsMemoryConfigurationAction, runAssembleAction
        );
        // If assemble-all, allow previous Run menu settings to remain.
        // Otherwise, clear them out. DPS 9-Aug-2011
        if (!BOOL_SETTINGS.getSetting(BoolSetting.ASSEMBLE_ALL)) {
            setDisabled(
                this.runGoAction, this.runStepAction, this.runBackstepAction, this.runResetAction,
                this.runStopAction, this.runPauseAction, this.runClearBreakpointsAction,
                this.runToggleBreakpointsAction
            );
        }
        setEnabled(helpHelpAction, helpAboutAction);
        this.updateUndoAndRedoState();
    }

    private void setMenuStateEditing() {
        /* Note: undo and redo are handled separately by the undo manager */
        setEnabled(
            fileNewAction, fileOpenAction, fileCloseAction, fileCloseAllAction, fileSaveAction,
            fileSaveAsAction, fileSaveAllAction
        );
        setDisabled(fileDumpMemoryAction);
        setEnabled(
            fileExitAction, editCutAction, editCopyAction, editPasteAction, editFindReplaceAction,
            settingsMemoryConfigurationAction, runAssembleAction
        );
        setDisabled(
            runGoAction, runStepAction, runBackstepAction, runResetAction,
            runStopAction, runPauseAction, runClearBreakpointsAction, runToggleBreakpointsAction
        );
        setEnabled(helpHelpAction, helpAboutAction);
        this.updateUndoAndRedoState();
    }

    /**
     * Use this when "File -> New" is used
     */
    private void setMenuStateEditingNew() {
        /* Note: undo and redo are handled separately by the undo manager */
        setEnabled(
            fileNewAction, fileOpenAction, fileCloseAction, fileCloseAllAction, fileSaveAction,
            fileSaveAsAction, fileSaveAllAction
        );
        setDisabled(fileDumpMemoryAction);
        setEnabled(
            fileExitAction, editCutAction, editCopyAction, editPasteAction, editFindReplaceAction,
            settingsMemoryConfigurationAction
        );
        setDisabled(
            runAssembleAction, runGoAction, runStepAction, runBackstepAction, runResetAction,
            runStopAction, runPauseAction, runClearBreakpointsAction, runToggleBreakpointsAction
        );
        setEnabled(helpHelpAction, helpAboutAction);
        this.updateUndoAndRedoState();
    }

    /*
     * Use this upon successful assemble or reset
     */
    private void setMenuStateRunnable() {
        /* Note: undo and redo are handled separately by the undo manager */
        setEnabled(
            fileNewAction, fileOpenAction, fileCloseAction, fileCloseAllAction, fileSaveAction,
            fileSaveAsAction, fileSaveAllAction, fileDumpMemoryAction, fileExitAction, editCutAction,
            editCopyAction, editPasteAction, editFindReplaceAction,
            settingsMemoryConfigurationAction, runAssembleAction, runGoAction, runStepAction
        );
        runBackstepAction.setEnabled(OtherSettings.getBackSteppingEnabled() && !Globals.PROGRAM.getBackStepper()
            .empty());
        setEnabled(runResetAction);
        setDisabled(runStopAction, runPauseAction);
        setEnabled(runToggleBreakpointsAction, helpHelpAction, helpAboutAction);
        this.updateUndoAndRedoState();
    }

    /*
     * Use this while program is running
     */
    private void setMenuStateRunning() {
        setDisabled(
            fileNewAction, fileOpenAction, fileCloseAction, fileCloseAllAction, fileSaveAction,
            fileSaveAsAction, fileSaveAllAction, fileDumpMemoryAction, fileExitAction, editCutAction,
            editCopyAction, editPasteAction, editFindReplaceAction,
            settingsMemoryConfigurationAction, runAssembleAction, runGoAction, runStepAction,
            runBackstepAction, runResetAction
        );
        setEnabled(runStopAction, runPauseAction);
        setDisabled(runToggleBreakpointsAction);
        setEnabled(helpHelpAction, helpAboutAction);
        setDisabled(editUndoAction, editRedoAction);
    }

    /*
     * Use this upon completion of execution
     */
    private void setMenuStateTerminated() {
        /* Note: undo and redo are handled separately by the undo manager */
        setEnabled(
            fileNewAction, fileOpenAction, fileCloseAction, fileCloseAllAction, fileSaveAction,
            fileSaveAsAction, fileSaveAllAction, fileDumpMemoryAction, fileExitAction, editCutAction,
            editCopyAction, editPasteAction, editFindReplaceAction,
            settingsMemoryConfigurationAction, runAssembleAction
        );
        setDisabled(runGoAction, runStepAction);
        runBackstepAction.setEnabled(OtherSettings.getBackSteppingEnabled() && !Globals.PROGRAM.getBackStepper()
            .empty());
        setEnabled(runResetAction);
        setDisabled(runStopAction, runPauseAction);
        setEnabled(runToggleBreakpointsAction, helpHelpAction, helpAboutAction);

        this.updateUndoAndRedoState();
    }

    // endregion Menu states

    /**
     * Return reference tothe Run->Assemble item's action. Needed by File->Open in
     * case
     * assemble-upon-open flag is set.
     *
     * @return the Action object for the Run->Assemble operation.
     */
    public @NotNull Action getRunAssembleAction() {
        return this.runAssembleAction;
    }

    /**
     * Have the menu request keyboard focus. DPS 5-4-10
     */
    void haveMenuRequestFocus() {
        this.menu.requestFocus();
    }

    private void updateUndoAndRedoState() {
        final EditPane editPane = this.mainPane.getEditPane();
        this.editUndoAction.setEnabled(editPane != null && editPane.canUndo());
        this.editRedoAction.setEnabled(editPane != null && editPane.canRedo());
    }
}
