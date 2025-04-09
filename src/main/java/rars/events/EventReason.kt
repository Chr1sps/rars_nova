package rars.events

enum class EventReason(val value: Int) {
    SOFTWARE_INTERRUPT(-0x80000000),
    TIMER_INTERRUPT(-0x7ffffffc),
    EXTERNAL_INTERRUPT(-0x7ffffff8),
    INSTRUCTION_ADDR_MISALIGNED(0),
    INSTRUCTION_ACCESS_FAULT(1),
    ILLEGAL_INSTRUCTION(2),
    LOAD_ADDRESS_MISALIGNED(4),
    LOAD_ACCESS_FAULT(5),
    STORE_ADDRESS_MISALIGNED(6),
    STORE_ACCESS_FAULT(7),
    ENVIRONMENT_CALL(8),
    OTHER(-1);

    val isInterrupt
        get() = when (this) {
            SOFTWARE_INTERRUPT, TIMER_INTERRUPT, EXTERNAL_INTERRUPT -> true
            else -> false
        }
}