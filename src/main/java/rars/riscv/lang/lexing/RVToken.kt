package rars.riscv.lang.lexing

import rars.riscv.lang.Position

@JvmRecord
data class RVToken(val position: Position, val type: RVTokenType, val text: String)
