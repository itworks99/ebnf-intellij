package com.github.itworks99.ebnf.language.intention

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

/**
 * Intention action to simplify alternations with common prefixes or suffixes.
 *
 * This intention converts expressions like "a, b | a, c" to "a, (b | c)"
 * or "b, a | c, a" to "(b | c), a", which can improve readability and
 * reduce redundancy in the grammar.
 */
class EbnfSimplifyAlternationIntention : PsiElementBaseIntentionAction(), IntentionAction {
    override fun getText(): String = "Simplify alternation"
    
    override fun getFamilyName(): String = "EBNF"
    
    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        // Check if the element is an expression with alternation
        if (element.node.elementType != EbnfElementTypes.EXPRESSION) {
            return false
        }
        
        // Find all terms in the expression
        val terms = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.TERM }
            .toList()
        
        // We need at least two terms for alternation
        if (terms.size < 2) {
            return false
        }
        
        // Check if there's a common prefix or suffix
        return hasCommonPrefix(terms) || hasCommonSuffix(terms)
    }
    
    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        // Find all terms in the expression
        val terms = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.TERM }
            .toList()
        
        // Check if there's a common prefix or suffix
        if (hasCommonPrefix(terms)) {
            simplifyWithCommonPrefix(project, element, terms)
        } else if (hasCommonSuffix(terms)) {
            simplifyWithCommonSuffix(project, element, terms)
        }
    }
    
    /**
     * Checks if the terms have a common prefix.
     */
    private fun hasCommonPrefix(terms: List<PsiElement>): Boolean {
        if (terms.size < 2) {
            return false
        }
        
        // Get the first factor of each term
        val firstFactors = terms.mapNotNull { term ->
            PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
                .firstOrNull { it.node.elementType == EbnfElementTypes.FACTOR }
        }
        
        // If any term doesn't have a factor, there's no common prefix
        if (firstFactors.size != terms.size) {
            return false
        }
        
        // Check if all first factors have the same text
        val firstFactorText = firstFactors.first().text
        return firstFactors.all { it.text == firstFactorText }
    }
    
    /**
     * Checks if the terms have a common suffix.
     */
    private fun hasCommonSuffix(terms: List<PsiElement>): Boolean {
        if (terms.size < 2) {
            return false
        }
        
        // Get the last factor of each term
        val lastFactors = terms.mapNotNull { term ->
            val factors = PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                .toList()
            factors.lastOrNull()
        }
        
        // If any term doesn't have a factor, there's no common suffix
        if (lastFactors.size != terms.size) {
            return false
        }
        
        // Check if all last factors have the same text
        val lastFactorText = lastFactors.first().text
        return lastFactors.all { it.text == lastFactorText }
    }
    
    /**
     * Simplifies an expression with a common prefix.
     */
    private fun simplifyWithCommonPrefix(project: Project, expression: PsiElement, terms: List<PsiElement>) {
        // Get the common prefix (first factor of each term)
        val commonPrefix = PsiTreeUtil.findChildrenOfType(terms.first(), PsiElement::class.java)
            .first { it.node.elementType == EbnfElementTypes.FACTOR }

        // Create the parts without the prefix
        val remainingParts = terms.map { term ->
            val factors = PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                .toList()
            
            // Skip the first factor (the common prefix)
            val remainingFactors = factors.drop(1)

            if (remainingFactors.isEmpty()) {
                // If there are no remaining factors, use empty string
                ""
            } else {
                // Otherwise, join them with commas
                remainingFactors.joinToString(", ") { it.text }
            }
        }

        // Create the new expression text: "prefix, (part1 | part2 | ...)"
        val nonEmptyParts = remainingParts.filter { it.isNotEmpty() }
        val innerExpression = if (nonEmptyParts.isEmpty()) {
            // If all parts are empty, don't add parentheses
            ""
        } else {
            // Otherwise, create an alternation of the remaining parts
            "(${nonEmptyParts.joinToString(" | ")})"
        }

        // Create the simplified expression
        val newExpressionText = if (innerExpression.isEmpty()) {
            commonPrefix.text
        } else {
            "${commonPrefix.text}, $innerExpression"
        }
        
        // Replace the original expression with the simplified one
        replaceExpression(project, expression, newExpressionText)
    }
    
    /**
     * Simplifies an expression with a common suffix.
     */
    private fun simplifyWithCommonSuffix(project: Project, expression: PsiElement, terms: List<PsiElement>) {
        // Get the common suffix (last factor of each term)
        val commonSuffix = run {
            val factors = PsiTreeUtil.findChildrenOfType(terms.first(), PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                .toList()
            factors.last()
        }
        
        // Create the parts without the suffix
        val remainingParts = terms.map { term ->
            val factors = PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                .toList()
            
            // Skip the last factor (the common suffix)
            val remainingFactors = factors.dropLast(1)

            if (remainingFactors.isEmpty()) {
                // If there are no remaining factors, use empty string
                ""
            } else {
                // Otherwise, join them with commas
                remainingFactors.joinToString(", ") { it.text }
            }
        }
        
        // Create the new expression text: "(part1 | part2 | ...), suffix"
        val nonEmptyParts = remainingParts.filter { it.isNotEmpty() }
        val innerExpression = if (nonEmptyParts.isEmpty()) {
            // If all parts are empty, don't add parentheses
            ""
        } else {
            // Otherwise, create an alternation of the remaining parts
            "(${nonEmptyParts.joinToString(" | ")})"
        }

        // Create the simplified expression
        val newExpressionText = if (innerExpression.isEmpty()) {
            commonSuffix.text
        } else {
            "$innerExpression, ${commonSuffix.text}"
        }
        
        // Replace the original expression with the simplified one
        replaceExpression(project, expression, newExpressionText)
    }
    
    /**
     * Replaces the expression with a new one.
     */
    private fun replaceExpression(project: Project, expression: PsiElement, newText: String) {
        // Create a dummy file with the new expression
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.ebnf", EbnfFileType, "dummy = $newText;") as EbnfFile

        // Find the expression element in the dummy file
        val newExpression = PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement::class.java)
            .first { it.node.elementType == EbnfElementTypes.EXPRESSION }
        
        // Replace the original expression with the new one
        expression.replace(newExpression)
    }
}