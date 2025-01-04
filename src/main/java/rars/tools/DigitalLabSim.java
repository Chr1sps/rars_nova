package rars.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.exceptions.AddressErrorException;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;
import rars.riscv.hardware.InterruptController;
import rars.util.BinaryUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * <p>DigitalLabSim class.</p>
 */
/*
 * Didier Teifreto LIFC Universit� de franche-Comt�
 * www.lifc.univ-fcomte.fr/~teifreto
 * didier.teifreto@univ-fcomte.fr
 */
public class DigitalLabSim extends AbstractTool {
    /**
     * Constant <code>EXTERNAL_INTERRUPT_TIMER=0x00000100</code>
     */
    public static final int EXTERNAL_INTERRUPT_TIMER = 0x00000100;
    /**
     * Constant <code>EXTERNAL_INTERRUPT_HEXA_KEYBOARD=0x00000200</code>
     */
    public static final int EXTERNAL_INTERRUPT_HEXA_KEYBOARD = 0x00000200;
    private static final Logger LOGGER = LogManager.getLogger(DigitalLabSim.class);
    private static final String heading = "Digital Lab Sim";
    private static final String version = " Version 1.0 (Didier Teifreto)";
    // Counter
    private static final int CounterValueMax = 30;
    // GUI Interface.
    private static JPanel panelTools;
    // Keyboard
    private static int KeyBoardValueButtonClick = -1; // -1 no button click
    private static boolean KeyboardInterruptOnOff = false;
    private static int CounterValue = DigitalLabSim.CounterValueMax;
    private static boolean CounterInterruptOnOff = false;
    // Used to be static final variables now they are regenerated per instance
    private final int IN_ADRESS_DISPLAY_1, IN_ADRESS_DISPLAY_2, IN_ADRESS_HEXA_KEYBOARD, IN_ADRESS_COUNTER,
        OUT_ADRESS_HEXA_KEYBOARD;
    // Seven Segment display
    private SevenSegmentPanel sevenSegPanel;
    private HexaKeyboard hexaKeyPanel;

    /**
     * <p>Constructor for DigitalLabSim.</p>
     *
     * @param title
     *     a {@link java.lang.String} object
     * @param heading
     *     a {@link java.lang.String} object
     */
    public DigitalLabSim(final String title, final String heading) {
        super(title, heading);

        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        this.IN_ADRESS_DISPLAY_1 = memoryConfiguration.memoryMapBaseAddress + 0x10;
        this.IN_ADRESS_DISPLAY_2 = memoryConfiguration.memoryMapBaseAddress + 0x11;
        this.IN_ADRESS_HEXA_KEYBOARD = memoryConfiguration.memoryMapBaseAddress + 0x12;
        this.IN_ADRESS_COUNTER = memoryConfiguration.memoryMapBaseAddress + 0x13;
        this.OUT_ADRESS_HEXA_KEYBOARD = memoryConfiguration.memoryMapBaseAddress + 0x14;
    }

    public DigitalLabSim() {
        this(DigitalLabSim.heading + ", " + DigitalLabSim.version, DigitalLabSim.heading);
    }

    public static void updateOneSecondCounter(final char value) {
        if (value != 0) {
            DigitalLabSim.CounterInterruptOnOff = true;
            DigitalLabSim.CounterValue = DigitalLabSim.CounterValueMax;
        } else {
            DigitalLabSim.CounterInterruptOnOff = false;
        }
    }

    public static void resetOneSecondCounter() {
        DigitalLabSim.CounterInterruptOnOff = false;
        DigitalLabSim.CounterValue = DigitalLabSim.CounterValueMax;
    }

    @Override
    public String getName() {
        return "Digital Lab Sim";
    }

    @Override
    protected void addAsObserver() {
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        this.addAsObserver(this.IN_ADRESS_DISPLAY_1, this.IN_ADRESS_DISPLAY_1);
        this.addAsObserver(memoryConfiguration.textBaseAddress, memoryConfiguration.textLimitAddress);
    }

