package rars.venus.editors.jeditsyntax.tokenmarker;

public enum TokenType {
    /**
     * Normal text token id. This should be used to mark
     * normal text.
     */
    NULL, // 0

    /**
     * Comment 1 token id. This can be used to mark a comment.
     */
    COMMENT1,

    /**
     * Comment 2 token id. This can be used to mark a comment.
     */
    COMMENT2,

    /**
     * Literal 1 token id. This can be used to mark a string
     * literal (eg, C mode uses this to mark "..." literals)
     */
    LITERAL1,

    /**
     * Literal 2 token id. This can be used to mark an object
     * literal (eg, Java mode uses this to mark true, false, etc)
     */
    LITERAL2,

    /**
     * Label token id. This can be used to mark labels
     * (eg, C mode uses this to mark ...: sequences)
     */
    LABEL,

    /**
     * Keyword 1 token id. This can be used to mark a
     * keyword. This should be used for general language
     * constructs.
     */
    KEYWORD1,

    /**
     * Keyword 2 token id. This can be used to mark a
     * keyword. This should be used for preprocessor
     * commands, or variables.
     */
    KEYWORD2,

    /**
     * Keyword 3 token id. This can be used to mark a
     * keyword. This should be used for data types.
     */
    KEYWORD3,

    /**
     * Operator token id. This can be used to mark an
     * operator. (eg, SQL mode marks +, -, etc with this
     * token type)
     */
    OPERATOR,

    /**
     * Invalid token id. This can be used to mark invalid
     * or incomplete tokens, so the user can easily spot
     * syntax errors.
     */
    INVALID,

    /**
     * Macro parameter token. Added for MARS 4.3.
     */
    MACRO_ARG/*,

     *//*
     * The token type, that along with a length of 0
     * marks the end of the token list.
     *//*
    END*/;

    public static final byte ID_COUNT = (byte) TokenType.values().length;
    /**
     * The first id that can be used for internal state
     * in a token marker.
     */
    public static final byte INTERNAL_FIRST = 100;

    /**
     * The last id that can be used for internal state
     * in a token marker.
     */
    public static final byte INTERNAL_LAST = 126;
}
