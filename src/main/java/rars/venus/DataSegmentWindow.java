package rars.venus;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.api.DisplayFormat;
import rars.notices.AccessType;
import rars.notices.MemoryAccessNotice;
import rars.notices.SimulatorNotice;
import rars.riscv.hardware.memory.AbstractMemoryConfiguration;
import rars.riscv.hardware.memory.MemoryListenerHandle;
import rars.settings.*;
import rars.util.BinaryUtilsKt;
import rars.venus.run.RunSpeedPanel;
import rars.venus.util.RepeatButton;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.Instant;

import static rars.Globals.SIMULATOR;
import static rars.riscv.hardware.memory.MemoryConfigurationKt.*;
import static rars.util.UtilsKt.*;
import static rars.venus.util.IconLoadingKt.loadIcon;

/**
 * Represents the Data Segment window, which is a type of JInternalFrame.
 *
 * @author Sanderson and Bumgarner
 */
public final class DataSegmentWindow extends JInternalFrame {

    private static final int VALUES_PER_ROW = 8;
    private static final int NUMBER_OF_ROWS = 16; // with 8 value columns, this shows 512 bytes;
    private static final int NUMBER_OF_COLUMNS = VALUES_PER_ROW + 1;// 1 for address and 8 for values
    private static final int BYTES_PER_VALUE = 4;
    private static final int BYTES_PER_ROW = VALUES_PER_ROW * BYTES_PER_VALUE;
    private static final int MEMORY_CHUNK_SIZE = NUMBER_OF_ROWS * BYTES_PER_ROW;
    // PREV_NEXT_CHUNK_SIZE determines how many rows will be scrolled when Prev or
    // Next buttons fire.
    // MEMORY_CHUNK_SIZE/2 means scroll half a table up or down. Easier to view
    // series that flows off the edge.
    // MEMORY_CHUNK_SIZE means scroll a full table's worth. Scrolls through memory
    // faster. 
    private static final int PREV_NEXT_CHUNK_SIZE = MEMORY_CHUNK_SIZE / 2;
    private static final int ADDRESS_COLUMN = 0;
    private static final boolean USER_MODE = false;
    private static final boolean KERNEL_MODE = true;
    // Initalize arrays used with Base Address combo box chooser.
    // The combo box replaced the row of buttons when number of buttons expanded to 7.
    private static final int EXTERN_BASE_ADDRESS_INDEX = 0;
    private static final int GLOBAL_POINTER_ADDRESS_INDEX = 3; // 1;
    private static final int TEXT_BASE_ADDRESS_INDEX = 5; // 2;
    private static final int DATA_BASE_ADDRESS_INDEX = 1; // 3;
    private static final int HEAP_BASE_ADDRESS_INDEX = 2; // 4;
    private static final int STACK_POINTER_BASE_ADDRESS_INDEX = 4; // 5;
    private static final int MMIO_BASE_ADDRESS_INDEX = 6;
    // Must agree with above in number and order...
    private static final String[] descriptions = {
        " (.extern)", " (.data)", " (heap)", "current gp",
        "current sp", " (.text)", " (MMIO)"
    };
    private final Container contentPane;
    private final JPanel tablePanel;
    // The combo box replaced the row of buttons when number of buttons expanded to
    // 7!
    // We'll keep the button objects however and manually invoke their action
    // listeners
    // when the corresponding combo box item is selected. 
    private final JComboBox<String> baseAddressSelector;
    // Must agree with above in number and order...
    private final int[] displayBaseAddressArray;
    @NotNull
    private final ExecutePane executePane;
    private final @NotNull BoolSettingsImpl boolSettings;
    private final @NotNull FontSettingsImpl fontSettings;
    private final @NotNull EditorThemeSettingsImpl editorThemeSettings;
    private final @NotNull HighlightingSettingsImpl highlightingSettings;
    private final JButton dataButton, stackButton, globButton, heapButton, extnButton, mmioButton,
        textButton;
    private final RepeatButton nextButton, prevButton;
    private Object[][] dataData;
    private MyTippedJTable dataTable;
    private JScrollPane dataTableScroller;
    private boolean addressHighlighting = false;
    private boolean asciiDisplay = false;
    private int addressColumn;
    private int addressRowFirstAddress;
    private int firstAddress;
    private int homeAddress;
    private boolean userOrKernelMode;
    // The next bunch are initialized dynamically in initializeBaseAddressChoices()
    private String[] displayBaseAddressChoices;
    private int defaultBaseAddressIndex;
    private JButton[] baseAddressButtons;
    private @Nullable MemoryListenerHandle<Integer> handle;

