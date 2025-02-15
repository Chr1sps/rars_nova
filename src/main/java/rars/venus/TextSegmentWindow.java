package rars.venus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.ProgramStatement;
import rars.assembler.DataTypes;
import rars.exceptions.AddressErrorException;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;
import rars.notices.SimulatorNotice;
import rars.settings.BoolSetting;
import rars.util.BinaryUtils;
import rars.util.FontUtilities;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Objects;
import java.util.function.Consumer;

import static rars.Globals.*;
import static rars.util.Utils.deriveFontFromStyle;

/*
Copyright (c) 2003-2007,  Pete Sanderson and Kenneth Vollmar

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
 * Creates the Text Segment window in the Execute tab of the UI
 *
 * @author Team JSpim
 */
public final class TextSegmentWindow extends JInternalFrame {
    private static final Logger LOGGER = LogManager.getLogger(TextSegmentWindow.class);
    private static final int PROGRAM_ARGUMENT_TEXTFIELD_COLUMNS = 40;
    // The following is displayed in the Basic and Source columns if existing code
    // is overwritten using self-modifying code feature
    private static final String modifiedCodeMarker = " ------ ";
    private static final @NotNull String breakpointToolTip = "If checked, will set an execution breakpoint. Click " +
        "header to disable/enable breakpoints",
        instructionAddressToolTip = "Text segment address of binary instruction code",
        instructionCodeToolTip = "32-bit binary RISCV instruction",
        basicInstructionsToolTip = "Basic assembler instruction",
        sourceToolTip = "Source code line";
    private final JPanel programArgumentsPanel; // DPS 17-July-2008
    private final JTextField programArgumentsTextField; // DPS 17-July-2008
    private final Container contentPane;
    @NotNull
    private final ExecutePane executePane;

    private JTable table;
    private JScrollPane tableScroller;
    private Object[][] data;
    /*
     * Maintain an int array of code addresses in parallel with ADDRESS_COLUMN,
     * to speed model-row -> text-address mapping. Maintain a Hashtable of
     * (text-address, model-row) pairs to speed text-address -> model-row mapping.
     * The former is used for breakpoints and changing display base (e.g. base 10
     * to 16); the latter is used for highlighting. Both structures will remain
     * consistent once set up, since address column is not editable.
     */
    private int[] intAddresses; // index is table model row, value is text address
    private Hashtable<Integer, Integer> addressRows; // first is text address, value is table model row
    private Hashtable<Integer, ModifiedCode> executeMods; // first is table model row, value is original code, basic,
    private TextTableModel tableModel;
    private final @NotNull Consumer<@NotNull MemoryAccessNotice> processMemoryAccessNotice = notice -> {
        if (notice.accessType == AccessNotice.AccessType.WRITE) {
            this.updateTable(notice.address, notice.value);
        }
    };
    private boolean codeHighlighting;
    private boolean breakpointsEnabled; // Added 31 Dec 2009
    private int highlightAddress;
    private TableModelListener tableModelListener;

    /**
     * Constructor, sets up a new JInternalFrame.
     */
    public TextSegmentWindow(final @NotNull ExecutePane executePane) {
        super("Text Segment", true, false, true, true);
        this.executePane = executePane;
        SIMULATOR.simulatorNoticeHook.subscribe(notice -> {
            if (notice.action() == SimulatorNotice.Action.START) {
                this.deleteAsTextSegmentObserver();
                if (BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) { // &&
                    // (notice.getRunSpeed()
                    // !=
                    // RunSpeedPanel.UNLIMITED_SPEED
                    // ||
                    // notice.getMaxSteps()==1))
                    // {
                    this.addAsTextSegmentObserver();
                }
            }
        });
        BOOL_SETTINGS.onChangeListenerHook.subscribe(ignore -> {
            this.deleteAsTextSegmentObserver();
            if (BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) {
                this.addAsTextSegmentObserver();
            }
        });
        FONT_SETTINGS.onChangeListenerHook.subscribe(ignore -> this.updateRowHeight());
        this.contentPane = this.getContentPane();
        this.codeHighlighting = true;
        this.breakpointsEnabled = true;
        this.programArgumentsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.programArgumentsPanel.add(new JLabel("Program Arguments: "));
        this.programArgumentsTextField = new JTextField(TextSegmentWindow.PROGRAM_ARGUMENT_TEXTFIELD_COLUMNS);
        this.programArgumentsTextField
            .setToolTipText("Arguments provided to program at runtime via a0 (argc) and a1 (argv)");
        this.programArgumentsPanel.add(this.programArgumentsTextField);
    }

