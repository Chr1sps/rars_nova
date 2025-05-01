package rars.tools;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.notices.AccessNotice;
import rars.notices.AccessType;
import rars.notices.MemoryAccessNotice;
import rars.riscv.instructions.Branch;
import rars.venus.VenusUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static rars.riscv.hardware.memory.MemoryConfigurationKt.getTextSegmentBaseAddress;
import static rars.riscv.hardware.memory.MemoryConfigurationKt.getTextSegmentLimitAddress;

/**
 * A RARS tool for simulating branch prediction with a Branch History Table
 * (BHT)
 * <p>
 * The simulation is based on observing the access to the instruction memory
 * area (text segment).
 * If a branch instruction is encountered, a prediction based on a BHT is
 * performed.
 * The outcome of the branch is compared with the prediction and the prediction
 * is updated accordingly.
 * Statistics about the correct and incorrect number of predictions can be
 * obtained for each BHT entry.
 * The number of entries in the BHT and the history that is considered for each
 * prediction can be configured interactively.
 * A change of the configuration however causes a re-initialization of the BHT.
 * <p>
 * The tool can be used to show how branch prediction works in case of loops and
 * how effective such simple methods are.
 * In case of nested loops the difference of BHT with 1 or 2 Bit history can be
 * explored and visualized.
 *
 * @author ingo.kofler@itec.uni-klu.ac.at
 */
public final class BHTSimulator extends AbstractTool implements ActionListener {
    /**
     * constant for the default size of the BHT
     */
    public static final int BHT_DEFAULT_SIZE = 16;

    /**
     * constant for the default history size
     */
    public static final int BHT_DEFAULT_HISTORY = 1;

    /**
     * constant for the default inital value
     */
    public static final boolean BHT_DEFAULT_INITVAL = false;

    /**
     * the name of the tool
     */
    public static final String BHT_NAME = "BHT Simulator";

    /**
     * the version of the tool
     */
    public static final String BHT_VERSION = "Version 1.0 (Ingo Kofler)";

    /**
     * the heading of the tool
     */
    public static final String BHT_HEADING = "Branch History Table Simulator";

    /**
     * the GUI of the BHT simulator
     */
    private BHTSimGUI myGui;

    /**
     * the model of the BHT
     */
    private BHTableModel myBhtModel;

    /**
     * state variable that indicates that the last instruction was a branch
     * instruction (if address != 0) or not (address == 0)
     */
    private int myPendingBranchInstAddress;

    /**
     * state variable that signals if the last branch was taken
     */
    private boolean myLastBranchTaken;

    /**
     * Creates a BHT Simulator with given name and heading.
     */
    public BHTSimulator(final @NotNull VenusUI mainUI) {
        super(BHTSimulator.BHT_NAME + ", " + BHTSimulator.BHT_VERSION, BHTSimulator.BHT_HEADING, mainUI);
    }

    /**
     * Extracts the target address of the branch.
     *
     * @param stmt
     *     the branch instruction
     * @return the address of the instruction that is executed if the branch is
     * taken
     */
    private static int extractBranchAddress(final ProgramStatement stmt) {
        assert stmt.getInstruction() instanceof Branch : "Should only be called on branch instructions";
        final int offset = stmt.getOperand(2);

        return stmt.getAddress() + (offset << 1);
    }

    /** Adds BHTSimulator as observer of the text segment. */
    @Override
    protected void addAsObserver() {
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        this.addAsObserver(
            getTextSegmentBaseAddress(memoryConfiguration),
            getTextSegmentLimitAddress(memoryConfiguration)
        );
        this.addAsObserver(Globals.REGISTER_FILE.pc);
    }

