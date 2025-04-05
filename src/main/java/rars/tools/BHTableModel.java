package rars.tools;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

/**
 * Simulates the actual functionality of a Branch History Table (BHT).
 * <p>
 * The BHT consists of a number of BHT entries which are used to perform branch
 * prediction.
 * The entries of the BHT are stored as a Vector of BHTEntry objects.
 * The number of entries is configurable but has to be a power of 2.
 * The history kept by each BHT entry is also configurable during run-time.
 * A change of the configuration however causes a complete reset of the BHT.
 * <p>
 * The typical interaction is as follows:
 * <ul>
 * <li>Construction of a BHT with a certain number of entries with a given
 * history size.</li>
 * <li>When encountering a branch instruction the index of the relevant BHT
 * entry is calculated via the {@link BHTableModel#getIdxForAddress(int)}
 * method.</li>
 * <li>The current prediction of the BHT entry at the calculated index is
 * obtained via the {@link BHTableModel#getPredictionAtIdx(int)} method.</li>
 * <li>After detecting if the branch was really taken or not, this feedback is
 * provided to the BHT by the
 * {@link BHTableModel#updatePredictionAtIdx(int, boolean)} method.</li>
 * </ul>
 * <p>
 * Additionally it serves as TableModel that can be directly used to render the
 * state of the BHT in a JTable.
 * Feedback provided to the BHT causes a change of the internal state and a
 * repaint of the table(s) associated to this model.
 *
 * @author ingo.kofler@itec.uni-klu.ac.at
 */
public final class BHTableModel extends AbstractTableModel {

    /**
     * name of the table columns
     */
    private final String[] myColumnNames = {"Index", "History", "Prediction", "Correct", "Incorrect", "Precision"};
    /**
     * type of the table columns
     */
    private final Class<?>[] myColumnClasses = {
        Integer.class, String.class, String.class, Integer.class, Integer.class,
        Double.class
    };
    /**
     * vector holding the entries of the BHT
     */
    private ArrayList<BHTEntry> myEntries;
    /**
     * number of entries in the BHT
     */
    private int myEntryCount;

    /**
     * Constructs a new BHT with given number of entries and history size.
     *
     * @param numEntries
     *     number of entries in the BHT
     * @param historySize
     *     size of the history (in bits/number of past branches)
     * @param initVal
     *     a boolean
     */
    public BHTableModel(final int numEntries, final int historySize, final boolean initVal) {
        super();
        this.initBHT(numEntries, historySize, initVal);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the name of the i-th column of the table.
     * Required by the TableModel interface.
     */
    @Override
    public String getColumnName(final int i) {
        if (i < 0 || i > this.myColumnNames.length) {
            throw new IllegalArgumentException(
                "Illegal column index " + i + " (must be in range 0.." + (this.myColumnNames.length - 1) + ')');
        }

        return this.myColumnNames[i];
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the class/type of the i-th column of the table.
     * Required by the TableModel interface.
     */
    @Override
    public Class<?> getColumnClass(final int i) {
        if (i < 0 || i > this.myColumnClasses.length) {
            throw new IllegalArgumentException(
                "Illegal column index " + i + " (must be in range 0.." + (this.myColumnClasses.length - 1) + ')');
        }

        return this.myColumnClasses[i];
    }

    /**
     * Returns the number of columns.
     * Required by the TableModel interface.
     *
     * @return currently the constant 6
     */
    @Override
    public int getColumnCount() {
        return 6;
    }

    /**
     * Returns the number of entries of the BHT.
     * Required by the TableModel interface.
     *
     * @return number of rows / entries of the BHT
     */
    @Override
    public int getRowCount() {
        return this.myEntryCount;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the value of the cell at the given row and column
     * Required by the TableModel interface.
     */
    @Override
    public Object getValueAt(final int row, final int col) {

        final BHTEntry e = this.myEntries.get(row);
        if (e == null) {
            return "";
        }

        return switch (col) {
            case 0 -> row;
            case 1 -> e.getHistoryAsStr();
            case 2 -> e.getPredictionAsStr();
            case 3 -> e.getStatsPredCorrect();
            case 4 -> e.getStatsPredIncorrect();
            case 5 -> e.getStatsPredPrecision();
            default -> "";
        };
    }

    /**
     * Initializes the BHT with the given size and history.
     * All previous data like the BHT entries' history and statistics will get lost.
     * A refresh of the table that use this BHT as model will be triggered.
     *
     * @param numEntries
     *     number of entries in the BHT (has to be a power of 2)
     * @param historySize
     *     size of the history to consider
     * @param initVal
     *     initial value for each entry (true means take branch,
     *     false do not take branch)
     */
    public void initBHT(final int numEntries, final int historySize, final boolean initVal) {

        if (numEntries <= 0 || (numEntries & (numEntries - 1)) != 0) {
            throw new IllegalArgumentException("Number of entries must be a positive power of 2.");
        }
        if (historySize < 1 || historySize > 2) {
            throw new IllegalArgumentException("Only history sizes of 1 or 2 supported.");
        }

        this.myEntryCount = numEntries;

        this.myEntries = new ArrayList<>(this.myEntryCount);

        for (int i = 0; i < this.myEntryCount; i++) {
            this.myEntries.add(new BHTEntry(historySize, initVal));
        }

        // refresh the table(s)
        this.fireTableStructureChanged();
    }

    /**
     * Returns the index into the BHT for a given branch instruction address.
     * A simple direct mapping is used.
     *
     * @param address
     *     the address of the branch instruction
     * @return the index into the BHT
     */
    public int getIdxForAddress(final int address) {
        if (address < 0) {
            throw new IllegalArgumentException("No negative addresses supported");
        }

        return (address >> 2) % this.myEntryCount;
    }

    /**
     * Retrieve the prediction for the i-th BHT entry.
     *
     * @param index
     *     the index of the entry in the BHT
     * @return the prediction to take (true) or do not take (false) the branch
     */
    public boolean getPredictionAtIdx(final int index) {
        if (index < 0 || index > this.myEntryCount) {
            throw new IllegalArgumentException("Only indexes in the range 0 to " + (this.myEntryCount - 1) + " allowed");
        }

        return this.myEntries.get(index).getPrediction();
    }

    /**
     * Updates the BHT entry with the outcome of the branch instruction.
     * This causes a change in the model and signals to update the connected
     * table(s).
     *
     * @param index
     *     the index of the entry in the BHT
     * @param branchTaken
     *     a boolean
     */
    public void updatePredictionAtIdx(final int index, final boolean branchTaken) {
        if (index < 0 || index > this.myEntryCount) {
            throw new IllegalArgumentException("Only indexes in the range 0 to " + (this.myEntryCount - 1) + " allowed");
        }

        this.myEntries.get(index).updatePrediction(branchTaken);
        this.fireTableRowsUpdated(index, index);
    }

}
