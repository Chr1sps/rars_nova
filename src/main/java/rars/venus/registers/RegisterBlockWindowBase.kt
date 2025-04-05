package rars.venus.registers

import rars.Globals
import rars.notices.AccessNotice
import rars.notices.RegisterAccessNotice
import rars.notices.SimulatorNotice
import rars.riscv.hardware.registerfiles.RegisterFileBase
import rars.riscv.hardware.registers.Register
import rars.settings.AllSettings
import rars.settings.BoolSetting
import rars.settings.BoolSettings
import rars.util.translateToInt
import rars.util.translateToLong
import rars.venus.VenusUI
import rars.venus.run.RunSpeedPanel
import rars.venus.util.BorderLayout
import java.awt.*
import java.awt.event.MouseEvent
import java.util.concurrent.locks.ReentrantLock
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.JTableHeader

/**
 * Sets up a window to display registers in the UI.
 *
 * @author Sanderson, Bumgarner
 */
abstract class RegisterBlockWindowBase internal constructor(
    private val registerFile: RegisterFileBase,
    registerDescriptions: Array<String>,
    valueTip: String,
    private val mainUI: VenusUI,
    protected val settings: AllSettings
) : JPanel() {
    @JvmField
    val processRegisterNotice = { notice: RegisterAccessNotice ->
        if (notice.accessType == AccessNotice.AccessType.WRITE) {
            // Uses the same highlighting technique as for Text Segment -- see
            // AddressCellRenderer class in DataSegmentWindow.java.
            this.highlightCellForRegister(notice.register)
            this.mainUI.registersPane.setSelectedComponent(this)
        }
    }
    private val table: JTable = MyTippedJTable(
        RegisterTableModel(
            registerFile,
            settings.boolSettings,
            Globals.MEMORY_REGISTERS_LOCK,
        ),
        registerDescriptions,
        arrayOf(
            "Each register has a tool tip describing its usage convention",
            "Corresponding register number",
            valueTip
        )
    )
    private var highlightRow: Int = -1

    /**
     * Constructor which sets up a fresh window with a table that contains the
     * register values.
     *
     * @param registerFile
     * the register file to be displayed
     * @param registerDescriptions
     * an array of [java.lang.String] objects
     * @param valueTip
     * a [java.lang.String] object
     */
    init {
        Globals.SIMULATOR.simulatorNoticeHook.subscribe { notice ->
            if (notice.action == SimulatorNotice.Action.START) {
                // Simulated MIPS execution starts.  Respond to memory changes if running in timed
                // or stepped mode.
                if (notice.runSpeed.compareTo(RunSpeedPanel.UNLIMITED_SPEED) != 0 || notice.maxSteps == 1) {
                    beginObserving()
                }
            } else {
                // Simulated MIPS execution stops.  Stop responding.
                endObserving()
            }
        }
        this.updateRowHeight()
        settings.fontSettings.onChangeListenerHook.subscribe { this.updateRowHeight() }
        val columnModel = this.table.getColumnModel()

        val nameColumn = columnModel.getColumn(NAME_COLUMN)
        nameColumn.setMinWidth(NAME_SIZE)
        nameColumn.setMaxWidth(NAME_SIZE)
        nameColumn.setCellRenderer(RegisterCellRenderer(SwingConstants.LEFT, table))

        val numberColumn = columnModel.getColumn(NUMBER_COLUMN)
        numberColumn.setMinWidth(NUMBER_SIZE)
        numberColumn.setMaxWidth(NUMBER_SIZE)
        // Display register values (String-ified) right-justified in mono font
        numberColumn.setCellRenderer(RegisterCellRenderer(SwingConstants.RIGHT, table))

        val valueColumn = columnModel.getColumn(VALUE_COLUMN)
        valueColumn.setMinWidth(VALUE_SIZE)
        valueColumn.setMaxWidth(VALUE_SIZE)
        valueColumn.setCellRenderer(RegisterCellRenderer(SwingConstants.RIGHT, table))

        this.table.preferredScrollableViewportSize = Dimension(
            NAME_SIZE + NUMBER_SIZE + VALUE_SIZE,
            700
        )
        BorderLayout {
            this[BorderLayout.CENTER] = JScrollPane(
                table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            )
        }
    }

    protected abstract fun formatRegisterValue(value: Long, base: Int): String

    private fun beginObserving() {
        this.registerFile.addRegistersListener(this.processRegisterNotice)
    }

    private fun endObserving() {
        this.registerFile.deleteRegistersListener(this.processRegisterNotice)
    }

    private fun resetRegisters() {
        this.registerFile.resetRegisters()
    }

    /**
     * Reset and redisplay registers
     */
    fun clearWindow() {
        this.clearHighlighting()
        this.resetRegisters()
        this.updateRegisters()
    }

    /**
     * Clear highlight background color from any row currently highlighted.
     */
    fun clearHighlighting() {
        this.table.tableChanged(TableModelEvent(this.table.model))
        this.highlightRow = -1
    }

    fun updateRegisters() {
        (table.model as AbstractTableModel).fireTableDataChanged()
    }

    /**
     * Highlight the row corresponding to the given register.
     *
     * @param register
     * Register object corresponding to row to be selected.
     */
    private fun highlightCellForRegister(register: Register) {
        val registers = this.registerFile.registers
        for (i in registers.indices) {
            if (registers[i] === register) {
                this.highlightRow = i
                table.tableChanged(TableModelEvent(table.model))
                return
            }
        }
        this.highlightRow = -1
    }

    private fun updateRowHeight() {
        val font = this.settings.fontSettings.currentFont
        val height = this.getFontMetrics(font).height
        this.table.setRowHeight(height)
    }

    /**
     * Cell renderer for displaying register entries. This does highlighting, so if you
     * don't want highlighting for a given column, don't use this. Currently we highlight
     * all columns.
     */
    private inner class RegisterCellRenderer(
        private val alignment: Int,
        private val table: JTable,
    ) : DefaultTableCellRenderer() {
        private var font: Font?

        init {
            this.font = settings.fontSettings.currentFont
            settings.fontSettings.onChangeListenerHook.subscribe {
                this.font = settings.fontSettings.currentFont
                this.table.repaint()
            }
        }

        override fun getTableCellRendererComponent(
            table: JTable?, value: Any?,
            isSelected: Boolean, hasFocus: Boolean,
            row: Int, column: Int
        ): Component {
            val formattedValue = if (column == VALUE_COLUMN) {
                val value = value as Long
                val displayBase = this@RegisterBlockWindowBase.mainUI.mainPane.executePane.getValueDisplayBase()
                this@RegisterBlockWindowBase.formatRegisterValue(value, displayBase)
            } else value
            val cell = super.getTableCellRendererComponent(
                table, formattedValue,
                isSelected, hasFocus, row, column
            ) as JLabel
            return cell.apply {
                font = this@RegisterCellRenderer.font
                horizontalAlignment = this@RegisterCellRenderer.alignment
                if (settings.boolSettings.getSetting(BoolSetting.REGISTERS_HIGHLIGHTING) && row == highlightRow) {
                    val highlightingStyle = settings.highlightingSettings.registerHighlightingStyle
                    foreground = highlightingStyle!!.foreground
                    background = highlightingStyle.background
                } else {
                    val theme = settings.editorThemeSettings.currentTheme
                    foreground = theme.foregroundColor
                    background = theme.backgroundColor
                }
            }
        }
    }

    private class RegisterTableModel(
        private val registersFile: RegisterFileBase,
        private val boolSettings: BoolSettings,
        private val memoryAndRegistersLock: ReentrantLock,
    ) : AbstractTableModel() {
        private val registers = registersFile.registers

        companion object {
            private val columnNames = mapOf(
                NUMBER_COLUMN to "No.",
                NAME_COLUMN to "Name",
                VALUE_COLUMN to "Value"
            )
        }

        override fun getColumnName(col: Int): String = columnNames[col]!!

        override fun getRowCount() = registers.size

        override fun getColumnCount() = 3

        override fun getValueAt(rowIndex: Int, columnIndex: Int) = registers[rowIndex].let {
            when (columnIndex) {
                NUMBER_COLUMN -> it.number
                NAME_COLUMN -> it.name
                VALUE_COLUMN -> it.value
                else -> error("Invalid column index")
            }
        }

        override fun getColumnClass(c: Int): Class<*> = this.getValueAt(0, c).javaClass

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == VALUE_COLUMN

        /**
         * Update cell contents in table model. This method should be called
         * only when user edits cell, so input validation has to be done. If
         * value is valid, the register is updated.
         */
        override fun setValueAt(value: Any?, row: Int, col: Int) {
            if (value == null) return
            val newValue: Long = try {
                if (boolSettings.getSetting(BoolSetting.RV64_ENABLED)) {
                    value.toString().translateToLong() ?: return
                } else {
                    value.toString().translateToInt()!!.toLong()
                }
            } catch (_: NumberFormatException) {
                // If the user enters an invalid value, don't do anything.
                return
            }
            // Assures that if changed during program execution, the update will
            // occur only between instructions.
            this.memoryAndRegistersLock.lock()
            try {
                registersFile.registers[row].value = newValue
            } finally {
                this.memoryAndRegistersLock.unlock()
            }
            this.registersFile.registers[row].value = newValue
            this.fireTableCellUpdated(row, col)
        }
    }

    /**
     * JTable subclass to provide custom tool tips for each of the
     * register table column headers and for each register name in
     * the first column. From
     * [Sun's JTable tutorial](http://java.sun.com/docs/books/tutorial/uiswing/components/table.html).
     */
    private open inner class MyTippedJTable(
        model: RegisterTableModel,
        private val regToolTips: Array<String>,
        private val columnToolTips: Array<String>
    ) : JTable(model) {
        init {
            this.setRowSelectionAllowed(true) // highlights background color of entire row
            this.setSelectionBackground(Color.GREEN)
        }

        override fun getToolTipText(event: MouseEvent): String? {
            // Implement table cell tool tips.
            val point = event.getPoint()
            val rowIndex = this.rowAtPoint(point)
            val colIndex = this.columnAtPoint(point)
            val realColumnIndex = this.convertColumnIndexToModel(colIndex)
            return if (realColumnIndex == NAME_COLUMN) {
                // Register name column
                this.regToolTips[rowIndex]
            } else {
                // You can omit this part if you know you don't have any
                // renderers that supply their own tool tips.
                super.getToolTipText(event)
            }
        }

        // Implement table header tool tips.
        override fun createDefaultTableHeader(): JTableHeader = object : JTableHeader(this.columnModel) {
            override fun getToolTipText(event: MouseEvent): String? {
                val point = event.getPoint()
                val index = this.columnModel.getColumnIndexAtX(point.x)
                val realIndex = this.columnModel.getColumn(index).getModelIndex()
                return this@MyTippedJTable.columnToolTips[realIndex]
            }
        }
    }

    companion object {
        private const val NUMBER_COLUMN = 0
        private const val NAME_COLUMN = 1
        private const val VALUE_COLUMN = 2
        private const val NUMBER_SIZE = 45
        private const val NAME_SIZE = 80
        private const val VALUE_SIZE = 160
    }
}
