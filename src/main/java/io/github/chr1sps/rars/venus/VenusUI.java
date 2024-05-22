package io.github.chr1sps.rars.venus;

//import com.formdev.flatlaf.extras.FlatSVGIcon;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.Settings;
import io.github.chr1sps.rars.riscv.InstructionSet;
import io.github.chr1sps.rars.riscv.dump.DumpFormatLoader;
import io.github.chr1sps.rars.simulator.Simulator;
import io.github.chr1sps.rars.venus.registers.ControlAndStatusWindow;
import io.github.chr1sps.rars.venus.registers.FloatingPointWindow;
import io.github.chr1sps.rars.venus.registers.RegistersPane;
import io.github.chr1sps.rars.venus.registers.RegistersWindow;
import io.github.chr1sps.rars.venus.run.*;
import io.github.chr1sps.rars.venus.settings.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;

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
public class VenusUI extends JFrame {
    private final VenusUI mainUI;
    public JMenuBar menu;
    private final JToolBar toolbar;
    private final MainPane mainPane;
    private final RegistersPane registersPane;
    private final RegistersWindow registersTab;
    private final FloatingPointWindow fpTab;
    private final ControlAndStatusWindow csrTab;
    private final MessagesPane messagesPane;
    private final JSplitPane splitter;
    private final JSplitPane horizonSplitter;

    private int frameState; // see windowActivated() and windowDeactivated()
    private static int menuState = FileStatus.NO_FILE;

    // PLEASE PUT THESE TWO (& THEIR METHODS) SOMEWHERE THEY BELONG, NOT HERE
    private boolean reset = true; // registers/memory reset for execution
    private boolean started = false; // started execution
    private final Editor editor;

    // components of the menubar
    private JMenu file, run, window, help, edit, settings;
    private JMenuItem fileNew, fileOpen, fileClose, fileCloseAll, fileSave, fileSaveAs, fileSaveAll, fileDumpMemory,
            fileExit;
    private JMenuItem editUndo, editRedo, editCut, editCopy, editPaste, editFindReplace, editSelectAll;
    private JMenuItem runGo, runStep, runBackstep, runReset, runAssemble, runStop, runPause, runClearBreakpoints,
            runToggleBreakpoints;
    private JCheckBoxMenuItem settingsLabel, settingsPopupInput, settingsValueDisplayBase, settingsAddressDisplayBase,
            settingsExtended, settingsAssembleOnOpen, settingsAssembleAll, settingsAssembleOpen,
            settingsWarningsAreErrors,
            settingsStartAtMain, settingsProgramArguments, settingsSelfModifyingCode, settingsRV64,
            settingsDeriveCurrentWorkingDirectory;
    private JMenuItem settingsExceptionHandler, settingsEditor, settingsHighlighting, settingsMemoryConfiguration;
    private JMenuItem helpHelp, helpAbout;

    // components of the toolbar
    private JButton Undo, Redo, Cut, Copy, Paste, FindReplace, SelectAll;
    private JButton New, Open, Save, SaveAs, SaveAll, DumpMemory;
    private JButton Run, Assemble, Reset, Step, Backstep, Stop, Pause;
    private JButton Help;

    // The "action" objects, which include action listeners. One of each will be
    // created then
    // shared between a menu item and its corresponding toolbar button. This is a
    // very cool
    // technique because it relates the button and menu item so closely

    private Action fileNewAction, fileOpenAction, fileCloseAction, fileCloseAllAction, fileSaveAction;
    private Action fileSaveAsAction, fileSaveAllAction, fileDumpMemoryAction, fileExitAction;
    private Action editUndoAction;
    private Action editRedoAction;
    private Action editCutAction, editCopyAction, editPasteAction, editFindReplaceAction, editSelectAllAction;
    private Action runAssembleAction, runGoAction, runStepAction, runBackstepAction, runResetAction,
            runStopAction, runPauseAction, runClearBreakpointsAction, runToggleBreakpointsAction;
    private Action settingsLabelAction, settingsPopupInputAction, settingsValueDisplayBaseAction,
            settingsAddressDisplayBaseAction,
            settingsExtendedAction, settingsAssembleOnOpenAction, settingsAssembleOpenAction, settingsAssembleAllAction,
            settingsWarningsAreErrorsAction, settingsStartAtMainAction, settingsProgramArgumentsAction,
            settingsExceptionHandlerAction, settingsEditorAction, settingsHighlightingAction,
            settingsMemoryConfigurationAction,
            settingsSelfModifyingCodeAction, settingsRV64Action, settingsDeriveCurrentWorkingDirectoryAction;
    private Action helpHelpAction, helpAboutAction;

