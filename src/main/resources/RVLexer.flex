package rars.riscv.lang.lexing;

import javax.swing.text.Segment;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

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
            
            private static final Set<String> instructions = Set.of(
                    // region Arithmetic
                    "add",
                    "and",
                    "div",
                    "divu",
                    "mul",
                    "mulh",
                    "mulhsu",
                    "mulhu",
                    "or",
                    "rem",
                    "remu",
                    "sll",
                    "slt",
                    "sltu",
                    "sra",
                    "srl",
                    "sub",
                    "xor",
                    // endregion Arithmetic
                    
                    // region Branch
                    "beq",
                    "bge",
                    "bgeu",
                    "blt",
                    "bltu",
                    "bne",
                    // endregion Branch
                    
                    // region Double
                    "fadd.d",
                    "fdiv.d",
                    "fmax.d",
                    "fmin.d",
                    "fmul.d",
                    "fsub.d",
                    // endregion Double
                    
                    // region Floating
                    "fadd.s",
                    "fdiv.s",
                    "fmax.s",
                    "fmin.s",
                    "fmul.s",
                    "fsub.s",
                    // endregion Floating
                    
                    // region FusedDouble
                    "fmadd.d",
                    "fmsub.d",
                    "fnmadd.d",
                    "fnmsub.d",
                    // endregion FusedDouble
                    
                    // region FusedFloat
                    "fmadd.s",
                    "fmsub.s",
                    "fnmadd.s",
                    "fnmsub.s",
                    // endregion FusedFloat
                    
                    // region ImmediateInstruction
                    "addi",
                    "andi",
                    "ori",
                    "slti",
                    "sltiu",
                    "xori",
                    // endregion ImmediateInstruction
                    
                    // region Load
                    "lb",
                    "lbu",
                    "lh",
                    "lhu",
                    "lw",
                    // endregion Load
                    
                    // region Store
                    "sb",
                    "sh",
                    "sw",
                    // endregion Store
                    
                    // region Other
                    "auipc",
                    "csrrc",
                    "csrrci",
                    "csrrs",
                    "csrrsi",
                    "csrrw",
                    "csrrwi",
                    "ebreak",
                    "ecall",
                    "fclass.d",
                    "fclass.s",
                    "fcvt.d.s",
                    "fcvt.d.w",
                    "fcvt.d.wu",
                    "fcvt.s.d",
                    "fcvt.s.w",
                    "fcvt.s.wu",
                    "fcvt.w.d",
                    "fcvt.w.s",
                    "fcvt.wu.d",
                    "fcvt.wu.s",
                    "fence",
                    "fence.i",
                    "feq.d",
                    "feq.s",
                    "fld",
                    "fle.d",
                    "fle.s",
                    "flt.d",
                    "flt.s",
                    "flw",
                    "fmv.s.x",
                    "fmv.x.s",
                    "fsd",
                    "fsgnj.d",
                    "fsgnjn.d",
                    "fsgnjn.s",
                    "fsgnj.s",
                    "fsgnjx.d",
                    "fsgnjx.s",
                    "fsqrt.d",
                    "fsqrt.s",
                    "fsw",
                    "jal",
                    "jalr",
                    "lui",
                    "slli",
                    "srai",
                    "srli",
                    "uret",
                    "wfi"
                    // endregion Other
            );
            
            private static final Set<String> extendedInstructions = Set.of(
                    "nop",
                    
                    "not",
                    "mv",
                    "neg",
                    
                    "fmv.s",
                    "fabs.s",
                    "fneg.s",
                    "fmv.d",
                    "fabs.d",
                    "fneg.d",
                    
                    "sgt",
                    "sgtu",
                    "seqz",
                    "snez",
                    "sgtz",
                    "sltz",
                    
                    "b",
                    "beqz",
                    "bnez",
                    "bgez",
                    "bltz",
                    "bgtz",
                    "blez",
                    "bgt",
                    "bgtu",
                    "ble",
                    "bleu",
                    
                    "j",
                    "jal",
                    "jr",
                    "jalr",
                    "ret",
                    "call",
                    "tail",
                    
                    "li",
                    "la",
                    "lw",
                    "sw",
                    "lh",
                    "sh",
                    "lb",
                    "sb",
                    "lhu",
                    "lbu",
                    "flw",
                    "fsw",
                    "fld",
                    "fsd",
                    
                    "csrr",
                    "csrw",
                    "csrs",
                    "csrc",
                    
                    "csrwi",
                    "csrsi",
                    "csrci",
                    
                    "frcsr",
                    "fscsr",
                    
                    "frsr",
                    "fssr",
                    
                    "frrm",
                    "fsrm",
                    
                    "frflags",
                    "fsflags",
                    
                    "rdcycle",
                    "rdtime",
                    "rdinstret",
                    "rdcycleh",
                    "rdtimeh",
                    "rdinstreth",
                        
                    "fscrt.s",
                    "fsub.s",
                    "fadd.s",
                    "fmul.s",
                    "fdiv.s",
                    "fmadd.s",
                    "fnmadd.s",
                    "fmsub.s",
                    "fnmsub.s",
                    "fcvt.s.wu",
                    "fcvt.s.w",
                    "fcvt.w.s",
                    "fcvt.wu.s",
                    
                    "fsqrt.d",
                    "fsub.d",
                    "fadd.d",
                    "fmul.d",
                    "fdiv.d",
                    "fmadd.d",
                    "fnmadd.d",
                    "fmsub.d",
                    "fnmsub.d",
                    "fcvt.d.wu",
                    "fcvt.d.w",
                    "fcvt.w.d",
                    "fcvt.wu.d",
                    "fcvt.s.d",
                    "fcvt.d.s",
                    
                    "sext.b",
                    "sext.h",
                    "zext.b",
                    "zext.h",
                    
                    "fmv.x.w",
                    "fmv.w.x",
                    
                    "lui",
                    "addi",
    //                "lb",
    //                "lh",
    //                "lw",
    //                "flw",
    //                "fld",
                    
                    "fgt.s",
                    "fge.s",
                    "fgt.d",
                    "fge.d"
            );
        
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

