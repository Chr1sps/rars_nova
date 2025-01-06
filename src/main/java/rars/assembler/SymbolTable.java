package rars.assembler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.ErrorList;
import rars.Globals;
import rars.util.BinaryUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

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
    private static final @NotNull Logger LOGGER = LogManager.getLogger(SymbolTable.class);
    private static final @NotNull String START_LABEL = "main";
    private final @Nullable File file;
    private @NotNull ArrayList<@NotNull Symbol> table;

    /**
     * Create a new empty symbol table for given file
     *
     * @param file
     *     file this symbol table is associated with. Will be
     *     used only for output/display so it can be any descriptive
     *     string.
     */
    public SymbolTable(final @Nullable File file) {
        this.file = file;
        this.table = new ArrayList<>();
    }

    public static @NotNull SymbolTable createGlobalSymbolTable() {
        return new SymbolTable(null);
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
            errors.addTokenError(token, "label \"%s\" already defined".formatted(label));
        } else {
            this.table.add(new Symbol(label, address, isData));
            if (Globals.debug) {
                SymbolTable.LOGGER.debug(
                    "The symbol {} with address {} has been added to the {} symbol table.",
                    label, address, (this.file == null) ? "global" : this.file.getAbsolutePath()
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
            SymbolTable.LOGGER.debug(
                "The symbol {} has been removed from the {} symbol table.",
                label,
                (this.file == null) ? "global" : this.file.getAbsolutePath()
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
        return (address == SymbolTable.NOT_FOUND)
            ? Globals.symbolTable.getAddress(label)
            : address;
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
    public @Nullable Symbol getSymbolGivenAddress(final @NotNull String addressString) {
        final int address;
        try {
            address = BinaryUtils.stringToInt(addressString);// DPS 2-Aug-2010: was Integer.parseInt(s) but
            // croaked
            // on hex
        } catch (final NumberFormatException e) {
            return null;
        }
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
     * @param s
     *     String representing address
     * @return Symbol object having requested address, null if address not found in
     * symbol table.
     */
    public @Nullable Symbol getSymbolGivenAddressLocalOrGlobal(final @NotNull String s) {
        final Symbol sym = this.getSymbolGivenAddress(s);
        return (sym == null) ? Globals.symbolTable.getSymbolGivenAddress(s) : sym;
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
    @Contract()
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
    public void fixSymbolTableAddress(final int originalAddress, final int replacementAddress) {
        this.table.replaceAll(symbol -> {
            if (symbol.address() == originalAddress) {
                return new Symbol(symbol.name(), replacementAddress, symbol.isData());
            }
            return symbol;
        });
    }
}
