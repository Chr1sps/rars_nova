package rars.tools;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.notices.AccessNotice;
import rars.riscv.hardware.registers.Register;
import rars.util.BinaryUtilsKt;
import rars.util.BinaryUtilsOld;
import rars.venus.VenusUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.Flow;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * Tool to help students learn about IEEE 754 representation of 32 bit
 * floating point values. This representation is used by the "float"
 * directive and instructions and also the Java (and most other languages)
 * "float" data type. As written, it can ALMOST be adapted to 64 bit by
 * changing a few constants.
 */
public final class FloatRepresentation extends AbstractTool {
    private static final String version = "Version 1.1";
    private static final String heading = "32-bit IEEE 754 Floating Point Representation";
    private static final String title = "Floating Point Representation, ";

    private static final String defaultHex = "00000000";
    private static final String defaultDecimal = "0.0";
    private static final String defaultBinarySign = "0";
    private static final String defaultBinaryExponent = "00000000";
    private static final String defaultBinaryFraction = "00000000000000000000000";
    private static final int maxLengthHex = 8;
    private static final int maxLengthBinarySign = 1;
    private static final int maxLengthBinaryExponent = 8;
    private static final int maxLengthBinaryFraction = 23;
    private static final int maxLengthBinaryTotal =
        FloatRepresentation.maxLengthBinarySign + FloatRepresentation.maxLengthBinaryExponent
            + FloatRepresentation.maxLengthBinaryFraction;
    private static final int maxLengthDecimal = 20;
    private static final String denormalizedLabel = "                 significand (denormalized - no 'hidden bit')";
    private static final String normalizedLabel = "                 significand ('hidden bit' underlined)       ";
    private static final Font instructionsFont = new Font("Arial", Font.PLAIN, 14);
    private static final Font hexDisplayFont = new Font("Courier", Font.PLAIN, 32);
    private static final Font binaryDisplayFont = new Font("Courier", Font.PLAIN, 18);
    private static final Font decimalDisplayFont = new Font("Courier", Font.PLAIN, 18);
    private static final Color hexDisplayColor = Color.red;
    private static final Color binaryDisplayColor = Color.black;
    private static final Color decimalDisplayColor = Color.blue;
    private static final String expansionFontTag = "<font size=\"+1\" face=\"Courier\" color=\"#000000\">";
    private static final int exponentBias = 127; // 32 bit floating point exponent bias
    // Put here because inner class cannot have static members.
    private static final String zeroes = "0000000000000000000000000000000000000000000000000000000000000000"; // 64
    private static final String HTMLspaces = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
    private final JLabel significandLabel = new JLabel(FloatRepresentation.denormalizedLabel, JLabel.CENTER);
    private final String defaultInstructions = "Modify any value then press the Enter first to update all values.";
    private Register attachedRegister = null;
    private Register[] fpRegisters;
    // Panels to hold binary displays and decorations (labels, arrows)
    private JPanel binarySignDecoratedDisplay,
        binaryExponentDecoratedDisplay, binaryFractionDecoratedDisplay;
    // Editable fields for the hex, binary and decimal representations.
    private JTextField hexDisplay, decimalDisplay,
        binarySignDisplay, binaryExponentDisplay, binaryFractionDisplay;
    // Non-editable fields to display formula translating binary to decimal.
    private JLabel expansionDisplay;
    private BinaryToDecimalFormulaGraphic binaryToDecimalFormulaGraphic;
    // Non-editable field to display instructions
    private InstructionsPane instructions;
    private Flow.Subscription subscription;

