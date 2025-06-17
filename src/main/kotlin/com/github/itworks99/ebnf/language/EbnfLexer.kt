package com.github.itworks99.ebnf.language

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * A simple lexer for EBNF syntax.
 * 
 * This lexer tokenizes EBNF code according to the standard EBNF syntax rules.
 */
class EbnfLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var currentToken: IElementType? = null
    private var state: Int = 0
    
    companion object {
        // Lexer states
        private const val INITIAL = 0
        private const val IN_COMMENT = 1
        private const val IN_STRING = 2
    }
    
    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.state = initialState
        advance()
    }
    
    override fun getState(): Int = state
    
    override fun getTokenType(): IElementType? = currentToken
    
    override fun getTokenStart(): Int = tokenStart
    
    override fun getTokenEnd(): Int = tokenEnd
    
    override fun advance() {
        tokenStart = tokenEnd
        
        if (tokenStart >= bufferEnd) {
            currentToken = null
            return
        }
        
        when (state) {
            INITIAL -> scanInitial()
            IN_COMMENT -> scanComment()
            IN_STRING -> scanString()
        }
    }
    
    override fun getBufferSequence(): CharSequence = buffer
    
    override fun getBufferEnd(): Int = bufferEnd
    
    private fun scanInitial() {
        val c = buffer[tokenStart]
        
        when {
            c.isWhitespace() -> scanWhitespace()
            c.isLetter() -> scanIdentifier()
            c == '"' -> {
                state = IN_STRING
                tokenEnd = tokenStart + 1
                scanString()
            }
            c == '(' && tokenStart + 1 < bufferEnd && buffer[tokenStart + 1] == '*' -> {
                state = IN_COMMENT
                tokenEnd = tokenStart + 2
                currentToken = EbnfTokenTypes.COMMENT_START
            }
            else -> scanOperator()
        }
    }
    
    private fun scanWhitespace() {
        tokenEnd = tokenStart + 1
        while (tokenEnd < bufferEnd && buffer[tokenEnd].isWhitespace()) {
            tokenEnd++
        }
        currentToken = EbnfTokenTypes.WHITE_SPACE
    }
    
    private fun scanIdentifier() {
        tokenEnd = tokenStart + 1
        while (tokenEnd < bufferEnd && (buffer[tokenEnd].isLetterOrDigit() || buffer[tokenEnd] == '_')) {
            tokenEnd++
        }
        currentToken = EbnfTokenTypes.IDENTIFIER
    }
    
    private fun scanString() {
        if (state != IN_STRING) {
            state = IN_STRING
            tokenEnd = tokenStart + 1
        }
        
        while (tokenEnd < bufferEnd) {
            if (buffer[tokenEnd] == '"' && (tokenEnd == tokenStart || buffer[tokenEnd - 1] != '\\')) {
                tokenEnd++
                state = INITIAL
                currentToken = EbnfTokenTypes.STRING_LITERAL
                return
            }
            tokenEnd++
        }
        
        // Unclosed string
        currentToken = EbnfTokenTypes.BAD_CHARACTER
        state = INITIAL
    }
    
    private fun scanComment() {
        while (tokenEnd + 1 < bufferEnd) {
            if (buffer[tokenEnd] == '*' && buffer[tokenEnd + 1] == ')') {
                tokenEnd += 2
                state = INITIAL
                currentToken = EbnfTokenTypes.COMMENT
                return
            }
            tokenEnd++
        }
        
        // Unclosed comment
        tokenEnd = bufferEnd
        currentToken = EbnfTokenTypes.COMMENT
    }
    
    private fun scanOperator() {
        val c = buffer[tokenStart]
        tokenEnd = tokenStart + 1
        
        currentToken = when (c) {
            '=' -> EbnfTokenTypes.EQUALS
            ';' -> EbnfTokenTypes.SEMICOLON
            ',' -> EbnfTokenTypes.COMMA
            '|' -> EbnfTokenTypes.VERTICAL_BAR
            '(' -> EbnfTokenTypes.LEFT_PAREN
            ')' -> EbnfTokenTypes.RIGHT_PAREN
            '{' -> EbnfTokenTypes.LEFT_BRACE
            '}' -> EbnfTokenTypes.RIGHT_BRACE
            '[' -> EbnfTokenTypes.LEFT_BRACKET
            ']' -> EbnfTokenTypes.RIGHT_BRACKET
            '*' -> EbnfTokenTypes.REPETITION_SYMBOL
            '-' -> EbnfTokenTypes.EXCEPTION_SYMBOL
            else -> EbnfTokenTypes.BAD_CHARACTER
        }
    }
}