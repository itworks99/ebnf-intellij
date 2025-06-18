package com.github.itworks99.ebnf.language.annotator

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Annotator for EBNF language.
 *
 * This class provides syntax and semantic validation for EBNF files, including:
 * - Validating rule references
 * - Checking for undefined rules
 * - Detecting recursive rules without base case
 * - Identifying unreachable rules
 * - Checking for ambiguous grammar constructs
 */
class EbnfAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Only process elements in EBNF files
        if (element.containingFile !is EbnfFile) {
            return
        }

        // Check for undefined rule references
        if (element.node.elementType == EbnfElementTypes.REFERENCE) {
            validateRuleReference(element, holder)
        }

        // Check for recursive rules without base case
        if (element.node.elementType == EbnfElementTypes.RULE) {
            val ruleNameElements = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.RULE_NAME }
            
            if (ruleNameElements.isNotEmpty()) {
                val ruleName = ruleNameElements.first()
                checkRecursiveRuleWithoutBaseCase(element, ruleName, holder)
            }
        }

        // Check for unreachable rules at the file level
        if (element is EbnfFile && element.firstChild != null) {
            checkUnreachableRules(element, holder)
        }

        // Check for ambiguous grammar constructs
        if (element.node.elementType == EbnfElementTypes.EXPRESSION) {
            checkAmbiguousGrammar(element, holder)
        }
    }

    /**
     * Validates a rule reference by checking if the referenced rule exists.
     */
    private fun validateRuleReference(element: PsiElement, holder: AnnotationHolder) {
        val referenceName = element.text
        val file = element.containingFile as EbnfFile

        // Find all rule name elements in the file
        val ruleNameElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE_NAME }

        // Check if there's a rule with the referenced name
        val matchingRules = ruleNameElements.filter { it.text == referenceName }

        // If no matching rules found, report an error
        if (matchingRules.isEmpty()) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                "Undefined rule: $referenceName"
            ).create()
        }
    }

    /**
     * Checks if a rule is recursive without a base case.
     * 
     * A rule is considered to have no base case if all alternatives directly or indirectly
     * reference the rule itself without any non-recursive alternatives.
     */
    private fun checkRecursiveRuleWithoutBaseCase(ruleElement: PsiElement, ruleNameElement: PsiElement, holder: AnnotationHolder) {
        val ruleName = ruleNameElement.text
        
        // Find the rule body
        val ruleBodyElements = PsiTreeUtil.findChildrenOfType(ruleElement, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE_BODY }
        
        if (ruleBodyElements.isEmpty()) {
            return
        }
        
        val ruleBody = ruleBodyElements.first()

        // Check if the rule has at least one non-recursive alternative
        val hasNonRecursiveAlternative = hasNonRecursiveAlternative(ruleBody, ruleName, mutableSetOf())
        
        if (!hasNonRecursiveAlternative) {
            holder.newAnnotation(
                HighlightSeverity.WARNING,
                "Recursive rule without base case: $ruleName"
            ).create()
        }
    }

    /**
     * Checks if an expression has at least one non-recursive alternative.
     */
    private fun hasNonRecursiveAlternative(element: PsiElement, ruleName: String, visited: MutableSet<String>): Boolean {
        // If we've already visited this rule, assume it has a base case to avoid infinite recursion
        if (visited.contains(ruleName)) {
            return true
        }
        
        visited.add(ruleName)
        
        // If this is an expression (alternation), check if any alternative is non-recursive
        if (element.node.elementType == EbnfElementTypes.EXPRESSION) {
            val terms = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.TERM }
            
            // If any term is non-recursive, the rule has a base case
            return terms.any { isNonRecursiveTerm(it, ruleName, visited) }
        }
        
        // If this is a term, check if it's non-recursive
        if (element.node.elementType == EbnfElementTypes.TERM) {
            return isNonRecursiveTerm(element, ruleName, visited)
        }
        
        // For other elements, check their children
        val children = element.children
        return children.isEmpty() || children.any { hasNonRecursiveAlternative(it, ruleName, visited) }
    }

    /**
     * Checks if a term is non-recursive (doesn't reference the rule directly or indirectly).
     */
    private fun isNonRecursiveTerm(term: PsiElement, ruleName: String, visited: MutableSet<String>): Boolean {
        // Find all references in this term
        val references = PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.REFERENCE }
        
        // If there are no references, it's non-recursive
        if (references.isEmpty()) {
            return true
        }
        
        // If any reference is to the rule itself, it's recursive
        val directlyRecursive = references.any { it.text == ruleName }
        
        // If it's not directly recursive, it's non-recursive
        if (!directlyRecursive) {
            return true
        }
        
        // If it's directly recursive, check if it has a non-recursive part
        // This is a simplified check - a more thorough analysis would be needed for complex cases
        return references.size > 1
    }

    /**
     * Checks for unreachable rules in the file.
     * 
     * A rule is considered unreachable if it's not referenced by any other rule
     * and it's not the starting rule.
     */
    private fun checkUnreachableRules(file: EbnfFile, holder: AnnotationHolder) {
        // Find all rule elements
        val ruleElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE }
        
        // Extract rule names
        val ruleNames = mutableListOf<String>()
        val ruleNameElements = mutableMapOf<String, PsiElement>()
        
        for (rule in ruleElements) {
            val nameElements = PsiTreeUtil.findChildrenOfType(rule, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.RULE_NAME }
            
            if (nameElements.isNotEmpty()) {
                val name = nameElements.first().text
                ruleNames.add(name)
                ruleNameElements[name] = nameElements.first()
            }
        }
        
        // Find all references
        val references = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.REFERENCE }
            .map { it.text }
            .toSet()
        
        // Assume the first rule is the starting rule
        val startingRule = ruleNames.firstOrNull()
        
        // Check each rule to see if it's referenced
        for (ruleName in ruleNames) {
            if (ruleName != startingRule && !references.contains(ruleName)) {
                val nameElement = ruleNameElements[ruleName]
                
                if (nameElement != null) {
                    holder.newAnnotation(
                        HighlightSeverity.WARNING,
                        "Unreachable rule: $ruleName"
                    ).range(nameElement).create()
                }
            }
        }
    }

    /**
     * Checks for ambiguous grammar constructs.
     * 
     * This is a simplified check that looks for common patterns that might indicate ambiguity.
     */
    private fun checkAmbiguousGrammar(expression: PsiElement, holder: AnnotationHolder) {
        // Find all terms (alternatives) in this expression
        val terms = PsiTreeUtil.findChildrenOfType(expression, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.TERM }
        
        // Check for alternatives that start with the same element
        val termStarts = mutableMapOf<String, MutableList<PsiElement>>()
        
        for (term in terms) {
            // Find the first primary in this term
            val primaries = PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.PRIMARY }
            
            if (primaries.isNotEmpty()) {
                val firstPrimary = primaries.first()
                
                // Find what's inside this primary (reference, string literal, etc.)
                val references = PsiTreeUtil.findChildrenOfType(firstPrimary, PsiElement::class.java)
                    .filter { it.node.elementType == EbnfElementTypes.REFERENCE }
                
                val startText = if (references.isNotEmpty()) references.first().text else firstPrimary.text
                
                termStarts.getOrPut(startText) { mutableListOf() }.add(term)
            }
        }
        
        // Report ambiguities
        for ((start, termsWithSameStart) in termStarts) {
            if (termsWithSameStart.size > 1) {
                holder.newAnnotation(
                    HighlightSeverity.WARNING,
                    "Potentially ambiguous alternatives starting with: $start"
                ).range(expression).create()
                break  // Only report once per expression
            }
        }
    }
}