    /**
     * Method to be called once the user compiles the program.
     * Should convert the lines of code over to the table rows and columns.
     */
    public void setupTable() {
        final int addressBase = this.executePane.getAddressDisplayBase();
        this.codeHighlighting = true;
        this.breakpointsEnabled = true;
        final var sourceStatementList = Globals.program.getMachineList();
        this.data = new Object[sourceStatementList.size()][ColumnData.values().length];
        this.intAddresses = new int[this.data.length];
        this.addressRows = new Hashtable<>(this.data.length);
        this.executeMods = new Hashtable<>(this.data.length);
        // Get highest source line number to determine #leading spaces so line numbers
        // will vertically align
        // In multi-file situation, this will not necessarily be the last line b/c
        // sourceStatementList contains
        // source lines from all files. DPS 3-Oct-10
        final var maxSourceLineNumber = sourceStatementList.stream()
            .mapToInt(statement -> statement.sourceLine.lineNumber())
            .max()
            .orElse(0);

        final var sourceLineDigitCount = Integer.toString(maxSourceLineNumber).length();
        int lastLine = -1;
        for (int i = 0; i < sourceStatementList.size(); i++) {
            final ProgramStatement statement = sourceStatementList.get(i);
            this.intAddresses[i] = statement.getAddress();
            this.addressRows.put(this.intAddresses[i], i);
            this.data[i][ColumnData.BREAKPOINT_COLUMN.number] = false;
            this.data[i][ColumnData.INSTRUCTION_ADDRESS_COLUMN.number] =
                NumberDisplayBaseChooser.formatUnsignedInteger(
                    statement.getAddress(),
                    addressBase
                );
            this.data[i][ColumnData.INSTRUCTION_CODE_COLUMN.number] =
                NumberDisplayBaseChooser.formatNumber(statement.getBinaryStatement(), 16);
            this.data[i][ColumnData.BASIC_INSTRUCTIONS_COLUMN.number] = statement.getPrintableBasicAssemblyStatement();
            final var builder = new StringBuilder();
            if (statement.sourceLine != null) {
                final int leadingSpacesCount = sourceLineDigitCount - Integer.toString(statement.sourceLine.lineNumber())
                    .length();
                final String lineNumber;
                if (statement.sourceLine.lineNumber() == lastLine) {
                    lineNumber = " ".repeat(sourceLineDigitCount) + "  ";
                } else {
                    lineNumber = " ".repeat(leadingSpacesCount) + statement.sourceLine.lineNumber() + ": ";
                }
                builder.append(lineNumber)
                    .append(FontUtilities.substituteSpacesForTabs(
                        statement.sourceLine.source(),
                        OTHER_SETTINGS.getEditorTabSize()
                    ));
                lastLine = statement.sourceLine.lineNumber();
            } else {
                lastLine = -1;
            }
            this.data[i][ColumnData.SOURCE_COLUMN.number] = builder.toString();
        }
        this.contentPane.removeAll();
        this.tableModel = new TextTableModel(this.data);
        if (this.tableModelListener != null) {
            this.tableModel.addTableModelListener(this.tableModelListener);
            this.tableModel.fireTableDataChanged();// initialize listener
        }
        this.table = new MyTippedJTable(this.tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        this.updateRowHeight();

        // prevents cells in row from being highlighted when user clicks on breakpoint
        // checkbox
        this.table.setRowSelectionAllowed(false);

        // region Columns setup

        final var columnModel = this.table.getColumnModel();

        final var breakpointColumn = columnModel.getColumn(ColumnData.BREAKPOINT_COLUMN.number);
        breakpointColumn.setMinWidth(40);
        breakpointColumn.setPreferredWidth(40);
        breakpointColumn.setMaxWidth(40);
        breakpointColumn.setCellRenderer(new CheckBoxTableCellRenderer());

        final CodeCellRenderer codeStepHighlighter = new CodeCellRenderer();

        final var sourceColumn = columnModel.getColumn(ColumnData.SOURCE_COLUMN.number);
        sourceColumn.setPreferredWidth(320);
        sourceColumn.setCellRenderer(codeStepHighlighter);

        final var basicInstructionsColumn = columnModel.getColumn(ColumnData.BASIC_INSTRUCTIONS_COLUMN.number);
        basicInstructionsColumn.setMinWidth(80);
        basicInstructionsColumn.setPreferredWidth(200);
        basicInstructionsColumn.setMaxWidth(320);
        basicInstructionsColumn.setCellRenderer(codeStepHighlighter);

        final var monoRightCellRenderer = new MonoRightCellRenderer();

        final var instructionAddressColumn = columnModel.getColumn(ColumnData.INSTRUCTION_ADDRESS_COLUMN.number);
        instructionAddressColumn.setMinWidth(80);
        instructionAddressColumn.setPreferredWidth(80);
        instructionAddressColumn.setMaxWidth(160);
        instructionAddressColumn.setCellRenderer(monoRightCellRenderer);

        final var instructionCodeColumn = columnModel.getColumn(ColumnData.INSTRUCTION_CODE_COLUMN.number);
        instructionCodeColumn.setMinWidth(80);
        instructionCodeColumn.setPreferredWidth(80);
        instructionCodeColumn.setMaxWidth(160);
        // to render String right-justified in mono font
        instructionCodeColumn.setCellRenderer(monoRightCellRenderer);

        // endregion Columns setup

        this.tableScroller = new JScrollPane(
            this.table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        this.contentPane.add(this.tableScroller);
        if (BOOL_SETTINGS.getSetting(BoolSetting.PROGRAM_ARGUMENTS)) {
            this.addProgramArgumentsPanel();
        }

        this.deleteAsTextSegmentObserver();
        if (BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) {
            this.addAsTextSegmentObserver();
        }
    }

    /**
     * Get program arguments from text field in south border of text segment window.
     *
     * @return String containing program arguments
     */
    public String getProgramArguments() {
        return this.programArgumentsTextField.getText();
    }

    public void addProgramArgumentsPanel() {
        // Don't add it if text segment window blank (file closed or no assemble yet)
        if (this.contentPane != null && this.contentPane.getComponentCount() > 0) {
            this.contentPane.add(this.programArgumentsPanel, BorderLayout.NORTH);
            this.contentPane.validate();
        }
    }

    public void removeProgramArgumentsPanel() {
        if (this.contentPane != null) {
            this.contentPane.remove(this.programArgumentsPanel);
            this.contentPane.validate();
        }
    }

    /**
     * remove all components
     */
    public void clearWindow() {
        this.contentPane.removeAll();
    }

    /**
     * Assign listener to Table model. Used for breakpoints, since that is the only
     * editable
     * column in the table. Since table model objects are transient (get a new one
     * with each
     * successful assemble), this method will simply keep the identity of the
     * listener then
     * add it as a listener each time a new table model object is created. Limit 1
     * listener.
     *
     * @param tml
     *     a {@link TableModelListener} object
     */
    public void registerTableModelListener(final TableModelListener tml) {
        this.tableModelListener = tml;
    }

    /**
     * Redisplay the addresses. This should only be done when address display base
     * is
     * modified (e.g. between base 16 hex and base 10 dec).
     */
    public void updateCodeAddresses() {
        if (this.contentPane.getComponentCount() == 0) {
            return; // ignore if no content to change
        }
        final int addressBase = this.executePane.getAddressDisplayBase();
        for (int i = 0; i < this.intAddresses.length; i++) {
            final var formattedAddress = NumberDisplayBaseChooser.formatUnsignedInteger(
                this.intAddresses[i],
                addressBase
            );
            this.table.getModel().setValueAt(formattedAddress, i, ColumnData.INSTRUCTION_ADDRESS_COLUMN.number);
        }
    }

    /**
     * Redisplay the basic statements. This should only be done when address or
     * value display base is
     * modified (e.g. between base 16 hex and base 10 dec).
     */
    public void updateBasicStatements() {
        if (this.contentPane.getComponentCount() == 0) {
            return; // ignore if no content to change
        }
        final var sourceStatementList = Globals.program.getMachineList();
        for (int i = 0; i < sourceStatementList.size(); i++) {
            // Loop has been extended to cover self-modifying code. If code at this memory
            // location has been
            // modified at runtime, construct a ProgramStatement from the current address
            // and binary code
            // then display its basic code. DPS 11-July-2013
            if (this.executeMods.get(i) == null) { // not modified, so use original logic.
                final ProgramStatement statement = sourceStatementList.get(i);
                this.table.getModel().setValueAt(
                    statement.getPrintableBasicAssemblyStatement(), i,
                    ColumnData.BASIC_INSTRUCTIONS_COLUMN.number
                );
            } else {
                try {
                    final ProgramStatement statement = new ProgramStatement(
                        BinaryUtils
                            .stringToInt((String) this.table.getModel().getValueAt(
                                i,
                                ColumnData.INSTRUCTION_CODE_COLUMN.number
                            )),
                        BinaryUtils
                            .stringToInt((String) this.table.getModel().getValueAt(
                                i,
                                ColumnData.INSTRUCTION_ADDRESS_COLUMN.number
                            ))
                    );
                    this.table.getModel().setValueAt(
                        statement.getPrintableBasicAssemblyStatement(), i,
                        ColumnData.BASIC_INSTRUCTIONS_COLUMN.number
                    );
                } catch (
                    final
                    NumberFormatException e) { // should never happen but just in case...
                    this.table.getModel().setValueAt("", i, ColumnData.BASIC_INSTRUCTIONS_COLUMN.number);
                }
            }
        }
    }

    /**
     * Called by RunResetAction to restore display of any table rows that were
     * overwritten due to self-modifying code feature.
     */
    public void resetModifiedSourceCode() {
        if (this.executeMods != null && !this.executeMods.isEmpty()) {
            for (final var modifiedCode : this.executeMods.values()) {
                this.tableModel.setValueAt(
                    modifiedCode.code(), modifiedCode.row(),
                    ColumnData.INSTRUCTION_CODE_COLUMN.number
                );
                this.tableModel.setValueAt(
                    modifiedCode.basic(), modifiedCode.row(),
                    ColumnData.BASIC_INSTRUCTIONS_COLUMN.number
                );
                this.tableModel.setValueAt(modifiedCode.source(), modifiedCode.row(), ColumnData.SOURCE_COLUMN.number);
            }
            this.executeMods.clear();
        }
    }

    /**
     * Return code address as an int, for the specified row of the table. This
     * should only
     * be used by the code renderer so I will not verify row.
     */
    int getIntCodeAddressAtRow(final int row) {
        return this.intAddresses[row];
    }

    /**
     * Returns number of breakpoints currently set.
     *
     * @return number of current breakpoints
     */
    public int getBreakpointCount() {
        int breakpointCount = 0;
        for (final Object[] data : this.data) {
            if ((Boolean) data[ColumnData.BREAKPOINT_COLUMN.number]) {
                breakpointCount++;
            }
        }
        return breakpointCount;
    }

    /**
     * Returns array of current breakpoints, each represented by a MIPS program
     * counter address.
     * These are stored in the BREAK_COLUMN of the table model.
     *
     * @return int array of breakpoints, sorted by PC address, or null if there are
     * none.
     */
    public int[] getSortedBreakPointsArray() {
        int breakpointCount = this.getBreakpointCount();
        if (breakpointCount == 0 || !this.breakpointsEnabled) { // added second condition 31-dec-09 DPS
            return new int[0];
        }
        final int[] breakpoints = new int[breakpointCount];
        breakpointCount = 0;
        for (int i = 0; i < this.data.length; i++) {
            if ((Boolean) this.data[i][ColumnData.BREAKPOINT_COLUMN.number]) {
                breakpoints[breakpointCount++] = this.intAddresses[i];
            }
        }
        Arrays.sort(breakpoints);
        return breakpoints;
    }

    /**
     * Clears all breakpoints that have been set since last assemble, and
     * updates the display of the breakpoint column.
     */
    public void clearAllBreakpoints() {
        for (int i = 0; i < this.tableModel.getRowCount(); i++) {
            if ((Boolean) this.data[i][ColumnData.BREAKPOINT_COLUMN.number]) {
                // must use this method to assure display updated and listener notified
                this.tableModel.setValueAt(false, i, ColumnData.BREAKPOINT_COLUMN.number);
            }
        }
        // Handles an obscure situation: if you click to set some breakpoints then
        // "immediately" clear them
        // all using the shortcut (CTRL-K), the last checkmark set is not removed even
        // though the breakpoint
        // is removed (tableModel.setValueAt(Boolean.FALSE, i, BREAK_COLUMN)) and all
        // the other checkmarks
        // are removed. The checkmark remains although if you subsequently run the
        // program it will blow
        // through because the data model cell really has been cleared (contains false).
        // Occurs only when
        // the last checked breakpoint check box still has the "focus". There is but one
        // renderer and editor
        // per column. Getting the renderer and setting it "setSelected(false)" will not
        // work. You have
        // to get the editor instead. (PS, 7 Aug 2006)
        (
            (JCheckBox) (
                (DefaultCellEditor) this.table.getCellEditor(
                    0,
                    ColumnData.BREAKPOINT_COLUMN.number
                )
            ).getComponent()
        ).setSelected(false);
    }

    /**
     * Highlights the source code line whose address matches the current
     * program counter value. This is used for stepping through code
     * execution and when reaching breakpoints.
     */
    public void highlightStepAtPC() {
        this.highlightStepAtAddress(Globals.REGISTER_FILE.getProgramCounter());
    }

    /**
     * Highlights the source code line whose address matches the given
     * text segment address.
     *
     * @param address
     *     Text segment address of instruction to be highlighted.
     */
    public void highlightStepAtAddress(final int address) {
        this.highlightAddress = address;
        // Scroll if necessary to assure highlighted row is visible.
        final int row;
        try {
            row = this.findRowForAddress(address);
        } catch (final IllegalArgumentException e) {
            return;
        }
        this.table.scrollRectToVisible(this.table.getCellRect(row, 0, true));
        // Trigger highlighting, which is done by the column's cell renderer.
        // IMPLEMENTATION NOTE: Pretty crude implementation; mark all rows
        // as changed so assure that the previously highlighted row is
        // unhighlighted. Would be better to keep track of previous row
        // then fire two events: one for it and one for the new row.
        this.table.tableChanged(new TableModelEvent(this.tableModel));
        // this.inDelaySlot = false;// Added 25 June 2007
    }

    /**
     * Get code highlighting status.
     *
     * @return true if code highlighting currently enabled, false otherwise.
     */
    public boolean getCodeHighlighting() {
        return this.codeHighlighting;
    }

    /**
     * Used to enable or disable source code highlighting. If true (normally while
     * stepping through execution) then MIPS statement at current program counter
     * is highlighted. The code column's cell renderer tests this variable.
     *
     * @param highlightSetting
     *     true to enable highlighting, false to disable.
     */
    public void setCodeHighlighting(final boolean highlightSetting) {
        this.codeHighlighting = highlightSetting;
    }

    /**
     * If any steps are highlighted, this erases the highlighting.
     */
    public void unhighlightAllSteps() {
        final boolean saved = this.codeHighlighting;
        this.codeHighlighting = false;
        this.table.tableChanged(new TableModelEvent(
            this.tableModel, 0, this.data.length - 1,
            ColumnData.BASIC_INSTRUCTIONS_COLUMN.number
        ));
        this.table.tableChanged(new TableModelEvent(
            this.tableModel, 0, this.data.length - 1,
            ColumnData.SOURCE_COLUMN.number
        ));
        this.codeHighlighting = saved;
    }

    /**
     * Scroll the viewport so the step (table row) at the given text segment address
     * is visible, vertically centered if possible, and selected.
     * Developed July 2007 for new feature that shows source code step where
     * label is defined when that label is clicked on in the Label Window.
     *
     * @param address
     *     text segment address of source code step.
     */

    void selectStepAtAddress(final int address) {
        final int addressRow;
        try {
            addressRow = this.findRowForAddress(address);
        } catch (final IllegalArgumentException e) {
            return;
        }
        // Scroll to assure desired row is centered in view port.
        final int addressSourceColumn = this.table.convertColumnIndexToView(ColumnData.SOURCE_COLUMN.number);
        final Rectangle sourceCell = this.table.getCellRect(addressRow, addressSourceColumn, true);
        final double cellHeight = sourceCell.getHeight();
        final double viewHeight = this.tableScroller.getViewport().getExtentSize().getHeight();
        final int numberOfVisibleRows = (int) (viewHeight / cellHeight);
        final int newViewPositionY = Math.max(
            (int) ((addressRow - ((double) numberOfVisibleRows / 2)) * cellHeight),
            0
        );
        this.tableScroller.getViewport().setViewPosition(new Point(0, newViewPositionY));
        // Select the source code cell for this row by generating a fake Mouse Pressed
        // event
        // and explicitly invoking the table's mouse listener.
        final MouseEvent fakeMouseEvent = new MouseEvent(
            this.table, MouseEvent.MOUSE_PRESSED,
            new Date().getTime(), MouseEvent.BUTTON1_DOWN_MASK,
            (int) sourceCell.getX() + 1,
            (int) sourceCell.getY() + 1, 1, false
        );
        final MouseListener[] mouseListeners = this.table.getMouseListeners();
        for (final MouseListener mouseListener : mouseListeners) {
            mouseListener.mousePressed(fakeMouseEvent);
        }
    }

    /**
     * Enable or disable all items in the Breakpoints column.
     */
    public void toggleBreakpoints() {
        // Already programmed to toggle by clicking on column header, so we'll create
        // a fake mouse event with coordinates on that header then generate the fake
        // event on its mouse listener.
        final Rectangle rect = ((MyTippedJTable) this.table).getRectForColumnIndex(ColumnData.BREAKPOINT_COLUMN.number);
        final MouseEvent fakeMouseEvent = new MouseEvent(
            this.table, MouseEvent.MOUSE_CLICKED,
            new Date().getTime(), MouseEvent.BUTTON1_DOWN_MASK,
            (int) rect.getX(), (int) rect.getY(), 1, false
        );
        final MouseListener[] mouseListeners = ((MyTippedJTable) this.table).tableHeader.getMouseListeners();
        for (final MouseListener mouseListener : mouseListeners) {
            mouseListener.mouseClicked(fakeMouseEvent);
        }

    }

    /**
     * Little convenience method to add this as observer of text segment
     */
    private void addAsTextSegmentObserver() {
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        try {
            Globals.MEMORY_INSTANCE.subscribe(
                this.processMemoryAccessNotice,
                memoryConfiguration.textBaseAddress,
                memoryConfiguration.dataSegmentBaseAddress
            );
        } catch (final AddressErrorException ignored) {
        }
    }

    private void updateTable(final int address, final int value) {
        final String strValue = BinaryUtils.intToHexString(value);
        // Translate the address into table model row and modify the values in that row
        // accordingly.
        final int row;
        try {
            row = this.findRowForAddress(address);
        } catch (final IllegalArgumentException e) {
            return; // do nothing if address modified is outside the range of original program.
        }
        ModifiedCode mc = this.executeMods.get(row);
        String strSource = TextSegmentWindow.modifiedCodeMarker;
        final String strBasic;
        if (mc == null) { // if not already modified
            // Not already modified and new code is same as original --> do nothing.
            if (this.tableModel.getValueAt(
                row,
                ColumnData.INSTRUCTION_CODE_COLUMN.number
            ).equals(strValue)) {
                return;
            }
            mc = new ModifiedCode(
                row,
                this.tableModel.getValueAt(row, ColumnData.INSTRUCTION_CODE_COLUMN.number),
                this.tableModel.getValueAt(row, ColumnData.BASIC_INSTRUCTIONS_COLUMN.number),
                this.tableModel.getValueAt(row, ColumnData.SOURCE_COLUMN.number)
            );
            this.executeMods.put(row, mc);
            // make a ProgramStatement and get basic code to display in BASIC_COLUMN
            strBasic = new ProgramStatement(value, address).getPrintableBasicAssemblyStatement();
        } else {
            // If restored to original value, restore the basic and source
            // This will be the case upon backstepping.
            if (mc.code().equals(strValue)) {
                strBasic = (String) mc.basic();
                strSource = (String) mc.source();
                // remove from executeMods since we are back to original
                this.executeMods.remove(row);
            } else {
                // make a ProgramStatement and get basic code to display in BASIC_COLUMN
                strBasic = new ProgramStatement(value, address).getPrintableBasicAssemblyStatement();
            }
        }
        // For the code column, we don't want to do the following:
        // tableModel.setValueAt(strValue, row, CODE_COLUMN)
        // because that method will write to memory using Memory.setRawWord() which will
        // trigger notification to observers, which brings us back to here!!! Infinite
        // indirect recursion results. Neither fun nor productive. So what happens is
        // this: (1) change to memory cell causes setValueAt() to be automatically be
        // called. (2) it updates the memory cell which in turn notifies us which
        // invokes
        // the update() method - the method we're in right now. All we need to do here
        // is
        // update the table model then notify the controller/view to update its display.
        this.data[row][ColumnData.INSTRUCTION_CODE_COLUMN.number] = strValue;
        this.tableModel.fireTableCellUpdated(row, ColumnData.INSTRUCTION_CODE_COLUMN.number);
        // The other columns do not present a problem since they are not editable by
        // user.
        this.tableModel.setValueAt(strBasic, row, ColumnData.BASIC_INSTRUCTIONS_COLUMN.number);
        this.tableModel.setValueAt(strSource, row, ColumnData.SOURCE_COLUMN.number);
        // Let's update the value displayed in the DataSegmentWindow too. But it only
        // observes memory while
        // the MIPS program is running, and even then only in timed or step mode. There
        // are good reasons
        // for that. So we'll pretend to be Memory observable and send it a fake memory
        // write update.
        try {
            this.executePane.dataSegment.processMemoryAccessNotice.accept(new MemoryAccessNotice(
                AccessNotice.AccessType.WRITE,
                address, DataTypes.WORD_SIZE,
                value
            ));
        } catch (final Exception e) {
            // Not sure if anything bad can happen in this sequence, but if anything does we
            // can let it go.
        }

    }

    /**
     * Little convenience method to remove this as observer of text segment
     */
    private void deleteAsTextSegmentObserver() {
        Globals.MEMORY_INSTANCE.deleteSubscriber(this.processMemoryAccessNotice);
    }

    /**
     * Helper method to find the table row corresponding to the given.
     * text segment address. This method is called by
     * a couple different public methods. Returns the table row
     * corresponding to this address.
     */
    private int findRowForAddress(final int address) throws IllegalArgumentException {
        final int addressRow;
        try {
            addressRow = this.addressRows.get(address);
        } catch (final NullPointerException e) {
            throw new IllegalArgumentException(); // address not found in map
            // return addressRow;// if address not in map, do nothing.
        }
        return addressRow;
    }

    private void updateRowHeight() {
        if (this.table == null) {
            return;
        }
        final var font = FONT_SETTINGS.getCurrentFont();
        final var height = this.getFontMetrics(font).getHeight();
        this.table.setRowHeight(height);
    }

    private enum ColumnData {
        BREAKPOINT_COLUMN(0, "Bkpt", breakpointToolTip),
        SOURCE_COLUMN(1, "Source code", sourceToolTip),
        BASIC_INSTRUCTIONS_COLUMN(2, "Basic instructions", basicInstructionsToolTip),
        INSTRUCTION_ADDRESS_COLUMN(3, "Instruction address", instructionAddressToolTip),
        INSTRUCTION_CODE_COLUMN(4, "Instruction opcode", instructionCodeToolTip),
        ;

        public final int number;
        public final @NotNull String name, description;

        ColumnData(
            final int number,
            final @NotNull String name,
            final @NotNull String description
        ) {
            this.number = number;
            this.name = name;
            this.description = description;
        }

        public static @Nullable ColumnData fromInt(final int number) {
            for (final ColumnData column : ColumnData.values()) {
                if (column.number == number) {
                    return column;
                }
            }
            return null;
        }
    }

    /**
     * Inner class to implement the Table model for this JTable.
     */
    static class TextTableModel extends AbstractTableModel {
        final Object[][] data;

        public TextTableModel(final Object[][] d) {
            this.data = d;
        }

        @Override
        public int getColumnCount() {
            return ColumnData.values().length;
        }

        @Override
        public int getRowCount() {
            return this.data.length;
        }

        @Override
        public String getColumnName(final int col) {
            return Objects.requireNonNull(ColumnData.fromInt(col)).name;
        }

        @Override
        public Object getValueAt(final int row, final int col) {
            return this.data[row][col];
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell. If we didn't implement this method,
         * then the break column would contain text ("true"/"false"),
         * rather than a check box.
         */
        @Override
        public Class<?> getColumnClass(final int c) {
            return this.getValueAt(0, c).getClass();
        }

        /*
         * Don't need to implement this method unless your table's
         * editable. Only Column #1, the Breakpoint, can be edited.
         */
        @Override
        public boolean isCellEditable(final int row, final int col) {
            // Note that the data/cell address is constant,
            // no matter where the cell appears onscreen.
            if (col == ColumnData.BREAKPOINT_COLUMN.number) return true;
            if (col != ColumnData.INSTRUCTION_CODE_COLUMN.number) return false;
            return BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED);
        }

        /**
         * Set cell contents in the table model. Overrides inherited empty method.
         * Straightforward process except for the Code column.
         */
        @Override
        public void setValueAt(final Object value, final int row, final int col) {
            if (col != ColumnData.INSTRUCTION_CODE_COLUMN.number) {
                this.data[row][col] = value;
                this.fireTableCellUpdated(row, col);
                return;
            }
            // Handle changes in the Code column.
            if (value.equals(this.data[row][col])) {
                return;
            }
            final int val;
            try {
                val = BinaryUtils.stringToInt((String) value);
            } catch (final NumberFormatException nfe) {
                LOGGER.error("NumberFormatException when decoding value from table model.", nfe);
                this.data[row][col] = "INVALID";
                this.fireTableCellUpdated(row, col);
                return;
            }
            // calculate address from row and column
            int address = 0;
            try {
                address =
                    BinaryUtils.stringToInt((String) this.data[row][ColumnData.INSTRUCTION_ADDRESS_COLUMN.number]);
            } catch (final NumberFormatException nfe) {
                // can't really happen since memory addresses are completely under
                // the control of my software.
                LOGGER.error("Unexpected NumberFormatException when decoding address from table model.", nfe);
            }
            // Assures that if changed during MIPS program execution, the update will
            // occur only between instructions.
            Globals.MEMORY_REGISTERS_LOCK.lock();
            try {
                try {
                    Globals.MEMORY_INSTANCE.setRawWord(address, val);
                }
                // somehow, user was able to display out-of-range address. Most likely to occur
                // between
                // stack base and Kernel.
                catch (final AddressErrorException exception) {
                    LOGGER.error("Address error exception when setting memory word in TextSegmentWindow.", exception);
                }
            } finally {
                Globals.MEMORY_REGISTERS_LOCK.unlock();
            } // end synchronized block
        }
    }

    private record ModifiedCode(Integer row, Object code, Object basic,
                                Object source) {
    }

    /*
     * a custom table cell renderer that we'll use to highlight the current line of
     * source code when executing using Step or breakpoint.
     */
    class CodeCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
            final JTable table, final Object value,
            final boolean isSelected, final boolean hasFocus,
            final int row, final int column
        ) {
            final Component cell = super.getTableCellRendererComponent(
                table, value,
                isSelected, hasFocus, row, column
            );
            // cell.setFont(tableCellFont);
            final TextSegmentWindow textSegment =
                TextSegmentWindow.this.executePane.textSegment;
            final boolean highlighting = textSegment.getCodeHighlighting();

            if (highlighting && textSegment.getIntCodeAddressAtRow(row) == TextSegmentWindow.this.highlightAddress) {
                final var style = HIGHLIGHTING_SETTINGS.getTextSegmentHighlightingStyle();
                cell.setBackground(style.background());
                cell.setForeground(style.foreground());
                cell.setFont(deriveFontFromStyle(FONT_SETTINGS.getCurrentFont(), style));
            } else {
                final var theme = Globals.EDITOR_THEME_SETTINGS.getCurrentTheme();
                cell.setBackground(theme.backgroundColor);
                cell.setForeground(theme.foregroundColor);
                cell.setFont(FONT_SETTINGS.getCurrentFont());
            }
            return cell;
        }

    }

