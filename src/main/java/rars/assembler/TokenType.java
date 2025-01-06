package rars.assembler;

import org.jetbrains.annotations.NotNull;
import rars.riscv.InstructionsRegistry;
import rars.riscv.hardware.ControlAndStatusRegisterFile;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;
import rars.util.BinaryUtils;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * Constants to identify the types of tokens found in RISCV programs.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public enum TokenType {
    COMMENT,
    DIRECTIVE,
    OPERATOR,
    /**
     * note: REGISTER_NAME is token of form zero whereas REGISTER_NUMBER is token
     * of form x0. The former is part of extended assembler, and latter is part
     * of basic assembler.
     **/
    // TODO: merge REGISTER_NUMBER and REGISTER_NAME
    REGISTER_NAME,
    REGISTER_NUMBER,
    FP_REGISTER_NAME,
    CSR_NAME,
    ROUNDING_MODE,
    IDENTIFIER,
    LEFT_PAREN,
    RIGHT_PAREN,
    INTEGER_5,
    INTEGER_6,
    INTEGER_12,
    INTEGER_12U,
    INTEGER_20,
    INTEGER_32,
    INTEGER_64,
    REAL_NUMBER,
    QUOTED_STRING,
    PLUS,
    MINUS,
    COLON, // TODO: consider removing it in favour of a combined LABEL element
    ERROR,
    MACRO_PARAMETER, // TODO: make it do something
    HI,// TODO: make it do something
    LO, // TODO: make it do something
    TAG; // TODO: remove?

    /**
     * Classifies the given string into one of the types.
     *
     * @param value
     *     String containing candidate language element, extracted from
     *     MIPS program.
     * @return Returns the corresponding TokenTypes object if the parameter matches
     * a
     * defined MIPS token type, else returns <code>null</code>.
     */
    public static @NotNull TokenType matchTokenType(@NotNull final String value) {
        // If it starts with single quote ('), it is a mal-formed character literal
        // because a well-formed character literal was converted to string-ified
        // integer before getting here...
        if (value.charAt(0) == '\'') {
            return TokenType.ERROR;
        }

        // See if it is a comment
        if (value.charAt(0) == '#') {
            return TokenType.COMMENT;
        }

        if (value.charAt(0) == '@') {
            return TokenType.TAG;
        }

        // See if it is one of the simple tokens
        if (value.length() == 1) {
            switch (value.charAt(0)) {
                case '(':
                    return TokenType.LEFT_PAREN;
                case ')':
                    return TokenType.RIGHT_PAREN;
                case ':':
                    return TokenType.COLON;
                case '+':
                    return TokenType.PLUS;
                case '-':
                    return TokenType.MINUS;
            }
        }

        switch (value) {
            case "%hi" -> {
                return TokenType.HI;
            }
            case "%lo" -> {
                return TokenType.LO;
            }
            case "rne", "rtz", "rdn", "rup", "rmm", "dyn" -> {
                return TokenType.ROUNDING_MODE;
            }
        }

        // See if it is a macro parameter
        if (Macro.tokenIsMacroParameter(value, false)) {
            return TokenType.MACRO_PARAMETER;
        }

        // See if it is a register
        if (RegisterFile.INSTANCE.getRegisterByName(value) != null) {
            if (value.startsWith("x")) {
                return TokenType.REGISTER_NUMBER;
            } else {
                return TokenType.REGISTER_NAME;
            }
        }
        // See if it is a floating point register

        if (FloatingPointRegisterFile.getRegister(value) != null) {
            return TokenType.FP_REGISTER_NAME;
        }

        if (ControlAndStatusRegisterFile.getRegister(value) != null) {
            return TokenType.CSR_NAME;
        }
        // See if it is an immediate (constant) integer second
        // Classify based on # bits needed to represent in binary
        // This is needed because most immediate operands limited to 16 bits
        // others limited to 5 bits unsigned (shift amounts) others 32 bits.

        try {

            final int i = BinaryUtils.stringToInt(value); // KENV 1/6/05

            // Comments from 2008 and 2005 were removed - Benjamin Landers 2019

            // shift operands must be in range 0-31
            if (i >= 0 && i <= 31) {
                return TokenType.INTEGER_5;
            }
            if (i >= 0 && i <= 64) {
                return TokenType.INTEGER_6;
            }
            if (i >= DataTypes.MIN_IMMEDIATE_VALUE && i <= DataTypes.MAX_IMMEDIATE_VALUE) {
                return TokenType.INTEGER_12;
            }
            if (i >= 0 && i <= 0xFFF) {
                return TokenType.INTEGER_12U;
            }
            if (i >= DataTypes.MIN_UPPER_VALUE && i <= DataTypes.MAX_UPPER_VALUE) {
                return TokenType.INTEGER_20;
            }
            return TokenType.INTEGER_32; // default when no other type is applicable
        } catch (final NumberFormatException e) {
            // NO ACTION -- exception suppressed
        }

        try {
            BinaryUtils.stringToLong(value);
            return TokenType.INTEGER_64;
        } catch (final NumberFormatException e) {
            // NO ACTION -- exception suppressed
        }

        // See if it is a real (fixed or floating point) number. Note that parseDouble()
        // accepts integer values but if it were an integer literal we wouldn't get this
        // far.
        if (value.equals("Inf") || value.equals("NaN")) {
            return TokenType.REAL_NUMBER;
        }
        if (('0' <= value.charAt(0) && value.charAt(0) <= '9') || value.charAt(0) == '.' || value.charAt(0) == '-') {
            try {
                Double.parseDouble(value);
                return TokenType.REAL_NUMBER;
            } catch (final NumberFormatException e) {
                // NO ACTION -- exception suppressed
            }
        }

        // See if it is a directive
        if (value.charAt(0) == '.' && Directive.matchDirective(value) != null) {
            return TokenType.DIRECTIVE;
        }

        // See if it is a quoted string
        if (value.charAt(0) == '"') {
            return TokenType.QUOTED_STRING;
        }

        // See if it is an instruction operator
        if (!InstructionsRegistry.matchOperator(value).isEmpty()) {
            return TokenType.OPERATOR;
        }

        // Test for identifier goes last because I have defined tokens for various
        // MIPS constructs (such as operators and directives) that also could fit
        // the lexical specifications of an identifier, and those need to be
        // recognized first.
        if (TokenType.isValidIdentifier(value)) {
            return TokenType.IDENTIFIER;
        }

        // Matches no language token.
        return TokenType.ERROR;
    }

    /**
     * Lets you know if given tokentype is for integers (INTGER_5, INTEGER_16,
     * INTEGER_32).
     *
     * @param type
     *     the TokenType of interest
     * @return true if type is an integer type, false otherwise.
     */
    public static boolean isIntegerTokenType(final TokenType type) {
        return type == TokenType.INTEGER_5 || type == TokenType.INTEGER_6 || type == TokenType.INTEGER_12 ||
            type == TokenType.INTEGER_12U || type == TokenType.INTEGER_20 || type == TokenType.INTEGER_32 ||
            type == TokenType.INTEGER_64;
    }

    /**
     * Lets you know if given tokentype is for floating point numbers (REAL_NUMBER).
     *
     * @param type
     *     the TokenType of interest
     * @return true if type is an floating point type, false otherwise.
     */
    public static boolean isFloatingTokenType(final TokenType type) {
        return type == TokenType.REAL_NUMBER;
    }

    /**
     * <p>isValidIdentifier.</p>
     *
     * @param value
     *     a {@link java.lang.String} object
     * @return a boolean
     */
    public static boolean isValidIdentifier(final String value) {
        boolean result = (
            Character.isLetter(value.charAt(0)) || value.charAt(0) == '_' || value.charAt(0) == '.'
                || value.charAt(0) == '$'
        );
        int index = 1;
        while (result && index < value.length()) {
            if (!(
                Character.isLetterOrDigit(value.charAt(index)) || value.charAt(index) == '_'
                    || value.charAt(index) == '.' || value.charAt(index) == '$'
            )) {
                result = false;
            }
            index++;
        }
        return result;
    }

    // TODO: is $ still relevant?
    // COD2, A-51: "Identifiers are a sequence of alphanumeric characters,
    // underbars (_), and dots (.) that do not begin with a number."
    // Ideally this would be in a separate Identifier class but I did not see an
    // immediate
    // need beyond this method (refactoring effort would probably identify other
    // uses
    // related to symbol table).
    // DPS 14-Jul-2008: added '$' as valid symbol. Permits labels to include $.
    // MIPS-target GCC will produce labels that start with $.

    /**
     * Produces String equivalent of this token type, which is its name.
     *
     * @return String containing descriptive name for token type.
     */
    @Override
    public String toString() {
        return this.name(); // Get the literal string from enum
    }

}