    /**
     * Simple constructor, likely used by the RARS Tools menu mechanism
     */
    public FloatRepresentation(final @NotNull VenusUI mainUI) {
        super(FloatRepresentation.title + FloatRepresentation.version, FloatRepresentation.heading, mainUI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Floating Point Representation";
    }

    /**
     * Override the inherited method, which registers us as an Observer over the
     * static data segment
     * (starting address 0x10010000) only. This version will register us as observer
     * over the selected
     * floating point register, if any. If no register is selected, it will not do
     * anything.
     * If you use the inherited GUI buttons, this method is invoked when you click
     * "Connect" button
     * on Tool or the "Assemble and Run" button on a Rars-based app.
     */
    @Override
    protected void addAsObserver() {
        this.addAsObserver(this.attachedRegister);
    }

    /**
     * Delete this app/tool as an Observer of the attached register. This overrides
     * the inherited version which deletes only as an Observer of memory.
     * This method is called when the default "Disconnect" button on a Tool is
     * selected or
     * when the program execution triggered by the default "Assemble and run" on a
     * stand-alone
     * app terminates (e.g. when the button is re-enabled).
     */
    @Override
    protected void deleteAsSubscriber() {
        this.deleteAsSubscriber(this.attachedRegister);
    }

    /**
     * Method that constructs the main display area. This will be vertically
     * sandwiched between
     * the standard heading area at the top and the control area at the bottom.
     *
     * @return the GUI component containing the application/tool-specific part of
     * the user interface
     */
    @Override
    protected JComponent buildMainDisplayArea() {
        return this.buildDisplayArea();
    }

    @Override
    protected void processAccessNotice(@NotNull final AccessNotice notice) {
        if (notice.accessType == AccessNotice.AccessType.WRITE) {
            this.updateDisplays(new FlavorsOfFloat().buildOneFromInt((int) this.attachedRegister.getValue()));
        }
    }

    /**
     * Method to reset display values to 0 when the Reset button selected.
     * If attached to a register at the time, the register will be reset as well.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void reset() {
        this.instructions.setText(this.defaultInstructions);
        this.updateDisplaysAndRegister(new FlavorsOfFloat());
    }

    /**
     * <p>buildDisplayArea.</p>
     *
     * @return a {@link javax.swing.JComponent} object
     */
    protected JComponent buildDisplayArea() {
        // Panel to hold all floating point dislay and editing components
        final Box mainPanel = Box.createVerticalBox();
        final JPanel leftPanel = new JPanel(new GridLayout(5, 1, 0, 0));
        final JPanel rightPanel = new JPanel(new GridLayout(5, 1, 0, 0));
        final Box subMainPanel = Box.createHorizontalBox();
        subMainPanel.add(leftPanel);
        subMainPanel.add(rightPanel);
        mainPanel.add(subMainPanel);

        // Editable display for hexadecimal version of the float value
        this.hexDisplay = new JTextField(FloatRepresentation.defaultHex, FloatRepresentation.maxLengthHex + 1);
        this.hexDisplay.setFont(FloatRepresentation.hexDisplayFont);
        this.hexDisplay.setForeground(FloatRepresentation.hexDisplayColor);
        this.hexDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.hexDisplay.setToolTipText(FloatRepresentation.maxLengthHex + "-digit hexadecimal (base 16) display");
        this.hexDisplay.setEditable(true);
        this.hexDisplay.revalidate();
        this.hexDisplay.addKeyListener(new HexDisplayKeystrokeListener(8));

        final JPanel hexPanel = new JPanel();
        hexPanel.add(this.hexDisplay);
        // ################ Grid Row : Hexadecimal ##################################
        leftPanel.add(hexPanel);

        final HexToBinaryGraphicPanel hexToBinaryGraphic = new HexToBinaryGraphicPanel();
        // ################ Grid Row : Hex-to-binary graphic ########################
        leftPanel.add(hexToBinaryGraphic);

        // Editable display for binary version of float value.
        // It is split into 3 separately editable components (sign,exponent,fraction)

        this.binarySignDisplay = new JTextField(
            FloatRepresentation.defaultBinarySign,
            FloatRepresentation.maxLengthBinarySign + 1
        );
        this.binarySignDisplay.setFont(FloatRepresentation.binaryDisplayFont);
        this.binarySignDisplay.setForeground(FloatRepresentation.binaryDisplayColor);
        this.binarySignDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.binarySignDisplay.setToolTipText("The sign bit");
        this.binarySignDisplay.setEditable(true);
        this.binarySignDisplay.revalidate();

        this.binaryExponentDisplay = new JTextField(
            FloatRepresentation.defaultBinaryExponent,
            FloatRepresentation.maxLengthBinaryExponent + 1
        );
        this.binaryExponentDisplay.setFont(FloatRepresentation.binaryDisplayFont);
        this.binaryExponentDisplay.setForeground(FloatRepresentation.binaryDisplayColor);
        this.binaryExponentDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.binaryExponentDisplay.setToolTipText(FloatRepresentation.maxLengthBinaryExponent + "-bit exponent");
        this.binaryExponentDisplay.setEditable(true);
        this.binaryExponentDisplay.revalidate();

        this.binaryFractionDisplay = new BinaryFractionDisplayTextField(
            FloatRepresentation.defaultBinaryFraction,
            FloatRepresentation.maxLengthBinaryFraction + 1
        );
        this.binaryFractionDisplay.setFont(FloatRepresentation.binaryDisplayFont);
        this.binaryFractionDisplay.setForeground(FloatRepresentation.binaryDisplayColor);
        this.binaryFractionDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.binaryFractionDisplay.setToolTipText(FloatRepresentation.maxLengthBinaryFraction + "-bit fraction");
        this.binaryFractionDisplay.setEditable(true);
        this.binaryFractionDisplay.revalidate();

        this.binarySignDisplay.addKeyListener(new BinaryDisplayKeystrokeListener(FloatRepresentation.maxLengthBinarySign));
        this.binaryExponentDisplay.addKeyListener(new BinaryDisplayKeystrokeListener(FloatRepresentation.maxLengthBinaryExponent));
        this.binaryFractionDisplay.addKeyListener(new BinaryDisplayKeystrokeListener(FloatRepresentation.maxLengthBinaryFraction));
        final JPanel binaryPanel = new JPanel();

        this.binarySignDecoratedDisplay = new JPanel(new BorderLayout());
        this.binaryExponentDecoratedDisplay = new JPanel(new BorderLayout());
        this.binaryFractionDecoratedDisplay = new JPanel(new BorderLayout());
        this.binarySignDecoratedDisplay.add(this.binarySignDisplay, BorderLayout.CENTER);
        this.binarySignDecoratedDisplay.add(new JLabel("sign", JLabel.CENTER), BorderLayout.SOUTH);
        this.binaryExponentDecoratedDisplay.add(this.binaryExponentDisplay, BorderLayout.CENTER);
        this.binaryExponentDecoratedDisplay.add(new JLabel("exponent", JLabel.CENTER), BorderLayout.SOUTH);
        this.binaryFractionDecoratedDisplay.add(this.binaryFractionDisplay, BorderLayout.CENTER);
        this.binaryFractionDecoratedDisplay.add(new JLabel("fraction", JLabel.CENTER), BorderLayout.SOUTH);

        binaryPanel.add(this.binarySignDecoratedDisplay);
        binaryPanel.add(this.binaryExponentDecoratedDisplay);
        binaryPanel.add(this.binaryFractionDecoratedDisplay);

        // ################ Grid Row : Binary ##################################
        leftPanel.add(binaryPanel);

        // ################ Grid Row : Binary to decimal formula arrows ##########
        this.binaryToDecimalFormulaGraphic = new BinaryToDecimalFormulaGraphic();
        this.binaryToDecimalFormulaGraphic.setBackground(leftPanel.getBackground());
        leftPanel.add(this.binaryToDecimalFormulaGraphic);

        // Non-Editable display for expansion of binary representation

        this.expansionDisplay = new JLabel(new FlavorsOfFloat().expansionString);
        this.expansionDisplay.setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.expansionDisplay.setFocusable(false); // causes it to be skipped in "tab sequence".
        this.expansionDisplay.setBackground(leftPanel.getBackground());
        final JPanel expansionDisplayBox = new JPanel(new GridLayout(2, 1));
        expansionDisplayBox.add(this.expansionDisplay);
        expansionDisplayBox.add(this.significandLabel); // initialized at top
        // ################ Grid Row : Formula mapping binary to decimal ########
        leftPanel.add(expansionDisplayBox);

        // Editable display for decimal version of float value.
        this.decimalDisplay = new JTextField(
            FloatRepresentation.defaultDecimal,
            FloatRepresentation.maxLengthDecimal + 1
        );
        this.decimalDisplay.setFont(FloatRepresentation.decimalDisplayFont);
        this.decimalDisplay.setForeground(FloatRepresentation.decimalDisplayColor);
        this.decimalDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.decimalDisplay.setToolTipText("Decimal floating point value");
        this.decimalDisplay.setMargin(new Insets(0, 0, 0, 0));
        this.decimalDisplay.setEditable(true);
        this.decimalDisplay.revalidate();
        this.decimalDisplay.addKeyListener(new DecimalDisplayKeystokeListenter());
        final Box decimalDisplayBox = Box.createVerticalBox();
        decimalDisplayBox.add(Box.createVerticalStrut(5));
        decimalDisplayBox.add(this.decimalDisplay);
        decimalDisplayBox.add(Box.createVerticalStrut(15));

        final FlowLayout rightPanelLayout = new FlowLayout(FlowLayout.LEFT);
        final JPanel place1 = new JPanel(rightPanelLayout);
        final JPanel place2 = new JPanel(rightPanelLayout);
        final JPanel place3 = new JPanel(rightPanelLayout);
        final JPanel place4 = new JPanel(rightPanelLayout);

        final JEditorPane hexExplain = new JEditorPane(
            "text/html",
            FloatRepresentation.expansionFontTag + "&lt;&nbsp;&nbsp;Hexadecimal representation" + "</font>"
        );
        hexExplain.setEditable(false);
        hexExplain.setFocusable(false);
        hexExplain.setForeground(Color.black);
        hexExplain.setBackground(place1.getBackground());
        final JEditorPane hexToBinExplain = new JEditorPane(
            "text/html",
            FloatRepresentation.expansionFontTag + "&lt;&nbsp;&nbsp;Each hex digit represents 4 bits" + "</font>"
        );
        hexToBinExplain.setEditable(false);
        hexToBinExplain.setFocusable(false);
        hexToBinExplain.setBackground(place2.getBackground());
        final JEditorPane binExplain = new JEditorPane(
            "text/html",
            FloatRepresentation.expansionFontTag + "&lt;&nbsp;&nbsp;Binary representation" + "</font>"
        );
        binExplain.setEditable(false);
        binExplain.setFocusable(false);
        binExplain.setBackground(place3.getBackground());
        final JEditorPane binToDecExplain = new JEditorPane(
            "text/html",
            FloatRepresentation.expansionFontTag + "&lt;&nbsp;&nbsp;Binary-to-decimal conversion" + "</font>"
        );
        binToDecExplain.setEditable(false);
        binToDecExplain.setFocusable(false);
        binToDecExplain.setBackground(place4.getBackground());
        place1.add(hexExplain);
        place2.add(hexToBinExplain);
        place3.add(binExplain);
        place4.add(binToDecExplain);
        // ################ 4 Grid Rows : Explanations #########################
        rightPanel.add(place1);
        rightPanel.add(place2);
        rightPanel.add(place3);
        rightPanel.add(place4);
        // ################ Grid Row : Decimal ##################################
        rightPanel.add(decimalDisplayBox);

        // ######## mainPanel is vertical box, instructions get a row #################
        final JPanel instructionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.instructions = new InstructionsPane(instructionsPanel);
        instructionsPanel.add(this.instructions);
        instructionsPanel.setBorder(new TitledBorder("Instructions"));
        mainPanel.add(instructionsPanel);

        // Means of selecting and deselecting an attached floating point register

        this.fpRegisters = Globals.FP_REGISTER_FILE.getRegisters();
        final String[] registerList = new String[this.fpRegisters.length + 1];
        registerList[0] = "None";
        for (int i = 0; i < this.fpRegisters.length; i++) {
            registerList[i + 1] = this.fpRegisters[i].name;
        }
        final JComboBox<String> registerSelect = new JComboBox<>(registerList);
        registerSelect.setSelectedIndex(0); // No register attached
        registerSelect.setToolTipText("Attach to selected FP register");
        registerSelect.addActionListener(
            e -> {
                @SuppressWarnings("unchecked")
                final var cb = (JComboBox<String>) e.getSource();
                final int selectedIndex = cb.getSelectedIndex();
                if (this.isObserving()) {
                    this.deleteAsSubscriber();
                }
                if (selectedIndex == 0) {
                    this.attachedRegister = null;
                    this.updateDisplays(new FlavorsOfFloat());
                    this.instructions.setText("The program is not attached to any floating point registers.");
                } else {
                    this.attachedRegister = this.fpRegisters[selectedIndex - 1];
                    this.updateDisplays(new FlavorsOfFloat().buildOneFromInt((int) this.attachedRegister.getValue()));
                    if (this.isObserving()) {
                        this.addAsObserver();
                    }
                    this.instructions.setText("The program and register " + this.attachedRegister.name
                        + " will respond to each other when the program is connected or running.");
                }
            });

        final JPanel registerPanel = new JPanel(new BorderLayout(5, 5));
        final JPanel registerAndLabel = new JPanel();
        registerAndLabel.add(new JLabel("Floating point Register of interest: "));
        registerAndLabel.add(registerSelect);
        registerPanel.add(registerAndLabel, BorderLayout.WEST);
        registerPanel.add(new JLabel(" "), BorderLayout.NORTH); // just for padding
        mainPanel.add(registerPanel);
        return mainPanel;
    } // end of buildDisplayArea()

    /**
     * If display is attached to a register then update the register value.
     */
    private synchronized void updateAnyAttachedRegister(final int intValue) {
        if (this.attachedRegister != null) {
            Globals.MEMORY_REGISTERS_LOCK.lock();
            try {
                this.attachedRegister.setValue(intValue | 0xFFFFFFFF_00000000L); // NaN box 32 bit value
            } finally {
                Globals.MEMORY_REGISTERS_LOCK.unlock();
            }
            // HERE'S A HACK!! Want to immediately display the updated register value in
            // RARS
            // but that code was not written for event-driven update (e.g. Observer) --
            // it was written to poll the registers for their values. So we force it to do
            // so.
            this.mainUI.registersPane.getFloatingPointWindow().updateRegisters();
        }
    }

    // Updates all components displaying various representations of the 32 bit
    // floating point value.
    private void updateDisplays(final FlavorsOfFloat flavors) {
        final int hexIndex = (
            flavors.hexString.charAt(0) == '0'
                && (flavors.hexString.charAt(1) == 'x' || flavors.hexString.charAt(1) == 'X')
        ) ? 2 : 0;
        this.hexDisplay.setText(flavors.hexString.substring(hexIndex).toUpperCase()); // lop off leading "Ox" if present
        this.binarySignDisplay.setText(flavors.binaryString.substring(0, FloatRepresentation.maxLengthBinarySign));
        this.binaryExponentDisplay.setText(
            flavors.binaryString.substring(
                FloatRepresentation.maxLengthBinarySign,
                FloatRepresentation.maxLengthBinarySign + FloatRepresentation.maxLengthBinaryExponent
            ));
        this.binaryFractionDisplay.setText(
            flavors.binaryString.substring(
                FloatRepresentation.maxLengthBinarySign + FloatRepresentation.maxLengthBinaryExponent,
                FloatRepresentation.maxLengthBinaryTotal
            ));
        this.decimalDisplay.setText(flavors.decimalString);
        this.binaryToDecimalFormulaGraphic.drawSubtractLabel(BinaryUtilsOld.binaryStringToInt(
            (
                flavors.binaryString.substring(
                    FloatRepresentation.maxLengthBinarySign,
                    FloatRepresentation.maxLengthBinarySign + FloatRepresentation.maxLengthBinaryExponent
                )
            )));
        this.expansionDisplay.setText(flavors.expansionString);
        this.updateSignificandLabel(flavors);
    }

    /// //// THE REST OF THE TOOL CONSISTS OF LITTLE PRIVATE CLASSES THAT MAKE
    /// //// LIFE EASIER FOR THE ABOVE CODE.

    // Should be called only by those who know a register should be changed due to
    // user action (reset button or Enter first on one of the input fields). Note
    // this will not update the register unless we are an active Observer.
    private void updateDisplaysAndRegister(final FlavorsOfFloat flavors) {
        this.updateDisplays(flavors);
        if (this.isObserving()) {
            this.updateAnyAttachedRegister(flavors.intValue);
        }
    }

    // Called by updateDisplays() to determine whether or not the significand label
    // needs
    // to be changed and if so to change it. The label explains presence/absence of
    // normalizing "hidden bit".
    private void updateSignificandLabel(final FlavorsOfFloat flavors) {
        // Will change significandLabel text only if it needs to be changed...
        if (flavors.binaryString.substring(
                FloatRepresentation.maxLengthBinarySign,
                FloatRepresentation.maxLengthBinarySign + FloatRepresentation.maxLengthBinaryExponent
            )
            .equals(FloatRepresentation.zeroes.substring(
                FloatRepresentation.maxLengthBinarySign,
                FloatRepresentation.maxLengthBinarySign + FloatRepresentation.maxLengthBinaryExponent
            ))) {
            // Will change text only if it truly is changing....
            if (!this.significandLabel.getText().contains("deno")) {
                this.significandLabel.setText(FloatRepresentation.denormalizedLabel);
            }
        } else {
            if (!this.significandLabel.getText().contains("unde")) {
                this.significandLabel.setText(FloatRepresentation.normalizedLabel);
            }
        }
    }

    // Use this to draw custom background in the binary fraction display.
    static class BinaryFractionDisplayTextField extends JTextField {

        public BinaryFractionDisplayTextField(final String value, final int columns) {
            super(value, columns);
        }

        // This overrides inherited JPanel method. Override is necessary to
        // assure my drawn graphics get painted immediately after painting the
        // underlying JPanel (see first statement).
        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            // The code below is commented out because I decided to abandon
            // my effort to provide "striped" background that alternates colors
            // for every 4 characters (bits) of the display. This would make
            // the correspondence between bits and hex digits very clear.
            // NOTE: this is the only reason for subclassing JTextField.

            /*
             * int columnWidth = getWidth()/getColumns();
             * Color shadedColor = Color.red;
             * Polygon p;
             * // loop will handle the lower order 5 "nibbles" (hex digits)
             * for (int i=3; i<20; i+=8) {
             * p = new Polygon();
             * p.addPoint(getX()+columnWidth*i, getY());
             * p.addPoint(getX()+columnWidth*i, getY()+getHeight());
             * p.addPoint(getX()+columnWidth*(i+4), getY()+getHeight());
             * p.addPoint(getX()+columnWidth*(i+4), getY());
             * // System.out.println("Polygon vertices are:"+
             * // " ("+(getX()+columnWidth*i) +","+getY() +") "+
             * // " ("+(getX()+columnWidth*i) +","+(getY()+getHeight())+") "+
             * // " ("+(getX()+columnWidth*(i+4))+","+(getY()+getHeight())+") "+
             * // " ("+(getX()+columnWidth*(i+4))+","+getY() +") "
             * // );
             * g.setColor(shadedColor);
             * g.fillPolygon(p);
             * }
             */
            /*
             * // Nibble 5 straddles binary display of exponent and fraction.
             * p = new Polygon();
             * p.addPoint(hexDisplay.getX()+hexColumnWidth*(hexDisplay.getColumns()-6)+
             * hexColumnWidth/2, upperY);
             * p.addPoint(binaryFractionDisplay.getX()+binaryColumnWidth*(
             * binaryFractionDisplay.getColumns()-20), lowerY);
             * p.addPoint(binaryExponentDisplay.getX()+binaryColumnWidth*(
             * binaryExponentDisplay.getColumns()-1), lowerY);
             * g.fillPolygon(p);
             * // Nibble 6 maps to binary display of exponent.
             * p = new Polygon();
             * p.addPoint(hexDisplay.getX()+hexColumnWidth*(hexDisplay.getColumns()-7)+
             * hexColumnWidth/2, upperY);
             * p.addPoint(binaryExponentDisplay.getX()+binaryColumnWidth*(
             * binaryExponentDisplay.getColumns()-1), lowerY);
             * p.addPoint(binaryExponentDisplay.getX()+binaryColumnWidth*(
             * binaryExponentDisplay.getColumns()-5), lowerY);
             * g.fillPolygon(p);
             * // Nibble 7 straddles binary display of sign and exponent.
             * p = new Polygon();
             * p.addPoint(hexDisplay.getX()+hexColumnWidth*(hexDisplay.getColumns()-8)+
             * hexColumnWidth/2, upperY);
             * p.addPoint(binaryExponentDisplay.getX()+binaryColumnWidth*(
             * binaryExponentDisplay.getColumns()-5), lowerY);
             * p.addPoint(binarySignDisplay.getX(), lowerY);
             * g.fillPolygon(p);
             */
        }
    }

