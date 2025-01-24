package rars.venus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.RISCVProgram;
import rars.assembler.Symbol;
import rars.assembler.SymbolTable;
import rars.util.BinaryUtils;
import rars.venus.run.RunAssembleAction;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static rars.Globals.OTHER_SETTINGS;

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
 * Represents the Labels window, which is a type of JInternalFrame. Venus user
 * can view MIPS program labels.
 *
 * @author Sanderson and Team JSpim
 */
public final class LabelsWindow extends JInternalFrame {
    private static final int MAX_DISPLAYED_CHARS = 24;
    private static final int LABEL_COLUMN = 0;
    private static final int ADDRESS_COLUMN = 1;
    private static final String[] columnToolTips = {
        "Programmer-defined label (identifier).", // LABEL_COLUMN
        "Text or data segment address at which label is defined." // ADDRESS_COLUMN
    };
    /**
     * The array of state transitions; primary index corresponds to state in table
     * above, secondary index corresponds to table columns (0==label name, 1==address).
     */
    private static final int[][] sortStateTransitions = {
        {4, 1}, // 0
        {5, 0}, // 1
        {6, 3}, // 2
        {7, 2}, // 3
        {6, 0}, // 4
        {7, 1}, // 5
        {4, 2}, // 6
        {5, 3}  // 7
    };
    private static final char ASCENDING_SYMBOL = '▲';
    private static final char DESCENDING_SYMBOL = '▼';
    /** The array of column headings; index corresponds to state in table above. */
    private static final String[][] sortColumnHeadings = {
        {"Label", "Address  " + LabelsWindow.ASCENDING_SYMBOL},  // 0
        {"Label", "Address  " + LabelsWindow.DESCENDING_SYMBOL}, // 1
        {"Label", "Address  " + LabelsWindow.ASCENDING_SYMBOL},  // 2
        {"Label", "Address  " + LabelsWindow.DESCENDING_SYMBOL}, // 3
        {"Label  " + LabelsWindow.ASCENDING_SYMBOL, "Address"},  // 4
        {"Label  " + LabelsWindow.ASCENDING_SYMBOL, "Address"},  // 5
        {"Label  " + LabelsWindow.DESCENDING_SYMBOL, "Address"}, // 6
        {"Label  " + LabelsWindow.DESCENDING_SYMBOL, "Address"}  // 7
    };
    private static final @NotNull Comparator<@NotNull Symbol> labelNameAscending = Comparator.comparing(
        symbol -> symbol.name().toLowerCase()
    );
    private static final @NotNull Comparator<@NotNull Symbol> labelAddressAscending = (a, b) -> {
        final int addrA = a.address();
        final int addrB = b.address();
        return (addrA >= 0 && addrB >= 0 || addrA < 0 && addrB < 0) ? addrA - addrB : addrB;
    };
    private static String[] columnNames;
    private final JPanel labelPanel; // holds J
    private final JCheckBox dataLabels;
    private final JCheckBox textLabels;

    /*
     Use 8-state machine to track sort status for displaying tables
     State    Sort Column     Name sort order   Address sort order  Click Name   Click Addr
       0         Addr              ascend             ascend            4            1
       1         Addr              ascend             descend           5            0
       2         Addr              descend            ascend            6            3
       3         Addr              descend            descend           7            2
       4         Name              ascend             ascend            6            0
       5         Name              ascend             descend           7            1
       6         Name              descend            ascend            4            2
       7         Name              descend            descend           5            3
     "Click Name" column shows which state to go to when Name column is clicked.
     "Click Addr" column shows which state to go to when Addr column is clicked.
    */

    /** The array of comparators; index corresponds to state in table above. */
    private final List<Comparator<Symbol>> tableSortingComparators = List.of(
        /* 0 */ labelAddressAscending,
        /* 1 */ labelAddressAscending.reversed(),
        /* 2 */ labelAddressAscending,
        /* 3 */ labelAddressAscending.reversed(),
        /* 4 */ labelNameAscending,
        /* 5 */ labelNameAscending,
        /* 6 */ labelNameAscending.reversed(),
        /* 7 */ labelNameAscending.reversed()
    );
    @NotNull
    private final ExecutePane executePane;
    private ArrayList<LabelsForSymbolTable> listOfLabelsForSymbolTable;
    private Comparator<Symbol> tableSortComparator;
    // Current sort state (0-7, see table above). Will be set from saved Settings in
    // construtor.
    private int sortState;

