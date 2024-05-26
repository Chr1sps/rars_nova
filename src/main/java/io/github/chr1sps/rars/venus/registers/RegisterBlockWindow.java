package io.github.chr1sps.rars.venus.registers;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.Settings;
import io.github.chr1sps.rars.notices.*;
import io.github.chr1sps.rars.riscv.hardware.Register;
import io.github.chr1sps.rars.util.Binary;
import io.github.chr1sps.rars.util.SimpleSubscriber;
import io.github.chr1sps.rars.venus.MonoRightCellRenderer;
import io.github.chr1sps.rars.venus.NumberDisplayBaseChooser;
import io.github.chr1sps.rars.venus.run.RunSpeedPanel;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.Flow;

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
    private final JTable table;
    private boolean highlighting;
    private int highlightRow;
    private final Register[] registers;

    private static final int NUMBER_COLUMN = 0;
    private static final int NAME_COLUMN = 1;
    private static final int VALUE_COLUMN = 2;

    private static final int NUMBER_SIZE = 45;
    private static final int NAME_SIZE = 80;
    private static final int VALUE_SIZE = 150;
    private final Settings settings;

    /**
     * Constructor which sets up a fresh window with a table that contains the
     * register values.
     *
     * @param registers            an array of {@link io.github.chr1sps.rars.riscv.hardware.Register} objects
     * @param registerDescriptions an array of {@link java.lang.String} objects
     * @param valueTip             a {@link java.lang.String} object
     */
    RegisterBlockWindow(final Register[] registers, final String[] registerDescriptions, final String valueTip) {
//        Simulator.getInstance().addObserver(this);
        this.settings = Globals.getSettings();
//        this.settings.addObserver(this);
        this.registers = registers;
        this.clearHighlighting();
        this.table = new MyTippedJTable(new RegTableModel(this.setupWindow()), registerDescriptions,
                new String[]{"Each register has a tool tip describing its usage convention",
                        "Corresponding register number", valueTip}) {
        };
        this.updateRowHeight();
        this.table.getColumnModel().getColumn(NAME_COLUMN).setMinWidth(NAME_SIZE);
        this.table.getColumnModel().getColumn(NAME_COLUMN).setMaxWidth(NAME_SIZE);
        this.table.getColumnModel().getColumn(NUMBER_COLUMN).setMinWidth(NUMBER_SIZE);
        this.table.getColumnModel().getColumn(NUMBER_COLUMN).setMaxWidth(NUMBER_SIZE);
        this.table.getColumnModel().getColumn(VALUE_COLUMN).setMinWidth(VALUE_SIZE);
        this.table.getColumnModel().getColumn(VALUE_COLUMN).setMaxWidth(VALUE_SIZE);
        // Display register values (String-ified) right-justified in mono font
        this.table.getColumnModel().getColumn(NAME_COLUMN).setCellRenderer(
                new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.LEFT));
        this.table.getColumnModel().getColumn(NUMBER_COLUMN).setCellRenderer(
                new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.RIGHT));
        this.table.getColumnModel().getColumn(VALUE_COLUMN).setCellRenderer(
                new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, SwingConstants.RIGHT));
        this.table.setPreferredScrollableViewportSize(new Dimension(NAME_SIZE + NUMBER_SIZE + VALUE_SIZE, 700));
        this.setLayout(new BorderLayout()); // table display will occupy entire width if widened
        this.add(new JScrollPane(this.table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    }

    /**
     * <p>formatRegister.</p>
     *
     * @param value a {@link io.github.chr1sps.rars.riscv.hardware.Register} object
     * @param base  a int
     * @return a {@link java.lang.String} object
     */
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

    private Object[][] setupWindow() {
        final Object[][] tableData = new Object[this.registers.length][3];
        for (int i = 0; i < this.registers.length; i++) {
            tableData[i][NAME_COLUMN] = this.registers[i].getName();
            final int temp = this.registers[i].getNumber();
            tableData[i][NUMBER_COLUMN] = temp == -1 ? "" : temp;
            tableData[i][VALUE_COLUMN] = this.formatRegister(this.registers[i],
                    NumberDisplayBaseChooser.getBase(this.settings.getBooleanSetting(Settings.Bool.DISPLAY_VALUES_IN_HEX)));
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
        this.highlighting = false;
        if (this.table != null) {
            this.table.tableChanged(new TableModelEvent(this.table.getModel()));
        }
        this.highlightRow = -1; // assure highlight will not occur upon re-assemble.
    }

    /**
     * Refresh the table, triggering re-rendering.
     */
    public void refresh() {
        if (this.table != null) {
            this.table.tableChanged(new TableModelEvent(this.table.getModel()));
        }
    }

    /**
     * Update register display using specified display base
     */
    public void updateRegisters() {
        for (int i = 0; i < this.registers.length; i++) {
            ((RegTableModel) this.table.getModel()).setDisplayAndModelValueAt(this.formatRegister(this.registers[i],
                    Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase()), i, VALUE_COLUMN);
        }
    }

    /**
     * Highlight the row corresponding to the given register.
     *
     * @param register Register object corresponding to row to be selected.
     */
    private void highlightCellForRegister(final Register register) {
        for (int i = 0; i < this.registers.length; i++) {
            if (this.registers[i] == register) {
                this.highlightRow = i;
                this.table.tableChanged(new TableModelEvent(this.table.getModel()));
                return;
            }
        }
        this.highlightRow = -1;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Required by Observer interface. Called when notified by an Observable that we
     * are registered with.
     * Observables include:
     * The Simulator object, which lets us know when it starts and stops running
     * A register object, which lets us know of register operations
     * The Simulator keeps us informed of when simulated MIPS execution is active.
     * This is the only time we care about register operations.
     */
//    @Override
//    public void update(final Observable observable, final Object obj) {
//        if (observable == io.github.chr1sps.rars.simulator.Simulator.getInstance()) {
//            final SimulatorNotice notice = (SimulatorNotice) obj;
//            if (notice.getAction() == SimulatorNotice.SIMULATOR_START) {
//                // Simulated MIPS execution starts. Respond to memory changes if running in
//                // timed
//                // or stepped mode.
//                if (notice.getRunSpeed() != RunSpeedPanel.UNLIMITED_SPEED || notice.getMaxSteps() == 1) {
//                    this.beginObserving();
//                    this.highlighting = true;
//                }
//            } else {
//                // Simulated MIPS execution stops. Stop responding.
//                this.endObserving();
//            }
//        } else if (observable == this.settings) {
//            this.updateRowHeight();
//        } else if (obj instanceof final RegisterAccessNotice access) {
//            // NOTE: each register is a separate Observable
//            if (access.getAccessType() == AccessNotice.WRITE) {
//                // Uses the same highlighting technique as for Text Segment -- see
//                // AddressCellRenderer class in DataSegmentWindow.java.
//                this.highlighting = true;
//                this.highlightCellForRegister((Register) observable);
//                Globals.getGui().getRegistersPane().setSelectedComponent(this);
//            }
//        }
//    }
    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(final Notice notice) {
        switch (notice) {
            case SimulatorNotice s -> {
                if (s.getAction() == SimulatorNotice.SIMULATOR_START) {
                    // Simulated MIPS execution starts. Respond to memory changes if running in
                    // timed
                    // or stepped mode.
                    if (s.getRunSpeed() != RunSpeedPanel.UNLIMITED_SPEED || s.getMaxSteps() == 1) {
                        this.beginObserving();
                        this.highlighting = true;
                    }
                } else {
                    // Simulated MIPS execution stops. Stop responding.
                    this.endObserving();
                }
            }
            case SettingsNotice ignored -> this.updateRowHeight();
            case RegisterAccessNotice a -> {
                // NOTE: each register is a separate Observable
                if (a.getAccessType() == AccessNotice.WRITE) {
                    // Uses the same highlighting technique as for Text Segment -- see
                    // AddressCellRenderer class in DataSegmentWindow.java.
                    this.highlighting = true;
//                  TODO:  this.highlightCellForRegister((Register) observable);
                    Globals.getGui().getRegistersPane().setSelectedComponent(this);
                }
            }
            default -> {
            }
        }
        this.subscription.request(1);
    }

    private void updateRowHeight() {
        final Font[] possibleFonts = {
                this.settings.getFontByPosition(Settings.REGISTER_HIGHLIGHT_FONT),
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
        this.table.setRowHeight(maxHeight);
    }

    /*
     * Cell renderer for displaying register entries. This does highlighting, so if
     * you
     * don't want highlighting for a given column, don't use this. Currently we
     * highlight
     * all columns.
     */
    private class RegisterCellRenderer extends DefaultTableCellRenderer {
        private final Font font;
        private final int alignment;

        private RegisterCellRenderer(final Font font, final int alignment) {
            super();
            this.font = font;
            this.alignment = alignment;
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                       final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            final JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);
            cell.setFont(this.font);
            cell.setHorizontalAlignment(this.alignment);
//            if (RegisterBlockWindow.this.settings.getBooleanSetting(Settings.Bool.REGISTERS_HIGHLIGHTING) && RegisterBlockWindow.this.highlighting
//                    && row == RegisterBlockWindow.this.highlightRow) {
//                cell.setBackground(RegisterBlockWindow.this.settings.getColorSettingByPosition(Settings.REGISTER_HIGHLIGHT_BACKGROUND));
//                cell.setForeground(RegisterBlockWindow.this.settings.getColorSettingByPosition(Settings.REGISTER_HIGHLIGHT_FOREGROUND));
//                cell.setFont(RegisterBlockWindow.this.settings.getFontByPosition(Settings.REGISTER_HIGHLIGHT_FONT));
//            } else if (row % 2 == 0) {
//                cell.setBackground(RegisterBlockWindow.this.settings.getColorSettingByPosition(Settings.EVEN_ROW_BACKGROUND));
//                cell.setForeground(RegisterBlockWindow.this.settings.getColorSettingByPosition(Settings.EVEN_ROW_FOREGROUND));
//                cell.setFont(RegisterBlockWindow.this.settings.getFontByPosition(Settings.EVEN_ROW_FONT));
//            } else {
//                cell.setBackground(RegisterBlockWindow.this.settings.getColorSettingByPosition(Settings.ODD_ROW_BACKGROUND));
//                cell.setForeground(RegisterBlockWindow.this.settings.getColorSettingByPosition(Settings.ODD_ROW_FOREGROUND));
//                cell.setFont(RegisterBlockWindow.this.settings.getFontByPosition(Settings.ODD_ROW_FONT));
//            }
            return cell;
        }
    }

    private class RegTableModel extends AbstractTableModel {
        private final String[] columnNames = {"", "", ""};
        private final Object[][] data;

        private RegTableModel(final Object[][] d) {
            this.data = d;
            this.columnNames[NUMBER_COLUMN] = "No.";
            this.columnNames[NAME_COLUMN] = "Name";
            this.columnNames[VALUE_COLUMN] = "Value";
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
        public Class getColumnClass(final int c) {
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
            return col == VALUE_COLUMN;
        }

        /*
         * Update cell contents in table model. This method should be called
         * only when user edits cell, so input validation has to be done. If
         * value is valid, the register is updated.
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

    ///////////////////////////////////////////////////////////////////
    //
    // JTable subclass to provide custom tool tips for each of the
    // register table column headers and for each register name in
    // the first column. From Sun's JTable tutorial.
    // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
    //
    private class MyTippedJTable extends JTable {
        private MyTippedJTable(final RegTableModel m, final String[] row, final String[] col) {
            super(m);
            this.regToolTips = row;
            this.columnToolTips = col;
            this.setRowSelectionAllowed(true); // highlights background color of entire row
            this.setSelectionBackground(Color.GREEN);
        }

        private final String[] regToolTips;

        // Implement table cell tool tips.
        @Override
        public String getToolTipText(final MouseEvent e) {
            final java.awt.Point p = e.getPoint();
            final int rowIndex = this.rowAtPoint(p);
            final int colIndex = this.columnAtPoint(p);
            final int realColumnIndex = this.convertColumnIndexToModel(colIndex);
            if (realColumnIndex == NAME_COLUMN) { // Register name column
                return this.regToolTips[rowIndex];
            } else {
                // You can omit this part if you know you don't have any
                // renderers that supply their own tool tips.
                return super.getToolTipText(e);
            }
        }

        private final String[] columnToolTips;

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
