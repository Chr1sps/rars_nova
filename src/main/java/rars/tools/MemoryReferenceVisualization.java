package rars.tools;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.assembler.DataTypes;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;
import rars.util.BinaryUtils;
import rars.venus.VenusUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Arrays;

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
 * Memory reference visualization. It can be run either as a stand-alone Java
 * application having
 * access to the com.chrisps.rars package, or through RARS as an item in its
 * Tools menu. It makes
 * maximum use of methods inherited from its abstract superclass
 * AbstractToolAndApplication.
 * Pete Sanderson, verison 1.0, 14 November 2006.
 */
public final class MemoryReferenceVisualization extends AbstractTool {

    private static final String version = "Version 1.0";
    private static final String heading = "Visualizing memory reference patterns";
    private static final String[] wordsPerUnitChoices = {
        "1", "2", "4", "8", "16", "32", "64", "128", "256", "512",
        "1024", "2048"
    };
    private static final int defaultWordsPerUnitIndex = 0;
    private static final String[] visualizationUnitPixelWidthChoices = {"1", "2", "4", "8", "16", "32"};
    private static final int defaultVisualizationUnitPixelWidthIndex = 4;
    private static final String[] visualizationUnitPixelHeightChoices = {"1", "2", "4", "8", "16", "32"};

