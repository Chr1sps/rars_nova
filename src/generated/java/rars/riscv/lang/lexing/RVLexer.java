// DO NOT EDIT
// Generated by JFlex 1.9.1 http://jflex.de/
// source: src/main/resources/RVLexer.flex

package rars.riscv.lang.lexing;

import rars.Globals;
import rars.riscv.InstructionsRegistry;

import javax.swing.text.Segment;
import java.io.IOException;
import java.io.Reader;

/** @noinspection All */
@SuppressWarnings({
    "fallthrough",
    "UnnecessaryUnicodeEscape",
    "SameParameterValue",
    "UnusedAssignment",
    "CStyleArrayDeclaration",
    "FieldCanBeLocal",
    "unused"
})

public final class RVLexer<T, P extends TokensProducer<T>> implements Lexer<T, P> {

    /** This character denotes the end of file. */
    public static final int YYEOF = -1;
    // Lexical states.
    public static final int YYINITIAL = 0;
    /** Initial size of the lookahead buffer. */
    private static final int ZZ_BUFFERSIZE = 16384;
    /**
     * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
     * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
     * at the beginning of a line
     * l is of the form l = 2*k, k a non negative integer
     */
    private static final int ZZ_LEXSTATE[] = {
        0, 0
    };
    private static final String ZZ_CMAP_TOP_PACKED_0 =
        "\1\0\1\u0100\36\u0200\1\u0300\267\u0200\10\u0400\u1020\u0200";
    /**
     * Top-level table for translating characters to character classes
     */
    private static final int[] ZZ_CMAP_TOP = zzUnpackcmap_top();
    private static final String ZZ_CMAP_BLOCKS_PACKED_0 =
        "\11\0\1\1\1\2\1\3\1\4\1\3\22\0\1\1" +
            "\1\0\1\5\1\6\1\7\1\10\1\0\1\11\1\12" +
            "\1\13\2\0\1\14\1\15\1\16\1\0\1\17\11\20" +
            "\1\21\6\0\3\22\1\23\1\24\1\22\1\7\1\25" +
            "\1\26\2\7\1\27\1\30\1\31\1\32\1\33\1\7" +
            "\1\34\1\7\1\35\1\36\2\7\1\37\1\40\1\41" +
            "\4\0\1\7\1\0\3\22\1\23\1\24\1\22\1\7" +
            "\1\25\1\26\2\7\1\27\1\30\1\31\1\32\1\33" +
            "\1\7\1\34\1\7\1\35\1\36\2\7\1\37\1\40" +
            "\1\41\12\0\1\3\252\0\2\42\u01f6\0\2\3\326\0" +
            "\u0100\3";
    /**
     * Second-level tables for translating characters to character classes
     */
    private static final int[] ZZ_CMAP_BLOCKS = zzUnpackcmap_blocks();
    private static final String ZZ_ACTION_PACKED_0 =
        "\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\1" +
            "\1\7\1\10\1\11\1\12\1\1\1\6\2\13\2\6" +
            "\1\14\1\15\3\16\1\17\2\13\1\20\1\0\5\6" +
            "\2\21\1\22\1\0\1\13\1\23\1\24";
    /**
     * Translates DFA states to action switch labels.
     */
    private static final int[] ZZ_ACTION = zzUnpackAction();
    private static final String ZZ_ROWMAP_PACKED_0 =
        "\0\0\0\43\0\106\0\43\0\151\0\214\0\257\0\322" +
            "\0\365\0\43\0\43\0\43\0\u0118\0\u013b\0\u015e\0\u0181" +
            "\0\u01a4\0\u01c7\0\43\0\43\0\u01ea\0\u020d\0\u0230\0\43" +
            "\0\u0253\0\u0276\0\u0299\0\u02bc\0\u02df\0\u0302\0\u0325\0\u0348" +
            "\0\u036b\0\u01ea\0\43\0\u01ea\0\u038e\0\u02bc\0\257\0\u038e";
    /**
     * Translates a state to a row index in the transition table
     */
    private static final int[] ZZ_ROWMAP = zzUnpackRowMap();
    private static final String ZZ_TRANS_PACKED_0 =
        "\1\2\1\3\1\4\1\0\1\3\1\5\1\6\1\7" +
            "\1\10\1\11\1\12\1\13\1\14\1\15\1\16\1\17" +
            "\1\20\1\2\1\7\1\21\10\7\1\22\5\7\1\2" +
            "\44\0\1\3\2\0\1\3\36\0\5\5\1\23\35\5" +
            "\2\6\3\0\36\6\7\0\1\7\6\0\3\7\1\24" +
            "\20\7\10\0\1\25\6\0\1\25\3\0\3\25\1\26" +
            "\1\25\1\27\12\25\1\0\11\11\1\30\31\11\17\0" +
            "\1\31\1\32\31\0\1\33\6\0\1\33\2\7\1\24" +
            "\20\33\20\0\2\20\16\0\1\34\22\0\2\20\31\0" +
            "\1\7\6\0\3\7\1\24\16\7\1\35\1\7\10\0" +
            "\1\7\6\0\3\7\1\24\1\7\1\35\4\7\1\36" +
            "\1\37\3\7\1\40\1\41\3\7\10\0\1\25\6\0" +
            "\3\25\1\0\20\25\10\0\1\25\6\0\3\25\1\0" +
            "\4\25\1\42\13\25\1\43\7\0\1\25\6\0\3\25" +
            "\1\0\10\25\1\44\7\25\17\0\1\45\2\32\16\0" +
            "\1\34\21\0\1\45\2\32\31\0\1\33\6\0\3\33" +
            "\1\24\20\33\20\0\2\46\1\0\3\46\25\0\1\7" +
            "\6\0\3\7\1\24\7\7\1\47\10\7\10\0\1\7" +
            "\6\0\3\7\1\24\6\7\1\47\11\7\10\0\1\7" +
            "\6\0\3\7\1\24\2\7\1\47\15\7\10\0\1\7" +
            "\6\0\3\7\1\24\17\7\1\47\10\0\1\7\6\0" +
            "\3\7\1\24\11\7\1\47\6\7\20\0\2\50\22\0";
    /**
     * The transition table of the DFA
     */
    private static final int[] ZZ_TRANS = zzUnpacktrans();
    /** Error code for "Unknown internal scanner error". */
    private static final int ZZ_UNKNOWN_ERROR = 0;
    /** Error code for "could not match input". */
    private static final int ZZ_NO_MATCH = 1;
    /** Error code for "pushback value was too large". */
    private static final int ZZ_PUSHBACK_2BIG = 2;
    /**
     * Error messages for {@link #ZZ_UNKNOWN_ERROR}, {@link #ZZ_NO_MATCH}, and
     * {@link #ZZ_PUSHBACK_2BIG} respectively.
     */
    private static final String ZZ_ERROR_MSG[] = {
        "Unknown internal scanner error",
        "Error: could not match input",
        "Error: pushback value was too large"
    };
    private static final String ZZ_ATTRIBUTE_PACKED_0 =
        "\1\0\1\11\1\1\1\11\5\1\3\11\6\1\2\11" +
            "\3\1\1\11\3\1\1\0\6\1\1\11\1\1\1\0" +
            "\3\1";
    /**
     * ZZ_ATTRIBUTE[aState] contains the attributes of state {@code aState}
     */
    private static final int[] ZZ_ATTRIBUTE = zzUnpackAttribute();
    /** Input device. */
    private java.io.Reader zzReader;
    /** Current state of the DFA. */
    private int zzState;
    /** Current lexical state. */
    private int zzLexicalState = YYINITIAL;
    /**
     * This buffer contains the current text to be matched and is the source of the {@link #yytext()}
     * string.
     */
    private char zzBuffer[] = new char[Math.min(ZZ_BUFFERSIZE, zzMaxBufferLen())];
    /** Text position at the last accepting state. */
    private int zzMarkedPos;
    /** Current text position in the buffer. */
    private int zzCurrentPos;
    /** Marks the beginning of the {@link #yytext()} string in the buffer. */
    private int zzStartRead;
    /** Marks the last character in the buffer, that has been read from input. */
    private int zzEndRead;
    /**
     * Whether the scanner is at the end of file.
     *
     * @see #yyatEOF
     */
    private boolean zzAtEOF;
    /**
     * The number of occupied positions in {@link #zzBuffer} beyond {@link #zzEndRead}.
     *
     * <p>When a lead/high surrogate has been read from the input stream into the final
     * {@link #zzBuffer} position, this will have a value of 1; otherwise, it will have a value of 0.
     */
    private int zzFinalHighSurrogate = 0;
    /** Number of newlines encountered up to the start of the matched text. */
    @SuppressWarnings("unused")
    private int yyline;
    /** Number of characters from the last newline up to the start of the matched text. */
    @SuppressWarnings("unused")
    private int yycolumn;
    /** Number of characters up to the start of the matched text. */
    @SuppressWarnings("unused")
    private long yychar;
    /** Whether the scanner is currently at the beginning of a line. */
    @SuppressWarnings("unused")
    private boolean zzAtBOL = true;
    /** Whether the user-EOF-code has already been executed. */
    @SuppressWarnings("unused")
    private boolean zzEOFDone;
    /* user code: */
    private Segment s;
    private int offsetShift;
    private P producer;

