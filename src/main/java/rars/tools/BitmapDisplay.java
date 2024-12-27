package rars.tools;

import org.jetbrains.annotations.Nullable;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;
import rars.riscv.hardware.Memory;
import rars.util.GraphicsPanel;
import rars.util.Grid;
import rars.util.SimpleSubscriber;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/*
Copyright (c) 2010-2011,  Pete Sanderson and Kenneth Vollmar

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
 * Bitmapp display simulator. It can be run either as a stand-alone Java
 * application having
 * access to the rars package, or through RARS as an item in its
 * Tools menu. It makes
 * maximum use of methods inherited from its abstract superclass
 * AbstractToolAndApplication.
 * <br>
 * Pete Sanderson, verison 1.0, 23 December 2010.
 * <br>
 * <br>
 * Modified in 2024 to be more user-friendly and to remove stand-alone app functionality, which wasn't used.
 * <br>
 * Chr1sps, 2024
 */
public class BitmapDisplay extends AbstractTool {

    private static final String name = "Bitmap Display";
    // Some GUI settings
    private final EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
    private final JLabel preSpinnerLabel = new JLabel("0x"), postSpinnerLabel = new JLabel("0000");
    // Major GUI components
    private JSlider pixelSizeSlider, displayHeightSlider, displayWidthSlider;
    private JSpinner baseAddressSpinner;
    private JLabel pixelSizeLabel, displayHeightLabel, displayWidthLabel;

    // Values for display canvas.
    private int unitPixelSize = 1;
    private int displayAreaWidthInPixels = 512;
    private int displayAreaHeightInPixels = 256;

    private int baseAddress = Memory.dataBaseAddress;

    private Grid theGrid;

    private GridWindow gridWindow;

    /**
     * Simple constructor.
     */
    public BitmapDisplay() {
        super(BitmapDisplay.name, BitmapDisplay.name);
    }

    // Use this for consistent results.
    private static JPanel getPanelWithBorderLayout() {
        return new JPanel(new BorderLayout(2, 2));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return BitmapDisplay.name;
    }

    /**
     * Override the inherited method, which registers us as an Observer over the
     * static data segment
     * (starting address 0x10010000) only. This version will register us as observer
     * over the
     * the memory range as selected by the base address combo box and capacity of
     * the visualization display
     * (number of visualization elements times the number of memory words each one
     * represents).
     * It does so by calling the inherited 2-parameter overload of this method.
     * If you use the inherited GUI buttons, this
     * method is invoked when you click "Connect" button on Tool or the
     * "Assemble and Run" button on a Rars-based app.
     */
    @Override
    protected void addAsObserver() {
        int highAddress = this.baseAddress + this.theGrid.rows * this.theGrid.columns * Memory.WORD_LENGTH_BYTES;
        // Special case: baseAddress<0 means we're in kernel memory (0x80000000 and up)
        // and most likely
        // in memory map address space (0xffff0000 and up). In this case, we need to
        // make sure the high address
        // does not drop off the high end of 32 bit address space. Highest allowable
        // word address is 0xfffffffc,
        // which is interpreted in Java int as -4.
        if (this.baseAddress < 0 && highAddress > -4) {
            highAddress = -4;
        }
        this.addAsObserver(this.baseAddress, highAddress);
    }

    // Rest of the protected methods. These override do-nothing methods inherited
    ////////////////////////////////////////////////////////////////////////////////////// from
    // the abstract superclass.

