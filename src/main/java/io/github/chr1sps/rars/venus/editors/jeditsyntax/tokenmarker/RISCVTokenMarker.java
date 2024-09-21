/*
 * MIPSTokenMarker.java - MIPS Assembly token marker
 * Copyright (C) 1998, 1999 Slava Pestov, 2010 Pete Sanderson
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package io.github.chr1sps.rars.venus.editors.jeditsyntax.tokenmarker;

import io.github.chr1sps.rars.Settings;
import io.github.chr1sps.rars.assembler.Directive;
import io.github.chr1sps.rars.assembler.TokenType;
import io.github.chr1sps.rars.riscv.BasicInstruction;
import io.github.chr1sps.rars.riscv.Instruction;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.Register;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;
import io.github.chr1sps.rars.venus.editors.jeditsyntax.KeywordMap;
import io.github.chr1sps.rars.venus.editors.jeditsyntax.PopupHelpItem;

import javax.swing.text.Segment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import static io.github.chr1sps.rars.Globals.*;

/**
 * RISCV token marker.
 *
 * @author Pete Sanderson (2010) and Slava Pestov (1999)
 */
public class RISCVTokenMarker extends TokenMarker {
    // private members
    private static KeywordMap cKeywords;
    private static String[] tokenLabels, tokenExamples;
    private final KeywordMap keywords;
    private int lastOffset;
    private int lastKeyword;

    /**
     * <p>Constructor for RISCVTokenMarker.</p>
     */
    public RISCVTokenMarker() {
        this(RISCVTokenMarker.getKeywords());
    }

    /**
     * <p>Constructor for RISCVTokenMarker.</p>
     *
     * @param keywords a {@link io.github.chr1sps.rars.venus.editors.jeditsyntax.KeywordMap} object
     */
    public RISCVTokenMarker(final KeywordMap keywords) {
        this.keywords = keywords;
    }

    /**
     * <p>getRISCVTokenLabels.</p>
     *
     * @return an array of {@link java.lang.String} objects
     */
    public static String[] getRISCVTokenLabels() {
        if (RISCVTokenMarker.tokenLabels == null) {
            RISCVTokenMarker.tokenLabels = new String[Token.ID_COUNT];
            RISCVTokenMarker.tokenLabels[Token.COMMENT1] = "Comment";
            RISCVTokenMarker.tokenLabels[Token.LITERAL1] = "String literal";
            RISCVTokenMarker.tokenLabels[Token.LITERAL2] = "Character literal";
            RISCVTokenMarker.tokenLabels[Token.LABEL] = "Label";
            RISCVTokenMarker.tokenLabels[Token.KEYWORD1] = "Instruction";
            RISCVTokenMarker.tokenLabels[Token.KEYWORD2] = "Assembler directive";
            RISCVTokenMarker.tokenLabels[Token.KEYWORD3] = "Register";
            RISCVTokenMarker.tokenLabels[Token.INVALID] = "In-progress, invalid";
            RISCVTokenMarker.tokenLabels[Token.MACRO_ARG] = "Macro parameter";
        }
        return RISCVTokenMarker.tokenLabels;
    }