    /**
     * Constructor for the Class. Sets up a window object for the UI
     *
     * @param name  Name of the window to be created.
     * @param paths File paths to open width
     */
    public VenusUI(final String name, final ArrayList<String> paths) {
        super(name);
        this.mainUI = this;
        Globals.setGui(this);
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

        final Dimension messagesPanePreferredSize = new Dimension((int) (screenWidth * messageWidthPct),
                (int) (screenHeight * messageHeightPct));
        final Dimension mainPanePreferredSize = new Dimension((int) (screenWidth * mainWidthPct),
                (int) (screenHeight * mainHeightPct));
        final Dimension registersPanePreferredSize = new Dimension((int) (screenWidth * registersWidthPct),
                (int) (screenHeight * registersHeightPct));

        // the "restore" size (window control button that toggles with maximize)
        // I want to keep it large, with enough room for user to get handles
        // this.setSize((int)(screenWidth*.8),(int)(screenHeight*.8));

        Globals.initialize();

        // image courtesy of NASA/JPL.
        final URL im = this.getClass().getResource(Globals.imagesPath + "RISC-V.png");
        if (im == null) {
            System.out.println("Internal Error: images folder or file not found");
            System.exit(0);
        }
        final Image mars = Toolkit.getDefaultToolkit().getImage(im);
        this.setIconImage(mars);
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

        this.registersTab = new RegistersWindow();
        this.fpTab = new FloatingPointWindow();
        this.csrTab = new ControlAndStatusWindow();
        this.registersPane = new RegistersPane(this.mainUI, this.registersTab, this.fpTab, this.csrTab);
        this.registersPane.setPreferredSize(registersPanePreferredSize);

        // Insets defaultTabInsets = (Insets)UIManager.get("TabbedPane.tabInsets");
        // UIManager.put("TabbedPane.tabInsets", new Insets(1, 1, 1, 1));
        this.mainPane = new MainPane(this.mainUI, this.editor, this.registersTab, this.fpTab, this.csrTab);
        // UIManager.put("TabbedPane.tabInsets", defaultTabInsets);

        this.mainPane.setPreferredSize(mainPanePreferredSize);
        this.messagesPane = new MessagesPane();
        this.messagesPane.setPreferredSize(messagesPanePreferredSize);
        this.splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.mainPane, this.messagesPane);
        this.splitter.setOneTouchExpandable(true);
        this.splitter.resetToPreferredSizes();
        this.horizonSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.splitter, this.registersPane);
        this.horizonSplitter.setOneTouchExpandable(true);
        this.horizonSplitter.resetToPreferredSizes();

        // due to dependencies, do not set up menu/toolbar until now.
        this.createActionObjects();
        this.menu = this.setUpMenuBar();
        this.setJMenuBar(this.menu);

        this.toolbar = this.setUpToolBar();

        final JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp.add(this.toolbar);
        jp.add(RunSpeedPanel.getInstance());
        final JPanel center = new JPanel(new BorderLayout());
        center.add(jp, BorderLayout.NORTH);
        center.add(this.horizonSplitter);

        this.getContentPane().add(center);

        FileStatus.reset();
        // The following has side effect of establishing menu state
        FileStatus.set(FileStatus.NO_FILE);

        // This is invoked when opening the app. It will set the app to
        // appear at full screen size.
        this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowOpened(final WindowEvent e) {
                        VenusUI.this.mainUI.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                });

        // This is invoked when exiting the app through the X icon. It will in turn
        // check for unsaved edits before exiting.
        this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent e) {
                        if (VenusUI.this.mainUI.editor.closeAll()) {
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
        if (!this.editor.open(paths)) {
            System.out.println("Internal Error: could not open files" + String.join(", ", paths));
            System.exit(1);
        }
    }

    /*
     * Action objects are used instead of action listeners because one can be easily
     * shared between
     * a menu item and a toolbar button. Does nice things like disable both if the
     * action is
     * disabled, etc.
     */
    private void createActionObjects() {
        try {
            this.fileNewAction = new GuiAction("New",
                    this.loadIcon("New22.png"),
                    "Create a new file for editing", KeyEvent.VK_N, this.makeShortcut(KeyEvent.VK_N)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.editor.newFile();
                }
            };
            this.fileOpenAction = new GuiAction("Open ...", this.loadIcon("Open22.png"),
                    "Open a file for editing", KeyEvent.VK_O, this.makeShortcut(KeyEvent.VK_O)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.editor.open();
                }
            };
            this.fileCloseAction = new GuiAction("Close", null, "Close the current file", KeyEvent.VK_C,
                    this.makeShortcut(KeyEvent.VK_W)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.editor.close();
                }
            };
            this.fileCloseAllAction = new GuiAction("Close All", null, "Close all open files",
                    KeyEvent.VK_L, null) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.editor.closeAll();
                }
            };
            this.fileSaveAction = new GuiAction("Save", this.loadIcon("Save22.png"), "Save the current file",
                    KeyEvent.VK_S, this.makeShortcut(KeyEvent.VK_S)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.editor.save();
                }
            };
            this.fileSaveAsAction = new GuiAction("Save as ...", this.loadIcon("SaveAs22.png"),
                    "Save current file with different name", KeyEvent.VK_A, null) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.editor.saveAs();
                }
            };
            this.fileSaveAllAction = new GuiAction("Save All", null, "Save all open files",
                    KeyEvent.VK_V, null) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.editor.saveAll();
                }
            };
            this.fileDumpMemoryAction = new FileDumpMemoryAction("Dump Memory ...", this.loadIcon("Dump22.png"),
                    "Dump machine code or data in an available format", KeyEvent.VK_D, this.makeShortcut(KeyEvent.VK_D),
                    this.mainUI);
            this.fileExitAction = new GuiAction("Exit", null, "Exit Rars", KeyEvent.VK_X, null) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (VenusUI.this.editor.closeAll()) {
                        System.exit(0);
                    }
                }
            };

            this.editUndoAction = new GuiAction("Undo", this.loadIcon("Undo22.png"), "Undo last edit",
                    KeyEvent.VK_U, this.makeShortcut(KeyEvent.VK_Z)) {
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
            this.editRedoAction = new GuiAction("Redo", this.loadIcon("Redo22.png"), "Redo last edit",
                    KeyEvent.VK_R, this.makeShortcut(KeyEvent.VK_Y)) {
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
            this.editCutAction = new GuiAction("Cut", this.loadIcon("Cut22.gif"), "Cut", KeyEvent.VK_C,
                    this.makeShortcut(KeyEvent.VK_X)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.mainPane.getEditPane().cutText();
                }
            };
            this.editCopyAction = new GuiAction("Copy", this.loadIcon("Copy22.png"), "Copy", KeyEvent.VK_O,
                    this.makeShortcut(KeyEvent.VK_C)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.mainPane.getEditPane().copyText();
                }
            };
            this.editPasteAction = new GuiAction("Paste", this.loadIcon("Paste22.png"), "Paste", KeyEvent.VK_P,
                    this.makeShortcut(KeyEvent.VK_V)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.mainPane.getEditPane().pasteText();
                }
            };
            this.editFindReplaceAction = new EditFindReplaceAction("Find/Replace", this.loadIcon("Find22.png"),
                    "Find/Replace", KeyEvent.VK_F, this.makeShortcut(KeyEvent.VK_F), this.mainUI);
            this.editSelectAllAction = new GuiAction("Select All",
                    null, // new ImageIcon(tk.getImage(cs.getResource(Globals.imagesPath+"Find22.png"),
                    "Select All", KeyEvent.VK_A,
                    this.makeShortcut(KeyEvent.VK_A)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.mainPane.getEditPane().selectAllText();
                }
            };

            this.runAssembleAction = new RunAssembleAction("Assemble", this.loadIcon("Assemble22.png"),
                    "Assemble the current file and clear breakpoints", KeyEvent.VK_A,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), this.mainUI);
            this.runGoAction = new RunGoAction("Go", this.loadIcon("Play22.png"), "Run the current program",
                    KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), this.mainUI);
            this.runStepAction = new RunStepAction("Step", this.loadIcon("StepForward22.png"),
                    "Run one step at a time", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), this.mainUI);
            this.runBackstepAction = new RunBackstepAction("Backstep", this.loadIcon("StepBack22.png"),
                    "Undo the last step", KeyEvent.VK_B, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), this.mainUI);
            this.runPauseAction = new GuiAction("Pause", this.loadIcon("Pause22.png"),
                    "Pause the currently running program", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    Simulator.getInstance().pauseExecution();
                    // RunGoAction's "paused" method will do the cleanup.
                }
            };
            this.runStopAction = new GuiAction("Stop", this.loadIcon("Stop22.png"),
                    "Stop the currently running program", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    Simulator.getInstance().stopExecution();
                    // RunGoAction's "stopped" method will take care of the cleanup.
                }
            };
            this.runResetAction = new RunResetAction("Reset", this.loadIcon("Reset22.png"), "Reset memory and registers",
                    KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), this.mainUI);
            this.runClearBreakpointsAction = new RunClearBreakpointsAction("Clear all breakpoints", null,
                    "Clears all execution breakpoints set since the last assemble.", KeyEvent.VK_K,
                    this.makeShortcut(KeyEvent.VK_K));
            this.runToggleBreakpointsAction = new GuiAction("Toggle all breakpoints", null,
                    "Disable/enable all breakpoints without clearing (can also click Bkpt column header)",
                    KeyEvent.VK_T, this.makeShortcut(KeyEvent.VK_T)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    // settingsLabelAction.actionPerformed(e);
                    VenusUI.this.mainPane.getExecutePane().getTextSegmentWindow().toggleBreakpoints();
                }
            };
            this.settingsLabelAction = new SettingsAction("Show Labels Window (symbol table)",
                    "Toggle visibility of Labels window (symbol table) in the Execute tab",
                    Settings.Bool.LABEL_WINDOW_VISIBILITY) {
                @Override
                public void handler(final boolean visibility) {
                    VenusUI.this.mainPane.getExecutePane().setLabelWindowVisibility(visibility);
                    System.out.println("ExecutePane reference 2");
                }
            };
            this.settingsPopupInputAction = new SettingsAction("Popup dialog for input syscalls (5,6,7,8,12)",
                    "If set, use popup dialog for input syscalls (5,6,7,8,12) instead of cursor in Run I/O window",
                    Settings.Bool.POPUP_SYSCALL_INPUT);

            this.settingsValueDisplayBaseAction = new SettingsAction("Values displayed in hexadecimal",
                    "Toggle between hexadecimal and decimal display of memory/register values",
                    Settings.Bool.DISPLAY_VALUES_IN_HEX) {
                @Override
                public void handler(final boolean isHex) {
                    VenusUI.this.mainPane.getExecutePane().getValueDisplayBaseChooser().setSelected(isHex);
                }
            };
            this.settingsAddressDisplayBaseAction = new SettingsAction("Addresses displayed in hexadecimal",
                    "Toggle between hexadecimal and decimal display of memory addresses",
                    Settings.Bool.DISPLAY_ADDRESSES_IN_HEX) {
                @Override
                public void handler(final boolean isHex) {
                    VenusUI.this.mainPane.getExecutePane().getAddressDisplayBaseChooser().setSelected(isHex);
                }
            };
            this.settingsExtendedAction = new SettingsAction("Permit extended (pseudo) instructions and formats",
                    "If set, extended (pseudo) instructions are formats are permitted.",
                    Settings.Bool.EXTENDED_ASSEMBLER_ENABLED);
            this.settingsAssembleOnOpenAction = new SettingsAction("Assemble file upon opening",
                    "If set, a file will be automatically assembled as soon as it is opened.  File Open dialog will show most recently opened file.",
                    Settings.Bool.ASSEMBLE_ON_OPEN);
            this.settingsAssembleAllAction = new SettingsAction("Assemble all files in directory",
                    "If set, all files in current directory will be assembled when Assemble operation is selected.",
                    Settings.Bool.ASSEMBLE_ALL);
            this.settingsAssembleOpenAction = new SettingsAction("Assemble all files currently open",
                    "If set, all files currently open for editing will be assembled when Assemble operation is selected.",
                    Settings.Bool.ASSEMBLE_OPEN);
            this.settingsWarningsAreErrorsAction = new SettingsAction("Assembler warnings are considered errors",
                    "If set, assembler warnings will be interpreted as errors and prevent successful assembly.",
                    Settings.Bool.WARNINGS_ARE_ERRORS);
            this.settingsStartAtMainAction = new SettingsAction("Initialize Program Counter to global 'main' if defined",
                    "If set, assembler will initialize Program Counter to text address globally labeled 'main', if defined.",
                    Settings.Bool.START_AT_MAIN);
            this.settingsProgramArgumentsAction = new SettingsAction("Program arguments provided to program",
                    "If set, program arguments for the program can be entered in border of Text Segment window.",
                    Settings.Bool.PROGRAM_ARGUMENTS) {
                @Override
                public void handler(final boolean selected) {
                    if (selected) {
                        VenusUI.this.mainPane.getExecutePane().getTextSegmentWindow().addProgramArgumentsPanel();
                    } else {
                        VenusUI.this.mainPane.getExecutePane().getTextSegmentWindow().removeProgramArgumentsPanel();
                    }
                }
            };
            this.settingsSelfModifyingCodeAction = new SettingsAction("Self-modifying code",
                    "If set, the program can write and branch to both text and data segments.",
                    Settings.Bool.SELF_MODIFYING_CODE_ENABLED);

            // TODO: review this
            this.settingsRV64Action = new SettingsAction("64 bit",
                    "If set, registers are 64 bits wide and new instructions are available",
                    Settings.Bool.RV64_ENABLED) {
                @Override
                public void handler(final boolean value) {
                    InstructionSet.rv64 = value;
                    Globals.instructionSet.populate();
                    VenusUI.this.registersTab.updateRegisters();
                    VenusUI.this.csrTab.updateRegisters();
                }
            };
            this.settingsDeriveCurrentWorkingDirectoryAction = new SettingsAction("Derive current working directory",
                    "If set, the working directory is derived from the main file instead of the RARS executable directory.",
                    Settings.Bool.DERIVE_CURRENT_WORKING_DIRECTORY);

            this.settingsEditorAction = new SettingsEditorAction("Editor...", null,
                    "View and modify text editor settings.", null, null);
            this.settingsHighlightingAction = new SettingsHighlightingAction("Highlighting...", null,
                    "View and modify Execute Tab highlighting colors", null, null);
            this.settingsExceptionHandlerAction = new SettingsExceptionHandlerAction("Exception Handler...", null,
                    "If set, the specified exception handler file will be included in all Assemble operations.",
                    null, null);
            this.settingsMemoryConfigurationAction = new SettingsMemoryConfigurationAction("Memory Configuration...",
                    null, "View and modify memory segment base addresses for the simulated processor",
                    null, null);

            this.helpHelpAction = new HelpHelpAction("Help", this.loadIcon("Help22.png"),
                    "Help", KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), this.mainUI);
            this.helpAboutAction = new HelpAboutAction("About ...", null,
                    "Information about Rars", null, null, this.mainUI);
        } catch (final NullPointerException e) {
            System.out.println(
                    "Internal Error: images folder not found, or other null pointer exception while creating Action objects");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /*
     * build the menus and connect them to action objects (which serve as action
     * listeners
     * shared between menu item and corresponding toolbar icon).
     */

    private JMenuBar setUpMenuBar() {
        UIManager.put("TitlePane.menuBarEmbedded", true);
        final JMenuBar menuBar = new JMenuBar();
//        FlatLaf.revalidateAndRepaintAllFramesAndDialogs();
        this.file = new JMenu("File");
        this.file.setMnemonic(KeyEvent.VK_F);
        this.edit = new JMenu("Edit");
        this.edit.setMnemonic(KeyEvent.VK_E);
        this.run = new JMenu("Run");
        this.run.setMnemonic(KeyEvent.VK_R);
        // window = new JMenu("Window");
        // window.setMnemonic(KeyEvent.VK_W);
        this.settings = new JMenu("Settings");
        this.settings.setMnemonic(KeyEvent.VK_S);
        this.help = new JMenu("Help");
        this.help.setMnemonic(KeyEvent.VK_H);
        // slight bug: user typing alt-H activates help menu item directly, not help
        // menu

        this.fileNew = new JMenuItem(this.fileNewAction);
        this.fileNew.setIcon(this.loadIcon("New16.png"));
        this.fileOpen = new JMenuItem(this.fileOpenAction);
        this.fileOpen.setIcon(this.loadIcon("Open16.png"));
        this.fileClose = new JMenuItem(this.fileCloseAction);
        this.fileClose.setIcon(this.loadIcon("MyBlank16.gif"));
        this.fileCloseAll = new JMenuItem(this.fileCloseAllAction);
        this.fileCloseAll.setIcon(this.loadIcon("MyBlank16.gif"));
        this.fileSave = new JMenuItem(this.fileSaveAction);
        this.fileSave.setIcon(this.loadIcon("Save16.png"));
        this.fileSaveAs = new JMenuItem(this.fileSaveAsAction);
        this.fileSaveAs.setIcon(this.loadIcon("SaveAs16.png"));
        this.fileSaveAll = new JMenuItem(this.fileSaveAllAction);
        this.fileSaveAll.setIcon(this.loadIcon("MyBlank16.gif"));
        this.fileDumpMemory = new JMenuItem(this.fileDumpMemoryAction);
        this.fileDumpMemory.setIcon(this.loadIcon("Dump16.png"));
        this.fileExit = new JMenuItem(this.fileExitAction);
        this.fileExit.setIcon(this.loadIcon("MyBlank16.gif"));
        this.file.add(this.fileNew);
        this.file.add(this.fileOpen);
        this.file.add(this.fileClose);
        this.file.add(this.fileCloseAll);
        this.file.addSeparator();
        this.file.add(this.fileSave);
        this.file.add(this.fileSaveAs);
        this.file.add(this.fileSaveAll);
        if (DumpFormatLoader.getDumpFormats().size() > 0) {
            this.file.add(this.fileDumpMemory);
        }
        this.file.addSeparator();
        this.file.add(this.fileExit);

        this.editUndo = new JMenuItem(this.editUndoAction);
        this.editUndo.setIcon(this.loadIcon("Undo16.png"));// "Undo16.gif"));
        this.editRedo = new JMenuItem(this.editRedoAction);
        this.editRedo.setIcon(this.loadIcon("Redo16.png"));// "Redo16.gif"));
        this.editCut = new JMenuItem(this.editCutAction);
        this.editCut.setIcon(this.loadIcon("Cut16.gif"));
        this.editCopy = new JMenuItem(this.editCopyAction);
        this.editCopy.setIcon(this.loadIcon("Copy16.png"));// "Copy16.gif"));
        this.editPaste = new JMenuItem(this.editPasteAction);
        this.editPaste.setIcon(this.loadIcon("Paste16.png"));// "Paste16.gif"));
        this.editFindReplace = new JMenuItem(this.editFindReplaceAction);
        this.editFindReplace.setIcon(this.loadIcon("Find16.png"));// "Paste16.gif"));
        this.editSelectAll = new JMenuItem(this.editSelectAllAction);
        this.editSelectAll.setIcon(this.loadIcon("MyBlank16.gif"));
        this.edit.add(this.editUndo);
        this.edit.add(this.editRedo);
        this.edit.addSeparator();
        this.edit.add(this.editCut);
        this.edit.add(this.editCopy);
        this.edit.add(this.editPaste);
        this.edit.addSeparator();
        this.edit.add(this.editFindReplace);
        this.edit.add(this.editSelectAll);

        this.runAssemble = new JMenuItem(this.runAssembleAction);
        this.runAssemble.setIcon(this.loadIcon("Assemble16.png"));// "MyAssemble16.gif"));
        this.runGo = new JMenuItem(this.runGoAction);
        this.runGo.setIcon(this.loadIcon("Play16.png"));// "Play16.gif"));
        this.runStep = new JMenuItem(this.runStepAction);
        this.runStep.setIcon(this.loadIcon("StepForward16.png"));// "MyStepForward16.gif"));
        this.runBackstep = new JMenuItem(this.runBackstepAction);
        this.runBackstep.setIcon(this.loadIcon("StepBack16.png"));// "MyStepBack16.gif"));
        this.runReset = new JMenuItem(this.runResetAction);
        this.runReset.setIcon(this.loadIcon("Reset16.png"));// "MyReset16.gif"));
        this.runStop = new JMenuItem(this.runStopAction);
        this.runStop.setIcon(this.loadIcon("Stop16.png"));// "Stop16.gif"));
        this.runPause = new JMenuItem(this.runPauseAction);
        this.runPause.setIcon(this.loadIcon("Pause16.png"));// "Pause16.gif"));
        this.runClearBreakpoints = new JMenuItem(this.runClearBreakpointsAction);
        this.runClearBreakpoints.setIcon(this.loadIcon("MyBlank16.gif"));
        this.runToggleBreakpoints = new JMenuItem(this.runToggleBreakpointsAction);
        this.runToggleBreakpoints.setIcon(this.loadIcon("MyBlank16.gif"));

        this.run.add(this.runAssemble);
        this.run.add(this.runGo);
        this.run.add(this.runStep);
        this.run.add(this.runBackstep);
        this.run.add(this.runPause);
        this.run.add(this.runStop);
        this.run.add(this.runReset);
        this.run.addSeparator();
        this.run.add(this.runClearBreakpoints);
        this.run.add(this.runToggleBreakpoints);

        this.settingsLabel = new JCheckBoxMenuItem(this.settingsLabelAction);
        this.settingsLabel.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.LABEL_WINDOW_VISIBILITY));
        this.settingsPopupInput = new JCheckBoxMenuItem(this.settingsPopupInputAction);
        this.settingsPopupInput.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.POPUP_SYSCALL_INPUT));
        this.settingsValueDisplayBase = new JCheckBoxMenuItem(this.settingsValueDisplayBaseAction);
        this.settingsValueDisplayBase
                .setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.DISPLAY_VALUES_IN_HEX));// mainPane.getExecutePane().getValueDisplayBaseChooser().isSelected());
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has
        // already been created.
        this.mainPane.getExecutePane().getValueDisplayBaseChooser().setSettingsMenuItem(this.settingsValueDisplayBase);
        this.settingsAddressDisplayBase = new JCheckBoxMenuItem(this.settingsAddressDisplayBaseAction);
        this.settingsAddressDisplayBase
                .setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.DISPLAY_ADDRESSES_IN_HEX));// mainPane.getExecutePane().getValueDisplayBaseChooser().isSelected());
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has
        // already been created.
        this.mainPane.getExecutePane().getAddressDisplayBaseChooser().setSettingsMenuItem(this.settingsAddressDisplayBase);
        this.settingsExtended = new JCheckBoxMenuItem(this.settingsExtendedAction);
        this.settingsExtended.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.EXTENDED_ASSEMBLER_ENABLED));
        this.settingsSelfModifyingCode = new JCheckBoxMenuItem(this.settingsSelfModifyingCodeAction);
        this.settingsSelfModifyingCode
                .setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.SELF_MODIFYING_CODE_ENABLED));
        this.settingsRV64 = new JCheckBoxMenuItem(this.settingsRV64Action);
        this.settingsRV64.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.RV64_ENABLED));
        this.settingsDeriveCurrentWorkingDirectory = new JCheckBoxMenuItem(this.settingsDeriveCurrentWorkingDirectoryAction);
        this.settingsDeriveCurrentWorkingDirectory
                .setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.DERIVE_CURRENT_WORKING_DIRECTORY));
        this.settingsAssembleOnOpen = new JCheckBoxMenuItem(this.settingsAssembleOnOpenAction);
        this.settingsAssembleOnOpen.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.ASSEMBLE_ON_OPEN));
        this.settingsAssembleAll = new JCheckBoxMenuItem(this.settingsAssembleAllAction);
        this.settingsAssembleAll.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.ASSEMBLE_ALL));
        this.settingsAssembleOpen = new JCheckBoxMenuItem(this.settingsAssembleOpenAction);
        this.settingsAssembleOpen.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.ASSEMBLE_OPEN));
        this.settingsWarningsAreErrors = new JCheckBoxMenuItem(this.settingsWarningsAreErrorsAction);
        this.settingsWarningsAreErrors
                .setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.WARNINGS_ARE_ERRORS));
        this.settingsStartAtMain = new JCheckBoxMenuItem(this.settingsStartAtMainAction);
        this.settingsStartAtMain.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.START_AT_MAIN));
        this.settingsProgramArguments = new JCheckBoxMenuItem(this.settingsProgramArgumentsAction);
        this.settingsProgramArguments.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.PROGRAM_ARGUMENTS));
        this.settingsEditor = new JMenuItem(this.settingsEditorAction);
        this.settingsHighlighting = new JMenuItem(this.settingsHighlightingAction);
        this.settingsExceptionHandler = new JMenuItem(this.settingsExceptionHandlerAction);
        this.settingsMemoryConfiguration = new JMenuItem(this.settingsMemoryConfigurationAction);

        this.settings.add(this.settingsLabel);
        this.settings.add(this.settingsProgramArguments);
        this.settings.add(this.settingsPopupInput);
        this.settings.add(this.settingsAddressDisplayBase);
        this.settings.add(this.settingsValueDisplayBase);
        this.settings.addSeparator();
        this.settings.add(this.settingsAssembleOnOpen);
        this.settings.add(this.settingsAssembleAll);
        this.settings.add(this.settingsAssembleOpen);
        this.settings.add(this.settingsWarningsAreErrors);
        this.settings.add(this.settingsStartAtMain);
        this.settings.add(this.settingsDeriveCurrentWorkingDirectory);
        this.settings.addSeparator();
        this.settings.add(this.settingsExtended);
        this.settings.add(this.settingsSelfModifyingCode);
        this.settings.add(this.settingsRV64);
        this.settings.addSeparator();
        this.settings.add(this.settingsEditor);
        this.settings.add(this.settingsHighlighting);
        this.settings.add(this.settingsExceptionHandler);
        this.settings.add(this.settingsMemoryConfiguration);

        this.helpHelp = new JMenuItem(this.helpHelpAction);
        this.helpHelp.setIcon(this.loadIcon("Help16.png"));// "Help16.gif"));
        this.helpAbout = new JMenuItem(this.helpAboutAction);
        this.helpAbout.setIcon(this.loadIcon("MyBlank16.gif"));
        this.help.add(this.helpHelp);
        this.help.addSeparator();
        this.help.add(this.helpAbout);

        menuBar.add(this.file);
        menuBar.add(this.edit);
        menuBar.add(this.run);
        menuBar.add(this.settings);
        final JMenu toolMenu = ToolLoader.buildToolsMenu();
        if (toolMenu != null) {
            menuBar.add(toolMenu);
        }
        menuBar.add(this.help);

        // experiment with popup menu for settings. 3 Aug 2006 PS
        // setupPopupMenu();

        return menuBar;
    }

    /*
     * build the toolbar and connect items to action objects (which serve as action
     * listeners
     * shared between toolbar icon and corresponding menu item).
     */

    private JToolBar setUpToolBar() {
        final JToolBar toolBar = new JToolBar();

        this.New = new JButton(this.fileNewAction);
        this.New.setText("");
        this.Open = new JButton(this.fileOpenAction);
        this.Open.setText("");
        this.Save = new JButton(this.fileSaveAction);
        this.Save.setText("");
        this.SaveAs = new JButton(this.fileSaveAsAction);
        this.SaveAs.setText("");
        this.DumpMemory = new JButton(this.fileDumpMemoryAction);
        this.DumpMemory.setText("");

        this.Undo = new JButton(this.editUndoAction);
        this.Undo.setText("");
        this.Redo = new JButton(this.editRedoAction);
        this.Redo.setText("");
        this.Cut = new JButton(this.editCutAction);
        this.Cut.setText("");
        this.Copy = new JButton(this.editCopyAction);
        this.Copy.setText("");
        this.Paste = new JButton(this.editPasteAction);
        this.Paste.setText("");
        this.FindReplace = new JButton(this.editFindReplaceAction);
        this.FindReplace.setText("");
        this.SelectAll = new JButton(this.editSelectAllAction);
        this.SelectAll.setText("");

        this.Run = new JButton(this.runGoAction);
        this.Run.setText("");
        this.Assemble = new JButton(this.runAssembleAction);
        this.Assemble.setText("");
        this.Step = new JButton(this.runStepAction);
        this.Step.setText("");
        this.Backstep = new JButton(this.runBackstepAction);
        this.Backstep.setText("");
        this.Reset = new JButton(this.runResetAction);
        this.Reset.setText("");
        this.Stop = new JButton(this.runStopAction);
        this.Stop.setText("");
        this.Pause = new JButton(this.runPauseAction);
        this.Pause.setText("");
        this.Help = new JButton(this.helpHelpAction);
        this.Help.setText("");

        toolBar.add(this.New);
        toolBar.add(this.Open);
        toolBar.add(this.Save);
        toolBar.add(this.SaveAs);
        if (DumpFormatLoader.getDumpFormats().size() > 0) {
            toolBar.add(this.DumpMemory);
        }
        toolBar.add(new JToolBar.Separator());
        toolBar.add(this.Undo);
        toolBar.add(this.Redo);
        toolBar.add(this.Cut);
        toolBar.add(this.Copy);
        toolBar.add(this.Paste);
        toolBar.add(this.FindReplace);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(this.Assemble);
        toolBar.add(this.Run);
        toolBar.add(this.Step);
        toolBar.add(this.Backstep);
        toolBar.add(this.Pause);
        toolBar.add(this.Stop);
        toolBar.add(this.Reset);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(this.Help);
        toolBar.add(new JToolBar.Separator());

        return toolBar;
    }

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

    /**
     * <p>Setter for the field <code>menuState</code>.</p>
     *
     * @param status a int
     */
    public void setMenuState(final int status) {
        menuState = status;
        switch (status) {
            case FileStatus.NO_FILE:
                this.setMenuStateInitial();
                break;
            case FileStatus.NEW_NOT_EDITED:
                this.setMenuStateEditingNew();
                break;
            case FileStatus.NEW_EDITED:
                this.setMenuStateEditingNew();
                break;
            case FileStatus.NOT_EDITED:
                this.setMenuStateNotEdited(); // was MenuStateEditing. DPS 9-Aug-2011
                break;
            case FileStatus.EDITED:
                this.setMenuStateEditing();
                break;
            case FileStatus.RUNNABLE:
                this.setMenuStateRunnable();
                break;
            case FileStatus.RUNNING:
                this.setMenuStateRunning();
                break;
            case FileStatus.TERMINATED:
                this.setMenuStateTerminated();
                break;
            case FileStatus.OPENING:// This is a temporary state. DPS 9-Aug-2011
                break;
            default:
                System.out.println("Invalid File Status: " + status);
                break;
        }
    }

    private void setMenuStateInitial() {
        this.fileNewAction.setEnabled(true);
        this.fileOpenAction.setEnabled(true);
        this.fileCloseAction.setEnabled(false);
        this.fileCloseAllAction.setEnabled(false);
        this.fileSaveAction.setEnabled(false);
        this.fileSaveAsAction.setEnabled(false);
        this.fileSaveAllAction.setEnabled(false);
        this.fileDumpMemoryAction.setEnabled(false);
        this.fileExitAction.setEnabled(true);
        this.editUndoAction.setEnabled(false);
        this.editRedoAction.setEnabled(false);
        this.editCutAction.setEnabled(false);
        this.editCopyAction.setEnabled(false);
        this.editPasteAction.setEnabled(false);
        this.editFindReplaceAction.setEnabled(false);
        this.editSelectAllAction.setEnabled(false);
        this.settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        this.runAssembleAction.setEnabled(false);
        this.runGoAction.setEnabled(false);
        this.runStepAction.setEnabled(false);
        this.runBackstepAction.setEnabled(false);
        this.runResetAction.setEnabled(false);
        this.runStopAction.setEnabled(false);
        this.runPauseAction.setEnabled(false);
        this.runClearBreakpointsAction.setEnabled(false);
        this.runToggleBreakpointsAction.setEnabled(false);
        this.helpHelpAction.setEnabled(true);
        this.helpAboutAction.setEnabled(true);
        this.updateUndoAndRedoState();
    }

    /*
     * Added DPS 9-Aug-2011, for newly-opened files. Retain
     * existing Run menu state (except Assemble, which is always true).
     * Thus if there was a valid assembly it is retained.
     */
    private void setMenuStateNotEdited() {
        /* Note: undo and redo are handled separately by the undo manager */
        this.fileNewAction.setEnabled(true);
        this.fileOpenAction.setEnabled(true);
        this.fileCloseAction.setEnabled(true);
        this.fileCloseAllAction.setEnabled(true);
        this.fileSaveAction.setEnabled(true);
        this.fileSaveAsAction.setEnabled(true);
        this.fileSaveAllAction.setEnabled(true);
        this.fileDumpMemoryAction.setEnabled(false);
        this.fileExitAction.setEnabled(true);
        this.editCutAction.setEnabled(true);
        this.editCopyAction.setEnabled(true);
        this.editPasteAction.setEnabled(true);
        this.editFindReplaceAction.setEnabled(true);
        this.editSelectAllAction.setEnabled(true);
        this.settingsMemoryConfigurationAction.setEnabled(true);
        this.runAssembleAction.setEnabled(true);
        // If assemble-all, allow previous Run menu settings to remain.
        // Otherwise, clear them out. DPS 9-Aug-2011
        if (!Globals.getSettings().getBooleanSetting(Settings.Bool.ASSEMBLE_ALL)) {
            this.runGoAction.setEnabled(false);
            this.runStepAction.setEnabled(false);
            this.runBackstepAction.setEnabled(false);
            this.runResetAction.setEnabled(false);
            this.runStopAction.setEnabled(false);
            this.runPauseAction.setEnabled(false);
            this.runClearBreakpointsAction.setEnabled(false);
            this.runToggleBreakpointsAction.setEnabled(false);
        }
        this.helpHelpAction.setEnabled(true);
        this.helpAboutAction.setEnabled(true);
        this.updateUndoAndRedoState();
    }

    private void setMenuStateEditing() {
        /* Note: undo and redo are handled separately by the undo manager */
        this.fileNewAction.setEnabled(true);
        this.fileOpenAction.setEnabled(true);
        this.fileCloseAction.setEnabled(true);
        this.fileCloseAllAction.setEnabled(true);
        this.fileSaveAction.setEnabled(true);
        this.fileSaveAsAction.setEnabled(true);
        this.fileSaveAllAction.setEnabled(true);
        this.fileDumpMemoryAction.setEnabled(false);
        this.fileExitAction.setEnabled(true);
        this.editCutAction.setEnabled(true);
        this.editCopyAction.setEnabled(true);
        this.editPasteAction.setEnabled(true);
        this.editFindReplaceAction.setEnabled(true);
        this.editSelectAllAction.setEnabled(true);
        this.settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        this.runAssembleAction.setEnabled(true);
        this.runGoAction.setEnabled(false);
        this.runStepAction.setEnabled(false);
        this.runBackstepAction.setEnabled(false);
        this.runResetAction.setEnabled(false);
        this.runStopAction.setEnabled(false);
        this.runPauseAction.setEnabled(false);
        this.runClearBreakpointsAction.setEnabled(false);
        this.runToggleBreakpointsAction.setEnabled(false);
        this.helpHelpAction.setEnabled(true);
        this.helpAboutAction.setEnabled(true);
        this.updateUndoAndRedoState();
    }

    /*
     * Use this when "File -> New" is used
     */
    private void setMenuStateEditingNew() {
        /* Note: undo and redo are handled separately by the undo manager */
        this.fileNewAction.setEnabled(true);
        this.fileOpenAction.setEnabled(true);
        this.fileCloseAction.setEnabled(true);
        this.fileCloseAllAction.setEnabled(true);
        this.fileSaveAction.setEnabled(true);
        this.fileSaveAsAction.setEnabled(true);
        this.fileSaveAllAction.setEnabled(true);
        this.fileDumpMemoryAction.setEnabled(false);
        this.fileExitAction.setEnabled(true);
        this.editCutAction.setEnabled(true);
        this.editCopyAction.setEnabled(true);
        this.editPasteAction.setEnabled(true);
        this.editFindReplaceAction.setEnabled(true);
        this.editSelectAllAction.setEnabled(true);
        this.settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        this.runAssembleAction.setEnabled(false);
        this.runGoAction.setEnabled(false);
        this.runStepAction.setEnabled(false);
        this.runBackstepAction.setEnabled(false);
        this.runResetAction.setEnabled(false);
        this.runStopAction.setEnabled(false);
        this.runPauseAction.setEnabled(false);
        this.runClearBreakpointsAction.setEnabled(false);
        this.runToggleBreakpointsAction.setEnabled(false);
        this.helpHelpAction.setEnabled(true);
        this.helpAboutAction.setEnabled(true);
        this.updateUndoAndRedoState();
    }

    /*
     * Use this upon successful assemble or reset
     */
    private void setMenuStateRunnable() {
        /* Note: undo and redo are handled separately by the undo manager */
        this.fileNewAction.setEnabled(true);
        this.fileOpenAction.setEnabled(true);
        this.fileCloseAction.setEnabled(true);
        this.fileCloseAllAction.setEnabled(true);
        this.fileSaveAction.setEnabled(true);
        this.fileSaveAsAction.setEnabled(true);
        this.fileSaveAllAction.setEnabled(true);
        this.fileDumpMemoryAction.setEnabled(true);
        this.fileExitAction.setEnabled(true);
        this.editCutAction.setEnabled(true);
        this.editCopyAction.setEnabled(true);
        this.editPasteAction.setEnabled(true);
        this.editFindReplaceAction.setEnabled(true);
        this.editSelectAllAction.setEnabled(true);
        this.settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        this.runAssembleAction.setEnabled(true);
        this.runGoAction.setEnabled(true);
        this.runStepAction.setEnabled(true);
        this.runBackstepAction.setEnabled(
                Globals.getSettings().getBackSteppingEnabled() && !Globals.program.getBackStepper().empty());
        this.runResetAction.setEnabled(true);
        this.runStopAction.setEnabled(false);
        this.runPauseAction.setEnabled(false);
        this.runToggleBreakpointsAction.setEnabled(true);
        this.helpHelpAction.setEnabled(true);
        this.helpAboutAction.setEnabled(true);
        this.updateUndoAndRedoState();
    }

    /*
     * Use this while program is running
     */
    private void setMenuStateRunning() {
        /* Note: undo and redo are handled separately by the undo manager */
        this.fileNewAction.setEnabled(false);
        this.fileOpenAction.setEnabled(false);
        this.fileCloseAction.setEnabled(false);
        this.fileCloseAllAction.setEnabled(false);
        this.fileSaveAction.setEnabled(false);
        this.fileSaveAsAction.setEnabled(false);
        this.fileSaveAllAction.setEnabled(false);
        this.fileDumpMemoryAction.setEnabled(false);
        this.fileExitAction.setEnabled(false);
        this.editCutAction.setEnabled(false);
        this.editCopyAction.setEnabled(false);
        this.editPasteAction.setEnabled(false);
        this.editFindReplaceAction.setEnabled(false);
        this.editSelectAllAction.setEnabled(false);
        this.settingsMemoryConfigurationAction.setEnabled(false); // added 21 July 2009
        this.runAssembleAction.setEnabled(false);
        this.runGoAction.setEnabled(false);
        this.runStepAction.setEnabled(false);
        this.runBackstepAction.setEnabled(false);
        this.runResetAction.setEnabled(false);
        this.runStopAction.setEnabled(true);
        this.runPauseAction.setEnabled(true);
        this.runToggleBreakpointsAction.setEnabled(false);
        this.helpHelpAction.setEnabled(true);
        this.helpAboutAction.setEnabled(true);
        this.editUndoAction.setEnabled(false);// updateUndoState(); // DPS 10 Jan 2008
        this.editRedoAction.setEnabled(false);// updateRedoState(); // DPS 10 Jan 2008
    }

    /*
     * Use this upon completion of execution
     */
    private void setMenuStateTerminated() {
        /* Note: undo and redo are handled separately by the undo manager */
        this.fileNewAction.setEnabled(true);
        this.fileOpenAction.setEnabled(true);
        this.fileCloseAction.setEnabled(true);
        this.fileCloseAllAction.setEnabled(true);
        this.fileSaveAction.setEnabled(true);
        this.fileSaveAsAction.setEnabled(true);
        this.fileSaveAllAction.setEnabled(true);
        this.fileDumpMemoryAction.setEnabled(true);
        this.fileExitAction.setEnabled(true);
        this.editCutAction.setEnabled(true);
        this.editCopyAction.setEnabled(true);
        this.editPasteAction.setEnabled(true);
        this.editFindReplaceAction.setEnabled(true);
        this.editSelectAllAction.setEnabled(true);
        this.settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        this.runAssembleAction.setEnabled(true);
        this.runGoAction.setEnabled(false);
        this.runStepAction.setEnabled(false);
        this.runBackstepAction.setEnabled(
                Globals.getSettings().getBackSteppingEnabled() && !Globals.program.getBackStepper().empty());
        this.runResetAction.setEnabled(true);
        this.runStopAction.setEnabled(false);
        this.runPauseAction.setEnabled(false);
        this.runToggleBreakpointsAction.setEnabled(true);
        this.helpHelpAction.setEnabled(true);
        this.helpAboutAction.setEnabled(true);
        this.updateUndoAndRedoState();
    }

    /**
     * Get current menu state. State values are constants in FileStatus class. DPS
     * 23 July 2008
     *
     * @return current menu state.
     */
    public static int getMenuState() {
        return menuState;
    }

    /**
     * To set whether the register values are reset.
     *
     * @param b Boolean true if the register values have been reset.
     */
    public void setReset(final boolean b) {
        this.reset = b;
    }

    /**
     * To set whether MIPS program execution has started.
     *
     * @param b true if the MIPS program execution has started.
     */
    public void setStarted(final boolean b) {
        this.started = b;
    }

    /**
     * To find out whether the register values are reset.
     *
     * @return Boolean true if the register values have been reset.
     */
    public boolean getReset() {
        return this.reset;
    }

    /**
     * To find out whether MIPS program is currently executing.
     *
     * @return true if MIPS program is currently executing.
     */
    public boolean getStarted() {
        return this.started;
    }

    /**
     * Get reference to Editor object associated with this GUI.
     *
     * @return Editor for the GUI.
     */
    public Editor getEditor() {
        return this.editor;
    }

    /**
     * Get reference to messages pane associated with this GUI.
     *
     * @return MessagesPane object associated with the GUI.
     */
    public MainPane getMainPane() {
        return this.mainPane;
    }

    /**
     * Get reference to messages pane associated with this GUI.
     *
     * @return MessagesPane object associated with the GUI.
     */
    public MessagesPane getMessagesPane() {
        return this.messagesPane;
    }

    /**
     * Get reference to registers pane associated with this GUI.
     *
     * @return RegistersPane object associated with the GUI.
     */
    public RegistersPane getRegistersPane() {
        return this.registersPane;
    }

    /**
     * Get reference to settings menu item for display base of memory/register
     * values.
     *
     * @return the menu item
     */
    public JCheckBoxMenuItem getValueDisplayBaseMenuItem() {
        return this.settingsValueDisplayBase;
    }

    /**
     * Get reference to settings menu item for display base of memory/register
     * values.
     *
     * @return the menu item
     */
    public JCheckBoxMenuItem getAddressDisplayBaseMenuItem() {
        return this.settingsAddressDisplayBase;
    }

    /**
     * Return reference tothe Run->Assemble item's action. Needed by File->Open in
     * case
     * assemble-upon-open flag is set.
     *
     * @return the Action object for the Run->Assemble operation.
     */
    public Action getRunAssembleAction() {
        return this.runAssembleAction;
    }

    /**
     * Have the menu request keyboard focus. DPS 5-4-10
     */
    void haveMenuRequestFocus() {
        this.menu.requestFocus();
    }

    /**
     * Send keyboard event to menu for possible processing. DPS 5-4-10
     *
     * @param evt KeyEvent for menu component to consider for processing.
     */
    public void dispatchEventToMenu(final KeyEvent evt) {
        this.menu.dispatchEvent(evt);
    }

    /**
     * <p>updateUndoAndRedoState.</p>
     */
    void updateUndoAndRedoState() {
        final EditPane editPane = this.getMainPane().getEditPane();
        this.editUndoAction.setEnabled(editPane != null && editPane.getUndoManager().canUndo());
        this.editRedoAction.setEnabled(editPane != null && editPane.getUndoManager().canRedo());
    }

    private ImageIcon loadIcon(final String name) {
        final var resource = this.getClass().getResource(Globals.imagesPath + name);
        return new ImageIcon(
                Toolkit.getDefaultToolkit().getImage(resource));
    }

//    private FlatSVGIcon loadSVGIcon(final String name) {
//        return new FlatSVGIcon(this.getClass().getResource(Globals.imagesPath + name));
//    }

    private KeyStroke makeShortcut(final int key) {
        return KeyStroke.getKeyStroke(key, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }
}
