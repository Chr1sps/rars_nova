package rars.tools;

import kotlin.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;
import rars.util.BinaryUtilsKt;
import rars.venus.VenusUI;
import rars.venus.util.MouseListenerBuilder;

import javax.swing.*;
import java.awt.*;

import static rars.util.UtilsKt.unwrap;

enum DisplaySegment {
    A((byte) 0b00000001),
    B((byte) 0b00000010),
    C((byte) 0b00000100),
    D((byte) 0b00001000),
    E((byte) 0b00010000),
    F((byte) 0b00100000),
    G((byte) 0b01000000),
    DOT((byte) 0b10000000);

    public final byte bitMask;

    DisplaySegment(final byte bitMask) {
        this.bitMask = bitMask;
    }
}

/*
 * Didier Teifreto LIFC Universit� de franche-Comt�
 * www.lifc.univ-fcomte.fr/~teifreto
 * didier.teifreto@univ-fcomte.fr
 */
public final class DigitalLabSim extends AbstractTool {
    public static final int EXTERNAL_INTERRUPT_TIMER = 0x00000100;
    public static final int EXTERNAL_INTERRUPT_HEXA_KEYBOARD = 0x00000200;
    private static final Logger LOGGER = LogManager.getLogger(DigitalLabSim.class);
    private static final String HEADING = "Digital Lab Sim";
    private static final String VERSION = " Version 1.0 (Didier Teifreto)";
    // Counter
    private static final int MAX_COUNTER_VALUE = 30;
    // GUI Interface.
    private static JPanel panelTools;
    // Keyboard
    private static int KeyBoardValueButtonClick = -1; // -1 no button click
    private static boolean KeyboardInterruptOnOff = false;
    private static int CounterValue = DigitalLabSim.MAX_COUNTER_VALUE;
    private static boolean CounterInterruptOnOff = false;
    // Used to be static final variables now they are regenerated per instance
    private final int rightDisplayAddress, leftDisplayAddress, keyboardInAddress, counterAddress,
        keyboardOutAddress;
    // Seven Segment display
    private SevenSegmentPanel sevenSegPanel;
    private HexaKeyboard hexaKeyPanel;

    public DigitalLabSim(final String title, final String heading, final VenusUI mainUI) {
        super(title, heading, mainUI);

        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        this.rightDisplayAddress = memoryConfiguration.memoryMapBaseAddress + 0x10;
        this.leftDisplayAddress = memoryConfiguration.memoryMapBaseAddress + 0x11;
        this.keyboardInAddress = memoryConfiguration.memoryMapBaseAddress + 0x12;
        this.counterAddress = memoryConfiguration.memoryMapBaseAddress + 0x13;
        this.keyboardOutAddress = memoryConfiguration.memoryMapBaseAddress + 0x14;
    }

    public DigitalLabSim(final @NotNull VenusUI mainUI) {
        this(DigitalLabSim.HEADING + ", " + DigitalLabSim.VERSION, DigitalLabSim.HEADING, mainUI);
    }

    public static void updateOneSecondCounter(final byte value) {
        if (value != 0) {
            DigitalLabSim.CounterInterruptOnOff = true;
            DigitalLabSim.CounterValue = DigitalLabSim.MAX_COUNTER_VALUE;
        } else {
            DigitalLabSim.CounterInterruptOnOff = false;
        }
    }

    public static void resetOneSecondCounter() {
        DigitalLabSim.CounterInterruptOnOff = false;
        DigitalLabSim.CounterValue = DigitalLabSim.MAX_COUNTER_VALUE;
    }

    @Override
    public String getName() {
        return "Digital Lab Sim";
    }

    @Override
    protected void addAsObserver() {
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        this.addAsObserver(this.rightDisplayAddress, this.rightDisplayAddress);
        this.addAsObserver(memoryConfiguration.textBaseAddress, memoryConfiguration.textLimitAddress);
    }

