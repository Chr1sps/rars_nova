package rars.notices

import rars.riscv.hardware.registers.Register

/**
 * Object provided to Observers of runtime access to memory or registers.
 * The access types READ and WRITE defined here; use subclasses defined for
 * MemoryAccessNotice and RegisterAccessNotice. This is abstract class.
 *
 * @author Pete Sanderson
 * @version July 2005
 */
sealed class AccessNotice protected constructor(
    /** Type of access: READ or WRITE.  */
    @JvmField val accessType: AccessType
) {
    /** Indicates whether the access is from the Simulator thread.  */
    @JvmField
    val isAccessFromRISCV: Boolean = Thread.currentThread().name.startsWith("RISCV")

}

/**
 * Object provided to Observers of runtime access to memory.
 * Observer can get the access type (R/W), address and length in bytes (4,2,1).
 *
 * @author Pete Sanderson
 * @version July 2005
 */
class MemoryAccessNotice(
    type: AccessType,
    /** Address in memory of the access operation.  */
    @JvmField val address: Int,
    /** Length in bytes of the access operation (4,2,1).  */
    val length: Int,
    /** The value of the access operation (the value read or written).  */
    @JvmField val value: Int
) : AccessNotice(type) {
    override fun toString(): String = "Memory ${accessType.repr} @$address ${length}B = $value"
}

/**
 * Object provided to Observers of runtime access to register.
 * Observer can get the access type (R/W) and register number.
 *
 * @author Pete Sanderson
 * @version July 2005
 */
class RegisterAccessNotice(
    type: AccessType,
    val register: Register
) : AccessNotice(type) {
    override fun toString(): String = "Register ${accessType.repr}: $register.name"
}