    /**
     * Cell renderer for Breakpoint column. We can use this to enable/disable
     * breakpoint checkboxes with
     * a single action. This class was blatantly copied/pasted from
     * <a href="http://www.javakb.com/Uwe/Forum.aspx/java-gui/1451/Java-TableCellRenderer-for-a-boolean-checkbox-field">here</a>
     * Slightly customized. DPS 31-Dec-2009
     */
    class CheckBoxTableCellRenderer extends JCheckBox implements TableCellRenderer {

        Border noFocusBorder;
        Border focusBorder;

        public CheckBoxTableCellRenderer() {
            super();
            this.setContentAreaFilled(true);
            this.setBorderPainted(true);
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setVerticalAlignment(SwingConstants.CENTER);

            /* *********************************************
             * Use this if you want to add "instant" recognition of breakpoint changes
             * during simulation run. Currently, the simulator gets array of breakpoints
             * only when "Go" is selected. Thus the system does not respond to breakpoints
             * added/removed during unlimited/timed execution. In order for it to do so,
             * we need to be informed of such changes and the ItemListener below will do
             * this.
             * Then the item listener needs to inform the SimThread object so it can request
             * a fresh breakpoint array. That would make SimThread an observer.
             * Synchronization
             * will come into play in the SimThread class? It could get complicated, which
             * is why I'm dropping it for release 3.8. DPS 31-dec-2009
             *
             * addItemListener(
             * new ItemListener(){
             * public void itemStateChanged(ItemEvent e) {
             * String what = "state changed";
             * if (e.getStateChange()==ItemEvent.SELECTED) what = "selected";
             * if (e.getStateChange()==ItemEvent.DESELECTED) what = "deselected";
             * System.out.println("Item "+what);
             * }});
             *
             * For a different approach, see RunClearBreakpointsAction.java. This menu item
             * registers
             * as a TableModelListener by calling the TextSegmentWindow's
             * registerTableModelListener
             * method. Then it is notified when the table model changes, and this occurs
             * whenever
             * the user clicks on a breakpoint checkbox! Using this approach, the SimThread
             * registers
             * similarly. A "GUI guard" is not needed in SimThread because it extends
             * SwingWorker and
             * thus is only invoked when the IDE is present (never when running MARS in
             * command mode).
             *
             *****************************************************/
        }

