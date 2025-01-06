package rars.tools;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.exceptions.AddressErrorException;
import rars.notices.AccessNotice;
import rars.riscv.hardware.registers.Register;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

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
 * A base class used to define a tool that can interact with an executing program.
 * <p>
 * The generic controls for RarsTools are 3 buttons:
 * connect/disconnect to a resource (memory and/or registers), reset, and close (exit).
 * <p>
 * A tool may receive communication from system resources
 * (registers or memory) by registering as an Observer with
 * Memory and/or Register objects.
 * <p>
 * It may also * communicate directly with those resources through their
 * published methods PROVIDED any such communication is
 * wrapped inside a block synchronized on the
 * Globals.memoryAndRegistersLock object.
 * <p>
 * Pete Sanderson, 14 November 2006.
 * Modified by Chr1sps, 2024.
 */

public abstract class AbstractTool extends JFrame {
    private final String title; // descriptive title for title bar provided to constructor.
    private final String heading; // Text to be displayed in the top portion of the main window.
    // Some GUI settings
    private final EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
    private final int lowMemoryAddress = Globals.MEMORY_INSTANCE.getMemoryConfiguration().dataSegmentBaseAddress;
    private final int highMemoryAddress = Globals.MEMORY_INSTANCE.getMemoryConfiguration().stackBaseAddress;
    protected Window theWindow; // highest level GUI component (a JFrame for app, a JDialog for Tool)
    protected ConnectButton connectButton;
    protected JDialog dialog; //  This is the pop-up dialog that appears when menu item is selected.
    // Major GUI components
    private JLabel headingLabel;
    // For Tool, is set true when "Connect" clicked, false when "Disconnect"
    // clicked.
    // For app, is set true when "Assemble and Run" clicked, false when program
    // terminates.
    private volatile boolean observing = false;

    /**
     * Simple constructor
     *
     * @param title
     *     String containing title bar text
     * @param heading
     *     a {@link java.lang.String} object
     */
    protected AbstractTool(final String title, final String heading) {
        this.title = title;
        this.heading = heading;
    }

    /**
     * Required Tool method to return Tool name. Must be defined by subclass.
     *
     * @return Tool name. RARS will display this in menu item.
     */
    @Override
    public abstract String getName();

    ///////////////////////////// METHODS WITH DEFAULT IMPLEMENTATIONS

