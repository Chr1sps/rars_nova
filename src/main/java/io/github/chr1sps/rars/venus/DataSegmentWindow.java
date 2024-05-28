package io.github.chr1sps.rars.venus;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.Settings;
import io.github.chr1sps.rars.exceptions.AddressErrorException;
import io.github.chr1sps.rars.notices.*;
import io.github.chr1sps.rars.riscv.hardware.Memory;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;
import io.github.chr1sps.rars.simulator.Simulator;
import io.github.chr1sps.rars.util.Binary;
import io.github.chr1sps.rars.util.SimpleSubscriber;
import io.github.chr1sps.rars.venus.run.RunSpeedPanel;
import io.github.chr1sps.rars.venus.util.RepeatButton;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;
import java.util.concurrent.Flow;

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
 * Represents the Data Segment window, which is a type of JInternalFrame.
 *
 * @author Sanderson and Bumgarner
 */
public class DataSegmentWindow extends JInternalFrame implements SimpleSubscriber<Notice> {

    private static final String[] dataSegmentNames = {"Data", "Stack", "Kernel"};
    private static Object[][] dataData;

    private static JTable dataTable;
    private JScrollPane dataTableScroller;
    private final Container contentPane;
    private final JPanel tablePanel;
    private JButton dataButton, nextButton, prevButton, stakButton, globButton, heapButton, extnButton, mmioButton,
            textButton;

    private static final int VALUES_PER_ROW = 8;
    private static final int NUMBER_OF_ROWS = 16; // with 8 value columns, this shows 512 bytes;
    private static final int NUMBER_OF_COLUMNS = DataSegmentWindow.VALUES_PER_ROW + 1;// 1 for address and 8 for values
    private static final int BYTES_PER_VALUE = 4;
    private static final int BYTES_PER_ROW = DataSegmentWindow.VALUES_PER_ROW * DataSegmentWindow.BYTES_PER_VALUE;
    private static final int MEMORY_CHUNK_SIZE = DataSegmentWindow.NUMBER_OF_ROWS * DataSegmentWindow.BYTES_PER_ROW;
    // PREV_NEXT_CHUNK_SIZE determines how many rows will be scrolled when Prev or
    // Next buttons fire.
    // MEMORY_CHUNK_SIZE/2 means scroll half a table up or down. Easier to view
    // series that flows off the edge.
    // MEMORY_CHUNK_SIZE means scroll a full table's worth. Scrolls through memory
    // faster. DPS 26-Jan-09
    private static final int PREV_NEXT_CHUNK_SIZE = DataSegmentWindow.MEMORY_CHUNK_SIZE / 2;
    private static final int ADDRESS_COLUMN = 0;
    private static final boolean USER_MODE = false;
    private static final boolean KERNEL_MODE = true;

    private boolean addressHighlighting = false;
    private boolean asciiDisplay = false;
    private int addressColumn;
    private int addressRowFirstAddress;
    private final Settings settings;

    private int firstAddress;
    private int homeAddress;
    private boolean userOrKernelMode;

    // The combo box replaced the row of buttons when number of buttons expanded to
    // 7!
    // We'll keep the button objects however and manually invoke their action
    // listeners
    // when the corresponding combo box item is selected. DPS 22-Nov-2006
    private final JComboBox<String> baseAddressSelector;

    // The next bunch are initialized dynamically in initializeBaseAddressChoices()
    private String[] displayBaseAddressChoices;
    private int[] displayBaseAddresses;
    private int defaultBaseAddressIndex;
    private JButton[] baseAddressButtons;

    /**
     * Constructor for the Data Segment window.
     *
     * @param choosers an array of objects used by user to select number display
     *                 base (10 or 16)
     */
    public DataSegmentWindow(final NumberDisplayBaseChooser[] choosers) {
        super("Data Segment", true, false, true, true);

        Simulator.getInstance().subscribe(this);
        this.settings = Globals.getSettings();
        this.settings.subscribe(this);

        this.homeAddress = Memory.dataBaseAddress; // address for Home button
        this.firstAddress = this.homeAddress; // first address to display at any given time
        this.userOrKernelMode = DataSegmentWindow.USER_MODE;
        this.addressHighlighting = false;
        this.contentPane = this.getContentPane();
        this.tablePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        final JPanel features = new JPanel();
        final Toolkit tk = Toolkit.getDefaultToolkit();
        final Class<? extends DataSegmentWindow> cs = this.getClass();
        try {
            this.prevButton = new PrevButton(
                    new ImageIcon(tk.getImage(cs.getResource(Globals.imagesPath + "Previous22.png"))));// "Back16.gif"))));//"Down16.gif"))));
            this.nextButton = new NextButton(new ImageIcon(tk.getImage(cs.getResource(Globals.imagesPath + "Next22.png"))));// "Forward16.gif"))));
            // //"Up16.gif"))));
            // This group of buttons was replaced by a combo box. Keep the JButton objects
            // for their action listeners.
            this.dataButton = new JButton();// ".data");
            this.stakButton = new JButton();// "$sp");
            this.globButton = new JButton();// "$gp");
            this.heapButton = new JButton();// "heap");
            this.extnButton = new JButton();// ".extern");
            this.mmioButton = new JButton();// "MMIO");
            this.textButton = new JButton();// ".text");
        } catch (final NullPointerException e) {
            System.out.println("Internal Error: images folder not found");
            System.exit(0);
        }

        this.initializeBaseAddressChoices();
        this.baseAddressSelector = new JComboBox<>();
        this.baseAddressSelector.setModel(new CustomComboBoxModel(this.displayBaseAddressChoices));
        this.baseAddressSelector.setEditable(false);
        this.baseAddressSelector.setSelectedIndex(this.defaultBaseAddressIndex);
        this.baseAddressSelector.setToolTipText("Base address for data segment display");
        this.baseAddressSelector.addActionListener(
                e -> {
                    // trigger action listener for associated invisible button.
                    DataSegmentWindow.this.baseAddressButtons[DataSegmentWindow.this.baseAddressSelector.getSelectedIndex()].getActionListeners()[0]
                            .actionPerformed(null);
                });

        this.addButtonActionListenersAndInitialize();
        final JPanel navButtons = new JPanel(new GridLayout(1, 4));
        navButtons.add(this.prevButton);
        navButtons.add(this.nextButton);
        features.add(navButtons);
        features.add(this.baseAddressSelector);
        for (final NumberDisplayBaseChooser chooser : choosers) {
            features.add(chooser);
        }
        final JCheckBox asciiDisplayCheckBox = new JCheckBox("ASCII", this.asciiDisplay);
        asciiDisplayCheckBox
                .setToolTipText("Display data segment values in ASCII (overrides Hexadecimal Values setting)");
        asciiDisplayCheckBox.addItemListener(
                e -> {
                    DataSegmentWindow.this.asciiDisplay = (e.getStateChange() == ItemEvent.SELECTED);
                    DataSegmentWindow.this.updateValues();
                });
        features.add(asciiDisplayCheckBox);

        this.contentPane.add(features, BorderLayout.SOUTH);
    }

