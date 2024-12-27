package rars.venus.registers;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.notices.AccessNotice;
import rars.notices.Notice;
import rars.notices.RegisterAccessNotice;
import rars.notices.SimulatorNotice;
import rars.riscv.hardware.Register;
import rars.settings.BoolSetting;
import rars.settings.FontSettings;
import rars.util.Binary;
import rars.util.SimpleSubscriber;
import rars.venus.NumberDisplayBaseChooser;
import rars.venus.run.RunSpeedPanel;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.Flow;

import static rars.settings.FontSettings.FONT_SETTINGS;
import static rars.settings.Settings.BOOL_SETTINGS;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
 * Sets up a window to display registers in the UI.
 *
 * @author Sanderson, Bumgarner
 */
public abstract class RegisterBlockWindow extends JPanel implements SimpleSubscriber<Notice> {
    private static final int NUMBER_COLUMN = 0;
    private static final int NAME_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;
    private static final int NUMBER_SIZE = 45;
    private static final int NAME_SIZE = 80;
    private static final int VALUE_SIZE = 160;
    private final @NotNull JTable table;
    private final Register[] registers;
    private Flow.Subscription subscription;

    /**
     * Constructor which sets up a fresh window with a table that contains the
     * register values.
     *
     * @param registers            an array of {@link Register} objects
     * @param registerDescriptions an array of {@link java.lang.String} objects
     * @param valueTip             a {@link java.lang.String} object
     */
    RegisterBlockWindow(final Register[] registers, final String[] registerDescriptions, final String valueTip) {
        this.registers = registers;
        this.table = new MyTippedJTable(new RegTableModel(this.setupWindow()), registerDescriptions,
            new String[]{"Each register has a tool tip describing its usage convention",
                "Corresponding register number", valueTip}) {
        };
        FONT_SETTINGS.addChangeListener(this::updateRowHeight, true);
        final var columnModel = this.table.getColumnModel();

        final var nameColumn = columnModel.getColumn(RegisterBlockWindow.NAME_COLUMN);
        nameColumn.setMinWidth(RegisterBlockWindow.NAME_SIZE);
        nameColumn.setMaxWidth(RegisterBlockWindow.NAME_SIZE);
        nameColumn.setCellRenderer(new RegisterCellRenderer(SwingConstants.LEFT, table));

        final var numberColumn = columnModel.getColumn(RegisterBlockWindow.NUMBER_COLUMN);
        numberColumn.setMinWidth(RegisterBlockWindow.NUMBER_SIZE);
        numberColumn.setMaxWidth(RegisterBlockWindow.NUMBER_SIZE);
        // Display register values (String-ified) right-justified in mono font
        numberColumn.setCellRenderer(new RegisterCellRenderer(SwingConstants.RIGHT, table));

        final var valueColumn = columnModel.getColumn(RegisterBlockWindow.VALUE_COLUMN);
        valueColumn.setMinWidth(RegisterBlockWindow.VALUE_SIZE);
        valueColumn.setMaxWidth(RegisterBlockWindow.VALUE_SIZE);
        valueColumn.setCellRenderer(new RegisterCellRenderer(SwingConstants.RIGHT, table));

        this.table.setPreferredScrollableViewportSize(new Dimension(RegisterBlockWindow.NAME_SIZE + RegisterBlockWindow.NUMBER_SIZE + RegisterBlockWindow.VALUE_SIZE, 700));
        this.setLayout(new BorderLayout()); // table display will occupy entire width if widened
        this.add(new JScrollPane(
            this.table,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        ));
    }

    protected abstract String formatRegister(Register value, int base);

    /**
     * <p>beginObserving.</p>
     */
    protected abstract void beginObserving();

    /**
     * <p>endObserving.</p>
     */
    protected abstract void endObserving();

    /**
     * <p>resetRegisters.</p>
     */
    protected abstract void resetRegisters();