    @Override
    public void onNext(final @NotNull AccessNotice notice) {
        final var memNotice = (MemoryAccessNotice) notice;
        final int address = memNotice.getAddress();
        final char value = (char) memNotice.getValue();
        if (address == this.IN_ADRESS_DISPLAY_1) {
            this.updateSevenSegment(1, value);
        } else if (address == this.IN_ADRESS_DISPLAY_2) {
            this.updateSevenSegment(0, value);
        } else if (address == this.IN_ADRESS_HEXA_KEYBOARD) {
            this.updateHexaKeyboard(value);
        } else if (address == this.IN_ADRESS_COUNTER) {
            DigitalLabSim.updateOneSecondCounter(value);
        }
        if (DigitalLabSim.CounterInterruptOnOff) {
            if (DigitalLabSim.CounterValue > 0) {
                DigitalLabSim.CounterValue--;
            } else {
                DigitalLabSim.CounterValue = DigitalLabSim.CounterValueMax;
                InterruptController.registerTimerInterrupt(DigitalLabSim.EXTERNAL_INTERRUPT_TIMER);
            }
        }
        this.subscription.request(1);
    }

    /**
     * <p>reset.</p>
     */
    @Override
    protected void reset() {
        this.sevenSegPanel.resetSevenSegment();
        this.hexaKeyPanel.resetHexaKeyboard();
        resetOneSecondCounter();
    }

    /**
     * <p>buildMainDisplayArea.</p>
     *
     * @return a {@link javax.swing.JComponent} object
     */
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

    /*
     * ...........................Seven segment display start here
     * ..............................
     */

    private synchronized void updateMMIOControlAndData(final int dataAddr, final int dataValue) {
        if (this.connectButton.isConnected()) {
            Globals.memoryAndRegistersLock.lock();
            try {
                try {
                    Globals.MEMORY_INSTANCE.setByte(dataAddr, dataValue);
                } catch (final AddressErrorException aee) {
                    DigitalLabSim.LOGGER.fatal("Tool author specified incorrect MMIO address!", aee);
                    System.exit(0);
                }
            } finally {
                Globals.memoryAndRegistersLock.unlock();
            }
            if (Globals.gui != null) {
                if (Globals.gui.mainPane.executeTab.textSegment.getCodeHighlighting()) {
                    Globals.gui.mainPane.executeTab.dataSegment.updateValues();
                }
            }
        }
    }

    /**
     * <p>getHelpComponent.</p>
     *
     * @return a {@link javax.swing.JComponent} object
     */
    @Override
    protected JComponent getHelpComponent() {
        final String helpContent = " This tool is composed of 3 parts : two seven-segment displays, an hexadecimal " +
            "keyboard and counter \n"
            +
            "Seven segment display\n" +
            " Byte second at address " + BinaryUtils.intToHexString(this.IN_ADRESS_DISPLAY_1)
            + " : command right seven segment display \n " +
            " Byte second at address " + BinaryUtils.intToHexString(this.IN_ADRESS_DISPLAY_2)
            + " : command left seven segment display \n " +
            " Each bit of these two bytes are connected to segments (bit 0 for a segment, 1 for b segment and 7 " +
            "for point \n \n"
            +
            "Hexadecimal keyboard\n" +
            " Byte second at address " + BinaryUtils.intToHexString(this.IN_ADRESS_HEXA_KEYBOARD)
            + " : command row number of hexadecimal keyboard (bit 0 to 3) and enable keyboard interrupt (bit 7) \n"
            +
            " Byte second at address " + BinaryUtils.intToHexString(this.OUT_ADRESS_HEXA_KEYBOARD)
            + " : receive row and column of the first pressed, 0 if not first pressed \n" +
            " The program has to scan, one by one, each row (send 1,2,4,8...)" +
            " and then observe if a first is pressed (that mean byte second at adresse 0xFFFF0014 is different " +
            "from zero). "
            +
            " This byte second is composed of row number (4 left bits) and column number (4 right bits)" +
            " Here you'll find the code for each first : 0x11,0x21,0x41,0x81,0x12,0x22,0x42,0x82,0x14,0x24,0x44," +
            "0x84,0x18,0x28,0x48,0x88. \n"
            +
            " For exemple first number 2 return 0x41, that mean the first is on column 3 and row 1. \n" +
            " If keyboard interruption is enable, an external interrupt is started with second 0x00000200\n \n" +
            "Counter\n" +
            " Byte second at address " + BinaryUtils.intToHexString(this.IN_ADRESS_COUNTER)
            + " : If one bit of this byte is set, the counter interruption is enabled.\n" +
            " If counter interruption is enable, every 30 instructions, a timer interrupt is started with second " +
            "0x00000100.\n"
            +
            "   (contributed by Didier Teifreto, dteifreto@lifc.univ-fcomte.fr)";
        final JButton help = new JButton("Help");
        help.addActionListener(
            e -> {
                final JTextArea ja = new JTextArea(helpContent);
                ja.setRows(20);
                ja.setColumns(60);
                ja.setLineWrap(true);
                ja.setWrapStyleWord(true);
                JOptionPane.showMessageDialog(
                    DigitalLabSim.this.theWindow, new JScrollPane(ja),
                    "Simulating the Hexa Keyboard and Seven segment display",
                    JOptionPane.INFORMATION_MESSAGE
                );
            });
        return help;
    }/*
     * ....................Seven Segment display start
     * here...................................
     */

