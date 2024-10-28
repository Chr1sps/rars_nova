/*
 * MIPSTokenMarker.java - MIPS Assembly token marker
 * Copyright (C) 1998, 1999 Slava Pestov, 2010 Pete Sanderson
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package rars.venus.editors.jeditsyntax.tokenmarker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Settings;
import rars.assembler.Directive;
import rars.riscv.BasicInstruction;
import rars.riscv.Instruction;
import rars.riscv.Instructions;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.Register;
import rars.riscv.hardware.RegisterFile;
import rars.venus.editors.jeditsyntax.KeywordMap;
import rars.venus.editors.jeditsyntax.PopupHelpItem;

import javax.swing.text.Segment;
import java.util.*;

import static rars.Globals.getSettings;
import static rars.assembler.TokenType.isValidIdentifier;

/**
 * RISCV token marker.
 *
 * @author Pete Sanderson (2010) and Slava Pestov (1999)
 */
public class RISCVTokenMarker extends TokenMarker {
    private static final @NotNull Map<TokenType, String> tokenLabels = Map.ofEntries(
            Map.entry(TokenType.COMMENT1, "Comment"),
            Map.entry(TokenType.LITERAL1, "String literal"),
            Map.entry(TokenType.LITERAL2, "Character literal"),
            Map.entry(TokenType.LABEL, "Label"),
            Map.entry(TokenType.KEYWORD1, "Instruction"),
            Map.entry(TokenType.KEYWORD2, "Assembler directive"),
            Map.entry(TokenType.KEYWORD3, "Register"),
            Map.entry(TokenType.INVALID, "In-progress, invalid"),
            Map.entry(TokenType.MACRO_ARG, "Macro parameter")
    );
    private static final @NotNull Map<TokenType, String> tokenExamples = Map.ofEntries(
            Map.entry(TokenType.COMMENT1, "# Load"),
            Map.entry(TokenType.LITERAL1, "\"First\""),
            Map.entry(TokenType.LITERAL2, "'\\n'"),
            Map.entry(TokenType.LABEL, "main:"),
            Map.entry(TokenType.KEYWORD1, "lui"),
            Map.entry(TokenType.KEYWORD2, ".text"),
            Map.entry(TokenType.KEYWORD3, "zero"),
            Map.entry(TokenType.INVALID, "\"Regi"),
            Map.entry(TokenType.MACRO_ARG, "%arg")
    );
    // private members
    private static KeywordMap cKeywords;
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
     * @param keywords a {@link KeywordMap} object
     */
    public RISCVTokenMarker(final KeywordMap keywords) {
        super();
        this.keywords = keywords;
    }

    public static @NotNull Map<TokenType, String> getRISCVTokenLabels() {
        return RISCVTokenMarker.tokenLabels;
    }