    // Values for Combo Boxes
    private static final int defaultVisualizationUnitPixelHeightIndex = 4;
    private static final String[] displayAreaPixelWidthChoices = {"64", "128", "256", "512", "1024"};
    private static final int defaultDisplayWidthIndex = 2;
    private static final String[] displayAreaPixelHeightChoices = {"64", "128", "256", "512", "1024"};
    private static final int defaultDisplayHeightIndex = 2;
    private static final boolean defaultDrawHashMarks = true;
    private static final int COUNT_INDEX_INIT = 10; // array element #10, arbitrary starting point
    // Some GUI settings
    private final EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
    private final Color backgroundColor = Color.WHITE;
    // This array of (count,color) pairs must be kept sorted! count is low end of
    // subrange.
    // This array will grow if user adds colors at additional counter points (see
    // below).
    private final CounterColor[] defaultCounterColors = {
        new CounterColor(0, Color.black),
        new CounterColor(1, Color.blue),
        new CounterColor(2, Color.green),
        new CounterColor(3, Color.yellow),
        new CounterColor(5, Color.orange),
        new CounterColor(10, Color.red)
    };
    /*
     * Values for reference count color slider. These are all possible counter
     * values for which
     * colors can be assigned. As you can see just above, not all these values are
     * assigned
     * a default color.
     */
    private final int[] countTable = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, // 0-10
        20, 30, 40, 50, 100, 200, 300, 400, 500, 1000, // 11-20
        2000, 3000, 4000, 5000, 10000, 50000, 100000, 500000, 1000000 // 21-29
    };

    // Values for display canvas. Note their initialization uses the identifiers
    // just above.
    // Major GUI components
    private JComboBox<String> wordsPerUnitSelector, visualizationUnitPixelWidthSelector,
        visualizationUnitPixelHeightSelector,
        visualizationPixelWidthSelector, visualizationPixelHeightSelector, displayBaseAddressSelector;
    private JCheckBox drawHashMarksSelector;
    private JPanel canvas;
    private int unitPixelWidth = Integer
        .parseInt(MemoryReferenceVisualization.visualizationUnitPixelWidthChoices[MemoryReferenceVisualization.defaultVisualizationUnitPixelWidthIndex]);
    private int unitPixelHeight = Integer
        .parseInt(MemoryReferenceVisualization.visualizationUnitPixelHeightChoices[MemoryReferenceVisualization.defaultVisualizationUnitPixelHeightIndex]);

    // `Values for mapping of reference counts to colors for display.
    private int wordsPerUnit =
        Integer.parseInt(MemoryReferenceVisualization.wordsPerUnitChoices[MemoryReferenceVisualization.defaultWordsPerUnitIndex]);
    private int visualizationAreaWidthInPixels = Integer
        .parseInt(MemoryReferenceVisualization.displayAreaPixelWidthChoices[MemoryReferenceVisualization.defaultDisplayWidthIndex]);
    private int visualizationAreaHeightInPixels = Integer
        .parseInt(MemoryReferenceVisualization.displayAreaPixelHeightChoices[MemoryReferenceVisualization.defaultDisplayHeightIndex]);
    // The next four are initialized dynamically in initializeDisplayBaseChoices()
    private String[] displayBaseAddressChoices;
    private int[] displayBaseAddresses;
    private int defaultBaseAddressIndex;
    private int baseAddress;

    private Grid theGrid;
    private CounterColorScale counterColorScale;

    public MemoryReferenceVisualization(final @NotNull VenusUI mainUI) {
        super(
            "Memory Reference Visualization, " + MemoryReferenceVisualization.version,
            MemoryReferenceVisualization.heading, mainUI
        );
    }

    // Will return int equivalent of specified combo box's current selection.
    // The selection must be a String that parses to an int.
    private static int getIntComboBoxSelection(final JComboBox<String> comboBox) {
        try {
            return Integer.parseInt((String) comboBox.getSelectedItem());
        } catch (final NumberFormatException nfe) {
            // Can occur only if initialization list contains badly formatted numbers. This
            // is a developer's error, not a user error, and better be caught before
            // release.
            return 1;
        }
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
        return "Memory Reference Visualization";
    }

    // Rest of the protected methods. These override do-nothing methods inherited
    ////////////////////////////////////////////////////////////////////////////////////// from
    // the abstract superclass.

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
        int highAddress = this.baseAddress
            + this.theGrid.getRows() * this.theGrid.getColumns() * DataTypes.WORD_SIZE * this.wordsPerUnit;
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
        final JPanel results = new JPanel();
        results.add(this.buildOrganizationArea());
        results.add(this.buildVisualizationArea());
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processRISCVUpdate(final AccessNotice accessNotice) {
        this.incrementReferenceCountForAddress(((MemoryAccessNotice) accessNotice).address);
    }

    /**
     * Initialize all JComboBox choice structures not already initialized at
     * declaration.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void initializePreGUI() {
        this.initializeDisplayBaseChoices();
        this.counterColorScale = new CounterColorScale(this.defaultCounterColors);
        // NOTE: Can't call "createNewGrid()" here because it uses settings from
        // several combo boxes that have not been created yet. But a default grid
        // needs to be allocated for initial canvas display.
        this.theGrid = new Grid(
            this.visualizationAreaHeightInPixels / this.unitPixelHeight,
            this.visualizationAreaWidthInPixels / this.unitPixelWidth
        );
    }

    /**
     * The only post-GUI initialization is to create the initial Grid object based
     * on the default settings
     * of the various combo boxes. Overrides inherited method that does nothing.
     */
    @Override
    protected void initializePostGUI() {
        this.wordsPerUnit = MemoryReferenceVisualization.getIntComboBoxSelection(this.wordsPerUnitSelector);
        this.theGrid = this.createNewGrid();
        this.updateBaseAddress();
    }

    /**
     * Method to reset counters and display when the Reset button selected.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void reset() {
        this.resetCounts();
        this.updateDisplay();
    }

    // Private methods defined to support the above.

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateDisplay() {
        this.canvas.repaint();
    }

    /**
     * Overrides default method, to provide a Help button for this tool/app.
     *
     * @return a {@link javax.swing.JComponent} object
     */
    @Override
    protected JComponent getHelpComponent() {
        final String helpContent = """
            Use this program to visualize dynamic memory reference
            patterns in assembly programs.  It may be run either
            from Tools menu or as a stand-alone application.  For
            the latter, simply write a small driver to instantiate a
            MemoryReferenceVisualization object and invoke its go() method.
            You can easily learn to use this small program by playing with
            it!  For the best animation, set the program to run in
            timed mode using the Run Speed slider.  Each rectangular unit
            on the display represents one or more memory words (default 1)
            and each time a memory word is accessed by the program,
            its reference count is incremented then rendered in the color
            assigned to the count value.  You can change the count-color
            assignments using the count slider and color patch.  Select a
            counter value then click on the color patch to change the color.
            This color will apply beginning at the selected count and
            extending up to the next slider-provided count.
            """;
        final JButton help = new JButton("Help");
        help.addActionListener(
            e -> JOptionPane.showMessageDialog(MemoryReferenceVisualization.this.theWindow, helpContent));
        return help;
    }

    // UI components and layout for left half of GUI, where settings are specified.
    private JComponent buildOrganizationArea() {
        final JPanel organization = new JPanel(new GridLayout(9, 1));

        this.drawHashMarksSelector = new JCheckBox();
        this.drawHashMarksSelector.setSelected(MemoryReferenceVisualization.defaultDrawHashMarks);
        this.drawHashMarksSelector.addActionListener(
            e -> MemoryReferenceVisualization.this.updateDisplay());
        this.wordsPerUnitSelector = new JComboBox<>(MemoryReferenceVisualization.wordsPerUnitChoices);
        this.wordsPerUnitSelector.setEditable(false);
        this.wordsPerUnitSelector.setBackground(this.backgroundColor);
        this.wordsPerUnitSelector.setSelectedIndex(MemoryReferenceVisualization.defaultWordsPerUnitIndex);
        this.wordsPerUnitSelector
            .setToolTipText("Number of memory words represented by one visualization element (rectangle)");
        this.wordsPerUnitSelector.addActionListener(
            e -> {
                MemoryReferenceVisualization.this.wordsPerUnit =
                    MemoryReferenceVisualization.getIntComboBoxSelection(MemoryReferenceVisualization.this.wordsPerUnitSelector);
                MemoryReferenceVisualization.this.reset();
            });
        this.visualizationUnitPixelWidthSelector =
            new JComboBox<>(MemoryReferenceVisualization.visualizationUnitPixelWidthChoices);
        this.visualizationUnitPixelWidthSelector.setEditable(false);
        this.visualizationUnitPixelWidthSelector.setBackground(this.backgroundColor);
        this.visualizationUnitPixelWidthSelector.setSelectedIndex(MemoryReferenceVisualization.defaultVisualizationUnitPixelWidthIndex);
        this.visualizationUnitPixelWidthSelector.setToolTipText("Width in pixels of rectangle representing memory " +
            "access");
        this.visualizationUnitPixelWidthSelector.addActionListener(
            e -> {
                MemoryReferenceVisualization.this.unitPixelWidth =
                    MemoryReferenceVisualization.getIntComboBoxSelection(MemoryReferenceVisualization.this.visualizationUnitPixelWidthSelector);
                MemoryReferenceVisualization.this.theGrid = MemoryReferenceVisualization.this.createNewGrid();
                MemoryReferenceVisualization.this.updateDisplay();
            });
        this.visualizationUnitPixelHeightSelector =
            new JComboBox<>(MemoryReferenceVisualization.visualizationUnitPixelHeightChoices);
        this.visualizationUnitPixelHeightSelector.setEditable(false);
        this.visualizationUnitPixelHeightSelector.setBackground(this.backgroundColor);
        this.visualizationUnitPixelHeightSelector.setSelectedIndex(MemoryReferenceVisualization.defaultVisualizationUnitPixelHeightIndex);
        this.visualizationUnitPixelHeightSelector.setToolTipText("Height in pixels of rectangle representing memory " +
            "access");
        this.visualizationUnitPixelHeightSelector.addActionListener(
            e -> {
                MemoryReferenceVisualization.this.unitPixelHeight =
                    MemoryReferenceVisualization.getIntComboBoxSelection(MemoryReferenceVisualization.this.visualizationUnitPixelHeightSelector);
                MemoryReferenceVisualization.this.theGrid = MemoryReferenceVisualization.this.createNewGrid();
                MemoryReferenceVisualization.this.updateDisplay();
            });
        this.visualizationPixelWidthSelector =
            new JComboBox<>(MemoryReferenceVisualization.displayAreaPixelWidthChoices);
        this.visualizationPixelWidthSelector.setEditable(false);
        this.visualizationPixelWidthSelector.setBackground(this.backgroundColor);
        this.visualizationPixelWidthSelector.setSelectedIndex(MemoryReferenceVisualization.defaultDisplayWidthIndex);
        this.visualizationPixelWidthSelector.setToolTipText("Total width in pixels of visualization area");
        this.visualizationPixelWidthSelector.addActionListener(
            e -> {
                MemoryReferenceVisualization.this.visualizationAreaWidthInPixels =
                    MemoryReferenceVisualization.getIntComboBoxSelection(MemoryReferenceVisualization.this.visualizationPixelWidthSelector);
                MemoryReferenceVisualization.this.canvas.setPreferredSize(MemoryReferenceVisualization.this.getDisplayAreaDimension());
                MemoryReferenceVisualization.this.canvas.setSize(MemoryReferenceVisualization.this.getDisplayAreaDimension());
                MemoryReferenceVisualization.this.theGrid = MemoryReferenceVisualization.this.createNewGrid();
                MemoryReferenceVisualization.this.canvas.repaint();
                MemoryReferenceVisualization.this.updateDisplay();
            });
        this.visualizationPixelHeightSelector =
            new JComboBox<>(MemoryReferenceVisualization.displayAreaPixelHeightChoices);
        this.visualizationPixelHeightSelector.setEditable(false);
        this.visualizationPixelHeightSelector.setBackground(this.backgroundColor);
        this.visualizationPixelHeightSelector.setSelectedIndex(MemoryReferenceVisualization.defaultDisplayHeightIndex);
        this.visualizationPixelHeightSelector.setToolTipText("Total height in pixels of visualization area");
        this.visualizationPixelHeightSelector.addActionListener(
            e -> {
                MemoryReferenceVisualization.this.visualizationAreaHeightInPixels =
                    MemoryReferenceVisualization.getIntComboBoxSelection(MemoryReferenceVisualization.this.visualizationPixelHeightSelector);
                MemoryReferenceVisualization.this.canvas.setPreferredSize(MemoryReferenceVisualization.this.getDisplayAreaDimension());
                MemoryReferenceVisualization.this.canvas.setSize(MemoryReferenceVisualization.this.getDisplayAreaDimension());
                MemoryReferenceVisualization.this.theGrid = MemoryReferenceVisualization.this.createNewGrid();
                MemoryReferenceVisualization.this.canvas.repaint();
                MemoryReferenceVisualization.this.updateDisplay();
            });
        this.displayBaseAddressSelector = new JComboBox<>(this.displayBaseAddressChoices);
        this.displayBaseAddressSelector.setEditable(false);
        this.displayBaseAddressSelector.setBackground(this.backgroundColor);
        this.displayBaseAddressSelector.setSelectedIndex(this.defaultBaseAddressIndex);
        this.displayBaseAddressSelector.setToolTipText("Base address for visualization area (upper left corner)");
        this.displayBaseAddressSelector.addActionListener(
            e -> {
                // This may also affect what address range we should be registered as an
                // Observer
                // for. The default (inherited) address range is the MIPS static data segment
                // starting at 0x10010000. To change this requires override of
                // AbstractToolAndApplication.addAsObserver(). The no-argument version of
                // that method is called automatically when "Connect" button is clicked for Tool
                // and when "Assemble and Run" button is clicked for Rars application.
                MemoryReferenceVisualization.this.updateBaseAddress();
                // If display base address is changed while connected to a program (this can
                // only occur
                // when being used as a Tool), we have to delete ourselves as an observer and
                // re-register.
                if (MemoryReferenceVisualization.this.connectButton != null && MemoryReferenceVisualization.this.connectButton.isConnected()) {
                    MemoryReferenceVisualization.this.deleteAsSubscriber();
                    MemoryReferenceVisualization.this.addAsObserver();
                }
                MemoryReferenceVisualization.this.theGrid = MemoryReferenceVisualization.this.createNewGrid();
                MemoryReferenceVisualization.this.updateDisplay();
            });

        // ALL COMPONENTS FOR "ORGANIZATION" SECTION

        final JPanel hashMarksRow = MemoryReferenceVisualization.getPanelWithBorderLayout();
        hashMarksRow.setBorder(this.emptyBorder);
        hashMarksRow.add(new JLabel("Show unit boundaries (grid marks)"), BorderLayout.WEST);
        hashMarksRow.add(this.drawHashMarksSelector, BorderLayout.EAST);

        final JPanel wordsPerUnitRow = MemoryReferenceVisualization.getPanelWithBorderLayout();
        wordsPerUnitRow.setBorder(this.emptyBorder);
        wordsPerUnitRow.add(new JLabel("Memory Words per Unit "), BorderLayout.WEST);
        wordsPerUnitRow.add(this.wordsPerUnitSelector, BorderLayout.EAST);

        final JPanel unitWidthInPixelsRow = MemoryReferenceVisualization.getPanelWithBorderLayout();
        unitWidthInPixelsRow.setBorder(this.emptyBorder);
        unitWidthInPixelsRow.add(new JLabel("Unit Width in Pixels "), BorderLayout.WEST);
        unitWidthInPixelsRow.add(this.visualizationUnitPixelWidthSelector, BorderLayout.EAST);

        final JPanel unitHeightInPixelsRow = MemoryReferenceVisualization.getPanelWithBorderLayout();
        unitHeightInPixelsRow.setBorder(this.emptyBorder);
        unitHeightInPixelsRow.add(new JLabel("Unit Height in Pixels "), BorderLayout.WEST);
        unitHeightInPixelsRow.add(this.visualizationUnitPixelHeightSelector, BorderLayout.EAST);

        final JPanel widthInPixelsRow = MemoryReferenceVisualization.getPanelWithBorderLayout();
        widthInPixelsRow.setBorder(this.emptyBorder);
        widthInPixelsRow.add(new JLabel("Display Width in Pixels "), BorderLayout.WEST);
        widthInPixelsRow.add(this.visualizationPixelWidthSelector, BorderLayout.EAST);

        final JPanel heightInPixelsRow = MemoryReferenceVisualization.getPanelWithBorderLayout();
        heightInPixelsRow.setBorder(this.emptyBorder);
        heightInPixelsRow.add(new JLabel("Display Height in Pixels "), BorderLayout.WEST);
        heightInPixelsRow.add(this.visualizationPixelHeightSelector, BorderLayout.EAST);

        final JPanel baseAddressRow = MemoryReferenceVisualization.getPanelWithBorderLayout();
        baseAddressRow.setBorder(this.emptyBorder);
        baseAddressRow.add(new JLabel("Base address for display "), BorderLayout.WEST);
        baseAddressRow.add(this.displayBaseAddressSelector, BorderLayout.EAST);

        final ColorChooserControls colorChooserControls = new ColorChooserControls();

        // Lay 'em out in the grid...
        organization.add(hashMarksRow);
        organization.add(wordsPerUnitRow);
        organization.add(unitWidthInPixelsRow);
        organization.add(unitHeightInPixelsRow);
        organization.add(widthInPixelsRow);
        organization.add(heightInPixelsRow);
        organization.add(baseAddressRow);
        organization.add(colorChooserControls.colorChooserRow);
        organization.add(colorChooserControls.countDisplayRow);
        return organization;
    }

    // UI components and layout for right half of GUI, the visualization display
    // area.
    private JComponent buildVisualizationArea() {
        this.canvas = new GraphicsPanel();
        this.canvas.setPreferredSize(this.getDisplayAreaDimension());
        this.canvas.setToolTipText("Memory reference count visualization area");
        return this.canvas;
    }

    // For greatest flexibility, initialize the display base choices directly from
    // the constants defined in the Memory class. This method called prior to
    // building the GUI. Here are current values from Memory.java:
    // textBaseAddress=0x00400000, dataSegmentBaseAddress=0x10000000,
    // globalPointer=0x10008000
    // dataBaseAddress=0x10010000, heapBaseAddress=0x10040000,
    // memoryMapBaseAddress=0xffff0000
    private void initializeDisplayBaseChoices() {
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        final int[] displayBaseAddressArray = {
            memoryConfiguration.textBaseAddress,
            memoryConfiguration.dataSegmentBaseAddress,
            memoryConfiguration.globalPointerAddress,
            memoryConfiguration.dataBaseAddress,
            memoryConfiguration.heapBaseAddress,
            memoryConfiguration.memoryMapBaseAddress
        };
        // Must agree with above in number and order...
        this.displayBaseAddresses = displayBaseAddressArray;
        this.displayBaseAddressChoices = new String[displayBaseAddressArray.length];
        final String[] descriptions = {
            " (text)", " (global data)", " (gp)", " (static data)", " (heap)", " (memory " +
            "map)"
        };
        for (int i = 0; i < this.displayBaseAddressChoices.length; i++) {
            this.displayBaseAddressChoices[i] = BinaryUtils.intToHexString(displayBaseAddressArray[i])
                + descriptions[i];
        }
        this.defaultBaseAddressIndex = 3; // default to 0x10010000 (static data)
        this.baseAddress = displayBaseAddressArray[this.defaultBaseAddressIndex];
    }

    // update based on combo box selection (currently not editable but that may
    // change).
    private void updateBaseAddress() {
        this.baseAddress = this.displayBaseAddresses[this.displayBaseAddressSelector.getSelectedIndex()];
        /*
         * If you want to extend this app to allow user to edit combo box, you can
         * always
         * parse the getSelectedItem() value, because the pre-defined items are all
         * formatted
         * such that the first 10 characters contain the integer's hex value. And if the
         * value is user-entered, the numeric part cannot exceed 10 characters for a
         * 32-bit
         * address anyway. So if the value is > 10 characters long, slice off the first
         * 10 and apply Integer.parseInt() to it to get custom base address.
         */
    }

    // Returns Dimension object with current width and height of display area as
    // determined
    // by current settings of respective combo boxes.
    private Dimension getDisplayAreaDimension() {
        return new Dimension(this.visualizationAreaWidthInPixels, this.visualizationAreaHeightInPixels);
    }

    // reset all counters in the Grid.
    private void resetCounts() {
        this.theGrid.reset();
    }

    // Method to determine grid dimensions based on durrent control settings.
    // Each grid element corresponds to one visualization unit.
    private Grid createNewGrid() {
        final int rows = this.visualizationAreaHeightInPixels / this.unitPixelHeight;
        final int columns = this.visualizationAreaWidthInPixels / this.unitPixelWidth;
        return new Grid(rows, columns);
    }

    // Given memory address, increment the counter for the corresponding grid
    // element.
    // Need to consider words per unit (number of memory words that each visual
    // element represents).
    // If address maps to invalid grid element (e.g. is outside the current bounds
    // based on all
    // display settings) then nothing happens.
    private void incrementReferenceCountForAddress(final int address) {
        final int offset = (address - this.baseAddress) / DataTypes.WORD_SIZE / this.wordsPerUnit;
        // If you care to do anything with it, the following will return -1 if the
        // address
        // maps outside the dimensions of the grid (e.g. below the base address or
        // beyond end).
        this.theGrid.incrementElement(offset / this.theGrid.getColumns(), offset % this.theGrid.getColumns());
    }

    // Specialized inner classes for modeling and animation.

    // Object that represents mapping from counter value to color it is displayed

    /// ///////////////////////////////////////////////////////////////////////////// as.
    private static class CounterColorScale {
        CounterColor[] counterColors;

        CounterColorScale(final CounterColor[] colors) {
            this.counterColors = colors;
        }

        // return color associated with specified counter value
        private Color getColor(final int count) {
            Color result = this.counterColors[0].associatedColor;
            int index = 0;
            while (index < this.counterColors.length && count >= this.counterColors[index].colorRangeStart) {
                result = this.counterColors[index].associatedColor;
                index++;
            }
            return result;
        }

        // For a given counter value, return the counter value at the high end of the
        // range of
        // counter values having the same color.
        private int getHighEndOfRange(final int count) {
            int highEnd = Integer.MAX_VALUE;
            if (count < this.counterColors[this.counterColors.length - 1].colorRangeStart) {
                int index = 0;
                while (index < this.counterColors.length - 1 && count >= this.counterColors[index].colorRangeStart) {
                    highEnd = this.counterColors[index + 1].colorRangeStart - 1;
                    index++;
                }
            }
            return highEnd;
        }

        // The given entry should either be inserted into the the scale or replace an
        // existing
        // element. The latter occurs if the new CounterColor has same starting counter
        // value
        // as an existing one.
        private void insertOrReplace(final CounterColor newColor) {
            final int index = Arrays.binarySearch(this.counterColors, newColor);
            if (index >= 0) { // found, so replace
                this.counterColors[index] = newColor;
            } else { // not found, so insert
                final int insertIndex = -index - 1;
                final CounterColor[] newSortedArray = new CounterColor[this.counterColors.length + 1];
                System.arraycopy(this.counterColors, 0, newSortedArray, 0, insertIndex);
                System.arraycopy(
                    this.counterColors, insertIndex, newSortedArray, insertIndex + 1,
                    this.counterColors.length - insertIndex
                );
                newSortedArray[insertIndex] = newColor;
                this.counterColors = newSortedArray;
            }
        }
    }

    // Class that simply defines UI controls for use with slider to view and/or
    // change the color associated with each memory reference count value.

    // Each object represents beginning of a counter value range (non-negative
    /////////////////////////////////////////////////////////////////////////////////////// integer)
    /////////////////////////////////////////////////////////////////////////////////////// and
    // color for rendering the range. High end of the range is defined as low end of

    /// //////////////////////////////////////////////////////////////////////////////////// the
    // next range minus 1. For last range, high end is Integer.MAX_VALUE.
    private static class CounterColor implements Comparable<CounterColor> {
        private final int colorRangeStart;
        private final Color associatedColor;

        public CounterColor(final int start, final Color color) {
            this.colorRangeStart = start;
            this.associatedColor = color;
        }

        // Necessary for sorting in ascending order of range low end.
        @Override
        public int compareTo(final CounterColor other) {
            return this.colorRangeStart - other.colorRangeStart;
        }
    }

    // Represents grid of memory access counts
    private static final class Grid {

        final int[][] grid;
        final int rows;
        final int columns;

        private Grid(final int rows, final int columns) {
            this.grid = new int[rows][columns];
            this.rows = rows;
            this.columns = columns;
            // automatically initialized to 0, so I won't bother to....
        }

        private int getRows() {
            return this.rows;
        }

        private int getColumns() {
            return this.columns;
        }

        // Returns value in given grid element; -1 if row or column is out of range.
        private int getElement(final int row, final int column) {
            return (row >= 0 && row <= this.rows && column >= 0 && column <= this.columns) ? this.grid[row][column] :
                -1;
        }

        // Returns value in given grid element without doing any row/column index
        // checking.
        // Is faster than getElement but will throw array index out of bounds exception
        // if
        // parameter values are outside the bounds of the grid.
        private int getElementFast(final int row, final int column) {
            return this.grid[row][column];
        }

        // Increment the given grid element and return incremented value.
        // Returns -1 if row or column is out of range.
        private int incrementElement(final int row, final int column) {
            return (row >= 0 && row <= this.rows && column >= 0 && column <= this.columns) ?
                ++this.grid[row][column] : -1;
        }

        // Just set all grid elements to 0.
        private void reset() {
            for (int i = 0; i < this.rows; i++) {
                for (int j = 0; j < this.columns; j++) {
                    this.grid[i][j] = 0;
                }
            }
        }
    }

    // Class that represents the panel for visualizing and animating memory

    /// ////////////////////////////////////////////////////////////////////////// reference
    // patterns.
    private class GraphicsPanel extends JPanel {
        private static Color getContrastingColor(final Color color) {
            /*
             * Usual and quick method is to XOR with 0xFFFFFF. Here's a better but slower
             * algorithm from www.codeproject.com/tips/JbColorContrast.asp :
             * If all 3 color components are "close" to 0x80 (midpoint - choose your
             * tolerance),
             * you can get better contrast by adding 0x7F7F7F then ANDing with 0xFFFFFF.
             */
            return new Color(color.getRGB() ^ 0xFFFFFF);
        }

        // override default paint method to assure visualized reference pattern is
        // produced every time
        // the panel is repainted.
        @Override
        public void paint(final Graphics g) {
            this.paintGrid(g, MemoryReferenceVisualization.this.theGrid);
            if (MemoryReferenceVisualization.this.drawHashMarksSelector.isSelected()) {
                this.paintHashMarks(g, MemoryReferenceVisualization.this.theGrid);
            }
        }

        // Paint (ash marks on the grid. Their color is chosef to be in
        // "contrast" to the current color for reference count of zero.
        private void paintHashMarks(final Graphics g, final Grid grid) {
            g.setColor(GraphicsPanel.getContrastingColor(MemoryReferenceVisualization.this.counterColorScale.getColor(0)));
            int leftX = 0;
            final int rightX = MemoryReferenceVisualization.this.visualizationAreaWidthInPixels;
            int upperY = 0;
            final int lowerY = MemoryReferenceVisualization.this.visualizationAreaHeightInPixels;
            // draw vertical hash marks
            for (int j = 0; j < grid.getColumns(); j++) {
                g.drawLine(leftX, upperY, leftX, lowerY);
                leftX += MemoryReferenceVisualization.this.unitPixelWidth; // faster than multiplying
            }
            leftX = 0;
            // draw horizontal hash marks
            for (int i = 0; i < grid.getRows(); i++) {
                g.drawLine(leftX, upperY, rightX, upperY);
                upperY += MemoryReferenceVisualization.this.unitPixelHeight; // faster than multiplying
            }
        }

        // Paint the color codes for reference counts.
        private void paintGrid(final Graphics g, final Grid grid) {
            int upperLeftX = 0, upperLeftY = 0;
            for (int i = 0; i < grid.getRows(); i++) {
                for (int j = 0; j < grid.getColumns(); j++) {
                    g.setColor(MemoryReferenceVisualization.this.counterColorScale.getColor(grid.getElementFast(i, j)));
                    g.fillRect(
                        upperLeftX, upperLeftY, MemoryReferenceVisualization.this.unitPixelWidth,
                        MemoryReferenceVisualization.this.unitPixelHeight
                    );
                    upperLeftX += MemoryReferenceVisualization.this.unitPixelWidth; // faster than multiplying
                }
                // get ready for next row...
                upperLeftX = 0;
                upperLeftY += MemoryReferenceVisualization.this.unitPixelHeight; // faster than multiplying
            }
        }
    }

    private final class ColorChooserControls {
        private final JButton currentColorButton;
        private final JPanel colorChooserRow;
        private final JPanel countDisplayRow;
        private final JLabel sliderLabel;
        private volatile int counterIndex;

        private ColorChooserControls() {
            final JSlider colorRangeSlider = new JSlider(
                JSlider.HORIZONTAL,
                0,
                MemoryReferenceVisualization.this.countTable.length - 1,
                MemoryReferenceVisualization.COUNT_INDEX_INIT
            );
            colorRangeSlider.setToolTipText("View or change color associated with each reference count value");
            colorRangeSlider.setPaintTicks(false);
            colorRangeSlider.addChangeListener(new ColorChooserListener());
            this.counterIndex = MemoryReferenceVisualization.COUNT_INDEX_INIT;
            this.sliderLabel =
                new JLabel(ColorChooserControls.setLabel(MemoryReferenceVisualization.this.countTable[this.counterIndex]));
            this.sliderLabel.setToolTipText("Reference count values listed on non-linear scale of " +
                MemoryReferenceVisualization.this.countTable[0] + " to " + MemoryReferenceVisualization.this.countTable[MemoryReferenceVisualization.this.countTable.length - 1]);
            this.sliderLabel.setHorizontalAlignment(JLabel.CENTER);
            this.sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            this.currentColorButton = new JButton("   ");
            this.currentColorButton.setToolTipText(
                "Click here to change color for the reference count subrange based at current value");
            this.currentColorButton.setBackground(MemoryReferenceVisualization.this.counterColorScale.getColor(
                MemoryReferenceVisualization.this.countTable[this.counterIndex]));
            this.currentColorButton.addActionListener(
                e -> {
                    final int counterValue =
                        MemoryReferenceVisualization.this.countTable[ColorChooserControls.this.counterIndex];
                    final int highEnd =
                        MemoryReferenceVisualization.this.counterColorScale.getHighEndOfRange(counterValue);
                    final String dialogLabel = "Select color for reference count " +
                        (
                            (counterValue == highEnd)
                                ? "value " + counterValue
                                : "range " + counterValue + "-" + highEnd
                        );
                    final Color newColor = JColorChooser.showDialog(
                        MemoryReferenceVisualization.this.theWindow,
                        dialogLabel,
                        MemoryReferenceVisualization.this.counterColorScale.getColor(counterValue)
                    );
                    if (newColor != null && !newColor.equals(MemoryReferenceVisualization.this.counterColorScale.getColor(
                        counterValue))) {
                        MemoryReferenceVisualization.this.counterColorScale.insertOrReplace(new CounterColor(
                            counterValue,
                            newColor
                        ));
                        ColorChooserControls.this.currentColorButton.setBackground(newColor);
                        MemoryReferenceVisualization.this.updateDisplay();
                    }
                });
            this.colorChooserRow = new JPanel();
            this.countDisplayRow = new JPanel();
            this.colorChooserRow.add(colorRangeSlider);
            this.colorChooserRow.add(this.currentColorButton);
            this.countDisplayRow.add(this.sliderLabel);
        }

        // set label wording depending on current speed setting
        private static String setLabel(final int value) {
            String spaces = "  ";
            if (value >= 100) {
                spaces = "";
            } else if (value >= 10) {
                spaces = " ";
            }
            return "Counter value " + spaces + value;
        }

        // Listener that both revises label as user slides and updates current index
        // when sliding stops.
        private class ColorChooserListener implements ChangeListener {
            @Override
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();
                if (!source.getValueIsAdjusting()) {
                    ColorChooserControls.this.counterIndex = source.getValue();
                } else {
                    final int count = MemoryReferenceVisualization.this.countTable[source.getValue()];
                    ColorChooserControls.this.sliderLabel.setText(ColorChooserControls.setLabel(count));
                    ColorChooserControls.this.currentColorButton.setBackground(MemoryReferenceVisualization.this.counterColorScale.getColor(
                        count));
                }
            }
        }
    }
}
