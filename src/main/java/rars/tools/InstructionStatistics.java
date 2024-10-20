/*
Copyright (c) 2009,  Ingo Kofler, ITEC, Klagenfurt University, Austria

Developed by Ingo Kofler (ingo.kofler@itec.uni-klu.ac.at)
Based on the Instruction Counter tool by Felipe Lessa (felipe.lessa@gmail.com)

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
package rars.tools;

import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;
import rars.riscv.Instruction;
import rars.riscv.hardware.Memory;
import rars.riscv.instructions.*;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * A RARS tool for obtaining instruction statistics by instruction category.
 * <p>
 * The code of this tools is initially based on the Instruction counter tool by
 * Felipe Lassa.
 *
 * @author Ingo Kofler &lt;ingo.kofler@itec.uni-klu.ac.at&gt;
 */
public class InstructionStatistics extends AbstractTool {

    /**
     * name of the tool
     */
    private static final String NAME = "Instruction Statistics";

    /**
     * version and author information of the tool
     */
    private static final String VERSION = "Version 1.0 (Ingo Kofler)";

    /**
     * heading of the tool
     */
    private static final String HEADING = "";

    /**
     * number of instruction categories used by this tool
     */
    private static final int MAX_CATEGORY = 5;

    /**
     * constant for ALU instructions category
     */
    private static final int CATEGORY_ALU = 0;

    /**
     * constant for jump instructions category
     */
    private static final int CATEGORY_JUMP = 1;

    /**
     * constant for branch instructions category
     */
    private static final int CATEGORY_BRANCH = 2;

    /**
     * constant for memory instructions category
     */
    private static final int CATEGORY_MEM = 3;

    /**
     * constant for any other instruction category
     */
    private static final int CATEGORY_OTHER = 4;
    /**
     * array of counter variables - one for each instruction category
     */
    private final int[] m_counters = new int[InstructionStatistics.MAX_CATEGORY];
    /**
     * names of the instruction categories as array
     */
    private final String[] m_categoryLabels = {"ALU", "Jump", "Branch", "Memory", "Other"};
    /**
     * The last address we saw. We ignore it because the only way for a
     * program to execute twice the same instruction is to enter an infinite
     * loop, which is not insteresting in the POV of counting instructions.
     */
    protected int lastAddress = -1;
    /**
     * text field for visualizing the total number of instructions processed
     */
    private JTextField m_tfTotalCounter;
    /**
     * array of text field - one for each instruction category
     */
    private JTextField[] m_tfCounters;
    /**
     * array of progress pars - one for each instruction category
     */
    private JProgressBar[] m_pbCounters;

    // From Felipe Lessa's instruction counter. Prevent double-counting of
    // instructions
    // which happens because 2 read events are generated.
    /**
     * counter for the total number of instructions processed
     */
    private int m_totalCounter = 0;

    /**
     * Simple construction, likely used by the RARS Tools menu mechanism.
     */
    public InstructionStatistics() {
        super(InstructionStatistics.NAME + ", " + InstructionStatistics.VERSION, InstructionStatistics.HEADING);
    }

    /**
     * decodes the instruction and determines the category of the instruction.
     * <p>
     * The instruction is decoded by checking the java instance of the instruction.
     * Only the most relevant instructions are decoded and categorized.
     *
     * @param instruction the instruction to decode
     * @return the category of the instruction
     * @author Giancarlo Pernudi Segura
     * @see InstructionStatistics#CATEGORY_ALU
     * @see InstructionStatistics#CATEGORY_JUMP
     * @see InstructionStatistics#CATEGORY_BRANCH
     * @see InstructionStatistics#CATEGORY_MEM
     * @see InstructionStatistics#CATEGORY_OTHER
     */
    protected static int getInstructionCategory(final Instruction instruction) {
        if (instruction instanceof Arithmetic)
            return InstructionStatistics.CATEGORY_ALU; // add, addw, sub, subw, and, or, xor, slt, sltu, m extension
        if (instruction instanceof ADDI || instruction instanceof ADDIW || instruction instanceof ANDI
                || instruction instanceof ORI || instruction instanceof XORI
                || instruction instanceof SLTI || instruction instanceof SLTIU
                || instruction instanceof LUI || instruction instanceof AUIPC)
            return InstructionStatistics.CATEGORY_ALU; // addi, addiw, andi, ori, xori, slti, sltiu, lui, auipc
        if (instruction instanceof SLLI || instruction instanceof SLLI64 || instruction instanceof SLLIW)
            return InstructionStatistics.CATEGORY_ALU; // slli, slliw
        if (instruction instanceof SRLI || instruction instanceof SRLI64 || instruction instanceof SRLIW)
            return InstructionStatistics.CATEGORY_ALU; // srli, srliw
        if (instruction instanceof SRAI || instruction instanceof SRAI64 || instruction instanceof SRAIW)
            return InstructionStatistics.CATEGORY_ALU; // srai, sraiw
        if (instruction instanceof JAL || instruction instanceof JALR)
            return InstructionStatistics.CATEGORY_JUMP; // jal, jalr
        if (instruction instanceof Branch)
            return InstructionStatistics.CATEGORY_BRANCH; // beq, bge, bgeu, blt, bltu, bne
        if (instruction instanceof Load)
            return InstructionStatistics.CATEGORY_MEM; // lb, lh, lwl, lw, lbu, lhu, lwr
        if (instruction instanceof Store)
            return InstructionStatistics.CATEGORY_MEM; // sb, sh, swl, sw, swr

        return InstructionStatistics.CATEGORY_OTHER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return InstructionStatistics.NAME;
    }