    /**
     * Constructor for the Data Segment window.
     *
     * @param choosers
     *     an array of objects used by user to select number display
     *     base (10 or 16)
     */
    public DataSegmentWindow(
        final @NotNull NumberDisplayBasePicker @NotNull [] choosers,
        final @NotNull ExecutePane executePane,
        final @NotNull AllSettings allSettings
    ) {
        super("Data Segment", true, false, true, true);
        this.executePane = executePane;
        this.fontSettings = allSettings.fontSettings;
        this.boolSettings = allSettings.boolSettings;
        this.editorThemeSettings = allSettings.editorThemeSettings;
        this.highlightingSettings = allSettings.highlightingSettings;

        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        this.displayBaseAddressArray = new int[]{
            memoryConfiguration.getExternAddress(),
            memoryConfiguration.getDataBaseAddress(),
            getHeapBaseAddress(
                memoryConfiguration),
            -1 /* memoryConfiguration.globalPointer */,
            -1 /* memoryConfiguration.stackPointer */,
            getTextSegmentBaseAddress(
                memoryConfiguration),
            getMemoryMapBaseAddress(
                memoryConfiguration),
        };
        SIMULATOR.simulatorNoticeHook.subscribe(s -> {
            if (s.action == SimulatorNotice.Action.START) {
                // Simulated MIPS execution starts. Respond to memory changes if running in
                // timed
                // or stepped mode.
                if (s.runSpeed != RunSpeedPanel.UNLIMITED_SPEED || s.maxSteps == 1) {
                    handle = unwrap(Globals.MEMORY_INSTANCE.subscribe(this::processMemoryAccessNotice));
                    this.addressHighlighting = true;
                }
            } else {
                // Simulated MIPS execution stops. Stop responding.
                if (handle != null) {
                    Globals.MEMORY_INSTANCE.unsubscribe(handle);
                    handle = null;
                }
            }
            return Unit.INSTANCE;
        });

        allSettings.fontSettings.onChangeListenerHook.subscribe(ignored -> {
            this.updateRowHeight();
            return Unit.INSTANCE;
        });

        this.homeAddress = memoryConfiguration.getDataBaseAddress(); // address for Home button
        this.firstAddress = this.homeAddress; // first address to display at any given time
        this.userOrKernelMode = USER_MODE;
        this.addressHighlighting = false;
        this.contentPane = this.getContentPane();
        this.tablePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        final var features = new JPanel();

        prevButton = new RepeatButton(loadIcon("Previous22.png"), null);
        prevButton.setInitialDelay(500);
        prevButton.setDelay(60);
        prevButton.addActionListener(e -> {
            firstAddress += PREV_NEXT_CHUNK_SIZE;
            firstAddress =
                setFirstAddressAndPrevNextButtonEnableStatus(firstAddress);
            updateModelForMemoryRange(firstAddress);

        });
        nextButton = new RepeatButton(loadIcon("Next22.png"), null);
        nextButton.setInitialDelay(500);
        nextButton.setDelay(60);
        nextButton.addActionListener(e -> {
            firstAddress += PREV_NEXT_CHUNK_SIZE;
            firstAddress =
                setFirstAddressAndPrevNextButtonEnableStatus(firstAddress);
            updateModelForMemoryRange(firstAddress);
        });
        // This group of buttons was replaced by a combo box. Keep the JButton objects
        // for their action listeners.
        this.dataButton = new JButton();// ".data");
        this.stackButton = new JButton();// "$sp");
        this.globButton = new JButton();// "$gp");
        this.heapButton = new JButton();// "heap");
        this.extnButton = new JButton();// ".extern");
        this.mmioButton = new JButton();// "MMIO");
        this.textButton = new JButton();// ".text");

        this.initializeBaseAddressChoices();
        this.baseAddressSelector = new JComboBox<>();
        this.baseAddressSelector.setModel(new CustomComboBoxModel(this.displayBaseAddressChoices));
        this.baseAddressSelector.setEditable(false);
        this.baseAddressSelector.setSelectedIndex(this.defaultBaseAddressIndex);
        this.baseAddressSelector.setToolTipText(
            "Base address for data segment display");
        this.baseAddressSelector.addActionListener(
            e -> {
                // trigger action listener for associated invisible button.
                baseAddressButtons[baseAddressSelector.getSelectedIndex()].getActionListeners()[0]
                    .actionPerformed(null);
            });

        this.addButtonActionListenersAndInitialize(memoryConfiguration);
        final JPanel navButtons = new JPanel(new GridLayout(1, 4));
        navButtons.add(this.prevButton);
        navButtons.add(this.nextButton);
        features.add(navButtons);
        features.add(this.baseAddressSelector);
        for (final NumberDisplayBasePicker chooser : choosers) {
            features.add(chooser);
        }
        final JCheckBox asciiDisplayCheckBox = new JCheckBox(
            "ASCII",
            this.asciiDisplay
        );
        asciiDisplayCheckBox
            .setToolTipText(
                "Display data segment values in ASCII (overrides Hexadecimal Values setting)");
        asciiDisplayCheckBox.addItemListener(
            e -> {
                asciiDisplay = (e.getStateChange() == ItemEvent.SELECTED);
                updateValues();
            });
        features.add(asciiDisplayCheckBox);

        this.contentPane.add(features, BorderLayout.SOUTH);
    }

    /**
     * Create and fill String array containing labels for base address combo box.
     */
    private static String[] createBaseAddressLabelsArray(
        final int[] baseAddressArray,
        final String[] descriptions
    ) {
        final String[] baseAddressChoices = new String[baseAddressArray.length];
        for (int i = 0; i < baseAddressChoices.length; i++) {
            baseAddressChoices[i] = (
                (baseAddressArray[i] != -1)
                    ? BinaryUtilsKt.intToHexStringWithPrefix(baseAddressArray[i])
                    : ""
            ) + descriptions[i];
        }
        return baseAddressChoices;
    }