    @Override
    public @NotNull Unit processAccessNotice(final @NotNull AccessNotice notice) {
        final var memNotice = (MemoryAccessNotice) notice;
        final int address = memNotice.address;
        final var value = (byte) memNotice.value;
        if (address == this.rightDisplayAddress) {
            this.updateSevenSegment(1, value);
        } else if (address == this.leftDisplayAddress) {
            this.updateSevenSegment(0, value);
        } else if (address == this.keyboardInAddress) {
            this.updateHexaKeyboard(value);
        } else if (address == this.counterAddress) {
            DigitalLabSim.updateOneSecondCounter(value);
        }
        if (DigitalLabSim.CounterInterruptOnOff) {
            if (DigitalLabSim.CounterValue > 0) {
                DigitalLabSim.CounterValue--;
            } else {
                DigitalLabSim.CounterValue = DigitalLabSim.MAX_COUNTER_VALUE;
                Globals.INTERRUPT_CONTROLLER.registerTimerInterrupt(DigitalLabSim.EXTERNAL_INTERRUPT_TIMER);
            }
        }
        return Unit.INSTANCE;
    }

    @Override
    protected void reset() {
        this.sevenSegPanel.resetSevenSegment();
        this.hexaKeyPanel.resetHexaKeyboard();
        resetOneSecondCounter();
    }

    @Override
    protected JComponent buildMainDisplayArea() {
        DigitalLabSim.panelTools = new JPanel(new GridLayout(1, 2));
        this.sevenSegPanel = new SevenSegmentPanel();
        DigitalLabSim.panelTools.add(this.sevenSegPanel);
        this.hexaKeyPanel = new HexaKeyboard();
        DigitalLabSim.panelTools.add(this.hexaKeyPanel);
        DigitalLabSim.CounterInterruptOnOff = false;
        return DigitalLabSim.panelTools;
    }

    private synchronized void updateMMIOControlAndData(final int dataAddr, final int dataValue) {
        if (this.connectButton.isConnected()) {
            Globals.MEMORY_REGISTERS_LOCK.lock();
            try {
                unwrap(Globals.MEMORY_INSTANCE.setByte(dataAddr, (byte) dataValue));
            } finally {
                Globals.MEMORY_REGISTERS_LOCK.unlock();
            }
            if (this.mainUI.mainPane.executePane.textSegment.getCodeHighlighting()) {
                this.mainUI.mainPane.executePane.dataSegment.updateValues();
            }
        }
    }

    @Override
    protected JComponent getHelpComponent() {
        final String helpContent = """
            This tool is composed of 3 parts : two seven-segment displays, a hexadecimal keyboard and a counter.
                Seven segment display
                Byte value at address %s : command right seven segment display
                Byte value at address %s : command left seven segment display
                Each bit of these two bytes are connected to segments (bit 0 for a segment, 1 for b segment and 7 for point
            
            Hexadecimal keyboard
                Byte value at address %s : command row number of hexadecimal keyboard (bit 0 to 3) and enable keyboard interrupt (bit 7)
                Byte value at address %s : receive row and column of the first pressed, 0 if not first pressed
                The program has to scan, one by one, each row (send 1,2,4,8...) and then observe if a first is pressed (that mean byte value at adresse 0xFFFF0014 is different from zero).  This byte value is composed of row number (4 left bits) and column number (4 right bits) Here you'll find the code for each first : 0x11,0x21,0x41,0x81,0x12,0x22,0x42,0x82,0x14,0x24,0x44,0x84,0x18,0x28,0x48,0x88.
                For exemple first number 2 return 0x41, that mean the first is on column 3 and row 1.
                If keyboard interruption is enable, an external interrupt is started with value 0x00000200
            
            Counter
                Byte value at address %s : If one bit of this byte is set, the counter interruption is enabled.
                If counter interruption is enable, every 30 instructions, a timer interrupt is started with value 0x00000100.
            (contributed by Didier Teifreto, dteifreto@lifc.univ-fcomte.fr)
            """.formatted(
            BinaryUtilsKt.intToHexStringWithPrefix(rightDisplayAddress),
            BinaryUtilsKt.intToHexStringWithPrefix(leftDisplayAddress),
            BinaryUtilsKt.intToHexStringWithPrefix(keyboardInAddress),
            BinaryUtilsKt.intToHexStringWithPrefix(keyboardOutAddress),
            BinaryUtilsKt.intToHexStringWithPrefix(counterAddress)
        );
        final var helpButton = new JButton("Help");
        helpButton.addActionListener(
            e -> {
                final var textArea = new JTextArea(helpContent);
                textArea.setRows(20);
                textArea.setColumns(60);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                JOptionPane.showMessageDialog(
                    DigitalLabSim.this.theWindow, new JScrollPane(textArea),
                    "Simulating the Hexa Keyboard and Seven segment display",
                    JOptionPane.INFORMATION_MESSAGE
                );
            });
        return helpButton;
    }

