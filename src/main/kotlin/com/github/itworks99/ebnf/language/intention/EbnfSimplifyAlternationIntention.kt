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
        // Get the first factor of each term
        val firstFactors = terms.mapNotNull { term ->
            PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
                .firstOrNull { it.node.elementType == EbnfElementTypes.FACTOR }
        }
        
        val commonPrefix = firstFactors.first().text
        
        // Create the remaining parts of each term (without the common prefix)
        val remainingParts = terms.mapIndexed { index, term ->
            val factors = PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                .toList()
            
            // Skip the first factor (common prefix)
            if (factors.size <= 1) {
                // If there's only one factor, use an empty string
                ""
            } else {
                // Join the remaining factors with commas
                factors.drop(1).joinToString(", ") { it.text }
            }
        }
        
        // Create the simplified expression: common_prefix, (remaining_part_1 | remaining_part_2 | ...)
        val alternativeParts = remainingParts.filter { it.isNotEmpty() }
        val alternativeExpression = if (alternativeParts.isEmpty()) {
            // If all terms had only the common prefix, just use the prefix
            commonPrefix
        } else {
            // Create the alternation of remaining parts
            "$commonPrefix, (${alternativeParts.joinToString(" | ")})"
        }
        
        // Replace the expression with the simplified version
        replaceExpression(project, expression, alternativeExpression)
    }
    
    /**
     * Simplifies an expression with a common suffix.
     */
    private fun simplifyWithCommonSuffix(project: Project, expression: PsiElement, terms: List<PsiElement>) {
        // Get the last factor of each term
        val lastFactors = terms.mapNotNull { term ->
            val factors = PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                .toList()
            factors.lastOrNull()
        }
        
        val commonSuffix = lastFactors.first().text
        
        // Create the remaining parts of each term (without the common suffix)
        val remainingParts = terms.mapIndexed { index, term ->
            val factors = PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                .toList()
            
            // Skip the last factor (common suffix)
            if (factors.size <= 1) {
                // If there's only one factor, use an empty string
                ""
            } else {
                // Join the remaining factors with commas
                factors.dropLast(1).joinToString(", ") { it.text }
            }
        }
        
        // Create the simplified expression: (remaining_part_1 | remaining_part_2 | ...), common_suffix
        val alternativeParts = remainingParts.filter { it.isNotEmpty() }
        val alternativeExpression = if (alternativeParts.isEmpty()) {
            // If all terms had only the common suffix, just use the suffix
            commonSuffix
        } else {
            // Create the alternation of remaining parts
            "(${alternativeParts.joinToString(" | ")}), $commonSuffix"
        }
        
        // Replace the expression with the simplified version
        replaceExpression(project, expression, alternativeExpression)
    }
    
    /**
     * Replaces an expression with a new expression.
     */
    private fun replaceExpression(project: Project, expression: PsiElement, newExpressionText: String) {
        // Create a dummy file with the new expression
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.ebnf", EbnfFileType, "dummy = $newExpressionText;") as EbnfFile
        
        // Find the expression element in the dummy file
        val newExpression = PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement::class.java)
            .first { it.node.elementType == EbnfElementTypes.EXPRESSION }
        
        // Replace the old expression with the new one
        expression.replace(newExpression)
    }
}