    public static @NotNull Map<TokenType, String> getRISCVTokenExamples() {
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
    private static @Nullable ArrayList<PopupHelpItem> getTextFromInstructionMatch(final String tokenText, final boolean exact) {
        final List<Instruction> matches;
        final ArrayList<PopupHelpItem> results = new ArrayList<>();
        if (exact) {
            matches = Instructions.matchOperator(tokenText);
        } else {
            matches = Instructions.matchOperatorByPrefix(tokenText);
        }
        if (matches.isEmpty()) {
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
     * Get KeywordMap containing all MIPS first words. This includes all instruction
     * mnemonics,
     * assembler directives, and register names.
     *
     * @return KeywordMap where first is the keyword and associated second is the token
     * type (e.g. Token.KEYWORD1).
     */
    private static KeywordMap getKeywords() {
        if (RISCVTokenMarker.cKeywords == null) {
            RISCVTokenMarker.cKeywords = new KeywordMap(false);
            // add Instruction mnemonics
            for (final Instruction inst : Instructions.INSTRUCTIONS_ALL) {
                RISCVTokenMarker.cKeywords.add(inst.getName(), TokenType.KEYWORD1);
            }
            // add assembler directives
            for (final Directive direct : Directive.getDirectiveList()) {
                RISCVTokenMarker.cKeywords.add(direct.getName(), TokenType.KEYWORD2);
            }
            // add integer register file
            for (final Register r : RegisterFile.getRegisters()) {
                RISCVTokenMarker.cKeywords.add(r.getName(), TokenType.KEYWORD3);
                RISCVTokenMarker.cKeywords.add("x" + r.getNumber(), TokenType.KEYWORD3); // also recognize x0, x1, x2,
                // etc
            }

            RISCVTokenMarker.cKeywords.add("fp", TokenType.KEYWORD3);

            // add floating point register file
            for (final Register r : FloatingPointRegisterFile.getRegisters()) {
                RISCVTokenMarker.cKeywords.add(r.getName(), TokenType.KEYWORD3);
                RISCVTokenMarker.cKeywords.add("f" + r.getNumber(), TokenType.KEYWORD3);
            }
        }
        return RISCVTokenMarker.cKeywords;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public rars.venus.editors.jeditsyntax.tokenmarker.TokenType markTokensImpl(final @NotNull ArrayList<Token> tokens, @NotNull rars.venus.editors.jeditsyntax.tokenmarker.TokenType token, final @NotNull Segment line, final int lineIndex) {
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
                case TokenType.NULL:
                    switch (c) {
                        case '"':
                            this.doKeyword(tokens, line, i);
                            if (backslash)
                                backslash = false;
                            else {
                                addToken(tokens, i - this.lastOffset, token);
                                token = TokenType.LITERAL1;
                                this.lastOffset = this.lastKeyword = i;
                            }
                            break;
                        case '\'':
                            this.doKeyword(tokens, line, i);
                            if (backslash)
                                backslash = false;
                            else {
                                addToken(tokens, i - this.lastOffset, token);
                                token = TokenType.LITERAL2;
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
                                validIdentifier = isValidIdentifier(new String(array, this.lastOffset, i1 - this.lastOffset - 1).trim());
                            } catch (final StringIndexOutOfBoundsException e) {
                                validIdentifier = false;
                            }
                            if (validIdentifier) {
                                addToken(tokens, i1 - this.lastOffset, TokenType.LABEL);
                                this.lastOffset = this.lastKeyword = i1;
                            }
                            break;
                        case '#':
                            backslash = false;
                            this.doKeyword(tokens, line, i);
                            addToken(tokens, i - this.lastOffset, token);
                            addToken(tokens, length - i, TokenType.COMMENT1);
                            this.lastOffset = this.lastKeyword = length;
                            break loop;

                        default:
                            backslash = false;
                            // . and $ added 4/6/10 DPS; % added 12/12 M.Sekhavat
                            if (!Character.isLetterOrDigit(c)
                                    && c != '_' && c != '.' && c != '$' && c != '%')
                                this.doKeyword(tokens, line, i);
                            break;
                    }
                    break;
                case TokenType.LITERAL1:
                    if (backslash)
                        backslash = false;
                    else if (c == '"') {
                        addToken(tokens, i1 - this.lastOffset, token);
                        token = TokenType.NULL;
                        this.lastOffset = this.lastKeyword = i1;
                    }
                    break;
                case TokenType.LITERAL2:
                    if (backslash)
                        backslash = false;
                    else if (c == '\'') {
                        addToken(tokens, i1 - this.lastOffset, TokenType.LITERAL1);
                        token = TokenType.NULL;
                        this.lastOffset = this.lastKeyword = i1;
                    }
                    break;
                default:
                    throw new InternalError("Invalid state: "
                            + token);
            }
        }

        if (token == TokenType.NULL)
            this.doKeyword(tokens, line, length);

        switch (token) {
            case TokenType.LITERAL1:
            case TokenType.LITERAL2:
                addToken(tokens, length - this.lastOffset, TokenType.INVALID);
                token = TokenType.NULL;
                break;
            case TokenType.KEYWORD2:
                addToken(tokens, length - this.lastOffset, token);
                if (!backslash)
                    token = TokenType.NULL;
            default:
                addToken(tokens, length - this.lastOffset, token);
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
    public @Nullable ArrayList<PopupHelpItem> getTokenExactMatchHelp(final @NotNull List<Token> tokens, final Token token, final String tokenText) {
        ArrayList<PopupHelpItem> matches = null;
        if (token != null && token.type() == TokenType.KEYWORD1) {
            final List<Instruction> instrMatches = Instructions.matchOperator(tokenText);
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
        if (token != null && token.type() == TokenType.KEYWORD2) {
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
    public @Nullable ArrayList<PopupHelpItem> getTokenPrefixMatchHelp(
            final String line,
            final @NotNull List<Token> tokens,
            final @Nullable Token tokenAtOffset,
            final String tokenText) {
        // CASE: Unlikely boundary case...
        if (tokens.isEmpty() || tokens.getFirst().type() == TokenType.END) {
            return null;
        }

        // CASE: if current token is a comment, turn off the text.
        if (tokenAtOffset != null && tokenAtOffset.type() == TokenType.COMMENT1) {
            return null;
        }

        // Let's see if the line already contains an instruction or directive. If so, we
        // need its token
        // text as well so we can do the match. Also need to distinguish the case where
        // current
        // token is also an instruction/directive (moreThanOneKeyword variable).

        String keywordTokenText = null;
        TokenType keywordType = null;
        int offset = 0;
        boolean moreThanOneKeyword = false;
        for (final var token : tokens) {
            if (token.type() == TokenType.KEYWORD1 || token.type() == TokenType.KEYWORD2) {
                if (keywordTokenText != null) {
                    moreThanOneKeyword = true;
                    break;
                }
                keywordTokenText = line.substring(offset, offset + token.length());
                keywordType = token.type();
            }
            offset += token.length();
        }

        // CASE: Current token is valid KEYWORD1 (MIPS instruction). If this line
        // contains a previous KEYWORD1 or KEYWORD2
        // token, then we ignore this one and do exact match on the first one. If it
        // does not, there may be longer
        // instructions for which this is a prefix, so do a prefix match on current
        // token.
        if (tokenAtOffset != null && tokenAtOffset.type() == TokenType.KEYWORD1) {
            if (moreThanOneKeyword) {
                return (keywordType == TokenType.KEYWORD1) ? RISCVTokenMarker.getTextFromInstructionMatch(keywordTokenText, true)
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
        if (tokenAtOffset != null && tokenAtOffset.type() == TokenType.KEYWORD2) {
            if (moreThanOneKeyword) {
                return (keywordType == TokenType.KEYWORD1) ? RISCVTokenMarker.getTextFromInstructionMatch(keywordTokenText, true)
                        : RISCVTokenMarker.getTextFromDirectiveMatch(keywordTokenText, true);
            } else {
                return RISCVTokenMarker.getTextFromDirectiveMatch(tokenText, false);
            }
        }

        // CASE: line already contains KEYWORD1 or KEYWORD2 and current token is
        // something other
        // than KEYWORD1 or KEYWORD2. Generate text based on exact match of that token.
        if (keywordTokenText != null) {
            if (keywordType == TokenType.KEYWORD1) {
                return RISCVTokenMarker.getTextFromInstructionMatch(keywordTokenText, true);
            }
            return RISCVTokenMarker.getTextFromDirectiveMatch(keywordTokenText, true);
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

        if (tokenAtOffset != null && tokenAtOffset.type() == TokenType.NULL) {

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

    private void doKeyword(final @NotNull ArrayList<Token> tokens, final Segment line, final int i) {
        final int i1 = i + 1;

        final int len = i - this.lastKeyword;
        final var id = this.keywords.lookup(line, this.lastKeyword, len);
        if (id != TokenType.NULL) {
            // If this is a Token.KEYWORD1 and line already contains a keyword,
            // then assume this one is a label reference and ignore it.
            // if (id == Token.KEYWORD1 && tokenListContainsKeyword()) {
            // }
            // else {
            if (this.lastKeyword != this.lastOffset)
                addToken(tokens, this.lastKeyword - this.lastOffset, TokenType.NULL);
            addToken(tokens, len, id);
            this.lastOffset = i;
            // }
        }
        this.lastKeyword = i1;
    }
}
