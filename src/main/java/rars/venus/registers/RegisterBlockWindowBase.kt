package rars.venus.registers

import rars.Globals
import rars.api.DisplayFormat
import rars.notices.AccessType
import rars.notices.RegisterAccessNotice
import rars.notices.SimulatorNotice
import rars.riscv.hardware.registerfiles.AbstractRegisterFile
import rars.riscv.hardware.registers.Register
import rars.settings.AllSettings
import rars.settings.BoolSetting
import rars.settings.BoolSettings
import rars.util.IntRefCell
import rars.util.translateToInt
import rars.util.translateToLong
import rars.venus.VenusUI
import rars.venus.run.RunSpeedPanel
import rars.venus.util.BorderLayout
import rars.venus.util.SettingsBasedCellRenderer
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.event.TableModelEvent
import javax.swing.table.AbstractTableModel
import javax.swing.table.JTableHeader
import kotlin.concurrent.withLock

/**
 * Sets up a window to display registers in the UI.
 *
 * @author Sanderson, Bumgarner
 */
abstract class RegisterBlockWindowBase internal constructor(
    private val registerFile: AbstractRegisterFile,
    registerDescriptions: Array<String>,
    valueTip: String,
    private val mainUI: VenusUI,
    protected val settings: AllSettings
) : JPanel() {
    @JvmField
    val processRegisterNotice = { notice: RegisterAccessNotice ->
        if (notice.accessType == AccessType.WRITE) {
            // Uses the same highlighting technique as for Text Segment -- see
            // AddressCellRenderer class in DataSegmentWindow.java.
            highlightCellForRegister(notice.register)
            mainUI.registersPane.selectedComponent = this
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
    private var highlightRow = IntRefCell(-1)

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
                if (notice.runSpeed != RunSpeedPanel.UNLIMITED_SPEED || notice.maxSteps == 1) {
                    beginObserving()
                }
            } else {
                // Simulated MIPS execution stops.  Stop responding.
                endObserving()
            }
        }
        updateRowHeight()
        settings.fontSettings.onChangeListenerHook.subscribe { updateRowHeight() }
        table.columnModel.apply {
            fun makeRenderer(alignment: Int) = RegisterCellRenderer(
                settings,
                alignment,
                table,
                mainUI,
                ::formatRegisterValue,
                highlightRow,
            )

            fun setupColumn(
                index: Int,
                width: Int,
                alignment: Int
            ) = getColumn(index).apply {
                minWidth = width
                maxWidth = width
                cellRenderer = makeRenderer(alignment)
            }
            setupColumn(NAME_COLUMN, NAME_SIZE, SwingConstants.LEFT)
            setupColumn(NUMBER_COLUMN, NUMBER_SIZE, SwingConstants.RIGHT)
            setupColumn(VALUE_COLUMN, VALUE_SIZE, SwingConstants.RIGHT)
        }

        table.preferredScrollableViewportSize = Dimension(
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

    protected abstract fun formatRegisterValue(
        value: Long,
        format: DisplayFormat
    ): String

    private fun beginObserving() {
        registerFile.addRegistersListener(processRegisterNotice)
    }

    private fun endObserving() {
        registerFile.deleteRegistersListener(processRegisterNotice)
    }

    private fun resetRegisters() {
        registerFile.resetRegisters()
    }

    /**
     * Reset and redisplay registers
     */
    fun clearWindow() {
        clearHighlighting()
        resetRegisters()
        updateRegisters()
    }

    /**
     * Clear highlight background color from any row currently highlighted.
     */
    fun clearHighlighting() {
        this.table.tableChanged(TableModelEvent(this.table.model))
        this.highlightRow.value = -1
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
        val registers = registerFile.registers
        for (i in registers.indices) {
            if (registers[i] === register) {
                table.tableChanged(TableModelEvent(table.model))
                this.highlightRow.value = i
                return
            }
        }
        this.highlightRow.value = -1
    }

    private fun updateRowHeight() {
        val font = settings.fontSettings.currentFont
        val height = getFontMetrics(font).height
        table.rowHeight = height
    }


    /**
     * JTable subclass to provide custom tool tips for each of the
     * register table column headers and for each register name in
     * the first column. From
     * [Sun's JTable tutorial](http://java.sun.com/docs/books/tutorial/uiswing/components/table.html).
     */
    private inner class MyTippedJTable(
        model: RegisterTableModel,
        private val regToolTips: Array<String>,
        private val columnToolTips: Array<String>
    ) : JTable(model) {
        init {
            // highlights background color of entire row
            rowSelectionAllowed = true
            selectionBackground = Color.GREEN
        }

        override fun getToolTipText(event: MouseEvent): String? {
            // Implement table cell tool tips.
            val point = event.point
            val rowIndex = rowAtPoint(point)
            val colIndex = columnAtPoint(point)
            val realColumnIndex = convertColumnIndexToModel(colIndex)
            return if (realColumnIndex == NAME_COLUMN) {
                // Register name column
                regToolTips[rowIndex]
            } else {
                // You can omit this part if you know you don't have any
                // renderers that supply their own tool tips.
                super.getToolTipText(event)
            }
        }

        // Implement table header tool tips.
        override fun createDefaultTableHeader(): JTableHeader =
            object : JTableHeader(columnModel) {
                override fun getToolTipText(event: MouseEvent): String? {
                    val point = event.point
                    val index = columnModel.getColumnIndexAtX(point.x)
                    val realIndex = columnModel.getColumn(index).modelIndex
                    return columnToolTips[realIndex]
                }
            }
    }
}

private const val NUMBER_COLUMN = 0
private const val NAME_COLUMN = 1
private const val VALUE_COLUMN = 2
private const val NUMBER_SIZE = 45
private const val NAME_SIZE = 80
private const val VALUE_SIZE = 160

private class RegisterTableModel(
    private val registersFile: AbstractRegisterFile,
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

    override fun getValueAt(rowIndex: Int, columnIndex: Int) =
        registers[rowIndex].let {
            when (columnIndex) {
                NUMBER_COLUMN -> it.number
                NAME_COLUMN -> it.name
                VALUE_COLUMN -> it.value
                else -> error("Invalid column index")
            }
        }

    override fun getColumnClass(c: Int): Class<*> =
        getValueAt(0, c).javaClass

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean =
        columnIndex == VALUE_COLUMN

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
        memoryAndRegistersLock.withLock {
            registersFile.registers[row].value = newValue
        }
        registersFile.registers[row].value = newValue
        fireTableCellUpdated(row, col)
    }
}

