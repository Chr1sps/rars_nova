package rars.assembler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.ErrorList;
import rars.Globals;
import rars.logging.RARSLogger;
import rars.logging.RARSLogging;
import rars.util.BinaryUtilsKt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static rars.logging.LoggingKt.debug;

/**
 * Represents a table of Symbol objects.
 *
 * @author Jason Bumgarner, Jason Shrewsbury
 * @version June 2003
 */
public final class SymbolTable {
    // Note -1 is legal 32-bit address (0xFFFFFFFF) but it is the high address in
    // kernel address space so highly unlikely that any symbol will have this as
    // its associated address!

    public static final int NOT_FOUND = -1;
    private static final @NotNull RARSLogger LOGGER = RARSLogging.forJavaClass(
        SymbolTable.class
    );
    private static final @NotNull String START_LABEL = "main";
    private final @Nullable File file;
    private final @Nullable SymbolTable globalSymbolTable;
    private @NotNull ArrayList<@NotNull Symbol> table;

    /**
     * Create a new empty symbol table for given file
     *
     * @param file
     *     file this symbol table is associated with. Will be
     *     used only for output/display so it can be any descriptive
     *     string.
     */
    public SymbolTable(
        final @Nullable File file,
        final @NotNull SymbolTable globalSymbolTable
    ) {
        this.file = file;
        this.table = new ArrayList<>();
        this.globalSymbolTable = globalSymbolTable;
    }

    /**
     * Special constructor only for the global symbol table.
     */
    public SymbolTable() {
        this.file = null;
        this.table = new ArrayList<>();
        this.globalSymbolTable = null;
    }

    /**
     * Fetches the text segment label (symbol) which, if declared global, indicates
     * the starting address for execution.
     *
     * @return String containing global label whose text segment address is starting
     * address for program execution.
     */
    public static @NotNull String getStartLabel() {
        return SymbolTable.START_LABEL;
    }

    /**
     * Adds a Symbol object into the array of Symbols.
     *
     * @param token
     *     The token representing the Symbol.
     * @param address
     *     The address of the Symbol.
     * @param isData
     *     The type of Symbol, true for data, false for text.
     * @param errors
     *     List to which to add any processing errors that occur.
     */
    public void addSymbol(
        final @NotNull Token token,
        final int address,
        final boolean isData,
        final @NotNull ErrorList errors
    ) {
        final var label = token.getText();
        if (this.getSymbol(label) != null) {
            errors.addTokenError(
                token,
                "label \"%s\" already defined".formatted(label)
            );
        } else {
            this.table.add(new Symbol(label, address, isData));
            if (Globals.debug) {
                debug(LOGGER,
                    () -> "The symbol %s with address %d has been added to the %s symbol table.".formatted(
                        label,
                        address,
                        file == null ? "global" : file.getAbsolutePath()
                    )
                );
            }
        }
    }

    /**
     * Removes a symbol from the Symbol table. If not found, it does nothing.
     * This will rarely happen (only when variable is declared .globl after already
     * being defined in the local symbol table).
     *
     * @param token
     *     The token representing the Symbol.
     */
    public void removeSymbol(final @NotNull Token token) {
        final var label = token.getText();
        final var removed = this.table.removeIf(symbol ->
            symbol.name().equals(label)
        );
        if (removed && Globals.debug) {
            debug(LOGGER, () ->
                "The symbol %s has been removed from the %s symbol table.".formatted(
                    label,
                    (this.file == null) ? "global" : this.file.getAbsolutePath()
                )
            );
        }
    }

    /**
     * Method to return the address associated with the given label.
     *
     * @param label
     *     The label.
     * @return The memory address of the label given, or NOT_FOUND if not found in
     * symbol table.
     */
    public int getAddress(final @NotNull String label) {
        return this.table
            .stream()
            .filter(symbol -> symbol.name().equals(label))
            .findAny()
            .map(Symbol::address)
            .orElse(SymbolTable.NOT_FOUND);
    }

