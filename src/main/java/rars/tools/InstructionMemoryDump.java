package rars.tools;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.logging.Logger;
import rars.logging.LoggingExtKt;
import rars.logging.RARSLogging;
import rars.notices.AccessNotice;
import rars.notices.AccessType;
import rars.notices.MemoryAccessNotice;
import rars.venus.VenusUI;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static rars.Globals.FONT_SETTINGS;

/**
 * Instruction/memory dump tool. Dumps every instruction run and every memory
 * access to a file.
 *
 * <p>
 * Code based on InstructionCounter.
 *
 * @author John Owens &lt;jowens@ece.ucdavis.edu&gt;
 */
public final class InstructionMemoryDump extends AbstractTool {
    private static final @NotNull Logger LOGGER = RARSLogging.forJavaClass(
        InstructionMemoryDump.class);
    private static final String NAME = "Instruction/Memory Dump";
    private static final String VERSION = "Version 1.0 (John Owens)";
    private static final String HEADING = "Dumps every executed instruction and data memory access to a file";

    /** Instructions and memory accesses get logged here */
    private final StringBuffer log = new StringBuffer();
    private final int lowDataSegmentAddress;
    private final int highDataSegmentAddress;
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
    public InstructionMemoryDump(final @NotNull VenusUI mainUI) {
        super(
            InstructionMemoryDump.NAME + ", " + InstructionMemoryDump.VERSION,
            InstructionMemoryDump.HEADING,
            mainUI
        );
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        this.lowDataSegmentAddress = rars.riscv.hardware.memory.MemoryConfigurationKt.getDataSegmentBaseAddress(
            memoryConfiguration);
        this.highDataSegmentAddress = rars.riscv.hardware.memory.MemoryConfigurationKt.getStackBaseAddress(
            memoryConfiguration);
    }

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
        this.logSuccess.setFont(FONT_SETTINGS.getCurrentFont());
        FONT_SETTINGS.onChangeListenerHook.subscribe(ignored -> {
            this.logSuccess.setFont(FONT_SETTINGS.getCurrentFont());
            return Unit.INSTANCE;
        });
        this.logSuccess.setFocusable(false);
        this.logSuccess.setBackground(panel.getBackground());
        panel.add(this.logSuccess);
        return panel;
    }

    @Override
    public String getName() {
        return InstructionMemoryDump.NAME;
    }

    @Override
    protected void addAsObserver() {
        // watch the text segment (the program)
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        this.addAsObserver(
            rars.riscv.hardware.memory.MemoryConfigurationKt.getTextSegmentBaseAddress(
                memoryConfiguration),
            rars.riscv.hardware.memory.MemoryConfigurationKt.getTextSegmentLimitAddress(
                memoryConfiguration)
        );
        // also watch the data segment
        this.addAsObserver(
            this.lowDataSegmentAddress,
            this.highDataSegmentAddress
        );
    }

    @Override
    protected void processRISCVUpdate(final AccessNotice notice) {
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        if (!notice.isAccessFromRISCV) {
            return;
        }
        // we've got two kinds of access here: instructions and data
        final MemoryAccessNotice m = (MemoryAccessNotice) notice;
        final int a = m.address;

        // is a in the text segment (program)?
        if ((
            a >= rars.riscv.hardware.memory.MemoryConfigurationKt.getTextSegmentBaseAddress(
                memoryConfiguration)
        ) && (
            a < rars.riscv.hardware.memory.MemoryConfigurationKt.getTextSegmentLimitAddress(
                memoryConfiguration)
        )) {
            if (notice.accessType != AccessType.READ) {
                return;
            }
            if (a == this.lastAddress) {
                return;
            }
            this.lastAddress = a;
            Globals.MEMORY_INSTANCE.getProgramStatement(a).fold(
                error -> {
                    LoggingExtKt.logError(LOGGER, () ->
                        "Error while trying to get a statement at address %d: %s".formatted(
                            a,
                            error
                        )
                    );
                    return Unit.INSTANCE;
                },
                stmt -> {
                    // If the program is finished, getStatement() will return null,
                    // A null statement will cause the simulator to stall.
                    if (stmt != null) {
                        // First dump the instruction address, prefixed by "I:"
                        this.log.append("I: 0x")
                            .append(Integer.toUnsignedString(a, 16))
                            .append('\n');
                        // Then dump the instruction, prefixed by "i:"
                        this.log.append("i: 0x")
                            .append(Integer.toUnsignedString(
                                stmt.getBinaryStatement(),
                                16
                            ))
                            .append('\n');
                    }
                    return Unit.INSTANCE;
                }
            );
        }

        // is a in the data segment?
        if ((a >= this.lowDataSegmentAddress) && (a < this.highDataSegmentAddress)) {
            if (notice.accessType == AccessType.READ) {
                this.log.append("L: 0x");
            }
            if (notice.accessType == AccessType.WRITE) {
                this.log.append("S: 0x");
            }
            this.log.append(Integer.toUnsignedString(a, 16)).append('\n');
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
                this.logSuccess.setText("Enter a file before trying to dump log");
                return;
            }
            final File file = new File(filename);
            final BufferedWriter bwr = new BufferedWriter(new FileWriter(file));
            bwr.write(this.log.toString());
            bwr.flush();
            bwr.close();
            final String fullpath = file.getCanonicalPath();
            this.logSuccess.setText("Successfully dumped to " + fullpath);
        } catch (final IOException e) {
            this.logSuccess.setText("Failed to successfully dump. Cause: " + e.getMessage());
        }
        this.theWindow.pack();
    }

    @Override
    protected void reset() {
        this.lastAddress = -1;
        this.logSuccess.setText("");
        this.updateDisplay();
    }

    /**
     * <p>getHelpComponent.</p>
     *
     * @return a {@link JComponent} object
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
                JOptionPane.showMessageDialog(
                    InstructionMemoryDump.this.theWindow, new JScrollPane(ja),
                    "Log format", JOptionPane.INFORMATION_MESSAGE
                );
            });
        return help;
    }
}