    /**
     * Creates a GUI and initialize the GUI with the default values.
     *
     * @return a {@link javax.swing.JComponent} object
     */
    @Override
    protected JComponent buildMainDisplayArea() {

        this.myGui = new BHTSimGUI();
        this.myBhtModel = new BHTableModel(
            BHTSimulator.BHT_DEFAULT_SIZE, BHTSimulator.BHT_DEFAULT_HISTORY,
            BHTSimulator.BHT_DEFAULT_INITVAL
        );

        this.myGui.getTabBHT().setModel(this.myBhtModel);
        this.myGui.getCbBHThistory().setSelectedItem(BHTSimulator.BHT_DEFAULT_HISTORY);
        this.myGui.getCbBHTentries().setSelectedItem(BHTSimulator.BHT_DEFAULT_SIZE);

        this.myGui.getCbBHTentries().addActionListener(this);
        this.myGui.getCbBHThistory().addActionListener(this);
        this.myGui.getCbBHTinitVal().addActionListener(this);

        return this.myGui;
    }

    @Override
    public String getName() {
        return BHTSimulator.BHT_NAME;
    }

    /**
     * Performs a reset of the simulator.
     * This causes the BHT to be reseted and the log messages to be cleared.
     */
    @Override
    protected void reset() {
        this.resetSimulator();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handles the actions when selecting another value in one of the two combo
     * boxes.
     * Selecting a different BHT size or history causes a reset of the simulator.
     */
    @Override
    public void actionPerformed(final ActionEvent event) {
        // change of the BHT size or BHT bit configuration
        // resets the simulator
        if (event.getSource() == this.myGui.getCbBHTentries() || event.getSource() == this.myGui.getCbBHThistory()
            || event.getSource() == this.myGui.getCbBHTinitVal()) {
            this.resetSimulator();
        }
    }

    /**
     * Resets the simulator by clearing the GUI elements and resetting the BHT.
     */
    private void resetSimulator() {
        this.myGui.getTfInstruction().setText("");
        this.myGui.getTfAddress().setText("");
        this.myGui.getTfIndex().setText("");
        this.myGui.getTaLog().setText("");
        this.myBhtModel.initBHT(
            (Integer) this.myGui.getCbBHTentries().getSelectedItem(),
            (Integer) this.myGui.getCbBHThistory().getSelectedItem(),
            this.myGui.getCbBHTinitVal().getSelectedItem().equals(BHTSimGUI.BHT_TAKE_BRANCH)
        );

        this.myPendingBranchInstAddress = 0;
        this.myLastBranchTaken = false;
    }

    /**
     * Handles the execution branch instruction.
     * This method is called each time a branch instruction is executed.
     * Based on the address of the instruction the corresponding index into the BHT
     * is calculated.
     * The prediction is obtained from the BHT at the calculated index and is
     * visualized appropriately.
     *
     * @param stmt
     *     the branch statement that is executed
     */
    private void handlePreBranchInst(final ProgramStatement stmt) {

        final String strStmt = stmt.getBasicAssemblyStatement();
        final int address = stmt.getAddress();
        final int idx = this.myBhtModel.getIdxForAddress(address);

        // update the GUI
        this.myGui.getTfInstruction().setText(strStmt);
        this.myGui.getTfAddress().setText("0x" + Integer.toHexString(address));
        this.myGui.getTfIndex().setText("" + idx);

        // mark the affected BHT row
        this.myGui.getTabBHT().setSelectionBackground(BHTSimGUI.COLOR_PREPREDICTION);
        this.myGui.getTabBHT().addRowSelectionInterval(idx, idx);

        // add output to log
        this.myGui.getTaLog().append("instruction " + strStmt + " at address 0x" + Integer.toHexString(address)
            + ", maps to index " + idx + '\n');
        this.myGui.getTaLog().append("branches to address 0x" + BHTSimulator.extractBranchAddress(stmt) + '\n');
        this.myGui.getTaLog()
            .append("prediction is: " + (this.myBhtModel.getPredictionAtIdx(idx) ? "take" : "do not take") + "...\n");
        this.myGui.getTaLog().setCaretPosition(this.myGui.getTaLog().getDocument().getLength());

    }

    /**
     * Handles the execution of the branch instruction.
     * The correctness of the prediction is visualized in both the table and the log
     * message area.
     * The BHT is updated based on the information if the branch instruction was
     * taken or not.
     *
     * @param branchInstAddr
     *     the address of the branch instruction
     * @param branchTaken
     *     the information if the branch is taken or not
     *     (determined in a step before)
     */
    private void handleExecBranchInst(final int branchInstAddr, final boolean branchTaken) {

        // determine the index in the BHT for the branch instruction
        final int idx = this.myBhtModel.getIdxForAddress(branchInstAddr);

        // check if the prediction is correct
        final boolean correctPrediction = this.myBhtModel.getPredictionAtIdx(idx) == branchTaken;

        this.myGui.getTabBHT().setSelectionBackground(
            correctPrediction ? BHTSimGUI.COLOR_PREDICTION_CORRECT : BHTSimGUI.COLOR_PREDICTION_INCORRECT);

        // add some output at the log
        this.myGui.getTaLog().append("branch " + (branchTaken ? "taken" : "not taken") + ", prediction was "
            + (correctPrediction ? "correct" : "incorrect") + "\n\n");
        this.myGui.getTaLog().setCaretPosition(this.myGui.getTaLog().getDocument().getLength());

        // update the BHT -> causes refresh of the table
        this.myBhtModel.updatePredictionAtIdx(idx, branchTaken);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Callback for text segment access by the RISCV simulator.
     * <p>
     * The method is called each time the text segment is accessed to fetch the next
     * instruction.
     * If the next instruction to execute was a branch instruction, the branch
     * prediction is performed and visualized.
     * In case the last instruction was a branch instruction, the outcome of the
     * branch prediction is analyzed and visualized.
     */
    @Override
    protected void processRISCVUpdate(final AccessNotice notice) {

        if (!notice.isAccessFromRISCV) {
            return;
        }

        if (notice.accessType == AccessType.READ && notice instanceof final MemoryAccessNotice memAccNotice) {

            // now it is safe to make a cast of the notice

            Globals.MEMORY_INSTANCE
                .getSilentMemoryView()
                .getProgramStatement(memAccNotice.address)
                .onRight(stmt -> {
                    // necessary to handle possible null pointers at the end of the program
                    // (e.g., if the simulator tries to execute the next instruction after the last
                    // instruction in the text segment)
                    if (stmt != null) {

                        boolean clearTextFields = true;

                        // first, check if there's a pending branch to handle
                        if (this.myPendingBranchInstAddress != 0) {
                            this.handleExecBranchInst(this.myPendingBranchInstAddress, this.myLastBranchTaken);
                            clearTextFields = false;
                            this.myPendingBranchInstAddress = 0;
                        }

                        // if current instruction is branch instruction
                        if (stmt.getInstruction() instanceof Branch) {
                            this.handlePreBranchInst(stmt);
                            this.myLastBranchTaken = ((Branch) stmt.getInstruction()).getWillBranch().invoke(
                                stmt,
                                Globals.REGISTER_FILE
                            );
                            this.myPendingBranchInstAddress = stmt.getAddress();
                            clearTextFields = false;
                        }

                        // clear text fields and selection
                        if (clearTextFields) {
                            this.myGui.getTfInstruction().setText("");
                            this.myGui.getTfAddress().setText("");
                            this.myGui.getTfIndex().setText("");
                            this.myGui.getTabBHT().clearSelection();
                        }
                    } else {
                        // check if there's a pending branch to handle
                        if (this.myPendingBranchInstAddress != 0) {
                            this.handleExecBranchInst(this.myPendingBranchInstAddress, this.myLastBranchTaken);
                            this.myPendingBranchInstAddress = 0;
                        }
                    }
                    return Unit.INSTANCE;
                });
        }
    }
}