    // NOTE: It would be nice to use InputVerifier class to verify user input
    // but I want keystroke-level monitoring to assure that no invalid
    // keystrokes are echoed and that maximum string length is not exceeded.

    // Class of objects that encapsulats 5 different representations of a 32 bit
    // floating point value:
    // string with hexadecimal value.
    // String with binary value. 32 characters long.
    // String with decimal float value. variable length.
    // int with 32 bit representation of float value ("int bits").
    // String for display only, showing formula for expanding bits to decimal.
    private final class FlavorsOfFloat {
        String hexString;
        String binaryString;
        String decimalString;
        String expansionString;
        int intValue;

        // Default object
        private FlavorsOfFloat() {
            this.hexString = FloatRepresentation.defaultHex;
            this.decimalString = FloatRepresentation.defaultDecimal;
            this.binaryString =
                FloatRepresentation.defaultBinarySign + FloatRepresentation.defaultBinaryExponent + FloatRepresentation.defaultBinaryFraction;
            this.expansionString = FlavorsOfFloat.buildExpansionFromBinaryString(this.binaryString);
            this.intValue = Float.floatToIntBits(Float.parseFloat(this.decimalString));
        }

        // Build binary expansion formula for display -- will not be editable.
        public static String buildExpansionFromBinaryString(final String binaryString) {
            final int biasedExponent = BinaryUtilsOld.binaryStringToInt(
                binaryString.substring(
                    FloatRepresentation.maxLengthBinarySign,
                    FloatRepresentation.maxLengthBinarySign + FloatRepresentation.maxLengthBinaryExponent
                ));
            final String stringExponent = Integer.toString(biasedExponent - FloatRepresentation.exponentBias);
            // stringExponent length will range from 1 to 4 (e.g. "0" to "-128") characters.
            // Right-pad with HTML spaces ("&nbsp;") to total length 5 displayed characters.
            return "<html><head></head><body>" + FloatRepresentation.expansionFontTag
                + "-1<sup>" + binaryString.charAt(0) + "</sup> &nbsp;*&nbsp; 2<sup>"
                + stringExponent + FloatRepresentation.HTMLspaces.substring(0, (5 - stringExponent.length()) * 6)
                + "</sup> &nbsp;* &nbsp;"
                + ((biasedExponent == 0) ? "&nbsp;." : "<u>1</u>.")
                + binaryString.substring(
                FloatRepresentation.maxLengthBinarySign + FloatRepresentation.maxLengthBinaryExponent,
                FloatRepresentation.maxLengthBinaryTotal
            )
                + " =</font></body></html>";
        }

