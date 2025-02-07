package rars.riscv.hardware.registerFiles

import arrow.core.Either
import arrow.core.raise.either
import rars.exceptions.SimulationError
import rars.notices.RegisterAccessNotice
import rars.riscv.hardware.registers.Register
import rars.util.Listener
import rars.util.translateToIntFast

abstract class RegisterFileBase protected constructor(
    private val registerNumberPrefix: Char,
    protected val myRegisters: Array<Register>
) {
    fun updateRegisterByName(registerName: String, newValue: Long): Either<SimulationError, Long?> = either {
        val register = this@RegisterFileBase.getRegisterByName(registerName)
        if (register == null) null
        else this@RegisterFileBase.updateRegister(register, newValue).bind()
    }

    /**
     * Returns all the registers in the register file.
     *
     * @return An array of all the registers in the register file.
     */
    open val registers: Array<Register> = myRegisters

    protected abstract fun convertFromLong(value: Long): Int

    fun getLongValue(register: Register): Long = register.getValue()

    fun getIntValue(register: Register): Int = this.convertFromLong(register.getValue())

    fun getIntValue(registerName: String): Int? {
        val register = this.getRegisterByName(registerName)
        if (register == null) {
            return null
        }
        return this.convertFromLong(register.getValue())
    }

    fun getIntValue(registerNumber: Int): Int? {
        val register = this.getRegisterByNumber(registerNumber)
        if (register == null) {
            return null
        }
        return this.convertFromLong(register.getValue())
    }

    fun getLongValue(registerName: String): Long? {
        val register = this.getRegisterByName(registerName)
        if (register == null) {
            return null
        }
        return register.getValue()
    }

    fun getLongValue(registerNumber: Int): Long? {
        val register = this.getRegisterByNumber(registerNumber)
        if (register == null) {
            return null
        }
        return this.getLongValue(register)
    }

    fun updateRegisterByNumber(registerNumber: Int, newValue: Long): Either<SimulationError, Long?> = either {
        val register = this@RegisterFileBase.getRegisterByNumber(registerNumber)
        if (register == null) null
        else this@RegisterFileBase.updateRegister(register, newValue).bind()
    }

    abstract fun updateRegister(register: Register, newValue: Long): Either<SimulationError, Long>

    fun getRegisterByNumber(registerNumber: Int): Register? {
        for (register in this.myRegisters) {
            if (register.number == registerNumber) {
                return register
            }
        }
        return null
    }

    fun getRegisterByName(name: String): Register? {
        if (name.length < 2) return null

        // Handle a direct name
        this.myRegisters.find { it.name == name }?.let { return it }

        // Handle prefix case
        return if (name[0] == this.registerNumberPrefix) {
            // Ensure that it is a normal decimal number
            if (name[1].code == 0) {
                if (name.length > 2) null
                else this.getRegisterByNumber(0)
            } else {
                val integerNumber = name.substring(1).translateToIntFast()
                integerNumber?.let { this.getRegisterByNumber(it) }
            }
        } else null
    }

    open fun resetRegisters() {
        for (register in this.myRegisters) {
            register.resetValue()
        }
    }

    fun addRegistersListener(listener: Listener<RegisterAccessNotice>) {
        for (register in this.myRegisters) {
            register.registerChangeHook.subscribe(listener)
        }
    }

    fun deleteRegistersListener(listener: Listener<RegisterAccessNotice>) {
        for (register in this.myRegisters) {
            register.registerChangeHook.unsubscribe(listener)
        }
    }
}