    /**
     * Abstract method that must be instantiated by subclass to build the main
     * display area
     * of the GUI. It will be placed in the CENTER area of a BorderLayout. The title
     * is in the NORTH area, and the controls are in the SOUTH area.
     *
     * @return a {@link javax.swing.JComponent} object
     */
    protected abstract JComponent buildMainDisplayArea();

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
    public void action() {
        this.dialog = new JDialog(Globals.gui, this.title);
        // assure the dialog goes away if user clicks the X
        this.dialog.addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(final WindowEvent e) {
                    AbstractTool.this.performToolClosingDuties();
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
        this.dialog.setLocationRelativeTo(Globals.gui);
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

    // Rest of the methods.

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
     * @param lowEnd
     *     low end of memory address range.
     * @param highEnd
     *     high end of memory address range; must be >= lowEnd
     */
    protected void addAsObserver(final int lowEnd, final int highEnd) {
        final String errorMessage = "Error connecting to memory";
        try {
            Globals.MEMORY_INSTANCE.subscribe(this::processAccessNotice, lowEnd, highEnd);
        } catch (final AddressErrorException aee) {
            this.headingLabel.setText(errorMessage);
        }
    }

    protected void processAccessNotice(final @NotNull AccessNotice notice) {
        if (notice.isAccessFromRISCV) {
            this.processRISCVUpdate(notice);
            this.updateDisplay();
        }
    }

    /**
     * Add this app/tool as an Observer of the specified register.
     *
     * @param reg
     *     a {@link Register} object
     */
    protected void addAsObserver(final Register reg) {
        if (reg != null) {
            reg.registerChangeHook.subscribe(this::processAccessNotice);
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
        Globals.MEMORY_INSTANCE.deleteSubscriber(this::processAccessNotice);
    }

    /**
     * Delete this app/tool as an Observer of the specified register
     *
     * @param reg
     *     a {@link Register} object
     */
    protected void deleteAsSubscriber(final Register reg) {
        if (reg != null) {
            reg.registerChangeHook.unsubscribe(this::processAccessNotice);
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
     * @param notice
     *     a {@link AccessNotice} object
     */
    protected void processRISCVUpdate(final AccessNotice notice) {
    }

    // PRIVATE HELPER METHODS 

    /**
     * Override this method to provide a JComponent (probably a JButton) of your
     * choice
     * to be placed just left of the Close/Exit button. Its anticipated use is for a
     * "help" button that launches a help message or dialog. But it can be any valid
     * JComponent that doesn't mind co-existing among a bunch of JButtons.
     *
     * @return a {@link javax.swing.JComponent} object
     */
    protected @Nullable JComponent getHelpComponent() {
        return null;
    }

    // PRIVATE HELPER CLASSES 

    // Specialized inner classes. Either used by stand-alone (JFrame-based) only //
    // or used by Tool (JDialog-based) only. //

    // Closing duties for Tool only.
    private void performToolClosingDuties() {
        this.performSpecialClosingDuties();
        if (this.connectButton.isConnected()) {
            this.connectButton.disconnect();
        }
        this.dialog.setVisible(false);
        this.dialog.dispose();
    }

    // Every control button will get one of these so when it has focus
    // the Enter first can be used instead of a mouse click to perform
    // its associated action. It will do nothing if no action listeners
    // are attached to the button at the time of the call. Otherwise,
    // it will call actionPerformed for the first action listener in the
    // button's list.
    protected static class EnterKeyListener extends KeyAdapter {
        final AbstractButton myButton;

        public EnterKeyListener(final AbstractButton who) {
            this.myButton = who;
        }

        @Override
        public void keyPressed(@NotNull final KeyEvent e) {
            if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                e.consume();
                try {
                    this.myButton.getActionListeners()[0].actionPerformed(new ActionEvent(
                        this.myButton, 0,
                        this.myButton.getText()
                    ));
                } catch (final ArrayIndexOutOfBoundsException oob) {
                    // do nothing, since there is no action listener.
                }
            }
        }
    }

    // Little class for this dual-purpose button. It is used only by the Tool
    // (not by the stand-alone app).
    protected class ConnectButton extends JButton {
        private static final String connectText = "Connect to Program";
        private static final String disconnectText = "Disconnect from Program";
        private final ArrayList<Callback> connectionListeners = new ArrayList<>();

        public ConnectButton() {
            super();
            this.disconnect();
        }

        public void connect() {
            AbstractTool.this.observing = true;
            Globals.memoryAndRegistersLock.lock();
            try {
                AbstractTool.this.addAsObserver();
            } finally {
                Globals.memoryAndRegistersLock.unlock();
            }
            this.setText(ConnectButton.disconnectText);
            this.notifyConnectionListeners();
        }

        public void disconnect() {
            Globals.memoryAndRegistersLock.lock();
            try {
                AbstractTool.this.deleteAsSubscriber();
            } finally {
                Globals.memoryAndRegistersLock.unlock();
            }
            AbstractTool.this.observing = false;
            this.setText(ConnectButton.connectText);
            this.notifyConnectionListeners();
        }

        private void notifyConnectionListeners() {
            for (final var listener : this.connectionListeners) {
                listener.run(AbstractTool.this.observing);
            }
        }

        public boolean isConnected() {
            return AbstractTool.this.observing;
        }

        public void addConnectListener(final Callback callback) {
            this.connectionListeners.add(callback);
        }

        public interface Callback {
            void run(boolean isConnected);
        }
    }
}