        // Handy utility. Pads with leading zeroes to specified length, maximum 64 of
        // 'em.
        private static String addLeadingZeroes(final String str, final int length) {
            return (str.length() < length)
                ? FloatRepresentation.zeroes.substring(
                0, Math.min(
                    FloatRepresentation.zeroes.length(),
                    length - str.length()
                )
            ) + str
                : str;
        }

        // Assign all fields given a string representing 32 bit hex value.
        public FlavorsOfFloat buildOneFromHexString(final String hexString) {
            this.hexString = "0x" + FlavorsOfFloat.addLeadingZeroes(
                (
                    (hexString.indexOf("0X") == 0 || hexString.indexOf("0x") == 0)
                        ? hexString.substring(2)
                        : hexString
                ),
                FloatRepresentation.maxLengthHex
            );
            this.binaryString = BinaryUtilsOld.hexStringToBinaryString(this.hexString);
            this.decimalString = Float.toString(Float.intBitsToFloat(BinaryUtilsOld.binaryStringToInt(this.binaryString)));
            this.expansionString = FlavorsOfFloat.buildExpansionFromBinaryString(this.binaryString);
            this.intValue = BinaryUtilsOld.binaryStringToInt(this.binaryString);
            return this;
        }

        // Assign all fields given a string representing 32 bit binary value
        private FlavorsOfFloat buildOneFromBinaryString() {
            this.binaryString = this.getFullBinaryStringFromDisplays();
            this.hexString = BinaryUtilsOld.binaryStringToHexString(this.binaryString);
            this.decimalString = Float.toString(Float.intBitsToFloat(BinaryUtilsOld.binaryStringToInt(this.binaryString)));
            this.expansionString = FlavorsOfFloat.buildExpansionFromBinaryString(this.binaryString);
            this.intValue = BinaryUtilsOld.binaryStringToInt(this.binaryString);
            return this;
        }

