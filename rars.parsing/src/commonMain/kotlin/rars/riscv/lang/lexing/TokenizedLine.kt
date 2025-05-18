package rars.riscv.lang.lexing

data class TokenizedLine(val tokens: List<RVToken>, val lineNumber: Int)
