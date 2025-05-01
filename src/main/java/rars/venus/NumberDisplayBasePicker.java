package rars.venus;

import org.jetbrains.annotations.NotNull;
import rars.api.DisplayFormat;
import rars.util.BinaryUtilsKt;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;

/**
 * Use to select base for displaying numbers. Initially the
 * choices are only 10 (decimal) and 16 (hex), so I'm using
 * a check box where checked means hex. If base 8 (octal)
 * is added later, the Component will need to change.
 */
public final class NumberDisplayBasePicker extends JCheckBox {
    private @NotNull DisplayFormat base;
    private JCheckBoxMenuItem settingMenuItem;

    /**
     * constructor. It assumes the text will be worded
     * so that a checked box means hexadecimal!
     *
     * @param text
     *     Text to accompany the check box.
     * @param displayInHex
     *     Currently either DECIMAL or HEXADECIMAL
     */
    public NumberDisplayBasePicker(
        final @NotNull String text,
        final boolean displayInHex,
        final @NotNull ExecutePane executePane
    ) {
        super(text, displayInHex);
        this.base = displayInHex ? DisplayFormat.HEX : DisplayFormat.DECIMAL;
        this.addItemListener(ie -> {
            final var picker = (NumberDisplayBasePicker) ie.getItem();
            picker.setBase(ie.getStateChange() == ItemEvent.SELECTED ? DisplayFormat.HEX : DisplayFormat.DECIMAL);
            if (settingMenuItem != null) {
                settingMenuItem.setSelected(picker.isSelected());
                final var listeners = settingMenuItem.getActionListeners();
                final var event = new ActionEvent(
                    settingMenuItem, 0,
                    "chooser"
                );
                for (final var listener : listeners) {
                    listener.actionPerformed(event);
                }
            }
            // Better to use notify, but I am tired...
            executePane.numberDisplayBaseChanged(picker);
        });
    }

    /**
     * Produces a string form of an unsigned given the value and the
     * numerical base to convert it to. This class
     * method can be used by anyone anytime. If base is 16, result
     * is same as for formatNumber(). If base is 10, will produce
     * string version of unsigned value. E.g. 0xffffffff will produce
     * "4294967295" instead of "-1".
     *
     * @param value
     *     the number to be converted
     * @param base
     *     the numerical base to use (currently 10 or 16)
     * @return a String equivalent of the value rendered appropriately.
     */
    public static String formatUnsignedInteger(final int value, final @NotNull DisplayFormat base) {
        if (base == DisplayFormat.HEX) {
            return BinaryUtilsKt.intToHexStringWithPrefix(value);
        } else {
            return BinaryUtilsKt.unsignedIntToIntString(value);
        }
    }

    /**
     * Produces a string form of an integer given the value and the
     * numerical base to convert it to. There is an instance
     * method that uses the internally stored base. This class
     * method can be used by anyone anytime.
     *
     * @param value
     *     the number to be converted
     * @param base
     *     the numerical base to use (currently 10 or 16)
     * @return a String equivalent of the value rendered appropriately.
     */
    public static @NotNull String formatNumber(final int value, final @NotNull DisplayFormat base) {
        return switch (base) {
            case HEX -> BinaryUtilsKt.intToHexStringWithPrefix(value);
            case ASCII -> BinaryUtilsKt.intToAscii(value);
            case DECIMAL -> Integer.toString(value);
        };
    }

    public static @NotNull String formatNumber(final long value, final @NotNull DisplayFormat base) {
        return switch (base) {
            case HEX -> BinaryUtilsKt.longToHexStringWithPrefix(value);
            case ASCII -> BinaryUtilsKt.longToAscii(value);
            case DECIMAL -> Long.toString(value);
        };
    }

    /**
     * Produces a string form of a double given a long containing
     * the 64 bit pattern and the numerical base to use (10 or 16). If the
     * base is 16, the string will be built from the 64 bits. If the
     * base is 10, the long bits will be converted to double and the
     * string constructed from that. Seems an odd distinction to make,
     * except that contents of floating point registers are stored
     * internally as int bits. If the int bits represent a NaN value
     * (of which there are many!), converting them to double then calling
     * formatNumber(double, int) above, causes the double value to become
     * the canonical NaN value. It does not preserve the bit
     * pattern! Then converting it to hex string yields the canonical NaN.
     * Not an issue if display base is 10 since result string will be NaN
     * no matter what the internal NaN value is.
     *
     * @param value
     *     the long bits to be converted to string of corresponding double.
     * @param base
     *     the numerical base to use (currently 10 or 16)
     * @return a String equivalent of the value rendered appropriately.
     */
    public static String formatDoubleNumber(final long value, final @NotNull DisplayFormat base) {
        if (base == DisplayFormat.HEX) {
            return BinaryUtilsKt.longToHexStringWithPrefix(value);
        } else {
            return Double.toString(Double.longBitsToDouble(value));
        }
    }

    /**
     * Retrieve the current number base.
     *
     * @return current number base, currently DECIMAL or HEXADECIMAL
     */
    public @NotNull DisplayFormat getBase() {
        return this.base;
    }

    /**
     * Set the current number base.
     *
     * @param newBase
     *     The new number base. Currently, if it is
     *     neither DECIMAL nor HEXADECIMAL, the base will not be changed.
     */
    public void setBase(final @NotNull DisplayFormat newBase) {
        if (newBase == DisplayFormat.DECIMAL || newBase == DisplayFormat.HEX) {
            this.base = newBase;
        }
    }

    /**
     * Set the menu item from Settings menu that corresponds to this chooser.
     * It is the responsibility of that item to register here, because this
     * one is created first (before the menu item). They need to communicate
     * with each other so that whenever one changes, so does the other. They
     * cannot be the same object (one is JCheckBox, the other is JCheckBoxMenuItem).
     *
     * @param setter
     *     a {@link JCheckBoxMenuItem} object
     */
    public void setSettingsMenuItem(final @NotNull JCheckBoxMenuItem setter) {
        this.settingMenuItem = setter;
    }
}