    /**
     * creates the display area for the tool as required by the API
     *
     * @return a panel that holds the GUI of the tool
     */
    @Override
    protected JComponent buildMainDisplayArea() {

        // Create GUI elements for the tool
        final JPanel panel = new JPanel(new GridBagLayout());

        this.m_tfTotalCounter = new JTextField("0", 10);
        this.m_tfTotalCounter.setEditable(false);

        this.m_tfCounters = new JTextField[InstructionStatistics.MAX_CATEGORY];
        this.m_pbCounters = new JProgressBar[InstructionStatistics.MAX_CATEGORY];

        // for each category a text field and a progress bar is created
        for (int i = 0; i < InstructionStatistics.MAX_CATEGORY; i++) {
            this.m_tfCounters[i] = new JTextField("0", 10);
            this.m_tfCounters[i].setEditable(false);
            this.m_pbCounters[i] = new JProgressBar(JProgressBar.HORIZONTAL);
            this.m_pbCounters[i].setStringPainted(true);
        }

        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridheight = c.gridwidth = 1;

        // create the label and text field for the total instruction counter
        c.gridx = 2;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 17, 0);
        panel.add(new JLabel("Total: "), c);
        c.gridx = 3;
        panel.add(this.m_tfTotalCounter, c);

        c.insets = new Insets(3, 3, 3, 3);

        // create label, text field and progress bar for each category
        for (int i = 0; i < InstructionStatistics.MAX_CATEGORY; i++) {
            c.gridy++;
            c.gridx = 2;
            panel.add(new JLabel(this.m_categoryLabels[i] + ":   "), c);
            c.gridx = 3;
            panel.add(this.m_tfCounters[i], c);
            c.gridx = 4;
            panel.add(this.m_pbCounters[i], c);
        }

        return panel;
    }

    /**
     * registers the tool as observer for the text segment of the program
     */
    @Override
    protected void addAsObserver() {
        this.addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
    }

    /**
     * {@inheritDoc}
     * <p>
     * method that is called each time the simulator accesses the text segment.
     * Before an instruction is executed by the simulator, the instruction is
     * fetched from the program memory.
     * This memory access is observed and the corresponding instruction is decoded
     * and categorized by the tool.
     * According to the category the counter values are increased and the display
     * gets updated.
     */
    @Override
    protected void processRISCVUpdate(final AccessNotice notice) {

        if (!notice.accessIsFromRISCV())
            return;

        // check for a read access in the text segment
        if (notice.getAccessType() == AccessNotice.AccessType.READ && notice instanceof final MemoryAccessNotice memAccNotice) {

            // now it is safe to make a cast of the notice

            // The next three statments are from Felipe Lessa's instruction counter.
            // Prevents double-counting.
            final int a = memAccNotice.getAddress();
            if (a == this.lastAddress)
                return;
            this.lastAddress = a;

            try {

                // access the statement in the text segment without notifying other tools etc.
                final ProgramStatement stmt = Memory.getInstance().getStatementNoNotify(memAccNotice.getAddress());

                // necessary to handle possible null pointers at the end of the program
                // (e.g., if the simulator tries to execute the next instruction after the last
                // instruction in the text segment)
                if (stmt != null) {
                    final int category = InstructionStatistics.getInstructionCategory(stmt.getInstruction());

                    this.m_totalCounter++;
                    this.m_counters[category]++;
                    this.updateDisplay();
                }
            } catch (final AddressErrorException e) {
                // silently ignore these exceptions
            }
        }
    }

    /**
     * performs initialization tasks of the counters before the GUI is created.
     */
    @Override
    protected void initializePreGUI() {
        this.m_totalCounter = 0;
        this.lastAddress = -1; // from Felipe Lessa's instruction counter tool
        Arrays.fill(this.m_counters, 0);
    }

    /**
     * resets the counter values of the tool and updates the display.
     */
    @Override
    protected void reset() {
        this.m_totalCounter = 0;
        this.lastAddress = -1; // from Felipe Lessa's instruction counter tool
        Arrays.fill(this.m_counters, 0);
        this.updateDisplay();
    }

    /**
     * updates the text fields and progress bars according to the current counter
     * values.
     */
    @Override
    protected void updateDisplay() {
        this.m_tfTotalCounter.setText(String.valueOf(this.m_totalCounter));

        for (int i = 0; i < InstructionStatistics.MAX_CATEGORY; i++) {
            this.m_tfCounters[i].setText(String.valueOf(this.m_counters[i]));
            this.m_pbCounters[i].setMaximum(this.m_totalCounter);
            this.m_pbCounters[i].setValue(this.m_counters[i]);
        }
    }
}
