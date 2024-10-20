/*
  Copyright (c) 2019, John Owens

  Developed by John Owens (jowens@ece.ucdavis.edu)

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

/*
 * Instructions for use
 * <p>
 * This tool allows you to generate a trace by doing the following:
 * <p>
 * Open your source file in RARS.
 * Tools menu, Instruction/Memory Dump.
 * Change filename to a filename of your choice.
 * Click button: Connect to Program
 * Run, Assemble.
 * Run, Go.
 * Go back to Instruction/Memory Dump window: click "Dump Log". This
 * saves the dump to the file you specified in step 3.
 * <p>
 * These steps are pretty brittle (i.e., do them in this exact order)
 * because the author doesn’t know how to use Swing very well.
 * <p>
 * The file you generate has one line per datum. The four kinds of
 * data you will see in the trace are:
 * <p>
 * ‘I': The address of an access into instruction memory
 * ‘i’: A 32-bit RISC-V instruction (the trace first dumps the address then
 * the instruction)
 * ‘L’: The address of a memory load into data memory
 * ‘S’: The address of a memory store into data memory (the contents of the
 * memory load/store aren’t in the trace)
 * <p>
 * The trace is in "text" mode for readability reasons, but for reducing
 * trace size, it would be possible to instead store it in a "binary" mode.
 */

package rars.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;
import rars.riscv.hardware.Memory;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Instruction/memory dump tool. Dumps every instruction run and every memory
 * access to a file.
 *
 * <p>
 * Code based on InstructionCounter.
 *
 * @author John Owens &lt;jowens@ece.ucdavis.edu&gt;
 */
public class InstructionMemoryDump extends AbstractTool {
    private static final Logger LOGGER = LogManager.getLogger(InstructionMemoryDump.class);
    private static final String name = "Instruction/Memory Dump";
    private static final String version = "Version 1.0 (John Owens)";
    private static final String heading = "Dumps every executed instruction and data memory access to a file";
    /**
     * Instructions and memory accesses get logged here
     */
    private final StringBuffer log = new StringBuffer();
    private final int lowDataSegmentAddress = Memory.dataSegmentBaseAddress;
    private final int highDataSegmentAddress = Memory.stackBaseAddress;
    /**
     * The last address we saw. We ignore it because the only way for a
     * program to execute twice the same instruction is to enter an infinite
     * loop, which is not insteresting in the POV of counting instructions.
     */
    private int lastAddress = -1;
    /**
     * Filename when we dump the log
     */
    private JTextField dumpLogFilename;
    private JLabel logSuccess;

    /**
     * Simple construction, likely used by the RARS Tools menu mechanism.
     */
    public InstructionMemoryDump() {
        super(InstructionMemoryDump.name + ", " + InstructionMemoryDump.version, InstructionMemoryDump.heading);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JComponent buildMainDisplayArea() {
        final JPanel panel = new JPanel(new FlowLayout());

        // Adds a "Dump Log" button, which, not surprisingly, dumps the log to a file
        final JButton dumpLogButton = new JButton("Dump Log");
        dumpLogButton.setToolTipText("Dumps the log to a file");
        dumpLogButton.addActionListener(
                e -> InstructionMemoryDump.this.dumpLog());
        dumpLogButton.addKeyListener(new EnterKeyListener(dumpLogButton));

        this.dumpLogFilename = new JTextField("dumplog.txt", 20);

        panel.add(dumpLogButton);
        panel.add(this.dumpLogFilename);

        this.logSuccess = new JLabel("");
        this.logSuccess.setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.logSuccess.setFocusable(false);
        this.logSuccess.setBackground(panel.getBackground());
        panel.add(this.logSuccess);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return InstructionMemoryDump.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAsObserver() {
        // watch the text segment (the program)
        this.addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
        // also watch the data segment
        this.addAsObserver(this.lowDataSegmentAddress, this.highDataSegmentAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processRISCVUpdate(final AccessNotice notice) {
        if (!notice.accessIsFromRISCV())
            return;
        // we've got two kinds of access here: instructions and data
        final MemoryAccessNotice m = (MemoryAccessNotice) notice;
        final int a = m.getAddress();

        // is a in the text segment (program)?
        if ((a >= Memory.textBaseAddress) && (a < Memory.textLimitAddress)) {
            if (notice.getAccessType() != AccessNotice.AccessType.READ)
                return;
            if (a == this.lastAddress)
                return;
            this.lastAddress = a;
            try {
                final ProgramStatement stmt = Memory.getInstance().getStatement(a);

                // If the program is finished, getStatement() will return null,
                // A null statement will cause the simulator to stall.
                if (stmt != null) {
                    // First dump the instruction address, prefixed by "I:"
                    this.log.append("I: 0x").append(Integer.toUnsignedString(a, 16)).append("\n");
                    // Then dump the instruction, prefixed by "i:"
                    this.log.append("i: 0x").append(Integer.toUnsignedString(stmt.getBinaryStatement(), 16)).append("\n");
                }
            } catch (final AddressErrorException e) {
                // TODO Auto-generated catch block
                InstructionMemoryDump.LOGGER.error("Error while trying to get statement at address {}", a, e);
            }
        }

        // is a in the data segment?
        if ((a >= this.lowDataSegmentAddress) && (a < this.highDataSegmentAddress)) {
            if (notice.getAccessType() == AccessNotice.AccessType.READ)
                this.log.append("L: 0x");
            if (notice.getAccessType() == AccessNotice.AccessType.WRITE)
                this.log.append("S: 0x");
            this.log.append(Integer.toUnsignedString(a, 16)).append("\n");
        }

        this.updateDisplay();
    }

    /**
     * <p>dumpLog.</p>
     */
    public void dumpLog() {
        // TODO: handle ressizing the window if the logSuccess label is not visible
        try {
            final String filename = this.dumpLogFilename.getText();
            if (filename.isEmpty()) {
                this.logSuccess.setText("Enter a filename before trying to dump log");
                return;
            }
            final File file = new File(filename);
            final String fullpath = file.getCanonicalPath();
            final BufferedWriter bwr = new BufferedWriter(new FileWriter(file));
            bwr.write(this.log.toString());
            bwr.flush();
            bwr.close();
            this.logSuccess.setText("Successfully dumped to " + fullpath);
        } catch (final IOException e) {
            this.logSuccess.setText("Failed to successfully dump. Cause: " + e.getMessage());
        }
        this.theWindow.pack();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        this.lastAddress = -1;
        this.logSuccess.setText("");
        this.updateDisplay();
    }


    /**
     * <p>getHelpComponent.</p>
     *
     * @return a {@link javax.swing.JComponent} object
     */
    @Override
    protected JComponent getHelpComponent() {
        final String helpContent = """
                 Generates a trace, to be stored in a file specified by the user, with one line per datum. The four kinds of data in the trace are:\s
                  - I: The address of an access into instruction memory\s
                  - i: A 32-bit RISC-V instruction (the trace first dumps the address then the instruction)
                  - L: The address of a memory load into data memory
                  - S: The address of a memory store into data memory (the contents of the memory load/store aren’t in the trace)
                """;
        final JButton help = new JButton("Help");
        help.addActionListener(
                e -> {
                    final JTextArea ja = new JTextArea(helpContent);
                    ja.setRows(20);
                    ja.setColumns(60);
                    ja.setLineWrap(true);
                    ja.setWrapStyleWord(true);
                    JOptionPane.showMessageDialog(InstructionMemoryDump.this.theWindow, new JScrollPane(ja),
                            "Log format", JOptionPane.INFORMATION_MESSAGE);
                });
        return help;
    }
}
