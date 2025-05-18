package rars.riscv.lang.preprocessing

import rars.riscv.lang.Position

class PPLexer {
}

sealed interface PPToken {
    val position: Position

    sealed interface Keyword : PPToken
}