        @Override
        public Component getTableCellRendererComponent(
            final JTable table, final Object value,
            final boolean isSelected,
            final boolean hasFocus,
            final int row, final int column
        ) {
            if (isSelected) {
                this.setForeground(table.getSelectionForeground());
                this.setBackground(table.getSelectionBackground());
            } else {
                this.setForeground(table.getForeground());
                this.setBackground(table.getBackground());
            }

            this.setEnabled(table.isEnabled() && TextSegmentWindow.this.breakpointsEnabled);
            this.setComponentOrientation(table.getComponentOrientation());

            if (hasFocus) {
                if (this.focusBorder == null) {
                    this.focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
                }
                this.setBorder(this.focusBorder);
            } else {
                if (this.noFocusBorder == null) {
                    if (this.focusBorder == null) {
                        this.focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
                    }
                    if (this.focusBorder != null) {
                        final Insets n = this.focusBorder.getBorderInsets(this);
                        this.noFocusBorder = new EmptyBorder(n);
                    }
                }
                this.setBorder(this.noFocusBorder);
            }
            this.setSelected(Boolean.TRUE.equals(value));

            return this;
        }
    }

    /**
     * JTable subclass to provide custom tool tips for each of the
     * text table column headers. From
     * <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/table.html">Sun's JTable tutorial</a>.
     */
    private class MyTippedJTable extends JTable {
        private JTableHeader tableHeader;

