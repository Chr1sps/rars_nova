package rars.assembler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/*
Copyright (c) 2003-2012,  Pete Sanderson and Kenneth Vollmar

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
 * Enum representing RISCV assembler directives based on the original class by
 * Pete Sanderson. The directive name is indicative of the directive it
 * represents. For example, <code>DATA</code> represents the RISCV .data
 * directive.
 *
 * @author Chr1sps
 */
public enum Directive {

    DATA(
        ".data",
        "Subsequent items stored in Data segment at next available address"
    ),
    TEXT(
        ".text",
        "Subsequent items (instructions) stored in Text segment at next available address"
    ),
    WORD(
        ".word",
        "Store the listed second(s) as 32 bit words on word boundary"
    ),
    DWORD(
        ".dword",
        "Store the listed second(s) as 64 bit double-word on word boundary"
    ),
    ASCII(
        ".ascii",
        "Store the string in the Data segment but do not add null terminator"
    ),
    ASCIZ(
        ".asciz",
        "Store the string in the Data segment and add null terminator"
    ),
    STRING(".string", "Alias for .asciz"),
    BYTE(".byte", "Store the listed second(s) as 8 bit bytes"),
    ALIGN(
        ".align",
        "Align next data item on specified byte boundary (0=byte, 1=half, 2=word, 3=double)"
    ),
    HALF(
        ".half",
        "Store the listed second(s) as 16 bit halfwords on halfword boundary"
    ),
    SPACE(
        ".space",
        "Reserve the next specified number of bytes in Data segment"
    ),
    DOUBLE(
        ".double",
        "Store the listed second(s) as double precision floating point"
    ),
    FLOAT(
        ".float",
        "Store the listed second(s) as single precision floating point"
    ),
    EXTERN(
        ".extern",
        "Declare the listed label and byte length to be a global data field"
    ),
    GLOBL(
        ".globl",
        "Declare the listed label(s) as global to enable referencing from other files"
    ),
    GLOBAL(
        ".global",
        "Declare the listed label(s) as global to enable referencing from other files"
    ),
    /* EQV added by DPS 11 July 2012 */
    EQV(
        ".eqv",
        "Substitute second operand for first. First operand is symbol, second operand is expression (like #define)"
    ),
    /* MACRO and END_MACRO added by Mohammad Sekhavat Oct 2012 */
    MACRO(".macro", "Begin macro definition.  See .end_macro"),
    END_MACRO(".end_macro", "End macro definition.  See .macro"),
    /* INCLUDE added by DPS 11 Jan 2013 */
    INCLUDE(
        ".include",
        "Insert the contents of the specified file.  Put file in quotes."
    ),
    SECTION(
        ".section",
        "Allows specifying sections without .text or .data directives. Included for gcc comparability"
    );

    private final @NotNull String name;
    private final @NotNull String description; // help text

    Directive(@NotNull final String name, @NotNull final String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Find Directive object, if any, which matches the given String.
     *
     * @param str
     *     A String containing candidate directive name (e.g. ".ascii")
     * @return If match is found, returns matching Directives object, else returns
     * <code>null</code>.
     */
    public static @Nullable Directive matchDirective(final @NotNull String str) {
        return Arrays.stream(Directive.values())
            .filter(directive -> directive.name.equalsIgnoreCase(str))
            .findAny()
            .orElse(null);
    }

    /**
     * Find Directive object, if any, which contains the given string as a prefix.
     * For example,
     * ".a" will match ".ascii", ".asciiz" and ".align"
     *
     * @param str
     *     A String
     * @return If match is found, returns ArrayList of matching Directives objects,
     * else returns <code>null</code>.
     */
    public static @NotNull List<@NotNull Directive> prefixMatchDirectives(final @NotNull String str) {
        return Arrays.stream(Directive.values())
            .filter(directive ->
                directive
                    .name
                    .toLowerCase()
                    .startsWith(str.toLowerCase())
            ).toList();
    }

    /**
     * Produces String-ified version of Directive object
     *
     * @return String representing Directive: its MIPS name
     */
    @Override
    public @NotNull String toString() {
        return this.name;
    }

    /**
     * Get name of this Directives object
     *
     * @return name of this directive as a String
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * Get description of this Directives object
     *
     * @return description of this directive (for help purposes)
     */
    public @NotNull String getDescription() {
        return this.description;
    }

    /**
     * Lets you know whether the directive is for integer (WORD,HALF,BYTE).
     *
     * @return true if the directive is WORD, HALF, or BYTE, false otherwise
     */
    public boolean isIntegerDirective() {
        return switch (this) {
            case DWORD, WORD, HALF, BYTE -> true;
            default -> false;
        };
    }

    /**
     * Lets you know whether the directive is for floating number (FLOAT,DOUBLE).
     *
     * @return true if the directive is FLOAT or DOUBLE, false otherwise.
     */
    public boolean isFloatingDirective() {
        return switch (this) {
            case FLOAT, DOUBLE -> true;
            default -> false;
        };
    }

}