package rars.assembler

import java.util.*


/**
 * Enum representing RISCV assembler directives based on the original class by
 * Pete Sanderson. The directive name is indicative of the directive it
 * represents. For example, `DATA` represents the RISCV .data
 * directive.
 */
enum class Directive(
    /**
     * Name of this Directives object
     */
    @JvmField val directiveName: String,
    /**
     * Get description of this Directives object
     *
     * @return description of this directive (for help purposes)
     */
    @JvmField val description: String
) {
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
        "Store the listed value(s) as 32 bit words on word boundary"
    ),
    DWORD(
        ".dword",
        "Store the listed value(s) as 64 bit double-word on word boundary"
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
    BYTE(".byte", "Store the listed value(s) as 8 bit bytes"),
    ALIGN(
        ".align",
        "Align next data item on specified byte boundary (0=byte, 1=half, 2=word, 3=double)"
    ),
    HALF(
        ".half",
        "Store the listed value(s) as 16 bit halfwords on halfword boundary"
    ),
    SPACE(
        ".space",
        "Reserve the next specified number of bytes in Data segment"
    ),
    DOUBLE(
        ".double",
        "Store the listed value(s) as double precision floating point"
    ),
    FLOAT(
        ".float",
        "Store the listed value(s) as single precision floating point"
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
    EQV(
        ".eqv",
        "Substitute second operand for first. First operand is symbol, second operand is expression (like #define)"
    ),
    MACRO(".macro", "Begin macro definition.  See .end_macro"),
    END_MACRO(".end_macro", "End macro definition.  See .macro"),
    INCLUDE(
        ".include",
        "Insert the contents of the specified file. Put file in quotes."
    ),
    SECTION(
        ".section",
        "Allows specifying sections without .text or .data directives. Included for gcc comparability"
    );

    /**
     * Produces String-ified version of Directive object
     *
     * @return String representing Directive: its MIPS name
     */
    override fun toString(): String = this.directiveName

    val isIntegerDirective: Boolean
        /**
         * Lets you know whether the directive is for integer (WORD,HALF,BYTE).
         *
         * @return true if the directive is WORD, HALF, or BYTE, false otherwise
         */
        get() = when (this) {
            DWORD, WORD, HALF, BYTE -> true
            else -> false
        }

    val isFloatingDirective: Boolean
        /**
         * Lets you know whether the directive is for floating number (FLOAT,DOUBLE).
         *
         * @return true if the directive is FLOAT or DOUBLE, false otherwise.
         */
        get() = when (this) {
            FLOAT, DOUBLE -> true
            else -> false
        }

    companion object {
        /**
         * Find Directive object, if any, which matches the given String.
         *
         * @param str
         * A String containing candidate directive name (e.g. ".ascii")
         * @return If match is found, returns matching Directives object, else returns
         * `null`.
         */
        @JvmStatic
        fun matchDirective(str: String): Directive? = entries.find {
            it.directiveName.equals(str, ignoreCase = true)
        }

        /**
         * Find Directive object, if any, which contains the given string as a prefix.
         * For example,
         * ".a" will match ".ascii", ".asciiz" and ".align"
         *
         * @param str
         * A String
         * @return If match is found, returns ArrayList of matching Directives objects,
         * else returns `null`.
         */
        fun prefixMatchDirectives(str: String): List<Directive> = entries.filter {
            it.directiveName
                .lowercase(Locale.getDefault())
                .startsWith(str.lowercase(Locale.getDefault()))
        }
    }
}