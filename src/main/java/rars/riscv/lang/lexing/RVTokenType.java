package rars.riscv.lang.lexing;

public enum RVTokenType {
    /**
     * Denotes the end of the token list for a line in the document (EOL,
     * basically).
     */
    NULL,
    WHITESPACE,
    ERROR,

    COMMENT,
    DIRECTIVE,
    OPERATOR,

    /**
     * Either register name or it's number with an according prefix (i.e.
     * `fa0`, `x11`, etc.);
     */
    REGISTER_NAME,

    IDENTIFIER,

    INTEGER,
    FLOATING,
    STRING,
    CHAR,
    UNFINISHED_STRING,
    UNFINISHED_CHAR,

    LABEL, // .ident
    INSTRUCTION,

    PLUS, // '+'
    MINUS, // '-'
    LEFT_PAREN, // '('
    RIGHT_PAREN, // ')'
    COMMA, // ','
    ROUNDING_MODE, // "rne", "rtz", "rdn", "rup", "rmm", "dyn"
    MACRO_PARAMETER, // %ident, $ident
    HI, // %hi
    LO // %lo
}
