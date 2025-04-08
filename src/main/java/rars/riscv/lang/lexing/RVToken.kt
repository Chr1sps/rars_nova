package rars.riscv.lang.lexing;

import rars.riscv.lang.Position;

public record RVToken(Position position, RVTokenType type, String text) {

}
