package org.dockbox.hartshorn.hsl.token;

public enum TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    PLUS_PLUS, MINUS_MINUS,

    //Bitwise operations
    SHIFT_RIGHT, SHIFT_LEFT, LOGICAL_SHIFT_RIGHT,

    //Conditional
    QUESTION_MARK, COLON,ELVIS,

    // Literals.
    IDENTIFIER, STRING, NUMBER, CHAR, NATIVE,

    PREFIX, INFIX,

    // Keywords.
    CLASS, ELSE, FALSE, FUN, FOR, REPEAT, IF, NONE, OR, AND, XOR, EXTENDS,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, DO, WHILE, BREAK, CONTINUE, NIL,
    TEST, MODULE,

    EOF, ARRAY, ARRAY_OPEN, ARRAY_CLOSE,
}
