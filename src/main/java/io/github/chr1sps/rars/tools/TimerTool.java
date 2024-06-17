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

package io.github.chr1sps.rars.tools;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.exceptions.AddressErrorException;
import io.github.chr1sps.rars.notices.MemoryAccessNotice;
import io.github.chr1sps.rars.riscv.hardware.ControlAndStatusRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.InterruptController;
import io.github.chr1sps.rars.riscv.hardware.Memory;
import io.github.chr1sps.rars.util.SimpleSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Flow;

/**
 * A RARS tool used to implement a timing module and timer inturrpts.
 */
public class TimerTool extends AbstractToolAndApplication {
    private static final Logger LOGGER = LogManager.getLogger(TimerTool.class);

    private static final String heading = "Timer Tool";
    private static final String version = "Version 1.0 (Zachary Selk)";
    private static final int TIME_ADDRESS = Memory.memoryMapBaseAddress + 0x18;
    private static final int TIME_CMP_ADDRESS = Memory.memoryMapBaseAddress + 0x20;

    // GUI window sections
    private static JPanel panelTools;
    private TimePanel timePanel;

    // Internal time values
    private static long time = 0L; // The current time of the program (starting from 0)
    private static long startTime = 0L; // Tmp unix time used to keep track of how much time has passed
    private static long savedTime = 0L; // Accumulates time as we pause/play the timer

    // Timing threads
    private static TimeCmpDaemon timeCmp = null; // Watches for changes made to timecmp
    private final Timer timer = new Timer();
    private final Tick tick = new Tick(); // Runs every millisecond to decide if a timer inturrupt should be raised

    // Internal timing flags
    private static boolean updateTime = false; // Controls when time progresses (for pausing)
    private static boolean running = false; // true while tick thread is running

    /**
     * <p>Constructor for TimerTool.</p>
     */
    public TimerTool() {
        super(TimerTool.heading + ", " + TimerTool.version, TimerTool.heading);
        this.startTimeCmpDaemon();
    }

    /**
     * <p>Constructor for TimerTool.</p>
     *
     * @param title   a {@link java.lang.String} object
     * @param heading a {@link java.lang.String} object
     */
    public TimerTool(final String title, final String heading) {
        super(title, heading);
        this.startTimeCmpDaemon();
    }

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects
     */
    public static void main(final String[] args) {
        new TimerTool(TimerTool.heading + ", " + TimerTool.version, TimerTool.heading);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Timer Tool";
    }