        // Assign all fields given string representing floating point decimal value.
        private @Nullable FlavorsOfFloat buildOneFromDecimalString(final String decimalString) {
            final float floatValue;
            try {
                floatValue = Float.parseFloat(decimalString);
            } catch (final NumberFormatException nfe) {
                return null;
            }
            this.decimalString = Float.toString(floatValue);
            this.intValue = Float.floatToIntBits(floatValue);// use floatToRawIntBits?
            this.binaryString = BinaryUtilsKt.intToBinaryString(this.intValue);
            this.hexString = BinaryUtilsOld.binaryStringToHexString(this.binaryString);
            this.expansionString = FlavorsOfFloat.buildExpansionFromBinaryString(this.binaryString);
            return this;
        }

        // Assign all fields given int representing 32 bit representation of float value
        private FlavorsOfFloat buildOneFromInt(final int intValue) {
            this.intValue = intValue;
            this.binaryString = BinaryUtilsKt.intToBinaryString(intValue);
            this.hexString = BinaryUtilsOld.binaryStringToHexString(this.binaryString);
            this.decimalString = Float.toString(Float.intBitsToFloat(BinaryUtilsOld.binaryStringToInt(this.binaryString)));
            this.expansionString = FlavorsOfFloat.buildExpansionFromBinaryString(this.binaryString);
            return this;
        }

