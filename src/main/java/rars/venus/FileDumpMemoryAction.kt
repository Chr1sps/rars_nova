package rars.venus

import rars.Globals
import rars.assembler.DataTypes
import rars.riscv.dump.DumpFormat
import rars.riscv.dump.DumpFormats
import rars.util.MemoryDump
import rars.util.MemoryDump.SegmentInfo
import rars.util.toHexStringWithPrefix
import rars.venus.actions.GuiAction
import rars.venus.settings.closeDialog
import rars.venus.util.BorderLayout
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Label
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.IOException
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicComboBoxRenderer

/**
 * Action for the File -> Save For Dump Memory menu item
 */
class FileDumpMemoryAction(
    name: String, icon: Icon, description: String,
    mnemonic: Int, accel: KeyStroke, gui: VenusUI
) : GuiAction(name, icon, description, mnemonic, accel, gui) {

    override fun actionPerformed(e: ActionEvent): Unit = createDumpDialog().run {
        pack()
        setLocationRelativeTo(this@FileDumpMemoryAction.mainUI)
        isVisible = true
    }

    /** The dump dialog that appears when menu item is selected. */
    private fun createDumpDialog() = JDialog(mainUI, TITLE, true).apply {
        contentPane = buildDialogPanel(this)
        defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(we: WindowEvent?) = closeDialog()
        })
    }

    /** Set contents of dump dialog. */
    private fun buildDialogPanel(dialog: JDialog): JPanel {

        // Calculate the actual highest address to be dumped. For text segment, this depends on the
        // program length (number of machine code instructions). For data segment, this depends on
        // how many MARS 4K word blocks have been referenced during assembly and/or execution.
        // This lets user know exactly what range will be dumped.
        // Initially not editable but maybe add this later.
        // If there is nothing to dump (e.g. address of first null == base address), then
        // the segment will not be listed.
        val actualSegments = buildList {
            for (segment in MemoryDump.SEGMENTS) {
                val highAddress = Globals.MEMORY_INSTANCE.getAddressOfFirstNull(
                    segment.baseAddress,
                    segment.limitAddress
                ).fold(
                    { segment.baseAddress - DataTypes.WORD_SIZE },
                    { it?.minus(DataTypes.WORD_SIZE) }
                )
                if (highAddress != null) {
                    add(AugmentedSegmentInfo(segment, highAddress))
                }
            }
        }

        // It is highly unlikely that no segments remain after the null check, since
        // there will always be at least one instruction (.text segment has one
        // non-null).
        // But just in case...
        if (actualSegments.isEmpty()) {
            return JPanel().apply {
                border = EmptyBorder(10, 10, 10, 10)
                add(Label("There is nothing to dump!"), BorderLayout.NORTH)
                val okButton = JButton("OK")
                okButton.addActionListener { dialog.closeDialog() }
                add(okButton, BorderLayout.SOUTH)
            }
        }

        // Create segment selector. First element selected by default.
        val segmentListSelector = JComboBox(actualSegments.toTypedArray()).apply {
            selectedIndex = 0
        }
        val segmentPanel = JPanel().apply {
            BorderLayout {
                this[BorderLayout.NORTH] = JLabel("Memory Segment")
                this[BorderLayout.CENTER] = segmentListSelector
            }
        }

        // Next, create list of all available dump formats.
        val dumpFormats = DumpFormats.DUMP_FORMATS
        val formatListSelector = JComboBox(dumpFormats.toTypedArray()).apply {
            renderer = DumpFormatComboBoxRenderer()
            selectedIndex = 0
        }
        val formatPanel = JPanel().apply {
            BorderLayout {
                this[BorderLayout.NORTH] = JLabel("Dump Format")
                this[BorderLayout.CENTER] = formatListSelector
            }
        }

        // Bottom row - the control buttons for Dump and Cancel
        val cancelButton = JButton("Cancel").apply {
            addActionListener { dialog.closeDialog() }
        }
        val controlPanel = Box.createHorizontalBox().apply {
            add(Box.createHorizontalGlue())
            add(createDumpButton(dialog, formatListSelector, segmentListSelector))
            add(Box.createHorizontalGlue())
            add(cancelButton)
            add(Box.createHorizontalGlue())
        }
        val contents = JPanel().apply {
            border = EmptyBorder(10, 10, 10, 10)
            BorderLayout(vgap = 20, hgap = 20) {
                this[BorderLayout.WEST] = segmentPanel
                this[BorderLayout.EAST] = formatPanel
                this[BorderLayout.SOUTH] = controlPanel
            }
        }
        return contents
    }

    private fun createDumpButton(
        dialog: JDialog,
        formatListSelector: JComboBox<DumpFormat>,
        segmentListSelector: JComboBox<AugmentedSegmentInfo>
    ): JButton = JButton("Dump").apply {
        addActionListener {
            val selectedSegment = segmentListSelector.selectedItem as AugmentedSegmentInfo
            val wasDumped = performDump(
                selectedSegment.segmentInfo.baseAddress,
                selectedSegment.actualHighAddress,
                formatListSelector.selectedItem as DumpFormat
            )
            if (wasDumped) {
                dialog.closeDialog()
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

    // Display tool tip for dump format list items. Got the technique from
    // http://forum.java.sun.com/thread.jspa?threadID=488762&messageID=2292482
    private class DumpFormatComboBoxRenderer : BasicComboBoxRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?, value: Any, index: Int,
            isSelected: Boolean, cellHasFocus: Boolean
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            value as DumpFormat
            toolTipText = value.description
            text = value.name
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
