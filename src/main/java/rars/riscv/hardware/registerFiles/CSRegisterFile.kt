package rars.riscv.hardware.registerFiles

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import rars.Globals
import rars.exceptions.ExceptionReason
import rars.exceptions.SimulationError
import rars.riscv.hardware.registers.LinkedRegister
import rars.riscv.hardware.registers.MaskedRegister
import rars.riscv.hardware.registers.ReadOnlyRegister
import rars.riscv.hardware.registers.Register
import rars.settings.OtherSettings.Companion.isBacksteppingEnabled

class CSRegisterFile : RegisterFileBase('_', createRegisters()) {
    val ustatus: Register = this.registers[0]
    val fflags: Register = this.registers[1]
    val frm: Register = this.registers[2]
    val fcsr: Register = this.registers[3]
    val uie: Register = this.registers[4]
    val utvec: Register = this.registers[5]
    val uscratch: Register = this.registers[6]
    val uepc: Register = this.registers[7]
    val ucause: Register = this.registers[8]
    val utval: Register = this.registers[9]
    val uip: Register = this.registers[10]
    val cycle: Register = this.registers[11]
    val time: Register = this.registers[12]
    val instret: Register = this.registers[13]
    val cycleh: Register = this.registers[14]
    val timeh: Register = this.registers[15]
    val instreth: Register = this.registers[16]

    override fun convertFromLong(value: Long): Int {
        return value.toInt()
    }

    override fun updateRegister(register: Register, newValue: Long): Either<SimulationError, Long> = either {
        ensure(!(register is ReadOnlyRegister || register === cycleh || register === timeh || register === instreth)) {
            SimulationError.create(
                "Attempt to write to read-only CSR",
                ExceptionReason.ILLEGAL_INSTRUCTION
            )
        }
        val previousValue = register.setValue(newValue)
        if (isBacksteppingEnabled) {
            Globals.PROGRAM!!.backStepper!!.addControlAndStatusRestore(register.number, previousValue)
        }
        previousValue
    }

    fun updateRegisterBackdoor(register: Register, newValue: Long): Long {
        val previousValue = register.setValueNoNotify(newValue)
        if (isBacksteppingEnabled) {
            Globals.PROGRAM!!.backStepper!!.addControlAndStatusBackdoor(
                register.number,
                previousValue
            )
        }
        return previousValue
    }

    fun updateRegisterBackdoorByNumber(registerNumber: Int, newValue: Long): Long? {
        val register = this.getRegisterByNumber(registerNumber)
        if (register == null) {
            return null
        }
        return this.updateRegisterBackdoor(register, newValue)
    }

    fun getLongValueNoNotifyByName(name: String): Long? {
        val register = this.getRegisterByName(name)
        if (register == null) {
            return null
        }
        return register.valueNoNotify
    }

    companion object {
        const val EXTERNAL_INTERRUPT: Int = 0x100
        const val TIMER_INTERRUPT: Int = 0x10
        const val SOFTWARE_INTERRUPT: Int = 0x1
        const val INTERRUPT_ENABLE: Int = 0x1

        private fun createRegisters(): Array<Register> {
            val fcsr = MaskedRegister("fcsr", 0x003, 0, 0xFF.inv())

            val fflags = LinkedRegister("fflags", 0x001, fcsr, 0x1F)
            val frm = LinkedRegister("frm", 0x002, fcsr, 0xE0)

            val cycle = ReadOnlyRegister("cycle", 0xC00, 0)
            val time = ReadOnlyRegister("time", 0xC01, 0)
            val instret = ReadOnlyRegister("instret", 0xC02, 0)

            val cycleh = LinkedRegister("cycleh", 0xC80, cycle, -0x100000000L)
            val timeh = LinkedRegister("timeh", 0xC81, time, -0x100000000L)
            val instreth = LinkedRegister("instreth", 0xC82, instret, -0x100000000L)

            return arrayOf(
                MaskedRegister("ustatus", 0x000, 0, 0x11.inv()),
                fflags,
                frm,
                fcsr,
                Register("uie", 0x004, 0),
                Register("utvec", 0x005, 0),
                Register("uscratch", 0x040, 0),
                Register("uepc", 0x041, 0),
                Register("ucause", 0x042, 0),
                Register("utval", 0x043, 0),
                Register("uip", 0x044, 0),
                cycle,
                time,
                instret,
                cycleh,
                timeh,
                instreth,
            )
        }
    }
}
