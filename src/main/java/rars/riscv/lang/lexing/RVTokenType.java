package rars.riscv.lang.lexing;

public enum RVTokenType {
    NULL,
    WHITESPACE,
    ERROR,

    COMMENT,
    DIRECTIVE,
    OPERATOR,

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
    LO; // %lo
}