    /**
     * <p>updateBaseAddressComboBox.</p>
     */
    public void updateBaseAddressComboBox() {
        this.displayBaseAddressArray[DataSegmentWindow.EXTERN_BASE_ADDRESS_INDEX] = Memory.externBaseAddress;
        this.displayBaseAddressArray[DataSegmentWindow.GLOBAL_POINTER_ADDRESS_INDEX] = -1; /* Memory.globalPointer */
        this.displayBaseAddressArray[DataSegmentWindow.DATA_BASE_ADDRESS_INDEX] = Memory.dataBaseAddress;
        this.displayBaseAddressArray[DataSegmentWindow.HEAP_BASE_ADDRESS_INDEX] = Memory.heapBaseAddress;
        this.displayBaseAddressArray[DataSegmentWindow.STACK_POINTER_BASE_ADDRESS_INDEX] = -1; /* Memory.stackPointer */
        this.displayBaseAddressArray[DataSegmentWindow.MMIO_BASE_ADDRESS_INDEX] = Memory.memoryMapBaseAddress;
        this.displayBaseAddressArray[DataSegmentWindow.TEXT_BASE_ADDRESS_INDEX] = Memory.textBaseAddress;
        this.displayBaseAddressChoices = this.createBaseAddressLabelsArray(this.displayBaseAddressArray, this.descriptions);
        this.baseAddressSelector.setModel(new CustomComboBoxModel(this.displayBaseAddressChoices));
        this.displayBaseAddresses = this.displayBaseAddressArray;
        this.baseAddressSelector.setSelectedIndex(this.defaultBaseAddressIndex);
    }

    /**
     * Scroll the viewport so the cell at the given data segment address
     * is visible, vertically centered if possible, and selected.
     * Developed July 2007 for new feature that shows source code step where
     * label is defined when that label is clicked on in the Label Window.
     * Note there is a separate method to highlight the cell by setting
     * its background color to a highlighting color. Thus one cell can be
     * highlighted while a different cell is selected at the same time.
     *
     * @param address data segment address of word to be selected.
     */
    void selectCellForAddress(final int address) {
        final Point rowColumn = this.displayCellForAddress(address);
        if (rowColumn == null) {
            return;
        }
        final Rectangle addressCell = DataSegmentWindow.dataTable.getCellRect(rowColumn.x, rowColumn.y, true);
        // Select the memory address cell by generating a fake Mouse Pressed event
        // within its
        // extent and explicitly invoking the table's mouse listener.
        final MouseEvent fakeMouseEvent = new MouseEvent(DataSegmentWindow.dataTable, MouseEvent.MOUSE_PRESSED,
                new Date().getTime(), MouseEvent.BUTTON1_DOWN_MASK,
                (int) addressCell.getX() + 1,
                (int) addressCell.getY() + 1, 1, false);
        final MouseListener[] mouseListeners = DataSegmentWindow.dataTable.getMouseListeners();
        for (final MouseListener mouseListener : mouseListeners) {
            mouseListener.mousePressed(fakeMouseEvent);
        }
    }

    /**
     * Scroll the viewport so the cell at the given data segment address
     * is visible, vertically centered if possible, and highlighted (but not
     * selected).
     *
     * @param address data segment address of word to be selected.
     */
    public void highlightCellForAddress(final int address) {
        final Point rowColumn = this.displayCellForAddress(address);
        if (rowColumn == null || rowColumn.x < 0 || rowColumn.y < 0) {
            return;
        }
        final int addressRow = rowColumn.x;
        this.addressColumn = rowColumn.y;
        this.addressRowFirstAddress = Binary
                .stringToInt(DataSegmentWindow.dataTable.getValueAt(addressRow, DataSegmentWindow.ADDRESS_COLUMN).toString());
        // System.out.println("Address "+Binary.intToHexString(address)+" becomes row "+
        // addressRow + " column "+addressColumn+
        // " starting addr "+dataTable.getValueAt(this.addressRow,ADDRESS_COLUMN));
        // Tell the system that table contents have changed. This will trigger
        // re-rendering
        // during which cell renderers are obtained. The cell of interest (identified by
        // instance variables this.addressRow and this.addressColumn) will get a
        // renderer
        // with highlight background color and all others get renderer with default
        // background.
        DataSegmentWindow.dataTable.tableChanged(new TableModelEvent(DataSegmentWindow.dataTable.getModel(), 0, DataSegmentWindow.dataData.length - 1));
    }