    /**
     * Method that constructs the main display area. It is organized vertically
     * into two major components: the display configuration which an be modified
     * using combo boxes, and the visualization display which is updated as the
     * attached program executes.
     *
     * @return the GUI component containing these two areas
     */
    @Override
    protected JComponent buildMainDisplayArea() {
        this.gridWindow = new GridWindow();
        this.dialog.setResizable(false);
        return this.buildOrganizationArea();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Update display when the connected program accesses (data) memory.
     */
    @Override
    protected void processRISCVUpdate(final AccessNotice accessNotice) {
        if (accessNotice.getAccessType() == AccessNotice.AccessType.WRITE) {
            this.updateColorForAddress((MemoryAccessNotice) accessNotice);
        }
    }

    /**
     * Initialize all JComboBox choice structures not already initialized at
     * declaration.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void initializePreGUI() {
        // NOTE: Can't call "createNewGrid()" here because it uses settings from
        // several combo boxes that have not been created yet. But a default grid
        // needs to be allocated for initial canvas display.
        this.theGrid = new Grid(this.displayAreaHeightInPixels / this.unitPixelSize,
                this.displayAreaWidthInPixels / this.unitPixelSize);
    }

    /**
     * The only post-GUI initialization is to create the initial Grid object based
     * on the default settings
     * of the various combo boxes. Overrides inherited method that does nothing.
     */
    @Override
    protected void initializePostGUI() {
        this.connectButton.addConnectListener((connected) -> {
            this.gridWindow.setVisible(connected);
            this.displayHeightSlider.setEnabled(!connected);
            this.displayWidthSlider.setEnabled(!connected);
            this.pixelSizeSlider.setEnabled(!connected);
            this.baseAddressSpinner.setEnabled(!connected);
            this.preSpinnerLabel.setEnabled(!connected);
            this.postSpinnerLabel.setEnabled(!connected);
            if (connected) {
                SimpleSubscriber.LOGGER.debug("Connected. Expected window size: {}. Actual window size: {}", this.getNewGridWindowSize(), this.gridWindow.getSize());
            }
        });
        this.theGrid = this.createNewGrid();
        this.updateBaseAddress();
    }

    /**
     * Method to reset counters and display when the Reset button selected.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void reset() {
        this.theGrid.reset();
        this.updateDisplay();
    }

    /**
     * Updates display immediately after each update (AccessNotice) is processed,
     * after
     * display configuration changes as needed, and after each execution step when
     * Rars
     * is running in timed mode. Overrides inherited method that does nothing.
     */
    @Override
    protected void updateDisplay() {
        this.gridWindow.canvas.repaint();
    }

    // Private methods defined to support the above.

    /**
     * Overrides default method, to provide a Help button for this tool/app.
     *
     * @return a {@link javax.swing.JComponent} object
     */
    @Override
    protected JComponent getHelpComponent() {
        final String helpContent = """
                Use this program to simulate a basic bitmap display where
                each memory word in a specified address space corresponds to
                one display pixel in row-major order starting at the upper left
                corner of the display.  This tool may be run either from the
                Tools menu or as a stand-alone application.
                You can easily learn to use this small program by playing with
                it!   Each rectangular unit on the display represents one memory
                word in a contiguous address space starting with the specified
                base address.  The value stored in that word will be interpreted
                as a 24-bit RGB color value with the red component in bits 16-23,
                the green component in bits 8-15, and the blue component in bits 0-7.
                Each time a memory word within the display address space is written
                by the program, its position in the display will be rendered in the
                color that its value represents.
                """;
        final JButton help = new JButton("Help");
        help.addActionListener(
                e -> JOptionPane.showMessageDialog(BitmapDisplay.this.theWindow, helpContent));
        return help;
    }

    // UI components and layout for left half of GUI, where settings are specified.
    private JComponent buildOrganizationArea() {
        final JPanel organization = new JPanel(new GridLayout(4, 1));

        this.pixelSizeLabel = new JLabel("Unit size in pixels: " + this.unitPixelSize);
        this.displayWidthLabel = new JLabel("Display width in pixels: " + this.displayAreaWidthInPixels);
        this.displayHeightLabel = new JLabel("Display height in pixels: " + this.displayAreaHeightInPixels);
        this.pixelSizeSlider = new JSlider(JSlider.HORIZONTAL, 1, 32, this.unitPixelSize);
        this.pixelSizeSlider.setMajorTickSpacing(31);
        this.pixelSizeSlider.setMinorTickSpacing(1);
        this.pixelSizeSlider.setSnapToTicks(true);
        this.pixelSizeSlider.setPaintTicks(true);
        this.pixelSizeSlider.setPaintLabels(true);
        this.pixelSizeSlider.setToolTipText("Width in pixels of rectangle representing memory word");
        this.pixelSizeSlider.addChangeListener(
                e -> {
                    BitmapDisplay.this.unitPixelSize = BitmapDisplay.this.pixelSizeSlider.getValue();
                    BitmapDisplay.this.pixelSizeLabel.setText("Unit size in pixels: " + BitmapDisplay.this.unitPixelSize);
                    BitmapDisplay.this.theGrid = BitmapDisplay.this.createNewGrid();
                    BitmapDisplay.this.gridWindow.resize();
                    BitmapDisplay.this.updateDisplay();
                });
        this.displayWidthSlider = new JSlider(JSlider.HORIZONTAL, 64, 1024, this.displayAreaWidthInPixels);
        this.displayWidthSlider.setMajorTickSpacing(960);
        this.displayWidthSlider.setMinorTickSpacing(64);
        this.displayWidthSlider.setSnapToTicks(true);
        this.displayWidthSlider.setPaintTicks(true);
        this.displayWidthSlider.setPaintLabels(true);
        this.displayWidthSlider.setToolTipText("Total width in pixels of display area");
        this.displayWidthSlider.addChangeListener(
                e -> {
                    BitmapDisplay.this.displayAreaWidthInPixels = BitmapDisplay.this.displayWidthSlider.getValue();
                    BitmapDisplay.this.displayWidthLabel.setText("Display width in pixels: " + BitmapDisplay.this.displayAreaWidthInPixels);
                    BitmapDisplay.this.theGrid = BitmapDisplay.this.createNewGrid();
                    BitmapDisplay.this.gridWindow.resize();
                    BitmapDisplay.this.updateDisplay();
                });
        this.displayHeightSlider = new JSlider(JSlider.HORIZONTAL, 64, 1024, this.displayAreaHeightInPixels);
        this.displayHeightSlider.setMajorTickSpacing(960);
        this.displayHeightSlider.setMinorTickSpacing(64);
        this.displayHeightSlider.setSnapToTicks(true);
        this.displayHeightSlider.setPaintTicks(true);
        this.displayHeightSlider.setPaintLabels(true);
        this.displayHeightSlider.setToolTipText("Total height in pixels of display area");
        this.displayHeightSlider.addChangeListener(
                e -> {
                    BitmapDisplay.this.displayAreaHeightInPixels = BitmapDisplay.this.displayHeightSlider.getValue();
                    BitmapDisplay.this.displayHeightLabel.setText("Display height in pixels: " + BitmapDisplay.this.displayAreaHeightInPixels);
                    BitmapDisplay.this.theGrid = BitmapDisplay.this.createNewGrid();
                    BitmapDisplay.this.gridWindow.resize();
                    BitmapDisplay.this.updateDisplay();
                });
        this.baseAddressSpinner = new JSpinner(new SpinnerNumberModel(this.baseAddress >> 16, 0x0000, 0xFFFF, 1));
        final var editor = (JSpinner.DefaultEditor) this.baseAddressSpinner.getEditor();
        final var txt = editor.getTextField();
        txt.setFormatterFactory(new DefaultFormatterFactory() {
            @Override
            public JFormattedTextField.AbstractFormatter getDefaultFormatter() {
                final var formatter = new NumberFormatter() {
                    @Override
                    public Object stringToValue(final String text) {
                        try {
                            var value = Integer.parseInt(text, 16);
                            value = Math.clamp(value, 0, 0xFFFF);
                            return value;
                        } catch (final NumberFormatException e) {
                            return 0;
                        }
                    }

                    @Override
                    public String valueToString(final Object value) {
                        return Integer.toHexString((int) value).toUpperCase();
                    }

                };
                formatter.setValueClass(Integer.class);
                formatter.setFormat(new NumberFormat() {
                    @Override
                    public StringBuffer format(final double number, final StringBuffer toAppendTo, final FieldPosition pos) {
                        return new StringBuffer(Integer.toHexString((int) number).toUpperCase());
                    }

                    @Override
                    public StringBuffer format(final long number, final StringBuffer toAppendTo, final FieldPosition pos) {
                        return new StringBuffer(Integer.toHexString((int) number).toUpperCase());
                    }

                    @Override
                    public @Nullable Number parse(final String source, final ParsePosition parsePosition) {
                        try {
                            int value = Integer.parseInt(source, 16);
                            value = Math.clamp(value, 0, 0xFFFF);
                            parsePosition.setIndex(source.length());
                            return value;
                        } catch (final NumberFormatException e) {
                            parsePosition.setErrorIndex(0);
                            return null;
                        }
                    }
                });
                return formatter;
            }
        });
        this.baseAddressSpinner.setToolTipText("Base address for display area (upper left corner)");
        this.baseAddressSpinner.addChangeListener(
                e -> {
                    // This may also affect what address range we should be registered as an
                    // Observer
                    // for. The default (inherited) address range is the static data segment
                    // starting at 0x10010000. To change this requires override of
                    // AbstractToolAndApplication.addAsObserver(). The no-argument version of
                    // that method is called automatically when "Connect" button is clicked for Tool
                    // and when "Assemble and Run" button is clicked for Rars application.
                    BitmapDisplay.this.updateBaseAddress();
                    // If display base address is changed while connected to the program (this can
                    // only occur
                    // when being used as a Tool), we have to delete ourselves as an observer and
                    // re-register.
                    if (BitmapDisplay.this.connectButton != null && BitmapDisplay.this.connectButton.isConnected()) {
                        BitmapDisplay.this.deleteAsSubscriber();
                        BitmapDisplay.this.addAsObserver();
                    }
                    BitmapDisplay.this.theGrid = BitmapDisplay.this.createNewGrid();
                    BitmapDisplay.this.updateDisplay();
                });

        // ALL COMPONENTS FOR "ORGANIZATION" SECTION

        final JPanel pixelSizeRow = BitmapDisplay.getPanelWithBorderLayout();
        pixelSizeRow.setBorder(this.emptyBorder);
        pixelSizeRow.add(this.pixelSizeLabel, BorderLayout.WEST);
        pixelSizeRow.add(this.pixelSizeSlider, BorderLayout.EAST);

        final JPanel displayWidthRow = BitmapDisplay.getPanelWithBorderLayout();
        displayWidthRow.setBorder(this.emptyBorder);
        displayWidthRow.add(this.displayWidthLabel, BorderLayout.WEST);
        displayWidthRow.add(this.displayWidthSlider, BorderLayout.EAST);

        final JPanel displayHeightRow = BitmapDisplay.getPanelWithBorderLayout();
        displayHeightRow.setBorder(this.emptyBorder);
        displayHeightRow.add(this.displayHeightLabel, BorderLayout.WEST);
        displayHeightRow.add(this.displayHeightSlider, BorderLayout.EAST);

        final var baseAddressPickerPanel = new JPanel();
        baseAddressPickerPanel.setLayout(new BoxLayout(baseAddressPickerPanel, BoxLayout.X_AXIS));
        baseAddressPickerPanel.add(this.preSpinnerLabel);
        baseAddressPickerPanel.add(this.baseAddressSpinner);
        baseAddressPickerPanel.add(this.postSpinnerLabel);

        final JPanel baseAddressRow = BitmapDisplay.getPanelWithBorderLayout();
        baseAddressRow.setBorder(this.emptyBorder);
        baseAddressRow.add(new JLabel("Base address for display "), BorderLayout.WEST);
        baseAddressRow.add(baseAddressPickerPanel, BorderLayout.EAST);

        // Lay 'em out in the grid...
        organization.add(pixelSizeRow);
        organization.add(displayWidthRow);
        organization.add(displayHeightRow);
        organization.add(baseAddressRow);
        return organization;
    }

    // Update based on the spinner second.
    private void updateBaseAddress() {
        this.baseAddress = (int) this.baseAddressSpinner.getValue() << 16;
    }

    // Method to determine grid dimensions based on current control settings.
    // Each grid element corresponds to one visualization unit.
    private Grid createNewGrid() {
        return new Grid(this.displayAreaHeightInPixels, this.displayAreaWidthInPixels);
    }

    // Given memory address, update color for the corresponding grid element.
    private void updateColorForAddress(final MemoryAccessNotice notice) {
        final int address = notice.getAddress();
        final int offset = (address - this.baseAddress) / Memory.WORD_LENGTH_BYTES;


        try {
            final var color = new Color(Memory.getInstance().getWord(address / Memory.WORD_LENGTH_BYTES * Memory.WORD_LENGTH_BYTES));
            final var row = offset / this.theGrid.columns;
            final var column = offset % this.theGrid.columns;
            this.theGrid.grid[row][column] = color;
        } catch (final Exception e) {
            // If address is out of range for display, do nothing.
        }
    }

    // Specialized inner classes for modeling and animation.

    private Dimension getNewGridWindowSize() {
        return new Dimension(this.displayAreaWidthInPixels * this.unitPixelSize,
                this.displayAreaHeightInPixels * this.unitPixelSize);
    }

    private class GridWindow extends JFrame {
        private final GraphicsPanel canvas;

        public GridWindow() {
            this.canvas = new GraphicsPanel(getNewGridWindowSize(), theGrid);
            this.setTitle("Bitmap Display");
            this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            this.setMinimumSize(new Dimension(0, 0));
            this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            this.add(this.canvas);
            this.setResizable(false);
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(final WindowEvent e) {
                    BitmapDisplay.this.connectButton.disconnect();
                }
            });
            this.resize();
        }

        void resize() {
            final var newSize = BitmapDisplay.this.getNewGridWindowSize();
            this.canvas.setPreferredSize(newSize);
            this.canvas.setMinimumSize(newSize);
            this.canvas.setMaximumSize(newSize);
            this.pack();
            SimpleSubscriber.LOGGER.debug("Resized to {}", newSize);
        }
    }
}
