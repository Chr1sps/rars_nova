package rars.riscv.hardware.registerfiles

import arrow.core.Either
import arrow.core.right
import rars.Globals
import rars.events.SimulationError
import rars.riscv.hardware.registers.Register
import rars.settings.OtherSettings
import rars.util.ignoreOk
import rars.util.unwrap

class FloatingPointRegisterFile : AbstractRegisterFile('f', createRegisters()) {
    val ft0: Register = this.registers[0]

    @JvmField
    val fa0: Register = this.registers[10]

    @JvmField
    val fa1: Register = this.registers[11]

    public override fun convertFromLong(value: Long): Int =
        if ((value and -0x100000000L) == -0x100000000L) {
            value.toInt()
        } else {
            0x7FC00000
        }

    override fun updateRegister(
        register: Register,
        newValue: Long
    ): Either<SimulationError, Long> {
        val previousValue = register.setValue(newValue)
        if ((OtherSettings.isBacksteppingEnabled)) {
            Globals.PROGRAM!!.backStepper!!.addFloatingPointRestore(
                register.number,
                previousValue
            )
        }
        return previousValue.right()
    }

    fun updateRegisterByNumberInt(
        registerNumber: Int,
        value: Int
    ): Either<SimulationError, Unit> {
        // NAN box if used as float
        val longValue = value.toLong() or -0x100000000L
        return this.updateRegisterByNumber(registerNumber, longValue).ignoreOk()
    }

    fun updateRegisterByNameInt(
        registerName: String,
        value: Int
    ): Either<SimulationError, Unit> {
        // NAN box if used as float
        val longValue = value.toLong() or -0x100000000L
        return this.updateRegisterByName(registerName, longValue).ignoreOk()
    }

    fun updateRegisterInt(register: Register, value: Int) {
        // NAN box if used as float
        val longValue = value.toLong() or -0x100000000L
        this.updateRegister(register, longValue).unwrap()
    }

    /**
     * Sets the value of the FPU register given to the value given.
     *
     * @param registerNumber
     * Register to set the value of.
     * @param value
     * The desired float value for the register.
     */
    fun setRegisterToFloat(
        registerNumber: Int,
        value: Float
    ): Either<SimulationError, Unit> {
        val intValue = java.lang.Float.floatToIntBits(value)
        return this.updateRegisterByNumberInt(registerNumber, intValue)
            .ignoreOk()
    }

    fun getFloatFromRegister(registerName: String): Float? {
        val intValue = this.getInt(registerName)
        return if (intValue == null)
            null
        else
            java.lang.Float.intBitsToFloat(intValue)
    }

    fun getFloatFromRegister(register: Register): Float =
        Float.fromBits(getInt(register))

    companion object {
        private fun createRegisters() = arrayOf(
            Register("ft0", 0, 0),
            Register("ft1", 1, 0),
            Register("ft2", 2, 0),
            Register("ft3", 3, 0),
            Register("ft4", 4, 0),
            Register("ft5", 5, 0),
            Register("ft6", 6, 0),
            Register("ft7", 7, 0),
            Register("fs0", 8, 0),
            Register("fs1", 9, 0),
            Register("fa0", 10, 0),
            Register("fa1", 11, 0),
            Register("fa2", 12, 0),
            Register("fa3", 13, 0),
            Register("fa4", 14, 0),
            Register("fa5", 15, 0),
            Register("fa6", 16, 0),
            Register("fa7", 17, 0),
            Register("fs2", 18, 0),
            Register("fs3", 19, 0),
            Register("fs4", 20, 0),
            Register("fs5", 21, 0),
            Register("fs6", 22, 0),
            Register("fs7", 23, 0),
            Register("fs8", 24, 0),
            Register("fs9", 25, 0),
            Register("fs10", 26, 0),
            Register("fs11", 27, 0),
            Register("ft8", 28, 0),
            Register("ft9", 29, 0),
            Register("ft10", 30, 0),
            Register("ft11", 31, 0)
        )
    }
}