    // Given address, will compute table cell location, adjusting table if necessary
    // to
    // contain this cell, make sure that cell is visible, then return a Point
    // containing
    // row and column position of cell in the table. This private helper method is
    // called
    // by selectCellForAddress() and highlightCellForAddress().
    // This is the kind of design I tell my students to avoid! The method both
    // translates
    // address to table cell coordinates and adjusts the display to assure the cell
    // is visible.
    // The two operations are related because the address may fall in within address
    // space not
    // currently in the (display) table, including a different MIPS data segment
    // (e.g. in
    // kernel instead of user data segment).
    private Point displayCellForAddress(final int address) {
        //////////////////////////////////////////////////////////
        // This requires a 5-step process. Each step is described
        // just above the statements that implement it.
        //////////////////////////////////////////////////////////

        // STEP 1: Determine which data segment contains this address.
        final int desiredComboBoxIndex = this.getBaseAddressIndexForAddress(address);
        if (desiredComboBoxIndex < 0) {
            // It is not a data segment address so good bye!
            return null;
        }
        // STEP 2: Set the combo box appropriately. This will also display the
        // first chunk of addresses from that segment.
        this.baseAddressSelector.setSelectedIndex(desiredComboBoxIndex);
        ((CustomComboBoxModel) this.baseAddressSelector.getModel()).forceComboBoxUpdate(desiredComboBoxIndex);
        this.baseAddressButtons[desiredComboBoxIndex].getActionListeners()[0].actionPerformed(null);
        // STEP 3: Display memory chunk containing this address, which may be
        // different than the one just displayed.
        int baseAddress = this.displayBaseAddressArray[desiredComboBoxIndex];
        if (baseAddress == -1) {
            if (desiredComboBoxIndex == DataSegmentWindow.GLOBAL_POINTER_ADDRESS_INDEX) {
                baseAddress = RegisterFile.getValue(RegisterFile.GLOBAL_POINTER_REGISTER)
                        - (RegisterFile.getValue(RegisterFile.GLOBAL_POINTER_REGISTER) % DataSegmentWindow.BYTES_PER_ROW);
            } else if (desiredComboBoxIndex == DataSegmentWindow.STACK_POINTER_BASE_ADDRESS_INDEX) {
                baseAddress = RegisterFile.getValue(RegisterFile.STACK_POINTER_REGISTER)
                        - (RegisterFile.getValue(RegisterFile.STACK_POINTER_REGISTER) % DataSegmentWindow.BYTES_PER_ROW);
            } else {
                return null;// shouldn't happen since these are the only two
            }
        }
        final int byteOffset = address - baseAddress;
        final int chunkOffset = byteOffset / DataSegmentWindow.MEMORY_CHUNK_SIZE;
        final int byteOffsetIntoChunk = byteOffset % DataSegmentWindow.MEMORY_CHUNK_SIZE;
        // Subtract 1 from chunkOffset because we're gonna call the "next" action
        // listener to get the correct chunk loaded and displayed, and the first
        // thing it does is increment firstAddress by MEMORY_CHUNK_SIZE. Here
        // we do an offsetting decrement in advance because we don't want the
        // increment but we want the other actions that method provides.
        this.firstAddress = this.firstAddress + chunkOffset * DataSegmentWindow.MEMORY_CHUNK_SIZE - DataSegmentWindow.PREV_NEXT_CHUNK_SIZE;
        this.nextButton.getActionListeners()[0].actionPerformed(null);
        // STEP 4: Find cell containing this address. Add 1 to column calculation
        // because table column 0 displays address, not memory contents. The
        // "convertColumnIndexToView()" is not necessary because the columns cannot be
        // reordered, but I included it as a precautionary measure in case that changes.
        final int addrRow = byteOffsetIntoChunk / DataSegmentWindow.BYTES_PER_ROW;
        int addrColumn = byteOffsetIntoChunk % DataSegmentWindow.BYTES_PER_ROW / DataSegmentWindow.BYTES_PER_VALUE + 1;
        addrColumn = DataSegmentWindow.dataTable.convertColumnIndexToView(addrColumn);
        final Rectangle addressCell = DataSegmentWindow.dataTable.getCellRect(addrRow, addrColumn, true);
        // STEP 5: Center the row containing the cell of interest, to the extent
        // possible.
        final double cellHeight = addressCell.getHeight();
        final double viewHeight = this.dataTableScroller.getViewport().getExtentSize().getHeight();
        final int numberOfVisibleRows = (int) (viewHeight / cellHeight);
        final int newViewPositionY = Math.max((int) ((addrRow - ((double) numberOfVisibleRows / 2)) * cellHeight), 0);
        this.dataTableScroller.getViewport().setViewPosition(new Point(0, newViewPositionY));
        return new Point(addrRow, addrColumn);
    }

    ////////////////////////////////////////////////////////////////////////
    // Initalize arrays used with Base Address combo box chooser.
    // The combo box replaced the row of buttons when number of buttons expanded to
    //////////////////////////////////////////////////////////////////////// 7!
    private static final int EXTERN_BASE_ADDRESS_INDEX = 0;
    private static final int GLOBAL_POINTER_ADDRESS_INDEX = 3; // 1;
    private static final int TEXT_BASE_ADDRESS_INDEX = 5; // 2;
    private static final int DATA_BASE_ADDRESS_INDEX = 1; // 3;
    private static final int HEAP_BASE_ADDRESS_INDEX = 2; // 4;
    private static final int STACK_POINTER_BASE_ADDRESS_INDEX = 4; // 5;
    private static final int MMIO_BASE_ADDRESS_INDEX = 6;
    // Must agree with above in number and order...
    private final int[] displayBaseAddressArray = {Memory.externBaseAddress,
            Memory.dataBaseAddress, Memory.heapBaseAddress, -1 /* Memory.globalPointer */,
            -1 /* Memory.stackPointer */, Memory.textBaseAddress,
            Memory.memoryMapBaseAddress,};
    // Must agree with above in number and order...
    final String[] descriptions = {" (.extern)", " (.data)", " (heap)", "current gp",
            "current sp", " (.text)", " (MMIO)"};

    private void initializeBaseAddressChoices() {
        // Also must agree in number and order. Upon combo box item selection, will
        // invoke
        // action listener for that item's button.
        this.baseAddressButtons = new JButton[this.descriptions.length];
        this.baseAddressButtons[DataSegmentWindow.EXTERN_BASE_ADDRESS_INDEX] = this.extnButton;
        this.baseAddressButtons[DataSegmentWindow.GLOBAL_POINTER_ADDRESS_INDEX] = this.globButton;
        this.baseAddressButtons[DataSegmentWindow.DATA_BASE_ADDRESS_INDEX] = this.dataButton;
        this.baseAddressButtons[DataSegmentWindow.HEAP_BASE_ADDRESS_INDEX] = this.heapButton;
        this.baseAddressButtons[DataSegmentWindow.STACK_POINTER_BASE_ADDRESS_INDEX] = this.stakButton;
        this.baseAddressButtons[DataSegmentWindow.MMIO_BASE_ADDRESS_INDEX] = this.mmioButton;
        this.baseAddressButtons[DataSegmentWindow.TEXT_BASE_ADDRESS_INDEX] = this.textButton;
        this.displayBaseAddresses = this.displayBaseAddressArray;
        this.displayBaseAddressChoices = this.createBaseAddressLabelsArray(this.displayBaseAddressArray, this.descriptions);
        this.defaultBaseAddressIndex = DataSegmentWindow.DATA_BASE_ADDRESS_INDEX;
    }

    // Create and fill String array containing labels for base address combo box.
    private String[] createBaseAddressLabelsArray(final int[] baseAddressArray, final String[] descriptions) {
        final String[] baseAddressChoices = new String[baseAddressArray.length];
        for (int i = 0; i < baseAddressChoices.length; i++) {
            baseAddressChoices[i] = ((baseAddressArray[i] != -1)
                    ? io.github.chr1sps.rars.util.Binary.intToHexString(baseAddressArray[i])
                    : "")
                    + descriptions[i];
        }
        return baseAddressChoices;
    }

