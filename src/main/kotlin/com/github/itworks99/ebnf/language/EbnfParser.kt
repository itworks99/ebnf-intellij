package com.github.itworks99.ebnf.language

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilder.Marker
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

/**
 * Parser for EBNF language.
 * 
 * This parser implements the EBNF grammar according to ISO/IEC 14977:1996 standard
 * and builds a PSI tree from the token stream provided by the lexer.
 */
class EbnfParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        parseFile(builder)
        rootMarker.done(root)
        return builder.treeBuilt
    }
    
    /**
     * Parses an EBNF file, which consists of a sequence of rule definitions.
     */
    private fun parseFile(builder: PsiBuilder) {
        while (!builder.eof()) {
            parseRule(builder)
        }
    }
    
    /**
     * Parses a rule definition, which has the form: identifier = expression ;
     */
    private fun parseRule(builder: PsiBuilder): Boolean {
        val marker = builder.mark()
        
        // Parse rule name (identifier)
        if (builder.tokenType == EbnfTokenTypes.IDENTIFIER) {
            val nameMarker = builder.mark()
            builder.advanceLexer()
            nameMarker.done(EbnfElementTypes.RULE_NAME)
            
            // Parse equals sign
            if (builder.tokenType == EbnfTokenTypes.EQUALS) {
                builder.advanceLexer()
                
                // Parse rule body (expression)
                val bodyMarker = builder.mark()
                if (parseExpression(builder)) {
                    bodyMarker.done(EbnfElementTypes.RULE_BODY)
                    
                    // Parse semicolon
                    if (builder.tokenType == EbnfTokenTypes.SEMICOLON) {
                        builder.advanceLexer()
                        marker.done(EbnfElementTypes.RULE)
                        return true
                    } else {
                        builder.error("Expected ';'")
                    }
                } else {
                    bodyMarker.drop()
                    builder.error("Expected expression")
                }
            } else {
                builder.error("Expected '='")
            }
        } else {
            builder.error("Expected identifier")
        }
        
        marker.drop()
        // Skip to the next token to avoid infinite loops
        if (!builder.eof()) {
            builder.advanceLexer()
        }
        return false
    }
    
    /**
     * Parses an expression, which is a sequence of terms separated by vertical bars.
     */
    private fun parseExpression(builder: PsiBuilder): Boolean {
        val marker = builder.mark()
        
        if (parseTerm(builder)) {
            while (builder.tokenType == EbnfTokenTypes.VERTICAL_BAR) {
                builder.advanceLexer()
                if (!parseTerm(builder)) {
                    builder.error("Expected term after '|'")
                    break
                }
            }
            
            marker.done(EbnfElementTypes.EXPRESSION)
            return true
        }
        
        marker.drop()
        return false
    }
    
    /**
     * Parses a term, which is a sequence of factors separated by commas.
     */
    private fun parseTerm(builder: PsiBuilder): Boolean {
        val marker = builder.mark()
        
        if (parseFactor(builder)) {
            while (builder.tokenType == EbnfTokenTypes.COMMA) {
                builder.advanceLexer()
                if (!parseFactor(builder)) {
                    builder.error("Expected factor after ','")
                    break
                }
            }
            
            marker.done(EbnfElementTypes.TERM)
            return true
        }
        
        marker.drop()
        return false
    }
    
    /**
     * Parses a factor, which is a primary followed by an optional exception.
     */
    private fun parseFactor(builder: PsiBuilder): Boolean {
        val marker = builder.mark()
        
        if (parsePrimary(builder)) {
            // Check for exception (- primary)
            if (builder.tokenType == EbnfTokenTypes.EXCEPTION_SYMBOL) {
                builder.advanceLexer()
                if (!parsePrimary(builder)) {
                    builder.error("Expected primary after '-'")
                }
            }
            
            marker.done(EbnfElementTypes.FACTOR)
            return true
        }
        
        marker.drop()
        return false
    }
    
    /**
     * Parses a primary, which can be an identifier, string literal, group, option, or repetition.
     */
    private fun parsePrimary(builder: PsiBuilder): Boolean {
        val marker = builder.mark()
        
        when (builder.tokenType) {
            // Identifier (rule reference)
            EbnfTokenTypes.IDENTIFIER -> {
                val refMarker = builder.mark()
                builder.advanceLexer()
                refMarker.done(EbnfElementTypes.REFERENCE)
                marker.done(EbnfElementTypes.PRIMARY)
                return true
            }
            
            // String literal
            EbnfTokenTypes.STRING_LITERAL -> {
                builder.advanceLexer()
                marker.done(EbnfElementTypes.PRIMARY)
                return true
            }
            
            // Group: ( expression )
            EbnfTokenTypes.LEFT_PAREN -> {
                val groupMarker = builder.mark()
                builder.advanceLexer()
                
                if (parseExpression(builder)) {
                    if (builder.tokenType == EbnfTokenTypes.RIGHT_PAREN) {
                        builder.advanceLexer()
                        groupMarker.done(EbnfElementTypes.GROUP)
                        marker.done(EbnfElementTypes.PRIMARY)
                        return true
                    } else {
                        builder.error("Expected ')'")
                    }
                } else {
                    builder.error("Expected expression")
                }
                
                groupMarker.drop()
            }
            
            // Option: [ expression ]
            EbnfTokenTypes.LEFT_BRACKET -> {
                val optionMarker = builder.mark()
                builder.advanceLexer()
                
                if (parseExpression(builder)) {
                    if (builder.tokenType == EbnfTokenTypes.RIGHT_BRACKET) {
                        builder.advanceLexer()
                        optionMarker.done(EbnfElementTypes.OPTION)
                        marker.done(EbnfElementTypes.PRIMARY)
                        return true
                    } else {
                        builder.error("Expected ']'")
                    }
                } else {
                    builder.error("Expected expression")
                }
                
                optionMarker.drop()
            }
            
            // Repetition: { expression }
            EbnfTokenTypes.LEFT_BRACE -> {
                val repetitionMarker = builder.mark()
                builder.advanceLexer()
                
                if (parseExpression(builder)) {
                    if (builder.tokenType == EbnfTokenTypes.RIGHT_BRACE) {
                        builder.advanceLexer()
                        repetitionMarker.done(EbnfElementTypes.REPETITION)
                        marker.done(EbnfElementTypes.PRIMARY)
                        return true
                    } else {
                        builder.error("Expected '}'")
                    }
                } else {
                    builder.error("Expected expression")
                }
                
                repetitionMarker.drop()
            }
            
            else -> {
                builder.error("Expected identifier, string literal, '(', '[', or '{'")
            }
        }
        
        marker.drop()
        return false
    }
}