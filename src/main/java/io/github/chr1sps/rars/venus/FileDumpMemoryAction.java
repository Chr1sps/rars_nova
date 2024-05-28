package io.github.chr1sps.rars.venus;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.exceptions.AddressErrorException;
import io.github.chr1sps.rars.riscv.dump.DumpFormat;
import io.github.chr1sps.rars.riscv.dump.DumpFormatLoader;
import io.github.chr1sps.rars.riscv.hardware.Memory;
import io.github.chr1sps.rars.util.Binary;
import io.github.chr1sps.rars.util.MemoryDump;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the File -> Save For Dump Memory menu item
 */
public class FileDumpMemoryAction extends GuiAction {

    private JDialog dumpDialog;
    private static final String title = "Dump Memory To File";

    private int[] segmentListBaseArray;
    private int[] segmentListHighArray;

    private JComboBox<String> segmentListSelector;
    private JComboBox<DumpFormat> formatListSelector;

    private final VenusUI mainUI;

    /**
     * <p>Constructor for FileDumpMemoryAction.</p>
     *
     * @param name     a {@link java.lang.String} object
     * @param icon     a {@link javax.swing.Icon} object
     * @param descrip  a {@link java.lang.String} object
     * @param mnemonic a {@link java.lang.Integer} object
     * @param accel    a {@link javax.swing.KeyStroke} object
     * @param gui      a {@link io.github.chr1sps.rars.venus.VenusUI} object
     */
    public FileDumpMemoryAction(final String name, final Icon icon, final String descrip,
                                final Integer mnemonic, final KeyStroke accel, final VenusUI gui) {
        super(name, icon, descrip, mnemonic, accel);
        this.mainUI = gui;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        this.dumpMemory();
    }

    /*
     * Save the memory segment in a supported format.
     */
    private void dumpMemory() {
        this.dumpDialog = this.createDumpDialog();
        this.dumpDialog.pack();
        this.dumpDialog.setLocationRelativeTo(Globals.getGui());
        this.dumpDialog.setVisible(true);
    }