    /**
     * Given an address, determine which segment it is in and return the
     * corresponding
     * combo box index. Note there is not a one-to-one correspondence between these
     * indexes and the Memory tables. For instance, the heap (0x10040000), the
     * global (0x10008000) and the data segment base (0x10000000) are all stored in
     * the
     * same table as the static (0x10010000) so all are "Memory.inDataSegment()".
     */
    private static int getBaseAddressIndexForAddress(final int address) {
        if (Globals.MEMORY_INSTANCE.isAddressInMemorySegment(address)) {
            return MMIO_BASE_ADDRESS_INDEX;
        } else if (Globals.MEMORY_INSTANCE.isAddressInTextSegment(address)) {
            return TEXT_BASE_ADDRESS_INDEX;
        }
        // Check distance from .extern base. Cannot be below it
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        int thisDistance = address - memoryConfiguration.getExternAddress();
        int shortDistance = 0x7fffffff;
        // assume not a data address.
        int desiredComboBoxIndex = -1;
        if (thisDistance >= 0 && thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = EXTERN_BASE_ADDRESS_INDEX;
        }
        // Check distance from global pointer; can be either side of it...
        final var gpValue = (int) Globals.REGISTER_FILE.gp.getValue();
        thisDistance = Math.abs(address - gpValue); // 
        // distance from
        // global
        // pointer
        if (thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = GLOBAL_POINTER_ADDRESS_INDEX;
        }
        // Check distance from .data base. Cannot be below it
        thisDistance = address - memoryConfiguration.getDataBaseAddress();
        if (thisDistance >= 0 && thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = DATA_BASE_ADDRESS_INDEX;
        }
        // Check distance from heap base. Cannot be below it
        thisDistance = address - getHeapBaseAddress(
            memoryConfiguration);
        if (thisDistance >= 0 && thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = HEAP_BASE_ADDRESS_INDEX;
        }
        // Check distance from stack pointer. Can be on either side of it...
        thisDistance = Math.abs(address - (int) Globals.REGISTER_FILE.gp.getValue());
        if (thisDistance < shortDistance) {
            desiredComboBoxIndex = STACK_POINTER_BASE_ADDRESS_INDEX;
        }
        return desiredComboBoxIndex;
    }

    // Little helper. Is called when headers set up and each time number base
    // changes.
    private static @NotNull String getHeaderStringForColumn(
        final int i,
        final @NotNull DisplayFormat base
    ) {
        final var offset = (i - 1) * BYTES_PER_VALUE;
        return (i == ADDRESS_COLUMN)
            ? "Address"
            : "Value (+%s)".formatted(BinaryUtilsKt.intFormatToString(
                offset,
                base
            ));
    }

    public @NotNull Unit processMemoryAccessNotice(final @NotNull MemoryAccessNotice notice) {
        if (notice.accessType == AccessType.WRITE) {
            // Uses the same highlighting technique as for Text Segment -- see
            // AddressCellRenderer class in java.
            final var address = notice.address;
            this.highlightCellForAddress(address);
        }
        return Unit.INSTANCE;
    }

