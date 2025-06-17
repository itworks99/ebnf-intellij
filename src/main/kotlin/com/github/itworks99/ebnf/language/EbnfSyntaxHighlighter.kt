package com.github.itworks99.ebnf.language

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

/**
 * Syntax highlighter for EBNF files.
 * 
 * This class defines the colors and styles for different EBNF syntax elements.
 */
class EbnfSyntaxHighlighter : SyntaxHighlighterBase() {
    
    companion object {
        // Define text attribute keys for different token types
        val IDENTIFIER = TextAttributesKey.createTextAttributesKey(
            "EBNF_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER
        )
        
        val STRING = TextAttributesKey.createTextAttributesKey(
            "EBNF_STRING", DefaultLanguageHighlighterColors.STRING
        )
        
        val NUMBER = TextAttributesKey.createTextAttributesKey(
            "EBNF_NUMBER", DefaultLanguageHighlighterColors.NUMBER
        )
        
        val COMMENT = TextAttributesKey.createTextAttributesKey(
            "EBNF_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT
        )
        
        val OPERATOR = TextAttributesKey.createTextAttributesKey(
            "EBNF_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        
        val BRACES = TextAttributesKey.createTextAttributesKey(
            "EBNF_BRACES", DefaultLanguageHighlighterColors.BRACES
        )
        
        val BRACKETS = TextAttributesKey.createTextAttributesKey(
            "EBNF_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS
        )
        
        val PARENTHESES = TextAttributesKey.createTextAttributesKey(
            "EBNF_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES
        )
        
        val SEMICOLON = TextAttributesKey.createTextAttributesKey(
            "EBNF_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON
        )
        
        val COMMA = TextAttributesKey.createTextAttributesKey(
            "EBNF_COMMA", DefaultLanguageHighlighterColors.COMMA
        )
        
        val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey(
            "EBNF_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER
        )
    }
    
    override fun getHighlightingLexer(): Lexer = EbnfLexer()
    
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            EbnfTokenTypes.IDENTIFIER -> arrayOf(IDENTIFIER)
            EbnfTokenTypes.STRING_LITERAL -> arrayOf(STRING)
            EbnfTokenTypes.NUMBER -> arrayOf(NUMBER)
            EbnfTokenTypes.COMMENT, EbnfTokenTypes.COMMENT_START, EbnfTokenTypes.COMMENT_END -> arrayOf(COMMENT)
            
            EbnfTokenTypes.EQUALS, EbnfTokenTypes.VERTICAL_BAR, 
            EbnfTokenTypes.REPETITION_SYMBOL, EbnfTokenTypes.EXCEPTION_SYMBOL -> arrayOf(OPERATOR)
            
            EbnfTokenTypes.LEFT_BRACE, EbnfTokenTypes.RIGHT_BRACE -> arrayOf(BRACES)
            EbnfTokenTypes.LEFT_BRACKET, EbnfTokenTypes.RIGHT_BRACKET -> arrayOf(BRACKETS)
            EbnfTokenTypes.LEFT_PAREN, EbnfTokenTypes.RIGHT_PAREN -> arrayOf(PARENTHESES)
            
            EbnfTokenTypes.SEMICOLON -> arrayOf(SEMICOLON)
            EbnfTokenTypes.COMMA -> arrayOf(COMMA)
            
            EbnfTokenTypes.BAD_CHARACTER -> arrayOf(BAD_CHARACTER)
            
            else -> TextAttributesKey.EMPTY_ARRAY
        }
    }
}