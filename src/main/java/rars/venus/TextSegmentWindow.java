package rars.venus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rars.Globals;
import rars.ProgramStatement;
import rars.Settings;
import rars.exceptions.AddressErrorException;
import rars.notices.*;
import rars.riscv.hardware.Memory;
import rars.riscv.hardware.RegisterFile;
import rars.simulator.Simulator;
import rars.util.Binary;
import rars.util.EditorFont;
import rars.util.SimpleSubscriber;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.concurrent.Flow;

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
public class TextSegmentWindow extends JInternalFrame implements SimpleSubscriber<Notice> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int PROGRAM_ARGUMENT_TEXTFIELD_COLUMNS = 40;
    private static final String[] columnNames = {"Bkpt", "Address", "Code", "Basic", "Source"};
    private static final int BREAK_COLUMN = 0;
    private static final int ADDRESS_COLUMN = 1;
    private static final int CODE_COLUMN = 2;
    private static final int BASIC_COLUMN = 3;
    private static final int SOURCE_COLUMN = 4;
    // The following is displayed in the Basic and Source columns if existing code
    // is overwritten using self-modifying code feature
    private static final String modifiedCodeMarker = " ------ ";
    private final JPanel programArgumentsPanel; // DPS 17-July-2008
    private final JTextField programArgumentsTextField; // DPS 17-July-2008
    // source.
    private final Container contentPane;
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
    private int[] intAddresses; // index is table model row, second is text address
    private Hashtable<Integer, Integer> addressRows; // first is text address, second is table model row
    private Hashtable<Integer, ModifiedCode> executeMods; // first is table model row, second is original code, basic,
    private TextTableModel tableModel;
    private boolean codeHighlighting;
    private boolean breakpointsEnabled; // Added 31 Dec 2009
    private int highlightAddress;
    private TableModelListener tableModelListener;
    private Flow.Subscription subscription;

    /**
     * Constructor, sets up a new JInternalFrame.
     */
    public TextSegmentWindow() {
        super("Text Segment", true, false, true, true);
        Simulator.getInstance().subscribe(this);
        Globals.getSettings().subscribe(this);
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

    //////////// Support for program arguments added DPS 17-July-2008 //////////////

    /**
     * Method to be called once the user compiles the program.
     * Should convert the lines of code over to the table rows and columns.
     */
    public void setupTable() {
        final int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
        this.codeHighlighting = true;
        this.breakpointsEnabled = true;
        final ArrayList<ProgramStatement> sourceStatementList = Globals.program.getMachineList();
        this.data = new Object[sourceStatementList.size()][TextSegmentWindow.columnNames.length];
        this.intAddresses = new int[this.data.length];
        this.addressRows = new Hashtable<>(this.data.length);
        this.executeMods = new Hashtable<>(this.data.length);
        // Get highest source line number to determine #leading spaces so line numbers
        // will vertically align
        // In multi-file situation, this will not necessarily be the last line b/c
        // sourceStatementList contains
        // source lines from all files. DPS 3-Oct-10
        int maxSourceLineNumber = 0;
        for (int i = sourceStatementList.size() - 1; i >= 0; i--) {
            final ProgramStatement statement = sourceStatementList.get(i);
            if (statement.getSourceLine() > maxSourceLineNumber) {
                maxSourceLineNumber = statement.getSourceLine();
            }
        }
        final int sourceLineDigits = ("" + maxSourceLineNumber).length();
        int leadingSpaces;
        int lastLine = -1;
        for (int i = 0; i < sourceStatementList.size(); i++) {
            final ProgramStatement statement = sourceStatementList.get(i);
            this.intAddresses[i] = statement.getAddress();
            this.addressRows.put(this.intAddresses[i], i);
            this.data[i][TextSegmentWindow.BREAK_COLUMN] = false;
            this.data[i][TextSegmentWindow.ADDRESS_COLUMN] =
                    NumberDisplayBaseChooser.formatUnsignedInteger(statement.getAddress(),
                            addressBase);
            this.data[i][TextSegmentWindow.CODE_COLUMN] =
                    NumberDisplayBaseChooser.formatNumber(statement.getBinaryStatement(), 16);
            this.data[i][TextSegmentWindow.BASIC_COLUMN] = statement.getPrintableBasicAssemblyStatement();
            String sourceString = "";
            if (!statement.getSource().isEmpty()) {
                leadingSpaces = sourceLineDigits - ("" + statement.getSourceLine()).length();
                String lineNumber = "          ".substring(0, leadingSpaces)
                        + statement.getSourceLine() + ": ";
                if (statement.getSourceLine() == lastLine)
                    lineNumber = "          ".substring(0, sourceLineDigits) + "  ";
                sourceString = lineNumber
                        + EditorFont.substituteSpacesForTabs(statement.getSource());
            }
            this.data[i][TextSegmentWindow.SOURCE_COLUMN] = sourceString;
            lastLine = statement.getSourceLine();
        }
        this.contentPane.removeAll();
        this.tableModel = new TextTableModel(this.data);
        if (this.tableModelListener != null) {
            this.tableModel.addTableModelListener(this.tableModelListener);
            this.tableModel.fireTableDataChanged();// initialize listener
        }
        this.table = new MyTippedJTable(this.tableModel);
        this.updateRowHeight();

        // prevents cells in row from being highlighted when user clicks on breakpoint
        // checkbox
        this.table.setRowSelectionAllowed(false);

        this.table.getColumnModel().getColumn(TextSegmentWindow.BREAK_COLUMN).setPreferredWidth(40);
        this.table.getColumnModel().getColumn(TextSegmentWindow.ADDRESS_COLUMN).setPreferredWidth(80);
        this.table.getColumnModel().getColumn(TextSegmentWindow.CODE_COLUMN).setPreferredWidth(80);
        this.table.getColumnModel().getColumn(TextSegmentWindow.BASIC_COLUMN).setPreferredWidth(160);
        this.table.getColumnModel().getColumn(TextSegmentWindow.SOURCE_COLUMN).setPreferredWidth(280);

        final CodeCellRenderer codeStepHighlighter = new CodeCellRenderer();
        this.table.getColumnModel().getColumn(TextSegmentWindow.BASIC_COLUMN).setCellRenderer(codeStepHighlighter);
        this.table.getColumnModel().getColumn(TextSegmentWindow.SOURCE_COLUMN).setCellRenderer(codeStepHighlighter);
        // to render String right-justified in mono font
        this.table.getColumnModel().getColumn(TextSegmentWindow.ADDRESS_COLUMN).setCellRenderer(new MonoRightCellRenderer());
        this.table.getColumnModel().getColumn(TextSegmentWindow.CODE_COLUMN).setCellRenderer(new MachineCodeCellRenderer());
        this.table.getColumnModel().getColumn(TextSegmentWindow.BREAK_COLUMN).setCellRenderer(new CheckBoxTableCellRenderer());
        this.reorderColumns(); // Re-order columns according to current preference...
        // Add listener to catch column re-ordering for updating settings.
        this.table.getColumnModel().addColumnModelListener(new MyTableColumnMovingListener());

        this.tableScroller = new JScrollPane(this.table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        this.contentPane.add(this.tableScroller);
        if (Globals.getSettings().getBooleanSetting(Settings.Bool.PROGRAM_ARGUMENTS)) {
            this.addProgramArgumentsPanel();
        }

        this.deleteAsTextSegmentObserver();
        if (Globals.getSettings().getBooleanSetting(Settings.Bool.SELF_MODIFYING_CODE_ENABLED)) {
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

    /**
     * <p>addProgramArgumentsPanel.</p>
     */
    public void addProgramArgumentsPanel() {
        // Don't add it if text segment window blank (file closed or no assemble yet)
        if (this.contentPane != null && this.contentPane.getComponentCount() > 0) {
            this.contentPane.add(this.programArgumentsPanel, BorderLayout.NORTH);
            this.contentPane.validate();
        }
    }
    //
    ///////////////////////// end program arguments section ////////////////////////

    /**
     * <p>removeProgramArgumentsPanel.</p>
     */
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
     * @param tml a {@link TableModelListener} object
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
        if (this.contentPane.getComponentCount() == 0)
            return; // ignore if no content to change
        final int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
        int address;
        String formattedAddress;
        for (int i = 0; i < this.intAddresses.length; i++) {
            formattedAddress = NumberDisplayBaseChooser.formatUnsignedInteger(this.intAddresses[i], addressBase);
            this.table.getModel().setValueAt(formattedAddress, i, TextSegmentWindow.ADDRESS_COLUMN);
        }
    }

    /**
     * Redisplay the basic statements. This should only be done when address or
     * second display base is
     * modified (e.g. between base 16 hex and base 10 dec).
     */
    public void updateBasicStatements() {
        if (this.contentPane.getComponentCount() == 0)
            return; // ignore if no content to change
        final ArrayList<ProgramStatement> sourceStatementList = Globals.program.getMachineList();
        for (int i = 0; i < sourceStatementList.size(); i++) {
            // Loop has been extended to cover self-modifying code. If code at this memory
            // location has been
            // modified at runtime, construct a ProgramStatement from the current address
            // and binary code
            // then display its basic code. DPS 11-July-2013
            if (this.executeMods.get(i) == null) { // not modified, so use original logic.
                final ProgramStatement statement = sourceStatementList.get(i);
                this.table.getModel().setValueAt(statement.getPrintableBasicAssemblyStatement(), i,
                        TextSegmentWindow.BASIC_COLUMN);
            } else {
                try {
                    final ProgramStatement statement = new ProgramStatement(
                            Binary
                                    .stringToInt((String) this.table.getModel().getValueAt(i,
                                            TextSegmentWindow.CODE_COLUMN)),
                            Binary
                                    .stringToInt((String) this.table.getModel().getValueAt(i,
                                            TextSegmentWindow.ADDRESS_COLUMN)));
                    this.table.getModel().setValueAt(statement.getPrintableBasicAssemblyStatement(), i,
                            TextSegmentWindow.BASIC_COLUMN);
                } catch (
                        final
                        NumberFormatException e) { // should never happen but just in case...
                    this.table.getModel().setValueAt("", i, TextSegmentWindow.BASIC_COLUMN);
                }
            }
        }
    }

    @Override
    public void onSubscribe(final Flow.Subscription subscription) {
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(final Notice notice) {
        switch (notice) {
            case final SimulatorNotice ignored -> {
                // Simulated MIPS execution starts. Respond to text segment changes only if
                // self-modifying code
                // enabled. I commented out conditions that would further limit it to running in
                // timed or stepped mode.
                // Seems reasonable for text segment display to be accurate in cases where
                // existing code is overwritten
                // even when running at unlimited speed. DPS 10-July-2013
                this.deleteAsTextSegmentObserver();
                if (Globals.getSettings().getBooleanSetting(Settings.Bool.SELF_MODIFYING_CODE_ENABLED)) { // &&
                    // (notice.getRunSpeed()
                    // !=
                    // RunSpeedPanel.UNLIMITED_SPEED
                    // ||
                    // notice.getMaxSteps()==1))
                    // {
                    this.addAsTextSegmentObserver();
                }
            }
            case final SettingsNotice ignored -> {
                this.deleteAsTextSegmentObserver();
                if (Globals.getSettings().getBooleanSetting(Settings.Bool.SELF_MODIFYING_CODE_ENABLED)) {
                    this.addAsTextSegmentObserver();
                }
                this.updateRowHeight();
            }
            case final MemoryAccessNotice m -> {

                // NOTE: observable != Memory.getInstance() because Memory class delegates
                // notification duty.
                // This will occur only if running program has written to text segment
                // (self-modifying code)
                if (m.getAccessType() == AccessNotice.AccessType.WRITE) {
                    final int address = m.getAddress();
                    final int value = m.getValue();
                    final String strValue = Binary.intToHexString(m.getValue());
                    final String strBasic;
                    String strSource = TextSegmentWindow.modifiedCodeMarker;
                    // Translate the address into table model row and modify the values in that row
                    // accordingly.
                    final int row;
                    try {
                        row = this.findRowForAddress(address);
                    } catch (final IllegalArgumentException e) {
                        return; // do nothing if address modified is outside the range of original program.
                    }
                    ModifiedCode mc = this.executeMods.get(row);
                    if (mc == null) { // if not already modified
                        // Not already modified and new code is same as original --> do nothing.
                        if (this.tableModel.getValueAt(row, TextSegmentWindow.CODE_COLUMN).equals(strValue)) {
                            return;
                        }
                        mc = new ModifiedCode(
                                row,
                                this.tableModel.getValueAt(row, TextSegmentWindow.CODE_COLUMN),
                                this.tableModel.getValueAt(row, TextSegmentWindow.BASIC_COLUMN),
                                this.tableModel.getValueAt(row, TextSegmentWindow.SOURCE_COLUMN));
                        this.executeMods.put(row, mc);
                        // make a ProgramStatement and get basic code to display in BASIC_COLUMN
                        strBasic = new ProgramStatement(value, address).getPrintableBasicAssemblyStatement();
                    } else {
                        // If restored to original second, restore the basic and source
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
                    this.data[row][TextSegmentWindow.CODE_COLUMN] = strValue;
                    this.tableModel.fireTableCellUpdated(row, TextSegmentWindow.CODE_COLUMN);
                    // The other columns do not present a problem since they are not editable by
                    // user.
                    this.tableModel.setValueAt(strBasic, row, TextSegmentWindow.BASIC_COLUMN);
                    this.tableModel.setValueAt(strSource, row, TextSegmentWindow.SOURCE_COLUMN);
                    // Let's update the second displayed in the DataSegmentWindow too. But it only
                    // observes memory while
                    // the MIPS program is running, and even then only in timed or step mode. There
                    // are good reasons
                    // for that. So we'll pretend to be Memory observable and send it a fake memory
                    // write update.
                    try {
                        Globals.getGui().getMainPane().getExecutePane().getDataSegmentWindow()
                                .onNext(new MemoryAccessNotice(AccessNotice.AccessType.WRITE, address, value));
                    } catch (final Exception e) {
                        // Not sure if anything bad can happen in this sequence, but if anything does we
                        // can let it go.
                    }
                }
            }
            default -> {
            }
        }
        this.subscription.request(1);
    }


    /**
     * Called by RunResetAction to restore display of any table rows that were
     * overwritten due to self-modifying code feature.
     */
    public void resetModifiedSourceCode() {
        if (this.executeMods != null && !this.executeMods.isEmpty()) {
            for (final Enumeration<ModifiedCode> elements = this.executeMods.elements(); elements.hasMoreElements(); ) {
                final ModifiedCode mc = elements.nextElement();
                this.tableModel.setValueAt(mc.code(), mc.row(), TextSegmentWindow.CODE_COLUMN);
                this.tableModel.setValueAt(mc.basic(), mc.row(), TextSegmentWindow.BASIC_COLUMN);
                this.tableModel.setValueAt(mc.source(), mc.row(), TextSegmentWindow.SOURCE_COLUMN);
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
            if ((Boolean) data[TextSegmentWindow.BREAK_COLUMN]) {
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
            if ((Boolean) this.data[i][TextSegmentWindow.BREAK_COLUMN]) {
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
            if ((Boolean) this.data[i][TextSegmentWindow.BREAK_COLUMN]) {
                // must use this method to assure display updated and listener notified
                this.tableModel.setValueAt(false, i, TextSegmentWindow.BREAK_COLUMN);
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
        ((JCheckBox) ((DefaultCellEditor) this.table.getCellEditor(0, TextSegmentWindow.BREAK_COLUMN)).getComponent()).setSelected(false);
    }

    /**
     * Highlights the source code line whose address matches the current
     * program counter second. This is used for stepping through code
     * execution and when reaching breakpoints.
     */
    public void highlightStepAtPC() {
        this.highlightStepAtAddress(RegisterFile.getProgramCounter());
    }

    /**
     * Highlights the source code line whose address matches the given
     * text segment address.
     *
     * @param address Text segment address of instruction to be highlighted.
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
     * @param highlightSetting true to enable highlighting, false to disable.
     */
    public void setCodeHighlighting(final boolean highlightSetting) {
        this.codeHighlighting = highlightSetting;
    }

    /**
     * If any steps are highlighted, this erases the highlighting.
     */
    public void unhighlightAllSteps() {
        final boolean saved = this.getCodeHighlighting();
        this.setCodeHighlighting(false);
        this.table.tableChanged(new TableModelEvent(this.tableModel, 0, this.data.length - 1,
                TextSegmentWindow.BASIC_COLUMN));
        this.table.tableChanged(new TableModelEvent(this.tableModel, 0, this.data.length - 1,
                TextSegmentWindow.SOURCE_COLUMN));
        this.setCodeHighlighting(saved);
    }

    /**
     * Scroll the viewport so the step (table row) at the given text segment address
     * is visible, vertically centered if possible, and selected.
     * Developed July 2007 for new feature that shows source code step where
     * label is defined when that label is clicked on in the Label Window.
     *
     * @param address text segment address of source code step.
     */

    void selectStepAtAddress(final int address) {
        final int addressRow;
        try {
            addressRow = this.findRowForAddress(address);
        } catch (final IllegalArgumentException e) {
            return;
        }
        // Scroll to assure desired row is centered in view port.
        final int addressSourceColumn = this.table.convertColumnIndexToView(TextSegmentWindow.SOURCE_COLUMN);
        final Rectangle sourceCell = this.table.getCellRect(addressRow, addressSourceColumn, true);
        final double cellHeight = sourceCell.getHeight();
        final double viewHeight = this.tableScroller.getViewport().getExtentSize().getHeight();
        final int numberOfVisibleRows = (int) (viewHeight / cellHeight);
        final int newViewPositionY = Math.max((int) ((addressRow - ((double) numberOfVisibleRows / 2)) * cellHeight),
                0);
        this.tableScroller.getViewport().setViewPosition(new Point(0, newViewPositionY));
        // Select the source code cell for this row by generating a fake Mouse Pressed
        // event
        // and explicitly invoking the table's mouse listener.
        final MouseEvent fakeMouseEvent = new MouseEvent(this.table, MouseEvent.MOUSE_PRESSED,
                new Date().getTime(), MouseEvent.BUTTON1_DOWN_MASK,
                (int) sourceCell.getX() + 1,
                (int) sourceCell.getY() + 1, 1, false);
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
        final Rectangle rect = ((MyTippedJTable) this.table).getRectForColumnIndex(TextSegmentWindow.BREAK_COLUMN);
        final MouseEvent fakeMouseEvent = new MouseEvent(this.table, MouseEvent.MOUSE_CLICKED,
                new Date().getTime(), MouseEvent.BUTTON1_DOWN_MASK,
                (int) rect.getX(), (int) rect.getY(), 1, false);
        final MouseListener[] mouseListeners = ((MyTippedJTable) this.table).tableHeader.getMouseListeners();
        for (final MouseListener mouseListener : mouseListeners) {
            mouseListener.mouseClicked(fakeMouseEvent);
        }

    }

    /*
     * Little convenience method to add this as observer of text segment
     */
    private void addAsTextSegmentObserver() {
        try {
            Memory.getInstance().subscribe(this, Memory.textBaseAddress, Memory.dataSegmentBaseAddress);
        } catch (final AddressErrorException ignored) {
        }
    }

    /*
     * Little convenience method to remove this as observer of text segment
     */
    private void deleteAsTextSegmentObserver() {
        Memory.getInstance().deleteSubscriber(this);
    }

    /*
     * Re-order the Text segment columns according to saved preferences.
     */

    private void reorderColumns() {
        final TableColumnModel oldtcm = this.table.getColumnModel();
        final TableColumnModel newtcm = new DefaultTableColumnModel();
        final int[] savedColumnOrder = Globals.getSettings().getTextColumnOrder();
        // Apply ordering only if correct number of columns.
        if (savedColumnOrder.length == this.table.getColumnCount()) {
            for (final int columnOrder : savedColumnOrder)
                newtcm.addColumn(oldtcm.getColumn(columnOrder));
            this.table.setColumnModel(newtcm);
        }
    }

    /*
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
        final Font[] possibleFonts = {
                Globals.getSettings().getFontByPosition(Settings.TEXTSEGMENT_HIGHLIGHT_FONT),
                Globals.getSettings().getFontByPosition(Settings.EVEN_ROW_FONT),
                Globals.getSettings().getFontByPosition(Settings.ODD_ROW_FONT),
        };
        int maxHeight = 0;
        for (final Font possibleFont : possibleFonts) {
            final int height = this.getFontMetrics(possibleFont).getHeight();
            if (height > maxHeight) {
                maxHeight = height;
            }
        }
        this.table.setRowHeight(maxHeight);
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
            return TextSegmentWindow.columnNames.length;
        }

        @Override
        public int getRowCount() {
            return this.data.length;
        }

        @Override
        public String getColumnName(final int col) {
            return TextSegmentWindow.columnNames[col];
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
            return col == TextSegmentWindow.BREAK_COLUMN || (col == TextSegmentWindow.CODE_COLUMN &&
                    Globals.getSettings().getBooleanSetting(Settings.Bool.SELF_MODIFYING_CODE_ENABLED));
        }

        /**
         * Set cell contents in the table model. Overrides inherited empty method.
         * Straightforward process except for the Code column.
         */
        @Override
        public void setValueAt(final Object value, final int row, final int col) {
            if (col != TextSegmentWindow.CODE_COLUMN) {
                this.data[row][col] = value;
                this.fireTableCellUpdated(row, col);
                return;
            }
            // Handle changes in the Code column.
            final int val;
            int address = 0;
            if (value.equals(this.data[row][col]))
                return;
            try {
                val = Binary.stringToInt((String) value);
            } catch (final NumberFormatException nfe) {
                this.data[row][col] = "INVALID";
                this.fireTableCellUpdated(row, col);
                return;
            }
            // calculate address from row and column
            try {
                address = Binary.stringToInt((String) this.data[row][TextSegmentWindow.ADDRESS_COLUMN]);
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
                // stack base and Kernel.
                catch (final AddressErrorException ignored) {
                }
            } finally {
                Globals.memoryAndRegistersLock.unlock();
            } // end synchronized block
        }

        private void printDebugData() {
            final int numRows = this.getRowCount();
            final int numCols = this.getColumnCount();

            for (int i = 0; i < numRows; i++) {
                TextSegmentWindow.LOGGER.debug("    row {}:", i);
                for (int j = 0; j < numCols; j++) {
                    TextSegmentWindow.LOGGER.debug("  {}", this.data[i][j]);
                }
                TextSegmentWindow.LOGGER.debug('\n');
            }
            TextSegmentWindow.LOGGER.debug("--------------------------");
        }
    }

    private record ModifiedCode(Integer row, Object code, Object basic,
                                Object source) {
    }

    /*
     * Cell renderer for Machine Code column. Alternates background color by row but
     * otherwise is
     * same as MonoRightCellRenderer.
     */
    static class MachineCodeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                       final boolean isSelected, final boolean hasFocus,
                                                       final int row, final int column) {
            final JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);
            cell.setFont(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT);
            cell.setHorizontalAlignment(SwingConstants.RIGHT);
            if (row % 2 == 0) {
                cell.setBackground(Globals.getSettings().getColorSettingByPosition(Settings.EVEN_ROW_BACKGROUND));
                cell.setForeground(Globals.getSettings().getColorSettingByPosition(Settings.EVEN_ROW_FOREGROUND));
            } else {
                cell.setBackground(Globals.getSettings().getColorSettingByPosition(Settings.ODD_ROW_BACKGROUND));
                cell.setForeground(Globals.getSettings().getColorSettingByPosition(Settings.ODD_ROW_FOREGROUND));
            }
            return cell;
        }
    }

    /*
     * a custom table cell renderer that we'll use to highlight the current line of
     * source code when executing using Step or breakpoint.
     */
    class CodeCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                       final boolean isSelected, final boolean hasFocus,
                                                       final int row, final int column) {
            final Component cell = super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);
            // cell.setFont(tableCellFont);
            final TextSegmentWindow textSegment =
                    Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow();
            final Settings settings = Globals.getSettings();
            final boolean highlighting = textSegment.getCodeHighlighting();

            if (highlighting && textSegment.getIntCodeAddressAtRow(row) == TextSegmentWindow.this.highlightAddress) {
                cell.setBackground(settings.getColorSettingByPosition(Settings.TEXTSEGMENT_HIGHLIGHT_BACKGROUND));
                cell.setForeground(settings.getColorSettingByPosition(Settings.TEXTSEGMENT_HIGHLIGHT_FOREGROUND));
                cell.setFont(settings.getFontByPosition(Settings.TEXTSEGMENT_HIGHLIGHT_FONT));
            } else if (row % 2 == 0) {
                cell.setBackground(settings.getColorSettingByPosition(Settings.EVEN_ROW_BACKGROUND));
                cell.setForeground(settings.getColorSettingByPosition(Settings.EVEN_ROW_FOREGROUND));
                cell.setFont(settings.getFontByPosition(Settings.EVEN_ROW_FONT));
            } else {
                cell.setBackground(settings.getColorSettingByPosition(Settings.ODD_ROW_BACKGROUND));
                cell.setForeground(settings.getColorSettingByPosition(Settings.ODD_ROW_FOREGROUND));
                cell.setFont(settings.getFontByPosition(Settings.ODD_ROW_FONT));
            }
            return cell;
        }

    }

    /*
     * Cell renderer for Breakpoint column. We can use this to enable/disable
     * breakpoint checkboxes with
     * a single action. This class blatantly copied/pasted from
     * http://www.javakb.com/Uwe/Forum.aspx/java-gui/1451/Java-TableCellRenderer-for
     * -a-boolean-checkbox-field
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
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row, final int column) {

            if (table == null) {
                // ???
            } else {
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
            }
            return this;
        }
    }

    /// ////////////////////////////////////////////////////////////////
    //
    // JTable subclass to provide custom tool tips for each of the
    // text table column headers. From Sun's JTable tutorial.
    // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
    //
    private class MyTippedJTable extends JTable {
        private final String[] columnToolTips = {
                /* break */ "If checked, will set an execution breakpoint. Click header to disable/enable breakpoints",
                /* address */ "Text segment address of binary instruction code",
                /* code */ "32-bit binary RISCV instruction",
                /* basic */ "Basic assembler instruction",
                /* source */ "Source code line"
        };
        private JTableHeader tableHeader;

        MyTippedJTable(final TextTableModel m) {
            super(m);
        }

        // Implement table header tool tips.
        @Override
        protected JTableHeader createDefaultTableHeader() {
            this.tableHeader = new TextTableHeader(this.columnModel);
            return this.tableHeader;
            /*
             * new JTableHeader(columnModel) {
             * public String getToolTipText(MouseEvent e) {
             * String tip = null;
             * java.awt.Point p = e.getPoint();
             * int index = columnModel.getColumnIndexAtX(p.x);
             * int realIndex = columnModel.getColumn(index).getModelIndex();
             * return columnToolTips[realIndex];
             * }
             * };
             */
        }

        // Given the model index of a column header, will return rectangle
        // rectangle of displayed header (may be in different position due to
        // column re-ordering).
        public Rectangle getRectForColumnIndex(final int realIndex) {
            for (int i = 0; i < this.columnModel.getColumnCount(); i++) {
                if (this.columnModel.getColumn(i).getModelIndex() == realIndex) {
                    return this.tableHeader.getHeaderRect(i);
                }
            }
            return this.tableHeader.getHeaderRect(realIndex);
        }

        /// /////////////////////////////////////////////////////////////
        //
        // Customized table header that will both display tool tip when
        // mouse hovers over each column, and also enable/disable breakpoints
        // when mouse is clicked on breakpoint column. Both are
        // customized based on the column under the mouse.

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
                return MyTippedJTable.this.columnToolTips[realIndex];
            }

            /// //////////////////////////////////////////////////////////////////
            // When user clicks on beakpoint column header, breakpoints are
            // toggled (enabled/disabled). DPS 31-Dec-2009
            private class TextTableHeaderMouseListener implements MouseListener {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Point p = e.getPoint();
                    final int index = TextTableHeader.this.columnModel.getColumnIndexAtX(p.x);
                    final int realIndex = TextTableHeader.this.columnModel.getColumn(index).getModelIndex();
                    if (realIndex == TextSegmentWindow.BREAK_COLUMN) {
                        final JCheckBox check =
                                ((JCheckBox) ((DefaultCellEditor) TextTableHeader.this.table.getCellEditor(0, index))
                                        .getComponent());
                        TextSegmentWindow.this.breakpointsEnabled = !TextSegmentWindow.this.breakpointsEnabled;
                        check.setEnabled(TextSegmentWindow.this.breakpointsEnabled);
                        TextTableHeader.this.table.tableChanged(new TableModelEvent(TextSegmentWindow.this.tableModel
                                , 0, TextSegmentWindow.this.data.length - 1, TextSegmentWindow.BREAK_COLUMN));
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

    /*
     * Will capture movement of text columns. This info goes into persistent store.
     */
    private class MyTableColumnMovingListener implements TableColumnModelListener {
        // Don't care about these events but no adapter provided so...
        @Override
        public void columnAdded(final TableColumnModelEvent e) {
        }

        @Override
        public void columnRemoved(final TableColumnModelEvent e) {
        }

        @Override
        public void columnMarginChanged(final ChangeEvent e) {
        }

        @Override
        public void columnSelectionChanged(final ListSelectionEvent e) {
        }

        // When column moves, save the new column order.
        @Override
        public void columnMoved(final TableColumnModelEvent e) {
            final int[] columnOrder = new int[TextSegmentWindow.this.table.getColumnCount()];
            for (int i = 0; i < columnOrder.length; i++) {
                columnOrder[i] = TextSegmentWindow.this.table.getColumnModel().getColumn(i).getModelIndex();
            }
            // If movement is slow, this event may fire multiple times w/o
            // actually changing the column order. If new column order is
            // same as previous, do not save changes to persistent store.
            final int[] oldOrder = Globals.getSettings().getTextColumnOrder();
            for (int i = 0; i < columnOrder.length; i++) {
                if (oldOrder[i] != columnOrder[i]) {
                    Globals.getSettings().setTextColumnOrder(columnOrder);
                    break;
                }
            }
        }
    }

}