    // The dump dialog that appears when menu item is selected.
    private JDialog createDumpDialog() {
        final JDialog dumpDialog = new JDialog(Globals.getGui(), FileDumpMemoryAction.title, true);
        dumpDialog.setContentPane(this.buildDialogPanel());
        dumpDialog.setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE);
        dumpDialog.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent we) {
                        FileDumpMemoryAction.this.closeDialog();
                    }
                });
        return dumpDialog;
    }

    // Set contents of dump dialog.
    private JPanel buildDialogPanel() {
        final JPanel contents = new JPanel(new BorderLayout(20, 20));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));

        // A series of parallel arrays representing the memory segments that can be
        // dumped.
        final String[] segmentArray = MemoryDump.getSegmentNames();
        final int[] baseAddressArray = MemoryDump.getBaseAddresses(segmentArray);
        final int[] limitAddressArray = MemoryDump.getLimitAddresses(segmentArray);
        final int[] highAddressArray = new int[segmentArray.length];

        // These three are allocated and filled by buildDialogPanel() and used by action
        // listeners.
        String[] segmentListArray = new String[segmentArray.length];
        this.segmentListBaseArray = new int[segmentArray.length];
        this.segmentListHighArray = new int[segmentArray.length];

        // Calculate the actual highest address to be dumped. For text segment, this
        // depends on the
        // program length (number of machine code instructions). For data segment, this
        // depends on
        // how many MARS 4K word blocks have been referenced during assembly and/or
        // execution.
        // Then generate label from concatentation of segmentArray[i],
        // baseAddressArray[i]
        // and highAddressArray[i]. This lets user know exactly what range will be
        // dumped. Initially not
        // editable but maybe add this later.
        // If there is nothing to dump (e.g. address of first null == base address),
        // then
        // the segment will not be listed.
        int segmentCount = 0;

        for (int i = 0; i < segmentArray.length; i++) {
            try {
                highAddressArray[i] = Globals.memory.getAddressOfFirstNull(baseAddressArray[i], limitAddressArray[i])
                        - Memory.WORD_LENGTH_BYTES;

            } // Exception will not happen since the Memory base and limit addresses are on
            // word boundaries!
            catch (final AddressErrorException aee) {
                highAddressArray[i] = baseAddressArray[i] - Memory.WORD_LENGTH_BYTES;
            }
            if (highAddressArray[i] >= baseAddressArray[i]) {
                this.segmentListBaseArray[segmentCount] = baseAddressArray[i];
                this.segmentListHighArray[segmentCount] = highAddressArray[i];
                segmentListArray[segmentCount] = segmentArray[i] + " (" + Binary.intToHexString(baseAddressArray[i]) +
                        " - " + Binary.intToHexString(highAddressArray[i]) + ")";
                segmentCount++;
            }
        }

        // It is highly unlikely that no segments remain after the null check, since
        // there will always be at least one instruction (.text segment has one
        // non-null).
        // But just in case...
        if (segmentCount == 0) {
            contents.add(new Label("There is nothing to dump!"), BorderLayout.NORTH);
            final JButton OKButton = new JButton("OK");
            OKButton.addActionListener(
                    e -> this.closeDialog());
            contents.add(OKButton, BorderLayout.SOUTH);
            return contents;
        }

        // This is needed to assure no null array elements in ComboBox list.
        if (segmentCount < segmentListArray.length) {
            final String[] tempArray = new String[segmentCount];
            System.arraycopy(segmentListArray, 0, tempArray, 0, segmentCount);
            segmentListArray = tempArray;
        }

        // Create segment selector. First element selected by default.
        this.segmentListSelector = new JComboBox<>(segmentListArray);
        this.segmentListSelector.setSelectedIndex(0);
        final JPanel segmentPanel = new JPanel(new BorderLayout());
        segmentPanel.add(new Label("Memory Segment"), BorderLayout.NORTH);
        segmentPanel.add(this.segmentListSelector);
        contents.add(segmentPanel, BorderLayout.WEST);

        // Next, create list of all available dump formats.
        final ArrayList<DumpFormat> dumpFormats = DumpFormatLoader.getDumpFormats();
        this.formatListSelector = new JComboBox<>(dumpFormats.toArray(new DumpFormat[0]));
        this.formatListSelector.setRenderer(new DumpFormatComboBoxRenderer<>(this.formatListSelector));
        this.formatListSelector.setSelectedIndex(0);
        final JPanel formatPanel = new JPanel(new BorderLayout());
        formatPanel.add(new Label("Dump Format"), BorderLayout.NORTH);
        formatPanel.add(this.formatListSelector);
        contents.add(formatPanel, BorderLayout.EAST);

        // Bottom row - the control buttons for Dump and Cancel
        final Box controlPanel = Box.createHorizontalBox();
        final JButton dumpButton = new JButton("Dump To File...");
        dumpButton.addActionListener(
                e -> {
                    if (this.performDump(this.segmentListBaseArray[this.segmentListSelector.getSelectedIndex()],
                            this.segmentListHighArray[this.segmentListSelector.getSelectedIndex()],
                            (DumpFormat) this.formatListSelector.getSelectedItem())) {
                        this.closeDialog();
                    }
                });
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(
                e -> this.closeDialog());
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(dumpButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalGlue());
        contents.add(controlPanel, BorderLayout.SOUTH);
        return contents;
    }

    // User has clicked "Dump" button, so launch a file chooser then get
    // segment (memory range) and format selections and save to the file.
    private boolean performDump(final int firstAddress, final int lastAddress, final DumpFormat format) {
        File theFile;
        final JFileChooser saveDialog;
        boolean operationOK = false;

        saveDialog = new JFileChooser(this.mainUI.getEditor().getCurrentSaveDirectory());
        saveDialog.setDialogTitle(FileDumpMemoryAction.title);
        while (!operationOK) {
            final int decision = saveDialog.showSaveDialog(this.mainUI);
            if (decision != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            theFile = saveDialog.getSelectedFile();
            operationOK = true;
            if (theFile.exists()) {
                final int overwrite = JOptionPane.showConfirmDialog(this.mainUI,
                        "File " + theFile.getName() + " already exists.  Do you wish to overwrite it?",
                        "Overwrite existing file?",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                switch (overwrite) {
                    case JOptionPane.YES_OPTION:
                        break;
                    case JOptionPane.NO_OPTION:
                        operationOK = false;
                        break;
                    case JOptionPane.CANCEL_OPTION:
                        return false;
                    default: // should never occur
                        return false;
                }
            }
            if (operationOK) {
                try {
                    format.dumpMemoryRange(theFile, firstAddress, lastAddress, Globals.memory);
                } catch (final AddressErrorException | IOException ignored) {

                }
            }
        }
        return true;
    }

    // We're finished with this modal dialog.
    private void closeDialog() {
        this.dumpDialog.setVisible(false);
        this.dumpDialog.dispose();
    }

    // Display tool tip for dump format list items. Got the technique from
    // http://forum.java.sun.com/thread.jspa?threadID=488762&messageID=2292482

    private static class DumpFormatComboBoxRenderer<T> extends BasicComboBoxRenderer {
        private final JComboBox<T> myMaster;

        public DumpFormatComboBoxRenderer(final JComboBox<T> myMaster) {
            super();
            this.myMaster = myMaster;
        }

        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                      final boolean isSelected, final boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            this.setToolTipText(value.toString());
            if (index >= 0 && ((DumpFormat) (this.myMaster.getItemAt(index))).getDescription() != null) {
                this.setToolTipText(((DumpFormat) (this.myMaster.getItemAt(index))).getDescription());
            }
            return this;
        }
    }

}
