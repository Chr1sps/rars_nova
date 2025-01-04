package rars.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rars.Globals;
import rars.assembler.DataTypes;
import rars.exceptions.AddressErrorException;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;
import rars.riscv.hardware.InterruptController;
import rars.util.BinaryUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Random;

import static rars.settings.FontSettings.FONT_SETTINGS;

/*
Copyright (c) 2003-2014,  Pete Sanderson and Kenneth Vollmar

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

// TODO: make an example that uses this. I'm not sure it wasn't broken in the porting process.

/**
 * Keyboard and Display Simulator. It can be run either as a stand-alone Java
 * application having
 * access to the {@link rars} package, or through RARS as an item in its
 * Tools menu. It makes
 * maximum use of methods inherited from its abstract superclass
 * AbstractToolAndApplication.
 * Pete Sanderson<br>
 * Version 1.0, 24 July 2008.<br>
 * Version 1.1, 24 November 2008 corrects two omissions: (1) the tool failed to
 * register as an observer
 * of kernel text memory when counting instruction executions for transmitter
 * ready bit
 * reset delay, and (2) the tool failed to test the Status register's Exception
 * Level bit before
 * raising the exception that results in the interrupt (if the Exception Level
 * bit is 1, that
 * means an interrupt is being processed, so disable further interrupts).
 * <p>
 * Version 1.2, August 2009, soft-codes the MMIO register locations for new
 * memory configuration
 * feature of MARS 3.7. Previously memory segment addresses were fixed and
 * final. Now they
 * can be modified dynamically so the tool has to get its values dynamically as
 * well.
 * <p>
 * Version 1.3, August 2011, corrects bug to enable Display window to scroll
 * when needed.
 * <p>
 * Version 1.4, August 2014, adds two features: (1) ASCII control character 12
 * (form feed) when
 * transmitted will clear the Display window. (2) ASCII control character 7
 * (bell) when
 * transmitted with properly coded (X,Y) values will reposition the cursor to
 * the specified
 * position of a virtual text-based terminal. X represents column, Y represents
 * row.
 */
public class KeyboardAndDisplaySimulator extends AbstractTool {
    public static final Dimension preferredTextAreaDimension = new Dimension(400, 200);
    public static final int EXTERNAL_INTERRUPT_KEYBOARD = 0x00000040;
    public static final int EXTERNAL_INTERRUPT_DISPLAY = 0x00000080;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String version = "Version 1.4";
    private static final String heading = "Keyboard and Display MMIO Simulator";
    private static final char VT_FILL = ' '; // fill character for virtual terminal (random access mode)
    private static final Insets textAreaInsets = new Insets(4, 4, 4, 4);
    private static final char CLEAR_SCREEN = 12; // ASCII Form Feed
    private static final char SET_CURSOR_X_Y = 7; // ASCII Bell (ding ding!)
    public static int RECEIVER_CONTROL; // keyboard Ready in low-order bit
    public static int RECEIVER_DATA; // keyboard character in low-order byte
    public static int TRANSMITTER_CONTROL; // display Ready in low-order bit
    public static int TRANSMITTER_DATA; // display character in low-order byte
    private static String displayPanelTitle, keyboardPanelTitle;
    /**
     * Time delay to process Transmitter Data is simulated by counting instruction
     * executions.
     * After this many executions, the Transmitter Controller Ready bit set to 1.
     */
    private final TransmitterDelayTechnique[] delayTechniques = {
            new FixedLengthDelay(),
            new UniformlyDistributedDelay(),
            new NormallyDistributedDelay()
    };
    private final KeyboardAndDisplaySimulator simulator;
    // These are used to track instruction counts to simulate driver delay of
    // Transmitter Data
    private boolean countingInstructions;
    private int instructionCount;
    private int transmitDelayInstructionCountLimit;
    /**
     * Should the transmitted character be displayed before the transmitter delay period?
     * If not, hold onto it and print at the end of delay period.
     */
    private int intWithCharacterToDisplay;
    private boolean displayAfterDelay = true;
    /**
     * Whether or not display position is sequential (JTextArea append)
     * or random access (row, column). Supports new random access feature. DPS
     * 17-July-2014
     */
    private boolean displayRandomAccessMode = false;
    private int rows, columns;
    private JTextArea display;
    private JPanel displayPanel;
    private JComboBox<TransmitterDelayTechnique> delayTechniqueChooser;
    private DelayLengthPanel delayLengthPanel;
    private JSlider delayLengthSlider;
    private JCheckBox displayAfterDelayCheckBox;
    private JTextArea keyEventAccepter;

    /**
     * Simple constructor, likely used to run a stand-alone keyboard/display
     * simulator.
     *
     * @param title
     *         String containing title for title bar
     * @param heading
     *         String containing text for heading shown in upper part of
     *         window.
     */
    public KeyboardAndDisplaySimulator(final String title, final String heading) {
        super(title, heading);
        this.simulator = this;
    }

    /**
     * Simple constructor, likely used by the RARS Tools menu mechanism
     */
    public KeyboardAndDisplaySimulator() {
        this(KeyboardAndDisplaySimulator.heading + ", " + KeyboardAndDisplaySimulator.version,
                KeyboardAndDisplaySimulator.heading);
    }

    // Return second of the given MMIO control register after ready (low order) bit

    // Have to preserve the second of Interrupt Enable bit (bit 1)
    private static boolean isReadyBitSet(final int mmioControlRegister) {
        try {
            return (Globals.MEMORY_INSTANCE.get(mmioControlRegister, DataTypes.WORD_SIZE) & 1) == 1;
        } catch (final AddressErrorException aee) {
            KeyboardAndDisplaySimulator.LOGGER.fatal("Tool author specified incorrect MMIO address!", aee);
            System.exit(0);
        }
        return false; // to satisfy the compiler -- this will never happen.
    }

