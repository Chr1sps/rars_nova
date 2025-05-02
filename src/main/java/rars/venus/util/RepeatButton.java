package rars.venus.util;

import org.jetbrains.annotations.NotNull;
import rars.logging.RARSLogger;
import rars.logging.RARSLogging;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static rars.logging.LoggingKt.debug;

// CREDIT  
// http://forums.sun.com/thread.jspa?threadID=499183&messageID=2505646
// Java Developer Forum, Useful Code of the Day: Button Fires Events While Held
//
// This is NOT one of the MARS buttons!  It is a subclass of JButton that can
// be used to create buttons that fire events after being held down for a 
// specified period of time and at a specified rate. 

/**
 * {@code RepeatButton} is a {@code JButton} which contains a timer
 * for firing events while the button is held down. There is a default
 * initial delay of 300ms before the key event is fired and a 60ms delay
 * between subsequent events. When the user holds the button down and moves
 * the mouse out from over the button, the timer stops, but if the user moves
 * the mouse back over the button without having released the mouse button,
 * the timer starts up again at the same delay rate. If the enabled state is
 * changed while the timer is active, it will be stopped.
 * <p>
 * NOTE: The normal button behavior is that the action event is fired after
 * the button is released. It may be important to konw then that this is
 * still the case. So in effect, listeners will get 1 more event then what
 * the internal timer fires. It's not a "bug", per se, just something to be
 * aware of. There seems to be no way to suppress the final event from
 * firing anyway, except to process all ActionListeners internally. But
 * realistically, it probably doesn't matter.
 */
public class RepeatButton extends JButton
    implements ActionListener, MouseListener {
    private static final @NotNull RARSLogger LOGGER = RARSLogging.forJavaClass(
        RepeatButton.class);
    /**
     * Testing flag. Set in main method.
     */
    private static boolean testing = false;
    /**
     * The pressed state for this button.
     */
    private boolean pressed = false;
    /**
     * Flag to indicate that the button should fire events when held.
     * If false, the button is effectively a plain old JButton, but
     * there may be times when this feature might wish to be disabled.
     */
    private boolean repeatEnabled = true;
    /**
     * The hold-down timer for this button.
     */
    private Timer timer = null;
    /**
     * The initial delay for this button. Hold-down time before key
     * timer firing. In milliseconds.
     */
    private int initialDelay = 300;
    /**
     * The delay between timer firings for this button once the delay
     * period is past. In milliseconds.
     */
    private int delay = 60;
    /**
     * Holder of the modifiers used when the mouse pressed the button.
     * This is used for subsequently fired action events. This may change
     * after mouse pressed if the user moves the mouse out, releases a key
     * and then moves the mouse back in.
     */
    private int modifiers = 0;

    /**
     * Creates a button with an icon.
     *
     * @param icon
     *     the button icon
     */
    public RepeatButton(final Icon icon) {
        super(icon);
        this.init();
    }

    /**
     * Creates a button with text.
     *
     * @param text
     *     the button text
     */
    public RepeatButton(final String text) {
        super(text);
        this.init();
    }

    /**
     * Main method, for testing. Creates a frame with both styles of menu.
     *
     * @param args
     *     the command-line arguments
     */
    public static void main(final String[] args) {
        RepeatButton.testing = true;
        final JFrame f = new JFrame("RepeatButton Test");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JPanel p = new JPanel();
        final RepeatButton b = new RepeatButton("hold me");
        b.setActionCommand("test");
        b.addActionListener(b);
        p.add(b);
        f.getContentPane().add(p);
        f.pack();
        f.setVisible(true);
    }

    /**
     * Initializes the button.
     */
    private void init() {
        this.addMouseListener(this);
        // initialize timers for button holding...
        this.timer = new Timer(this.delay, this);
        this.timer.setRepeats(true);
    }

    /**
     * Gets the delay for the timer of this button.
     *
     * @return the delay
     */
    public int getDelay() {
        return this.delay;
    }

    /**
     * Set the delay for the timer of this button.
     *
     * @param d
     *     the delay
     */
    public void setDelay(final int d) {
        this.delay = d;
    }

    /**
     * Gets the initial delay for the timer of this button.
     *
     * @return the initial delay
     */
    public int getInitialDelay() {
        return this.initialDelay;
    }

    /**
     * Sets the initial delay for the timer of this button.
     *
     * @param d
     *     the initial delay
     */
    public void setInitialDelay(final int d) {
        this.initialDelay = d;
    }

    /**
     * Checks if the button should fire events when held. If false, the
     * button is effectively a plain old JButton, but there may be times
     * when this feature might wish to be disabled.
     *
     * @return if true, the button should fire events when held
     */
    public boolean isRepeatEnabled() {
        return this.repeatEnabled;
    }

    /**
     * Sets if the button should fire events when held. If false, the
     * button is effectively a plain old JButton, but there may be times
     * when this feature might wish to be disabled. If false, it will
     * also stop the timer if it's running.
     *
     * @param en
     *     if true, the button should fire events when held
     */
    public void setRepeatEnabled(final boolean en) {
        if (!en) {
            this.pressed = false;
            if (this.timer.isRunning()) {
                this.timer.stop();
            }
        }
        this.repeatEnabled = en;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sets the enabled state of this button. Overridden to stop the timer
     * if it's running.
     */
    @Override
    public void setEnabled(final boolean en) {
        if (en != super.isEnabled()) {
            this.pressed = false;
            if (this.timer.isRunning()) {
                this.timer.stop();
            }
        }
        super.setEnabled(en);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handle action events. OVERRIDE THIS IN SUBCLASS!
     */
    @Override
    public void actionPerformed(final ActionEvent ae) {
        // process events only from this components
        if (ae.getSource() == this.timer) {
            final ActionEvent event = new ActionEvent(
                this, ActionEvent.ACTION_PERFORMED,
                super.getActionCommand(), this.modifiers
            );
            super.fireActionPerformed(event);
        }
        // testing code...
        else if (RepeatButton.testing && ae.getSource() == this) {
            debug(LOGGER, ae::getActionCommand);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handle mouse clicked events.
     */
    @Override
    public void mouseClicked(final MouseEvent me) {
        // process events only from this components
        if (me.getSource() == this) {
            this.pressed = false;
            if (this.timer.isRunning()) {
                this.timer.stop();
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handle mouse pressed events.
     */
    @Override
    public void mousePressed(final MouseEvent me) {
        // process events only from this components
        if (me.getSource() == this && this.isEnabled() && this.repeatEnabled) {
            this.pressed = true;
            if (!this.timer.isRunning()) {
                this.modifiers = me.getModifiersEx();
                this.timer.setInitialDelay(this.initialDelay);
                this.timer.start();
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handle mouse released events.
     */
    @Override
    public void mouseReleased(final MouseEvent me) {
        // process events only from this components
        if (me.getSource() == this) {
            this.pressed = false;
            if (this.timer.isRunning()) {
                this.timer.stop();
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handle mouse entered events.
     */
    @Override
    public void mouseEntered(final MouseEvent me) {
        // process events only from this components
        if (me.getSource() == this && this.isEnabled() && this.repeatEnabled) {
            if (this.pressed && !this.timer.isRunning()) {
                this.modifiers = me.getModifiersEx();
                this.timer.setInitialDelay(this.delay);
                this.timer.start();
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handle mouse exited events.
     */
    @Override
    public void mouseExited(final MouseEvent me) {
        // process events only from this components
        if (me.getSource() == this) {
            if (this.timer.isRunning()) {
                this.timer.stop();
            }
        }
    }
}