    /**
     * Sets up the data for the window.
     *
     * @return The array object with the data for the window.
     **/

    private Object @NotNull [] @NotNull [] setupWindow() {
        final Object[][] tableData = new Object[this.registers.length][3];
        for (int i = 0; i < this.registers.length; i++) {
            tableData[i][RegisterBlockWindow.NAME_COLUMN] = this.registers[i].getName();
            final int temp = this.registers[i].getNumber();
            tableData[i][RegisterBlockWindow.NUMBER_COLUMN] = temp == -1 ? "" : temp;
            tableData[i][RegisterBlockWindow.VALUE_COLUMN] = this.formatRegister(this.registers[i],
                NumberDisplayBaseChooser.getBase(BOOL_SETTINGS.getSetting(BoolSetting.DISPLAY_VALUES_IN_HEX)));
        }
        return tableData;
    }

    /**
     * Reset and redisplay registers
     */
    public void clearWindow() {
        this.clearHighlighting();
        this.resetRegisters();
        this.updateRegisters();
    }

    /**
     * Clear highlight background color from any row currently highlighted.
     */
    public void clearHighlighting() {
        this.table.tableChanged(new TableModelEvent(this.table.getModel()));
    }

    /**
     * Update register display using specified display base
     */
    public void updateRegisters() {
        for (int i = 0; i < this.registers.length; i++) {
            ((RegTableModel) this.table.getModel()).setDisplayAndModelValueAt(this.formatRegister(this.registers[i],
                    Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase()), i,
                RegisterBlockWindow.VALUE_COLUMN);
        }
    }

    @Override
    public void onSubscribe(final Flow.Subscription subscription) {
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(final @NotNull Notice notice) {
        switch (notice) {
            case final SimulatorNotice s -> {
                if (s.action() == SimulatorNotice.Action.START) {
                    // Simulated MIPS execution starts. Respond to memory changes if running in
                    // timed
                    // or stepped mode.
                    if (s.runSpeed() != RunSpeedPanel.UNLIMITED_SPEED || s.maxSteps() == 1) {
                        this.beginObserving();
                    }
                } else {
                    // Simulated MIPS execution stops. Stop responding.
                    this.endObserving();
                }
            }
//            case final SettingsNotice ignored -> this.updateRowHeight();
            case final RegisterAccessNotice a -> {
                // NOTE: each register is a separate Observable
                if (a.getAccessType() == AccessNotice.AccessType.WRITE) {
                    // Uses the same highlighting technique as for Text Segment -- see
                    // AddressCellRenderer class in DataSegmentWindow.java.
                    //                  TODO:  this.highlightCellForRegister((Register) observable);
                    Globals.getGui().getRegistersPane().setSelectedComponent(this);
                }
            }
            default -> {
            }
        }
        this.subscription.request(1);
    }

    private void updateRowHeight(final @NotNull FontSettings settings) {
        final var font = settings.getCurrentFont();
        final var height = this.getFontMetrics(font).getHeight();
        this.table.setRowHeight(height);
    }

    /**
     * Cell renderer for displaying register entries. This does highlighting, so if you
     * don't want highlighting for a given column, don't use this. Currently we highlight
     * all columns.
     */
    private static class RegisterCellRenderer extends DefaultTableCellRenderer {
        private final int alignment;
        private final @NotNull JTable table;
        private Font font;

        private RegisterCellRenderer(final int alignment, final @NotNull JTable table) {
            super();
            this.alignment = alignment;
            this.font = FONT_SETTINGS.getCurrentFont();
            this.table = table;
            FONT_SETTINGS.addChangeListener((settings) -> {
                this.font = settings.getCurrentFont();
                this.table.repaint();
            });
        }

        @Override
        public @NotNull Component getTableCellRendererComponent(final JTable table, final Object value,
                                                                final boolean isSelected, final boolean hasFocus,
                                                                final int row, final int column) {
            final JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value,
                isSelected, hasFocus, row, column);
            cell.setFont(this.font);
            cell.setHorizontalAlignment(this.alignment);
            return cell;
        }
    }

