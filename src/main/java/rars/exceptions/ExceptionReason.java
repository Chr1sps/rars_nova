package rars.exceptions;

public enum ExceptionReason {
    SOFTWARE_INTERRUPT(0x80000000),
    TIMER_INTERRUPT(0x80000004),
    EXTERNAL_INTERRUPT(0x80000008),
    INSTRUCTION_ADDR_MISALIGNED(0),
    INSTRUCTION_ACCESS_FAULT(1),
    ILLEGAL_INSTRUCTION(2),
    LOAD_ADDRESS_MISALIGNED(4),
    LOAD_ACCESS_FAULT(5),
    STORE_ADDRESS_MISALIGNED(6),
    STORE_ACCESS_FAULT(7),
    ENVIRONMENT_CALL(8),
    OTHER(-1);
    public final int value;

    ExceptionReason(final int value) {
        this.value = value;
    }

    public boolean isInterrupt() {
        return switch (this) {
            case SOFTWARE_INTERRUPT, TIMER_INTERRUPT, EXTERNAL_INTERRUPT ->
                    true;
            default -> false;
        };
    }
}
