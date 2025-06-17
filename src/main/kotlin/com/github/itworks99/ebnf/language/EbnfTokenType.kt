package com.github.itworks99.ebnf.language

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.NonNls

/**
 * Represents a token type in the EBNF language.
 */
class EbnfTokenType(@NonNls debugName: String) : IElementType(debugName, EbnfLanguage)

/**
 * Defines all token types used in the EBNF language.
 */
object EbnfTokenTypes {
    // Basic tokens
    val IDENTIFIER = EbnfTokenType("IDENTIFIER")
    val STRING_LITERAL = EbnfTokenType("STRING_LITERAL")
    val NUMBER = EbnfTokenType("NUMBER")
    val COMMENT = EbnfTokenType("COMMENT")
    val WHITE_SPACE = EbnfTokenType("WHITE_SPACE")
    val BAD_CHARACTER = EbnfTokenType("BAD_CHARACTER")
    
    // Keywords
    // EBNF doesn't have traditional keywords, but we might add some for extensions
    
    // Operators and punctuation
    val EQUALS = EbnfTokenType("EQUALS") // =
    val SEMICOLON = EbnfTokenType("SEMICOLON") // ;
    val COMMA = EbnfTokenType("COMMA") // ,
    val VERTICAL_BAR = EbnfTokenType("VERTICAL_BAR") // |
    val LEFT_PAREN = EbnfTokenType("LEFT_PAREN") // (
    val RIGHT_PAREN = EbnfTokenType("RIGHT_PAREN") // )
    val LEFT_BRACE = EbnfTokenType("LEFT_BRACE") // {
    val RIGHT_BRACE = EbnfTokenType("RIGHT_BRACE") // }
    val LEFT_BRACKET = EbnfTokenType("LEFT_BRACKET") // [
    val RIGHT_BRACKET = EbnfTokenType("RIGHT_BRACKET") // ]
    
    // Special EBNF operators
    val REPETITION_SYMBOL = EbnfTokenType("REPETITION_SYMBOL") // * (used in some EBNF variants)
    val EXCEPTION_SYMBOL = EbnfTokenType("EXCEPTION_SYMBOL") // - (used in some EBNF variants)
    
    // Comment delimiters
    val COMMENT_START = EbnfTokenType("COMMENT_START") // (*
    val COMMENT_END = EbnfTokenType("COMMENT_END") // *)
    
    // Token sets for easier handling
    val WHITESPACES = TokenSet.create(WHITE_SPACE)
    val COMMENTS = TokenSet.create(COMMENT)
    val STRING_LITERALS = TokenSet.create(STRING_LITERAL)
    val IDENTIFIERS = TokenSet.create(IDENTIFIER)
    val OPERATORS = TokenSet.create(
        EQUALS, SEMICOLON, COMMA, VERTICAL_BAR,
        LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
        LEFT_BRACKET, RIGHT_BRACKET, REPETITION_SYMBOL, EXCEPTION_SYMBOL
    )
}