    /**
     * <p>updateSevenSegment.</p>
     *
     * @param number
     *     a int
     * @param value
     *     a char
     */
    public void updateSevenSegment(final int number, final char value) {
        this.sevenSegPanel.display[number].modifyDisplay(value);
    }

    /*
     * ...........................Seven segment display end here
     * ..............................
     */
    /*
     * ....................Hexa Keyboard start
     * here...................................
     */

    /**
     * <p>updateHexaKeyboard.</p>
     *
     * @param row
     *     a char
     */
    public void updateHexaKeyboard(final char row) {
        final int key = DigitalLabSim.KeyBoardValueButtonClick;
        if ((key != -1) && ((1 << (key / 4)) == (row & 0xF))) {
            this.updateMMIOControlAndData(
                this.OUT_ADRESS_HEXA_KEYBOARD,
                (char) (1 << (key / 4)) | (1 << (4 + (key % 4)))
            );
        } else {
            this.updateMMIOControlAndData(this.OUT_ADRESS_HEXA_KEYBOARD, 0);
        }
        DigitalLabSim.KeyboardInterruptOnOff = (row & 0xF0) != 0;
    }

    public static class SevenSegmentDisplay extends JComponent {
        public char aff;

        public SevenSegmentDisplay(final char aff) {
            this.aff = aff;
            this.setPreferredSize(new Dimension(60, 80));
        }

        public static void SwitchSegment(final Graphics g, final char segment) {
            switch (segment) {
                case 'a': // a segment
                    final int[] pxa1 = {12, 9, 12};
                    final int[] pxa2 = {36, 39, 36};
                    final int[] pya = {5, 8, 11};
                    g.fillPolygon(pxa1, pya, 3);
                    g.fillPolygon(pxa2, pya, 3);
                    g.fillRect(12, 5, 24, 6);
                    break;
                case 'b': // b segment
                    final int[] pxb = {37, 40, 43};
                    final int[] pyb1 = {12, 9, 12};
                    final int[] pyb2 = {36, 39, 36};
                    g.fillPolygon(pxb, pyb1, 3);
                    g.fillPolygon(pxb, pyb2, 3);
                    g.fillRect(37, 12, 6, 24);
                    break;
                case 'c': // c segment
                    final int[] pxc = {37, 40, 43};
                    final int[] pyc1 = {44, 41, 44};
                    final int[] pyc2 = {68, 71, 68};
                    g.fillPolygon(pxc, pyc1, 3);
                    g.fillPolygon(pxc, pyc2, 3);
                    g.fillRect(37, 44, 6, 24);
                    break;
                case 'd': // d segment
                    final int[] pxd1 = {12, 9, 12};
                    final int[] pxd2 = {36, 39, 36};
                    final int[] pyd = {69, 72, 75};
                    g.fillPolygon(pxd1, pyd, 3);
                    g.fillPolygon(pxd2, pyd, 3);
                    g.fillRect(12, 69, 24, 6);
                    break;
                case 'e': // e segment
                    final int[] pxe = {5, 8, 11};
                    final int[] pye1 = {44, 41, 44};
                    final int[] pye2 = {68, 71, 68};
                    g.fillPolygon(pxe, pye1, 3);
                    g.fillPolygon(pxe, pye2, 3);
                    g.fillRect(5, 44, 6, 24);
                    break;
                case 'f': // f segment
                    final int[] pxf = {5, 8, 11};
                    final int[] pyf1 = {12, 9, 12};
                    final int[] pyf2 = {36, 39, 36};
                    g.fillPolygon(pxf, pyf1, 3);
                    g.fillPolygon(pxf, pyf2, 3);
                    g.fillRect(5, 12, 6, 24);
                    break;
                case 'g': // g segment
                    final int[] pxg1 = {12, 9, 12};
                    final int[] pxg2 = {36, 39, 36};
                    final int[] pyg = {37, 40, 43};
                    g.fillPolygon(pxg1, pyg, 3);
                    g.fillPolygon(pxg2, pyg, 3);
                    g.fillRect(12, 37, 24, 6);
                    break;
                case 'h': // decimal point
                    g.fillOval(49, 68, 8, 8);
                    break;
            }
        }