        MyTippedJTable(final TextTableModel m) {
            super(m);
        }

        @Override
        protected JTableHeader createDefaultTableHeader() {
            this.tableHeader = new TextTableHeader(this.columnModel);
            return this.tableHeader;
        }

        /**
         * Given the model index of a column header, will return rectangle
         * of displayed header (might be in a different position due to
         * column re-ordering).
         */
        public Rectangle getRectForColumnIndex(final int realIndex) {
            for (int i = 0; i < this.columnModel.getColumnCount(); i++) {
                if (this.columnModel.getColumn(i).getModelIndex() == realIndex) {
                    return this.tableHeader.getHeaderRect(i);
                }
            }
            return this.tableHeader.getHeaderRect(realIndex);
        }

        /**
         * Customized table header that will both display tool tip when
         * mouse hovers over each column, and also enable/disable breakpoints
         * when mouse is clicked on breakpoint column. Both are
         * customized based on the column under the mouse.
         */
        private class TextTableHeader extends JTableHeader {

            public TextTableHeader(final TableColumnModel cm) {
                super(cm);
                this.addMouseListener(new TextTableHeaderMouseListener());
            }

            @Override
            public String getToolTipText(final MouseEvent e) {
                final Point p = e.getPoint();
                final int index = this.columnModel.getColumnIndexAtX(p.x);
                final int realIndex = this.columnModel.getColumn(index).getModelIndex();
                return Objects.requireNonNull(ColumnData.fromInt(realIndex)).description;
            }

            /// When user clicks on beakpoint column header, breakpoints are
            /// toggled (enabled/disabled). DPS 31-Dec-2009
            private class TextTableHeaderMouseListener implements MouseListener {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Point p = e.getPoint();
                    final int index = TextTableHeader.this.columnModel.getColumnIndexAtX(p.x);
                    final int realIndex = TextTableHeader.this.columnModel.getColumn(index).getModelIndex();
                    if (realIndex == ColumnData.BREAKPOINT_COLUMN.number) {
                        final JCheckBox check =
                            (
                                (JCheckBox) ((DefaultCellEditor) TextTableHeader.this.table.getCellEditor(0, index))
                                    .getComponent()
                            );
                        TextSegmentWindow.this.breakpointsEnabled = !TextSegmentWindow.this.breakpointsEnabled;
                        check.setEnabled(TextSegmentWindow.this.breakpointsEnabled);
                        TextTableHeader.this.table.tableChanged(new TableModelEvent(
                            TextSegmentWindow.this.tableModel
                            , 0, TextSegmentWindow.this.data.length - 1, ColumnData.BREAKPOINT_COLUMN.number
                        ));
                    }
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                }
            }
        }
    }
}
