package io.github.chr1sps.rars.assembler;

import java.util.ArrayList;
import java.util.EnumSet;

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

    /**
     * Constant <code>DATA</code>
     */
    DATA(".data",
            "Subsequent items stored in Data segment at next available address"),
    /**
     * Constant <code>TEXT</code>
     */
    TEXT(".text",
            "Subsequent items (instructions) stored in Text segment at next available address"),
    /**
     * Constant <code>WORD</code>
     */
    WORD(".word",
            "Store the listed value(s) as 32 bit words on word boundary"),
    /**
     * Constant <code>DWORD</code>
     */
    DWORD(".dword",
            "Store the listed value(s) as 64 bit double-word on word boundary"),
    /**
     * Constant <code>ASCII</code>
     */
    ASCII(".ascii",
            "Store the string in the Data segment but do not add null terminator"),
    /**
     * Constant <code>ASCIZ</code>
     */
    ASCIZ(".asciz",
            "Store the string in the Data segment and add null terminator"),
    /**
     * Constant <code>STRING</code>
     */
    STRING(".string", "Alias for .asciz"),
    /**
     * Constant <code>BYTE</code>
     */
    BYTE(".byte", "Store the listed value(s) as 8 bit bytes"),
    /**
     * Constant <code>ALIGN</code>
     */
    ALIGN(".align",
            "Align next data item on specified byte boundary (0=byte, 1=half, 2=word, 3=double)"),
    /**
     * Constant <code>HALF</code>
     */
    HALF(".half",
            "Store the listed value(s) as 16 bit halfwords on halfword boundary"),
    /**
     * Constant <code>SPACE</code>
     */
    SPACE(".space",
            "Reserve the next specified number of bytes in Data segment"),
    /**
     * Constant <code>DOUBLE</code>
     */
    DOUBLE(".double",
            "Store the listed value(s) as double precision floating point"),
    /**
     * Constant <code>FLOAT</code>
     */
    FLOAT(".float",
            "Store the listed value(s) as single precision floating point"),
    /**
     * Constant <code>EXTERN</code>
     */
    EXTERN(".extern",
            "Declare the listed label and byte length to be a global data field"),
    /**
     * Constant <code>GLOBL</code>
     */
    GLOBL(".globl",
            "Declare the listed label(s) as global to enable referencing from other files"),
    /**
     * Constant <code>GLOBAL</code>
     */
    GLOBAL(".global",
            "Declare the listed label(s) as global to enable referencing from other files"),
    /* EQV added by DPS 11 July 2012 */
    /**
     * Constant <code>EQV</code>
     */
    EQV(".eqv",
            "Substitute second operand for first. First operand is symbol, second operand is expression (like #define)"),
    /* MACRO and END_MACRO added by Mohammad Sekhavat Oct 2012 */
    /**
     * Constant <code>MACRO</code>
     */
    MACRO(".macro", "Begin macro definition.  See .end_macro"),
    /**
     * Constant <code>END_MACRO</code>
     */
    END_MACRO(".end_macro", "End macro definition.  See .macro"),
    /* INCLUDE added by DPS 11 Jan 2013 */
    /**
     * Constant <code>INCLUDE</code>
     */
    INCLUDE(".include",
            "Insert the contents of the specified file.  Put filename in quotes."),
    /**
     * Constant <code>SECTION</code>
     */
    SECTION(".section",
            "Allows specifying sections without .text or .data directives. Included for gcc comparability");

    private final String name;
    private final String description; // help text

    private static final ArrayList<Directive> directiveList = new ArrayList<>();

    Directive(String name, String description) {
        this.name = name;
        this.description = description;
    }

    static {
        directiveList.addAll(EnumSet.allOf(Directive.class));
    }

    /**
     * Find Directive object, if any, which matches the given String.
     *
     * @param str A String containing candidate directive name (e.g. ".ascii")
     * @return If match is found, returns matching Directives object, else returns
     * <code>null</code>.
     */
    public static Directive matchDirective(String str) {
        for (Directive match : directiveList) {
            if (str.equalsIgnoreCase(match.name)) {
                return match;
            }
        }
        return null;
    }

    /**
     * Find Directive object, if any, which contains the given string as a prefix.
     * For example,
     * ".a" will match ".ascii", ".asciiz" and ".align"
     *
     * @param str A String
     * @return If match is found, returns ArrayList of matching Directives objects,
     * else returns <code>null</code>.
     */
    public static ArrayList<Directive> prefixMatchDirectives(String str) {
        ArrayList<Directive> matches = null;
        for (Directive direct : directiveList) {
            if (direct.name.toLowerCase().startsWith(str.toLowerCase())) {
                if (matches == null) {
                    matches = new ArrayList<>();
                }
                matches.add(direct);
            }
        }
        return matches;
    }

    /**
     * Produces String-ified version of Directive object
     *
     * @return String representing Directive: its MIPS name
     */
    public String toString() {
        return this.name;
    }

    /**
     * Get name of this Directives object
     *
     * @return name of this directive as a String
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get description of this Directives object
     *
     * @return description of this directive (for help purposes)
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Produces List of Directive objects
     *
     * @return All directives defined
     */
    public static ArrayList<Directive> getDirectiveList() {
        return directiveList;
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