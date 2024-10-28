package rars.venus.editors.jeditsyntax.tokenmarker;

import org.jetbrains.annotations.NotNull;

public record Token(int length, @NotNull TokenType type) {
}
