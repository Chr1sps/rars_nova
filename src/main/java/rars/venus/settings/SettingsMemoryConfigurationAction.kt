package rars.venus.settings

import rars.Globals
import rars.riscv.hardware.memory.*
import rars.settings.OtherSettings
import rars.util.toHexStringWithPrefix
import rars.venus.FileStatus
import rars.venus.FileStatus.Existing.Running
import rars.venus.FileStatus.Existing.Terminated
import rars.venus.VenusUI
import rars.venus.actions.GuiAction
import rars.venus.util.*
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Frame
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

private class ConfigurationButton(
    val configuration: MemoryConfiguration,
    otherSettings: OtherSettings,
) : JRadioButton(
    configuration.description,
    configuration == otherSettings.memoryConfiguration
)

/**
 * Action class for the Settings menu item for text editor settings.
 */
class SettingsMemoryConfigurationAction(
    name: String,
    icon: Icon?,
    descrip: String,
    mnemonic: Int?,
    accel: KeyStroke?,
    mainUI: VenusUI
) : GuiAction(name, descrip, icon, mnemonic, accel, mainUI) {
    /**
     * {@inheritDoc}
     *
     *
     * When this action is triggered, launch a dialog to view and modify
     * editor settings.
     */
    override fun actionPerformed(e: ActionEvent?) {
        val configDialog: JDialog = MemoryConfigurationDialog(
            this.mainUI,
            "Memory Configuration",
            true
        )
        configDialog.isVisible = true
    }

    private inner class MemoryConfigurationDialog(
        owner: Frame?,
        title: String?,
        modality: Boolean
    ) : JDialog(owner, title, modality) {
        private var addressDisplay =
            Array(MEMORY_CONFIGURATION_ADDRESSES_COUNT) {
                JTextField().apply {
                    isEditable = false
                    font = Globals.FONT_SETTINGS.currentFont
                    isFocusable = false
                }
            }
        private var nameDisplay =
            Array(MEMORY_CONFIGURATION_ADDRESSES_COUNT) { JLabel() }
        lateinit var selectedConfigurationButton: ConfigurationButton
        lateinit var initialConfigurationButton: ConfigurationButton

        init {
            contentPane = buildDialogPanel()
            defaultCloseOperation = DO_NOTHING_ON_CLOSE
            onWindowClosing { performClose() }
            pack()
            setLocationRelativeTo(owner)
        }

        private fun buildDialogPanel(): JPanel = JPanel {
            border = EmptyBorder(10, 10, 10, 10)
            this.BorderLayout {
                this[BorderLayout.CENTER] = JPanel {
                    FlowLayout {
                        +buildConfigChooser()
                        +buildConfigDisplay()
                    }
                }
                this[BorderLayout.SOUTH] = buildControlPanel()
            }
        }

        private fun buildConfigChooser() = JPanel {
            val choices = ButtonGroup()
            GridLayout(4, 1) {
                MemoryConfiguration.entries
                    .withIndex()
                    .forEach { (index, configuration) ->
                        val button = ConfigurationButton(
                            configuration,
                            Globals.OTHER_SETTINGS
                        ).apply {
                            addActionListener { e ->
                                val button = e.source as ConfigurationButton
                                val config = button.configuration
                                setConfigDisplay(config)
                                selectedConfigurationButton = button
                            }
                        }
                        if (button.isSelected) {
                            selectedConfigurationButton = button
                            initialConfigurationButton = button
                        }
                        choices.add(button)
                        this[index, 0] = button
                    }
            }
            border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(
                    Color.BLACK
                ), "Configuration"
            )
        }

        private fun buildConfigDisplay(): Component {
            val numItems = MEMORY_CONFIGURATION_ADDRESSES_COUNT
            Globals.FONT_SETTINGS.onChangeListenerHook.subscribe {
                for (textField in this.addressDisplay) {
                    textField.font = Globals.FONT_SETTINGS.currentFont
                }
            }
            // Display vertically from high to low memory addresses so
            // add the components in reverse order.
//            val namesPanel = JPanel(GridLayout(numItems, 1))
            val namesPanel = JPanel {
                GridLayout(numItems, 1) {
                    nameDisplay.reversed()
                        .withIndex()
                        .forEach { (index, label) ->
                            this[index, 0] = label
                        }
                }
                border = BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    "Memory Addresses",
                )
            }
            val valuesPanel = JPanel {
                GridLayout(rows = numItems, cols = 1) {
                    addressDisplay.reversed()
                        .withIndex()
                        .forEach { (index, textField) ->
                            this[index, 0] = textField
                        }
                }
            }
            val config = Globals.OTHER_SETTINGS.memoryConfiguration
            setConfigDisplay(config)
            val columns = Box.createHorizontalBox().apply {
                add(valuesPanel)
                add(Box.createHorizontalStrut(6))
                add(namesPanel)
            }
            return JPanel {
                add(columns)
            }
        }


        // Row of control buttons to be placed along the button of the dialog
        private fun buildControlPanel(): Component {
            val okButton = JButton("Apply and Close").apply {
                toolTipText = CLOSE_TOOL_TIP_TEXT
                addActionListener {
                    performApply()
                    performClose()
                }
            }
            val applyButton = JButton("Apply").apply {
                toolTipText = APPLY_TOOL_TIP_TEXT
                addActionListener { performApply() }
            }
            val cancelButton = JButton("Cancel").apply {
                toolTipText = CANCEL_TOOL_TIP_TEXT
                addActionListener { performClose() }
            }
            val resetButton = JButton("Reset").apply {
                toolTipText = RESET_TOOL_TIP_TEXT
                addActionListener { performReset() }
            }
            return Box.createHorizontalBox().apply {
                add(Box.createHorizontalGlue())
                add(okButton)
                add(Box.createHorizontalGlue())
                add(applyButton)
                add(Box.createHorizontalGlue())
                add(cancelButton)
                add(Box.createHorizontalGlue())
                add(resetButton)
                add(Box.createHorizontalGlue())
            }
        }

        private fun performApply() {
            val currentConfiguration =
                Globals.OTHER_SETTINGS.memoryConfiguration
            val newConfiguration =
                this.selectedConfigurationButton.configuration
            if (newConfiguration != currentConfiguration) {
                Globals.OTHER_SETTINGS.setMemoryConfigurationAndSave(
                    newConfiguration
                )
                Globals.setupGlobalMemoryConfiguration(newConfiguration)
                mainUI.apply {
                    registersPane.registersWindow.apply {
                        clearHighlighting()
                        updateRegisters()
                    }
                    mainPane.executePane.dataSegment.updateBaseAddressComboBox()
                }
                val globalStatus = mainUI.fileStatus!!
                if (globalStatus is FileStatus.Existing.Runnable ||
                    globalStatus is Running ||
                    globalStatus is Terminated
                ) {
                    if (globalStatus is Running) {
                        Globals.SIMULATOR.stopExecution()
                    }
                    mainUI.runAssembleAction.actionPerformed(null)
                }
            }
        }

        private fun performClose() {
            isVisible = false
            dispose()
        }

        private fun performReset() {
            selectedConfigurationButton = initialConfigurationButton
            selectedConfigurationButton.isSelected = true
            setConfigDisplay(selectedConfigurationButton.configuration)
        }

        // Set name values in JLabels and address values in the JTextFields
        private fun setConfigDisplay(config: AbstractMemoryConfiguration<Int>) {
            val labelAddressMap = config.run {
                listOf(
                    textSegmentBaseAddress to ".text base address",
                    dataSegmentBaseAddress to ".data base address",
                    externAddress to ".extern base address",
                    globalPointerAddress to "global pointer (gp)",
                    dataBaseAddress to ".data base address",
                    heapBaseAddress to "heap base address",
                    stackPointerAddress to "stack pointer (sp)",
                    stackBaseAddress to "stack base address",
                    userHighAddress to "user space high address",
                    kernelBaseAddress to "kernel space base address",
                    memoryMapBaseAddress to "MMIO base address",
                    kernelHighAddress to "kernel space high address",
                    dataSegmentLimitAddress to "data segment limit address",
                    textSegmentLimitAddress to "text limit address",
                    heapBaseAddress to "stack limit address",
                    memoryMapLimitAddress to "memory map limit address"
                )
            }
            val sorted = labelAddressMap.sortedWith(
                compareBy({ it.first }, { it.second })
            )
            sorted.withIndex().forEach { (index, pair) ->
                val (address, description) = pair
                nameDisplay[index].text = description
                addressDisplay[index].text = address.toHexStringWithPrefix()
            }
        }
    }
}