    /**
     * <p>getRISCVTokenExamples.</p>
     *
     * @return an array of {@link java.lang.String} objects
     */
    public static String[] getRISCVTokenExamples() {
        if (RISCVTokenMarker.tokenExamples == null) {
            RISCVTokenMarker.tokenExamples = new String[Token.ID_COUNT];
            RISCVTokenMarker.tokenExamples[Token.COMMENT1] = "# Load";
            RISCVTokenMarker.tokenExamples[Token.LITERAL1] = "\"First\"";
            RISCVTokenMarker.tokenExamples[Token.LITERAL2] = "'\\n'";
            RISCVTokenMarker.tokenExamples[Token.LABEL] = "main:";
            RISCVTokenMarker.tokenExamples[Token.KEYWORD1] = "lui";
            RISCVTokenMarker.tokenExamples[Token.KEYWORD2] = ".text";
            RISCVTokenMarker.tokenExamples[Token.KEYWORD3] = "zero";
            RISCVTokenMarker.tokenExamples[Token.INVALID] = "\"Regi";
            RISCVTokenMarker.tokenExamples[Token.MACRO_ARG] = "%arg";
        }
        return RISCVTokenMarker.tokenExamples;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Return ArrayList of PopupHelpItem for match of directives. If second argument
    // true, will do exact match. If false, will do prefix match. Returns null
    // if no matches.
    private static ArrayList<PopupHelpItem> getTextFromDirectiveMatch(final String tokenText, final boolean exact) {
        ArrayList<PopupHelpItem> matches = null;
        ArrayList<Directive> directiveMatches = null;
        if (exact) {
            final Directive dir = Directive.matchDirective(tokenText);
            if (dir != null) {
                directiveMatches = new ArrayList<>();
                directiveMatches.add(dir);
            }
        } else {
            directiveMatches = Directive.prefixMatchDirectives(tokenText);
        }
        if (directiveMatches != null) {
            matches = new ArrayList<>();
            for (final Directive direct : directiveMatches) {
                matches.add(new PopupHelpItem(tokenText, direct.getName(), direct.getDescription(), exact));
            }
        }
        return matches;
    }

    // Return text for match of instruction mnemonic. If second argument true, will
    // do exact match. If false, will do prefix match. Text is returned as ArrayList
    // of PopupHelpItem objects. If no matches, returns null.
    private static ArrayList<PopupHelpItem> getTextFromInstructionMatch(final String tokenText, final boolean exact) {
        final ArrayList<Instruction> matches;
        final ArrayList<PopupHelpItem> results = new ArrayList<>();
        if (exact) {
            matches = instructionSet.matchOperator(tokenText);
        } else {
            matches = instructionSet.prefixMatchOperator(tokenText);
        }
        if (matches == null) {
            return null;
        }
        int realMatches = 0;
        final HashMap<String, String> insts = new HashMap<>();
        final TreeSet<String> mnemonics = new TreeSet<>();
        for (final Instruction inst : matches) {
            if (getSettings().getBooleanSetting(Settings.Bool.EXTENDED_ASSEMBLER_ENABLED)
                    || inst instanceof BasicInstruction) {
                if (exact) {
                    results.add(new PopupHelpItem(tokenText, inst.getExampleFormat(), inst.getDescription(), true));
                } else {
                    final String mnemonic = inst.getExampleFormat().split(" ")[0];
                    if (!insts.containsKey(mnemonic)) {
                        mnemonics.add(mnemonic);
                        insts.put(mnemonic, inst.getDescription());
                    }
                }
                realMatches++;
            }
        }
        if (realMatches == 0) {
            if (exact) {
                results.add(new PopupHelpItem(tokenText, tokenText, "(not a basic instruction)", true));
            } else {
                return null;
            }
        } else {
            if (!exact) {
                for (final String mnemonic : mnemonics) {
                    final String info = insts.get(mnemonic);
                    results.add(new PopupHelpItem(tokenText, mnemonic, info, false));
                }
            }
        }
        return results;
    }

    /**
     * Get KeywordMap containing all MIPS key words. This includes all instruction
     * mnemonics,
     * assembler directives, and register names.
     *
     * @return KeywordMap where key is the keyword and associated value is the token
     * type (e.g. Token.KEYWORD1).
     */
    private static KeywordMap getKeywords() {
        if (RISCVTokenMarker.cKeywords == null) {
            RISCVTokenMarker.cKeywords = new KeywordMap(false);
            // add Instruction mnemonics
            for (final Instruction inst : instructionSet.getInstructionList()) {
                RISCVTokenMarker.cKeywords.add(inst.getName(), Token.KEYWORD1);
            }
            // add assembler directives
            for (final Directive direct : Directive.getDirectiveList()) {
                RISCVTokenMarker.cKeywords.add(direct.getName(), Token.KEYWORD2);
            }
            // add integer register file
            for (final Register r : RegisterFile.getRegisters()) {
                RISCVTokenMarker.cKeywords.add(r.getName(), Token.KEYWORD3);
                RISCVTokenMarker.cKeywords.add("x" + r.getNumber(), Token.KEYWORD3); // also recognize x0, x1, x2, etc
            }

            RISCVTokenMarker.cKeywords.add("fp", Token.KEYWORD3);

            // add floating point register file
            for (final Register r : FloatingPointRegisterFile.getRegisters()) {
                RISCVTokenMarker.cKeywords.add(r.getName(), Token.KEYWORD3);
                RISCVTokenMarker.cKeywords.add("f" + r.getNumber(), Token.KEYWORD3);
            }
        }
        return RISCVTokenMarker.cKeywords;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte markTokensImpl(byte token, final Segment line, final int lineIndex) {
        final char[] array = line.array;
        final int offset = line.offset;
        this.lastOffset = offset;
        this.lastKeyword = offset;
        final int length = line.count + offset;
        boolean backslash = false;

        loop:
        for (int i = offset; i < length; i++) {
            final int i1 = (i + 1);

            final char c = array[i];
            if (c == '\\') {
                backslash = !backslash;
                continue;
            }

            switch (token) {
                case Token.NULL:
                    switch (c) {
                        case '"':
                            this.doKeyword(line, i, c);
                            if (backslash)
                                backslash = false;
                            else {
                                this.addToken(i - this.lastOffset, token);
                                token = Token.LITERAL1;
                                this.lastOffset = this.lastKeyword = i;
                            }
                            break;
                        case '\'':
                            this.doKeyword(line, i, c);
                            if (backslash)
                                backslash = false;
                            else {
                                this.addToken(i - this.lastOffset, token);
                                token = Token.LITERAL2;
                                this.lastOffset = this.lastKeyword = i;
                            }
                            break;
                        case ':':
                            // Replacement code 3 Aug 2010. Will recognize label definitions when:
                            // (1) label is same as instruction name, (2) label begins after column 1,
                            // (3) there are spaces between label name and colon, (4) label is valid
                            // MIPS identifier (otherwise would catch, say, 0 (zero) in .word 0:10)
                            backslash = false;
                            // String lab = new String(array, lastOffset, i1-lastOffset-1).trim();
                            boolean validIdentifier;
                            try {
                                validIdentifier = TokenType
                                        .isValidIdentifier(new String(array, this.lastOffset, i1 - this.lastOffset - 1).trim());
                            } catch (final StringIndexOutOfBoundsException e) {
                                validIdentifier = false;
                            }
                            if (validIdentifier) {
                                this.addToken(i1 - this.lastOffset, Token.LABEL);
                                this.lastOffset = this.lastKeyword = i1;
                            }
                            break;
                        case '#':
                            backslash = false;
                            this.doKeyword(line, i, c);
                            this.addToken(i - this.lastOffset, token);
                            this.addToken(length - i, Token.COMMENT1);
                            this.lastOffset = this.lastKeyword = length;
                            break loop;

                        default:
                            backslash = false;
                            // . and $ added 4/6/10 DPS; % added 12/12 M.Sekhavat
                            if (!Character.isLetterOrDigit(c)
                                    && c != '_' && c != '.' && c != '$' && c != '%')
                                this.doKeyword(line, i, c);
                            break;
                    }
                    break;
                case Token.LITERAL1:
                    if (backslash)
                        backslash = false;
                    else if (c == '"') {
                        this.addToken(i1 - this.lastOffset, token);
                        token = Token.NULL;
                        this.lastOffset = this.lastKeyword = i1;
                    }
                    break;
                case Token.LITERAL2:
                    if (backslash)
                        backslash = false;
                    else if (c == '\'') {
                        this.addToken(i1 - this.lastOffset, Token.LITERAL1);
                        token = Token.NULL;
                        this.lastOffset = this.lastKeyword = i1;
                    }
                    break;
                default:
                    throw new InternalError("Invalid state: "
                            + token);
            }
        }

        if (token == Token.NULL)
            this.doKeyword(line, length, '\0');

        switch (token) {
            case Token.LITERAL1:
            case Token.LITERAL2:
                this.addToken(length - this.lastOffset, Token.INVALID);
                token = Token.NULL;
                break;
            case Token.KEYWORD2:
                this.addToken(length - this.lastOffset, token);
                if (!backslash)
                    token = Token.NULL;
            default:
                this.addToken(length - this.lastOffset, token);
                break;
        }

        return token;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Construct and return any appropriate help information for
     * the given token.
     */
    @Override
    public ArrayList<PopupHelpItem> getTokenExactMatchHelp(final Token token, final String tokenText) {
        ArrayList<PopupHelpItem> matches = null;
        if (token != null && token.id == Token.KEYWORD1) {
            final ArrayList<Instruction> instrMatches = instructionSet.matchOperator(tokenText);
            if (!instrMatches.isEmpty()) {
                int realMatches = 0;
                matches = new ArrayList<>();
                for (final Instruction inst : instrMatches) {
                    if (getSettings().getBooleanSetting(Settings.Bool.EXTENDED_ASSEMBLER_ENABLED)
                            || inst instanceof BasicInstruction) {
                        matches.add(new PopupHelpItem(tokenText, inst.getExampleFormat(), inst.getDescription()));
                        realMatches++;
                    }
                }
                if (realMatches == 0) {
                    matches.add(new PopupHelpItem(tokenText, tokenText, "(is not a basic instruction)"));
                }
            }
        }
        if (token != null && token.id == Token.KEYWORD2) {
            final Directive dir = Directive.matchDirective(tokenText);
            if (dir != null) {
                matches = new ArrayList<>();
                matches.add(new PopupHelpItem(tokenText, dir.getName(), dir.getDescription()));
            }
        }
        return matches;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Construct and return any appropriate help information for
     * prefix match based on current line's token list.
     */
    @Override
    public ArrayList<PopupHelpItem> getTokenPrefixMatchHelp(final String line, final Token tokenList, final Token token,
                                                            final String tokenText) {
        // CASE: Unlikely boundary case...
        if (tokenList == null || tokenList.id == Token.END) {
            return null;
        }

        // CASE: if current token is a comment, turn off the text.
        if (token != null && token.id == Token.COMMENT1) {
            return null;
        }

        // Let's see if the line already contains an instruction or directive. If so, we
        // need its token
        // text as well so we can do the match. Also need to distinguish the case where
        // current
        // token is also an instruction/directive (moreThanOneKeyword variable).

        Token tokens = tokenList;
        String keywordTokenText = null;
        byte keywordType = -1;
        int offset = 0;
        boolean moreThanOneKeyword = false;
        while (tokens.id != Token.END) {
            if (tokens.id == Token.KEYWORD1 || tokens.id == Token.KEYWORD2) {
                if (keywordTokenText != null) {
                    moreThanOneKeyword = true;
                    break;
                }
                keywordTokenText = line.substring(offset, offset + tokens.length);
                keywordType = tokens.id;
            }
            offset += tokens.length;
            tokens = tokens.next;
        }

        // CASE: Current token is valid KEYWORD1 (MIPS instruction). If this line
        // contains a previous KEYWORD1 or KEYWORD2
        // token, then we ignore this one and do exact match on the first one. If it
        // does not, there may be longer
        // instructions for which this is a prefix, so do a prefix match on current
        // token.
        if (token != null && token.id == Token.KEYWORD1) {
            if (moreThanOneKeyword) {
                return (keywordType == Token.KEYWORD1) ? RISCVTokenMarker.getTextFromInstructionMatch(keywordTokenText, true)
                        : RISCVTokenMarker.getTextFromDirectiveMatch(keywordTokenText, true);
            } else {
                return RISCVTokenMarker.getTextFromInstructionMatch(tokenText, false);
            }
        }

        // CASE: Current token is valid KEYWORD2 (MIPS directive). If this line contains
        // a previous KEYWORD1 or KEYWORD2
        // token, then we ignore this one and do exact match on the first one. If it
        // does not, there may be longer
        // directives for which this is a prefix, so do a prefix match on current token.
        if (token != null && token.id == Token.KEYWORD2) {
            if (moreThanOneKeyword) {
                return (keywordType == Token.KEYWORD1) ? RISCVTokenMarker.getTextFromInstructionMatch(keywordTokenText, true)
                        : RISCVTokenMarker.getTextFromDirectiveMatch(keywordTokenText, true);
            } else {
                return RISCVTokenMarker.getTextFromDirectiveMatch(tokenText, false);
            }
        }

        // CASE: line already contains KEYWORD1 or KEYWORD2 and current token is
        // something other
        // than KEYWORD1 or KEYWORD2. Generate text based on exact match of that token.
        if (keywordTokenText != null) {
            if (keywordType == Token.KEYWORD1) {
                return RISCVTokenMarker.getTextFromInstructionMatch(keywordTokenText, true);
            }
            if (keywordType == Token.KEYWORD2) {
                return RISCVTokenMarker.getTextFromDirectiveMatch(keywordTokenText, true);
            }
        }

        // CASE: Current token is NULL, which can be any number of things. Think of it
        // as being either white space
        // or an in-progress token possibly preceded by white space. We'll do a trim on
        // the token. Now there
        // are two subcases to consider:
        // SUBCASE: The line does not contain any KEYWORD1 or KEYWORD2 tokens but
        // nothing remains after trimming the
        // current token's text. This means it consists only of white space and there is
        // nothing more to do
        // but return.
        // SUBCASE: The line does not contain any KEYWORD1 or KEYWORD2 tokens. This
        // means we do a prefix match of
        // of the current token to either instruction or directive names. Easy to
        // distinguish since
        // directives start with "."

        if (token != null && token.id == Token.NULL) {

            final String trimmedTokenText = tokenText.trim();

            if (trimmedTokenText.isEmpty()) {
                // Subcase: no KEYWORD1 or KEYWORD2 but current token contains nothing but white
                // space. We're done.
                return null;
            } else {
                // Subcase: no KEYWORD1 or KEYWORD2. Generate text based on prefix match of
                // trimmed current token.
                if (trimmedTokenText.charAt(0) == '.') {
                    return RISCVTokenMarker.getTextFromDirectiveMatch(trimmedTokenText, false);
                } else if (trimmedTokenText.length() >= getSettings().getEditorPopupPrefixLength()) {
                    return RISCVTokenMarker.getTextFromInstructionMatch(trimmedTokenText, false);
                }
            }
        }
        // should never get here...
        return null;
    }

    private void doKeyword(final Segment line, final int i, final char c) {
        final int i1 = i + 1;

        final int len = i - this.lastKeyword;
        final byte id = this.keywords.lookup(line, this.lastKeyword, len);
        if (id != Token.NULL) {
            // If this is a Token.KEYWORD1 and line already contains a keyword,
            // then assume this one is a label reference and ignore it.
            // if (id == Token.KEYWORD1 && tokenListContainsKeyword()) {
            // }
            // else {
            if (this.lastKeyword != this.lastOffset)
                this.addToken(this.lastKeyword - this.lastOffset, Token.NULL);
            this.addToken(len, id);
            this.lastOffset = i;
            // }
        }
        this.lastKeyword = i1;
    }
}
