package rars.venus.settings

import rars.settings.BoolSetting
import rars.settings.BoolSettingsImpl
import rars.settings.OtherSettingsImpl
import rars.venus.VenusUI
import rars.venus.actions.GuiAction
import rars.venus.util.BorderLayout
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * Action class for the Settings menu item for optionally loading a MIPS
 * exception handler.
 */
class SettingsExceptionHandlerAction(
    name: String,
    descrip: String,
    mainUI: VenusUI,
    private val boolSettings: BoolSettingsImpl,
    private val otherSettings: OtherSettingsImpl,
) : GuiAction(name, descrip, null, null, null, mainUI) {

    override fun actionPerformed(e: ActionEvent?) {
        JDialog(mainUI, "Exception Handler", true).apply {
            contentPane = buildDialogPanel(this, boolSettings, otherSettings)
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(we: WindowEvent?) {
                    closeDialog()
                }
            })
            defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
            pack()
            setLocationRelativeTo(mainUI)
            isVisible = true
        }
    }

    private fun buildDialogPanel(
        dialog: JDialog,
        boolSettings: BoolSettingsImpl,
        otherSettings: OtherSettingsImpl,
    ): JPanel {
        val initialSelected = boolSettings.getSetting(BoolSetting.EXCEPTION_HANDLER_ENABLED)
        val initialPath = otherSettings.exceptionHandler
        val exceptionHandlerDisplay = JTextField().apply {
            text = otherSettings.exceptionHandler
            columns = 30
            isEditable = false
            isEnabled = initialSelected
        }
        val exceptionHandlerSelectionButton = JButton().apply {
            text = "Browse"
            enabled = initialSelected
            addActionListener {
                val chooser = JFileChooser()
                var pathname = otherSettings.exceptionHandler
                val file = File(pathname)
                if (file.exists()) {
                    chooser.setSelectedFile(file)
                }
                val result = chooser.showOpenDialog(mainUI)
                if (result == JFileChooser.APPROVE_OPTION) {
                    pathname = chooser.selectedFile.path // .replaceAll("\\\\","/");
                    exceptionHandlerDisplay.text = pathname
                }
            }
        }
        val exceptionHandlerSetting = JCheckBox().apply {
            text = "Include this exception handler file in all assemble operations"
            isSelected = initialSelected
            addActionListener {
                val selected = (it.source as JCheckBox).isSelected
                exceptionHandlerSelectionButton.isEnabled = selected
                exceptionHandlerDisplay.isEnabled = selected
            }
        }
        val specifyHandlerFile = JPanel().apply {
            add(exceptionHandlerSelectionButton)
            add(exceptionHandlerDisplay)
        }
        // Bottom row - the control buttons for OK and Cancel
        val okButton = JButton("OK").apply {
            addActionListener {
                performOK(
                    boolSettings,
                    otherSettings,
                    initialSelected,
                    initialPath,
                    exceptionHandlerSetting.isSelected,
                    exceptionHandlerDisplay.text
                )
                dialog.closeDialog()
            }
        }
        val cancelButton = JButton("Cancel").apply {
            addActionListener {
                dialog.closeDialog()
            }
        }
        val controlPanel = Box.createHorizontalBox().apply {
            add(Box.createHorizontalGlue())
            add(okButton)
            add(Box.createHorizontalGlue())
            add(cancelButton)
            add(Box.createHorizontalGlue())
        }

        return JPanel().apply {
            border = EmptyBorder(10, 10, 10, 10)
            BorderLayout {
                this[BorderLayout.NORTH] = exceptionHandlerSetting
                this[BorderLayout.CENTER] = specifyHandlerFile
                this[BorderLayout.SOUTH] = controlPanel
            }
        }
    }

    companion object {
        private fun performOK(
            boolSettings: BoolSettingsImpl,
            otherSettings: OtherSettingsImpl,
            initialSelected: Boolean,
            initialPath: String,
            isSelected: Boolean,
            path: String,
        ) {
            if (initialSelected != isSelected || path.isNotEmpty() || initialPath != path) {
                boolSettings.setSettingAndSave(
                    BoolSetting.EXCEPTION_HANDLER_ENABLED,
                    isSelected
                )
                if (isSelected) {
                    otherSettings.setExceptionHandlerAndSave(path)
                }
            }
        }
    }
}

fun JDialog.closeDialog() {
    isVisible = false
    dispose()
}
