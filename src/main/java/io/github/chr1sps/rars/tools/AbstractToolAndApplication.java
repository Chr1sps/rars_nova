package io.github.chr1sps.rars.tools;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.RISCVprogram;
import io.github.chr1sps.rars.Settings;
import io.github.chr1sps.rars.exceptions.AddressErrorException;
import io.github.chr1sps.rars.exceptions.AssemblyException;
import io.github.chr1sps.rars.notices.AccessNotice;
import io.github.chr1sps.rars.notices.SimulatorNotice;
import io.github.chr1sps.rars.riscv.hardware.*;
import io.github.chr1sps.rars.simulator.Simulator;
import io.github.chr1sps.rars.util.FilenameFinder;
import io.github.chr1sps.rars.util.SimpleSubscriber;
import io.github.chr1sps.rars.venus.run.RunSpeedPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Flow;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * An abstract class that provides generic components to facilitate
 * implementation of
 * a Tool and/or stand-alone Rars-based application. Provides default
 * definitions
 * of both the action() method required to implement Tool and the go() method
 * conventionally used to launch a Rars-based stand-alone application. It also
 * provides
 * generic definitions for interactively controlling the application. The
 * generic controls
 * for RarsTools are 3 buttons: connect/disconnect to a resource (memory and/or
 * registers), reset, and close (exit). The generic controls for stand-alone
 * Rars apps
 * include: button that triggers a file open dialog, a text field to display
 * status
 * messages, the run-speed slider to control execution rate when running a
 * program,
 * a button that assembles and runs the current program, a button to interrupt
 * the running program, a reset button, and an exit button.
 * Pete Sanderson, 14 November 2006.
 */

public abstract class AbstractToolAndApplication extends JFrame implements Tool, SimpleSubscriber<AccessNotice> {
    protected boolean isBeingUsedAsATool = false; // can use to determine whether invoked as Tool or stand-alone.
    private JDialog dialog; // used only for Tool use. This is the pop-up dialog that appears when menu item
    // selected.
    protected Window theWindow; // highest level GUI component (a JFrame for app, a JDialog for Tool)

    // Major GUI components
    private JLabel headingLabel;
    private final String title; // descriptive title for title bar provided to constructor.
    private final String heading; // Text to be displayed in the top portion of the main window.

    // Some GUI settings
    private final EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
    private final Color backgroundColor = Color.WHITE;

    private final int lowMemoryAddress = Memory.dataSegmentBaseAddress;
    private final int highMemoryAddress = Memory.stackBaseAddress;
    // For Tool, is set true when "Connect" clicked, false when "Disconnect"
    // clicked.
    // For app, is set true when "Assemble and Run" clicked, false when program
    // terminates.
    private volatile boolean observing = false;

    // Several structures required for stand-alone use only (not Tool use)
    private File mostRecentlyOpenedFile = null;
    private final Runnable interactiveGUIUpdater = new GUIUpdater();
    private MessageField operationStatusMessages;
    private JButton openFileButton, assembleRunButton, stopButton;
    private boolean multiFileAssemble = false;

    // Structure required for Tool use only (not stand-alone use). Want subclasses
    // to have access.
    protected ConnectButton connectButton;