    // Return second of the given MMIO control register after ready (low order) bit

    // Have to preserve the second of Interrupt Enable bit (bit 1)
    private static int readyBitSet(final int mmioControlRegister) {
        try {
            return Globals.MEMORY_INSTANCE.get(mmioControlRegister, DataTypes.WORD_SIZE) | 1;
        } catch (final AddressErrorException aee) {
            KeyboardAndDisplaySimulator.LOGGER.fatal("Tool author specified incorrect MMIO address!", aee);
            System.exit(0);
        }
        return 1; // to satisfy the compiler -- this will never happen.
    }

    // Rest of the protected methods. These all override do-nothing methods
    // the abstract superclass.

    // Return second of the given MMIO control register after ready (low order) bit
    // Have to preserve the second of Interrupt Enable bit (bit 1). Bits 2 and higher

    private static int readyBitCleared(final int mmioControlRegister) {
        try {
            return Globals.MEMORY_INSTANCE.get(mmioControlRegister, DataTypes.WORD_SIZE) & 2;
        } catch (final AddressErrorException aee) {
            KeyboardAndDisplaySimulator.LOGGER.fatal("Tool author specified incorrect MMIO address!", aee);
            System.exit(0);
        }
        return 0; // to satisfy the compiler -- this will never happen.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return KeyboardAndDisplaySimulator.heading;
    }

    @Override
    protected void initializePreGUI() {
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();

        KeyboardAndDisplaySimulator.RECEIVER_CONTROL = memoryConfiguration.memoryMapBaseAddress; // 0xffff0000; // keyboard Ready 
        // in low-order bit
        KeyboardAndDisplaySimulator.RECEIVER_DATA = memoryConfiguration.memoryMapBaseAddress + 4; // 0xffff0004; // keyboard 
        // character in low-order byte
        KeyboardAndDisplaySimulator.TRANSMITTER_CONTROL = memoryConfiguration.memoryMapBaseAddress + 8; // 0xffff0008; // display 
        // Ready in low-order bit
        KeyboardAndDisplaySimulator.TRANSMITTER_DATA = memoryConfiguration.memoryMapBaseAddress + 12; // 0xffff000c; // display 
        // character in low-order byte
        KeyboardAndDisplaySimulator.displayPanelTitle =
                "DISPLAY: Store to Transmitter Data " + BinaryUtils.intToHexString(KeyboardAndDisplaySimulator.TRANSMITTER_DATA);
        KeyboardAndDisplaySimulator.keyboardPanelTitle = "KEYBOARD: Characters typed here are stored to Receiver Data "
                + BinaryUtils.intToHexString(KeyboardAndDisplaySimulator.RECEIVER_DATA);
    }

    /**
     * Override the inherited method, which registers us as an Observer over the
     * static data segment
     * (starting address 0x10010000) only.
     * <p>
     * When user enters keystroke, set RECEIVER_CONTROL and RECEIVER_DATA using the
     * action listener.
     * When user loads word (lw) from RECEIVER_DATA (we are notified of the read),
     * then clear RECEIVER_CONTROL.
     * When user stores word (sw) to TRANSMITTER_DATA (we are notified of the
     * write), then clear TRANSMITTER_CONTROL, read TRANSMITTER_DATA,
     * echo the character to display, wait for delay period, then set
     * TRANSMITTER_CONTROL.
     * <p>
     * If you use the inherited GUI buttons, this method is invoked when you click
     * "Connect" button on Tool or the
     * "Assemble and Run" button on a Rars-based app.
     */
    @Override
    protected void addAsObserver() {
        // Set transmitter Control ready bit to 1, means we're ready to accept display
        // character.
        this.updateMMIOControl(KeyboardAndDisplaySimulator.TRANSMITTER_CONTROL,
                KeyboardAndDisplaySimulator.readyBitSet(KeyboardAndDisplaySimulator.TRANSMITTER_CONTROL));
        // We want to be an observer only of reads from RECEIVER_DATA and writes to
        // TRANSMITTER_DATA.
        // Use the Globals.memory.addObserver() methods instead of inherited method to
        // achieve this.
        this.addAsObserver(KeyboardAndDisplaySimulator.RECEIVER_DATA, KeyboardAndDisplaySimulator.RECEIVER_DATA);
        this.addAsObserver(KeyboardAndDisplaySimulator.TRANSMITTER_DATA, KeyboardAndDisplaySimulator.TRANSMITTER_DATA);
        // We want to be notified of each instruction execution, because instruction
        // count is the
        // basis for delay in re-setting (literally) the TRANSMITTER_CONTROL register.
        // SPIM does
        // this too. This simulates the time required for the display unit to process
        // the
        // TRANSMITTER_DATA.
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        this.addAsObserver(memoryConfiguration.textBaseAddress, memoryConfiguration.textLimitAddress);
    }

