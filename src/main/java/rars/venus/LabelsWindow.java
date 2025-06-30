package rars.venus;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.assembler.Symbol;
import rars.assembler.SymbolTable;
import rars.util.BinaryUtilsKt;
import rars.venus.run.RunAssembleAction;
import rars.venus.util.LabelsSortState;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static rars.Globals.OTHER_SETTINGS;
import static rars.venus.util.LabelsSortStateKt.columnNamesFor;
import static rars.venus.util.ListenerUtilsKt.onMouseClicked;

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
        "Programmer-defined label (identifier).",
        // LABEL_COLUMN
        "Text or data segment address at which label is defined."
        // ADDRESS_COLUMN
    };
    private final JPanel labelPanel; // holds J
    private final JCheckBox dataLabels;
    private final JCheckBox textLabels;

    @NotNull
    private final ExecutePane executePane;
    private @NotNull String[] columnNames;
    private ArrayList<LabelsForSymbolTable> listOfLabelsForSymbolTable;
    // Current sort state (0-7, see table above). Will be set from saved Settings in
    // construtor.
    private @NotNull LabelsSortState sortState;

    /**
     * Constructor for the Labels (symbol table) window.
     */
    public LabelsWindow(final @NotNull ExecutePane executePane) {
        super("Labels", true, false, true, true);
        this.executePane = executePane;
        sortState = OTHER_SETTINGS.getLabelSortState();
        columnNames = columnNamesFor(sortState);
        final ItemListener listener = item -> {
            for (final LabelsForSymbolTable symtab : listOfLabelsForSymbolTable) {
                symtab.generateLabelTable();
            }
        };

        this.textLabels = new JCheckBox("Text", true);
        this.textLabels.addItemListener(listener);
        this.textLabels.setToolTipText(
            "If checked, will display labels defined in text segment");

        this.dataLabels = new JCheckBox("Data", true);
        this.dataLabels.addItemListener(listener);
        this.dataLabels.setToolTipText(
            "If checked, will display labels defined in data segment");

        labelPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        final var features = new JPanel();
        features.add(this.dataLabels);
        features.add(this.textLabels);

        final var contentPane = getContentPane();
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
        listOfLabelsForSymbolTable = new ArrayList<>();
        listOfLabelsForSymbolTable.add(
            new LabelsForSymbolTable(Globals.GLOBAL_SYMBOL_TABLE, "(global)")
        );
        final var allSymtabTables = Box.createVerticalBox();
        for (final var program : RunAssembleAction.getProgramsToAssemble()) {
            listOfLabelsForSymbolTable.add(new LabelsForSymbolTable(
                program.getLocalSymbolTable(),
                program.getFile().getName()
            ));
        }
        final var tableNames = new ArrayList<Box>();
        JTableHeader tableHeader = null;
        for (final var symtab : this.listOfLabelsForSymbolTable) {
            if (symtab.hasSymbols()) {
                var name = symtab.getSymbolTableName();
                if (name.length() > LabelsWindow.MAX_DISPLAYED_CHARS) {
                    name = name.substring(
                        0, LabelsWindow.MAX_DISPLAYED_CHARS - 3
                    ) + "...";
                }
                // To get left-justified, put file name into first slot of horizontal Box, then
                // glue.
                final var nameLabel = new JLabel(name, JLabel.LEFT);
                final var nameSection = Box.createHorizontalBox();
                nameSection.add(nameLabel);
                nameSection.add(Box.createHorizontalGlue());
                nameSection.add(Box.createHorizontalStrut(1));
                tableNames.add(nameSection);
                allSymtabTables.add(nameSection);
                final var table = symtab.generateLabelTable();
                tableHeader = table.getTableHeader();
                tableHeader.setReorderingAllowed(false);
                table.setSelectionBackground(table.getBackground());
                // Sense click on label/address and scroll Text/Data segment display to it.
                // table.addMouseListener(new LabelDisplayMouseListener());
                onMouseClicked(table, e -> {
                    final int row = table.rowAtPoint(e.getPoint());
                    final int column = table.columnAtPoint(e.getPoint());
                    Object data = table.getValueAt(row, column);
                    if (table.getColumnName(column)
                        .equals(columnNames[LabelsWindow.LABEL_COLUMN])) {
                        // Selected a Label name, so get its address.
                        data = table.getModel()
                            .getValueAt(row, LabelsWindow.ADDRESS_COLUMN);
                    }
                    final int address = BinaryUtilsKt.stringToInt((String) data);

                    // Scroll to this address, either in Text Segment display or Data Segment
                    // display
                    if (Globals.MEMORY_INSTANCE.isAddressInTextSegment(address)) {
                        executePane.getTextSegment()
                            .selectStepAtAddress(address);
                    } else {
                        executePane.getDataSegment()
                            .selectCellForAddress(address);
                    }
                    return Unit.INSTANCE;
                });
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
                (int) (
                    1.5 * nameLabel.getFontMetrics(nameLabel.getFont())
                        .getHeight()
                )
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
            super();
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
        public void setValueAt(
            final Object value,
            final int row,
            final int col
        ) {
            this.data[row][col] = value;
            this.fireTableCellUpdated(row, col);
        }
    }

    /** Represents one symbol table for the display. */
    private class LabelsForSymbolTable {
        private final SymbolTable symbolTable;
        private final String tableName;
        private Object[][] labelData;
        private JTable labelTable;
        private List<Symbol> symbols;

        public LabelsForSymbolTable(
            final @NotNull SymbolTable symbolTable,
            final @NotNull String name
        ) {
            this.symbolTable = symbolTable;
            this.tableName = name; /*(program == null)
                ? "(global)"
                : program.getFile().getName();*/
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
            final var addressFormat = executePane.getAddressDisplayFormat();
            if (textLabels.isSelected() && dataLabels.isSelected()) {
                symbols = symbolTable.getAllSymbols();
            } else if (textLabels.isSelected()) {
                symbols = symbolTable.getTextSymbols();
            } else if (dataLabels.isSelected()) {
                symbols = symbolTable.getDataSymbols();
            } else {
                symbols = new ArrayList<>();
            }
            symbols = symbols.stream()
                .sorted(sortState.getComparator())
                .toList();
            // this.symbols.sort(sortState.getComparator());
            this.labelData = new Object[this.symbols.size()][2];

            for (int i = 0; i < this.symbols.size(); i++) {
                // sets up the label table
                final Symbol s = this.symbols.get(i);
                this.labelData[i][LabelsWindow.LABEL_COLUMN] = s.name();
                this.labelData[i][LabelsWindow.ADDRESS_COLUMN] = NumberDisplayBasePicker.formatNumber(
                    s.address(),
                    addressFormat
                );
            }
            final LabelTableModel m = new LabelTableModel(this.labelData,
                columnNames);
            if (this.labelTable == null) {
                this.labelTable = new MyTippedJTable(m);
            } else {
                this.labelTable.setModel(m);
            }
            this.labelTable.getColumnModel()
                .getColumn(LabelsWindow.ADDRESS_COLUMN)
                .setCellRenderer(new MonoRightCellRenderer(Globals.FONT_SETTINGS,
                    Globals.EDITOR_THEME_SETTINGS));
            return this.labelTable;
        }

        public void updateLabelAddresses() {
            if (labelPanel.getComponentCount() == 0) {
                return; // ignore if no content to change
            }
            final var addressFormat = executePane.getAddressDisplayFormat();
            final int numSymbols = (this.labelData == null)
                ? 0
                : this.labelData.length;
            for (int i = 0; i < numSymbols; i++) {
                final int address = this.symbols.get(i).address();
                final String formattedAddress = NumberDisplayBasePicker.formatNumber(
                    address,
                    addressFormat);
                this.labelTable.getModel()
                    .setValueAt(formattedAddress,
                        i,
                        LabelsWindow.ADDRESS_COLUMN);
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
        public Component prepareRenderer(
            final TableCellRenderer renderer,
            final int rowIndex,
            final int vColIndex
        ) {
            final Component c = super.prepareRenderer(renderer,
                rowIndex,
                vColIndex);
            if (c instanceof final JComponent jc) {
                jc.setToolTipText(
                    "Click on label or address to view it in Text/Data Segment");
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
                onMouseClicked(this, e -> {
                    final var point = e.getPoint();
                    final int index = columnModel.getColumnIndexAtX(point.x);
                    final int realIndex = columnModel.getColumn(index)
                        .getModelIndex();
                    sortState = switch (realIndex) {
                        case LABEL_COLUMN -> sortState.stateOnLabelClick();
                        case ADDRESS_COLUMN -> sortState.stateOnAddressClick();
                        default ->
                            throw new IllegalStateException("Unreachable code.");
                    };
                    columnNames = columnNamesFor(sortState);
                    OTHER_SETTINGS.setLabelsStateAndSave(sortState);
                    setupTable();
                    executePane.setLabelWindowVisibility(false);
                    executePane.setLabelWindowVisibility(true);
                    return Unit.INSTANCE;
                });
            }

            @Override
            public String getToolTipText(final MouseEvent e) {
                final Point p = e.getPoint();
                final int index = this.columnModel.getColumnIndexAtX(p.x);
                final int realIndex = this.columnModel.getColumn(index)
                    .getModelIndex();
                return LabelsWindow.columnToolTips[realIndex];
            }

        }
    }

}