    public void updateBaseAddressComboBox() {
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        this.displayBaseAddressArray[EXTERN_BASE_ADDRESS_INDEX] = memoryConfiguration.getExternAddress();
        this.displayBaseAddressArray[GLOBAL_POINTER_ADDRESS_INDEX] = -1; /* Memory.globalPointer */
        this.displayBaseAddressArray[DATA_BASE_ADDRESS_INDEX] = memoryConfiguration.getDataBaseAddress();
        this.displayBaseAddressArray[HEAP_BASE_ADDRESS_INDEX] = getHeapBaseAddress(
            memoryConfiguration);
        this.displayBaseAddressArray[STACK_POINTER_BASE_ADDRESS_INDEX] = -1; /* Memory.stackPointer */
        this.displayBaseAddressArray[MMIO_BASE_ADDRESS_INDEX] = getMemoryMapBaseAddress(
            memoryConfiguration);
        this.displayBaseAddressArray[TEXT_BASE_ADDRESS_INDEX] = getTextSegmentBaseAddress(
            memoryConfiguration);
        this.displayBaseAddressChoices = createBaseAddressLabelsArray(
            this.displayBaseAddressArray,
            this.descriptions
        );
        this.baseAddressSelector.setModel(new CustomComboBoxModel(this.displayBaseAddressChoices));
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
     * @param address
     *     data segment address of word to be selected.
     */
    void selectCellForAddress(final int address) {
        final Point rowColumn = this.displayCellForAddress(address);
        if (rowColumn == null) {
            return;
        }
        final var addressCell = dataTable.getCellRect(
            rowColumn.x,
            rowColumn.y,
            true
        );
        // Select the memory address cell by generating a fake Mouse Pressed event
        // within its
        // extent and explicitly invoking the table's mouse listener.
        final var fakeMouseEvent = new MouseEvent(
            dataTable, MouseEvent.MOUSE_PRESSED,
            Instant.now().toEpochMilli(), MouseEvent.BUTTON1_DOWN_MASK,
            (int) addressCell.getX() + 1,
            (int) addressCell.getY() + 1, 1, false
        );
        final MouseListener[] mouseListeners = dataTable.getMouseListeners();
        for (final MouseListener mouseListener : mouseListeners) {
            mouseListener.mousePressed(fakeMouseEvent);
        }
    }

    /**
     * Scroll the viewport so the cell at the given data segment address
     * is visible, vertically centered if possible, and highlighted (but not
     * selected).
     *
     * @param address
     *     data segment address of word to be selected.
     */
    public void highlightCellForAddress(final int address) {
        final Point rowColumn = this.displayCellForAddress(address);
        if (rowColumn == null || rowColumn.x < 0 || rowColumn.y < 0) {
            return;
        }
        final int addressRow = rowColumn.x;
        this.addressColumn = rowColumn.y;
        this.addressRowFirstAddress = BinaryUtilsKt
            .stringToInt(dataTable.getValueAt(
                    addressRow,
                    ADDRESS_COLUMN
                )
                .toString());
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
        dataTable.tableChanged(new TableModelEvent(
            dataTable.getModel(), 0,
            dataData.length - 1
        ));
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
    private @Nullable Point displayCellForAddress(final int address) {
        // This requires a 5-step process. Each step is described
        // just above the statements that implement it.

        // STEP 1: Determine which data segment contains this address.
        final int desiredComboBoxIndex = getBaseAddressIndexForAddress(
            address);
        if (desiredComboBoxIndex < 0) {
            // It is not a data segment address so good bye!
            return null;
        }
        // STEP 2: Set the combo box appropriately. This will also display the
        // first chunk of addresses from that segment.
        this.baseAddressSelector.setSelectedIndex(desiredComboBoxIndex);
        ((CustomComboBoxModel) this.baseAddressSelector.getModel()).forceComboBoxUpdate(
            desiredComboBoxIndex);
        this.baseAddressButtons[desiredComboBoxIndex].getActionListeners()[0].actionPerformed(
            null);
        // STEP 3: Display memory chunk containing this address, which may be
        // different than the one just displayed.
        int baseAddress = this.displayBaseAddressArray[desiredComboBoxIndex];
        if (baseAddress == -1) {
            final var gpValue = (int) Globals.REGISTER_FILE.gp.getValue();
            if (desiredComboBoxIndex == GLOBAL_POINTER_ADDRESS_INDEX) {
                baseAddress = gpValue - (gpValue % BYTES_PER_ROW);
            } else if (desiredComboBoxIndex == STACK_POINTER_BASE_ADDRESS_INDEX) {
                baseAddress = gpValue - (gpValue % BYTES_PER_ROW);
            } else {
                return null;// shouldn't happen since these are the only two
            }
        }
        final int byteOffset = address - baseAddress;
        final int chunkOffset = byteOffset / MEMORY_CHUNK_SIZE;
        // Subtract 1 from chunkOffset because we're gonna call the "next" action
        // listener to get the correct chunk loaded and displayed, and the first
        // thing it does is increment firstAddress by MEMORY_CHUNK_SIZE. Here
        // we do an offsetting decrement in advance because we don't want the
        // increment but we want the other actions that method provides.
        this.firstAddress =
            this.firstAddress + chunkOffset * MEMORY_CHUNK_SIZE - PREV_NEXT_CHUNK_SIZE;
        this.nextButton.getActionListeners()[0].actionPerformed(null);
        // STEP 4: Find cell containing this address. Add 1 to column calculation
        // because table column 0 displays address, not memory contents. The
        // "convertColumnIndexToView()" is not necessary because the columns cannot be
        // reordered, but I included it as a precautionary measure in case that changes.
        final int byteOffsetIntoChunk = byteOffset % MEMORY_CHUNK_SIZE;
        int addrColumn = byteOffsetIntoChunk % BYTES_PER_ROW / BYTES_PER_VALUE + 1;
        addrColumn = dataTable.convertColumnIndexToView(
            addrColumn);
        final int addrRow = byteOffsetIntoChunk / BYTES_PER_ROW;
        final Rectangle addressCell = dataTable.getCellRect(
            addrRow,
            addrColumn,
            true
        );
        // STEP 5: Center the row containing the cell of interest, to the extent
        // possible.
        final double cellHeight = addressCell.getHeight();
        final double viewHeight = this.dataTableScroller.getViewport()
            .getExtentSize()
            .getHeight();
        final int numberOfVisibleRows = (int) (viewHeight / cellHeight);
        final int newViewPositionY = Math.max(
            (int) ((addrRow - ((double) numberOfVisibleRows / 2)) * cellHeight),
            0
        );
        this.dataTableScroller.getViewport()
            .setViewPosition(new Point(0, newViewPositionY));
        return new Point(addrRow, addrColumn);
    }

    private void initializeBaseAddressChoices() {
        // Also must agree in number and order. Upon combo box item selection, will
        // invoke
        // action listener for that item's button.
        this.baseAddressButtons = new JButton[this.descriptions.length];
        this.baseAddressButtons[EXTERN_BASE_ADDRESS_INDEX] = this.extnButton;
        this.baseAddressButtons[GLOBAL_POINTER_ADDRESS_INDEX] = this.globButton;
        this.baseAddressButtons[DATA_BASE_ADDRESS_INDEX] = this.dataButton;
        this.baseAddressButtons[HEAP_BASE_ADDRESS_INDEX] = this.heapButton;
        this.baseAddressButtons[STACK_POINTER_BASE_ADDRESS_INDEX] = this.stackButton;
        this.baseAddressButtons[MMIO_BASE_ADDRESS_INDEX] = this.mmioButton;
        this.baseAddressButtons[TEXT_BASE_ADDRESS_INDEX] = this.textButton;
        this.displayBaseAddressChoices = createBaseAddressLabelsArray(
            this.displayBaseAddressArray,
            this.descriptions
        );
        this.defaultBaseAddressIndex = DATA_BASE_ADDRESS_INDEX;
    }

    // Generates the Address/Data part of the Data Segment window.
    // Returns the JScrollPane for the Address/Data part of the Data Segment window.
    private JScrollPane generateDataPanel() {
        dataData = new Object[NUMBER_OF_ROWS][NUMBER_OF_COLUMNS];
        final var valueBase = this.executePane.getValueDisplayFormat();
        final var addressBase = this.executePane.getAddressDisplayFormat();
        int address = this.homeAddress;
        for (int row = 0; row < NUMBER_OF_ROWS; row++) {
            dataData[row][ADDRESS_COLUMN] =
                NumberDisplayBasePicker.formatUnsignedInteger(
                    address,
                    addressBase
                );
            for (int column = 1; column < NUMBER_OF_COLUMNS; column++) {
                final var value = rightOr(
                    Globals.MEMORY_INSTANCE.getRawWord(address), 0);
                NumberDisplayBasePicker.formatNumber(
                    value,
                    valueBase
                );
                address += BYTES_PER_VALUE;
            }
        }
        final String[] names = new String[NUMBER_OF_COLUMNS];
        for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
            names[i] = getHeaderStringForColumn(
                i,
                addressBase
            );
        }
        dataTable = new MyTippedJTable(new DataTableModel(
            dataData,
            names
        ));

        this.updateRowHeight();
        // Do not allow user to re-order columns; column order corresponds to MIPS
        // memory order
        dataTable.getTableHeader()
            .setReorderingAllowed(false);
        dataTable.setRowSelectionAllowed(false);
        // Addresses are column 0, render right-justified in mono font
        final var monoRightCellRenderer = new MonoRightCellRenderer(
            fontSettings,
            editorThemeSettings
        );
        dataTable.getColumnModel()
            .getColumn(ADDRESS_COLUMN)
            .setPreferredWidth(60);
        dataTable.getColumnModel()
            .getColumn(ADDRESS_COLUMN)
            .setCellRenderer(
                monoRightCellRenderer);
        // Data cells are columns 1 onward, render right-justitifed in mono font but
        // highlightable.
        final AddressCellRenderer addressCellRenderer = new AddressCellRenderer();
        for (int i = 1; i < NUMBER_OF_COLUMNS; i++) {
            dataTable.getColumnModel()
                .getColumn(i)
                .setPreferredWidth(60);
            dataTable.getColumnModel()
                .getColumn(i)
                .setCellRenderer(addressCellRenderer);
        }
        this.dataTableScroller = new JScrollPane(
            dataTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        return this.dataTableScroller;
    }

    /**
     * Generates and displays fresh table, typically done upon successful assembly.
     */
    public void setupTable() {
        tablePanel.removeAll();
        tablePanel.add(generateDataPanel());
        contentPane.add(tablePanel);
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
        dataTable.tableChanged(new TableModelEvent(
            dataTable.getModel(), 0,
            dataData.length - 1
        ));
        // The below addresses situation in which addressRow and addressColum hold their
        // values across assemble operations. Whereupon at the first step of the next
        // run the last cells from the previous run are highlighted! This method is
        // called
        // after each successful assemble (or reset, which just re-assembles). The
        // assignment below assures the highlighting condition column==addressColumn
        // will be
        // initially false since column>=0. 
        this.addressColumn = -1;
    }

    private @NotNull DisplayFormat getValueDisplayFormat() {
        if (this.asciiDisplay) {
            return DisplayFormat.ASCII;
        } else {
            return this.executePane.getValueDisplayFormat();
        }
    }

    /**
     * Update table model with contents of new memory "chunk". Mars supports
     * megabytes of
     * data segment space so we only plug a "chunk" at a time into the table.
     *
     * @param firstAddr
     *     the first address in the memory range to be placed in the
     *     model.
     */
    public void updateModelForMemoryRange(final int firstAddr) {
        if (this.tablePanel.getComponentCount() == 0) {
            return; // ignore if no content to change
        }
        final var valueBase = this.getValueDisplayFormat();
        final var addressBase = this.executePane.getAddressDisplayFormat();
        int address = firstAddr;
        final TableModel dataModel = dataTable.getModel();
        for (int row = 0; row < NUMBER_OF_ROWS; row++) {
            ((DataTableModel) dataModel).setDisplayAndModelValueAt(
                NumberDisplayBasePicker.formatUnsignedInteger(
                    address,
                    addressBase
                ), row,
                ADDRESS_COLUMN
            );
            for (int column = 1; column < NUMBER_OF_COLUMNS; column++) {
                final var finalRow = row;
                final var finalColumn = column;
                final int finalAddress = address;
                Globals.MEMORY_INSTANCE.getSilentMemoryView()
                    .getWord(address)
                    .fold(
                        error -> {
                            // Bit of a hack here. Memory will throw an exception if you try to read
                            // directly from text segment when the
                            // self-modifying code setting is disabled. This is a good thing if it is the
                            // executing MIPS program trying to
                            // read. But not a good thing if it is the DataSegmentDisplay trying to read.
                            // I'll trick Memory by
                            // temporarily enabling the setting as "non-persistent" so it won't write
                            // through to the registry.
                            if (Globals.MEMORY_INSTANCE.isAddressInTextSegment(
                                finalAddress)) {
                                int displayValue = 0;
                                if (!boolSettings.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) {
                                    boolSettings.setSetting(
                                        BoolSetting.SELF_MODIFYING_CODE_ENABLED,
                                        true
                                    );
                                    displayValue = unwrap(Globals.MEMORY_INSTANCE.getSilentMemoryView()
                                        .getWord(finalAddress));
                                    boolSettings.setSetting(
                                        BoolSetting.SELF_MODIFYING_CODE_ENABLED,
                                        false
                                    );
                                }
                                ((DataTableModel) dataModel).setDisplayAndModelValueAt(
                                    NumberDisplayBasePicker.formatNumber(
                                        displayValue,
                                        valueBase
                                    ), finalRow, finalColumn
                                );
                            }
                            // Bug Fix: the following line of code disappeared during the release 4.4 mods,
                            // but is essential to
                            // display values of 0 for valid MIPS addresses that are outside the MARS
                            // simulated address space. Such
                            // addresses cause an AddressErrorException. Prior to 4.4, they performed this
                            // line of code unconditionally.
                            // With 4.4, I added the above IF statement to work with the text segment but
                            // inadvertently removed this line!
                            // Now it becomes the "else" part, executed when not in text segment. 
                            else {
                                ((DataTableModel) dataModel).setDisplayAndModelValueAt(
                                    NumberDisplayBasePicker.formatNumber(
                                        0,
                                        valueBase
                                    ), finalRow, finalColumn
                                );
                            }
                            return Unit.INSTANCE;
                        },
                        value -> {
                            ((DataTableModel) dataModel).setDisplayAndModelValueAt(
                                NumberDisplayBasePicker.formatNumber(
                                    value,
                                    valueBase
                                ),
                                finalRow, finalColumn
                            );
                            return Unit.INSTANCE;
                        }
                    );
                address += BYTES_PER_VALUE;
            }
        }
    }

    /**
     * Redisplay the addresses. This should only be done when address display base
     * is
     * modified (e.g. between base 16, hex, and base 10, dec).
     */
    public void updateDataAddresses() {
        if (this.tablePanel.getComponentCount() == 0) {
            return; // ignore if no content to change
        }
        final var addressBase = this.executePane.getAddressDisplayFormat();
        int address = this.firstAddress;
        for (int i = 0; i < NUMBER_OF_ROWS; i++) {
            final String formattedAddress = NumberDisplayBasePicker.formatUnsignedInteger(
                address,
                addressBase
            );
            ((DataTableModel) dataTable.getModel()).setDisplayAndModelValueAt(
                formattedAddress,
                i,
                0
            );
            address += BYTES_PER_ROW;
        }
        // column headers include address offsets, so translate them too
        for (int i = 1; i < NUMBER_OF_COLUMNS; i++) {
            dataTable.getColumnModel()
                .getColumn(i)
                .setHeaderValue(getHeaderStringForColumn(
                    i,
                    addressBase
                ));
        }
        dataTable.getTableHeader().repaint();
    }

    /** Update data display to show all values */
    public void updateValues() {
        updateModelForMemoryRange(firstAddress);
    }

    private void disableAllButtons() {
        baseAddressSelector.setEnabled(false);
        globButton.setEnabled(false);
        stackButton.setEnabled(false);
        heapButton.setEnabled(false);
        extnButton.setEnabled(false);
        mmioButton.setEnabled(false);
        textButton.setEnabled(false);
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);
        dataButton.setEnabled(false);
    }

    private void enableAllButtons() {
        baseAddressSelector.setEnabled(true);
        globButton.setEnabled(true);
        stackButton.setEnabled(true);
        heapButton.setEnabled(true);
        extnButton.setEnabled(true);
        mmioButton.setEnabled(true);
        textButton.setEnabled(boolSettings.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED));
        prevButton.setEnabled(true);
        nextButton.setEnabled(true);
        dataButton.setEnabled(true);
    }