    /**
     * Method that constructs the main display area. It is organized vertically
     * into two major components: the display and the keyboard. The display itself
     * is a JTextArea and it echoes characters placed into the low order byte of
     * the Transmitter Data location, 0xffff000c. They keyboard is also a JTextArea
     * places each typed character into the Receive Data location 0xffff0004.
     *
     * @return the GUI component containing these two areas
     */
    @Override
    protected JComponent buildMainDisplayArea() {
        // Changed arrangement of the display and keyboard panels from GridLayout(2,1)
        // to BorderLayout to hold a JSplitPane containing both panels. This permits
        // user
        // to apportion the relative sizes of the display and keyboard panels within
        // the overall frame. Will be convenient for use with the new random-access
        // display positioning feature. Previously, both the display and the keyboard
        // text areas were equal in size and there was no way for the user to change
        // that.
        // DPS 17-July-2014
        // Major GUI components
        final JPanel keyboardAndDisplay = new JPanel(new BorderLayout());
        final JSplitPane both = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.buildDisplay(), this.buildKeyboard());
        both.setResizeWeight(0.5);
        keyboardAndDisplay.add(both);
        return keyboardAndDisplay;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processRISCVUpdate(final AccessNotice accessNotice) {
        final MemoryAccessNotice notice = (MemoryAccessNotice) accessNotice;
        // If the program has just read (loaded) the receiver (keyboard) data register,
        // then clear the Ready bit to indicate there is no longer a keystroke
        // available.
        // If Ready bit was initially clear, they'll get the old keystroke -- serves 'em
        // right
        // for not checking!
        if (notice.getAddress() == KeyboardAndDisplaySimulator.RECEIVER_DATA && notice.getAccessType() == AccessNotice.AccessType.READ) {
            this.updateMMIOControl(KeyboardAndDisplaySimulator.RECEIVER_CONTROL,
                    KeyboardAndDisplaySimulator.readyBitCleared(KeyboardAndDisplaySimulator.RECEIVER_CONTROL));
        }
        // The program has just written (stored) the transmitter (display) data
        // register. If transmitter
        // Ready bit is clear, device is not ready yet so ignore this event -- serves
        // 'em right for not checking!
        // If transmitter Ready bit is set, then clear it to indicate the display device
        // is processing the character.
        // Also start an intruction counter that will simulate the delay of the slower
        // display device processing the character.
        if (KeyboardAndDisplaySimulator.isReadyBitSet(KeyboardAndDisplaySimulator.TRANSMITTER_CONTROL) && notice.getAddress() == KeyboardAndDisplaySimulator.TRANSMITTER_DATA
                && notice.getAccessType() == AccessNotice.AccessType.WRITE) {
            this.updateMMIOControl(KeyboardAndDisplaySimulator.TRANSMITTER_CONTROL,
                    KeyboardAndDisplaySimulator.readyBitCleared(KeyboardAndDisplaySimulator.TRANSMITTER_CONTROL));
            this.intWithCharacterToDisplay = notice.getValue();
            if (!this.displayAfterDelay)
                this.displayCharacter(this.intWithCharacterToDisplay);
            this.countingInstructions = true;
            this.instructionCount = 0;
            this.transmitDelayInstructionCountLimit = this.generateDelay();
        }
        // We have been notified of an instruction execution.
        // If we are in transmit delay period, increment instruction count and if limit
        // has been reached, set the transmitter Ready flag to indicate the program
        // can write another character to the transmitter data register. If the
        // Interrupt-Enabled
        // bit had been set by the program, generate an interrupt!
        if (this.countingInstructions &&
                notice.getAccessType() == AccessNotice.AccessType.READ && Globals.MEMORY_INSTANCE.isAddressInTextSegment(
                notice.getAddress())) {
            this.instructionCount++;
            if (this.instructionCount >= this.transmitDelayInstructionCountLimit) {
                if (this.displayAfterDelay)
                    this.displayCharacter(this.intWithCharacterToDisplay);
                this.countingInstructions = false;
                final int updatedTransmitterControl =
                        KeyboardAndDisplaySimulator.readyBitSet(KeyboardAndDisplaySimulator.TRANSMITTER_CONTROL);
                this.updateMMIOControl(KeyboardAndDisplaySimulator.TRANSMITTER_CONTROL, updatedTransmitterControl);
                if (updatedTransmitterControl != 1) {
                    InterruptController.registerExternalInterrupt(KeyboardAndDisplaySimulator.EXTERNAL_INTERRUPT_DISPLAY);
                }
            }
        }
    }

