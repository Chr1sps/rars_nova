package rars.venus.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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
public class RepeatButton extends JButton {

    private boolean isPressed = false;
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

    public RepeatButton(final Icon icon, final String text) {
        super(text, icon);
        // initialize timers for button holding...
        timer = new Timer(this.delay, ae -> {
            // process events only from this components
            if (ae.getSource() == timer) {
                final var event = new ActionEvent(
                    this, ActionEvent.ACTION_PERFORMED,
                    super.getActionCommand(), modifiers
                );
                super.fireActionPerformed(event);
            }
        });
        timer.setRepeats(true);
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getSource() == RepeatButton.this) {
                    isPressed = false;
                    if (timer.isRunning()) {
                        timer.stop();
                    }
                }
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.getSource() == RepeatButton.this && isEnabled()) {
                    isPressed = true;
                    if (!timer.isRunning()) {
                        modifiers = e.getModifiersEx();
                        timer.setInitialDelay(initialDelay);
                        timer.start();
                    }
                }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                if (e.getSource() == RepeatButton.this) {
                    isPressed = false;
                    if (timer.isRunning()) {
                        timer.stop();
                    }
                }
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                if (e.getSource() == RepeatButton.this && isEnabled()) {
                    if (isPressed && !timer.isRunning()) {
                        modifiers = e.getModifiersEx();
                        timer.setInitialDelay(delay);
                        timer.start();
                    }
                }
            }

            @Override
            public void mouseExited(final MouseEvent me) {
                if (me.getSource() == RepeatButton.this) {
                    if (timer.isRunning()) {
                        timer.stop();
                    }
                }
            }
        });
    }

    /**
     * Gets the delay for the timer of this button.
     *
     * @return the delay
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Set the delay for the timer of this button.
     *
     * @param d
     *     the delay
     */
    public void setDelay(final int d) {
        delay = d;
    }

    /**
     * Gets the initial delay for the timer of this button.
     *
     * @return the initial delay
     */
    public int getInitialDelay() {
        return initialDelay;
    }

    /**
     * Sets the initial delay for the timer of this button.
     *
     * @param d
     *     the initial delay
     */
    public void setInitialDelay(final int d) {
        initialDelay = d;
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
            isPressed = false;
            if (timer.isRunning()) {
                timer.stop();
            }
        }
        super.setEnabled(en);
    }
}