    // Set up the tools interface

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
                e -> this.play());
        playButton.addKeyListener(new EnterKeyListener(playButton));

        // Adds a pause button to pause time
        final JButton pauseButton = new JButton("Pause");
        pauseButton.setToolTipText("Pauses the counter");
        pauseButton.addActionListener(
                e -> this.pause());
        pauseButton.addKeyListener(new EnterKeyListener(pauseButton));

        this.timePanel.add(playButton);
        this.timePanel.add(pauseButton);
        panelTools.add(this.timePanel);
        return panelTools;
    }

    // A daemon that watches the timecmp MMIO for any changes
    private void startTimeCmpDaemon() {
        if (TimerTool.timeCmp == null) {
            TimerTool.timeCmp = new TimeCmpDaemon();
        }
    }

    // Overwrites the empty parent method, called when the tool is closed

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

    /**
     * <p>play.</p>
     */
    public void play() {
        // Gaurd against multiple plays
        if (!TimerTool.updateTime) {
            TimerTool.updateTime = true;
            TimerTool.startTime = System.currentTimeMillis();
        }

    }

    /**
     * <p>pause.</p>
     */
    public void pause() {
        // Gaurd against multiple pauses
        if (TimerTool.updateTime) {
            TimerTool.updateTime = false;
            TimerTool.time = TimerTool.savedTime + System.currentTimeMillis() - TimerTool.startTime;
            TimerTool.savedTime = TimerTool.time;
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

    /***************************** Timer Classes *****************************/

    // Watches for changes made to the timecmp MMIO
    public static class TimeCmpDaemon implements SimpleSubscriber<MemoryAccessNotice> {
        public boolean postInterrupt = false;
        public long value = 0L; // Holds the most recent value of timecmp writen to the MMIO

        public TimeCmpDaemon() {
            this.addAsObserver();
        }

        public void addAsObserver() {
            try {
                Globals.memory.subscribe(this, TimerTool.TIME_CMP_ADDRESS, TimerTool.TIME_CMP_ADDRESS + 8);
            } catch (final AddressErrorException aee) {
                LOGGER.fatal("Error while adding observer in Timer Tool");
                System.exit(0);
            }
        }

        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(final Flow.Subscription subscription) {
            this.subscription = subscription;
            this.subscription.request(1);
        }

        @Override
        public void onNext(final MemoryAccessNotice notice) {
            final int accessType = notice.getAccessType();
            // If is was a WRITE operation
            if (accessType == 1) {
                final int address = notice.getAddress();
                final int value = notice.getValue();

                // Check what word was changed, then update the corrisponding information
                if (address == TimerTool.TIME_CMP_ADDRESS) {
                    this.value = ((this.value >> 32) << 32) + value;
                    this.postInterrupt = true; // timecmp was writen to
                } else if (address == TimerTool.TIME_CMP_ADDRESS + 4) {
                    this.value = (this.value) + (((long) value) << 32);
                    this.postInterrupt = true; // timecmp was writen to
                }
            }
            this.subscription.request(1);
        }
    }

    // Runs every millisecond to decide if a timer inturrupt should be raised
    private class Tick extends TimerTask {
        public volatile boolean updateTimecmp = true;

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
                    TimerTool.this.updateMMIOControlAndData(TimerTool.TIME_ADDRESS, (int) (TimerTool.time));
                    TimerTool.this.updateMMIOControlAndData(TimerTool.TIME_ADDRESS + 4, (int) (TimerTool.time >> 32));

                    // The logic for if a timer interrupt should be raised
                    // Note: if either the UTIP bit in the uie CSR or the UIE bit in the ustatus CSR
                    // are zero then this interrupt will be stopped further on in the pipeline
                    if (TimerTool.time >= TimerTool.timeCmp.value && TimerTool.timeCmp.postInterrupt && this.bitsEnabled()) {
                        InterruptController.registerTimerInterrupt(ControlAndStatusRegisterFile.TIMER_INTERRUPT);
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

        // Checks the control bits to see if user-level timer inturrupts are enabled
        private boolean bitsEnabled() {
            final boolean utip = (ControlAndStatusRegisterFile.getValue("uie") & 0x10) == 0x10;
            final boolean uie = (ControlAndStatusRegisterFile.getValue("ustatus") & 0x1) == 0x1;

            return (utip && uie);
        }

        // Set time MMIO to zero
        public void reset() {
            TimerTool.this.updateMMIOControlAndData(TimerTool.TIME_ADDRESS, 0);
            TimerTool.this.updateMMIOControlAndData(TimerTool.TIME_ADDRESS + 4, 0);
        }
    }

    // Writes a word to a virtual memory address
    private synchronized void updateMMIOControlAndData(final int dataAddr, final int dataValue) {
        Globals.memoryAndRegistersLock.lock();
        try {
            try {
                Globals.memory.setRawWord(dataAddr, dataValue);
            } catch (final AddressErrorException aee) {
                LOGGER.fatal("Tool author specified incorrect MMIO address!", aee);
                System.exit(0);
            }
        } finally {
            Globals.memoryAndRegistersLock.unlock();
        }
    }

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
            this.currentTime.setText(String.format("%02d:%02d.%02d", TimerTool.time / 60000, (TimerTool.time / 1000) % 60, TimerTool.time % 100));
        }
    }

    // A help popup window on how to use this tool

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
                    JOptionPane.showMessageDialog(this.theWindow, new JScrollPane(ja),
                            "Simulating a timing device", JOptionPane.INFORMATION_MESSAGE);
                });
        return help;
    }
}
