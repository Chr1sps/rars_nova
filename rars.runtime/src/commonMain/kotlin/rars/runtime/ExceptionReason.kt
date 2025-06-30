package rars.runtime

sealed interface ExceptionReason {
    val value: Int

    data object IllegalInstruction : ExceptionReason {
        override val value = 0x2
    }

    data object EnvironmentCall : ExceptionReason {
        override val value = 0x8
    }

    data object Other : ExceptionReason {
        override val value = -0x1
    }

    enum class Interrupt(override val value: Int) : ExceptionReason {
        Software(-0x80000000),
        Timer(-0x7ffffffc),
        External(-0x7ffffff8),
    }

    enum class Memory(override val value: Int) : ExceptionReason {
        InstructionAddressMisaligned(0x0),
        InstructionAddressFault(0x1),
        LoadAddressMisaligned(0x4),
        LoadAddressFault(0x5),
        StoreAddressMisaligned(0x6),
        StoreAddressFault(0x7);

    }
}