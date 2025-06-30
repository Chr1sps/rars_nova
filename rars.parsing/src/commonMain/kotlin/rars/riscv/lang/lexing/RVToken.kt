package rars.riscv.lang.lexing

data class RVToken(
    val startOffset: Int,
    val endOffset: Int,
    val type: RVTokenType,
    val text: CharSequence
)
