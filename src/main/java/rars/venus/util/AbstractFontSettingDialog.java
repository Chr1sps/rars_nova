package rars.venus.util;

import rars.util.EditorFont;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

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

/**
 * Abstract class for a font selection dialog.
 */
public abstract class AbstractFontSettingDialog extends JDialog {

    private static final String SEPARATOR = "___SEPARATOR____";
    protected final Font currentFont;
    JDialog editorDialog;
    JComboBox<String> fontFamilySelector, fontStyleSelector;
    JSlider fontSizeSelector;
    JSpinner fontSizeSpinSelector;
    JLabel fontSample;
    // Used to determine upon OK, whether or not anything has changed.
    String initialFontFamily, initialFontStyle, initialFontSize;

    // The dialog area, not including control buttons at bottom

    /**
     * Create a new font chooser. Has pertinent JDialog parameters.
     * Will do everything except make it visible.
     *
     * @param owner       a {@link java.awt.Frame} object
     * @param title       a {@link java.lang.String} object
     * @param modality    a boolean
     * @param currentFont a {@link java.awt.Font} object
     */
    public AbstractFontSettingDialog(final Frame owner, final String title, final boolean modality, final Font currentFont) {
        super(owner, title, modality);
        this.currentFont = currentFont;
        final JPanel overallPanel = new JPanel(new BorderLayout());
        overallPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        overallPanel.add(this.buildDialogPanel(), BorderLayout.CENTER);
        overallPanel.add(this.buildControlPanel(), BorderLayout.SOUTH);
        this.setContentPane(overallPanel);
        this.setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent we) {
                        AbstractFontSettingDialog.this.closeDialog();
                    }
                });
        this.pack();
        this.setLocationRelativeTo(owner);
    }

    // Build component containing the buttons for dialog control
    // Such as OK, Cancel, Reset, Apply, etc. These may vary
    // by application

    // Given an array of string arrays, will produce a Vector contenating
    // the arrays with a separator between each.
    private static Vector<String> makeVectorData(final String[][] strs) {
        boolean needSeparator = false;
        final Vector<String> data = new Vector<>();
        for (final String[] strA : strs) {
            if (needSeparator) {
                data.addElement(AbstractFontSettingDialog.SEPARATOR);
            }
            for (final String str : strA) {
                data.addElement(str);
                needSeparator = true;
            }
        }
        return data;
    }

    /**
     * <p>buildDialogPanel.</p>
     *
     * @return a {@link javax.swing.JPanel} object
     */
    protected JPanel buildDialogPanel() {
        final JPanel contents = new JPanel(new BorderLayout(20, 20));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Font currentFont = Globals.getSettings().getEditorFont();
        this.initialFontFamily = this.currentFont.getFamily();
        this.initialFontStyle = EditorFont.styleIntToStyleString(this.currentFont.getStyle());
        this.initialFontSize = EditorFont.sizeIntToSizeString(this.currentFont.getSize());
        final String[] commonFontFamilies = EditorFont.getCommonFamilies();
        final String[] allFontFamilies = EditorFont.getAllFamilies();
        // The makeVectorData() method will combine these two into one Vector
        // with a horizontal line separating the two groups.
        final String[][] fullList = {commonFontFamilies, allFontFamilies};

        this.fontFamilySelector = new JComboBox<>(AbstractFontSettingDialog.makeVectorData(fullList));
        this.fontFamilySelector.setRenderer(new ComboBoxRenderer());
        this.fontFamilySelector.addActionListener(new BlockComboListener(this.fontFamilySelector));
        this.fontFamilySelector.setSelectedItem(this.currentFont.getFamily());
        this.fontFamilySelector.setEditable(false);
        this.fontFamilySelector.setMaximumRowCount(commonFontFamilies.length);
        this.fontFamilySelector.setToolTipText("Short list of common font families followed by complete list.");

        final String[] fontStyles = EditorFont.getFontStyleStrings();
        this.fontStyleSelector = new JComboBox<>(fontStyles);
        this.fontStyleSelector.setSelectedItem(EditorFont.styleIntToStyleString(this.currentFont.getStyle()));
        this.fontStyleSelector.setEditable(false);
        this.fontStyleSelector.setToolTipText("List of available font styles.");

        this.fontSizeSelector = new JSlider(EditorFont.MIN_SIZE, EditorFont.MAX_SIZE, this.currentFont.getSize());
        this.fontSizeSelector.setToolTipText(
                "Use slider to select font size from " + EditorFont.MIN_SIZE + " to " + EditorFont.MAX_SIZE + ".");
        this.fontSizeSelector.addChangeListener(
                e -> {
                    final Integer value = ((JSlider) e.getSource()).getValue();
                    AbstractFontSettingDialog.this.fontSizeSpinSelector.setValue(value);
                    AbstractFontSettingDialog.this.fontSample.setFont(AbstractFontSettingDialog.this.getFont());
                });
        final SpinnerNumberModel fontSizeSpinnerModel = new SpinnerNumberModel(this.currentFont.getSize(), EditorFont.MIN_SIZE,
                EditorFont.MAX_SIZE, 1);
        this.fontSizeSpinSelector = new JSpinner(fontSizeSpinnerModel);
        this.fontSizeSpinSelector.setToolTipText("Current font size in points.");
        this.fontSizeSpinSelector.addChangeListener(
                e -> {
                    final Object value = ((JSpinner) e.getSource()).getValue();
                    AbstractFontSettingDialog.this.fontSizeSelector.setValue(((Integer) value));
                    AbstractFontSettingDialog.this.fontSample.setFont(AbstractFontSettingDialog.this.getFont());
                });
        // Action listener to update sample when family or style selected
        final ActionListener updateSample = e -> AbstractFontSettingDialog.this.fontSample.setFont(AbstractFontSettingDialog.this.getFont());
        this.fontFamilySelector.addActionListener(updateSample);
        this.fontStyleSelector.addActionListener(updateSample);

        final JPanel familyStyleComponents = new JPanel(new GridLayout(2, 2, 4, 4));
        familyStyleComponents.add(new JLabel("Font Family"));
        familyStyleComponents.add(new JLabel("Font Style"));
        familyStyleComponents.add(this.fontFamilySelector);
        familyStyleComponents.add(this.fontStyleSelector);

        this.fontSample = new JLabel("Sample of this font", SwingConstants.CENTER);
        this.fontSample.setBorder(new LineBorder(Color.BLACK));
        this.fontSample.setFont(this.getFont());
        this.fontSample.setToolTipText("Dynamically updated font sample based on current settings");
        final JPanel sizeComponents = new JPanel();
        sizeComponents.add(new JLabel("Font Size "));
        sizeComponents.add(this.fontSizeSelector);
        sizeComponents.add(this.fontSizeSpinSelector);
        final JPanel sizeAndSample = new JPanel(new GridLayout(2, 1, 4, 8));
        sizeAndSample.add(sizeComponents);
        sizeAndSample.add(this.fontSample);
        contents.add(familyStyleComponents, BorderLayout.NORTH);
        contents.add(sizeAndSample, BorderLayout.CENTER);
        return contents;
    }

    // User has clicked "Apply" or "Apply and Close" button.

    /**
     * <p>buildControlPanel.</p>
     *
     * @return a {@link java.awt.Component} object
     */
    protected abstract Component buildControlPanel();

    // We're finished with this modal dialog.

    /**
     * <p>getFont.</p>
     *
     * @return a {@link java.awt.Font} object
     */
    @Override
    public Font getFont() {
        return EditorFont.createFontFromStringValues(
                (String) this.fontFamilySelector.getSelectedItem(),
                (String) this.fontStyleSelector.getSelectedItem(),
                this.fontSizeSpinSelector.getValue().toString());
    }

    // Reset font to its initial setting

    /**
     * <p>performApply.</p>
     */
    protected void performApply() {
        this.apply(this.getFont());
    }

    /**
     * <p>closeDialog.</p>
     */
    protected void closeDialog() {
        this.setVisible(false);
        this.dispose();
    }

    /////////////////////////////////////////////////////////////////////
    //
    // Method and two classes to permit one or more horizontal separators
    // within a combo box list. I obtained this code on 13 July 2007
    // from http://www.codeguru.com/java/articles/164.shtml. Author
    // is listed: Nobuo Tamemasa. Code is old, 1999, but fine for this.
    // I will use it to separate the short list of "common" font
    // families from the very long list of all font families. No attempt
    // to keep a list of recently-used fonts like Word does. The list
    // of common font families is static.
    //
    /////////////////////////////////////////////////////////////////////

    /**
     * <p>reset.</p>
     */
    protected void reset() {
        this.fontFamilySelector.setSelectedItem(this.initialFontFamily);
        this.fontStyleSelector.setSelectedItem(this.initialFontStyle);
        this.fontSizeSelector.setValue(EditorFont.sizeStringToSizeInt(this.initialFontSize));
        this.fontSizeSpinSelector.setValue(EditorFont.sizeStringToSizeInt(this.initialFontSize));
    }

    /**
     * Apply the given font. Left for the client to define.
     *
     * @param font a font to be applied by the client.
     */
    protected abstract void apply(Font font);

    // Required renderer for handling the separator bar.
    private static class ComboBoxRenderer extends JLabel implements ListCellRenderer<String> {
        final JSeparator separator;

        public ComboBoxRenderer() {
            this.setOpaque(true);
            this.setBorder(new EmptyBorder(1, 1, 1, 1));
            this.separator = new JSeparator(JSeparator.HORIZONTAL);
        }

        @Override
        public Component getListCellRendererComponent(final JList<? extends String> list,
                                                      final String value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            final String str = (value == null) ? "" : value;
            if (AbstractFontSettingDialog.SEPARATOR.equals(str)) {
                return this.separator;
            }
            if (isSelected) {
                this.setBackground(list.getSelectionBackground());
                this.setForeground(list.getSelectionForeground());
            } else {
                this.setBackground(list.getBackground());
                this.setForeground(list.getForeground());
            }
            this.setFont(list.getFont());
            this.setText(str);
            return this;
        }
    }

    // Required listener to handle the separator bar.
    private static class BlockComboListener implements ActionListener {
        final JComboBox<String> combo;
        Object currentItem;

        BlockComboListener(final JComboBox<String> combo) {
            this.combo = combo;
            combo.setSelectedIndex(0);
            this.currentItem = combo.getSelectedItem();
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final String tempItem = (String) this.combo.getSelectedItem();
            if (AbstractFontSettingDialog.SEPARATOR.equals(tempItem)) {
                this.combo.setSelectedItem(this.currentItem);
            } else {
                this.currentItem = tempItem;
            }
        }
    }

}