    // Given an address, determine which segment it is in and return the
    // corresponding
    // combo box index. Note there is not a one-to-one correspondence between these
    // indexes and the Memory tables. For instance, the heap (0x10040000), the
    // global (0x10008000) and the data segment base (0x10000000) are all stored in
    // the
    // same table as the static (0x10010000) so all are "Memory.inDataSegment()".
    private int getBaseAddressIndexForAddress(final int address) {
        int desiredComboBoxIndex = -1; // assume not a data address.
        if (Memory.inMemoryMapSegment(address)) {
            return DataSegmentWindow.MMIO_BASE_ADDRESS_INDEX;
        } else if (Memory.inTextSegment(address)) { // DPS. 8-July-2013
            return DataSegmentWindow.TEXT_BASE_ADDRESS_INDEX;
        }
        int shortDistance = 0x7fffffff;
        int thisDistance;
        // Check distance from .extern base. Cannot be below it
        thisDistance = address - Memory.externBaseAddress;
        if (thisDistance >= 0 && thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = DataSegmentWindow.EXTERN_BASE_ADDRESS_INDEX;
        }
        // Check distance from global pointer; can be either side of it...
        thisDistance = Math.abs(address - RegisterFile.getValue(RegisterFile.GLOBAL_POINTER_REGISTER)); // distance from
        // global
        // pointer
        if (thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = DataSegmentWindow.GLOBAL_POINTER_ADDRESS_INDEX;
        }
        // Check distance from .data base. Cannot be below it
        thisDistance = address - Memory.dataBaseAddress;
        if (thisDistance >= 0 && thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = DataSegmentWindow.DATA_BASE_ADDRESS_INDEX;
        }
        // Check distance from heap base. Cannot be below it
        thisDistance = address - Memory.heapBaseAddress;
        if (thisDistance >= 0 && thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = DataSegmentWindow.HEAP_BASE_ADDRESS_INDEX;
        }
        // Check distance from stack pointer. Can be on either side of it...
        thisDistance = Math.abs(address - RegisterFile.getValue(RegisterFile.STACK_POINTER_REGISTER));
        if (thisDistance < shortDistance) {
            desiredComboBoxIndex = DataSegmentWindow.STACK_POINTER_BASE_ADDRESS_INDEX;
        }
        return desiredComboBoxIndex;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Generates the Address/Data part of the Data Segment window.
    // Returns the JScrollPane for the Address/Data part of the Data Segment window.
    private JScrollPane generateDataPanel() {
        DataSegmentWindow.dataData = new Object[DataSegmentWindow.NUMBER_OF_ROWS][DataSegmentWindow.NUMBER_OF_COLUMNS];
        final int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
        final int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
        int address = this.homeAddress;
        for (int row = 0; row < DataSegmentWindow.NUMBER_OF_ROWS; row++) {
            DataSegmentWindow.dataData[row][DataSegmentWindow.ADDRESS_COLUMN] = NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase);
            for (int column = 1; column < DataSegmentWindow.NUMBER_OF_COLUMNS; column++) {
                try {
                    DataSegmentWindow.dataData[row][column] = NumberDisplayBaseChooser.formatNumber(Globals.memory.getRawWord(address),
                            valueBase);
                } catch (final AddressErrorException aee) {
                    DataSegmentWindow.dataData[row][column] = NumberDisplayBaseChooser.formatNumber(0, valueBase);
                }
                address += DataSegmentWindow.BYTES_PER_VALUE;
            }
        }
        final String[] names = new String[DataSegmentWindow.NUMBER_OF_COLUMNS];
        for (int i = 0; i < DataSegmentWindow.NUMBER_OF_COLUMNS; i++) {
            names[i] = this.getHeaderStringForColumn(i, addressBase);
        }
        DataSegmentWindow.dataTable = new MyTippedJTable(new DataTableModel(DataSegmentWindow.dataData, names));
        this.updateRowHeight();
        // Do not allow user to re-order columns; column order corresponds to MIPS
        // memory order
        DataSegmentWindow.dataTable.getTableHeader().setReorderingAllowed(false);
        DataSegmentWindow.dataTable.setRowSelectionAllowed(false);
        // Addresses are column 0, render right-justified in mono font
        final MonoRightCellRenderer monoRightCellRenderer = new MonoRightCellRenderer();
        DataSegmentWindow.dataTable.getColumnModel().getColumn(DataSegmentWindow.ADDRESS_COLUMN).setPreferredWidth(60);
        DataSegmentWindow.dataTable.getColumnModel().getColumn(DataSegmentWindow.ADDRESS_COLUMN).setCellRenderer(monoRightCellRenderer);
        // Data cells are columns 1 onward, render right-justitifed in mono font but
        // highlightable.
        final AddressCellRenderer addressCellRenderer = new AddressCellRenderer();
        for (int i = 1; i < DataSegmentWindow.NUMBER_OF_COLUMNS; i++) {
            DataSegmentWindow.dataTable.getColumnModel().getColumn(i).setPreferredWidth(60);
            DataSegmentWindow.dataTable.getColumnModel().getColumn(i).setCellRenderer(addressCellRenderer);
        }
        this.dataTableScroller = new JScrollPane(DataSegmentWindow.dataTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        return this.dataTableScroller;
    }

    // Little helper. Is called when headers set up and each time number base
    // changes.
    private String getHeaderStringForColumn(final int i, final int base) {
        return (i == DataSegmentWindow.ADDRESS_COLUMN) ? "Address" : "Value (+" + Integer.toString((i - 1) * DataSegmentWindow.BYTES_PER_VALUE, base) + ")";
    }

    /**
     * Generates and displays fresh table, typically done upon successful assembly.
     */
    public void setupTable() {
        this.tablePanel.removeAll();
        this.tablePanel.add(this.generateDataPanel());
        this.contentPane.add(this.tablePanel);
        this.enableAllButtons();
    }

    /**
     * Removes the table from its frame, typically done when a file is closed.
     */
    public void clearWindow() {
        this.tablePanel.removeAll();
        this.disableAllButtons();
    }

    /**
     * Clear highlight background color from any cell currently highlighted.
     */
    public void clearHighlighting() {
        this.addressHighlighting = false;
        DataSegmentWindow.dataTable.tableChanged(new TableModelEvent(DataSegmentWindow.dataTable.getModel(), 0, DataSegmentWindow.dataData.length - 1));
        // The below addresses situation in which addressRow and addressColum hold their
        // values across assemble operations. Whereupon at the first step of the next
        // run the last cells from the previous run are highlighted! This method is
        // called
        // after each successful assemble (or reset, which just re-assembles). The
        // assignment below assures the highlighting condition column==addressColumn
        // will be
        // initially false since column>=0. DPS 23 jan 2009
        this.addressColumn = -1;
    }

    private int getValueDisplayFormat() {
        return (this.asciiDisplay) ? NumberDisplayBaseChooser.ASCII
                : Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
    }

    /**
     * Update table model with contents of new memory "chunk". Mars supports
     * megabytes of
     * data segment space so we only plug a "chunk" at a time into the table.
     *
     * @param firstAddr the first address in the memory range to be placed in the
     *                  model.
     */
    public void updateModelForMemoryRange(final int firstAddr) {
        if (this.tablePanel.getComponentCount() == 0)
            return; // ignore if no content to change
        final int valueBase = this.getValueDisplayFormat();
        final int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
        int address = firstAddr;
        final TableModel dataModel = DataSegmentWindow.dataTable.getModel();
        for (int row = 0; row < DataSegmentWindow.NUMBER_OF_ROWS; row++) {
            ((DataTableModel) dataModel).setDisplayAndModelValueAt(
                    NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase), row, DataSegmentWindow.ADDRESS_COLUMN);
            for (int column = 1; column < DataSegmentWindow.NUMBER_OF_COLUMNS; column++) {
                try {
                    ((DataTableModel) dataModel).setDisplayAndModelValueAt(
                            NumberDisplayBaseChooser.formatNumber(Globals.memory.getWordNoNotify(address), valueBase),
                            row, column);
                } catch (final AddressErrorException aee) {
                    // Bit of a hack here. Memory will throw an exception if you try to read
                    // directly from text segment when the
                    // self-modifying code setting is disabled. This is a good thing if it is the
                    // executing MIPS program trying to
                    // read. But not a good thing if it is the DataSegmentDisplay trying to read.
                    // I'll trick Memory by
                    // temporarily enabling the setting as "non persistent" so it won't write
                    // through to the registry.
                    if (Memory.inTextSegment(address)) {
                        int displayValue = 0;
                        if (!Globals.getSettings().getBooleanSetting(Settings.Bool.SELF_MODIFYING_CODE_ENABLED)) {
                            Globals.getSettings()
                                    .setBooleanSettingNonPersistent(Settings.Bool.SELF_MODIFYING_CODE_ENABLED, true);
                            try {
                                displayValue = Globals.memory.getWordNoNotify(address);
                            } catch (final AddressErrorException e) {
                                // Still got an exception? Doesn't seem possible but if we drop through it will
                                // write default value 0.
                            }
                            Globals.getSettings()
                                    .setBooleanSettingNonPersistent(Settings.Bool.SELF_MODIFYING_CODE_ENABLED, false);
                        }
                        ((DataTableModel) dataModel).setDisplayAndModelValueAt(
                                NumberDisplayBaseChooser.formatNumber(displayValue, valueBase), row, column);
                    }
                    // Bug Fix: the following line of code disappeared during the release 4.4 mods,
                    // but is essential to
                    // display values of 0 for valid MIPS addresses that are outside the MARS
                    // simulated address space. Such
                    // addresses cause an AddressErrorException. Prior to 4.4, they performed this
                    // line of code unconditionally.
                    // With 4.4, I added the above IF statement to work with the text segment but
                    // inadvertently removed this line!
                    // Now it becomes the "else" part, executed when not in text segment. DPS
                    // 8-July-2014.
                    else {
                        ((DataTableModel) dataModel).setDisplayAndModelValueAt(
                                NumberDisplayBaseChooser.formatNumber(0, valueBase), row, column);
                    }
                }
                address += DataSegmentWindow.BYTES_PER_VALUE;
            }
        }
    }

