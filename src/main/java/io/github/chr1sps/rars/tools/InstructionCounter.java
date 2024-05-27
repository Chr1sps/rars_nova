/*
Copyright (c) 2008,  Felipe Lessa

Developed by Felipe Lessa (felipe.lessa@gmail.com)

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
package io.github.chr1sps.rars.tools;

import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.exceptions.AddressErrorException;
import io.github.chr1sps.rars.notices.AccessNotice;
import io.github.chr1sps.rars.notices.MemoryAccessNotice;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.BasicInstructionFormat;
import io.github.chr1sps.rars.riscv.hardware.Memory;

import javax.swing.*;
import java.awt.*;

/**
 * Instruction counter tool. Can be used to know how many instructions
 * were executed to complete a given program.
 * <p>
 * Code slightly based on MemoryReferenceVisualization.
 *
 * @author Felipe Lessa &lt;felipe.lessa@gmail.com&gt;
 */
public class InstructionCounter extends AbstractToolAndApplication {
    private static final String name = "Instruction Counter";
    private static final String version = "Version 1.0 (Felipe Lessa)";
    private static final String heading = "Counting the number of instructions executed";

    /**
     * Number of instructions executed until now.
     */
    private int counter = 0;
    private JTextField counterField;

    /**
     * Number of instructions of type R.
     */
    private int counterR = 0;
    private JTextField counterRField;
    private JProgressBar progressbarR;

    /**
     * Number of instructions of type R4.
     */
    private int counterR4 = 0;
    private JTextField counterR4Field;
    private JProgressBar progressbarR4;

    /**
     * Number of instructions of type I.
     */
    private int counterI = 0;
    private JTextField counterIField;
    private JProgressBar progressbarI;

    /**
     * Number of instructions of type S.
     */
    private int counterS = 0;
    private JTextField counterSField;
    private JProgressBar progressbarS;

    /**
     * Number of instructions of type B.
     */
    private int counterB = 0;
    private JTextField counterBField;
    private JProgressBar progressbarB;

    /**
     * Number of instructions of type U.
     */
    private int counterU = 0;
    private JTextField counterUField;
    private JProgressBar progressbarU;

    /**
     * Number of instructions of type J.
     */
    private int counterJ = 0;
    private JTextField counterJField;
    private JProgressBar progressbarJ;

    /**
     * The last address we saw. We ignore it because the only way for a
     * program to execute twice the same instruction is to enter an infinite
     * loop, which is not insteresting in the POV of counting instructions.
     */
    private int lastAddress = -1;

    /**
     * Simple constructor, likely used to run a stand-alone memory reference
     * visualizer.
     *
     * @param title   String containing title for title bar
     * @param heading String containing text for heading shown in upper part of
     *                window.
     */
    public InstructionCounter(final String title, final String heading) {
        super(title, heading);
    }

    /**
     * Simple construction, likely used by the RARS Tools menu mechanism.
     */
    public InstructionCounter() {
        super(InstructionCounter.name + ", " + InstructionCounter.version, InstructionCounter.heading);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return InstructionCounter.name;
    }