    private void addButtonActionListenersAndInitialize(final @NotNull AbstractMemoryConfiguration<Integer> memoryConfiguration) {
        // set initial states
        this.disableAllButtons();
        // add tool tips
        // NOTE: For buttons that are now combo box items, the tool tips are not
        // displayed w/o custom renderer.
        this.globButton.setToolTipText("View range around global pointer");
        this.stackButton.setToolTipText("View range around stack pointer");
        this.heapButton.setToolTipText("View range around heap base address " +
            BinaryUtilsKt.intToHexStringWithPrefix(getHeapBaseAddress(
                memoryConfiguration)));
        this.extnButton.setToolTipText(
            "View range around static global base address " +
                BinaryUtilsKt.intToHexStringWithPrefix(memoryConfiguration.getExternAddress()));
        this.mmioButton.setToolTipText("View range around MMIO base address " +
            BinaryUtilsKt.intToHexStringWithPrefix(getMemoryMapBaseAddress(
                memoryConfiguration)));
        this.textButton.setToolTipText("View range around program code " +
            BinaryUtilsKt.intToHexStringWithPrefix(getTextSegmentBaseAddress(
                memoryConfiguration)));
        this.prevButton.setToolTipText(
            "View next lower address range; hold down for rapid fire");
        this.nextButton.setToolTipText(
            "View next higher address range; hold down for rapid fire");
        this.dataButton.setToolTipText(
            "View range around static data segment base address " +
                BinaryUtilsKt.intToHexStringWithPrefix(memoryConfiguration.getDataBaseAddress()));

        this.globButton.addActionListener(
            ae -> {
                userOrKernelMode = USER_MODE;
                // get $gp global pointer, but guard against it having value below data segment
                firstAddress = Math.max(
                    getDataSegmentBaseAddress(
                        memoryConfiguration),
                    (int) Globals.REGISTER_FILE.gp.getValue()
                );
                // updateModelForMemoryRange requires argument to be multiple of 4
                // but for cleaner display we'll make it multiple of 32 (last nibble is 0).
                // This makes it easier to mentally calculate address from row address + column
                // offset.
                firstAddress =
                    firstAddress - (firstAddress % BYTES_PER_ROW);
                homeAddress = firstAddress;
                firstAddress =
                    setFirstAddressAndPrevNextButtonEnableStatus(firstAddress);
                updateModelForMemoryRange(firstAddress);
            });

        stackButton.addActionListener(ae -> {
            userOrKernelMode = USER_MODE;
            // get $sp stack pointer, but guard against it having value below data segment
            firstAddress = Math.max(
                getDataSegmentBaseAddress(memoryConfiguration),
                (int) Globals.REGISTER_FILE.sp.getValue()
            );
            // See comment above for gloButton...
            firstAddress = firstAddress - (firstAddress % BYTES_PER_ROW);
            homeAddress = getStackBaseAddress(memoryConfiguration);
            firstAddress = setFirstAddressAndPrevNextButtonEnableStatus(
                firstAddress);
            updateModelForMemoryRange(firstAddress);
        });

        heapButton.addActionListener(ae -> {
            userOrKernelMode = USER_MODE;
            homeAddress = getHeapBaseAddress(
                memoryConfiguration);
            firstAddress =
                setFirstAddressAndPrevNextButtonEnableStatus(
                    homeAddress);
            updateModelForMemoryRange(
                firstAddress);
        });

        extnButton.addActionListener(ae -> {
            userOrKernelMode = USER_MODE;
            homeAddress = memoryConfiguration.getExternAddress();
            firstAddress =
                setFirstAddressAndPrevNextButtonEnableStatus(
                    homeAddress);
            updateModelForMemoryRange(
                firstAddress);
        });

        mmioButton.addActionListener(ae -> {
            userOrKernelMode = KERNEL_MODE;
            homeAddress = getMemoryMapBaseAddress(
                memoryConfiguration);
            firstAddress = homeAddress;
            firstAddress =
                setFirstAddressAndPrevNextButtonEnableStatus(
                    firstAddress);
            updateModelForMemoryRange(
                firstAddress);
        });

        textButton.addActionListener(ae -> {
            userOrKernelMode = USER_MODE;
            homeAddress = getTextSegmentBaseAddress(memoryConfiguration);
            firstAddress = homeAddress;
            firstAddress = setFirstAddressAndPrevNextButtonEnableStatus(
                firstAddress);
            updateModelForMemoryRange(firstAddress);
        });

        dataButton.addActionListener(ae -> {
            userOrKernelMode = USER_MODE;
            homeAddress = memoryConfiguration.getDataBaseAddress();
            firstAddress = homeAddress;
            firstAddress = setFirstAddressAndPrevNextButtonEnableStatus(
                firstAddress);
            updateModelForMemoryRange(firstAddress);
        });

    }

