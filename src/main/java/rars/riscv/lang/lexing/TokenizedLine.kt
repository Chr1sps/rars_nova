package rars.riscv.lang.lexing

@JvmRecord
data class TokenizedLine(val tokens: List<RVToken>, val lineNumber: Int)