    /**
     * Constructor for the Labels (symbol table) window.
     */
    public LabelsWindow(final @NotNull ExecutePane executePane) {
        super("Labels", true, false, true, true);
        this.executePane = executePane;
        this.sortState = OTHER_SETTINGS.getLabelSortState();
        LabelsWindow.columnNames = LabelsWindow.sortColumnHeadings[this.sortState];
        this.tableSortComparator = this.tableSortingComparators.get(this.sortState);
        final Container contentPane = this.getContentPane();
        this.labelPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        final JPanel features = new JPanel();
        this.dataLabels = new JCheckBox("Data", true);
        this.textLabels = new JCheckBox("Text", true);
        final ItemListener listener = item -> {
            for (final LabelsForSymbolTable symtab : LabelsWindow.this.listOfLabelsForSymbolTable) {
                symtab.generateLabelTable();
            }
        };
        this.dataLabels.addItemListener(listener);
        this.textLabels.addItemListener(listener);
        this.dataLabels.setToolTipText("If checked, will display labels defined in data segment");
        this.textLabels.setToolTipText("If checked, will display labels defined in text segment");
        features.add(this.dataLabels);
        features.add(this.textLabels);
        contentPane.add(features, BorderLayout.SOUTH);
        contentPane.add(this.labelPanel);
    }

    /**
     * Initialize table of labels (symbol table)
     */
    public void setupTable() {
        this.labelPanel.removeAll();
        this.labelPanel.add(this.generateLabelScrollPane());
    }

    /**
     * Clear the window
     */
    public void clearWindow() {
        this.labelPanel.removeAll();
    }