    /**
     * Update data display to show this value (I'm not sure it is being called).
     *
     * @param address a int
     * @param value   a int
     */
    public void updateCell(final int address, final int value) {
        final int offset = address - this.firstAddress;
        if (offset < 0 || offset >= DataSegmentWindow.MEMORY_CHUNK_SIZE) { // out of range
            return;
        }
        final int row = offset / DataSegmentWindow.BYTES_PER_ROW;
        final int column = (offset % DataSegmentWindow.BYTES_PER_ROW) / DataSegmentWindow.BYTES_PER_VALUE + 1; // column 0 reserved for address
        final int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
        ((DataTableModel) DataSegmentWindow.dataTable.getModel()).setDisplayAndModelValueAt(
                NumberDisplayBaseChooser.formatNumber(value, valueBase),
                row, column);
    }

    /**
     * Redisplay the addresses. This should only be done when address display base
     * is
     * modified (e.g. between base 16, hex, and base 10, dec).
     */
    public void updateDataAddresses() {
        if (this.tablePanel.getComponentCount() == 0)
            return; // ignore if no content to change
        final int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
        int address = this.firstAddress;
        String formattedAddress;
        for (int i = 0; i < DataSegmentWindow.NUMBER_OF_ROWS; i++) {
            formattedAddress = NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase);
            ((DataTableModel) DataSegmentWindow.dataTable.getModel()).setDisplayAndModelValueAt(formattedAddress, i, 0);
            address += DataSegmentWindow.BYTES_PER_ROW;
        }
        // column headers include address offsets, so translate them too
        for (int i = 1; i < DataSegmentWindow.NUMBER_OF_COLUMNS; i++) {
            DataSegmentWindow.dataTable.getColumnModel().getColumn(i).setHeaderValue(this.getHeaderStringForColumn(i, addressBase));
        }
        DataSegmentWindow.dataTable.getTableHeader().repaint();
    }

    /**
     * Update data display to show all values
     */
    public void updateValues() {
        this.updateModelForMemoryRange(this.firstAddress);
    }

    /**
     * Reset range of memory addresses to base address of currently selected segment
     * and update display.
     */
    public void resetMemoryRange() {
        this.baseAddressSelector.getActionListeners()[0].actionPerformed(null); // previously dataButton
    }

    /**
     * Reset all data display values to 0
     */
    public void resetValues() {
        final int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
        final TableModel dataModel = DataSegmentWindow.dataTable.getModel();
        for (int row = 0; row < DataSegmentWindow.NUMBER_OF_ROWS; row++) {
            for (int column = 1; column < DataSegmentWindow.NUMBER_OF_COLUMNS; column++) {
                ((DataTableModel) dataModel)
                        .setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(0, valueBase), row, column);
            }
        }
        this.disableAllButtons();
    }

    /*
     * Do this initially and upon reset.
     */
    private void disableAllButtons() {
        this.baseAddressSelector.setEnabled(false);
        this.globButton.setEnabled(false);
        this.stakButton.setEnabled(false);
        this.heapButton.setEnabled(false);
        this.extnButton.setEnabled(false);
        this.mmioButton.setEnabled(false);
        this.textButton.setEnabled(false);
        this.prevButton.setEnabled(false);
        this.nextButton.setEnabled(false);
        this.dataButton.setEnabled(false);
    }

    /*
     * Do this upon reset.
     */
    private void enableAllButtons() {
        this.baseAddressSelector.setEnabled(true);
        this.globButton.setEnabled(true);
        this.stakButton.setEnabled(true);
        this.heapButton.setEnabled(true);
        this.extnButton.setEnabled(true);
        this.mmioButton.setEnabled(true);
        this.textButton.setEnabled(this.settings.getBooleanSetting(Settings.Bool.SELF_MODIFYING_CODE_ENABLED));
        this.prevButton.setEnabled(true);
        this.nextButton.setEnabled(true);
        this.dataButton.setEnabled(true);
    }

    /*
     * Establish action listeners for the data segment navigation buttons.
     */

    private void addButtonActionListenersAndInitialize() {
        // set initial states
        this.disableAllButtons();
        // add tool tips
        // NOTE: For buttons that are now combo box items, the tool tips are not
        // displayed w/o custom renderer.
        this.globButton.setToolTipText("View range around global pointer");
        this.stakButton.setToolTipText("View range around stack pointer");
        this.heapButton.setToolTipText("View range around heap base address " +
                Binary.intToHexString(Memory.heapBaseAddress));
        this.extnButton.setToolTipText("View range around static global base address " +
                Binary.intToHexString(Memory.externBaseAddress));
        this.mmioButton.setToolTipText("View range around MMIO base address " +
                Binary.intToHexString(Memory.memoryMapBaseAddress));
        this.textButton.setToolTipText("View range around program code " +
                Binary.intToHexString(Memory.textBaseAddress));
        this.prevButton.setToolTipText("View next lower address range; hold down for rapid fire");
        this.nextButton.setToolTipText("View next higher address range; hold down for rapid fire");
        this.dataButton.setToolTipText("View range around static data segment base address " +
                Binary.intToHexString(Memory.dataBaseAddress));

        // add the action listeners to maintain button state and table contents
        // Currently there is no memory upper bound so next button always enabled.

        this.globButton.addActionListener(
                ae -> {
                    DataSegmentWindow.this.userOrKernelMode = DataSegmentWindow.USER_MODE;
                    // get $gp global pointer, but guard against it having value below data segment
                    DataSegmentWindow.this.firstAddress = Math.max(Memory.dataSegmentBaseAddress,
                            RegisterFile.getValue(RegisterFile.GLOBAL_POINTER_REGISTER));
                    // updateModelForMemoryRange requires argument to be multiple of 4
                    // but for cleaner display we'll make it multiple of 32 (last nibble is 0).
                    // This makes it easier to mentally calculate address from row address + column
                    // offset.
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.firstAddress - (DataSegmentWindow.this.firstAddress % DataSegmentWindow.BYTES_PER_ROW);
                    DataSegmentWindow.this.homeAddress = DataSegmentWindow.this.firstAddress;
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
                    DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
                });

        this.stakButton.addActionListener(
                ae -> {
                    DataSegmentWindow.this.userOrKernelMode = DataSegmentWindow.USER_MODE;
                    // get $sp stack pointer, but guard against it having value below data segment
                    DataSegmentWindow.this.firstAddress = Math.max(Memory.dataSegmentBaseAddress,
                            RegisterFile.getValue(RegisterFile.STACK_POINTER_REGISTER));
                    // See comment above for gloButton...
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.firstAddress - (DataSegmentWindow.this.firstAddress % DataSegmentWindow.BYTES_PER_ROW);
                    DataSegmentWindow.this.homeAddress = Memory.stackBaseAddress;
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
                    DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
                });

        this.heapButton.addActionListener(
                ae -> {
                    DataSegmentWindow.this.userOrKernelMode = DataSegmentWindow.USER_MODE;
                    DataSegmentWindow.this.homeAddress = Memory.heapBaseAddress;
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.homeAddress);
                    DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
                });

        this.extnButton.addActionListener(
                ae -> {
                    DataSegmentWindow.this.userOrKernelMode = DataSegmentWindow.USER_MODE;
                    DataSegmentWindow.this.homeAddress = Memory.externBaseAddress;
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.homeAddress);
                    DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
                });

        this.mmioButton.addActionListener(
                ae -> {
                    DataSegmentWindow.this.userOrKernelMode = DataSegmentWindow.KERNEL_MODE;
                    DataSegmentWindow.this.homeAddress = Memory.memoryMapBaseAddress;
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.homeAddress;
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
                    DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
                });

        this.textButton.addActionListener(
                ae -> {
                    DataSegmentWindow.this.userOrKernelMode = DataSegmentWindow.USER_MODE;
                    DataSegmentWindow.this.homeAddress = Memory.textBaseAddress;
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.homeAddress;
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
                    DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
                });

        this.dataButton.addActionListener(
                ae -> {
                    DataSegmentWindow.this.userOrKernelMode = DataSegmentWindow.USER_MODE;
                    DataSegmentWindow.this.homeAddress = Memory.dataBaseAddress;
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.homeAddress;
                    DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
                    DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
                });

        // NOTE: action listeners for prevButton and nextButton are now in their
        // specialized inner classes at the bottom of this listing. DPS 20 July 2008

    }

    ////////////////////////////////////////////////////////////////////////////////////
    // This will assure that user cannot view memory locations outside the data
    //////////////////////////////////////////////////////////////////////////////////// segment
    // for selected mode. For user mode, this means no lower than data segment base,
    // or higher than user memory boundary. For kernel mode, this means no lower
    //////////////////////////////////////////////////////////////////////////////////// than
    // kernel data segment base or higher than kernel memory. It is called by the
    // above action listeners.
    //
    // lowAddress is lowest desired address to view, it is adjusted if necessary
    // and returned.
    //
    // PrevButton and NextButton are enabled/disabled appropriately.
    //
    private int setFirstAddressAndPrevNextButtonEnableStatus(int lowAddress) {
        final int lowLimit = (this.userOrKernelMode == DataSegmentWindow.USER_MODE) ? Math.min(Math.min(Memory.textBaseAddress,
                        Memory.dataSegmentBaseAddress),
                Memory.dataBaseAddress)
                : Memory.memoryMapBaseAddress;
        final int highLimit = (this.userOrKernelMode == DataSegmentWindow.USER_MODE) ? Memory.userHighAddress
                : Memory.kernelHighAddress;
        if (lowAddress <= lowLimit) {
            lowAddress = lowLimit;
            this.prevButton.setEnabled(false);
        } else {
            this.prevButton.setEnabled(true);
        }
        if (lowAddress >= highLimit - DataSegmentWindow.MEMORY_CHUNK_SIZE) {
            lowAddress = highLimit - DataSegmentWindow.MEMORY_CHUNK_SIZE + 1;
            this.nextButton.setEnabled(false);
        } else {
            this.nextButton.setEnabled(true);
        }
        return lowAddress;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Required by Observer interface. Called when notified by an Observable that we
     * are registered with.
     * Observables include:
     * The Simulator object, which lets us know when it starts and stops running
     * A delegate of the Memory object, which lets us know of memory operations
     * The Simulator keeps us informed of when simulated MIPS execution is active.
     * This is the only time we care about memory operations.
     */

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(final Flow.Subscription subscription) {
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(final Notice notice) {
        switch (notice) {
            case final SimulatorNotice s -> {
                if (s.getAction() == SimulatorNotice.SIMULATOR_START) {
                    // Simulated MIPS execution starts. Respond to memory changes if running in
                    // timed
                    // or stepped mode.
                    if (s.getRunSpeed() != RunSpeedPanel.UNLIMITED_SPEED || s.getMaxSteps() == 1) {
                        Memory.getInstance().subscribe(this);
                        this.addressHighlighting = true;
                    }
                } else {
                    // Simulated MIPS execution stops. Stop responding.
                    Memory.getInstance().deleteSubscriber(this);
                }
            }
            case final SettingsNotice ignored -> this.updateRowHeight();
            case final MemoryAccessNotice m -> {
                // NOTE: each register is a separate Observable
                if (m.getAccessType() == AccessNotice.WRITE) {
                    // Uses the same highlighting technique as for Text Segment -- see
                    // AddressCellRenderer class in DataSegmentWindow.java.
                    final var address = m.getAddress();
                    this.highlightCellForAddress(address);
                }
            }
            default -> {
            }
        }
        this.subscription.request(1);
    }

    private void updateRowHeight() {
        if (DataSegmentWindow.dataTable == null) {
            return;
        }
        final Font[] possibleFonts = {
                this.settings.getFontByPosition(Settings.DATASEGMENT_HIGHLIGHT_FONT),
                this.settings.getFontByPosition(Settings.EVEN_ROW_FONT),
                this.settings.getFontByPosition(Settings.ODD_ROW_FONT),
        };
        int maxHeight = 0;
        for (final Font possibleFont : possibleFonts) {
            final int height = this.getFontMetrics(possibleFont).getHeight();
            if (height > maxHeight) {
                maxHeight = height;
            }
        }
        DataSegmentWindow.dataTable.setRowHeight(maxHeight);
    }

    ///////////////////////////////////////////////////////////////////////////////
    // Class defined to address apparent Javax.swing.JComboBox bug: when selection
    /////////////////////////////////////////////////////////////////////////////// is
    // is set programmatically using setSelectedIndex() rather than by
    /////////////////////////////////////////////////////////////////////////////// user-initiated
    // event (such as mouse click), the text displayed in the JComboBox is not
    /////////////////////////////////////////////////////////////////////////////// always
    // updated correctly. Sometimes it is, sometimes updated to incorrect value.
    // No pattern that I can detect. Google search yielded many forums addressing
    // this problem. One suggested solution, a JComboBox superclass overriding
    // setSelectedIndex to also call selectedItemChanged() did not help. Only this
    // solution to extend the model class to call the protected
    // "fireContentsChanged()" method worked. DPS 25-Jan-2009
    private static class CustomComboBoxModel extends DefaultComboBoxModel<String> {
        public CustomComboBoxModel(final String[] list) {
            super(list);
        }

        private void forceComboBoxUpdate(final int index) {
            super.fireContentsChanged(this, index, index);
        }
    }

    ////////////////////////////////////////////////////////////////////////
    // Class representing memory data table data

    class DataTableModel extends AbstractTableModel {
        final String[] columnNames;
        final Object[][] data;

        public DataTableModel(final Object[][] d, final String[] n) {
            this.data = d;
            this.columnNames = n;
        }

        @Override
        public int getColumnCount() {
            return this.columnNames.length;
        }

        @Override
        public int getRowCount() {
            return this.data.length;
        }

        @Override
        public String getColumnName(final int col) {
            return this.columnNames[col];
        }

        @Override
        public Object getValueAt(final int row, final int col) {
            return this.data[row][col];
        }

        /*
         * The cells in the Address column are not editable.
         * Value cells are editable except when displayed
         * in ASCII view - don't want to give the impression
         * that ASCII text can be entered directly because
         * it can't. It is possible but not worth the
         * effort to implement.
         */
        @Override
        public boolean isCellEditable(final int row, final int col) {
            // Note that the data/cell address is constant,
            // no matter where the cell appears onscreen.
            return col != DataSegmentWindow.ADDRESS_COLUMN && !DataSegmentWindow.this.asciiDisplay;
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.
         */
        @Override
        public Class<?> getColumnClass(final int c) {
            return this.getValueAt(0, c).getClass();
        }

        /*
         * Update cell contents in table model. This method should be called
         * only when user edits cell, so input validation has to be done. If
         * value is valid, MIPS memory is updated.
         */
        @Override
        public void setValueAt(final Object value, final int row, final int col) {
            final int val;
            int address = 0;
            try {
                val = Binary.stringToInt((String) value);
            } catch (final NumberFormatException nfe) {
                this.data[row][col] = "INVALID";
                this.fireTableCellUpdated(row, col);
                return;
            }

            // calculate address from row and column
            try {
                address = Binary.stringToInt((String) this.data[row][DataSegmentWindow.ADDRESS_COLUMN]) + (col - 1) * DataSegmentWindow.BYTES_PER_VALUE; // KENV
                // 1/6/05
            } catch (final NumberFormatException nfe) {
                // can't really happen since memory addresses are completely under
                // the control of my software.
            }
            // Assures that if changed during MIPS program execution, the update will
            // occur only between instructions.
            Globals.memoryAndRegistersLock.lock();
            try {
                try {
                    Globals.memory.setRawWord(address, val);
                }
                // somehow, user was able to display out-of-range address. Most likely to occur
                // between
                // stack base and Kernel. Also text segment with self-modifying-code setting
                // off.
                catch (final AddressErrorException aee) {
                    return;
                }
            } finally {
                Globals.memoryAndRegistersLock.unlock();
            } // end synchronized block
            final int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
            this.data[row][col] = NumberDisplayBaseChooser.formatNumber(val, valueBase);
            this.fireTableCellUpdated(row, col);
        }

        /*
         * Update cell contents in table model. Does not affect MIPS memory.
         */
        private void setDisplayAndModelValueAt(final Object value, final int row, final int col) {
            this.data[row][col] = value;
            this.fireTableCellUpdated(row, col);
        }

        private void printDebugData() {
            final int numRows = this.getRowCount();
            final int numCols = this.getColumnCount();

            for (int i = 0; i < numRows; i++) {
                System.out.print("    row " + i + ":");
                for (int j = 0; j < numCols; j++) {
                    System.out.print("  " + this.data[i][j]);
                }
                System.out.println();
            }
            System.out.println("--------------------------");
        }
    }

    // Special renderer capable of highlighting cells by changing background color.
    // Will set background to highlight color if certain conditions met.

    class AddressCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                       final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            final JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);

            cell.setHorizontalAlignment(SwingConstants.RIGHT);
            final int rowFirstAddress = Binary.stringToInt(table.getValueAt(row, DataSegmentWindow.ADDRESS_COLUMN).toString());
            if (DataSegmentWindow.this.settings.getBooleanSetting(Settings.Bool.DATA_SEGMENT_HIGHLIGHTING) && DataSegmentWindow.this.addressHighlighting &&
                    rowFirstAddress == DataSegmentWindow.this.addressRowFirstAddress && column == DataSegmentWindow.this.addressColumn) {
                cell.setBackground(DataSegmentWindow.this.settings.getColorSettingByPosition(Settings.DATASEGMENT_HIGHLIGHT_BACKGROUND));
                cell.setForeground(DataSegmentWindow.this.settings.getColorSettingByPosition(Settings.DATASEGMENT_HIGHLIGHT_FOREGROUND));
                cell.setFont(DataSegmentWindow.this.settings.getFontByPosition(Settings.DATASEGMENT_HIGHLIGHT_FONT));
            } else if (row % 2 == 0) {
                cell.setBackground(DataSegmentWindow.this.settings.getColorSettingByPosition(Settings.EVEN_ROW_BACKGROUND));
                cell.setForeground(DataSegmentWindow.this.settings.getColorSettingByPosition(Settings.EVEN_ROW_FOREGROUND));
                cell.setFont(DataSegmentWindow.this.settings.getFontByPosition(Settings.EVEN_ROW_FONT));
            } else {
                cell.setBackground(DataSegmentWindow.this.settings.getColorSettingByPosition(Settings.ODD_ROW_BACKGROUND));
                cell.setForeground(DataSegmentWindow.this.settings.getColorSettingByPosition(Settings.ODD_ROW_FOREGROUND));
                cell.setFont(DataSegmentWindow.this.settings.getFontByPosition(Settings.ODD_ROW_FONT));
            }
            return cell;
        }

    }

    ///////////////////////////////////////////////////////////////////
    //
    // JTable subclass to provide custom tool tips for each of the
    // text table column headers. From Sun's JTable tutorial.
    // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
    //
    private class MyTippedJTable extends JTable {
        MyTippedJTable(final DataTableModel m) {
            super(m);
        }

        private final String[] columnToolTips = {
                /* address */ "Base memory address for this row of the table.",
                /* value +0 */ "32-bit value stored at base address for its row.",
                /* value +n */ "32-bit value stored ",
                /* value +n */ " bytes beyond base address for its row."
        };

        // Implement table header tool tips.
        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(this.columnModel) {
                @Override
                public String getToolTipText(final MouseEvent e) {
                    final String tip = null;
                    final java.awt.Point p = e.getPoint();
                    final int index = this.columnModel.getColumnIndexAtX(p.x);
                    final int realIndex = this.columnModel.getColumn(index).getModelIndex();
                    return (realIndex < 2) ? MyTippedJTable.this.columnToolTips[realIndex]
                            : MyTippedJTable.this.columnToolTips[2] + ((realIndex - 1) * 4) + MyTippedJTable.this.columnToolTips[3];
                }
            };
        }
    }

    ///////////////////////////////////////////////////////////////////////
    //
    // The Prev button (left arrow) scrolls downward through the
    // selected address range. It is a RepeatButton, which means
    // if the mouse is held down on the button, it will repeatedly
    // fire after an initial delay. Allows rapid scrolling.
    // DPS 20 July 2008
    private class PrevButton extends RepeatButton {
        public PrevButton(final Icon ico) {
            super(ico);
            this.setInitialDelay(500); // 500 milliseconds hold-down before firing
            this.setDelay(60); // every 60 milliseconds after that
            this.addActionListener(this);
        }

        // This one will respond when either timer goes off or button lifted.
        @Override
        public void actionPerformed(final ActionEvent ae) {
            DataSegmentWindow.this.firstAddress -= DataSegmentWindow.PREV_NEXT_CHUNK_SIZE;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
        }
    }//////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////
    //
    // The Next button (right arrow) scrolls upward through the
    // selected address range. It is a RepeatButton, which means
    // if the mouse is held down on the button, it will repeatedly
    // fire after an initial delay. Allows rapid scrolling.
    // DPS 20 July 2008
    private class NextButton extends RepeatButton {
        public NextButton(final Icon ico) {
            super(ico);
            this.setInitialDelay(500); // 500 milliseconds hold-down before firing
            this.setDelay(60); // every 60 milliseconds after that
            this.addActionListener(this);
        }

        // This one will respond when either timer goes off or button lifted.
        @Override
        public void actionPerformed(final ActionEvent ae) {
            DataSegmentWindow.this.firstAddress += DataSegmentWindow.PREV_NEXT_CHUNK_SIZE;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
        }
    }//////////////////////////////////////////////////////////////////////

}
