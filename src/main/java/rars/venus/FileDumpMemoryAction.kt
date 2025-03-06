package rars.venus

import rars.Globals
import rars.assembler.DataTypes
import rars.riscv.dump.DumpFormat
import rars.riscv.dump.DumpFormats
import rars.util.MemoryDump
import rars.util.MemoryDump.SegmentInfo
import rars.util.toHexStringWithPrefix
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Label
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.IOException
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicComboBoxRenderer
import kotlin.collections.toTypedArray

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
class FileDumpMemoryAction(
    name: String, icon: Icon, description: String,
    mnemonic: Int, accel: KeyStroke, gui: VenusUI
) : GuiAction(name, icon, description, mnemonic, accel, gui) {
    private var dumpDialog: JDialog? = null
    private var segmentListSelector: JComboBox<AugmentedSegmentInfo>? = null
    private var formatListSelector: JComboBox<DumpFormat>? = null

    override fun actionPerformed(e: ActionEvent) = this.dumpMemory()

    /**
     * Save the memory segment in a supported format.
     */
    private fun dumpMemory() {
        this.dumpDialog = this.createDumpDialog().apply {
            pack()
            setLocationRelativeTo(mainUI)
            isVisible = true
        }
    }

    /** The dump dialog that appears when menu item is selected. */
    private fun createDumpDialog() = JDialog(mainUI, TITLE, true)
        .apply {
            contentPane = buildDialogPanel()
            defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(we: WindowEvent?) = closeDialog()
            })
        }

    /** Set contents of dump dialog. */
    private fun buildDialogPanel(): JPanel {
        val contents = JPanel(BorderLayout(20, 20))
        contents.setBorder(EmptyBorder(10, 10, 10, 10))

        val segments = MemoryDump.SEGMENTS

        // Calculate the actual highest address to be dumped. For text segment, this depends on the
        // program length (number of machine code instructions). For data segment, this depends on
        // how many MARS 4K word blocks have been referenced during assembly and/or execution.
        // This lets user know exactly what range will be dumped.
        // Initially not editable but maybe add this later.
        // If there is nothing to dump (e.g. address of first null == base address), then
        // the segment will not be listed.
        val actualSegments: ArrayList<AugmentedSegmentInfo?> = ArrayList<AugmentedSegmentInfo?>()
        for (segment in segments) {
            val highAddress = Globals.MEMORY_INSTANCE.getAddressOfFirstNull(
                segment.baseAddress,
                segment.limitAddress
            ).fold(
                { segment.baseAddress - DataTypes.WORD_SIZE },
                { it?.minus(DataTypes.WORD_SIZE) }
            )
            if (highAddress != null) {
                actualSegments.add(AugmentedSegmentInfo(segment, highAddress))
            }
        }

        // It is highly unlikely that no segments remain after the null check, since
        // there will always be at least one instruction (.text segment has one
        // non-null).
        // But just in case...
        if (segments.isEmpty()) {
            contents.add(Label("There is nothing to dump!"), BorderLayout.NORTH)
            val okButton = JButton("OK")
            okButton.addActionListener(
                ActionListener { e: ActionEvent? -> this.closeDialog() })
            contents.add(okButton, BorderLayout.SOUTH)
            return contents
        }

        // Create segment selector. First element selected by default.
        this.segmentListSelector = JComboBox(actualSegments.toTypedArray())
        this.segmentListSelector!!.setSelectedIndex(0)
        val segmentPanel = JPanel(BorderLayout())
        segmentPanel.add(Label("Memory Segment"), BorderLayout.NORTH)
        segmentPanel.add(this.segmentListSelector)
        contents.add(segmentPanel, BorderLayout.WEST)

        // Next, create list of all available dump formats.
        val dumpFormats = DumpFormats.DUMP_FORMATS
        this.formatListSelector = JComboBox(dumpFormats.toTypedArray())
        this.formatListSelector!!.setRenderer(DumpFormatComboBoxRenderer(this.formatListSelector!!))
        this.formatListSelector!!.setSelectedIndex(0)
        val formatPanel = JPanel(BorderLayout())
        formatPanel.add(Label("Dump Format"), BorderLayout.NORTH)
        formatPanel.add(this.formatListSelector)
        contents.add(formatPanel, BorderLayout.EAST)

        // Bottom row - the control buttons for Dump and Cancel
        val controlPanel = Box.createHorizontalBox()
        val dumpButton = createDumpButton()
        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener(
            ActionListener { e: ActionEvent? -> this.closeDialog() })
        controlPanel.add(Box.createHorizontalGlue())
        controlPanel.add(dumpButton)
        controlPanel.add(Box.createHorizontalGlue())
        controlPanel.add(cancelButton)
        controlPanel.add(Box.createHorizontalGlue())
        contents.add(controlPanel, BorderLayout.SOUTH)
        return contents
    }

    private fun createDumpButton(): JButton = JButton("Dump").apply {
        addActionListener { 
            val selectedSegment = segmentListSelector!!.selectedItem as AugmentedSegmentInfo?
            if (selectedSegment == null) return@addActionListener
            val wasDumped = performDump(
                selectedSegment.segmentInfo.baseAddress,
                selectedSegment.actualHighAddress,
                (formatListSelector!!.selectedItem as DumpFormat?)!!
            )
            if (wasDumped) {
                closeDialog()
            }
        }
    }

    // User has clicked "Dump" button, so launch a file chooser then get
    // segment (memory range) and format selections and save to the file.
    private fun performDump(firstAddress: Int, lastAddress: Int, format: DumpFormat): Boolean {
        val saveDialog = JFileChooser(this.mainUI.editor.currentSaveDirectory)
        saveDialog.setDialogTitle(TITLE)
        var operationOK = false
        while (!operationOK) {
            val decision = saveDialog.showSaveDialog(this.mainUI)
            if (decision != JFileChooser.APPROVE_OPTION) {
                return false
            }
            val theFile = saveDialog.selectedFile
            operationOK = true
            if (theFile.exists()) {
                val overwrite = JOptionPane.showConfirmDialog(
                    this.mainUI,
                    "File " + theFile.getName() + " already exists.  Do you wish to overwrite it?",
                    "Overwrite existing file?",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE
                )
                when (overwrite) {
                    JOptionPane.YES_OPTION -> {}
                    JOptionPane.NO_OPTION -> operationOK = false
                    JOptionPane.CANCEL_OPTION -> return false
                    else -> return false
                }
            }
            if (operationOK) {
                try {
                    format.dumpMemoryRange(theFile, firstAddress, lastAddress, Globals.MEMORY_INSTANCE)
                } catch (_: IOException) {
                }
            }
        }
        return true
    }

    // We're finished with this modal dialog.
    private fun closeDialog() {
        this.dumpDialog!!.isVisible = false
        this.dumpDialog!!.dispose()
    }

    // Display tool tip for dump format list items. Got the technique from
    // http://forum.java.sun.com/thread.jspa?threadID=488762&messageID=2292482
    private class DumpFormatComboBoxRenderer<T>(private val myMaster: JComboBox<T>) : BasicComboBoxRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?, value: Any, index: Int,
            isSelected: Boolean, cellHasFocus: Boolean
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            this.setToolTipText(value.toString())
            if (index >= 0) {
                ((this.myMaster.getItemAt(index)) as DumpFormat).description
                this.setToolTipText(((this.myMaster.getItemAt(index)) as DumpFormat).description)
            }
            return this
        }
    }

    private data class AugmentedSegmentInfo(val segmentInfo: SegmentInfo, val actualHighAddress: Int) {
        override fun toString() = String.format(
            "%s (%s - %s)",
            segmentInfo.name,
            segmentInfo.baseAddress.toHexStringWithPrefix(),
            actualHighAddress.toHexStringWithPrefix()
        )
    }

    companion object {
        private const val TITLE = "Dump Memory To File"
    }
}