        public void modifyDisplay(final char val) {
            this.aff = val;
            this.repaint();
        }

        @Override
        public void paint(final Graphics g) {
            char c = 'a';
            while (c <= 'h') {
                if ((this.aff & 0x1) == 1) {
                    g.setColor(Color.RED);
                } else {
                    g.setColor(Color.LIGHT_GRAY);
                }
                SevenSegmentDisplay.SwitchSegment(g, c);
                this.aff = (char) (this.aff >>> 1);
                c++;
            }
        }
    }

    /*
     * ....................Hexa Keyboard end here...................................
     */
    /* ....................Timer start here................................... */

    public static class SevenSegmentPanel extends JPanel {
        public final SevenSegmentDisplay[] display;

        public SevenSegmentPanel() {
            int i;
            final FlowLayout fl = new FlowLayout();
            this.setLayout(fl);
            this.display = new SevenSegmentDisplay[2];
            for (i = 0; i < 2; i++) {
                this.display[i] = new SevenSegmentDisplay((char) (0));
                this.add(this.display[i]);
            }
        }

        public void modifyDisplay(final int num, final char val) {
            this.display[num].modifyDisplay(val);
            this.display[num].repaint();
        }

        public void resetSevenSegment() {
            int i;
            for (i = 0; i < 2; i++) {
                this.modifyDisplay(i, (char) 0);
            }
        }
    }

    public class HexaKeyboard extends JPanel {
        public final JButton[] button;

        public HexaKeyboard() {
            int i;
            final GridLayout layout = new GridLayout(4, 4);
            this.setLayout(layout);
            this.button = new JButton[16];
            for (i = 0; i < 16; i++) {
                this.button[i] = new JButton(Integer.toHexString(i));
                this.button[i].setBackground(Color.WHITE);
                this.button[i].setMargin(new Insets(10, 10, 10, 10));
                this.button[i].addMouseListener(new EcouteurClick(i));
                this.add(this.button[i]);
            }
        }

        public void resetHexaKeyboard() {
            int i;
            DigitalLabSim.KeyBoardValueButtonClick = -1;
            for (i = 0; i < 16; i++) {
                this.button[i].setBackground(Color.WHITE);
            }
        }

        public class EcouteurClick implements MouseListener {
            private final int buttonValue;

            public EcouteurClick(final int val) {
                this.buttonValue = val;
            }

            @Override
            public void mouseEntered(final MouseEvent arg0) {
            }

            @Override
            public void mouseExited(final MouseEvent arg0) {
            }

            @Override
            public void mousePressed(final MouseEvent arg0) {
            }

            @Override
            public void mouseReleased(final MouseEvent arg0) {
            }

            @Override
            public void mouseClicked(final MouseEvent arg0) {
                int i;
                if (DigitalLabSim.KeyBoardValueButtonClick != -1) {// Button already pressed -> now realease
                    DigitalLabSim.KeyBoardValueButtonClick = -1;
                    DigitalLabSim.this.updateMMIOControlAndData(DigitalLabSim.this.OUT_ADRESS_HEXA_KEYBOARD, 0);
                    for (i = 0; i < 16; i++) {
                        HexaKeyboard.this.button[i].setBackground(Color.WHITE);
                    }
                } else { // new button pressed
                    DigitalLabSim.KeyBoardValueButtonClick = this.buttonValue;
                    HexaKeyboard.this.button[DigitalLabSim.KeyBoardValueButtonClick].setBackground(Color.GREEN);
                    if (DigitalLabSim.KeyboardInterruptOnOff) {
                        InterruptController.registerExternalInterrupt(DigitalLabSim.EXTERNAL_INTERRUPT_HEXA_KEYBOARD);
                    }

                }
            }
        }
    }
}
