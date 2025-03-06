/*
Developed by Zachary Selk at the University of Alberta (zrselk@gmail.com)

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

import kotlin.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.notices.MemoryAccessNotice;
import rars.riscv.hardware.registerFiles.CSRegisterFile;
import rars.venus.VenusUI;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A RARS tool used to implement a timing module and timer inturrpts.
 */
public final class TimerTool extends AbstractTool {
    private static final Logger LOGGER = LogManager.getLogger(TimerTool.class);

    private static final String heading = "Timer Tool";
    private static final String version = "Version 1.0 (Zachary Selk)";
    // Internal time values

    /** The current time of the program (starting from 0) */
    private static long time = 0L;
    /** Tmp unix time used to keep track of how much time has passed */
    private static long startTime = 0L;
    /** Accumulates time as we pause/play the timer */
    private static long savedTime = 0L;
    // Timing threads
    private static TimeCmpDaemon timeCmp = null; // Watches for changes made to timecmp
    // Internal timing flags
    private static boolean updateTime = false; // Controls when time progresses (for pausing)
    private static boolean running = false; // true while tick thread is running
    private final Timer timer = new Timer();
    private final Tick tick = new Tick(); // Runs every millisecond to decide if a timer inturrupt should be raised
    // GUI window sections
    private TimePanel timePanel;

    public TimerTool(final @NotNull VenusUI mainUI) {
        super(TimerTool.heading + ", " + TimerTool.version, TimerTool.heading, mainUI);
        TimerTool.startTimeCmpDaemon();
    }

    private static int getTimeAddress() {
        return Globals.MEMORY_INSTANCE.getMemoryConfiguration().memoryMapBaseAddress + 0x18;
    }

    private static int getTimeCmpAddress() {
        return Globals.MEMORY_INSTANCE.getMemoryConfiguration().memoryMapBaseAddress + 0x20;
    }

    // A daemon that watches the timecmp MMIO for any changes
    private static void startTimeCmpDaemon() {
        if (TimerTool.timeCmp == null) {
            TimerTool.timeCmp = new TimeCmpDaemon();
        }
    }

    // Set up the tools interface

    /**
     * <p>play.</p>
     */
    public static void play() {
        // Gaurd against multiple plays
        if (!TimerTool.updateTime) {
            TimerTool.updateTime = true;
            TimerTool.startTime = System.currentTimeMillis();
        }

    }

    /**
     * <p>pause.</p>
     */
    public static void pause() {
        // Gaurd against multiple pauses
        if (TimerTool.updateTime) {
            TimerTool.updateTime = false;
            TimerTool.time = TimerTool.savedTime + System.currentTimeMillis() - TimerTool.startTime;
            TimerTool.savedTime = TimerTool.time;
        }
    }

