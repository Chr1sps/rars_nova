package rars.riscv.lang.lexing;

import rars.riscv.InstructionsRegistry;
import rars.riscv.hardware.ControlAndStatusRegisterFile;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;
import javax.swing.text.Segment;
import java.io.IOException;
import java.io.Reader;

/** @noinspection All */
@SuppressWarnings({"fallthrough", "UnnecessaryUnicodeEscape", "SameParameterValue", "UnusedAssignment", "CStyleArrayDeclaration", "FieldCanBeLocal", "unused"})
%%

%public
%final
%class RVLexer<T, P extends TokensProducer<T>>
%implements Lexer<T, P>
%no_suppress_warnings
%unicode
%ignorecase
%type T

%{
    private Segment s;
            private int offsetShift;
            private P producer;
        
            public RVLexer() {
                super();
            }
        
            /**
             * Adds the token specified to the current linked list of tokens.
             *
             * @param tokenType The token's type.
             */
            private void addToken(final RVTokenType tokenType) {
                addToken(zzStartRead, zzMarkedPos-1, tokenType);
            }
        
        
            /**
             * Adds the token specified to the current linked list of tokens.
             *
             * @param tokenType The token's type.
             */
            private void addToken(final int start, final int end, final RVTokenType tokenType) {
                addToken(zzBuffer, start, end, tokenType, getOffset(start));
            }
        
            /**
             * Adds the token specified to the current linked list of tokens.
             *
             * @param array The character array.
             * @param start The starting offset in the array.
             * @param end The ending offset in the array.
             * @param tokenType The token's type.
             * @param startOffset The offset in the document at which this token
             *                    occurs.
             */
            private void addToken(final char[] array, final int start, final int end, final RVTokenType tokenType, final int startOffset) {
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
             * @param text The text from which to get tokens.
             * @param initialTokenType The token type we should start with.
             * @param lineOffset The offset into the document at which
             *                    <code>text</code> starts.
             * @return The first <code>Token</code> in a linked list representing
             *         the syntax highlighted text.
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
             * @return      <code>true</code> if EOF was reached, otherwise
             *              <code>false</code>.
             */
            private boolean zzRefill() {
                return zzCurrentPos>=s.offset+s.count;
            }
        
        
            /**
             * Resets the scanner to read from a new input stream.
             * Does not close the old reader.
             * <br>
             * All internal variables are reset, the old input stream 
             * <b>cannot</b> be reused (internal buffer is discarded and lost).
             * Lexical state is set to <tt>YY_INITIAL</tt>.
             *
             * @param reader   the new input stream 
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
                //zzStartRead = zzEndRead = s.offset;
                zzStartRead = s.offset;
                zzEndRead = zzStartRead + s.count;
                zzCurrentPos = zzMarkedPos = s.offset;
                zzLexicalState = YYINITIAL;
                zzReader = reader;
                zzAtBOL  = true;
                zzAtEOF  = false;
            }
            
            
%}

Letter                   = ([a-zA-Z])
Digit                    = ([0-9])
FirstDigit               = ([1-9])
Integer                  = ({Digit}+)
Float = ([-]{Integer}"."{Integer})

HexDigit                 = ([0-9a-fA-F])
HexInteger               = ([0][xX]{HexDigit}+)

IntegerNew               = ({FirstDigit}{Digit}*|{Digit})

// Adapted from isValidIdentifier()
FirstIdentifierChar     = ({Letter}|[_]|[.]|[$])
IdentifierChar          = ({FirstIdentifierChar}|{Digit})

Identifier		    	= ({FirstIdentifierChar}{IdentifierChar}*)

EscapeSequence          = ([\\][nrt\"\'\\])

UnclosedStringLiteral	= ([\"][^\"]*)
StringLiteral			= ({UnclosedStringLiteral}[\"])
UnclosedCharLiteral		= ([\'][^\']*)
CharLiteral			    = ({UnclosedCharLiteral}[\'])


LineTerminator			= (\n)
WhiteSpace			    = ([ \t\f])

Label				    = ({Identifier}[\:])

Operator				= ("+"|"-"|"*"|"/"|"%"|"^"|"|"|"&"|"~"|"!"|"="|"<"|">")

Comma                   = (",")
Directive               = ("."{Identifier})

MacroArg = ([\%]{Identifier})

CommentBegin			= ([#])
Comment = ({CommentBegin}.*)

RoundingMode            = ("rne"|"rtz"|"rdn"|"rup"|"rmm"|"dyn")
Hi                      = ("%hi")
Lo                      = ("%lo")

LeftParen = ("(")
RightParen = (")")

%%

<YYINITIAL> {
	/* String/Character Literals. */
	{CharLiteral}					{ addToken(RVTokenType.CHAR); }
	{UnclosedCharLiteral}			{ addToken(RVTokenType.UNFINISHED_CHAR); /*addNullToken(); return firstToken;*/ }
	{StringLiteral}				{ addToken(RVTokenType.STRING); }
	{UnclosedStringLiteral}			{ addToken(RVTokenType.UNFINISHED_STRING); addNullToken(); return getResult(); }

	{WhiteSpace}+					{ addToken(RVTokenType.WHITESPACE); }

	{LineTerminator}				{ addNullToken(); return getResult(); }

	{Label}						{ addToken(RVTokenType.LABEL); }

	{Comment}   				{ addToken(RVTokenType.COMMENT); addNullToken(); return getResult(); }
      
    {RoundingMode}				{ addToken(RVTokenType.ROUNDING_MODE); }
      
    {Hi}						{ addToken(RVTokenType.HI); }
    {Lo}                        { addToken(RVTokenType.LO); }
      
    {Comma}                     { addToken(RVTokenType.COMMA); }
    {Directive}					{ addToken(RVTokenType.DIRECTIVE); }
    {Identifier}                {
                            final var foundOps = InstructionsRegistry.matchOperator(yytext());
                            if (foundOps.isEmpty()) {
                                final var foundRegister = RegisterFile.INSTANCE.getRegisterByName(yytext());
                                final var foundFPRegister = FloatingPointRegisterFile.getRegister(yytext());
                                final var foundCASRegister = ControlAndStatusRegisterFile.getRegister(yytext());
                                if (foundRegister != null || foundFPRegister != null || foundCASRegister != null) {
                                    addToken(RVTokenType.REGISTER_NAME);
                                } else {
                                    addToken(RVTokenType.IDENTIFIER);
                                }
                            } else {
                                addToken(RVTokenType.INSTRUCTION);
                            }
                        }

    {LeftParen}                 { addToken(RVTokenType.LEFT_PAREN); }
    {RightParen}                { addToken(RVTokenType.RIGHT_PAREN); }
      
    [\-]?{Integer}                    { addToken(RVTokenType.INTEGER); }
    [\-]?{HexInteger}                 { addToken(RVTokenType.INTEGER); }
    {Float}                     { addToken(RVTokenType.FLOATING); }
      
    {MacroArg}                  { addToken(RVTokenType.MACRO_PARAMETER); }

	/* Ended with a line not in a string or comment. */
	<<EOF>>						{ addNullToken(); return getResult(); }

	/* Catch any other (unhandled) characters. */
	.							{ addToken(RVTokenType.ERROR); }

}