        // Handy utility to concatentate the binary field values into one 32 character
        // string
        // Left-pad each field with zeroes as needed to reach its full length.
        private String getFullBinaryStringFromDisplays() {
            return FlavorsOfFloat.addLeadingZeroes(
                FloatRepresentation.this.binarySignDisplay.getText(),
                FloatRepresentation.maxLengthBinarySign
            ) +
                FlavorsOfFloat.addLeadingZeroes(
                    FloatRepresentation.this.binaryExponentDisplay.getText(),
                    FloatRepresentation.maxLengthBinaryExponent
                ) +
                FlavorsOfFloat.addLeadingZeroes(
                    FloatRepresentation.this.binaryFractionDisplay.getText(),
                    FloatRepresentation.maxLengthBinaryFraction
                );
        }

    }

    // Class to handle input keystrokes for hexadecimal field
    private class HexDisplayKeystrokeListener extends KeyAdapter {

        private final int digitLength; // maximum number of digits long

        public HexDisplayKeystrokeListener(final int length) {
            this.digitLength = length;
        }

        // handy utility.
        private static boolean isHexDigit(final char digit) {
            return switch (digit) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
                     'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F' -> true;
                default -> false;
            };
        }

        // Process user keystroke. If not valid for the context, this
        // will consume the stroke and beep.
        @Override
        public void keyTyped(final KeyEvent e) {
            final JTextField source = (JTextField) e.getComponent();
            if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE || e.getKeyChar() == KeyEvent.VK_TAB) {
                return;
            }
            if (!HexDisplayKeystrokeListener.isHexDigit(e.getKeyChar()) ||
                source.getText().length() == this.digitLength && source.getSelectedText() == null) {
                if (e.getKeyChar() != KeyEvent.VK_ENTER && e.getKeyChar() != KeyEvent.VK_TAB) {
                    Toolkit.getDefaultToolkit().beep();
                    if (source.getText().length() == this.digitLength && source.getSelectedText() == null) {
                        FloatRepresentation.this.instructions.setText("Maximum length of this field is " + this.digitLength + ".");
                    } else {
                        FloatRepresentation.this.instructions.setText("Only digits and A-F (or a-f) are accepted in " +
                            "hexadecimal field.");
                    }
                }
                e.consume();
            }
        }

        // Enter first is echoed on component after keyPressed but before keyTyped?
        // Consuming the VK_ENTER event in keyTyped does not suppress it but this will.
        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyChar() == KeyEvent.VK_ENTER || e.getKeyChar() == KeyEvent.VK_TAB) {
                FloatRepresentation.this.updateDisplaysAndRegister(
                    new FlavorsOfFloat().buildOneFromHexString(((JTextField) e.getSource()).getText()));
                FloatRepresentation.this.instructions.setText(FloatRepresentation.this.defaultInstructions);
                e.consume();
            }
        }
    }

    // Class to handle input keystrokes for binary field
    private class BinaryDisplayKeystrokeListener extends KeyAdapter {

        private final int bitLength; // maximum number of bits permitted

        public BinaryDisplayKeystrokeListener(final int length) {
            this.bitLength = length;
        }

        // handy utility
        private static boolean isBinaryDigit(final char digit) {
            return switch (digit) {
                case '0', '1' -> true;
                default -> false;
            };
        }

        // Process user keystroke. If not valid for the context, this
        // will consume the stroke and beep.
        @Override
        public void keyTyped(final KeyEvent e) {
            final JTextField source = (JTextField) e.getComponent();
            if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
                return;
            }
            if (!BinaryDisplayKeystrokeListener.isBinaryDigit(e.getKeyChar()) ||
                e.getKeyChar() == KeyEvent.VK_ENTER ||
                source.getText().length() == this.bitLength && source.getSelectedText() == null) {
                if (e.getKeyChar() != KeyEvent.VK_ENTER) {
                    Toolkit.getDefaultToolkit().beep();
                    if (source.getText().length() == this.bitLength && source.getSelectedText() == null) {
                        FloatRepresentation.this.instructions.setText("Maximum length of this field is " + this.bitLength + ".");
                    } else {
                        FloatRepresentation.this.instructions.setText("Only 0 and 1 are accepted in binary field.");
                    }
                }
                e.consume();
            }
        }

        // Enter first is echoed on component after keyPressed but before keyTyped?
        // Consuming the VK_ENTER event in keyTyped does not suppress it but this will.
        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                FloatRepresentation.this.updateDisplaysAndRegister(new FlavorsOfFloat().buildOneFromBinaryString());
                FloatRepresentation.this.instructions.setText(FloatRepresentation.this.defaultInstructions);
                e.consume();
            }
        }

    }

    // Class to handle input keystrokes for decimal field
    private class DecimalDisplayKeystokeListenter extends KeyAdapter {

        // handy utility
        private static boolean isDecimalFloatDigit(final char digit) {
            return switch (digit) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+',
                     '.', 'e', 'E' -> true;
                default -> false;
            };
        }

        // Process user keystroke. If not valid for the context, this
        // will consume the stroke and beep.
        @Override
        public void keyTyped(final KeyEvent e) {
            if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
                return;
            }
            if (!DecimalDisplayKeystokeListenter.isDecimalFloatDigit(e.getKeyChar())) {
                if (e.getKeyChar() != KeyEvent.VK_ENTER) {
                    FloatRepresentation.this.instructions.setText("Only digits, period, signs and E (or e) are " +
                        "accepted in decimal field.");
                    Toolkit.getDefaultToolkit().beep();
                }
                e.consume();
            }
        }

        // Enter first is echoed on component after keyPressed but before keyTyped?
        // Consuming the VK_ENTER event in keyTyped does not suppress it but this will.
        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                final FlavorsOfFloat fof = new FlavorsOfFloat()
                    .buildOneFromDecimalString(((JTextField) e.getSource()).getText());
                if (fof == null) {
                    Toolkit.getDefaultToolkit().beep();
                    FloatRepresentation.this.instructions.setText(
                        "'" + ((JTextField) e.getSource()).getText() + "' is not a valid floating point number.");
                } else {
                    FloatRepresentation.this.updateDisplaysAndRegister(fof);
                    FloatRepresentation.this.instructions.setText(FloatRepresentation.this.defaultInstructions);
                }
                e.consume();
            }
        }

    }

    // Use this to draw graphics visually relating the hexadecimal values
    // displayed above) to the binary values (displayed below).
    class HexToBinaryGraphicPanel extends JPanel {

        // This overrides inherited JPanel method. Override is necessary to
        // assure my drawn graphics get painted immediately after painting the
        // underlying JPanel (see first statement).
        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.red);
            // FontMetrics fontMetrics = hexDisplay.getGraphics().getFontMetrics();
            final int upperY = 0;
            final int lowerY = 60;
            final int hexColumnWidth =
                FloatRepresentation.this.hexDisplay.getWidth() / FloatRepresentation.this.hexDisplay.getColumns();
            // assume all 3 binary displays use same geometry, so column width same for all.
            final int binaryColumnWidth =
                FloatRepresentation.this.binaryFractionDisplay.getWidth() / FloatRepresentation.this.binaryFractionDisplay.getColumns();
            Polygon p;
            // loop will handle the lower order 5 "nibbles" (hex digits)
            for (int i = 1; i < 6; i++) {
                p = new Polygon();
                p.addPoint(
                    FloatRepresentation.this.hexDisplay.getX() + hexColumnWidth * (FloatRepresentation.this.hexDisplay.getColumns() - i) + hexColumnWidth / 2,
                    upperY
                );
                p.addPoint(
                    FloatRepresentation.this.binaryFractionDecoratedDisplay.getX()
                        + binaryColumnWidth * (FloatRepresentation.this.binaryFractionDisplay.getColumns() - ((i * 5) - i)),
                    lowerY
                );
                p.addPoint(
                    FloatRepresentation.this.binaryFractionDecoratedDisplay.getX()
                        + binaryColumnWidth * (FloatRepresentation.this.binaryFractionDisplay.getColumns() - (((i * 5) - i) - 4)),
                    lowerY
                );
                g.fillPolygon(p);
            }
            // Nibble 5 straddles binary display of exponent and fraction.
            p = new Polygon();
            p.addPoint(
                FloatRepresentation.this.hexDisplay.getX() + hexColumnWidth * (FloatRepresentation.this.hexDisplay.getColumns() - 6) + hexColumnWidth / 2,
                upperY
            );
            p.addPoint(
                FloatRepresentation.this.binaryFractionDecoratedDisplay.getX()
                    + binaryColumnWidth * (FloatRepresentation.this.binaryFractionDisplay.getColumns() - 20), lowerY
            );
            p.addPoint(
                FloatRepresentation.this.binaryExponentDecoratedDisplay.getX()
                    + binaryColumnWidth * (FloatRepresentation.this.binaryExponentDisplay.getColumns() - 1), lowerY
            );
            g.fillPolygon(p);
            // Nibble 6 maps to binary display of exponent.
            p = new Polygon();
            p.addPoint(
                FloatRepresentation.this.hexDisplay.getX() + hexColumnWidth * (FloatRepresentation.this.hexDisplay.getColumns() - 7) + hexColumnWidth / 2,
                upperY
            );
            p.addPoint(
                FloatRepresentation.this.binaryExponentDecoratedDisplay.getX()
                    + binaryColumnWidth * (FloatRepresentation.this.binaryExponentDisplay.getColumns() - 1), lowerY
            );
            p.addPoint(
                FloatRepresentation.this.binaryExponentDecoratedDisplay.getX()
                    + binaryColumnWidth * (FloatRepresentation.this.binaryExponentDisplay.getColumns() - 5), lowerY
            );
            g.fillPolygon(p);
            // Nibble 7 straddles binary display of sign and exponent.
            p = new Polygon();
            p.addPoint(
                FloatRepresentation.this.hexDisplay.getX() + hexColumnWidth * (FloatRepresentation.this.hexDisplay.getColumns() - 8) + hexColumnWidth / 2,
                upperY
            );
            p.addPoint(
                FloatRepresentation.this.binaryExponentDecoratedDisplay.getX()
                    + binaryColumnWidth * (FloatRepresentation.this.binaryExponentDisplay.getColumns() - 5), lowerY
            );
            p.addPoint(FloatRepresentation.this.binarySignDecoratedDisplay.getX(), lowerY);
            g.fillPolygon(p);
        }

    }

    // Handly little class defined only to allow client to use "setText()" without
    // needing to know how/whether the text needs to be formatted. This one is
    // used to display instructions.

    // Panel to hold arrows explaining transformation of binary represntation
    // into formula for calculating decimal value.
    class BinaryToDecimalFormulaGraphic extends JPanel {
        final String subtractLabelTrailer = " - 127";
        final int arrowHeadOffset = 5;
        final int lowerY = 0;
        final int upperY = 50;
        final int centerY = (this.upperY - this.lowerY) / 2;
        final int upperYArrowHead = this.upperY - this.arrowHeadOffset;
        int centerX, exponentCenterX;
        int subtractLabelWidth, subtractLabelHeight;
        int currentExponent = BinaryUtilsOld.binaryStringToInt(FloatRepresentation.defaultBinaryExponent);

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            // Arrow down from binary sign field
            this.centerX =
                FloatRepresentation.this.binarySignDecoratedDisplay.getX() + FloatRepresentation.this.binarySignDecoratedDisplay.getWidth() / 2;
            g.drawLine(this.centerX, this.lowerY, this.centerX, this.upperY);
            g.drawLine(this.centerX - this.arrowHeadOffset, this.upperYArrowHead, this.centerX, this.upperY);
            g.drawLine(this.centerX + this.arrowHeadOffset, this.upperYArrowHead, this.centerX, this.upperY);
            // Arrow down from binary exponent field
            this.centerX =
                FloatRepresentation.this.binaryExponentDecoratedDisplay.getX() + FloatRepresentation.this.binaryExponentDecoratedDisplay.getWidth() / 2;
            g.drawLine(this.centerX, this.lowerY, this.centerX, this.upperY);
            g.drawLine(this.centerX - this.arrowHeadOffset, this.upperYArrowHead, this.centerX, this.upperY);
            g.drawLine(this.centerX + this.arrowHeadOffset, this.upperYArrowHead, this.centerX, this.upperY);
            // Label on exponent arrow. The two assignments serve to initialize two
            // instance variables that are used by drawSubtractLabel(). They are
            // initialized here because they cannot be initialized sooner AND because
            // the drawSubtractLabel() method will later be called by updateDisplays(),
            // an outsider which has no other access to that information. Once set they
            // do not change so it does no harm that they are "re-initialized" each time
            // this method is called (which occurs only upon startup and when this portion
            // of the GUI needs to be repainted).
            this.exponentCenterX = this.centerX;
            this.subtractLabelHeight = g.getFontMetrics().getHeight();
            this.drawSubtractLabel(g, this.buildSubtractLabel(this.currentExponent));
            // Arrow down from binary fraction field
            this.centerX =
                FloatRepresentation.this.binaryFractionDecoratedDisplay.getX() + FloatRepresentation.this.binaryFractionDecoratedDisplay.getWidth() / 2;
            g.drawLine(this.centerX, this.lowerY, this.centerX, this.upperY);
            g.drawLine(this.centerX - this.arrowHeadOffset, this.upperYArrowHead, this.centerX, this.upperY);
            g.drawLine(this.centerX + this.arrowHeadOffset, this.upperYArrowHead, this.centerX, this.upperY);
        }

        // To be used only by "outsiders" to update the display of the exponent and
        // bias.
        public void drawSubtractLabel(final int exponent) {
            if (exponent != this.currentExponent) { // no need to redraw if it hasn't changed...
                this.currentExponent = exponent;
                this.drawSubtractLabel(this.getGraphics(), this.buildSubtractLabel(exponent));
            }
        }

        // Is called by both drawSubtractLabel() just above and by paintComponent().
        private void drawSubtractLabel(final Graphics g, final String label) {
            // Clear the existing subtract label. The "+2" overwrites the arrow at initial
            // paint when label width is 0.
            // Originally used "clearRect()" but changed to "fillRect()" with background
            // color, because when running
            // as a Tool it would clear with a different color.
            final Color saved = g.getColor();
            g.setColor(FloatRepresentation.this.binaryToDecimalFormulaGraphic.getBackground());
            g.fillRect(
                this.exponentCenterX - this.subtractLabelWidth / 2, this.centerY - this.subtractLabelHeight / 2,
                this.subtractLabelWidth + 2, this.subtractLabelHeight
            );
            g.setColor(saved);
            this.subtractLabelWidth = g.getFontMetrics().stringWidth(label);
            g.drawString(
                label, this.exponentCenterX - this.subtractLabelWidth / 2,
                this.centerY + this.subtractLabelHeight / 2 - 3
            ); // -3
            // makes
            // it
            // more
            // visually
            // appealing
        }

        // format the label for a given integer exponent value...
        private String buildSubtractLabel(final int value) {
            return value + this.subtractLabelTrailer;
        }

    }

    class InstructionsPane extends JLabel {

        InstructionsPane(final Component parent) {
            super(FloatRepresentation.this.defaultInstructions);
            this.setFont(FloatRepresentation.instructionsFont);
            this.setBackground(parent.getBackground());
        }

        @Override
        public void setText(final String text) {
            super.setText(text);
        }
    }

}