    public void updateSevenSegment(final int number, final byte value) {
        this.sevenSegPanel.display[number].modifyDisplay(value);
    }

    public void updateHexaKeyboard(final byte row) {
        final int key = DigitalLabSim.KeyBoardValueButtonClick;
        if ((key != -1) && ((1 << (key / 4)) == (row & 0xF))) {
            this.updateMMIOControlAndData(
                this.keyboardOutAddress,
                (char) (1 << (key / 4)) | (1 << (4 + (key % 4)))
            );
        } else {
            this.updateMMIOControlAndData(this.keyboardOutAddress, 0);
        }
        DigitalLabSim.KeyboardInterruptOnOff = (row & 0xF0) != 0;
    }

    public static class SevenSegmentDisplay extends JComponent {
        public byte displaySegmentBits;

        public SevenSegmentDisplay(final byte displaySegmentBits) {
            super();
            this.displaySegmentBits = displaySegmentBits;
            this.setPreferredSize(new Dimension(60, 80));
        }

        static void paintSegment(final Graphics g, final @NotNull DisplaySegment segment) {
            switch (segment) {
                case A -> {
                    final int[] pxa1 = {12, 9, 12};
                    final int[] pya = {5, 8, 11};
                    g.fillPolygon(pxa1, pya, 3);
                    final int[] pxa2 = {36, 39, 36};
                    g.fillPolygon(pxa2, pya, 3);
                    g.fillRect(12, 5, 24, 6);
                }
                case B -> {
                    final int[] pxb = {37, 40, 43};
                    final int[] pyb1 = {12, 9, 12};
                    g.fillPolygon(pxb, pyb1, 3);
                    final int[] pyb2 = {36, 39, 36};
                    g.fillPolygon(pxb, pyb2, 3);
                    g.fillRect(37, 12, 6, 24);
                }
                case C -> {
                    final int[] pxc = {37, 40, 43};
                    final int[] pyc1 = {44, 41, 44};
                    g.fillPolygon(pxc, pyc1, 3);
                    final int[] pyc2 = {68, 71, 68};
                    g.fillPolygon(pxc, pyc2, 3);
                    g.fillRect(37, 44, 6, 24);
                }
                case D -> {
                    final int[] pxd1 = {12, 9, 12};
                    final int[] pyd = {69, 72, 75};
                    g.fillPolygon(pxd1, pyd, 3);
                    final int[] pxd2 = {36, 39, 36};
                    g.fillPolygon(pxd2, pyd, 3);
                    g.fillRect(12, 69, 24, 6);
                }
                case E -> {
                    final int[] pxe = {5, 8, 11};
                    final int[] pye1 = {44, 41, 44};
                    g.fillPolygon(pxe, pye1, 3);
                    final int[] pye2 = {68, 71, 68};
                    g.fillPolygon(pxe, pye2, 3);
                    g.fillRect(5, 44, 6, 24);
                }
                case F -> {
                    final int[] pxf = {5, 8, 11};
                    final int[] pyf1 = {12, 9, 12};
                    g.fillPolygon(pxf, pyf1, 3);
                    final int[] pyf2 = {36, 39, 36};
                    g.fillPolygon(pxf, pyf2, 3);
                    g.fillRect(5, 12, 6, 24);
                }
                case G -> {
                    final int[] pxg1 = {12, 9, 12};
                    final int[] pyg = {37, 40, 43};
                    g.fillPolygon(pxg1, pyg, 3);
                    final int[] pxg2 = {36, 39, 36};
                    g.fillPolygon(pxg2, pyg, 3);
                    g.fillRect(12, 37, 24, 6);
                }
                case DOT -> g.fillOval(49, 68, 8, 8);
            }
        }