    private class RegTableModel extends AbstractTableModel {
        private final String[] columnNames = {"", "", ""};
        private final Object[][] data;

        private RegTableModel(final Object[][] d) {
            this.data = d;
            this.columnNames[RegisterBlockWindow.NUMBER_COLUMN] = "No.";
            this.columnNames[RegisterBlockWindow.NAME_COLUMN] = "Name";
            this.columnNames[RegisterBlockWindow.VALUE_COLUMN] = "Value";
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
         * JTable uses this method to determine the default renderer/
         * editor for each cell.
         */
        @Override
        public Class<?> getColumnClass(final int c) {
            return this.getValueAt(0, c).getClass();
        }

        /*
         * Don't need to implement this method unless your table's
         * editable.
         */
        @Override
        public boolean isCellEditable(final int row, final int col) {
            // Note that the data/cell address is constant,
            // no matter where the cell appears onscreen.
            return col == RegisterBlockWindow.VALUE_COLUMN;
        }

        /*
         * Update cell contents in table model. This method should be called
         * only when user edits cell, so input validation has to be done. If
         * second is valid, the register is updated.
         */
        @Override
        public void setValueAt(final Object value, final int row, final int col) {
            final int val;
            try {
                val = Binary.stringToInt((String) value);
            } catch (final NumberFormatException nfe) {
                this.data[row][col] = "INVALID";
                this.fireTableCellUpdated(row, col);
                return;
            }
            // Assures that if changed during program execution, the update will
            // occur only between instructions.
            Globals.memoryAndRegistersLock.lock();
            try {
                RegisterBlockWindow.this.registers[row].setValue(val);
            } finally {
                Globals.memoryAndRegistersLock.unlock();
            }
            final int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
            this.data[row][col] = NumberDisplayBaseChooser.formatNumber(val, valueBase);
            this.fireTableCellUpdated(row, col);
        }

        /**
         * Update cell contents in table model.
         */
        private void setDisplayAndModelValueAt(final Object value, final int row, final int col) {
            this.data[row][col] = value;
            this.fireTableCellUpdated(row, col);
        }
    }

    /**
     * JTable subclass to provide custom tool tips for each of the
     * register table column headers and for each register name in
     * the first column. From
     * <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/table.html">Sun's JTable tutorial</a>.
     */
    private class MyTippedJTable extends JTable {
        private final String[] regToolTips;
        private final String[] columnToolTips;

        private MyTippedJTable(final @NotNull RegTableModel m, final String[] row, final String[] col) {
            super(m);
            this.regToolTips = row;
            this.columnToolTips = col;
            this.setRowSelectionAllowed(true); // highlights background color of entire row
            this.setSelectionBackground(Color.GREEN);
        }

        // Implement table cell tool tips.
        @Override
        public String getToolTipText(final MouseEvent e) {
            final java.awt.Point p = e.getPoint();
            final int rowIndex = this.rowAtPoint(p);
            final int colIndex = this.columnAtPoint(p);
            final int realColumnIndex = this.convertColumnIndexToModel(colIndex);
            if (realColumnIndex == RegisterBlockWindow.NAME_COLUMN) { // Register name column
                return this.regToolTips[rowIndex];
            } else {
                // You can omit this part if you know you don't have any
                // renderers that supply their own tool tips.
                return super.getToolTipText(e);
            }
        }

        // Implement table header tool tips.
        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(this.columnModel) {
                @Override
                public String getToolTipText(final MouseEvent e) {
                    final java.awt.Point p = e.getPoint();
                    final int index = this.columnModel.getColumnIndexAtX(p.x);
                    final int realIndex = this.columnModel.getColumn(index).getModelIndex();
                    return MyTippedJTable.this.columnToolTips[realIndex];
                }
            };
        }
    }
}