    /**
     * This will assure that user cannot view memory locations outside the data
     * for selected mode. For user mode, this means no lower than data segment base,
     * or higher than user memory boundary. For kernel mode, this means no lower than
     * kernel data segment base or higher than kernel memory. It is called by the
     * above action listeners.
     * lowAddress is lowest desired address to view, it is adjusted if necessary
     * and returned.
     * PrevButton and NextButton are enabled/disabled appropriately.
     */
    private int setFirstAddressAndPrevNextButtonEnableStatus(int lowAddress) {
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        final int lowLimit =
            (this.userOrKernelMode == USER_MODE) ?
                Math.min(Math.min(
                    getTextSegmentBaseAddress(memoryConfiguration),
                    getDataSegmentBaseAddress(memoryConfiguration)
                ), memoryConfiguration.getDataBaseAddress()) :
                getMemoryMapBaseAddress(memoryConfiguration);
        final int highLimit = (this.userOrKernelMode == USER_MODE)
            ? (memoryConfiguration).getUserHighAddress()
            : getKernelHighAddress(memoryConfiguration);
        if (lowAddress <= lowLimit) {
            lowAddress = lowLimit;
            this.prevButton.setEnabled(false);
        } else {
            this.prevButton.setEnabled(true);
        }
        if (lowAddress >= highLimit - MEMORY_CHUNK_SIZE) {
            lowAddress = highLimit - MEMORY_CHUNK_SIZE + 1;
            this.nextButton.setEnabled(false);
        } else {
            this.nextButton.setEnabled(true);
        }
        return lowAddress;
    }

