package com.github.itworks99.ebnf.language.documentation

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfTokenTypes
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Provides documentation for EBNF language elements.
 *
 * This class implements quick documentation for EBNF syntax and rules.
 */
class EbnfDocumentationProvider : AbstractDocumentationProvider() {

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) return null

        // For rule names, show quick navigation info
        if (element.node.elementType == EbnfElementTypes.RULE_NAME) {
            return "Rule: ${element.text}"
        }

        return null
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) return null

        // For rule names, show documentation
        if (element.node.elementType == EbnfElementTypes.RULE_NAME) {
            val ruleName = element.text
            val ruleElement = element.parent
            
            // Look for a comment before the rule
            val comment = findCommentBeforeRule(ruleElement)
            
            return buildString {
                append("<b>Rule: $ruleName</b><br><br>")
                
                if (comment != null) {
                    // Extract comment text and format it
                    val commentText = comment.text
                        .removePrefix("(*")
                        .removeSuffix("*)")
                        .trim()
                    
                    append(commentText.replace("\n", "<br>"))
                } else {
                    append("No documentation available.")
                }
            }
        }
        
        // For built-in syntax elements, show documentation
        if (element.node.elementType == EbnfTokenTypes.EQUALS) {
            return "<b>=</b><br><br>Equals sign. Used to define a rule."
        }
        
        if (element.node.elementType == EbnfTokenTypes.SEMICOLON) {
            return "<b>;</b><br><br>Semicolon. Used to terminate a rule definition."
        }
        
        if (element.node.elementType == EbnfTokenTypes.VERTICAL_BAR) {
            return "<b>|</b><br><br>Vertical bar. Used for alternation (logical OR)."
        }
        
        if (element.node.elementType == EbnfTokenTypes.COMMA) {
            return "<b>,</b><br><br>Comma. Used for concatenation (logical AND)."
        }
        
        if (element.node.elementType == EbnfTokenTypes.LEFT_PAREN) {
            return "<b>(</b><br><br>Left parenthesis. Used to group expressions."
        }
        
        if (element.node.elementType == EbnfTokenTypes.RIGHT_PAREN) {
            return "<b>)</b><br><br>Right parenthesis. Used to group expressions."
        }
        
        if (element.node.elementType == EbnfTokenTypes.LEFT_BRACKET) {
            return "<b>[</b><br><br>Left bracket. Used to denote optional expressions."
        }
        
        if (element.node.elementType == EbnfTokenTypes.RIGHT_BRACKET) {
            return "<b>]</b><br><br>Right bracket. Used to denote optional expressions."
        }
        
        if (element.node.elementType == EbnfTokenTypes.LEFT_BRACE) {
            return "<b>{</b><br><br>Left brace. Used to denote repetition (zero or more)."
        }
        
        if (element.node.elementType == EbnfTokenTypes.RIGHT_BRACE) {
            return "<b>}</b><br><br>Right brace. Used to denote repetition (zero or more)."
        }
        
        if (element.node.elementType == EbnfTokenTypes.EXCEPTION_SYMBOL) {
            return "<b>-</b><br><br>Exception symbol. Used to exclude certain patterns."
        }
        
        return null
    }
    
    private fun findCommentBeforeRule(ruleElement: PsiElement): PsiElement? {
        var prevElement = ruleElement.prevSibling
        
        // Skip whitespace
        while (prevElement != null && prevElement.node.elementType == EbnfTokenTypes.WHITE_SPACE) {
            prevElement = prevElement.prevSibling
        }
        
        // Check if the previous element is a comment
        if (prevElement != null && prevElement.node.elementType == EbnfTokenTypes.COMMENT) {
            return prevElement
        }
        
        return null
    }
}