Letter                  = ([a-zA-Z])
Digit                   = ([0-9])
FirstDigit              = ([1-9])
Integer                  = ({Digit}+)
Float = ([-]{Integer}"."{Integer})

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


// Everything below is *stable*.
CommentBegin			= ([#])
Comment = ({CommentBegin}.*)

RoundingMode            = ("rne"|"rtz"|"rdn"|"rup"|"rmm"|"dyn")
Hi                      = ("%hi")
Lo                      = ("%lo")

LeftParen = ("(")
RightParen = (")")

RegisterNumber          = ("x"{IntegerNew})
RegisterName            = (
  "zero" |
    "ra" |
    "sp" |
    "gp" |
    "tp" |
    "t0" |
    "t1" |
    "t2" |
    "s0" |
    "s1" |
    "a0" |
    "a1" |
    "a2" |
    "a3" |
    "a4" |
    "a5" |
    "a6" |
    "a7" |
    "s2" |
    "s3" |
    "s4" |
    "s5" |
    "s6" |
    "s7" |
    "s8" |
    "s9" |
    "s10" |
    "s11" |
    "t3" |
    "t4" |
    "t5" |
    "t6"
)

FloatingRegisterNumber = ("f"{IntegerNew})
FloatingRegisterName = (
    "ft0" |
        "ft1" |
        "ft2" |
        "ft3" |
        "ft4" |
        "ft5" |
        "ft6" |
        "ft7" |
        "fs0" |
        "fs1" |
        "fa0" |
        "fa1" |
        "fa2" |
        "fa3" |
        "fa4" |
        "fa5" |
        "fa6" |
        "fa7" |
        "fs2" |
        "fs3" |
        "fs4" |
        "fs5" |
        "fs6" |
        "fs7" |
        "fs8" |
        "fs9" |
        "fs10" |
        "fs11" |
        "ft8" |
        "ft9" |
        "ft10" |
        "ft11"
)

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

    {RegisterNumber}            { addToken(RVTokenType.REGISTER_NAME); }
    {RegisterName}              { addToken(RVTokenType.REGISTER_NAME); }
      
    {RoundingMode}				{ addToken(RVTokenType.ROUNDING_MODE); }
      
    {Hi}						{ addToken(RVTokenType.HI); }
    {Lo}                        { addToken(RVTokenType.LO); }
      
    {Comma}                     { addToken(RVTokenType.COMMA); }
    {Directive}					{ addToken(RVTokenType.DIRECTIVE); }
    {Identifier}                {
        if (instructions.contains(yytext()) || extendedInstructions.contains(yytext())) {
            addToken(RVTokenType.INSTRUCTION);
        } else {
            addToken(RVTokenType.IDENTIFIER);
        }
    }

    {LeftParen}                 { addToken(RVTokenType.LEFT_PAREN); }
    {RightParen}                { addToken(RVTokenType.RIGHT_PAREN); }
      
    [\-]?{Integer}                    { addToken(RVTokenType.INTEGER); }
    {Float}                     { addToken(RVTokenType.FLOATING); }
      
    {MacroArg}                  { addToken(RVTokenType.MACRO_PARAMETER); }

	/* Ended with a line not in a string or comment. */
	<<EOF>>						{ addNullToken(); return getResult(); }

	/* Catch any other (unhandled) characters. */
	.							{ addToken(RVTokenType.ERROR); }

}