    private void updateRowHeight() {
        if (dataTable == null) {
            return;
        }
        final var font = fontSettings.getCurrentFont();
        final var height = this.getFontMetrics(font).getHeight();
        dataTable.setRowHeight(height);
    }

    /**
     * Class defined to address apparent Javax.swing.JComboBox bug: when selection
     * is set programmatically using setSelectedIndex() rather than by user-initiated
     * event (such as mouse click), the text displayed in the JComboBox is not
     * updated correctly. Sometimes it is, sometimes updated to incorrect value.
     * No pattern that I can detect. Google search yielded many forums addressing
     * this problem. One suggested solution, a JComboBox superclass overriding
     * setSelectedIndex to also call selectedItemChanged() did not help. Only this
     * solution to extend the model class to call the protected
     * "fireContentsChanged()" method worked.
     */
    private static final class CustomComboBoxModel
        extends DefaultComboBoxModel<String> {
        private CustomComboBoxModel(final String[] list) {
            super(list);
        }

        private void forceComboBoxUpdate(final int index) {
            super.fireContentsChanged(this, index, index);
        }
    }

    /**
     * Class representing memory data table data.
     */
    private final class DataTableModel extends AbstractTableModel {
        final String[] columnNames;
        final Object[][] data;

        private DataTableModel(final Object[][] d, final String[] n) {
            super();
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
            return col != ADDRESS_COLUMN && !asciiDisplay;
        }