    /**
     * Method to return the address associated with the given label. Look first
     * in this (local) symbol table then in symbol table of labels declared
     * global (.globl directive).
     *
     * @param label
     *     The label.
     * @return The memory address of the label given, or NOT_FOUND if not found in
     * symbol table.
     */
    public int getAddressLocalOrGlobal(final @NotNull String label) {
        final int address = this.getAddress(label);
        if (address != SymbolTable.NOT_FOUND) {
            return address;
        } else if (this.globalSymbolTable != null) {
            return this.globalSymbolTable.getAddress(label);
        } else {
            return SymbolTable.NOT_FOUND;
        }
    }

    /**
     * Produce Symbol object from symbol table that corresponds to given String.
     *
     * @param s
     *     target String
     * @return Symbol object for requested target, null if not found in symbol
     * table.
     */
    public @Nullable Symbol getSymbol(final @NotNull String s) {
        return this.table.stream()
            .filter(symbol -> symbol.name().equals(s))
            .findAny()
            .orElse(null);
    }

    /**
     * Produce Symbol object from symbol table that has the given address.
     *
     * @param addressString
     *     String representing address
     * @return Symbol object having requested address, null if address not found in
     * symbol table.
     */
    private @Nullable Symbol getSymbolGivenAddress(final @NotNull String addressString) {
        final var address = BinaryUtilsKt.stringToInt(addressString);
        if (address == null) return null;
        for (final Symbol sym : this.table) {
            if (sym.address() == address) {
                return sym;
            }
        }
        return null;
    }

    /**
     * Produce Symbol object from either local or global symbol table that has the
     * given address.
     *
     * @param addressString
     *     String representing address
     * @return Symbol object having requested address, null if address not found in
     * symbol table.
     */
    public @Nullable Symbol getSymbolGivenAddressLocalOrGlobal(
        final @NotNull String addressString
    ) {
        final var foundSymbol = this.getSymbolGivenAddress(addressString);
        if (foundSymbol != null) {
            return foundSymbol;
        } else if (this.globalSymbolTable != null) {
            return this.globalSymbolTable.getSymbolGivenAddress(addressString);
        } else {
            return null;
        }
    }

    /**
     * For obtaining the Data Symbols.
     *
     * @return An ArrayList of Symbol objects.
     */
    public @NotNull List<@NotNull Symbol> getDataSymbols() {
        return table.stream().filter(Symbol::isData).toList();
    }

    /**
     * For obtaining the Text Symbols.
     *
     * @return An ArrayList of Symbol objects.
     */
    public @NotNull List<@NotNull Symbol> getTextSymbols() {
        return table.stream().filter(Predicate.not(Symbol::isData)).toList();
    }

    /**
     * For obtaining all the Symbols.
     *
     * @return An ArrayList of Symbol objects.
     */
    public @NotNull List<Symbol> getAllSymbols() {
        return new ArrayList<>(this.table);
    }

    /**
     * Get the count of entries currently in the table.
     *
     * @return Number of symbol table entries.
     */
    public int getSize() {
        return this.table.size();
    }

    /**
     * Creates a fresh arrayList for a new table.
     */
    public void clear() {
        this.table = new ArrayList<>();
    }

    /**
     * Fix address in symbol table entry. Any and all entries that match the
     * original
     * address will be modified to contain the replacement address. There is no
     * effect,
     * if none of the addresses matches.
     *
     * @param originalAddress
     *     Address associated with 0 or more symtab entries.
     * @param replacementAddress
     *     Any entry that has originalAddress will have its
     *     address updated to this value. Does nothing if none
     *     do.
     */
    public void fixSymbolTableAddress(
        final int originalAddress,
        final int replacementAddress
    ) {
        this.table.replaceAll(symbol -> {
            if (symbol.address() == originalAddress) {
                return new Symbol(
                    symbol.name(),
                    replacementAddress,
                    symbol.isData()
                );
            }
            return symbol;
        });
    }
}
