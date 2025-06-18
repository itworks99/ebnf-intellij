package com.github.itworks99.ebnf.language.inspection

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil

/**
 * Inspection for unused rules in EBNF grammar.
 *
 * This inspection detects rules that are defined but never referenced
 * and provides a quick fix to remove them.
 */
class EbnfUnusedRuleInspection : LocalInspectionTool() {
    override fun getDisplayName(): String = "Unused rule"
    
    override fun getGroupDisplayName(): String = "EBNF"
    
    override fun getShortName(): String = "EbnfUnusedRule"
    
    override fun getStaticDescription(): String = 
        "Detects rules that are defined but never referenced and provides a quick fix to remove them."
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                // Only check rule name elements
                if (element.node.elementType == EbnfElementTypes.RULE_NAME) {
                    val file = element.containingFile as? EbnfFile ?: return
                    
                    // Get the rule name
                    val ruleName = element.text
                    
                    // Find all references in the file
                    val references = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
                        .filter { it.node.elementType == EbnfElementTypes.REFERENCE }
                        .map { it.text }
                        .toSet()
                    
                    // Find all rule names in the file
                    val ruleNames = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
                        .filter { it.node.elementType == EbnfElementTypes.RULE_NAME }
                        .map { it.text }
                        .toList()
                    
                    // Assume the first rule is the starting rule
                    val startingRule = ruleNames.firstOrNull()
                    
                    // If this rule is not the starting rule and not referenced, it's unused
                    if (ruleName != startingRule && !references.contains(ruleName)) {
                        // Find the containing rule element
                        val rule = findContainingRule(element)
                        
                        if (rule != null) {
                            holder.registerProblem(
                                element,
                                "Unused rule: $ruleName",
                                RemoveUnusedRuleFix()
                            )
                        }
                    }
                }
                super.visitElement(element)
            }
        }
    }
    
    /**
     * Finds the rule element containing the given element.
     */
    private fun findContainingRule(element: PsiElement): PsiElement? {
        var current: PsiElement? = element
        
        while (current != null) {
            if (current.node.elementType == EbnfElementTypes.RULE) {
                return current
            }
            current = current.parent
        }
        
        return null
    }
    
    /**
     * Quick fix to remove an unused rule.
     */
    private class RemoveUnusedRuleFix : LocalQuickFix {
        override fun getName(): String = "Remove unused rule"
        
        override fun getFamilyName(): String = name
        
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            
            // Find the containing rule
            val rule = findContainingRule(element)
            
            // Remove the rule
            rule?.delete()
        }
        
        /**
         * Finds the rule element containing the given element.
         */
        private fun findContainingRule(element: PsiElement): PsiElement? {
            var current: PsiElement? = element
            
            while (current != null) {
                if (current.node.elementType == EbnfElementTypes.RULE) {
                    return current
                }
                current = current.parent
            }
            
            return null
        }
    }
}