        /**
         * JTable uses this method to determine the default renderer/
         * editor for each cell.
         */
        @Override
        public Class<?> getColumnClass(final int c) {
            return this.getValueAt(0, c).getClass();
        }

        /**
         * Update cell contents in table model. This method should be called
         * only when user edits cell, so input validation has to be done. If
         * value is valid, MIPS memory is updated.
         */
        @Override
        public void setValueAt(
            final Object value,
            final int row,
            final int col
        ) {
            final var parsed = BinaryUtilsKt.stringToInt((String) value);
            if (parsed == null) {
                this.data[row][col] = "INVALID";
                this.fireTableCellUpdated(row, col);
                return;
            }

            // calculate address from row and column
            final var address = BinaryUtilsKt.stringToInt(
                (String) this.data[row][ADDRESS_COLUMN]
            ) + (col - 1) * BYTES_PER_VALUE;

            // Assures that if changed during program execution, the update will
            // occur only between instructions.
            Globals.MEMORY_REGISTERS_LOCK.lock();
            try {
                final var isValid = Globals.MEMORY_INSTANCE.setRawWord(
                    address,
                    parsed
                ).isRight();
                if (!isValid) {
                    // somehow, user was able to display out-of-range address. Most likely to occur
                    // between
                    // stack base and Kernel. Also text segment with self-modifying-code setting
                    // off.
                    return;
                }
            } finally {
                Globals.MEMORY_REGISTERS_LOCK.unlock();
            }
            final var valueBase = executePane.getValueDisplayFormat();
            this.data[row][col] = NumberDisplayBasePicker.formatNumber(
                parsed,
                valueBase
            );
            this.fireTableCellUpdated(row, col);
        }

        /**
         * Update cell contents in table model. Does not affect MIPS memory.
         */
        private void setDisplayAndModelValueAt(
            final Object value,
            final int row,
            final int col
        ) {
            this.data[row][col] = value;
            this.fireTableCellUpdated(row, col);
        }
    }

    /**
     * Special renderer capable of highlighting cells by changing background color.
     * Will set background to highlight color if certain conditions met.
     */
    private class AddressCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
            final JTable table, final Object value,
            final boolean isSelected, final boolean hasFocus,
            final int row, final int column
        ) {
            final JLabel cell = (JLabel) super.getTableCellRendererComponent(
                table, value,
                isSelected, hasFocus, row, column
            );

            cell.setHorizontalAlignment(SwingConstants.RIGHT);
            final var rowFirstAddress = BinaryUtilsKt.stringToInt(table.getValueAt(
                row,
                ADDRESS_COLUMN
            ).toString());
            final var theme = editorThemeSettings.getCurrentTheme();
            final var defaultFont = fontSettings.getCurrentFont();
            if (/*this.settings.getBoolSettings().getSetting(BoolSetting.DATA_SEGMENT_HIGHLIGHTING)
             &&*/
                addressHighlighting &&
                    rowFirstAddress == addressRowFirstAddress &&
                    column == addressColumn) {
                final var style = highlightingSettings.getDataSegmentHighlightingStyle();
                if (style != null) {
                    cell.setBackground(style.getBackground());
                    cell.setForeground(style.getForeground());
                    cell.setFont(applyStyle(defaultFont, style));
                } else {
                    cell.setBackground(theme.backgroundColor);
                    cell.setForeground(theme.foregroundColor);
                    cell.setFont(defaultFont);
                }
            } else {
                cell.setBackground(theme.backgroundColor);
                cell.setForeground(theme.foregroundColor);
                cell.setFont(defaultFont);
            }
            return cell;
        }

    }

    /**
     * JTable subclass to provide custom tool tips for each of the
     * text table column headers. From
     * <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/table.html">Sun's JTable tutorial</a>.
     */
    private class MyTippedJTable extends JTable {
        private final String[] columnToolTips = {
            /* address */ "Base memory address for this row of the table.",
            /* value +0 */ "32-bit value stored at base address for its row.",
            /* value +n */ "32-bit value stored ",
            /* value +n */ " bytes beyond base address for its row."
        };

        MyTippedJTable(final DataTableModel m) {
            super(m);
        }

        // Implement table header tool tips.
        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(columnModel) {
                @Override
                public String getToolTipText(final MouseEvent e) {
                    final var p = e.getPoint();
                    final var index = columnModel.getColumnIndexAtX(p.x);
                    final var realIndex = columnModel.getColumn(index)
                        .getModelIndex();
                    return switch (realIndex) {
                        case 0 ->
                            "Base memory address for this row of the table.";
                        case 1 ->
                            "32-bit value stored at base address for its row.";
                        default ->
                            "32-bit value stored %d bytes beyond base address for its row."
                                .formatted((realIndex - 1) * 4);
                    };
                }
            };
        }
    }
}
