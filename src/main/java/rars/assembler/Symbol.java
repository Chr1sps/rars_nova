package rars.assembler;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a program identifier to be stored in the symbol table.
 *
 * @param name
 *     The name of the Symbol.
 * @param address
 *     The memory address that the Symbol refers to.
 * @param isData
 *     true if it represents data, false if code.
 * @author Jason Bumgarner, Jason Shrewsbury
 * @version June 2003
 */
public record Symbol(@NotNull String name, int address, boolean isData) {
}
