package rars.tools;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Represents the GUI of the BHT Simulator Tool.
 * <p>
 * The GUI consists of mainly four parts:
 * <ul>
 * <li>A configuration panel to select the number of entries and the history
 * size
 * <li>A information panel that displays the most recent branch instruction
 * including its address and BHT index
 * <li>A table representing the BHT with all entries and their internal state
 * and statistics
 * <li>A log panel that summarizes the predictions in a textual form
 * </ul>
 *
 * @author ingo.kofler@itec.uni-klu.ac.at
 */
public final class BHTSimGUI extends JPanel {

    /**
     * constant for the color that highlights the current BHT entry
     */
    public final static Color COLOR_PREPREDICTION = Color.yellow;
    /**
     * constant for the color to signal a correct prediction
     */
    public final static Color COLOR_PREDICTION_CORRECT = Color.green;
    /**
     * constant for the color to signal a misprediction
     */
    public final static Color COLOR_PREDICTION_INCORRECT = Color.red;
    /**
     * constant for the String representing "take the branch"
     */
    public final static String BHT_TAKE_BRANCH = "TAKE";
    /**
     * constant for the String representing "do not take the branch"
     */
    public final static String BHT_DO_NOT_TAKE_BRANCH = "NOT TAKE";
    /**
     * the table representing the BHT
     */
    private final JTable bhtTable;
    /**
     * text field presenting the most recent branch instruction
     */
    private JTextField instructionTextField;
    /**
     * text field representing the address of the most recent branch instruction
     */
    private JTextField addressTextField;
    /**
     * text field representing the resulting BHT index of the branch instruction
     */
    private JTextField indexTextField;
    /**
     * combo box for selecting the number of BHT entries
     */
    private JComboBox<Integer> bhtEntries;
    /**
     * combo box for selecting the history size
     */
    private JComboBox<Integer> bhtHistory;
    /**
     * combo box for selecting the initial value
     */
    private JComboBox<String> bhtInitialValues;
    /**
     * text field for log output
     */
    private JTextArea logsTextArea;

    /**
     * Creates the GUI components of the BHT Simulator
     * The GUI is a subclass of JPanel which is integrated in the GUI of the RARS
     * tool
     */
    public BHTSimGUI() {
        super();
        final BorderLayout layout = new BorderLayout();
        layout.setVgap(10);
        layout.setHgap(10);
        setLayout(layout);

        bhtTable = createAndInitTable();

        add(buildConfigPanel(), BorderLayout.NORTH);
        add(buildInfoPanel(), BorderLayout.WEST);
        add(new JScrollPane(bhtTable), BorderLayout.CENTER);
        add(buildLogPanel(), BorderLayout.SOUTH);
    }

    /**
     * Creates and initializes the JTable representing the BHT.
     *
     * @return the JTable representing the BHT
     */
    private static JTable createAndInitTable() {
        // create the table
        final JTable theTable = new JTable();

        // create a default renderer for double values (percentage)
        final DefaultTableCellRenderer doubleRenderer = new DefaultTableCellRenderer() {
            private final DecimalFormat formatter = new DecimalFormat("##0.00");

            @Override
            public void setValue(final Object value) {
                setText((value == null) ? "" : formatter.format(value));
            }
        };
        doubleRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        // create a default renderer for all other values with center alignment
        final DefaultTableCellRenderer defRenderer = new DefaultTableCellRenderer();
        defRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        theTable.setDefaultRenderer(Double.class, doubleRenderer);
        theTable.setDefaultRenderer(Integer.class, defRenderer);
        theTable.setDefaultRenderer(String.class, defRenderer);

        theTable.setSelectionBackground(BHTSimGUI.COLOR_PREPREDICTION);
        theTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        return theTable;

    }