        public void modifyDisplay(final byte val) {
            this.displaySegmentBits = val;
            this.repaint();
        }

        @Override
        public void paint(final Graphics g) {
            for (final var segment : DisplaySegment.values()) {
                if ((this.displaySegmentBits & 0x1) == 1) {
                    g.setColor(Color.RED);
                } else {
                    g.setColor(Color.LIGHT_GRAY);
                }
                SevenSegmentDisplay.paintSegment(g, segment);
            }
        }
    }

    public static class SevenSegmentPanel extends JPanel {
        public final SevenSegmentDisplay[] display;

        public SevenSegmentPanel() {
            super();
            final FlowLayout fl = new FlowLayout();
            this.setLayout(fl);
            this.display = new SevenSegmentDisplay[2];
            for (int i = 0; i < 2; i++) {
                this.display[i] = new SevenSegmentDisplay((byte) (0));
                this.add(this.display[i]);
            }
        }

        public void modifyDisplay(final int num, final byte val) {
            this.display[num].modifyDisplay(val);
            this.display[num].repaint();
        }

        public void resetSevenSegment() {
            for (int i = 0; i < 2; i++) {
                this.modifyDisplay(i, (byte) 0);
            }
        }
    }

    public class HexaKeyboard extends JPanel {
        public final JButton[] button;

        public HexaKeyboard() {
            super();
            final GridLayout layout = new GridLayout(4, 4);
            this.setLayout(layout);
            this.button = new JButton[16];
            for (int i = 0; i < 16; i++) {
                final var button = new JButton(Integer.toHexString(i));
                button.setBackground(Color.WHITE);
                button.setMargin(new Insets(10, 10, 10, 10));
                final var keyboardClickValue = i;
                button.addMouseListener(MouseListenerBuilder.create().onMouseClicked(e -> {
                    if (DigitalLabSim.KeyBoardValueButtonClick != -1) {
                        // Button already pressed -> now release
                        DigitalLabSim.KeyBoardValueButtonClick = -1;
                        DigitalLabSim.this.updateMMIOControlAndData(DigitalLabSim.this.keyboardOutAddress, 0);
                        for (final var btn : HexaKeyboard.this.button) {
                            btn.setBackground(Color.WHITE);
                        }
                    } else {
                        // new button pressed
                        DigitalLabSim.KeyBoardValueButtonClick = keyboardClickValue;
                        HexaKeyboard.this.button[DigitalLabSim.KeyBoardValueButtonClick].setBackground(Color.GREEN);
                        if (DigitalLabSim.KeyboardInterruptOnOff) {
                            Globals.INTERRUPT_CONTROLLER.registerExternalInterrupt(DigitalLabSim.EXTERNAL_INTERRUPT_HEXA_KEYBOARD);
                        }

                    }
                    return Unit.INSTANCE;
                }).build());
                this.button[i] = button;
                this.add(button);
            }
        }

        public void resetHexaKeyboard() {
            DigitalLabSim.KeyBoardValueButtonClick = -1;
            for (int i = 0; i < 16; i++) {
                this.button[i].setBackground(Color.WHITE);
            }
        }

    }
}