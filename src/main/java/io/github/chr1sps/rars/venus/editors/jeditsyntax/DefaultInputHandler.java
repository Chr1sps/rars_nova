/*
 * DefaultInputHandler.java - Default implementation of an input handler
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package io.github.chr1sps.rars.venus.editors.jeditsyntax;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * The default input handler. It maps sequences of keystrokes into actions
 * and inserts key typed events into the text area.
 *
 * @author Slava Pestov
 * @version $Id: DefaultInputHandler.java,v 1.18 1999/12/13 03:40:30 sp Exp $
 */
public class DefaultInputHandler extends InputHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Creates a new input handler with no key bindings defined.
     */
    public DefaultInputHandler() {
        this.bindings = this.currentBindings = new BindingMap();
    }

    /**
     * Sets up the default key bindings.
     */
    @Override
    public void addDefaultKeyBindings() {
        this.addKeyBinding("BACK_SPACE", InputHandler.BACKSPACE);
        this.addKeyBinding("C+BACK_SPACE", InputHandler.BACKSPACE_WORD);
        this.addKeyBinding("DELETE", InputHandler.DELETE);
        this.addKeyBinding("C+DELETE", InputHandler.DELETE_WORD);

        this.addKeyBinding("ENTER", InputHandler.INSERT_BREAK);
        this.addKeyBinding("TAB", InputHandler.INSERT_TAB);
        this.addKeyBinding("S+TAB", InputHandler.DELETE_TAB);

        this.addKeyBinding("INSERT", InputHandler.OVERWRITE);
        this.addKeyBinding("C+\\", InputHandler.TOGGLE_RECT);

        this.addKeyBinding("HOME", InputHandler.HOME);
        this.addKeyBinding("END", InputHandler.END);
        this.addKeyBinding("C+A", InputHandler.SELECT_ALL);
        this.addKeyBinding("S+HOME", InputHandler.SELECT_HOME);
        this.addKeyBinding("S+END", InputHandler.SELECT_END);
        this.addKeyBinding("C+HOME", InputHandler.DOCUMENT_HOME);
        this.addKeyBinding("C+END", InputHandler.DOCUMENT_END);
        this.addKeyBinding("CS+HOME", InputHandler.SELECT_DOC_HOME);
        this.addKeyBinding("CS+END", InputHandler.SELECT_DOC_END);

        this.addKeyBinding("PAGE_UP", InputHandler.PREV_PAGE);
        this.addKeyBinding("PAGE_DOWN", InputHandler.NEXT_PAGE);
        this.addKeyBinding("S+PAGE_UP", InputHandler.SELECT_PREV_PAGE);
        this.addKeyBinding("S+PAGE_DOWN", InputHandler.SELECT_NEXT_PAGE);

        this.addKeyBinding("LEFT", InputHandler.PREV_CHAR);
        this.addKeyBinding("S+LEFT", InputHandler.SELECT_PREV_CHAR);
        this.addKeyBinding("C+LEFT", InputHandler.PREV_WORD);
        this.addKeyBinding("CS+LEFT", InputHandler.SELECT_PREV_WORD);
        this.addKeyBinding("RIGHT", InputHandler.NEXT_CHAR);
        this.addKeyBinding("S+RIGHT", InputHandler.SELECT_NEXT_CHAR);
        this.addKeyBinding("C+RIGHT", InputHandler.NEXT_WORD);
        this.addKeyBinding("CS+RIGHT", InputHandler.SELECT_NEXT_WORD);
        this.addKeyBinding("UP", InputHandler.PREV_LINE);
        this.addKeyBinding("S+UP", InputHandler.SELECT_PREV_LINE);
        this.addKeyBinding("DOWN", InputHandler.NEXT_LINE);
        this.addKeyBinding("S+DOWN", InputHandler.SELECT_NEXT_LINE);

        this.addKeyBinding("C+ENTER", InputHandler.REPEAT);

        // Clipboard
        this.addKeyBinding("C+C", InputHandler.CLIP_COPY);
        this.addKeyBinding("C+V", InputHandler.CLIP_PASTE);
        this.addKeyBinding("C+X", InputHandler.CLIP_CUT);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Adds a key binding to this input handler. The key binding is
     * a list of white space separated key strokes of the form
     * <i>[modifiers+]key</i> where modifier is C for Control, A for Alt,
     * or S for Shift, and key is either a character (a-z) or a field
     * name in the KeyEvent class prefixed with VK_ (e.g., BACK_SPACE)
     */
    @Override
    public void addKeyBinding(final String keyBinding, final ActionListener action) {
        BindingMap current = this.bindings;

        final StringTokenizer st = new StringTokenizer(keyBinding);
        while (st.hasMoreTokens()) {
            final KeyStroke keyStroke = DefaultInputHandler.parseKeyStroke(st.nextToken());
            if (keyStroke == null)
                return;

            if (st.hasMoreTokens()) {
                Binding o = current.get(keyStroke);
                if (!(o instanceof BindingMap)) {
                    o = new BindingMap();
                    current.put(keyStroke, o);
                }
                current = (BindingMap) o;
            } else
                current.put(keyStroke, new BindingAction(action));
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes a key binding from this input handler. This is not yet
     * implemented.
     */
    @Override
    public void removeKeyBinding(final String keyBinding) {
        throw new InternalError("Not yet implemented");
    }

    /**
     * Removes all key bindings from this input handler.
     */
    @Override
    public void removeAllKeyBindings() {
        this.bindings.clear();
    }

    /**
     * Returns a copy of this input handler that shares the same
     * key bindings. Setting key bindings in the copy will also
     * set them in the original.
     *
     * @return a {@link io.github.chr1sps.rars.venus.editors.jeditsyntax.InputHandler} object
     */
    @Override
    public InputHandler copy() {
        return new DefaultInputHandler(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handle a key pressed event. This will look up the binding for
     * the key stroke and execute it.
     */
    @Override
    public void keyPressed(final KeyEvent evt) {
        final int keyCode = evt.getKeyCode();
        final int modifiers = evt.getModifiersEx();
        if (keyCode == KeyEvent.VK_CONTROL ||
                keyCode == KeyEvent.VK_SHIFT ||
                keyCode == KeyEvent.VK_ALT ||
                keyCode == KeyEvent.VK_META)
            return;

        if ((modifiers & ~KeyEvent.SHIFT_DOWN_MASK) != 0
                || evt.isActionKey()
                || keyCode == KeyEvent.VK_BACK_SPACE
                || keyCode == KeyEvent.VK_DELETE
                || keyCode == KeyEvent.VK_ENTER
                || keyCode == KeyEvent.VK_TAB
                || keyCode == KeyEvent.VK_ESCAPE) {
            if (this.grabAction != null) {
                this.handleGrabAction(evt);
                return;
            }

            final KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
            final Binding o = this.currentBindings.get(keyStroke);

            switch (o) {
                case null -> {
                    // Don't beep if the user presses some
                    // key we don't know about unless a
                    // prefix is active. Otherwise it will
                    // beep when caps lock is pressed, etc.
                    if (this.currentBindings != this.bindings) {
                        Toolkit.getDefaultToolkit().beep();
                        // F10 should be passed on, but C+e F10
                        // shouldn't
                        this.repeatCount = 0;
                        this.repeat = false;
                        evt.consume();
                    }
                    this.currentBindings = this.bindings;
                    // No binding for this keyStroke, pass it to menu
                    // (mnemonic, accelerator). DPS 4-may-2010
                    io.github.chr1sps.rars.Globals.getGui().dispatchEventToMenu(evt);
                    evt.consume();
                }
                case final BindingAction bindingAction -> {
                    this.currentBindings = this.bindings;
                    this.executeAction(bindingAction.actionListener,
                            evt.getSource(), null);

                    evt.consume();
                }
                case final BindingMap bindingMap -> {
                    this.currentBindings = bindingMap;
                    evt.consume();
                }
                default -> {
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handle a key typed event. This inserts the key into the text area.
     */
    @Override
    public void keyTyped(final KeyEvent evt) {
        final int modifiers = evt.getModifiersEx();
        final char c = evt.getKeyChar();
        // This IF statement needed to prevent Macintosh shortcut keyChar from
        // being echoed to the text area. E.g. Command-s, for Save, will echo
        // the 's' character unless filtered out here. Command modifier
        // matches KeyEvent.META_MASK. DPS 30-Nov-2010
        if ((modifiers & KeyEvent.META_DOWN_MASK) != 0)
            return;
        // DPS 9-Jan-2013. Umberto Villano from Italy describes Alt combinations
        // not working on Italian Mac keyboards, where # requires Alt (Option).
        // This is preventing him from writing comments. Similar complaint from
        // Joachim Parrow in Sweden, only for the $ character. Villano pointed
        // me to this method. Plus a Google search on "jeditsyntax alt key"
        // (without quotes) took me to
        // http://compgroups.net/comp.lang.java.programmer/option-key-in-jedit-syntax-package/1068227
        // which says to comment out the second condition in this IF statement:
        // if(c != KeyEvent.CHAR_UNDEFINED && (modifiers & KeyEvent.ALT_MASK) == 0)
        // So let's give it a try!
        // (...later) Bummer, it results in keystroke echoed into editing area when I
        // use Alt
        // combination for shortcut menu access (e.g. Alt+f to open the File menu).
        //
        // Torsten Maehne: This is a shortcoming of the menu
        // shortcuts handling in the jedit component: It assumes that
        // modifier keys are the same across all platforms. However,
        // the menu shortcut keymask varies between OS X and
        // Windows/Linux, it is Cmd + <key> instead of Alt +
        // <key>. The "Java Development Guide for Mac" explicitly
        // discusses the issue in:
        // <https://developer.apple.com/library/mac/#documentation/Java/Conceptual/Java14Development/07-NativePlatformIntegration/NativePlatformIntegration.html#//apple_ref/doc/uid/TP40001909-211884-TPXREF130>
        //
        // As jedit always considers Alt + <key> as a keyboard
        // shortcut, they block their output in the editor, which
        // prevents the entry of special characters on OS X that uses
        // Alt + <key> for this purpose instead of AltGr + <key>, as
        // on Windows or Linux.
        //
        // For the latest jedit version (5.0.0), the menu
        // accelerators don't work on OS X, at least the special
        // characters can be entered using Alt + <key>. The issue is
        // still open, but there seems to be progress:
        //
        // http://sourceforge.net/tracker/index.php?func=detail&aid=3558572&group_id=588&atid=300588
        // http://sourceforge.net/tracker/?func=detail&atid=300588&aid=3604532&group_id=588
        //
        // Until this is resolved upstream, don't ignore characters
        // on OS X, which have been entered with the ALT modifier:
        if (c != KeyEvent.CHAR_UNDEFINED
                && (((modifiers & KeyEvent.ALT_DOWN_MASK) == 0) || System.getProperty("os.name").contains("OS X"))) {
            if (c >= 0x20 && c != 0x7f) {
                final KeyStroke keyStroke = KeyStroke.getKeyStroke(
                        Character.toUpperCase(c));
                final Binding o = this.currentBindings.get(keyStroke);

                if (o instanceof BindingMap) {
                    this.currentBindings = (BindingMap) o;
                    return;
                } else if (o instanceof ActionListener) {
                    this.currentBindings = this.bindings;
                    this.executeAction((ActionListener) o,
                            evt.getSource(),
                            String.valueOf(c));
                    return;
                }

                this.currentBindings = this.bindings;

                if (this.grabAction != null) {
                    this.handleGrabAction(evt);
                    return;
                }

                // 0-9 adds another 'digit' to the repeat number
                if (this.repeat && Character.isDigit(c)) {
                    this.repeatCount *= 10;
                    this.repeatCount += (c - '0');
                    return;
                }
                this.executeAction(InputHandler.INSERT_CHAR, evt.getSource(),
                        String.valueOf(evt.getKeyChar()));
                this.repeatCount = 0;
                this.repeat = false;
            }
        }
    }

    /**
     * Converts a string to a keystroke. The string should be of the
     * form <i>modifiers</i>+<i>shortcut</i> where <i>modifiers</i>
     * is any combination of A for Alt, C for Control, S for Shift
     * or M for Meta, and <i>shortcut</i> is either a single character,
     * or a keycode name from the <code>KeyEvent</code> class, without
     * the <code>VK_</code> prefix.
     *
     * @param keyStroke A string description of the key stroke
     * @return a {@link javax.swing.KeyStroke} object
     */
    public static KeyStroke parseKeyStroke(final String keyStroke) {
        if (keyStroke == null)
            return null;
        int modifiers = 0;
        final int index = keyStroke.indexOf('+');
        if (index != -1) {
            for (int i = 0; i < index; i++) {
                switch (Character.toUpperCase(keyStroke
                        .charAt(i))) {
                    case 'A':
                        modifiers |= InputEvent.ALT_DOWN_MASK;
                        break;
                    case 'C':
                        modifiers |= InputEvent.CTRL_DOWN_MASK;
                        break;
                    case 'M':
                        modifiers |= InputEvent.META_DOWN_MASK;
                        break;
                    case 'S':
                        modifiers |= InputEvent.SHIFT_DOWN_MASK;
                        break;
                }
            }
        }
        final String key = keyStroke.substring(index + 1);
        if (key.length() == 1) {
            final char ch = Character.toUpperCase(key.charAt(0));
            if (modifiers == 0)
                return KeyStroke.getKeyStroke(ch);
            else
                return KeyStroke.getKeyStroke(ch, modifiers);
        } else if (key.isEmpty()) {
            DefaultInputHandler.LOGGER.error("Invalid key stroke: {} - empty key", keyStroke);
            return null;
        } else {
            final int ch;

            try {
                ch = KeyEvent.class.getField("VK_".concat(key))
                        .getInt(null);
            } catch (final Exception e) {
                DefaultInputHandler.LOGGER.error("Invalid key stroke: {} - couldn't get the int value", keyStroke);
                return null;
            }

            return KeyStroke.getKeyStroke(ch, modifiers);
        }
    }

    // private members
    private final BindingMap bindings;
    private BindingMap currentBindings;

    private static class Binding {
    }

    private static class BindingAction extends Binding {
        final ActionListener actionListener;

        BindingAction(final ActionListener ac) {
            this.actionListener = ac;
        }
    }

    private static class BindingMap extends Binding {
        final Hashtable<KeyStroke, Binding> map;

        BindingMap() {
            this.map = new Hashtable<>();
        }

        void clear() {
            this.map.clear();
        }

        void put(final KeyStroke k, final Binding b) {
            this.map.put(k, b);
        }

        Binding get(final KeyStroke k) {
            return this.map.get(k);
        }
    }

    private DefaultInputHandler(final DefaultInputHandler copy) {
        this.bindings = this.currentBindings = copy.bindings;
    }
}
