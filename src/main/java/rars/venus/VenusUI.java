package rars.venus;

//import com.formdev.flatlaf.extras.FlatSVGIcon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rars.Globals;
import rars.Settings;
import rars.riscv.Instructions;
import rars.riscv.dump.DumpFormatLoader;
import rars.settings.BoolSetting;
import rars.simulator.Simulator;
import rars.venus.registers.ControlAndStatusWindow;
import rars.venus.registers.FloatingPointWindow;
import rars.venus.registers.RegistersPane;
import rars.venus.registers.RegistersWindow;
import rars.venus.run.*;
import rars.venus.settings.*;

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
    private static final Logger LOGGER = LogManager.getLogger(VenusUI.class);
    public final JMenuBar menu;
    private final VenusUI mainUI;
    private final MainPane mainPane;
    private final RegistersPane registersPane;
    private final RegistersWindow registersTab;
    private final ControlAndStatusWindow csrTab;
    private final MessagesPane messagesPane;
    private final Editor editor;
    // PLEASE PUT THESE TWO (& THEIR METHODS) SOMEWHERE THEY BELONG, NOT HERE
    private boolean reset = true; // registers/memory reset for execution
    private boolean started = false; // started execution

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
            VenusUI.LOGGER.fatal("Internal Error: images folder or file not found.");
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
        final FloatingPointWindow fpTab = new FloatingPointWindow();
        this.csrTab = new ControlAndStatusWindow();
        this.registersPane = new RegistersPane(this.mainUI, this.registersTab, fpTab, this.csrTab);
        this.registersPane.setPreferredSize(registersPanePreferredSize);

        this.mainPane = new MainPane(this.mainUI, this.editor, this.registersTab, fpTab, this.csrTab);

        this.mainPane.setPreferredSize(mainPanePreferredSize);
        this.messagesPane = new MessagesPane();
        this.messagesPane.setPreferredSize(messagesPanePreferredSize);
        final JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.mainPane, this.messagesPane);
        splitter.setOneTouchExpandable(true);
        splitter.resetToPreferredSizes();
        final var horizontalLayout = new JPanel(new BorderLayout());
        horizontalLayout.add(splitter, BorderLayout.CENTER);
        horizontalLayout.add(this.registersPane, BorderLayout.EAST);

        // due to dependencies, do not set up menu/toolbar until now.
        this.createActionObjects();
        this.menu = this.setUpMenuBar();
        this.setJMenuBar(this.menu);

        final JToolBar toolbar = this.setUpToolBar();

        final JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp.add(toolbar);
        jp.add(RunSpeedPanel.getInstance());
        final JPanel center = new JPanel(new BorderLayout());
        center.add(jp, BorderLayout.NORTH);
        center.add(horizontalLayout);

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
            VenusUI.LOGGER.fatal("Internal Error: could not open files {}", String.join(", ", paths));
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

    /*
     * build the toolbar and connect items to action objects (which serve as action
     * listeners
     * shared between toolbar icon and corresponding menu item).
     */

    /**
     * <p>Setter for the field <code>menuState</code>.</p>
     *
     * @param status a int
     */
    public void setMenuState(final int status) {
        switch (status) {
            case FileStatus.NO_FILE:
                this.setMenuStateInitial();
                break;
            case FileStatus.NEW_NOT_EDITED, FileStatus.NEW_EDITED:
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
                VenusUI.LOGGER.error("Invalid File Status: {}", status);
                break;
        }
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
                    "Create a new file for editing", KeyEvent.VK_N, VenusUI.makeShortcut(KeyEvent.VK_N)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.editor.newFile();
                }
            };
            this.fileOpenAction = new GuiAction("Open ...", this.loadIcon("Open22.png"),
                    "Open a file for editing", KeyEvent.VK_O, VenusUI.makeShortcut(KeyEvent.VK_O)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.editor.open();
                }
            };
            this.fileCloseAction = new GuiAction("Close", null, "Close the current file", KeyEvent.VK_C,
                    VenusUI.makeShortcut(KeyEvent.VK_W)) {
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
                    KeyEvent.VK_S, VenusUI.makeShortcut(KeyEvent.VK_S)) {
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
                    "Dump machine code or data in an available format", KeyEvent.VK_D, VenusUI.makeShortcut(KeyEvent.VK_D),
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
                    KeyEvent.VK_U, VenusUI.makeShortcut(KeyEvent.VK_Z)) {
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
                    KeyEvent.VK_R, VenusUI.makeShortcut(KeyEvent.VK_Y)) {
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
                    VenusUI.makeShortcut(KeyEvent.VK_X)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.mainPane.getEditPane().cutText();
                }
            };
            this.editCopyAction = new GuiAction("Copy", this.loadIcon("Copy22.png"), "Copy", KeyEvent.VK_O,
                    VenusUI.makeShortcut(KeyEvent.VK_C)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.mainPane.getEditPane().copyText();
                }
            };
            this.editPasteAction = new GuiAction("Paste", this.loadIcon("Paste22.png"), "Paste", KeyEvent.VK_P,
                    VenusUI.makeShortcut(KeyEvent.VK_V)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    VenusUI.this.mainPane.getEditPane().pasteText();
                }
            };
            this.editFindReplaceAction = new EditFindReplaceAction("Find/Replace", this.loadIcon("Find22.png"),
                    "Find/Replace", KeyEvent.VK_F, VenusUI.makeShortcut(KeyEvent.VK_F), this.mainUI);
            this.editSelectAllAction = new GuiAction("Select All",
                    null, // new ImageIcon(tk.getImage(cs.getResource(Globals.imagesPath+"Find22.png"),
                    "Select All", KeyEvent.VK_A,
                    VenusUI.makeShortcut(KeyEvent.VK_A)) {
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
                    VenusUI.makeShortcut(KeyEvent.VK_K));
            this.runToggleBreakpointsAction = new GuiAction("Toggle all breakpoints", null,
                    "Disable/enable all breakpoints without clearing (can also click Bkpt column header)",
                    KeyEvent.VK_T, VenusUI.makeShortcut(KeyEvent.VK_T)) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    // settingsLabelAction.actionPerformed(e);
                    VenusUI.this.mainPane.getExecutePane().getTextSegmentWindow().toggleBreakpoints();
                }
            };
            this.settingsLabelAction = new SettingsAction("Show Labels Window (symbol table)",
                    "Toggle visibility of Labels window (symbol table) in the Execute tab",
                    BoolSetting.LABEL_WINDOW_VISIBILITY, (value) -> {
                VenusUI.this.mainPane.getExecutePane().setLabelWindowVisibility(value);
                VenusUI.LOGGER.info("ExecutePane reference 2");
            });
            this.settingsPopupInputAction = new SettingsAction("Popup dialog for input syscalls (5,6,7,8,12)",
                    "If set, use popup dialog for input syscalls (5,6,7,8,12) instead of cursor in Run I/O window",
                    BoolSetting.POPUP_SYSCALL_INPUT, (v) -> {
            });

            this.settingsValueDisplayBaseAction = new SettingsAction("Values displayed in hexadecimal",
                    "Toggle between hexadecimal and decimal display of memory/register values",
                    BoolSetting.DISPLAY_VALUES_IN_HEX,
                    (isHex) -> VenusUI.this.mainPane.getExecutePane().getValueDisplayBaseChooser().setSelected(isHex));
            this.settingsAddressDisplayBaseAction = new SettingsAction("Addresses displayed in hexadecimal",
                    "Toggle between hexadecimal and decimal display of memory addresses",
                    BoolSetting.DISPLAY_ADDRESSES_IN_HEX, (isHex) ->
                    VenusUI.this.mainPane.getExecutePane().getAddressDisplayBaseChooser().setSelected(isHex));
            this.settingsExtendedAction = new SettingsAction("Permit extended (pseudo) instructions and formats",
                    "If set, extended (pseudo) instructions are formats are permitted.",
                    BoolSetting.EXTENDED_ASSEMBLER_ENABLED);
            this.settingsAssembleOnOpenAction = new SettingsAction("Assemble file upon opening",
                    "If set, a file will be automatically assembled as soon as it is opened.  File Open dialog will show most recently opened file.",
                    BoolSetting.ASSEMBLE_ON_OPEN);
            this.settingsAssembleAllAction = new SettingsAction("Assemble all files in directory",
                    "If set, all files in current directory will be assembled when Assemble operation is selected.",
                    BoolSetting.ASSEMBLE_ALL);
            this.settingsAssembleOpenAction = new SettingsAction("Assemble all files currently open",
                    "If set, all files currently open for editing will be assembled when Assemble operation is selected.",
                    BoolSetting.ASSEMBLE_OPEN);
            this.settingsWarningsAreErrorsAction = new SettingsAction("Assembler warnings are considered errors",
                    "If set, assembler warnings will be interpreted as errors and prevent successful assembly.",
                    BoolSetting.WARNINGS_ARE_ERRORS);
            this.settingsStartAtMainAction = new SettingsAction("Initialize Program Counter to global 'main' if defined",
                    "If set, assembler will initialize Program Counter to text address globally labeled 'main', if defined.",
                    BoolSetting.START_AT_MAIN);
            this.settingsProgramArgumentsAction = new SettingsAction("Program arguments provided to program",
                    "If set, program arguments for the program can be entered in border of Text Segment window.",
                    BoolSetting.PROGRAM_ARGUMENTS, (selected) -> {
                if (selected) {
                    VenusUI.this.mainPane.getExecutePane().getTextSegmentWindow().addProgramArgumentsPanel();
                } else {
                    VenusUI.this.mainPane.getExecutePane().getTextSegmentWindow().removeProgramArgumentsPanel();
                }
            })
            ;
            this.settingsSelfModifyingCodeAction = new SettingsAction("Self-modifying code",
                    "If set, the program can write and branch to both text and data segments.",
                    BoolSetting.SELF_MODIFYING_CODE_ENABLED);

            // TODO: review this
            this.settingsRV64Action = new SettingsAction("64 bit",
                    "If set, registers are 64 bits wide and new instructions are available",
                    BoolSetting.RV64_ENABLED, (isRV64) -> {
                Instructions.RV64 = isRV64;
                VenusUI.this.registersTab.updateRegisters();
                VenusUI.this.csrTab.updateRegisters();
            });
            this.settingsDeriveCurrentWorkingDirectoryAction = new SettingsAction("Derive current working directory",
                    "If set, the working directory is derived from the main file instead of the RARS executable directory.",
                    BoolSetting.DERIVE_CURRENT_WORKING_DIRECTORY);

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
            VenusUI.LOGGER.fatal("Internal Error: images folder not found, or other null pointer exception while creating Action objects", e);
            System.exit(0);
        }
    }

    private JMenuBar setUpMenuBar() {
        UIManager.put("TitlePane.menuBarEmbedded", true);
        final JMenuBar menuBar = new JMenuBar();
//        FlatLaf.revalidateAndRepaintAllFramesAndDialogs();
        // components of the menubar
        final JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        final JMenu edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);
        final JMenu run = new JMenu("Run");
        run.setMnemonic(KeyEvent.VK_R);
        // window = new JMenu("Window");
        // window.setMnemonic(KeyEvent.VK_W);
        final JMenu settings = new JMenu("Settings");
        settings.setMnemonic(KeyEvent.VK_S);
        final JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        // slight bug: user typing alt-H activates help menu item directly, not help
        // menu

        final JMenuItem fileNew = new JMenuItem(this.fileNewAction);
        fileNew.setIcon(this.loadIcon("New16.png"));
        final JMenuItem fileOpen = new JMenuItem(this.fileOpenAction);
        fileOpen.setIcon(this.loadIcon("Open16.png"));
        final JMenuItem fileClose = new JMenuItem(this.fileCloseAction);
        fileClose.setIcon(this.loadIcon("MyBlank16.gif"));
        final JMenuItem fileCloseAll = new JMenuItem(this.fileCloseAllAction);
        fileCloseAll.setIcon(this.loadIcon("MyBlank16.gif"));
        final JMenuItem fileSave = new JMenuItem(this.fileSaveAction);
        fileSave.setIcon(this.loadIcon("Save16.png"));
        final JMenuItem fileSaveAs = new JMenuItem(this.fileSaveAsAction);
        fileSaveAs.setIcon(this.loadIcon("SaveAs16.png"));
        final JMenuItem fileSaveAll = new JMenuItem(this.fileSaveAllAction);
        fileSaveAll.setIcon(this.loadIcon("MyBlank16.gif"));
        final JMenuItem fileDumpMemory = new JMenuItem(this.fileDumpMemoryAction);
        fileDumpMemory.setIcon(this.loadIcon("Dump16.png"));
        final JMenuItem fileExit = new JMenuItem(this.fileExitAction);
        fileExit.setIcon(this.loadIcon("MyBlank16.gif"));
        file.add(fileNew);
        file.add(fileOpen);
        file.add(fileClose);
        file.add(fileCloseAll);
        file.addSeparator();
        file.add(fileSave);
        file.add(fileSaveAs);
        file.add(fileSaveAll);
        if (!DumpFormatLoader.getDumpFormats().isEmpty()) {
            file.add(fileDumpMemory);
        }
        file.addSeparator();
        file.add(fileExit);

        final JMenuItem editUndo = new JMenuItem(this.editUndoAction);
        editUndo.setIcon(this.loadIcon("Undo16.png"));// "Undo16.gif"));
        final JMenuItem editRedo = new JMenuItem(this.editRedoAction);
        editRedo.setIcon(this.loadIcon("Redo16.png"));// "Redo16.gif"));
        final JMenuItem editCut = new JMenuItem(this.editCutAction);
        editCut.setIcon(this.loadIcon("Cut16.gif"));
        final JMenuItem editCopy = new JMenuItem(this.editCopyAction);
        editCopy.setIcon(this.loadIcon("Copy16.png"));// "Copy16.gif"));
        final JMenuItem editPaste = new JMenuItem(this.editPasteAction);
        editPaste.setIcon(this.loadIcon("Paste16.png"));// "Paste16.gif"));
        final JMenuItem editFindReplace = new JMenuItem(this.editFindReplaceAction);
        editFindReplace.setIcon(this.loadIcon("Find16.png"));// "Paste16.gif"));
        final JMenuItem editSelectAll = new JMenuItem(this.editSelectAllAction);
        editSelectAll.setIcon(this.loadIcon("MyBlank16.gif"));
        edit.add(editUndo);
        edit.add(editRedo);
        edit.addSeparator();
        edit.add(editCut);
        edit.add(editCopy);
        edit.add(editPaste);
        edit.addSeparator();
        edit.add(editFindReplace);
        edit.add(editSelectAll);

        final JMenuItem runAssemble = new JMenuItem(this.runAssembleAction);
        runAssemble.setIcon(this.loadIcon("Assemble16.png"));// "MyAssemble16.gif"));
        final JMenuItem runGo = new JMenuItem(this.runGoAction);
        runGo.setIcon(this.loadIcon("Play16.png"));// "Play16.gif"));
        final JMenuItem runStep = new JMenuItem(this.runStepAction);
        runStep.setIcon(this.loadIcon("StepForward16.png"));// "MyStepForward16.gif"));
        final JMenuItem runBackstep = new JMenuItem(this.runBackstepAction);
        runBackstep.setIcon(this.loadIcon("StepBack16.png"));// "MyStepBack16.gif"));
        final JMenuItem runReset = new JMenuItem(this.runResetAction);
        runReset.setIcon(this.loadIcon("Reset16.png"));// "MyReset16.gif"));
        final JMenuItem runStop = new JMenuItem(this.runStopAction);
        runStop.setIcon(this.loadIcon("Stop16.png"));// "Stop16.gif"));
        final JMenuItem runPause = new JMenuItem(this.runPauseAction);
        runPause.setIcon(this.loadIcon("Pause16.png"));// "Pause16.gif"));
        final JMenuItem runClearBreakpoints = new JMenuItem(this.runClearBreakpointsAction);
        runClearBreakpoints.setIcon(this.loadIcon("MyBlank16.gif"));
        final JMenuItem runToggleBreakpoints = new JMenuItem(this.runToggleBreakpointsAction);
        runToggleBreakpoints.setIcon(this.loadIcon("MyBlank16.gif"));

        run.add(runAssemble);
        run.add(runGo);
        run.add(runStep);
        run.add(runBackstep);
        run.add(runPause);
        run.add(runStop);
        run.add(runReset);
        run.addSeparator();
        run.add(runClearBreakpoints);
        run.add(runToggleBreakpoints);

        final JCheckBoxMenuItem settingsLabel = new JCheckBoxMenuItem(this.settingsLabelAction);
        settingsLabel.setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.LABEL_WINDOW_VISIBILITY));
        final JCheckBoxMenuItem settingsPopupInput = new JCheckBoxMenuItem(this.settingsPopupInputAction);
        settingsPopupInput.setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.POPUP_SYSCALL_INPUT));
        final JCheckBoxMenuItem settingsValueDisplayBase = new JCheckBoxMenuItem(this.settingsValueDisplayBaseAction);
        settingsValueDisplayBase
                .setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.DISPLAY_VALUES_IN_HEX));// mainPane.getExecutePane().getValueDisplayBaseChooser().isSelected());
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has
        // already been created.
        this.mainPane.getExecutePane().getValueDisplayBaseChooser().setSettingsMenuItem(settingsValueDisplayBase);
        final JCheckBoxMenuItem settingsAddressDisplayBase = new JCheckBoxMenuItem(this.settingsAddressDisplayBaseAction);
        settingsAddressDisplayBase
                .setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.DISPLAY_ADDRESSES_IN_HEX));// mainPane.getExecutePane().getValueDisplayBaseChooser().isSelected());
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has
        // already been created.
        this.mainPane.getExecutePane().getAddressDisplayBaseChooser().setSettingsMenuItem(settingsAddressDisplayBase);
        final JCheckBoxMenuItem settingsExtended = new JCheckBoxMenuItem(this.settingsExtendedAction);
        settingsExtended.setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.EXTENDED_ASSEMBLER_ENABLED));
        final JCheckBoxMenuItem settingsSelfModifyingCode = new JCheckBoxMenuItem(this.settingsSelfModifyingCodeAction);
        settingsSelfModifyingCode
                .setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED));
        final JCheckBoxMenuItem settingsRV64 = new JCheckBoxMenuItem(this.settingsRV64Action);
        settingsRV64.setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.RV64_ENABLED));
        final JCheckBoxMenuItem settingsDeriveCurrentWorkingDirectory = new JCheckBoxMenuItem(this.settingsDeriveCurrentWorkingDirectoryAction);
        settingsDeriveCurrentWorkingDirectory
                .setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.DERIVE_CURRENT_WORKING_DIRECTORY));
        final JCheckBoxMenuItem settingsAssembleOnOpen = new JCheckBoxMenuItem(this.settingsAssembleOnOpenAction);
        settingsAssembleOnOpen.setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.ASSEMBLE_ON_OPEN));
        final JCheckBoxMenuItem settingsAssembleAll = new JCheckBoxMenuItem(this.settingsAssembleAllAction);
        settingsAssembleAll.setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.ASSEMBLE_ALL));
        final JCheckBoxMenuItem settingsAssembleOpen = new JCheckBoxMenuItem(this.settingsAssembleOpenAction);
        settingsAssembleOpen.setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.ASSEMBLE_OPEN));
        final JCheckBoxMenuItem settingsWarningsAreErrors = new JCheckBoxMenuItem(this.settingsWarningsAreErrorsAction);
        settingsWarningsAreErrors
                .setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.WARNINGS_ARE_ERRORS));
        final JCheckBoxMenuItem settingsStartAtMain = new JCheckBoxMenuItem(this.settingsStartAtMainAction);
        settingsStartAtMain.setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.START_AT_MAIN));
        final JCheckBoxMenuItem settingsProgramArguments = new JCheckBoxMenuItem(this.settingsProgramArgumentsAction);
        settingsProgramArguments.setSelected(Globals.getSettings().getBoolSettings().getSetting(BoolSetting.PROGRAM_ARGUMENTS));
        final JMenuItem settingsEditor = new JMenuItem(this.settingsEditorAction);
        final JMenuItem settingsHighlighting = new JMenuItem(this.settingsHighlightingAction);
        final JMenuItem settingsExceptionHandler = new JMenuItem(this.settingsExceptionHandlerAction);
        final JMenuItem settingsMemoryConfiguration = new JMenuItem(this.settingsMemoryConfigurationAction);

        settings.add(settingsLabel);
        settings.add(settingsProgramArguments);
        settings.add(settingsPopupInput);
        settings.add(settingsAddressDisplayBase);
        settings.add(settingsValueDisplayBase);
        settings.addSeparator();
        settings.add(settingsAssembleOnOpen);
        settings.add(settingsAssembleAll);
        settings.add(settingsAssembleOpen);
        settings.add(settingsWarningsAreErrors);
        settings.add(settingsStartAtMain);
        settings.add(settingsDeriveCurrentWorkingDirectory);
        settings.addSeparator();
        settings.add(settingsExtended);
        settings.add(settingsSelfModifyingCode);
        settings.add(settingsRV64);
        settings.addSeparator();
        settings.add(settingsEditor);
        settings.add(settingsHighlighting);
        settings.add(settingsExceptionHandler);
        settings.add(settingsMemoryConfiguration);

        final JMenuItem helpHelp = new JMenuItem(this.helpHelpAction);
        helpHelp.setIcon(this.loadIcon("Help16.png"));// "Help16.gif"));
        final JMenuItem helpAbout = new JMenuItem(this.helpAboutAction);
        helpAbout.setIcon(this.loadIcon("MyBlank16.gif"));
        help.add(helpHelp);
        help.addSeparator();
        help.add(helpAbout);

        menuBar.add(file);
        menuBar.add(edit);
        menuBar.add(run);
        menuBar.add(settings);
        final JMenu toolMenu = ToolLoader.buildToolsMenu();
        menuBar.add(toolMenu);
        menuBar.add(help);

        // experiment with popup menu for settings. 3 Aug 2006 PS
        // setupPopupMenu();

        return menuBar;
    }

    private JToolBar setUpToolBar() {
        final JToolBar toolBar = new JToolBar();

        final JButton aNew = new JButton(this.fileNewAction);
        aNew.setText("");
        final JButton open = new JButton(this.fileOpenAction);
        open.setText("");
        final JButton save = new JButton(this.fileSaveAction);
        save.setText("");
        final JButton saveAs = new JButton(this.fileSaveAsAction);
        saveAs.setText("");
        final JButton dumpMemory = new JButton(this.fileDumpMemoryAction);
        dumpMemory.setText("");

        // components of the toolbar
        final JButton undo = new JButton(this.editUndoAction);
        undo.setText("");
        final JButton redo = new JButton(this.editRedoAction);
        redo.setText("");
        final JButton cut = new JButton(this.editCutAction);
        cut.setText("");
        final JButton copy = new JButton(this.editCopyAction);
        copy.setText("");
        final JButton paste = new JButton(this.editPasteAction);
        paste.setText("");
        final JButton findReplace = new JButton(this.editFindReplaceAction);
        findReplace.setText("");
        final JButton selectAll = new JButton(this.editSelectAllAction);
        selectAll.setText("");

        final JButton run1 = new JButton(this.runGoAction);
        run1.setText("");
        final JButton assemble = new JButton(this.runAssembleAction);
        assemble.setText("");
        final JButton step = new JButton(this.runStepAction);
        step.setText("");
        final JButton backstep = new JButton(this.runBackstepAction);
        backstep.setText("");
        final JButton reset1 = new JButton(this.runResetAction);
        reset1.setText("");
        final JButton stop = new JButton(this.runStopAction);
        stop.setText("");
        final JButton pause = new JButton(this.runPauseAction);
        pause.setText("");
        final JButton help1 = new JButton(this.helpHelpAction);
        help1.setText("");

        toolBar.add(aNew);
        toolBar.add(open);
        toolBar.add(save);
        toolBar.add(saveAs);
        if (!DumpFormatLoader.getDumpFormats().isEmpty()) {
            toolBar.add(dumpMemory);
        }
        toolBar.add(new JToolBar.Separator());
        toolBar.add(undo);
        toolBar.add(redo);
        toolBar.add(cut);
        toolBar.add(copy);
        toolBar.add(paste);
        toolBar.add(findReplace);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(assemble);
        toolBar.add(run1);
        toolBar.add(step);
        toolBar.add(backstep);
        toolBar.add(pause);
        toolBar.add(stop);
        toolBar.add(reset1);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(help1);
        toolBar.add(new JToolBar.Separator());

        return toolBar;
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
        if (!Globals.getSettings().getBoolSettings().getSetting(BoolSetting.ASSEMBLE_ALL)) {
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
                Settings.getBackSteppingEnabled() && !Globals.program.getBackStepper().empty());
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
                Settings.getBackSteppingEnabled() && !Globals.program.getBackStepper().empty());
        this.runResetAction.setEnabled(true);
        this.runStopAction.setEnabled(false);
        this.runPauseAction.setEnabled(false);
        this.runToggleBreakpointsAction.setEnabled(true);
        this.helpHelpAction.setEnabled(true);
        this.helpAboutAction.setEnabled(true);
        this.updateUndoAndRedoState();
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
     * To set whether the register values are reset.
     *
     * @param b Boolean true if the register values have been reset.
     */
    public void setReset(final boolean b) {
        this.reset = b;
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
     * To set whether MIPS program execution has started.
     *
     * @param b true if the MIPS program execution has started.
     */
    public void setStarted(final boolean b) {
        this.started = b;
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
        this.editUndoAction.setEnabled(editPane != null && editPane.canUndo());
        this.editRedoAction.setEnabled(editPane != null && editPane.canRedo());
    }

//    private FlatSVGIcon loadSVGIcon(final String name) {
//        return new FlatSVGIcon(this.getClass().getResource(Globals.imagesPath + name));
//    }

    private ImageIcon loadIcon(final String name) {
        final var resource = this.getClass().getResource(Globals.imagesPath + name);
        return new ImageIcon(
                Toolkit.getDefaultToolkit().getImage(resource));
    }
}