    private JScrollPane generateLabelScrollPane() {
        this.listOfLabelsForSymbolTable = new ArrayList<>();
        this.listOfLabelsForSymbolTable.add(new LabelsForSymbolTable(null));// global symtab
        final Box allSymtabTables = Box.createVerticalBox();
        for (final RISCVProgram program : RunAssembleAction.getProgramsToAssemble()) {
            this.listOfLabelsForSymbolTable.add(new LabelsForSymbolTable(program));
        }
        final ArrayList<Box> tableNames = new ArrayList<>();
        JTableHeader tableHeader = null;
        for (final LabelsForSymbolTable symtab : this.listOfLabelsForSymbolTable) {
            if (symtab.hasSymbols()) {
                String name = symtab.getSymbolTableName();
                if (name.length() > LabelsWindow.MAX_DISPLAYED_CHARS) {
                    name = name.substring(0, LabelsWindow.MAX_DISPLAYED_CHARS - 3) + "...";
                }
                // To get left-justified, put file name into first slot of horizontal Box, then
                // glue.
                final JLabel nameLab = new JLabel(name, JLabel.LEFT);
                final Box nameLabel = Box.createHorizontalBox();
                nameLabel.add(nameLab);
                nameLabel.add(Box.createHorizontalGlue());
                nameLabel.add(Box.createHorizontalStrut(1));
                tableNames.add(nameLabel);
                allSymtabTables.add(nameLabel);
                final JTable table = symtab.generateLabelTable();
                tableHeader = table.getTableHeader();
                // The following is selfish on my part. Column re-ordering doesn't work
                // correctly when
                // displaying multiple symbol tables; the headers re-order but the columns do
                // not.
                // Given the low perceived benefit of reordering displayed symbol table
                // information
                // versus the perceived effort to make reordering work for multiple symbol
                // tables,
                // I am taking the easy way out here. PS 19 July 2007.
                tableHeader.setReorderingAllowed(false);
                table.setSelectionBackground(table.getBackground());
                // Sense click on label/address and scroll Text/Data segment display to it.
                table.addMouseListener(new LabelDisplayMouseListener());
                allSymtabTables.add(table);
            }
        }
        final JScrollPane labelScrollPane = new JScrollPane(
            allSymtabTables,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        // Set file name label's max width to scrollpane's viewport width, max height to
        // small.
        // Does it do any good? Addressing problem that occurs when label (file) is
        // wider than
        // the table beneath it -- the table column widths are stretched to attain the
        // same width and
        // the address information requires scrolling to see. All because of a long file
        // name.
        for (final Box nameLabel : tableNames) {
            nameLabel.setMaximumSize(new Dimension(
                labelScrollPane.getViewport().getViewSize().width,
                (int) (1.5 * nameLabel.getFontMetrics(nameLabel.getFont()).getHeight())
            ));
        }
        labelScrollPane.setColumnHeaderView(tableHeader);
        return labelScrollPane;
    }

    /**
     * Method to update display of label addresses. Since label information doesn't
     * change,
     * this should only be done when address base is changed.
     * (e.g. between base 16 hex and base 10 dec).
     */
    public void updateLabelAddresses() {
        if (this.listOfLabelsForSymbolTable != null) {
            for (final LabelsForSymbolTable symtab : this.listOfLabelsForSymbolTable) {
                symtab.updateLabelAddresses();
            }
        }
    }

    /** Class representing label table data */
    static class LabelTableModel extends AbstractTableModel {
        final String[] columns;
        final Object[][] data;

        public LabelTableModel(final Object[][] d, final String[] n) {
            this.data = d;
            this.columns = n;
        }

        @Override
        public int getColumnCount() {
            return this.columns.length;
        }

        @Override
        public int getRowCount() {
            return this.data.length;
        }

        @Override
        public String getColumnName(final int col) {
            return this.columns[col];
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
         * data can change.
         */
        @Override
        public void setValueAt(final Object value, final int row, final int col) {
            this.data[row][col] = value;
            this.fireTableCellUpdated(row, col);
        }
    }

    /**
     * Private listener class to sense clicks on a table entry's
     * Label or Address. This will trigger action by Text or Data
     * segment to scroll to the corresponding label/address.
     * Suggested by Ken Vollmar, implemented by Pete Sanderson
     * July 2007.
     */
    private class LabelDisplayMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(final MouseEvent e) {
            final JTable table = (JTable) e.getSource();
            final int row = table.rowAtPoint(e.getPoint());
            final int column = table.columnAtPoint(e.getPoint());
            Object data = table.getValueAt(row, column);
            if (table.getColumnName(column).equals(LabelsWindow.columnNames[LabelsWindow.LABEL_COLUMN])) {
                // Selected a Label name, so get its address.
                data = table.getModel().getValueAt(row, LabelsWindow.ADDRESS_COLUMN);
            }
            int address = 0;
            try {
                address = BinaryUtils.stringToInt((String) data);
            } catch (final NumberFormatException nfe) {
                // Cannot happen because address is generated internally.
            } catch (final ClassCastException cce) {
                // Cannot happen because table contains only strings.
            }
            // Scroll to this address, either in Text Segment display or Data Segment
            // display
            if (Globals.MEMORY_INSTANCE.isAddressInTextSegment(address)) {
                LabelsWindow.this.executePane.textSegment.selectStepAtAddress(address);
            } else {
                LabelsWindow.this.executePane.dataSegment.selectCellForAddress(address);
            }
        }
    }

    /** Represents one symbol table for the display. */
    private class LabelsForSymbolTable {
        private final RISCVProgram program;
        private final SymbolTable symbolTable;
        private final String tableName;
        private Object[][] labelData;
        private JTable labelTable;
        private List<Symbol> symbols;

        /**
         * @param program
         *     the program to associate with this symbol table
         */
        public LabelsForSymbolTable(final @Nullable RISCVProgram program) {
            this.program = program;
            this.symbolTable = (program == null) ? Globals.GLOBAL_SYMBOL_TABLE : program.getLocalSymbolTable();
            this.tableName = (program == null) ? "(global)" : program.getFile().getName();
        }

        /**
         * Returns file name of associated file for local symbol table or "(global)"
         */
        public String getSymbolTableName() {
            return this.tableName;
        }

        public boolean hasSymbols() {
            return this.symbolTable.getSize() != 0;
        }

        // builds the Table containing labels and addresses for this symbol table.
        private JTable generateLabelTable() {
            final SymbolTable symbolTable = (this.program == null)
                ? Globals.GLOBAL_SYMBOL_TABLE
                : this.program.getLocalSymbolTable();
            final int addressBase = LabelsWindow.this.executePane.getAddressDisplayBase();
            if (LabelsWindow.this.textLabels.isSelected() && LabelsWindow.this.dataLabels.isSelected()) {
                this.symbols = symbolTable.getAllSymbols();
            } else if (LabelsWindow.this.textLabels.isSelected()) {
                this.symbols = symbolTable.getTextSymbols();
            } else if (LabelsWindow.this.dataLabels.isSelected()) {
                this.symbols = symbolTable.getDataSymbols();
            } else {
                this.symbols = new ArrayList<>();
            }
            this.symbols.sort(LabelsWindow.this.tableSortComparator); // DPS 25 Dec 2008
            this.labelData = new Object[this.symbols.size()][2];

            for (int i = 0; i < this.symbols.size(); i++) {// sets up the label table
                final Symbol s = this.symbols.get(i);
                this.labelData[i][LabelsWindow.LABEL_COLUMN] = s.name();
                this.labelData[i][LabelsWindow.ADDRESS_COLUMN] = NumberDisplayBaseChooser.formatNumber(
                    s.address(),
                    addressBase
                );
            }
            final LabelTableModel m = new LabelTableModel(this.labelData, LabelsWindow.columnNames);
            if (this.labelTable == null) {
                this.labelTable = new MyTippedJTable(m);
            } else {
                this.labelTable.setModel(m);
            }
            this.labelTable.getColumnModel()
                .getColumn(LabelsWindow.ADDRESS_COLUMN)
                .setCellRenderer(new MonoRightCellRenderer());
            return this.labelTable;
        }

        public void updateLabelAddresses() {
            if (LabelsWindow.this.labelPanel.getComponentCount() == 0) {
                return; // ignore if no content to change
            }
            final int addressBase = LabelsWindow.this.executePane.getAddressDisplayBase();
            final int numSymbols = (this.labelData == null) ? 0 : this.labelData.length;
            for (int i = 0; i < numSymbols; i++) {
                final int address = this.symbols.get(i).address();
                final String formattedAddress = NumberDisplayBaseChooser.formatNumber(address, addressBase);
                this.labelTable.getModel().setValueAt(formattedAddress, i, LabelsWindow.ADDRESS_COLUMN);
            }
        }
    }

    /**
     * JTable subclass to provide custom tool tips for each of the
     * label table column headers. From Sun's JTable tutorial.
     * <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/table.html">
     * http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
     * </a>
     */
    private class MyTippedJTable extends JTable {
        MyTippedJTable(final LabelTableModel m) {
            super(m);
        }

        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new SymbolTableHeader(this.columnModel);
        }

        /**
         * Implement cell tool tips. All of them are the same (although they could be
         * customized).
         */
        @Override
        public Component prepareRenderer(final TableCellRenderer renderer, final int rowIndex, final int vColIndex) {
            final Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
            if (c instanceof final JComponent jc) {
                jc.setToolTipText("Click on label or address to view it in Text/Data Segment");
            }
            return c;
        }

        /**
         * Customized table header that will both display tool tip when
         * mouse hovers over each column, and also sort the table when
         * mouse is clicked on each column. The tool tip and sort are
         * customized based on the column under the mouse.
         */
        private class SymbolTableHeader extends JTableHeader {

            public SymbolTableHeader(final TableColumnModel cm) {
                super(cm);
                this.addMouseListener(new SymbolTableHeaderMouseListener());
            }

            @Override
            public String getToolTipText(final MouseEvent e) {
                final Point p = e.getPoint();
                final int index = this.columnModel.getColumnIndexAtX(p.x);
                final int realIndex = this.columnModel.getColumn(index).getModelIndex();
                return LabelsWindow.columnToolTips[realIndex];
            }

            /**
             * When user clicks on table column header, system will sort the
             * table based on that column then redraw it.
             */
            private class SymbolTableHeaderMouseListener implements MouseListener {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Point p = e.getPoint();
                    final int index = SymbolTableHeader.this.columnModel.getColumnIndexAtX(p.x);
                    final int realIndex = SymbolTableHeader.this.columnModel.getColumn(index).getModelIndex();
                    LabelsWindow.this.sortState =
                        LabelsWindow.sortStateTransitions[LabelsWindow.this.sortState][realIndex];
                    LabelsWindow.this.tableSortComparator =
                        LabelsWindow.this.tableSortingComparators.get(LabelsWindow.this.sortState);
                    LabelsWindow.columnNames = LabelsWindow.sortColumnHeadings[LabelsWindow.this.sortState];
                    OTHER_SETTINGS.setLabelSortStateAndSave(LabelsWindow.this.sortState);
                    LabelsWindow.this.setupTable();
                    LabelsWindow.this.executePane.setLabelWindowVisibility(false);
                    LabelsWindow.this.executePane.setLabelWindowVisibility(true);
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