    @Override
    protected JComponent buildMainDisplayArea() {
        // Create everything
        final JPanel panel = new JPanel(new GridBagLayout());

        this.counterField = new JTextField("0", 10);
        this.counterField.setEditable(false);

        this.counterRField = new JTextField("0", 10);
        this.counterRField.setEditable(false);
        this.progressbarR = new JProgressBar(JProgressBar.HORIZONTAL);
        this.progressbarR.setStringPainted(true);

        this.counterR4Field = new JTextField("0", 10);
        this.counterR4Field.setEditable(false);
        this.progressbarR4 = new JProgressBar(JProgressBar.HORIZONTAL);
        this.progressbarR4.setStringPainted(true);

        this.counterIField = new JTextField("0", 10);
        this.counterIField.setEditable(false);
        this.progressbarI = new JProgressBar(JProgressBar.HORIZONTAL);
        this.progressbarI.setStringPainted(true);

        this.counterSField = new JTextField("0", 10);
        this.counterSField.setEditable(false);
        this.progressbarS = new JProgressBar(JProgressBar.HORIZONTAL);
        this.progressbarS.setStringPainted(true);

        this.counterBField = new JTextField("0", 10);
        this.counterBField.setEditable(false);
        this.progressbarB = new JProgressBar(JProgressBar.HORIZONTAL);
        this.progressbarB.setStringPainted(true);

        this.counterUField = new JTextField("0", 10);
        this.counterUField.setEditable(false);
        this.progressbarU = new JProgressBar(JProgressBar.HORIZONTAL);
        this.progressbarU.setStringPainted(true);

        this.counterJField = new JTextField("0", 10);
        this.counterJField.setEditable(false);
        this.progressbarJ = new JProgressBar(JProgressBar.HORIZONTAL);
        this.progressbarJ.setStringPainted(true);

        // Add them to the panel

        // Fields
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridheight = c.gridwidth = 1;
        c.gridx = 3;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 17, 0);
        panel.add(this.counterField, c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridy++;
        panel.add(this.counterRField, c);

        c.gridy++;
        panel.add(this.counterR4Field, c);

        c.gridy++;
        panel.add(this.counterIField, c);

        c.gridy++;
        panel.add(this.counterSField, c);

        c.gridy++;
        panel.add(this.counterBField, c);

        c.gridy++;
        panel.add(this.counterUField, c);

        c.gridy++;
        panel.add(this.counterJField, c);

        // Labels
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 1;
        c.gridwidth = 2;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 17, 0);
        panel.add(new JLabel("Instructions so far: "), c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 2;
        c.gridwidth = 1;
        c.gridy++;
        panel.add(new JLabel("R-type: "), c);

        c.gridy++;
        panel.add(new JLabel("R4-type: "), c);

        c.gridy++;
        panel.add(new JLabel("I-type: "), c);

        c.gridy++;
        panel.add(new JLabel("S-type: "), c);

        c.gridy++;
        panel.add(new JLabel("B-type: "), c);

        c.gridy++;
        panel.add(new JLabel("U-type: "), c);

        c.gridy++;
        panel.add(new JLabel("J-type: "), c);

        // Progress bars
        c.insets = new Insets(3, 3, 3, 3);
        c.gridx = 4;
        c.gridy = 2;
        panel.add(this.progressbarR, c);

        c.gridy++;
        panel.add(this.progressbarR4, c);

        c.gridy++;
        panel.add(this.progressbarI, c);

        c.gridy++;
        panel.add(this.progressbarS, c);

        c.gridy++;
        panel.add(this.progressbarB, c);

        c.gridy++;
        panel.add(this.progressbarU, c);

        c.gridy++;
        panel.add(this.progressbarJ, c);

        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAsObserver() {
        this.addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processRISCVUpdate(final AccessNotice notice) {
        if (!notice.accessIsFromRISCV())
            return;
        if (notice.getAccessType() != AccessNotice.READ)
            return;
        final MemoryAccessNotice m = (MemoryAccessNotice) notice;
        final int a = m.getAddress();
        if (a == this.lastAddress)
            return;
        this.lastAddress = a;
        this.counter++;
        try {
            final ProgramStatement stmt = Memory.getInstance().getStatement(a);

            // If the program is finished, getStatement() will return null,
            // a null statement will cause the simulator to stall.
            if (stmt != null) {
                final BasicInstruction instr = (BasicInstruction) stmt.getInstruction();
                final BasicInstructionFormat format = instr.getInstructionFormat();
                if (format == BasicInstructionFormat.R_FORMAT)
                    this.counterR++;
                else if (format == BasicInstructionFormat.R4_FORMAT)
                    this.counterR4++;
                else if (format == BasicInstructionFormat.I_FORMAT)
                    this.counterI++;
                else if (format == BasicInstructionFormat.S_FORMAT)
                    this.counterS++;
                else if (format == BasicInstructionFormat.B_FORMAT)
                    this.counterB++;
                else if (format == BasicInstructionFormat.U_FORMAT)
                    this.counterU++;
                else if (format == BasicInstructionFormat.J_FORMAT)
                    this.counterJ++;
            }
        } catch (final AddressErrorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.updateDisplay();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializePreGUI() {
        this.counter = this.counterR = this.counterR4 = this.counterI = this.counterS = this.counterB = this.counterU = this.counterJ = 0;
        this.lastAddress = -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        this.counter = this.counterR = this.counterR4 = this.counterI = this.counterS = this.counterB = this.counterU = this.counterJ = 0;
        this.lastAddress = -1;
        this.updateDisplay();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateDisplay() {
        this.counterField.setText(String.valueOf(this.counter));

        this.counterRField.setText(String.valueOf(this.counterR));
        this.progressbarR.setMaximum(this.counter);
        this.progressbarR.setValue(this.counterR);

        this.counterR4Field.setText(String.valueOf(this.counterR4));
        this.progressbarR4.setMaximum(this.counter);
        this.progressbarR4.setValue(this.counterR4);

        this.counterIField.setText(String.valueOf(this.counterI));
        this.progressbarI.setMaximum(this.counter);
        this.progressbarI.setValue(this.counterI);

        this.counterSField.setText(String.valueOf(this.counterS));
        this.progressbarS.setMaximum(this.counter);
        this.progressbarS.setValue(this.counterS);

        this.counterBField.setText(String.valueOf(this.counterB));
        this.progressbarB.setMaximum(this.counter);
        this.progressbarB.setValue(this.counterB);

        this.counterUField.setText(String.valueOf(this.counterU));
        this.progressbarU.setMaximum(this.counter);
        this.progressbarU.setValue(this.counterU);

        this.counterJField.setText(String.valueOf(this.counterJ));
        this.progressbarJ.setMaximum(this.counter);
        this.progressbarJ.setValue(this.counterJ);

        if (this.counter == 0) {
            this.progressbarR.setString("0%");
            this.progressbarR4.setString("0%");
            this.progressbarI.setString("0%");
            this.progressbarS.setString("0%");
            this.progressbarB.setString("0%");
            this.progressbarU.setString("0%");
            this.progressbarJ.setString("0%");
        } else {
            this.progressbarR.setString((this.counterR * 100) / this.counter + "%");
            this.progressbarR4.setString((this.counterR4 * 100) / this.counter + "%");
            this.progressbarI.setString((this.counterI * 100) / this.counter + "%");
            this.progressbarS.setString((this.counterS * 100) / this.counter + "%");
            this.progressbarB.setString((this.counterB * 100) / this.counter + "%");
            this.progressbarU.setString((this.counterU * 100) / this.counter + "%");
            this.progressbarJ.setString((this.counterJ * 100) / this.counter + "%");
        }
    }
}
