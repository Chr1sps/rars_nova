package rars.venus.registers;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.notices.AccessNotice;
import rars.notices.RegisterAccessNotice;
import rars.notices.SimulatorNotice;
import rars.riscv.hardware.registerFiles.RegisterFileBase;
import rars.riscv.hardware.registers.Register;
import rars.settings.BoolSetting;
import rars.util.BinaryUtilsOld;
import rars.venus.NumberDisplayBaseChooser;
import rars.venus.VenusUI;
import rars.venus.run.RunSpeedPanel;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import static rars.Globals.*;

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
public abstract class RegisterBlockWindowBase extends JPanel {
    private static final int NUMBER_COLUMN = 0;
    private static final int NAME_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;
    private static final int NUMBER_SIZE = 45;
    private static final int NAME_SIZE = 80;
    private static final int VALUE_SIZE = 160;
    public final @NotNull Consumer<@NotNull RegisterAccessNotice> processRegisterNotice;
    private final @NotNull JTable table;
    private final @NotNull RegisterFileBase registerFile;
    @NotNull
    private final VenusUI mainUI;
    private int highlightRow;

    /**
     * Constructor which sets up a fresh window with a table that contains the
     * register values.
     *
     * @param registerFile
     *     the register file to be displayed
     * @param registerDescriptions
     *     an array of {@link java.lang.String} objects
     * @param valueTip
     *     a {@link java.lang.String} object
     */
    RegisterBlockWindowBase(
        final @NotNull RegisterFileBase registerFile,
        final String[] registerDescriptions,
        final String valueTip,
        final @NotNull VenusUI mainUI
    ) {
        this.registerFile = registerFile;
        this.mainUI = mainUI;
        SIMULATOR.simulatorNoticeHook.subscribe(notice -> {
            if (notice.action() == SimulatorNotice.Action.START) {
                // Simulated MIPS execution starts.  Respond to memory changes if running in timed
                // or stepped mode.
                if (Double.compare(notice.runSpeed(), RunSpeedPanel.UNLIMITED_SPEED) != 0 || notice.maxSteps() == 1) {
                    beginObserving();
                }
            } else {
                // Simulated MIPS execution stops.  Stop responding.
                endObserving();
            }
        });
        this.highlightRow = -1;
        this.table = new MyTippedJTable(
            new RegTableModel(this.setupWindow()), registerDescriptions,
            new String[]{
                "Each register has a tool tip describing its usage convention",
                "Corresponding register number", valueTip
            }
        ) {
        };
        this.updateRowHeight();
        FONT_SETTINGS.onChangeListenerHook.subscribe(ignored -> this.updateRowHeight());
        final var columnModel = this.table.getColumnModel();

        final var nameColumn = columnModel.getColumn(RegisterBlockWindowBase.NAME_COLUMN);
        nameColumn.setMinWidth(RegisterBlockWindowBase.NAME_SIZE);
        nameColumn.setMaxWidth(RegisterBlockWindowBase.NAME_SIZE);
        nameColumn.setCellRenderer(new RegisterCellRenderer(SwingConstants.LEFT, table));

        final var numberColumn = columnModel.getColumn(RegisterBlockWindowBase.NUMBER_COLUMN);
        numberColumn.setMinWidth(RegisterBlockWindowBase.NUMBER_SIZE);
        numberColumn.setMaxWidth(RegisterBlockWindowBase.NUMBER_SIZE);
        // Display register values (String-ified) right-justified in mono font
        numberColumn.setCellRenderer(new RegisterCellRenderer(SwingConstants.RIGHT, table));

        final var valueColumn = columnModel.getColumn(RegisterBlockWindowBase.VALUE_COLUMN);
        valueColumn.setMinWidth(RegisterBlockWindowBase.VALUE_SIZE);
        valueColumn.setMaxWidth(RegisterBlockWindowBase.VALUE_SIZE);
        valueColumn.setCellRenderer(new RegisterCellRenderer(SwingConstants.RIGHT, table));

        this.table.setPreferredScrollableViewportSize(new Dimension(
            RegisterBlockWindowBase.NAME_SIZE + RegisterBlockWindowBase.NUMBER_SIZE + RegisterBlockWindowBase.VALUE_SIZE,
            700
        ));
        this.setLayout(new BorderLayout()); // table display will occupy entire width if widened
        this.add(new JScrollPane(
            this.table,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        ));
        processRegisterNotice = notice -> {

            if (notice.accessType == AccessNotice.AccessType.WRITE) {
                // Uses the same highlighting technique as for Text Segment -- see
                // AddressCellRenderer class in DataSegmentWindow.java.
                this.highlightCellForRegister(notice.register);
                this.mainUI.registersPane.setSelectedComponent(this);
            }
        };
    }

