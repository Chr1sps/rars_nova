package rars.riscv.lang.lexing;

import java.util.List;

public record TokenizedLine(List<RVToken> tokens, int lineNumber) {
}