    public RVLexer() {
        super();
    }

    /**
     * Creates a new scanner
     *
     * @param in
     *     the java.io.Reader to read input from.
     */
    public RVLexer(java.io.Reader in) {
        this.zzReader = in;
    }

    private static int[] zzUnpackcmap_top() {
        int[] result = new int[4352];
        int offset = 0;
        offset = zzUnpackcmap_top(ZZ_CMAP_TOP_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackcmap_top(String packed, int offset, int[] result) {
        int i = 0;       /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            do {
                result[j++] = value;
            } while (--count > 0);
        }
        return j;
    }

    private static int[] zzUnpackcmap_blocks() {
        int[] result = new int[1280];
        int offset = 0;
        offset = zzUnpackcmap_blocks(ZZ_CMAP_BLOCKS_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackcmap_blocks(String packed, int offset, int[] result) {
        int i = 0;       /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            do {
                result[j++] = value;
            } while (--count > 0);
        }
        return j;
    }

    private static int[] zzUnpackAction() {
        int[] result = new int[40];
        int offset = 0;
        offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackAction(String packed, int offset, int[] result) {
        int i = 0;       /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            do {
                result[j++] = value;
            } while (--count > 0);
        }
        return j;
    }

    private static int[] zzUnpackRowMap() {
        int[] result = new int[40];
        int offset = 0;
        offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackRowMap(String packed, int offset, int[] result) {
        int i = 0;  /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length() - 1;
        while (i < l) {
            int high = packed.charAt(i++) << 16;
            result[j++] = high | packed.charAt(i++);
        }
        return j;
    }

    private static int[] zzUnpacktrans() {
        int[] result = new int[945];
        int offset = 0;
        offset = zzUnpacktrans(ZZ_TRANS_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpacktrans(String packed, int offset, int[] result) {
        int i = 0;       /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            value--;
            do {
                result[j++] = value;
            } while (--count > 0);
        }
        return j;
    }

    private static int[] zzUnpackAttribute() {
        int[] result = new int[40];
        int offset = 0;
        offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackAttribute(String packed, int offset, int[] result) {
        int i = 0;       /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            do {
                result[j++] = value;
            } while (--count > 0);
        }
        return j;
    }

    /**
     * Translates raw input code points to DFA table row
     */
    private static int zzCMap(int input) {
        int offset = input & 255;
        return offset == input ? ZZ_CMAP_BLOCKS[offset] : ZZ_CMAP_BLOCKS[ZZ_CMAP_TOP[input >> 8] | offset];
    }

    /**
     * Reports an error that occurred while scanning.
     *
     * <p>In a well-formed scanner (no or only correct usage of {@code yypushback(int)} and a
     * match-all fallback rule) this method will only be called with things that
     * "Can't Possibly Happen".
     *
     * <p>If this method is called, something is seriously wrong (e.g. a JFlex bug producing a faulty
     * scanner etc.).
     *
     * <p>Usual syntax/scanner level error handling should be done in error fallback rules.
     *
     * @param errorCode
     *     the code of the error message to display.
     */
    private static void zzScanError(int errorCode) {
        String message;
        try {
            message = ZZ_ERROR_MSG[errorCode];
        } catch (ArrayIndexOutOfBoundsException e) {
            message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
        }

        throw new Error(message);
    }

    /**
     * Adds the token specified to the current linked list of tokens.
     *
     * @param tokenType
     *     The token's type.
     */
    private void addToken(final RVTokenType tokenType) {
        addToken(zzStartRead, zzMarkedPos - 1, tokenType);
    }

    /**
     * Adds the token specified to the current linked list of tokens.
     *
     * @param tokenType
     *     The token's type.
     */
    private void addToken(final int start, final int end, final RVTokenType tokenType) {
        addToken(zzBuffer, start, end, tokenType, getOffset(start));
    }

    /**
     * Adds the token specified to the current linked list of tokens.
     *
     * @param array
     *     The character array.
     * @param start
     *     The starting offset in the array.
     * @param end
     *     The ending offset in the array.
     * @param tokenType
     *     The token's type.
     * @param startOffset
     *     The offset in the document at which this token
     *     occurs.
     */
    private void addToken(
        final char[] array,
        final int start,
        final int end,
        final RVTokenType tokenType,
        final int startOffset
    ) {
        producer.addToken(array, start, end, tokenType, startOffset);
        zzStartRead = zzMarkedPos;
    }

    private void addNullToken() {
        this.producer.addNullToken(zzBuffer, zzStartRead, getOffset(zzStartRead));
    }

    private void addErrorToken(String notice) {
        this.producer.addErrorToken(zzBuffer, zzStartRead, getOffset(zzStartRead), notice);
    }

    private int getOffset(int start) {
        return start + offsetShift;
    }

    private T getResult() {
        return this.producer.getResult();
    }

    /**
     * Returns the first token in the linked list of tokens generated
     * from <code>text</code>.  This method must be implemented by
     * subclasses so they can correctly implement syntax highlighting.
     *
     * @param text
     *     The text from which to get tokens.
     * @param initialTokenType
     *     The token type we should start with.
     * @param lineOffset
     *     The offset into the document at which
     *     <code>text</code> starts.
     * @return The first <code>Token</code> in a linked list representing
     * the syntax highlighted text.
     */
    @Override
    public T getTokensList(Segment text, int initialTokenType, int lineOffset, P producer) {

        this.producer = producer;
        //		resetTokenList();
        this.offsetShift = -text.offset + lineOffset;

        // Start off in the proper state.

        s = text;
        try {
            yyreset(zzReader);
            yybegin(YYINITIAL);
            return yylex();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    /**
     * Refills the input buffer.
     *
     * @return <code>true</code> if EOF was reached, otherwise
     * <code>false</code>.
     */
    private boolean zzRefill() {
        return zzCurrentPos >= s.offset + s.count;
    }

    /**
     * Resets the scanner to read from a new input stream.
     * Does not close the old reader.
     * <br>
     * All internal variables are reset, the old input stream
     * <b>cannot</b> be reused (internal buffer is discarded and lost).
     * Lexical state is set to {@link YY_INITIAL}.
     *
     * @param reader
     *     the new input stream
     */
    public void yyreset(Reader reader) {
        // 's' has been updated.
        zzBuffer = s.array;
        /*
         * We replaced the line below with the two below it because zzRefill
         * no longer "refills" the buffer (since the way we do it, it's always
         * "full" the first time through, since it points to the segment's
         * array).  So, we assign zzEndRead here.
         */
        // zzStartRead = zzEndRead = s.offset;
        zzStartRead = s.offset;
        zzEndRead = zzStartRead + s.count;
        zzCurrentPos = zzMarkedPos = s.offset;
        zzLexicalState = YYINITIAL;
        zzReader = reader;
        zzAtBOL = true;
        zzAtEOF = false;
    }

    /** Returns the maximum size of the scanner buffer, which limits the size of tokens. */
    private int zzMaxBufferLen() {
        return Integer.MAX_VALUE;
    }

    /** Whether the scanner buffer can grow to accommodate a larger token. */
    private boolean zzCanGrow() {
        return true;
    }

    /**
     * Closes the input reader.
     *
     * @throws java.io.IOException
     *     if the reader could not be closed.
     */
    public final void yyclose() throws java.io.IOException {
        zzAtEOF = true; // indicate end of file
        zzEndRead = zzStartRead; // invalidate buffer

        if (zzReader != null) {
            zzReader.close();
        }
    }

    /**
     * Resets the input position.
     */
    private final void yyResetPosition() {
        zzAtBOL = true;
        zzAtEOF = false;
        zzCurrentPos = 0;
        zzMarkedPos = 0;
        zzStartRead = 0;
        zzEndRead = 0;
        zzFinalHighSurrogate = 0;
        yyline = 0;
        yycolumn = 0;
        yychar = 0L;
    }

    /**
     * Returns whether the scanner has reached the end of the reader it reads from.
     *
     * @return whether the scanner has reached EOF.
     */
    public final boolean yyatEOF() {
        return zzAtEOF;
    }

    /**
     * Returns the current lexical state.
     *
     * @return the current lexical state.
     */
    public final int yystate() {
        return zzLexicalState;
    }

    /**
     * Enters a new lexical state.
     *
     * @param newState
     *     the new lexical state
     */
    public final void yybegin(int newState) {
        zzLexicalState = newState;
    }

    /**
     * Returns the text matched by the current regular expression.
     *
     * @return the matched text.
     */
    public final String yytext() {
        return new String(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
    }

    /**
     * Returns the character at the given position from the matched text.
     *
     * <p>It is equivalent to {@code yytext().charAt(pos)}, but faster.
     *
     * @param position
     *     the position of the character to fetch. A value from 0 to {@code yylength()-1}.
     * @return the character at {@code position}.
     */
    public final char yycharat(int position) {
        return zzBuffer[zzStartRead + position];
    }

    /**
     * How many characters were matched.
     *
     * @return the length of the matched text region.
     */
    public final int yylength() {
        return zzMarkedPos - zzStartRead;
    }

    /**
     * Pushes the specified amount of characters back into the input stream.
     *
     * <p>They will be read again by then next call of the scanning method.
     *
     * @param number
     *     the number of characters to be read again. This number must not be greater than
     *     {@link #yylength()}.
     */
    public void yypushback(int number) {
        if (number > yylength()) {
            zzScanError(ZZ_PUSHBACK_2BIG);
        }

        zzMarkedPos -= number;
    }

    /**
     * Resumes scanning until the next regular expression is matched, the end of input is encountered
     * or an I/O-Error occurs.
     *
     * @return the next token.
     * @throws java.io.IOException
     *     if any I/O-Error occurs.
     */
    public T yylex() throws java.io.IOException {
        int zzInput;
        int zzAction;

        // cached fields:
        int zzCurrentPosL;
        int zzMarkedPosL;
        int zzEndReadL = zzEndRead;
        char[] zzBufferL = zzBuffer;

        int[] zzTransL = ZZ_TRANS;
        int[] zzRowMapL = ZZ_ROWMAP;
        int[] zzAttrL = ZZ_ATTRIBUTE;

        while (true) {
            zzMarkedPosL = zzMarkedPos;

            zzAction = -1;

            zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

            zzState = ZZ_LEXSTATE[zzLexicalState];

            // set up zzAction for empty match case:
            int zzAttributes = zzAttrL[zzState];
            if ((zzAttributes & 1) == 1) {
                zzAction = zzState;
            }

            zzForAction:
            {
                while (true) {

                    if (zzCurrentPosL < zzEndReadL) {
                        zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
                        zzCurrentPosL += Character.charCount(zzInput);
                    } else if (zzAtEOF) {
                        zzInput = YYEOF;
                        break zzForAction;
                    } else {
                        // store back cached positions
                        zzCurrentPos = zzCurrentPosL;
                        zzMarkedPos = zzMarkedPosL;
                        boolean eof = zzRefill();
                        // get translated positions and possibly new buffer
                        zzCurrentPosL = zzCurrentPos;
                        zzMarkedPosL = zzMarkedPos;
                        zzBufferL = zzBuffer;
                        zzEndReadL = zzEndRead;
                        if (eof) {
                            zzInput = YYEOF;
                            break zzForAction;
                        } else {
                            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
                            zzCurrentPosL += Character.charCount(zzInput);
                        }
                    }
                    int zzNext = zzTransL[zzRowMapL[zzState] + zzCMap(zzInput)];
                    if (zzNext == -1) break zzForAction;
                    zzState = zzNext;

                    zzAttributes = zzAttrL[zzState];
                    if ((zzAttributes & 1) == 1) {
                        zzAction = zzState;
                        zzMarkedPosL = zzCurrentPosL;
                        if ((zzAttributes & 8) == 8) break zzForAction;
                    }

                }
            }

            // store back cached position
            zzMarkedPos = zzMarkedPosL;

            if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
                zzAtEOF = true;
                switch (zzLexicalState) {
                    case YYINITIAL: {
                        addNullToken();
                        return getResult();
                    }  // fall though
                    case 41:
                        break;
                    default:
                        return null;
                }
            } else {
                switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
                    case 1: {
                        addToken(RVTokenType.ERROR);
                    }
                    // fall through
                    case 21:
                        break;
                    case 2: {
                        addToken(RVTokenType.WHITESPACE);
                    }
                    // fall through
                    case 22:
                        break;
                    case 3: {
                        addNullToken();
                        return getResult();
                    }
                    // fall through
                    case 23:
                        break;
                    case 4: {
                        addToken(RVTokenType.UNFINISHED_STRING);
                        addNullToken();
                        return getResult();
                    }
                    // fall through
                    case 24:
                        break;
                    case 5: {
                        addToken(RVTokenType.COMMENT);
                        addNullToken();
                        return getResult();
                    }
                    // fall through
                    case 25:
                        break;
                    case 6: {
                        final var foundOps = InstructionsRegistry.matchOperator(yytext());
                        if (foundOps.isEmpty()) {
                            final var foundRegister = Globals.REGISTER_FILE.getRegisterByName(yytext());
                            final var foundFPRegister = Globals.FP_REGISTER_FILE.getRegisterByName(yytext());
                            final var foundCASRegister = Globals.CS_REGISTER_FILE.getRegisterByName(yytext());
                            if (foundRegister != null || foundFPRegister != null || foundCASRegister != null) {
                                addToken(RVTokenType.REGISTER_NAME);
                            } else {
                                addToken(RVTokenType.IDENTIFIER);
                            }
                        } else {
                            addToken(RVTokenType.INSTRUCTION);
                        }
                    }
                    // fall through
                    case 26:
                        break;
                    case 7: {
                        addToken(RVTokenType.UNFINISHED_CHAR); /*addNullToken(); return firstToken;*/
                    }
                    // fall through
                    case 27:
                        break;
                    case 8: {
                        addToken(RVTokenType.LEFT_PAREN);
                    }
                    // fall through
                    case 28:
                        break;
                    case 9: {
                        addToken(RVTokenType.RIGHT_PAREN);
                    }
                    // fall through
                    case 29:
                        break;
                    case 10: {
                        addToken(RVTokenType.COMMA);
                    }
                    // fall through
                    case 30:
                        break;
                    case 11: {
                        addToken(RVTokenType.INTEGER);
                    }
                    // fall through
                    case 31:
                        break;
                    case 12: {
                        addToken(RVTokenType.STRING);
                    }
                    // fall through
                    case 32:
                        break;
                    case 13: {
                        addToken(RVTokenType.LABEL);
                    }
                    // fall through
                    case 33:
                        break;
                    case 14: {
                        addToken(RVTokenType.MACRO_PARAMETER);
                    }
                    // fall through
                    case 34:
                        break;
                    case 15: {
                        addToken(RVTokenType.CHAR);
                    }
                    // fall through
                    case 35:
                        break;
                    case 16: {
                        addToken(RVTokenType.DIRECTIVE);
                    }
                    // fall through
                    case 36:
                        break;
                    case 17: {
                        addToken(RVTokenType.HI);
                    }
                    // fall through
                    case 37:
                        break;
                    case 18: {
                        addToken(RVTokenType.LO);
                    }
                    // fall through
                    case 38:
                        break;
                    case 19: {
                        addToken(RVTokenType.ROUNDING_MODE);
                    }
                    // fall through
                    case 39:
                        break;
                    case 20: {
                        addToken(RVTokenType.FLOATING);
                    }
                    // fall through
                    case 40:
                        break;
                    default:
                        zzScanError(ZZ_NO_MATCH);
                }
            }
        }
    }

}