    protected abstract @NotNull String formatRegisterValue(final long value, int base);

    private void beginObserving() {
        this.registerFile.addRegistersListener(this.processRegisterNotice);
    }

    private void endObserving() {
        this.registerFile.deleteRegistersListener(this.processRegisterNotice);
    }

    private void resetRegisters() {
        this.registerFile.resetRegisters();
    }

    /**
     * Sets up the data for the window.
     *
     * @return The array object with the data for the window.
     **/

    private Object @NotNull [] @NotNull [] setupWindow() {
        final var registers = this.registerFile.getRegisters();
        final Object[][] tableData = new Object[registers.length][3];
        for (int i = 0; i < registers.length; i++) {
            tableData[i][RegisterBlockWindowBase.NAME_COLUMN] = registers[i].name;
            final int temp = registers[i].number;
            tableData[i][RegisterBlockWindowBase.NUMBER_COLUMN] = temp == -1 ? "" : temp;
            final int base =
                NumberDisplayBaseChooser.getBase(BOOL_SETTINGS.getSetting(BoolSetting.DISPLAY_VALUES_IN_HEX));
            tableData[i][RegisterBlockWindowBase.VALUE_COLUMN] = this.formatRegisterValue(
                registers[i].getValue(),
                base
            );
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
        this.highlightRow = -1;
    }

    /**
     * Update register display using specified display base
     */
    public void updateRegisters() {
        final var registers = this.registerFile.getRegisters();
        for (int i = 0; i < registers.length; i++) {
            final var model = (RegTableModel) this.table.getModel();
            final int base = RegisterBlockWindowBase.this.mainUI.mainPane.executePane.getValueDisplayBase();
            final var formattedValue = this.formatRegisterValue(registers[i].getValue(), base);
            model.setDisplayAndModelValueAt(formattedValue, i, RegisterBlockWindowBase.VALUE_COLUMN);
        }
    }

    /**
     * Highlight the row corresponding to the given register.
     *
     * @param register
     *     Register object corresponding to row to be selected.
     */
    private void highlightCellForRegister(final Register register) {
        final var registers = this.registerFile.getRegisters();
        for (int i = 0; i < registers.length; i++) {
            if (registers[i] == register) {
                this.highlightRow = i;
                table.tableChanged(new TableModelEvent(table.getModel()));
                return;
            }
        }
        this.highlightRow = -1;
    }

    private void updateRowHeight() {
        final var font = FONT_SETTINGS.getCurrentFont();
        final var height = this.getFontMetrics(font).getHeight();
        this.table.setRowHeight(height);
    }

    /**
     * Cell renderer for displaying register entries. This does highlighting, so if you
     * don't want highlighting for a given column, don't use this. Currently we highlight
     * all columns.
     */
    private final class RegisterCellRenderer extends DefaultTableCellRenderer {
        private final int alignment;
        private final @NotNull JTable table;
        private Font font;

        private RegisterCellRenderer(final int alignment, final @NotNull JTable table) {
            super();
            this.alignment = alignment;
            this.font = FONT_SETTINGS.getCurrentFont();
            this.table = table;
            FONT_SETTINGS.onChangeListenerHook.subscribe(ignore -> {
                this.font = FONT_SETTINGS.getCurrentFont();
                this.table.repaint();
            });
        }

        @Override
        public @NotNull Component getTableCellRendererComponent(
            final JTable table, final Object value,
            final boolean isSelected, final boolean hasFocus,
            final int row, final int column
        ) {
            final JLabel cell = (JLabel) super.getTableCellRendererComponent(
                table, value,
                isSelected, hasFocus, row, column
            );
            cell.setFont(this.font);
            cell.setHorizontalAlignment(this.alignment);
            if (BOOL_SETTINGS.getSetting(BoolSetting.REGISTERS_HIGHLIGHTING) && row == highlightRow) {
                final var highlightingStyle = Globals.HIGHLIGHTING_SETTINGS.getRegisterHighlightingStyle();
                cell.setForeground(highlightingStyle.foreground());
                cell.setBackground(highlightingStyle.background());
            } else {
                final var theme = EDITOR_THEME_SETTINGS.getCurrentTheme();
                cell.setForeground(theme.foregroundColor);
                cell.setBackground(theme.backgroundColor);
            }
            return cell;
        }
    }

    private final class RegTableModel extends AbstractTableModel {
        private final String[] columnNames = {"", "", ""};
        private final Object[][] data;

        private RegTableModel(final Object[][] d) {
            this.data = d;
            this.columnNames[RegisterBlockWindowBase.NUMBER_COLUMN] = "No.";
            this.columnNames[RegisterBlockWindowBase.NAME_COLUMN] = "Name";
            this.columnNames[RegisterBlockWindowBase.VALUE_COLUMN] = "Value";
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
            return col == RegisterBlockWindowBase.VALUE_COLUMN;
        }

        /**
         * Update cell contents in table model. This method should be called
         * only when user edits cell, so input validation has to be done. If
         * value is valid, the register is updated.
         */
        @Override
        public void setValueAt(final Object value, final int row, final int col) {
            final long newValue;
            try {
                if (BOOL_SETTINGS.getSetting(BoolSetting.RV64_ENABLED)) {
                    newValue = BinaryUtilsOld.stringToLong((String) value);
                } else {
                    newValue = BinaryUtilsOld.stringToInt((String) value);
                }
            } catch (final NumberFormatException nfe) {
                // If the user enters an invalid value, don't do anything.
                return;
            }
            // Assures that if changed during program execution, the update will
            // occur only between instructions.
            Globals.MEMORY_REGISTERS_LOCK.lock();
            try {
                RegisterBlockWindowBase.this.registerFile.getRegisters()[row].setValue(newValue);
            } finally {
                Globals.MEMORY_REGISTERS_LOCK.unlock();
            }
            final int valueBase = RegisterBlockWindowBase.this.mainUI.mainPane.executePane.getValueDisplayBase();
            final var formattedValue = RegisterBlockWindowBase.this.formatRegisterValue(newValue, valueBase);
            this.data[row][col] = formattedValue;
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

        @Override
        public String getToolTipText(final MouseEvent e) {
            // Implement table cell tool tips.
            final java.awt.Point p = e.getPoint();
            final int rowIndex = this.rowAtPoint(p);
            final int colIndex = this.columnAtPoint(p);
            final int realColumnIndex = this.convertColumnIndexToModel(colIndex);
            if (realColumnIndex == RegisterBlockWindowBase.NAME_COLUMN) { // Register name column
                return this.regToolTips[rowIndex];
            } else {
                // You can omit this part if you know you don't have any
                // renderers that supply their own tool tips.
                return super.getToolTipText(e);
            }
        }

        @Override
        protected JTableHeader createDefaultTableHeader() {
            // Implement table header tool tips.
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