/**
 * Cell renderer for displaying register entries. This does highlighting, so if you
 * don't want highlighting for a given column, don't use this. Currently we highlight
 * all columns.
 */
private class RegisterCellRenderer(
    settings: AllSettings,
    alignment: Int,
    table: JTable,
    private val mainUI: VenusUI,
    private val formatRegisterValue: (Long, DisplayFormat) -> String,
    var highlightRow: IntRefCell,
) : SettingsBasedCellRenderer(settings, alignment, table) {
    init {
        settings.boolSettings.onChangeListenerHook.subscribe { table.repaint() }
    }

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?,
        isSelected: Boolean, hasFocus: Boolean,
        row: Int, column: Int
    ): Component {
        val formattedValue = if (column == VALUE_COLUMN) {
            val value = value as Long
            val displayBase = mainUI.mainPane.executePane.valueDisplayFormat
            formatRegisterValue(value, displayBase)
        } else value
        val cell = super.getTableCellRendererComponent(
            table, formattedValue,
            isSelected, hasFocus, row, column
        )
        return cell.apply {
            if (settings.boolSettings.getSetting(BoolSetting.REGISTERS_HIGHLIGHTING) && row == highlightRow.value) {
                val highlightingStyle =
                    settings.highlightingSettings.registerHighlightingStyle!!
                foreground = highlightingStyle.foreground
                background = highlightingStyle.background
            }
        }
    }
}
