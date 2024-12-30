package rars.riscv.lang.lexing;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record TokenizedLine(@NotNull List<@NotNull RVToken> tokens, int lineNumber) {
}