    // Overwrites the empty parent method, called when the tool is closed

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Timer Tool";
    }

    /**
     * <p>buildMainDisplayArea.</p>
     *
     * @return a {@link javax.swing.JComponent} object
     */
    @Override
    protected JComponent buildMainDisplayArea() {
        final JPanel panelTools = new JPanel(new GridLayout(1, 2));
        this.timePanel = new TimePanel();

        // Adds a play button to start/resume time
        final JButton playButton = new JButton("Play");
        playButton.setToolTipText("Starts the counter");
        playButton.addActionListener(
            e -> TimerTool.play());
        playButton.addKeyListener(new EnterKeyListener(playButton));

        // Adds a pause button to pause time
        final JButton pauseButton = new JButton("Pause");
        pauseButton.setToolTipText("Pauses the counter");
        pauseButton.addActionListener(
            e -> TimerTool.pause());
        pauseButton.addKeyListener(new EnterKeyListener(pauseButton));

        this.timePanel.add(playButton);
        this.timePanel.add(pauseButton);
        panelTools.add(this.timePanel);
        return panelTools;
    }

    /**
     * <p>performSpecialClosingDuties.</p>
     */
    @Override
    protected void performSpecialClosingDuties() {
        this.stop();
    }

    /**
     * ************************ Timer controls ****************************
     */
    public void start() {
        if (!TimerTool.running) {
            // Start a timer that checks to see if a timer interupt needs to be raised
            // every millisecond
            this.timer.schedule(this.tick, 0, 1);
            TimerTool.running = true;
        }
    }

    // Reset all of our counters to their default values

    /**
     * <p>reset.</p>
     */
    @Override
    protected void reset() {
        TimerTool.time = 0L;
        TimerTool.savedTime = 0L;
        TimerTool.startTime = System.currentTimeMillis();
        this.tick.updateTimecmp = true;
        this.timePanel.updateTime();
        this.tick.reset();
    }

    // Shutdown the timer (note that we keep the TimeCmpDaemon running)

    /**
     * <p>stop.</p>
     */
    public void stop() {
        TimerTool.updateTime = false;
        this.timer.cancel();
        TimerTool.running = false;
        this.reset();
    }

    // Writes a word to a virtual memory address
    private synchronized void updateMMIOControlAndData(final int dataAddr, final int dataValue) {
        Globals.MEMORY_REGISTERS_LOCK.lock();
        try {
            Globals.MEMORY_INSTANCE.setRawWord(dataAddr, dataValue).onLeft(error -> {
                TimerTool.LOGGER.fatal("Tool author specified incorrect MMIO address!", error);
                System.exit(0);
                return Unit.INSTANCE;
            });
        } finally {
            Globals.MEMORY_REGISTERS_LOCK.unlock();
        }
    }

    /**
     * <p>getHelpComponent.</p>
     *
     * @return a {@link javax.swing.JComponent} object
     */
    @Override
    protected JComponent getHelpComponent() {
        final String helpContent = """
            Use this tool to simulate the Memory Mapped IO (MMIO) for a timing device allowing the program to utalize timer interupts. \
            While this tool is connected to the program it runs a clock (starting from time 0), storing the time in milliseconds. \
            The time is stored as a 64 bit integer and can be accessed (using a lw instruction) at 0xFFFF0018 for the lower 32 bits and 0xFFFF001B for the upper 32 bits.
            Three things must be done before an interrupt can be set:
             The address of your interrupt handler must be stored in the utvec CSR
             The fourth bit of the uie CSR must be set to 1 (ie. ori uie, uie, 0x10)
             The zeroth bit of the ustatus CSR must be set to 1 (ie. ori ustatus, ustatus, 0x1)
            To set the timer you must write the time that you want the timer to go off (called timecmp) as a 64 bit integer at the address of 0xFFFF0020 for the lower 32 bits and 0xFFFF0024 for the upper 32 bits. \
            An interrupt will occur when the time is greater than or equal to timecmp which is a 64 bit integer (interpreted as milliseconds) stored at 0xFFFF0020 for the lower 32 bits and 0xFFFF0024 for the upper 32 bits. \
            To set the timer you must set timecmp (using a sw instruction) to be the time that you want the timer to go off at.
            Note: the timer will only go off once after the time is reached and is not rearmed until timecmp is writen to again. \
            So if you are writing 64 bit values (opposed to on 32) then to avoid spuriously triggering a timer interrupt timecmp should be written to as such
                # a0: lower 32 bits of time
                # a1: upper 32 bits of time
                li  t0, -1
                la t1, timecmp
                sw t0, 0(t1)
                sw a1, 4(t1)
                sw a0, 0(t0)
            (contributed by Zachary Selk, zrselk@gmail.com)""";
        final JButton help = new JButton("Help");
        help.addActionListener(
            e -> {
                final JTextArea ja = new JTextArea(helpContent);
                ja.setRows(20);
                ja.setColumns(60);
                ja.setLineWrap(true);
                ja.setWrapStyleWord(true);
                JOptionPane.showMessageDialog(
                    this.theWindow, new JScrollPane(ja),
                    "Simulating a timing device", JOptionPane.INFORMATION_MESSAGE
                );
            });
        return help;
    }

    /***************************** Timer Classes *****************************/

    // Watches for changes made to the timecmp MMIO
    public final static class TimeCmpDaemon {
        public boolean postInterrupt = false;
        public long value = 0L; // Holds the most recent value of timecmp writen to the MMIO

        public TimeCmpDaemon() {
            this.addAsObserver();
        }

        public void addAsObserver() {
            Globals.MEMORY_INSTANCE.subscribe(
                notice -> {
                    final var accessType = notice.accessType;
                    // If is was a WRITE operation
                    if (accessType == MemoryAccessNotice.AccessType.WRITE) {
                        final int address = notice.address;
                        final int value = notice.value;

                        // Check what word was changed, then update the corrisponding information
                        if (address == TimerTool.getTimeCmpAddress()) {
                            this.value = ((this.value >> 32) << 32) + value;
                            this.postInterrupt = true; // timecmp was writen to
                        } else if (address == TimerTool.getTimeCmpAddress() + 4) {
                            this.value = (this.value) + (((long) value) << 32);
                            this.postInterrupt = true; // timecmp was writen to
                        }
                    }
                    return Unit.INSTANCE;
                },
                TimerTool.getTimeCmpAddress(),
                TimerTool.getTimeCmpAddress() + 8
            ).onLeft(aee -> {
                TimerTool.LOGGER.fatal("Error while adding observer in Timer Tool");
                System.exit(0);
                return Unit.INSTANCE;
            });
        }

    }

    /** Runs every millisecond to decide if a timer interrupt should be raised */
    private class Tick extends TimerTask {
        public volatile boolean updateTimecmp = true;

        /** Checks the control bits to see if user-level timer inturrupts are enabled */
        private static boolean bitsEnabled() {
            final boolean utip = (Globals.CS_REGISTER_FILE.getIntValue("uie") & 0x10) == 0x10;
            final boolean uie = (Globals.CS_REGISTER_FILE.getIntValue("ustatus") & 0x1) == 0x1;

            return (utip && uie);
        }

        @Override
        public void run() {
            // Check to see if the tool is connected
            // Note: "connectButton != null" short circuits the expression when null
            if (TimerTool.this.connectButton != null && TimerTool.this.connectButton.isConnected()) {
                // If the tool is not paused
                if (TimerTool.updateTime) {
                    // time is the difference between the last time we started the time and now,
                    // plus
                    // our time accumulator
                    TimerTool.time = TimerTool.savedTime + System.currentTimeMillis() - TimerTool.startTime;

                    // Write the lower and upper words of the time MMIO respectivly
                    TimerTool.this.updateMMIOControlAndData(TimerTool.getTimeAddress(), (int) (TimerTool.time));
                    TimerTool.this.updateMMIOControlAndData(
                        TimerTool.getTimeAddress() + 4,
                        (int) (TimerTool.time >> 32)
                    );

                    // The logic for if a timer interrupt should be raised
                    // Note: if either the UTIP bit in the uie CSR or the UIE bit in the ustatus CSR
                    // are zero then this interrupt will be stopped further on in the pipeline
                    if (TimerTool.time >= TimerTool.timeCmp.value && TimerTool.timeCmp.postInterrupt && Tick.bitsEnabled()) {
                        Globals.INTERRUPT_CONTROLLER.registerTimerInterrupt(CSRegisterFile.TIMER_INTERRUPT);
                        TimerTool.timeCmp.postInterrupt = false; // Wait for timecmp to be writen to again
                    }
                    TimerTool.this.timePanel.updateTime();
                }
            }
            // Otherwise we keep track of the last time the tool was not connected
            else {
                TimerTool.time = TimerTool.savedTime + System.currentTimeMillis() - TimerTool.startTime;
                TimerTool.startTime = System.currentTimeMillis();
            }
        }

        // Set time MMIO to zero
        public void reset() {
            TimerTool.this.updateMMIOControlAndData(TimerTool.getTimeAddress(), 0);
            TimerTool.this.updateMMIOControlAndData(TimerTool.getTimeAddress() + 4, 0);
        }
    }

    // A help popup window on how to use this tool

    /***************************** GUI Objects *******************************/

    // A panel that displays time
    public class TimePanel extends JPanel {
        final JLabel currentTime = new JLabel("Hello world");

        public TimePanel() {
            final FlowLayout fl = new FlowLayout();
            this.setLayout(fl);
            this.add(this.currentTime);
            this.updateTime();
            TimerTool.this.start();
        }

        public void updateTime() {
            this.currentTime.setText(String.format(
                "%02d:%02d.%02d", TimerTool.time / 60000,
                (TimerTool.time / 1000) % 60, TimerTool.time % 100
            ));
        }
    }
}