    /**
     * Creates and initializes the panel holding the instruction, address and index
     * text fields.
     *
     * @return the info panel
     */
    private JPanel buildInfoPanel() {
        instructionTextField = new JTextField();
        addressTextField = new JTextField();
        indexTextField = new JTextField();

        instructionTextField.setColumns(10);
        instructionTextField.setEditable(false);
        instructionTextField.setHorizontalAlignment(JTextField.CENTER);
        addressTextField.setColumns(10);
        addressTextField.setEditable(false);
        addressTextField.setHorizontalAlignment(JTextField.CENTER);
        indexTextField.setColumns(10);
        indexTextField.setEditable(false);
        indexTextField.setHorizontalAlignment(JTextField.CENTER);

        final JPanel panel = new JPanel();
        final JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BorderLayout());

        final GridBagLayout gbl = new GridBagLayout();
        panel.setLayout(gbl);

        final GridBagConstraints c = new GridBagConstraints();

        c.insets = new Insets(5, 5, 2, 5);
        c.gridx = 1;
        c.gridy = 1;

        panel.add(new JLabel("Instruction"), c);
        c.gridy++;
        panel.add(instructionTextField, c);
        c.gridy++;
        panel.add(new JLabel("@ Address"), c);
        c.gridy++;
        panel.add(addressTextField, c);
        c.gridy++;
        panel.add(new JLabel("-> Index"), c);
        c.gridy++;
        panel.add(indexTextField, c);

        outerPanel.add(panel, BorderLayout.NORTH);
        return outerPanel;
    }

    /**
     * Creates and initializes the panel for the configuration of the tool
     * The panel contains two combo boxes for selecting the number of BHT entries
     * and the history size.
     *
     * @return a panel for the configuration
     */
    private JPanel buildConfigPanel() {
        final JPanel panel = new JPanel();

        final var sizes = new Integer[]{8, 16, 32};

        final var bits = new Integer[]{1, 2};

        final var initialValues = new String[]{BHTSimGUI.BHT_DO_NOT_TAKE_BRANCH, BHTSimGUI.BHT_TAKE_BRANCH};

        bhtEntries = new JComboBox<>(sizes);
        bhtHistory = new JComboBox<>(bits);
        bhtInitialValues = new JComboBox<>(initialValues);

        panel.add(new JLabel("# of BHT entries"));
        panel.add(bhtEntries);
        panel.add(new JLabel("BHT history size"));
        panel.add(bhtHistory);
        panel.add(new JLabel("Initial value"));
        panel.add(bhtInitialValues);

        return panel;
    }

    /**
     * Creates and initializes the panel containing the log text area.
     *
     * @return the panel for the logging output
     */
    private JPanel buildLogPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        logsTextArea = new JTextArea();
        logsTextArea.setRows(6);
        logsTextArea.setEditable(false);

        panel.add(new JLabel("Log"), BorderLayout.NORTH);
        panel.add(new JScrollPane(logsTextArea), BorderLayout.CENTER);

        return panel;
    }

    /**
     * Returns the combo box for selecting the number of BHT entries.
     *
     * @return the reference to the combo box
     */
    public JComboBox<Integer> getCbBHTentries() {
        return bhtEntries;
    }

    /**
     * Returns the combo box for selecting the size of the BHT history.
     *
     * @return the reference to the combo box
     */
    public JComboBox<Integer> getCbBHThistory() {
        return bhtHistory;
    }

    /**
     * Returns the combo box for selecting the initial value of the BHT
     *
     * @return the reference to the combo box
     */
    public JComboBox<String> getCbBHTinitVal() {
        return bhtInitialValues;
    }

    /**
     * Returns the table representing the BHT.
     *
     * @return the reference to the table
     */
    public JTable getTabBHT() {
        return bhtTable;
    }

    /**
     * Returns the text area for log purposes.
     *
     * @return the reference to the text area
     */
    public JTextArea getTaLog() {
        return logsTextArea;
    }

    /**
     * Returns the text field for displaying the most recent branch instruction
     *
     * @return the reference to the text field
     */
    public JTextField getTfInstruction() {
        return instructionTextField;
    }

    /**
     * Returns the text field for displaying the address of the most recent branch
     * instruction
     *
     * @return the reference to the text field
     */
    public JTextField getTfAddress() {
        return addressTextField;
    }

    /**
     * Returns the text field for displaying the corresponding index into the BHT
     *
     * @return the reference to the text field
     */
    public JTextField getTfIndex() {
        return indexTextField;
    }

}