    /**
     * Simple constructor
     *
     * @param title   String containing title bar text
     * @param heading a {@link java.lang.String} object
     */
    protected AbstractToolAndApplication(final String title, final String heading) {
        this.title = title;
        this.heading = heading;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// ABSTRACT METHODS ///////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Required Tool method to return Tool name. Must be defined by subclass.
     *
     * @return Tool name. RARS will display this in menu item.
     */
    @Override
    public abstract String getName();

    /**
     * Abstract method that must be instantiated by subclass to build the main
     * display area
     * of the GUI. It will be placed in the CENTER area of a BorderLayout. The title
     * is in the NORTH area, and the controls are in the SOUTH area.
     *
     * @return a {@link javax.swing.JComponent} object
     */
    protected abstract JComponent buildMainDisplayArea();

    //////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// METHODS WITH DEFAULT IMPLEMENTATIONS
    ////////////////////////////////////////////////////////////////////////////////////// //////////////////
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Run the simulator as stand-alone application. For this default
     * implementation,
     * the user-defined main display of the user interface is identical for both
     * stand-alone
     * and RARS Tools menu use, but the control buttons are different because the
     * stand-alone
     * must include a mechansim for controlling the opening, assembling, and
     * executing of
     * an underlying source program. The generic controls include: a button that
     * triggers a
     * file open dialog, a text field to display status messages, the run-speed
     * slider
     * to control execution rate when running a program, a button that assembles and
     * runs the current source program, a reset button, and an exit button.
     * This method calls 3 methods that can be defined/overriden in the subclass:
     * initializePreGUI()
     * for any special initialization that must be completed before building the
     * user
     * interface (e.g. data structures whose properties determine default GUI
     * settings),
     * initializePostGUI() for any special initialization that cannot be
     * completed until after the building the user interface (e.g. data structure
     * whose
     * properties are determined by default GUI settings), and
     * buildMainDisplayArea()
     * to contain application-specific displays of parameters and results.
     */
    public void go() {
        this.theWindow = this;
        this.isBeingUsedAsATool = false;
        this.setTitle(this.title);
        io.github.chr1sps.rars.Globals.initialize();
        // assure the dialog goes away if user clicks the X
        this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent e) {
                        AbstractToolAndApplication.this.performAppClosingDuties();
                    }
                });
        this.initializePreGUI();

        final JPanel contentPane = new JPanel(new BorderLayout(5, 5));
        contentPane.setBorder(this.emptyBorder);
        contentPane.setOpaque(true);
        contentPane.add(this.buildHeadingArea(), BorderLayout.NORTH);
        contentPane.add(this.buildMainDisplayArea(), BorderLayout.CENTER);
        contentPane.add(this.buildButtonAreaStandAlone(), BorderLayout.SOUTH);

        this.setContentPane(contentPane);
        this.pack();
        this.setLocationRelativeTo(null); // center on screen
        this.setVisible(true);
        this.initializePostGUI();
    }

    /**
     * Required Tool method to carry out Tool functions. It is invoked when the
     * user selects this tool from the Tools menu. This default implementation
     * provides
     * generic definitions for interactively controlling the tool. The generic
     * controls
     * for RarsTools are 3 buttons: connect/disconnect to a resource (memory and/or
     * registers), reset, and close (exit). Like "go()" above, this default version
     * calls 3 methods that can be defined/overriden in the subclass:
     * initializePreGUI()
     * for any special initialization that must be completed before building the
     * user
     * interface (e.g. data structures whose properties determine default GUI
     * settings),
     * initializePostGUI() for any special initialization that cannot be
     * completed until after the building the user interface (e.g. data structure
     * whose
     * properties are determined by default GUI settings), and
     * buildMainDisplayArea()
     * to contain application-specific displays of parameters and results.
     */
    @Override
    public void action() {
        this.isBeingUsedAsATool = true;
        this.dialog = new JDialog(Globals.getGui(), this.title);
        // assure the dialog goes away if user clicks the X
        this.dialog.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent e) {
                        AbstractToolAndApplication.this.performToolClosingDuties();
                    }
                });
        this.theWindow = this.dialog;
        this.initializePreGUI();
        final JPanel contentPane = new JPanel(new BorderLayout(5, 5));
        contentPane.setBorder(this.emptyBorder);
        contentPane.setOpaque(true);
        contentPane.add(this.buildHeadingArea(), BorderLayout.NORTH);
        contentPane.add(this.buildMainDisplayArea(), BorderLayout.CENTER);
        contentPane.add(this.buildButtonAreaForTool(), BorderLayout.SOUTH);
        this.initializePostGUI();
        this.dialog.setContentPane(contentPane);
        this.dialog.pack();
        this.dialog.setLocationRelativeTo(Globals.getGui());
        this.dialog.setVisible(true);
    }

    /**
     * Method that will be called once just before the GUI is constructed in the
     * go() and action()
     * methods. Use it to initialize any data structures needed for the application
     * whose values
     * will be needed to determine the initial state of GUI components. By default
     * it does nothing.
     */
    protected void initializePreGUI() {
    }

    /**
     * Method that will be called once just after the GUI is constructed in the go()
     * and action()
     * methods. Use it to initialize data structures needed for the application
     * whose values
     * may depend on the initial state of GUI components. By default it does
     * nothing.
     */
    protected void initializePostGUI() {
    }

    /**
     * Method that will be called each time the default Reset button is clicked.
     * Use it to reset any data structures and/or GUI components. By default it does
     * nothing.
     */
    protected void reset() {
    }

    /**
     * Constructs GUI header as label with default positioning and font. May be
     * overridden.
     *
     * @return a {@link javax.swing.JComponent} object
     */
    protected JComponent buildHeadingArea() {
        // OVERALL STRUCTURE OF MESSAGE (TOP)
        this.headingLabel = new JLabel();
        final Box headingPanel = Box.createHorizontalBox();// new JPanel(new BorderLayout());
        headingPanel.add(Box.createHorizontalGlue());
        headingPanel.add(this.headingLabel);
        headingPanel.add(Box.createHorizontalGlue());
        // Details for heading area (top)
        this.headingLabel.setText(this.heading);
        this.headingLabel.setHorizontalTextPosition(JLabel.CENTER);
        this.headingLabel.setFont(new Font(this.headingLabel.getFont().getFontName(), Font.PLAIN, 18));
        return headingPanel;
    }

    /**
     * The Tool default set of controls has one row of 3 buttons. It includes a
     * dual-purpose button to
     * attach or detach simulator to memory, a button to reset the cache, and one to
     * close the tool.
     *
     * @return a {@link javax.swing.JComponent} object
     */
    protected JComponent buildButtonAreaForTool() {
        final Box buttonArea = Box.createHorizontalBox();
        final TitledBorder tc = new TitledBorder("Tool Control");
        tc.setTitleJustification(TitledBorder.CENTER);
        buttonArea.setBorder(tc);
        this.connectButton = new ConnectButton();
        this.connectButton.setToolTipText("Control whether tool will respond to running program");
        this.connectButton.addActionListener(
                e -> {
                    if (this.connectButton.isConnected()) {
                        this.connectButton.disconnect();
                    } else {
                        this.connectButton.connect();
                    }
                });
        this.connectButton.addKeyListener(new EnterKeyListener(this.connectButton));

        final JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText("Reset all counters and other structures");
        resetButton.addActionListener(
                e -> this.reset());
        resetButton.addKeyListener(new EnterKeyListener(resetButton));

        final JButton closeButton = new JButton("Close");
        closeButton.setToolTipText("Close (exit) this tool");
        closeButton.addActionListener(
                e -> this.performToolClosingDuties());
        closeButton.addKeyListener(new EnterKeyListener(closeButton));

        // Add all the buttons...
        buttonArea.add(this.connectButton);
        buttonArea.add(Box.createHorizontalGlue());
        buttonArea.add(resetButton);
        buttonArea.add(Box.createHorizontalGlue());
        final JComponent helpComponent = this.getHelpComponent();
        if (helpComponent != null) {
            buttonArea.add(helpComponent);
            buttonArea.add(Box.createHorizontalGlue());
        }
        buttonArea.add(closeButton);
        return buttonArea;
    }

    /**
     * The Rars stand-alone app default set of controls has two rows of controls. It
     * includes a text field for
     * displaying status messages, a button to trigger an open file dialog, the RARS
     * run speed slider
     * to control timed execution, a button to assemble and run the program, a reset
     * button
     * whose action is determined by the subclass reset() method, and an exit
     * button.
     *
     * @return a {@link javax.swing.JComponent} object
     */
    protected JComponent buildButtonAreaStandAlone() {
        // Overall structure of control area (two rows).
        final Box operationArea = Box.createVerticalBox();
        final Box fileControlArea = Box.createHorizontalBox();
        final Box buttonArea = Box.createHorizontalBox();
        operationArea.add(fileControlArea);
        operationArea.add(Box.createVerticalStrut(5));
        operationArea.add(buttonArea);
        final TitledBorder ac = new TitledBorder("Application Control");
        ac.setTitleJustification(TitledBorder.CENTER);
        operationArea.setBorder(ac);

        final AbstractToolAndApplication that = this;
        // Top row of controls consists of button to launch file open operation,
        // text field to show filename, and run speed slider.
        this.openFileButton = new JButton("Open program...");
        this.openFileButton.setToolTipText("Select program file to assemble and run");
        this.openFileButton.addActionListener(
                e -> {
                    final JFileChooser fileChooser = new JFileChooser();
                    final JCheckBox multiFileAssembleChoose = new JCheckBox("Assemble all in selected file's directory",
                            this.multiFileAssemble);
                    multiFileAssembleChoose.setToolTipText(
                            "If checked, selected file will be assembled first and all other assembly files in directory will be assembled also.");
                    fileChooser.setAccessory(multiFileAssembleChoose);
                    if (this.mostRecentlyOpenedFile != null) {
                        fileChooser.setSelectedFile(this.mostRecentlyOpenedFile);
                    }
                    // DPS 13 June 2007. The next 4 lines add file filter to file chooser.
                    final FileFilter defaultFileFilter = FilenameFinder.getFileFilter(Globals.fileExtensions,
                            "Assembler Files", true);
                    fileChooser.addChoosableFileFilter(defaultFileFilter);
                    fileChooser.addChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
                    fileChooser.setFileFilter(defaultFileFilter);

                    if (fileChooser.showOpenDialog(that) == JFileChooser.APPROVE_OPTION) {
                        this.multiFileAssemble = multiFileAssembleChoose.isSelected();
                        File theFile = fileChooser.getSelectedFile();
                        try {
                            theFile = theFile.getCanonicalFile();
                        } catch (final IOException ioe) {
                            // nothing to do, theFile will keep current value
                        }
                        final String currentFilePath = theFile.getPath();
                        this.mostRecentlyOpenedFile = theFile;
                        this.operationStatusMessages.setText("File: " + currentFilePath);
                        this.operationStatusMessages.setCaretPosition(0);
                        this.assembleRunButton.setEnabled(true);
                    }
                });
        this.openFileButton.addKeyListener(new EnterKeyListener(this.openFileButton));

        this.operationStatusMessages = new MessageField("No file open.");
        this.operationStatusMessages.setColumns(40);
        this.operationStatusMessages.setMargin(new Insets(0, 3, 0, 3)); // (top, left, bottom, right)
        this.operationStatusMessages.setBackground(this.backgroundColor);
        this.operationStatusMessages.setFocusable(false);
        this.operationStatusMessages.setToolTipText("Display operation status messages");

        final RunSpeedPanel speed = RunSpeedPanel.getInstance();

        // Bottom row of controls consists of the three buttons defined here.
        this.assembleRunButton = new JButton("Assemble and Run");
        this.assembleRunButton.setToolTipText("Assemble and run the currently selected program");
        this.assembleRunButton.setEnabled(false);
        this.assembleRunButton.addActionListener(
                e -> {
                    this.assembleRunButton.setEnabled(false);
                    this.openFileButton.setEnabled(false);
                    this.stopButton.setEnabled(true);
                    new Thread(new CreateAssembleRunProgram()).start();
                });
        this.assembleRunButton.addKeyListener(new EnterKeyListener(this.assembleRunButton));

        this.stopButton = new JButton("Stop");
        this.stopButton.setToolTipText("Terminate program execution");
        this.stopButton.setEnabled(false);
        this.stopButton.addActionListener(
                e -> Simulator.getInstance().stopExecution());
        this.stopButton.addKeyListener(new EnterKeyListener(this.stopButton));

        final JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText("Reset all counters and other structures");
        resetButton.addActionListener(
                e -> this.reset());
        resetButton.addKeyListener(new EnterKeyListener(resetButton));

        final JButton closeButton = new JButton("Exit");
        closeButton.setToolTipText("Exit this application");
        closeButton.addActionListener(
                e -> this.performAppClosingDuties());
        closeButton.addKeyListener(new EnterKeyListener(closeButton));

        // Add top row of controls...
        // fileControlArea.add(Box.createHorizontalStrut(5));

        final Box fileDisplayBox = Box.createVerticalBox();
        fileDisplayBox.add(Box.createVerticalStrut(8));
        fileDisplayBox.add(this.operationStatusMessages);
        fileDisplayBox.add(Box.createVerticalStrut(8));
        fileControlArea.add(fileDisplayBox);

        fileControlArea.add(Box.createHorizontalGlue());
        fileControlArea.add(speed);

        // Add bottom row of buttons...

        buttonArea.add(this.openFileButton);
        buttonArea.add(Box.createHorizontalGlue());
        buttonArea.add(this.assembleRunButton);
        buttonArea.add(Box.createHorizontalGlue());
        buttonArea.add(this.stopButton);
        buttonArea.add(Box.createHorizontalGlue());
        buttonArea.add(resetButton);
        buttonArea.add(Box.createHorizontalGlue());
        final JComponent helpComponent = this.getHelpComponent();
        if (helpComponent != null) {
            buttonArea.add(helpComponent);
            buttonArea.add(Box.createHorizontalGlue());
        }
        buttonArea.add(closeButton);
        return operationArea;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    // Rest of the methods. Some are used by stand-alone (JFrame-based) only, some
    ////////////////////////////////////////////////////////////////////////////////////// are
    // used by Tool (JDialog-based) only, others are used by both.
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * This method is called when tool/app is exited either through the close/exit
     * button or the window's X box.
     * Override it to perform any special housecleaning needed. By default it does
     * nothing.
     */
    protected void performSpecialClosingDuties() {
    }

    /**
     * Add this app/tool as an Observer of desired Observables (memory and
     * registers).
     * By default, will add as an Observer of the entire Data Segment in memory.
     * Override if you want something different. Note that the Memory methods to add
     * an
     * Observer to memory are flexible (you can register for a range of addresses)
     * but
     * may throw an AddressErrorException that you need to catch.
     * This method is called whenever the default "Connect" button on a Tool or the
     * default "Assemble and run" on a stand-alone Rars app is selected. The
     * corresponding
     * NOTE: if you do not want to register as an Observer of the entire data
     * segment
     * (starts at address 0x10000000) then override this to either do some
     * alternative
     * or nothing at all. This method is also overloaded to allow arbitrary memory
     * subrange.
     */
    protected void addAsObserver() {
        this.addAsObserver(this.lowMemoryAddress, this.highMemoryAddress);
    }

    /**
     * Add this app/tool as an Observer of the specified subrange of memory. Note
     * that this method is not invoked automatically like the no-argument version,
     * but
     * if you use this method, you can still take advantage of provided default
     * deleteAsObserver()
     * since it will remove the app as a memory observer regardless of the subrange
     * or number of subranges it is registered for.
     *
     * @param lowEnd  low end of memory address range.
     * @param highEnd high end of memory address range; must be >= lowEnd
     */
    protected void addAsObserver(final int lowEnd, final int highEnd) {
        final String errorMessage = "Error connecting to memory";
        try {
            Globals.memory.subscribe(this, lowEnd, highEnd);
        } catch (final AddressErrorException aee) {
            if (this.isBeingUsedAsATool) {
                this.headingLabel.setText(errorMessage);
            } else {
                this.operationStatusMessages.displayTerminatingMessage(errorMessage);
            }
        }
    }

    /**
     * Add this app/tool as an Observer of the specified register.
     *
     * @param reg a {@link io.github.chr1sps.rars.riscv.hardware.Register} object
     */
    protected void addAsObserver(final Register reg) {
        if (reg != null) {
            reg.subscribe(this);
        }
    }

    /**
     * Delete this app/tool as an Observer of Observables (memory and registers).
     * By default, will delete as an Observer of memory.
     * Override if you want something different.
     * This method is called when the default "Disconnect" button on a Tool is
     * selected or
     * when the RISCV program execution triggered by the default "Assemble and run"
     * on a stand-alone
     * app terminates (e.g. when the button is re-enabled).
     */
    protected void deleteAsSubscriber() {
        Globals.memory.deleteSubscriber(this);
    }

    /**
     * Delete this app/tool as an Observer of the specified register
     *
     * @param reg a {@link io.github.chr1sps.rars.riscv.hardware.Register} object
     */
    protected void deleteAsSubscriber(final Register reg) {
        if (reg != null) {
            reg.deleteSubscriber(this);
        }
    }

    /**
     * Query method to let you know if the tool/app is (or could be) currently
     * "observing" any resources. When running as a Tool, this
     * will be true by default after clicking the "Connect to Program" button until
     * "Disconnect
     * from Program" is clicked. When running as a stand-alone app, this will be
     * true by default after clicking the "Assemble and Run" button until until
     * program execution has terminated either normally or by clicking the "Stop"
     * button. The phrase "or could be" was added above because depending on how
     * the tool/app operates, it may be possible to run the program without
     * first registering as an Observer -- i.e. addAsObserver() is overridden and
     * takes no action.
     *
     * @return true if tool/app is (or could be) currently active as an Observer.
     */
    protected boolean isObserving() {
        return this.observing;
    }

    /**
     * Override this method to implement updating of GUI after each instruction is
     * executed,
     * while running in "timed" mode (user specifies execution speed on the slider
     * control).
     * Does nothing by default.
     */
    protected void updateDisplay() {
    }

    /**
     * Override this method to process a received notice from an Observable (memory
     * or register)
     * It will only be called if the notice was generated as the result of RISCV
     * instruction execution.
     * By default it does nothing. After this method is complete, the
     * updateDisplay() method will be
     * invoked automatically.
     *
     * @param notice a {@link io.github.chr1sps.rars.notices.AccessNotice} object
     */
    protected void processRISCVUpdate(final AccessNotice notice) {
    }

    /**
     * Override this method to provide a JComponent (probably a JButton) of your
     * choice
     * to be placed just left of the Close/Exit button. Its anticipated use is for a
     * "help" button that launches a help message or dialog. But it can be any valid
     * JComponent that doesn't mind co-existing among a bunch of JButtons.
     *
     * @return a {@link javax.swing.JComponent} object
     */
    protected JComponent getHelpComponent() {
        return null;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////// PRIVATE HELPER METHODS //////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////

    // Closing duties for Tool only.
    private void performToolClosingDuties() {
        this.performSpecialClosingDuties();
        if (this.connectButton.isConnected()) {
            this.connectButton.disconnect();
        }
        this.dialog.setVisible(false);
        this.dialog.dispose();
    }

    // Closing duties for stand-alone application only.
    private void performAppClosingDuties() {
        this.performSpecialClosingDuties();
        this.setVisible(false);
        System.exit(0);
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////// PRIVATE HELPER CLASSES //////////////////////////////////
    // Specialized inner classes. Either used by stand-alone (JFrame-based) only //
    // or used by Tool (JDialog-based) only. //
    //////////////////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////
    // Little class for this dual-purpose button. It is used only by the Tool
    // (not by the stand-alone app).
    protected class ConnectButton extends JButton {
        private static final String connectText = "Connect to Program";
        private static final String disconnectText = "Disconnect from Program";

        public ConnectButton() {
            super();
            this.disconnect();
        }

        public void connect() {
            AbstractToolAndApplication.this.observing = true;
            Globals.memoryAndRegistersLock.lock();
            try {
                AbstractToolAndApplication.this.addAsObserver();
            } finally {
                Globals.memoryAndRegistersLock.unlock();
            }
            this.setText(ConnectButton.disconnectText);
        }

        public void disconnect() {
            Globals.memoryAndRegistersLock.lock();
            try {
                AbstractToolAndApplication.this.deleteAsSubscriber();
            } finally {
                Globals.memoryAndRegistersLock.unlock();
            }
            AbstractToolAndApplication.this.observing = false;
            this.setText(ConnectButton.connectText);
        }

        public boolean isConnected() {
            return AbstractToolAndApplication.this.observing;
        }
    }

    ///////////////////////////////////////////////////////////////////////
    // Every control button will get one of these so when it has focus
    // the Enter key can be used instead of a mouse click to perform
    // its associated action. It will do nothing if no action listeners
    // are attached to the button at the time of the call. Otherwise,
    // it will call actionPerformed for the first action listener in the
    // button's list.
    protected static class EnterKeyListener extends KeyAdapter {
        AbstractButton myButton;

        public EnterKeyListener(final AbstractButton who) {
            this.myButton = who;
        }

        @Override
        public void keyPressed(@NotNull final KeyEvent e) {
            if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                e.consume();
                try {
                    this.myButton.getActionListeners()[0].actionPerformed(new ActionEvent(this.myButton, 0, this.myButton.getText()));
                } catch (final ArrayIndexOutOfBoundsException oob) {
                    // do nothing, since there is no action listener.
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    // called when the Assemble and Run button is pressed. Used only by stand-alone
    ///////////////////////////////////////////////////////////////////////////////// app.
    private class CreateAssembleRunProgram implements Runnable {
        @Override
        public void run() {
            // String noSupportForExceptionHandler = null; // no auto-loaded exception
            // handlers.
            // boolean extendedAssemblerEnabled = true; // In this context, no reason to
            // constrain.
            // boolean warningsAreErrors = false; // Ditto.

            String exceptionHandler = null;
            if (Globals.getSettings().getBooleanSetting(Settings.Bool.EXCEPTION_HANDLER_ENABLED) &&
                    Globals.getSettings().getExceptionHandler() != null &&
                    !Globals.getSettings().getExceptionHandler().isEmpty()) {
                exceptionHandler = Globals.getSettings().getExceptionHandler();
            }

            Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
            Thread.yield();
            final RISCVprogram program = new RISCVprogram();
            io.github.chr1sps.rars.Globals.program = program; // Shouldn't have to do this...
            final String fileToAssemble = AbstractToolAndApplication.this.mostRecentlyOpenedFile.getPath();
            final ArrayList<String> filesToAssemble;
            if (AbstractToolAndApplication.this.multiFileAssemble) {// setting (check box in file open dialog) calls for multiple file assembly
                filesToAssemble = FilenameFinder.getFilenameList(
                        new File(fileToAssemble).getParent(), Globals.fileExtensions);
            } else {
                filesToAssemble = new ArrayList<>();
                filesToAssemble.add(fileToAssemble);
            }
            final ArrayList<RISCVprogram> programsToAssemble;
            try {
                AbstractToolAndApplication.this.operationStatusMessages.displayNonTerminatingMessage("Assembling " + fileToAssemble);
                programsToAssemble = program.prepareFilesForAssembly(filesToAssemble, fileToAssemble, exceptionHandler);
            } catch (final AssemblyException pe) {
                AbstractToolAndApplication.this.operationStatusMessages.displayTerminatingMessage("Error reading file(s): " + fileToAssemble);
                return;
            }

            try {
                program.assemble(programsToAssemble,
                        Globals.getSettings().getBooleanSetting(Settings.Bool.EXTENDED_ASSEMBLER_ENABLED),
                        Globals.getSettings().getBooleanSetting(Settings.Bool.WARNINGS_ARE_ERRORS));
            } catch (final AssemblyException pe) {
                AbstractToolAndApplication.this.operationStatusMessages.displayTerminatingMessage("Assembly Error: " + fileToAssemble);
                return;
            }
            // Moved these three register resets from before the try block to after it.
            // 17-Dec-09 DPS.
            RegisterFile.resetRegisters();
            FloatingPointRegisterFile.resetRegisters();
            ControlAndStatusRegisterFile.resetRegisters();
            InterruptController.reset();

            AbstractToolAndApplication.this.addAsObserver();
            AbstractToolAndApplication.this.observing = true;
            AbstractToolAndApplication.this.operationStatusMessages.displayNonTerminatingMessage("Running " + fileToAssemble);
            final var stopListener = new SimpleSubscriber<SimulatorNotice>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(final Flow.Subscription subscription) {
                    this.subscription = subscription;
                    this.subscription.request(1);
                }

                @Override
                public void onNext(@NotNull final SimulatorNotice notice) {
                    if (notice.getAction() != SimulatorNotice.SIMULATOR_STOP) {
                        this.subscription.request(1);
                        return;
                    }
                    AbstractToolAndApplication.this.deleteAsSubscriber();
                    AbstractToolAndApplication.this.observing = false;
                    String terminatingMessage = "Normal termination: ";
                    if (notice.getReason() == Simulator.Reason.EXCEPTION)
                        terminatingMessage = "Runtime error: ";
                    if (notice.getReason() == Simulator.Reason.STOP || notice.getReason() == Simulator.Reason.PAUSE) {
                        terminatingMessage = "User interrupt: ";
                    }
                    AbstractToolAndApplication.this.operationStatusMessages.displayTerminatingMessage(terminatingMessage + fileToAssemble);
                    this.subscription.cancel();
                }
            };
            Simulator.getInstance().subscribe(stopListener);
            program.startSimulation(-1, null); // unlimited steps
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Class for text message field used to update operation status when
    // assembling and running programs.
    private class MessageField extends JTextField {

        public MessageField(final String text) {
            super(text);
        }

        private void displayTerminatingMessage(final String text) {
            this.displayMessage(text, true);
        }

        private void displayNonTerminatingMessage(final String text) {
            this.displayMessage(text, false);
        }

        private void displayMessage(final String text, final boolean terminating) {
            SwingUtilities.invokeLater(new MessageWriter(text, terminating));
        }

        /////////////////////////////////////////////////////////////////////////////////
        // Little inner-inner class to display processing error message on AWT thread.
        // Used only by stand-alone app.
        private class MessageWriter implements Runnable {
            private final String text;
            private final boolean terminatingMessage;

            public MessageWriter(final String text, final boolean terminating) {
                this.text = text;
                this.terminatingMessage = terminating;
            }

            @Override
            public void run() {
                if (this.text != null) {
                    AbstractToolAndApplication.this.operationStatusMessages.setText(this.text);
                    AbstractToolAndApplication.this.operationStatusMessages.setCaretPosition(0);
                }
                if (this.terminatingMessage) {
                    AbstractToolAndApplication.this.assembleRunButton.setEnabled(true);
                    AbstractToolAndApplication.this.openFileButton.setEnabled(true);
                    AbstractToolAndApplication.this.stopButton.setEnabled(false);
                }
            }
        }
    }

    //////////////////////////////////////////////////////////////////////
    // For scheduling GUI update on timed runs...used only by stand-alone app.
    private class GUIUpdater implements Runnable {
        @Override
        public void run() {
            AbstractToolAndApplication.this.updateDisplay();
        }
    }

    protected Flow.Subscription subscription;

    @Override
    public void onSubscribe(@NotNull final Flow.Subscription subscription) {
        System.out.println("onSubscribe called for: " + this);
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(final AccessNotice notice) {
        System.out.println("onNext called for: " + this);
        if (notice.accessIsFromRISCV()) {
            this.processRISCVUpdate(notice);
            this.updateDisplay();
        }
        this.subscription.request(1);
    }
}
