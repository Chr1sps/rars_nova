package rars.assembler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.ErrorList;
import rars.ErrorMessage;
import rars.riscv.Instruction;
import rars.riscv.Instructions;

import java.util.List;

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
 * Provides utility method related to operand formats.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public final class OperandFormat {
    private OperandFormat() {
    }

    /**
     * Syntax test for correct match in both numbers and types of operands.
     *
     * @param candidateList List of tokens generated from programmer's MIPS
     *                      statement.
     * @param inst          The (presumably best matched) RISCV instruction.
     * @param errors        ErrorList into which any error messages generated here
     *                      will be added.
     * @return Returns <tt>true</tt> if the programmer's statement matches the MIPS
     * specification, else returns <tt>false</tt>.
     */

    static boolean checkIfTokensMatchOperand(final TokenList candidateList, final Instruction inst, final ErrorList errors) {
        return OperandFormat.numOperandsCheck(candidateList, inst, errors) && OperandFormat.operandTypeCheck(candidateList, inst, errors);
    }

    /**
     * If candidate operator token matches more than one instruction mnemonic, then
     * select
     * first such Instruction that has an exact operand match. If none match,
     * return the first Instruction and let client deal with operand mismatches.
     */
    static @Nullable Instruction bestOperandMatch(final TokenList tokenList, final @NotNull List<Instruction> instrMatches) {
        if (instrMatches.size() == 1)
            return instrMatches.getFirst();
        for (final Instruction potentialMatch : instrMatches) {
            if (OperandFormat.checkIfTokensMatchOperand(tokenList, potentialMatch, new ErrorList()))
                return potentialMatch;
        }
        return instrMatches.getFirst();
    }

    // Simply check to see if numbers of operands are correct and generate error
    // message if not.
    private static boolean numOperandsCheck(final @NotNull TokenList cand, final @NotNull Instruction spec, final @NotNull ErrorList errors) {
        final int numOperands = cand.size() - 1;
        final int reqNumOperands = Instructions.getTokenList(spec).size() - 1;
        final Token operator = cand.get(0);
        if (numOperands == reqNumOperands) {
            return true;
        } else if (numOperands < reqNumOperands) {

            final String mess = "Too few or incorrectly formatted operands. Expected: " + spec.getExampleFormat();
            OperandFormat.generateMessage(operator, mess, errors);
        } else {
            final String mess = "Too many or incorrectly formatted operands. Expected: " + spec.getExampleFormat();
            OperandFormat.generateMessage(operator, mess, errors);
        }
        return false;
    }

    // Generate error message if operand is not of correct type for this operation &
    // operand position
    private static boolean operandTypeCheck(final TokenList cand, final Instruction spec, final ErrorList errors) {
        final var tokenList = Instructions.getTokenList(spec);

        Token candToken, specToken;
        TokenType candType, specType;
        for (int i = 1; i < tokenList.size(); i++) {
            candToken = cand.get(i);
            specToken = tokenList.get(i);
            candType = candToken.getType();
            specType = specToken.getType();
            // Type mismatch is error EXCEPT when (1) spec calls for register name and
            // candidate is
            // register number, (2) spec calls for register number, candidate is register
            // name and
            // names are permitted, (3)spec calls for integer of specified max bit length
            // and
            // candidate is integer of smaller bit length.
            // Type match is error when spec calls for register name, candidate is register
            // name, and
            // names are not permitted.

            // added 2-July-2010 DPS
            // Not an error if spec calls for identifier and candidate is operator, since
            // operator names can be used as labels.
            // TODO: maybe add more cases in here
            if (specType == TokenType.IDENTIFIER && candType == TokenType.OPERATOR) {
                final Token replacement = new Token(TokenType.IDENTIFIER, candToken.getValue(), candToken.getSourceProgram(),
                        candToken.getSourceLine(), candToken.getStartPos());
                cand.set(i, replacement);
                continue;
            }
            // end 2-July-2010 addition

            if ((specType == TokenType.REGISTER_NAME || specType == TokenType.REGISTER_NUMBER) &&
                    candType == TokenType.REGISTER_NAME) {
                continue;
            }
            if (specType == TokenType.REGISTER_NAME &&
                    candType == TokenType.REGISTER_NUMBER)
                continue;
            if (specType == TokenType.CSR_NAME &&
                    (candType == TokenType.INTEGER_5 || candType == TokenType.INTEGER_6
                            || candType == TokenType.INTEGER_12
                            || candType == TokenType.INTEGER_12U || candType == TokenType.CSR_NAME))
                continue;
            if ((specType == TokenType.INTEGER_6 && candType == TokenType.INTEGER_5) ||
                    (specType == TokenType.INTEGER_12 && candType == TokenType.INTEGER_5) ||
                    (specType == TokenType.INTEGER_20 && candType == TokenType.INTEGER_5) ||
                    (specType == TokenType.INTEGER_12 && candType == TokenType.INTEGER_6) ||
                    (specType == TokenType.INTEGER_20 && candType == TokenType.INTEGER_6) ||
                    (specType == TokenType.INTEGER_20 && candType == TokenType.INTEGER_12) ||
                    (specType == TokenType.INTEGER_20 && candType == TokenType.INTEGER_12U) ||
                    (specType == TokenType.INTEGER_32 && candType == TokenType.INTEGER_5) ||
                    (specType == TokenType.INTEGER_32 && candType == TokenType.INTEGER_6) ||
                    (specType == TokenType.INTEGER_32 && candType == TokenType.INTEGER_12) ||
                    (specType == TokenType.INTEGER_32 && candType == TokenType.INTEGER_12U) ||
                    (specType == TokenType.INTEGER_32 && candType == TokenType.INTEGER_20))
                continue;
            if (specType == TokenType.INTEGER_12 && candType == TokenType.INTEGER_12U) {
                OperandFormat.generateMessage(candToken, "Unsigned second is too large to fit into a sign-extended immediate", errors);
                return false;
            }
            if ((specType == TokenType.INTEGER_5 && candType == TokenType.INTEGER_6) ||
                    (specType == TokenType.INTEGER_5 && candType == TokenType.INTEGER_12) ||
                    (specType == TokenType.INTEGER_5 && candType == TokenType.INTEGER_12U) ||
                    (specType == TokenType.INTEGER_5 && candType == TokenType.INTEGER_20) ||
                    (specType == TokenType.INTEGER_5 && candType == TokenType.INTEGER_32) ||
                    (specType == TokenType.INTEGER_6 && candType == TokenType.INTEGER_12) ||
                    (specType == TokenType.INTEGER_6 && candType == TokenType.INTEGER_12U) ||
                    (specType == TokenType.INTEGER_6 && candType == TokenType.INTEGER_20) ||
                    (specType == TokenType.INTEGER_6 && candType == TokenType.INTEGER_32) ||
                    (specType == TokenType.INTEGER_12 && candType == TokenType.INTEGER_20) ||
                    (specType == TokenType.INTEGER_12 && candType == TokenType.INTEGER_32) ||
                    (specType == TokenType.INTEGER_20 && candType == TokenType.INTEGER_32)) {
                OperandFormat.generateMessage(candToken, "operand is out of range", errors);
                return false;
            }
            if (candType != specType) {
                OperandFormat.generateMessage(candToken, "operand is of incorrect type", errors);
                return false;
            }
        }

        return true;
    }

    // Handy utility for all parse errors...
    private static void generateMessage(final Token token, final String mess, final ErrorList errors) {
        errors.add(new ErrorMessage(token.getSourceProgram(), token.getSourceLine(), token.getStartPos(),
                "\"" + token.getValue() + "\": " + mess));
    }

}
