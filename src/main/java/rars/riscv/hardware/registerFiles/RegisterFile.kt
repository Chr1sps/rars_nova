package rars.riscv.hardware.registerFiles

import arrow.core.Either
import arrow.core.raise.either
import rars.Globals
import rars.assembler.SymbolTable
import rars.exceptions.SimulationError
import rars.riscv.hardware.MemoryConfiguration
import rars.riscv.hardware.registers.Register
import rars.settings.BoolSetting
import rars.settings.OtherSettings
import rars.util.ConversionUtils
import rars.util.unwrap

class RegisterFile(
    private val globalSymbolTable: SymbolTable,
    initialMemoryConfiguration: MemoryConfiguration
) : RegisterFileBase('x', createRegisters(initialMemoryConfiguration)) {
    val zero: Register = this.myRegisters[0]

    @JvmField
    val sp: Register = this.myRegisters[STACK_POINTER_REGISTER_INDEX]

    @JvmField
    val gp: Register = this.myRegisters[GLOBAL_POINTER_REGISTER_INDEX]

    @JvmField
    val pc: Register = Register(
        "pc",
        -1,
        initialMemoryConfiguration.textBaseAddress.toLong()
    )

    @JvmField
    val a0: Register = this.myRegisters[10]

    @JvmField
    val a1: Register = this.myRegisters[11]
    val a2: Register = this.myRegisters[12]
    val a7: Register = this.myRegisters[17]

    public override fun convertFromLong(value: Long): Int = ConversionUtils.longLowerHalfToInt(value)

    override fun updateRegister(register: Register, newValue: Long): Either<SimulationError, Long> = either {
        if (register === this@RegisterFile.zero) {
            0
        } else {
            val prevValue = register.setValue(newValue)
            if ((OtherSettings.isBacksteppingEnabled)) {
                Globals.PROGRAM!!.backStepper!!.addRegisterFileRestore(
                    register.number,
                    prevValue
                )
            }
            prevValue
        }
    }

    /**
     * Method to increment the Program counter in the general case (not a jump or
     * branch). The offset value is here to allow for non-32-bit instructions
     * (like compressed ones).
     */
    fun incrementPC(offset: Int) {
        this.pc.setValue(this.pc.getValue() + offset)
    }

    fun initializeProgramCounter(value: Int) {
        this.pc.setValue(value.toLong())
    }

    val programCounter: Int
        get() = this.pc.getValue().toInt()

    fun setProgramCounter(value: Int): Int {
        val oldValue = this.updateRegister(this.pc, value.toLong()).unwrap().toInt()
        if (OtherSettings.isBacksteppingEnabled) {
            Globals.PROGRAM!!.backStepper!!.addPCRestore(oldValue)
        }
        return oldValue
    }

    fun initializeProgramCounter(startAtMain: Boolean) {
        val mainAddr = this.globalSymbolTable.getAddress(SymbolTable.getStartLabel())
        val useMainAddr =
            startAtMain && mainAddr != SymbolTable.NOT_FOUND && Globals.MEMORY_INSTANCE.isAddressInTextSegment(
                mainAddr
            )
        val programCounterValue = if (useMainAddr) mainAddr else this.pc.resetValue.toInt()
        this.initializeProgramCounter(programCounterValue)
    }

    override fun resetRegisters() {
        val startAtMain: Boolean = Globals.BOOL_SETTINGS.getSetting(BoolSetting.START_AT_MAIN)
        val mainAddr = this.globalSymbolTable.getAddress(SymbolTable.getStartLabel())
        val useMainAddr =
            startAtMain && mainAddr != SymbolTable.NOT_FOUND && Globals.MEMORY_INSTANCE.isAddressInTextSegment(
                mainAddr
            )
        val programCounterValue = if (useMainAddr) mainAddr else this.pc.resetValue.toInt()
        this.resetRegisters(programCounterValue)
    }

    fun resetRegisters(programCounterValue: Int) {
        super.resetRegisters()
        this.initializeProgramCounter(programCounterValue)
    }

    fun setValuesFromConfiguration(configuration: MemoryConfiguration) {
        this.gp.changeResetValue(configuration.globalPointerAddress.toLong())
        this.sp.changeResetValue(configuration.stackPointerAddress.toLong())
        this.pc.changeResetValue(configuration.textBaseAddress.toLong())
        this.pc.setValue(configuration.textBaseAddress.toLong())
        this.resetRegisters()
    }

    /**
     * {@inheritDoc}
     *
     *
     * This implementation appends the program counter to the
     * end of the array.
     */
    override val registers: Array<Register> = myRegisters + pc

    companion object {
        const val GLOBAL_POINTER_REGISTER_INDEX: Int = 3
        const val STACK_POINTER_REGISTER_INDEX: Int = 2
        private fun createRegisters(initialMemoryConfiguration: MemoryConfiguration): Array<Register> {
            val sp = Register(
                "sp",
                STACK_POINTER_REGISTER_INDEX,
                initialMemoryConfiguration.stackPointerAddress.toLong()
            )
            val gp = Register(
                "gp",
                GLOBAL_POINTER_REGISTER_INDEX,
                initialMemoryConfiguration.globalPointerAddress.toLong()
            )
            val a0 = Register("a0", 10, 0)
            val a1 = Register("a1", 11, 0)
            return arrayOf(
                Register("zero", 0, 0),
                Register("ra", 1, 0),
                sp,
                gp,
                Register("tp", 4, 0),
                Register("t0", 5, 0),
                Register("t1", 6, 0),
                Register("t2", 7, 0),
                Register("s0", 8, 0),
                Register("s1", 9, 0),
                a0,
                a1,
                Register("a2", 12, 0),
                Register("a3", 13, 0),
                Register("a4", 14, 0),
                Register("a5", 15, 0),
                Register("a6", 16, 0),
                Register("a7", 17, 0),
                Register("s2", 18, 0),
                Register("s3", 19, 0),
                Register("s4", 20, 0),
                Register("s5", 21, 0),
                Register("s6", 22, 0),
                Register("s7", 23, 0),
                Register("s8", 24, 0),
                Register("s9", 25, 0),
                Register("s10", 26, 0),
                Register("s11", 27, 0),
                Register("t3", 28, 0),
                Register("t4", 29, 0),
                Register("t5", 30, 0),
                Register("t6", 31, 0)
            )
        }
    }
}
