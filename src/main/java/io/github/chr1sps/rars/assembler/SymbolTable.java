package io.github.chr1sps.rars.assembler;

import io.github.chr1sps.rars.ErrorList;
import io.github.chr1sps.rars.ErrorMessage;
import io.github.chr1sps.rars.Globals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

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
 * Creats a table of Symbol objects.
 *
 * @author Jason Bumgarner, Jason Shrewsbury
 * @version June 2003
 */
public class SymbolTable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String startLabel = "main";
    private final String filename;
    private ArrayList<Symbol> table;
    // Note -1 is legal 32 bit address (0xFFFFFFFF) but it is the high address in
    // kernel address space so highly unlikely that any symbol will have this as
    // its associated address!
    /**
     * Constant <code>NOT_FOUND=-1</code>
     */
    public static final int NOT_FOUND = -1;

    /**
     * Create a new empty symbol table for given file
     *
     * @param filename name of file this symbol table is associated with. Will be
     *                 used only for output/display so it can be any descriptive
     *                 string.
     */
    public SymbolTable(final String filename) {
        this.filename = filename;
        this.table = new ArrayList<>();
    }

    /**
     * Adds a Symbol object into the array of Symbols.
     *
     * @param token   The token representing the Symbol.
     * @param address The address of the Symbol.
     * @param b       The type of Symbol, true for data, false for text.
     * @param errors  List to which to add any processing errors that occur.
     */
    public void addSymbol(final Token token, final int address, final boolean b, final ErrorList errors) {
        final String label = token.getValue();
        if (this.getSymbol(label) != null) {
            errors.add(new ErrorMessage(token.getSourceProgram(), token.getSourceLine(), token.getStartPos(),
                    "label \"" + label + "\" already defined"));
        } else {
            this.table.add(new Symbol(label, address, b));
            if (Globals.debug)
                SymbolTable.LOGGER.debug("The symbol {} with address {} has been added to the {} symbol table.", label, address, this.filename);
        }
    }

    /**
     * Removes a symbol from the Symbol table. If not found, it does nothing.
     * This will rarely happen (only when variable is declared .globl after already
     * being defined in the local symbol table).
     *
     * @param token The token representing the Symbol.
     */
    public void removeSymbol(final Token token) {
        final String label = token.getValue();
        for (int i = 0; i < this.table.size(); i++) {
            if (this.table.get(i).getName().equals(label)) {
                this.table.remove(i);
                if (Globals.debug)
                    SymbolTable.LOGGER.debug("The symbol {} has been removed from the {} symbol table.", label, this.filename);
                break;
            }
        }
    }

    /**
     * Method to return the address associated with the given label.
     *
     * @param s The label.
     * @return The memory address of the label given, or NOT_FOUND if not found in
     * symbol table.
     */
    public int getAddress(final String s) {
        for (final Symbol sym : this.table) {
            if (sym.getName().equals(s)) {
                return sym.getAddress();
            }
        }
        return SymbolTable.NOT_FOUND;
    }

    /**
     * Method to return the address associated with the given label. Look first
     * in this (local) symbol table then in symbol table of labels declared
     * global (.globl directive).
     *
     * @param s The label.
     * @return The memory address of the label given, or NOT_FOUND if not found in
     * symbol table.
     */
    public int getAddressLocalOrGlobal(final String s) {
        final int address = this.getAddress(s);
        return (address == SymbolTable.NOT_FOUND) ? Globals.symbolTable.getAddress(s) : address;
    }

    /**
     * Produce Symbol object from symbol table that corresponds to given String.
     *
     * @param s target String
     * @return Symbol object for requested target, null if not found in symbol
     * table.
     */
    public Symbol getSymbol(final String s) {
        for (final Symbol sym : this.table) {
            if (sym.getName().equals(s)) {
                return sym;
            }
        }
        return null;
    }

    /**
     * Produce Symbol object from symbol table that has the given address.
     *
     * @param s String representing address
     * @return Symbol object having requested address, null if address not found in
     * symbol table.
     */
    public Symbol getSymbolGivenAddress(final String s) {
        final int address;
        try {
            address = io.github.chr1sps.rars.util.Binary.stringToInt(s);// DPS 2-Aug-2010: was Integer.parseInt(s) but
            // croaked
            // on hex
        } catch (final NumberFormatException e) {
            return null;
        }
        for (final Symbol sym : this.table) {
            if (sym.getAddress() == address) {
                return sym;
            }
        }
        return null;
    }

    /**
     * Produce Symbol object from either local or global symbol table that has the
     * given address.
     *
     * @param s String representing address
     * @return Symbol object having requested address, null if address not found in
     * symbol table.
     */
    public Symbol getSymbolGivenAddressLocalOrGlobal(final String s) {
        final Symbol sym = this.getSymbolGivenAddress(s);
        return (sym == null) ? Globals.symbolTable.getSymbolGivenAddress(s) : sym;
    }

    /**
     * For obtaining the Data Symbols.
     *
     * @return An ArrayList of Symbol objects.
     */
    public ArrayList<Symbol> getDataSymbols() {
        final ArrayList<Symbol> list = new ArrayList<>();
        for (final Symbol sym : this.table) {
            if (sym.getType()) {
                list.add(sym);
            }
        }
        return list;
    }

    /**
     * For obtaining the Text Symbols.
     *
     * @return An ArrayList of Symbol objects.
     */
    public ArrayList<Symbol> getTextSymbols() {
        final ArrayList<Symbol> list = new ArrayList<>();
        for (final Symbol sym : this.table) {
            if (!sym.getType()) {
                list.add(sym);
            }
        }

        return list;
    }

    /**
     * For obtaining all the Symbols.
     *
     * @return An ArrayList of Symbol objects.
     */
    public ArrayList<Symbol> getAllSymbols() {
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
     * @param originalAddress    Address associated with 0 or more symtab entries.
     * @param replacementAddress Any entry that has originalAddress will have its
     *                           address updated to this value. Does nothing if none
     *                           do.
     */
    public void fixSymbolTableAddress(final int originalAddress, final int replacementAddress) {
        Symbol label = this.getSymbolGivenAddress(Integer.toString(originalAddress));
        while (label != null) {
            label.setAddress(replacementAddress);
            label = this.getSymbolGivenAddress(Integer.toString(originalAddress));
        }
    }

    /**
     * Fetches the text segment label (symbol) which, if declared global, indicates
     * the starting address for execution.
     *
     * @return String containing global label whose text segment address is starting
     * address for program execution.
     */
    public static String getStartLabel() {
        return SymbolTable.startLabel;
    }
}