    // Method to display the character stored in the low-order byte of
    // the parameter. We also recognize two non-printing characters:
    // Decimal 12 (Ascii Form Feed) to clear the display
    // Decimal 7 (Ascii Bell) to place the cursor at a specified (X,Y) position.
    // of a virtual text terminal. The position is specified in the high
    // order 24 bits of the transmitter word (X in 20-31, Y in 8-19).
    // Thus the parameter is the entire word, not just the low-order byte.
    // Once the latter is performed, the display mode changes to random
    // access, which has repercussions for the implementation of character display.
    private void displayCharacter(final int intWithCharacterToDisplay) {
        final char characterToDisplay = (char) (intWithCharacterToDisplay & 0x000000FF);
        if (characterToDisplay == KeyboardAndDisplaySimulator.CLEAR_SCREEN) {
            this.initializeDisplay(this.displayRandomAccessMode);
        } else if (characterToDisplay == KeyboardAndDisplaySimulator.SET_CURSOR_X_Y) {
            // First call will activate random access mode.
            // We're using JTextArea, where caret has to be within text.
            // So initialize text to all spaces to fill the JTextArea to its
            // current capacity. Then set caret. Subsequent character
            // displays will replace, not append, in the text.
            if (!this.displayRandomAccessMode) {
                this.displayRandomAccessMode = true;
                this.initializeDisplay(true);
            }
            // For SET_CURSOR_X_Y, we need data from the rest of the word.
            // High order 3 bytes are split in half to store (X,Y) second.
            // High 12 bits contain X second, next 12 bits contain Y second.
            int x = (intWithCharacterToDisplay & 0xFFF00000) >>> 20;
            int y = (intWithCharacterToDisplay & 0x000FFF00) >>> 8;
            // If X or Y values are outside current range, set to range limit.
            if (x >= this.columns)
                x = this.columns - 1;
            if (y >= this.rows)
                y = this.rows - 1;
            // display is a JTextArea whose character positioning in the text is linear.
            // Converting (row,column) to linear position requires knowing how many columns
            // are in each row. I add one because each row except the last ends with '\n'
            // that
            // does not count as a column but occupies a position in the text string.
            // The values of rows and columns is set in initializeDisplay().
            this.display.setCaretPosition(y * (this.columns + 1) + x);
        } else {
            if (this.displayRandomAccessMode) {
                try {
                    int caretPosition = this.display.getCaretPosition();
                    // if caret is positioned at the end of a line (at the '\n'), skip over the '\n'
                    if ((caretPosition + 1) % (this.columns + 1) == 0) {
                        caretPosition++;
                        this.display.setCaretPosition(caretPosition);
                    }
                    this.display.replaceRange("" + characterToDisplay, caretPosition, caretPosition + 1);
                } catch (final IllegalArgumentException e) {
                    // tried to write off the end of the defined grid.
                    this.display.setCaretPosition(this.display.getCaretPosition() - 1);
                    this.display.replaceRange("" + characterToDisplay, this.display.getCaretPosition(),
                            this.display.getCaretPosition() + 1);
                }
            } else {
                this.display.append("" + characterToDisplay);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializePostGUI() {
        this.initializeTransmitDelaySimulator();
        this.keyEventAccepter.requestFocusInWindow();
    }

    /**
     * Method to reset counters and display when the Reset button selected.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void reset() {
        this.displayRandomAccessMode = false;
        this.initializeTransmitDelaySimulator();
        this.initializeDisplay(this.displayRandomAccessMode);
        this.keyEventAccepter.setText("");
        ((TitledBorder) this.displayPanel.getBorder()).setTitle(KeyboardAndDisplaySimulator.displayPanelTitle);
        this.displayPanel.repaint();
        this.keyEventAccepter.requestFocusInWindow();
        this.updateMMIOControl(KeyboardAndDisplaySimulator.TRANSMITTER_CONTROL,
                KeyboardAndDisplaySimulator.readyBitSet(KeyboardAndDisplaySimulator.TRANSMITTER_CONTROL));
    }

    // The display JTextArea (top half) is initialized either to the empty
    // string, or to a string filled with lines of spaces. It will do the
    // latter only if the program has sent the BELL character (Ascii 7) to
    // the transmitter. This sets the caret (cursor) to a specific (x,y) position
    // on a text-based virtual display. The lines of spaces is necessary because
    // the caret can only be placed at a position within the current text string.
    private void initializeDisplay(final boolean randomAccess) {
        String initialText = "";
        if (randomAccess) {
            final Dimension textDimensions = this.getDisplayPanelTextDimensions();
            this.columns = (int) textDimensions.getWidth();
            this.rows = (int) textDimensions.getHeight();
            this.repaintDisplayPanelBorder();
            final char[] charArray = new char[this.columns];
            Arrays.fill(charArray, KeyboardAndDisplaySimulator.VT_FILL);
            final String row = new String(charArray);
            initialText = row + ("\n" + row).repeat(Math.max(0, this.rows - 1));
        }
        this.display.setText(initialText);
        this.display.setCaretPosition(0);
    }

    // Update display window title with current text display capacity (columns and
    // rows)
    // This will be called when window resized or font changed.
    private void repaintDisplayPanelBorder() {
        final Dimension size = this.getDisplayPanelTextDimensions();
        final int cols = (int) size.getWidth();
        final int rows = (int) size.getHeight();
        final int caretPosition = this.display.getCaretPosition();
        final String stringCaretPosition;
        // display position as stream or 2D depending on random access
        if (this.displayRandomAccessMode) {
            // if ( caretPosition == rows*(columns+1)+1) {
            // stringCaretPosition = "(0,0)";
            // }
            // else if ( (caretPosition+1) % (columns+1) == 0) {
            // stringCaretPosition = "(0,"+((caretPosition/(columns+1))+1)+")";
            // }
            // else {
            // stringCaretPosition =
            // "("+(caretPosition%(columns+1))+","+(caretPosition/(columns+1))+")";
            // }
            if (((caretPosition + 1) % (this.columns + 1) != 0)) {
                stringCaretPosition =
                        "(" + (caretPosition % (this.columns + 1)) + "," + (caretPosition / (this.columns + 1))
                                + ")";
            } else if (((caretPosition + 1) % (this.columns + 1) == 0) && ((caretPosition / (this.columns + 1)) + 1 == rows)) {
                stringCaretPosition =
                        "(" + (caretPosition % (this.columns + 1) - 1) + "," + (caretPosition / (this.columns + 1))
                                + ")";
            } else {
                stringCaretPosition = "(0," + ((caretPosition / (this.columns + 1)) + 1) + ")";
            }
        } else {
            stringCaretPosition = "" + caretPosition;
        }
        final String title = KeyboardAndDisplaySimulator.displayPanelTitle + ", cursor " + stringCaretPosition + ", " +
                "area " + cols + " x " + rows;
        ((TitledBorder) this.displayPanel.getBorder()).setTitle(title);
        this.displayPanel.repaint();
    }

    // Private methods defined to support the above.


    // Calculate text display capacity of display window. Text dimensions are based
    // on pixel dimensions of window divided by font size properties.
    private Dimension getDisplayPanelTextDimensions() {
        final Dimension areaSize = this.display.getSize();
        final int widthInPixels = (int) areaSize.getWidth();
        final int heightInPixels = (int) areaSize.getHeight();
        final FontMetrics metrics = this.getFontMetrics(this.display.getFont());
        final int rowHeight = metrics.getHeight();
        final int charWidth = metrics.charWidth('m');
        // Estimate number of columns/rows of text that will fit in current window with
        // current font.
        // I subtract 1 because initial tests showed slight scroll otherwise.
        return new Dimension(widthInPixels / charWidth - 1, heightInPixels / rowHeight - 1);
    }

    @Override
    protected JComponent getHelpComponent() {
        final String helpContent = "Keyboard And Display MMIO Simulator\n\n" +
                "Use this program to simulate Memory-Mapped I/O (MMIO) for a keyboard input device and character " +
                "display output device.  It may be run either from Tools menu or as a stand-alone application. " +
                "For the latter, simply write a driver to instantiate a com.chrisps.rars.tools" +
                ".KeyboardAndDisplaySimulator object "
                +
                "and invoke its go() method.\n" +
                "\n" +
                "While the tool is connected to the program, each keystroke in the text area causes the corresponding" +
                " ASCII "
                +
                "code to be placed in the Receiver Data register (low-order byte of memory word "
                + BinaryUtils.intToHexString(KeyboardAndDisplaySimulator.RECEIVER_DATA) + "), and the " +
                "Ready bit to be set to 1 in the Receiver Control register (low-order bit of "
                + BinaryUtils.intToHexString(KeyboardAndDisplaySimulator.RECEIVER_CONTROL) + ").  The Ready " +
                "bit is automatically reset to 0 when the program reads the Receiver Data using an 'lw' instruction.\n"
                +
                "\n" +
                "A program may write to the display area by detecting the Ready bit set (1) in the Transmitter Control "
                +
                "register (low-order bit of memory word " + BinaryUtils.intToHexString(KeyboardAndDisplaySimulator.TRANSMITTER_CONTROL)
                + "), then storing the ASCII code of the character to be " +
                "displayed in the Transmitter Data register (low-order byte of "
                + BinaryUtils.intToHexString(KeyboardAndDisplaySimulator.TRANSMITTER_DATA) + ") using a 'sw' instruction." +
                " " +
                " This " +
                "triggers the simulated display to clear the Ready bit to 0, delay awhile to simulate processing the " +
                "data, "
                +
                "then set the Ready bit back to 1.  The delay is based on a count of executed instructions.\n" +
                "\n" +
                "In a polled approach to I/O, a program idles in a loop, testing the device's Ready bit on each " +
                "iteration until it is set to 1 before proceeding.  This tool also supports an interrupt-driven " +
                "approach "
                +
                "which requires the program to provide an interrupt handler but allows it to perform useful processing "
                +
                "instead of idly looping.  When the device is ready, it signals an interrupt and the RARS simuator " +
                "will "
                +
                "transfer control to the interrupt handler. Interrupt-driven I/O is enabled " +
                "when the program sets the Interrupt-Enable bit in the device's control register.  Details below.\n" +
                "\n" +
                "Upon setting the Receiver Controller's Ready bit to 1, its Interrupt-Enable bit (bit position 1) is " +
                "tested. "
                +
                "If 1, then an External Interrupt will be generated. The Interrupt-Enable " +
                "bit is 0 by default and has to be set by the program if interrupt-driven input is desired.  " +
                "Interrupt-driven "
                +
                "input permits the program to perform useful tasks instead of idling in a loop polling the Receiver " +
                "Ready bit!  "
                +
                "Very event-oriented.  The Ready bit is supposed to be read-only but in RARS it is not.\n" +
                "\n" +
                "A similar test and potential response occurs when the Transmitter Controller's Ready bit is set to 1" +
                ".  This "
                +
                "occurs after the simulated delay described above.  The only difference that utval will have a " +
                "different code.  This permits you to "
                +
                "write programs that perform interrupt-driven output - the program can perform useful tasks while the "
                +
                "output device is processing its data.  Much better than idling in a loop polling the Transmitter " +
                "Ready bit! "
                +
                "The Ready bit is supposed to be read-only but in RARS it is not.\n" +
                "\n" +
                "IMPORTANT NOTE: The Transmitter Controller Ready bit is set to its initial second of 1 only when you" +
                " click the tool's "
                +
                "'Connect to Program' button ('Assemble and Run' in the stand-alone version) or the tool's Reset " +
                "button!  If you run a "
                +
                "program and reset it in RARS, the controller's Ready bit is cleared to 0!  Configure the Data " +
                "Segment Window to "
                +
                "display the MMIO address range so you can directly observe values stored in the MMIO addresses given" +
                " above.\n"
                +
                "\n" +
                "Clear the display window from the program:\n" +
                "\n" +
                "When ASCII 12 (form feed) is stored in the Transmitter Data register, the tool's Display window will" +
                " be cleared "
                +
                "following the specified transmission delay.\n" +
                "\n" +
                "Simulate a text-based virtual terminal with (x,y) positioning:\n" +
                "\n" +
                "When ASCII 7 (bell) is stored in the Transmitter Data register, the cursor in the tool's Display " +
                "window will "
                +
                "be positioned at the (X,Y) coordinate specified by its high-order 3 bytes, following the specfied " +
                "transmission delay. "
                +
                "Place the X position (column) in bit positions 20-31 of the " +
                "Transmitter Data register and place the Y position (row) in bit positions 8-19.  The cursor is not " +
                "displayed "
                +
                "but subsequent transmitted characters will be displayed starting at that position. Position (0,0) is" +
                " at upper left. "
                +
                "Why did I select the ASCII Bell character?  Just for fun!\n" +
                "\n" +
                "The dimensions (number of columns and rows) of the virtual text-based terminal are calculated based " +
                "on the display "
                +
                "window size and font specifications.  This calculation occurs during program execution upon first " +
                "use of the ASCII 7 code. "
                +
                "It will not change until the Reset button is clicked, even if the window is resized.  The window " +
                "dimensions are included in "
                +
                "its title, which will be updated upon window resize or font change.  No attempt is made to " +
                "reposition data characters already "
                +
                "transmitted by the program.  To change the dimensions of the virtual terminal, resize the Display " +
                "window as desired (note there "
                +
                "is an adjustible splitter between the Display and Keyboard windows) then click the tool's Reset " +
                "button.  "
                +
                "Implementation detail: the window is implemented by a JTextArea to which text is written as a string. "
                +
                "Its caret (cursor) position is required to be a position within the string.  I simulated a text " +
                "terminal with random positioning "
                +
                "by pre-allocating a string of spaces with one space per (X,Y) position and an embedded newline where" +
                " each line ends. Each character "
                +
                "transmitted to the window thus replaces an existing character in the string.\n" +
                "\n" +
                "Thanks to Eric Wang at Washington State University, who requested these features to enable use of " +
                "this display as the target "
                +
                "for programming MMIO text-based games.";
        final JButton help = new JButton("Help");
        help.addActionListener(
                e -> {
                    final JTextArea ja = new JTextArea(helpContent);
                    ja.setRows(30);
                    ja.setColumns(60);
                    ja.setLineWrap(true);
                    ja.setWrapStyleWord(true);
                    // TODO: potentially implement method 2
                    // Make the Help dialog modeless (can remain visible while working with other
                    // components).
                    // Unfortunately, JOptionPane.showMessageDialog() cannot be made modeless. I
                    // found two
                    // workarounds:
                    // (1) Use JDialog and the additional work that requires
                    // (2) create JOptionPane object, get JDialog from it, make the JDialog modeless
                    // Solution 2 is shorter but requires Java 1.6. Trying to keep MARS at 1.5. So
                    // we
                    // do it the hard way. DPS 16-July-2014
                    final JDialog d;
                    final String title = "Simulating the Keyboard and Display";
                    // The following is necessary because there are different JDialog constructors
                    // for Dialog and
                    // Frame and theWindow is declared a Window, superclass for both.
                    d = (KeyboardAndDisplaySimulator.this.theWindow instanceof Dialog) ?
                            new JDialog((Dialog) KeyboardAndDisplaySimulator.this.theWindow, title, false)
                            : new JDialog((Frame) KeyboardAndDisplaySimulator.this.theWindow, title, false);
                    d.setSize(ja.getPreferredSize());
                    d.getContentPane().setLayout(new BorderLayout());
                    d.getContentPane().add(new JScrollPane(ja), BorderLayout.CENTER);
                    final JButton b = new JButton("Close");
                    b.addActionListener(
                            ev -> {
                                d.setVisible(false);
                                d.dispose();
                            });
                    final JPanel p = new JPanel(); // Flow layout will center button.
                    p.add(b);
                    d.getContentPane().add(p, BorderLayout.SOUTH);
                    d.setLocationRelativeTo(KeyboardAndDisplaySimulator.this.theWindow);
                    d.setVisible(true);
                    // This alternative technique is simpler than the above but requires java 1.6!
                    // DPS 16-July-2014
                    // JOptionPane theStuff = new JOptionPane(new
                    // JScrollPane(ja),JOptionPane.INFORMATION_MESSAGE,
                    // JOptionPane.DEFAULT_OPTION, null, new String[]{"Close"} );
                    // JDialog theDialog = theStuff.createDialog(theWindow, "Simulating the Keyboard
                    // and Display");
                    // theDialog.setModal(false);
                    // theDialog.setVisible(true);
                    // The original code. Cannot be made modeless.
                    // JOptionPane.showMessageDialog(theWindow, new JScrollPane(ja),
                    // "Simulating the Keyboard and Display", JOptionPane.INFORMATION_MESSAGE);
                });
        return help;
    }

    // UI components and layout for upper part of GUI, where simulated display is

    private JComponent buildDisplay() {
        this.displayPanel = new JPanel(new BorderLayout());
        final TitledBorder tb = new TitledBorder(KeyboardAndDisplaySimulator.displayPanelTitle);
        tb.setTitleJustification(TitledBorder.CENTER);
        this.displayPanel.setBorder(tb);
        this.display = new JTextArea();
        this.display.setFont(FONT_SETTINGS.getCurrentFont());
        this.display.setEditable(false);
        this.display.setMargin(KeyboardAndDisplaySimulator.textAreaInsets);
        final var updateDisplayBorder = new DisplayResizeAdapter();
        // To update display of size in the Display text area when window or font size
        // changes.
        this.display.addComponentListener(updateDisplayBorder);
        // To update display of caret position in the Display text area when caret
        // position changes.
        this.display.addCaretListener(
                e -> KeyboardAndDisplaySimulator.this.simulator.repaintDisplayPanelBorder());

        // 2011-07-29: Patrik Lundin, patrik@lundin.info
        // Added code so display autoscrolls.
        final DefaultCaret caret = (DefaultCaret) this.display.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        // end added autoscrolling

        final JScrollPane displayScrollPane = new JScrollPane(this.display);
        displayScrollPane.setPreferredSize(KeyboardAndDisplaySimulator.preferredTextAreaDimension);

        this.displayPanel.add(displayScrollPane);
        final JPanel displayOptions = new JPanel();
        this.delayTechniqueChooser = new JComboBox<>(this.delayTechniques);
        this.delayTechniqueChooser.setToolTipText("Technique for determining simulated transmitter device processing " +
                "delay");
        this.delayTechniqueChooser.addActionListener(
                e -> KeyboardAndDisplaySimulator.this.transmitDelayInstructionCountLimit =
                        KeyboardAndDisplaySimulator.this.generateDelay());
        this.delayLengthPanel = new DelayLengthPanel();
        this.displayAfterDelayCheckBox = new JCheckBox("DAD", true);
        this.displayAfterDelayCheckBox
                .setToolTipText("Display After Delay: if checked, transmitter data not displayed until after delay");
        this.displayAfterDelayCheckBox.addActionListener(
                e -> KeyboardAndDisplaySimulator.this.displayAfterDelay =
                        KeyboardAndDisplaySimulator.this.displayAfterDelayCheckBox.isSelected());

        displayOptions.add(this.displayAfterDelayCheckBox);
        displayOptions.add(this.delayTechniqueChooser);
        displayOptions.add(this.delayLengthPanel);
        this.displayPanel.add(displayOptions, BorderLayout.SOUTH);
        return this.displayPanel;
    }

    /// UI components and layout for lower part of GUI, where simulated keyboard is
    private JComponent buildKeyboard() {
        final JPanel keyboardPanel = new JPanel(new BorderLayout());
        this.keyEventAccepter = new JTextArea();
        this.keyEventAccepter.setEditable(true);
        this.keyEventAccepter.setFont(FONT_SETTINGS.getCurrentFont());
        this.keyEventAccepter.setMargin(KeyboardAndDisplaySimulator.textAreaInsets);
        final JScrollPane keyAccepterScrollPane = new JScrollPane(this.keyEventAccepter);
        keyAccepterScrollPane.setPreferredSize(KeyboardAndDisplaySimulator.preferredTextAreaDimension);
        this.keyEventAccepter.addKeyListener(new KeyboardKeyListener());
        keyboardPanel.add(keyAccepterScrollPane);
        final TitledBorder tb = new TitledBorder(KeyboardAndDisplaySimulator.keyboardPanelTitle);
        tb.setTitleJustification(TitledBorder.CENTER);
        keyboardPanel.setBorder(tb);
        return keyboardPanel;
    }

    // update the MMIO Control register memory cell. We will delegate.
    private void updateMMIOControl(final int addr, final int intValue) {
        this.updateMMIOControlAndData(addr, intValue, 0, 0, true);
    }

    // update the MMIO Control and Data register pair -- 2 memory cells. We will

    private void updateMMIOControlAndData(
            final int controlAddr, final int controlValue, final int dataAddr,
            final int dataValue) {
        this.updateMMIOControlAndData(controlAddr, controlValue, dataAddr, dataValue, false);
    }

    // This one does the work: update the MMIO Control and optionally the Data
    // NOTE: last argument TRUE means update only the MMIO Control register; FALSE

    private void updateMMIOControlAndData(
            final int controlAddr, final int controlValue, final int dataAddr,
            final int dataValue,
            final boolean controlOnly) {
        if (this.connectButton.isConnected()) {
            Globals.memoryAndRegistersLock.lock();
            try {
                try {
                    Globals.MEMORY_INSTANCE.setRawWord(controlAddr, controlValue);
                    if (!controlOnly)
                        Globals.MEMORY_INSTANCE.setRawWord(dataAddr, dataValue);
                } catch (final AddressErrorException aee) {
                    KeyboardAndDisplaySimulator.LOGGER.fatal("Tool author specified incorrect MMIO address!", aee);
                    System.exit(0);
                }
            } finally {
                Globals.memoryAndRegistersLock.unlock();
            }
            // HERE'S A HACK!! Want to immediately display the updated memory second in MARS
            // but that code was not written for event-driven update (e.g. Observer) --
            // it was written to poll the memory cells for their values. So we force it to
            // do so.

            if (Globals.gui != null) {
                if (Globals.gui.mainPane.executeTab.textSegment.getCodeHighlighting()) {
                    Globals.gui.mainPane.executeTab.dataSegment.updateValues();
                }
            }
        }
    }

    // Transmit delay is simulated by counting instruction executions.
    // Here we simly initialize (or reset) the variables.
    private void initializeTransmitDelaySimulator() {
        this.countingInstructions = false;
        this.instructionCount = 0;
        this.transmitDelayInstructionCountLimit = this.generateDelay();
    }

    private int generateDelay() {
        final double sliderValue = this.delayLengthPanel.getDelayLength();
        final TransmitterDelayTechnique technique =
                (TransmitterDelayTechnique) this.delayTechniqueChooser.getSelectedItem();
        return technique.generateDelay(sliderValue);
    }

    // Calculate transmitter delay (# instruction executions) based on
    // current combo box and slider settings.

    private interface TransmitterDelayTechnique {
        int generateDelay(double parameter);
    }

    // Class to grab keystrokes going to keyboard echo area and send them to MMIO


    // Delay second is fixed, and equal to slider second.
    private static class FixedLengthDelay implements TransmitterDelayTechnique {
        @Override
        public String toString() {
            return "Fixed transmitter delay, select using slider";
        }

        @Override
        public int generateDelay(final double fixedDelay) {
            return (int) fixedDelay;
        }
    }

    // Class for selecting transmitter delay lengths (# of instruction executions).

    // Randomly pick second from range 1 to slider setting, uniform distribution
    // (each second has equal probability of being chosen).
    private static class UniformlyDistributedDelay implements TransmitterDelayTechnique {
        final Random randu;

        public UniformlyDistributedDelay() {
            this.randu = new Random();
        }

        @Override
        public String toString() {
            return "Uniformly distributed delay, min=1, max=slider";
        }

        @Override
        public int generateDelay(final double max) {
            return this.randu.nextInt((int) max) + 1;
        }
    }

    // Interface and classes for Transmitter Delay-generating techniques.

    /**
     * Pretty badly-hacked normal distribution, but is more realistic than uniform!
     * Get sample from Normal(0,1) -- mean=0, s.d.=1 -- multiply it by slider
     * second, take absolute second to make sure we don't get negative,
     * add 1 to make sure we don't get 0.
     */
    private static class NormallyDistributedDelay implements TransmitterDelayTechnique {
        final Random randn;

        public NormallyDistributedDelay() {
            this.randn = new Random();
        }

        @Override
        public String toString() {
            return "'Normally' distributed delay: floor(abs(N(0,1)*slider)+1)";
        }

        @Override
        public int generateDelay(final double mult) {
            return (int) (Math.abs(this.randn.nextGaussian() * mult) + 1);
        }
    }

    // Trigger recalculation and update of display text dimensions when window
    // resized.
    private class DisplayResizeAdapter extends ComponentAdapter {
        @Override
        public void componentResized(final ComponentEvent e) {
            KeyboardAndDisplaySimulator.this.getDisplayPanelTextDimensions();
            KeyboardAndDisplaySimulator.this.repaintDisplayPanelBorder();
        }
    }

    private class KeyboardKeyListener implements KeyListener {
        @Override
        public void keyTyped(final KeyEvent e) {
            final int updatedReceiverControl =
                    KeyboardAndDisplaySimulator.readyBitSet(KeyboardAndDisplaySimulator.RECEIVER_CONTROL);
            KeyboardAndDisplaySimulator.this.updateMMIOControlAndData(KeyboardAndDisplaySimulator.RECEIVER_CONTROL,
                    updatedReceiverControl, KeyboardAndDisplaySimulator.RECEIVER_DATA,
                    e.getKeyChar() & 0x00000ff);
            if (updatedReceiverControl != 1) {
                InterruptController.registerExternalInterrupt(KeyboardAndDisplaySimulator.EXTERNAL_INTERRUPT_KEYBOARD);
            }
        }

        /* Ignore first pressed event from the text field. */
        @Override
        public void keyPressed(final KeyEvent e) {
        }

        /* Ignore first released event from the text field. */
        @Override
        public void keyReleased(final KeyEvent e) {
        }
    }

    private class DelayLengthPanel extends JPanel {
        private final static int DELAY_INDEX_MIN = 0;
        private final static int DELAY_INDEX_MAX = 40;
        private final static int DELAY_INDEX_INIT = 4;
        private final double[] delayTable = {
                1, 2, 3, 4, 5, 10, 20, 30, 40, 50, 100, // 0-10
                150, 200, 300, 400, 500, 600, 700, 800, 900, 1000, // 11-20
                1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, // 21-30
                20000, 40000, 60000, 80000, 100000, 200000, 400000, 600000, 800000, 1000000// 31-40
        };
        private JLabel sliderLabel = null;
        private volatile int delayLengthIndex = DelayLengthPanel.DELAY_INDEX_INIT;

        public DelayLengthPanel() {
            super(new BorderLayout());
            KeyboardAndDisplaySimulator.this.delayLengthSlider = new JSlider(JSlider.HORIZONTAL,
                    DelayLengthPanel.DELAY_INDEX_MIN, DelayLengthPanel.DELAY_INDEX_MAX,
                    DelayLengthPanel.DELAY_INDEX_INIT);
            KeyboardAndDisplaySimulator.this.delayLengthSlider.setSize(new Dimension(100,
                    (int) KeyboardAndDisplaySimulator.this.delayLengthSlider.getSize().getHeight()));
            KeyboardAndDisplaySimulator.this.delayLengthSlider.setMaximumSize(KeyboardAndDisplaySimulator.this.delayLengthSlider.getSize());
            KeyboardAndDisplaySimulator.this.delayLengthSlider.addChangeListener(new DelayLengthListener());
            this.sliderLabel = new JLabel(this.setLabel(this.delayLengthIndex));
            this.sliderLabel.setHorizontalAlignment(JLabel.CENTER);
            this.sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            this.add(this.sliderLabel, BorderLayout.NORTH);
            this.add(KeyboardAndDisplaySimulator.this.delayLengthSlider, BorderLayout.CENTER);
            this.setToolTipText("Parameter for simulated delay length (instruction execution count)");
        }

        // returns current delay length setting, in instructions.
        public double getDelayLength() {
            return this.delayTable[this.delayLengthIndex];
        }

        // set label wording depending on current speed setting
        private String setLabel(final int index) {
            return "Delay length: " + ((int) this.delayTable[index]) + " instruction executions";
        }

        // Both revises label as user slides and updates current index when sliding
        // stops.
        private class DelayLengthListener implements ChangeListener {
            @Override
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();
                if (!source.getValueIsAdjusting()) {
                    DelayLengthPanel.this.delayLengthIndex = source.getValue();
                    KeyboardAndDisplaySimulator.this.transmitDelayInstructionCountLimit =
                            KeyboardAndDisplaySimulator.this.generateDelay();
                } else {
                    DelayLengthPanel.this.sliderLabel.setText(DelayLengthPanel.this.setLabel(source.getValue()));
                }
            }
        }
    }
}
