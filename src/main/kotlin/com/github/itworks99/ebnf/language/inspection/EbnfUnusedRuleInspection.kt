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
                    
                    // Make sure we're looking at a rule definition, not a reference
                    val parent = element.parent
                    if (parent == null || parent.node.elementType != EbnfElementTypes.RULE) {
                        return
                    }

                    // Get the rule name
                    val ruleName = element.text
                    
                    // Skip the first rule (often the main/starting rule)
                    val allRules = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
                        .filter { it.node.elementType == EbnfElementTypes.RULE }
                        .toList()

                    if (allRules.isNotEmpty() && allRules.first() == parent) {
                        return
                    }

                    // Find all references in the file
                    val references = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
                        .filter { it.node.elementType == EbnfElementTypes.ID }
                        .map { it.text }
                        .toSet()
                    
                    // Check if the rule is unused
                    if (ruleName !in references) {
                        holder.registerProblem(
                            element,
                            "Unused rule: $ruleName",
                            RemoveUnusedRuleFix()
                        )
                    }
                }
                super.visitElement(element)
            }
        }
    }
    
    /**
     * Quick fix for removing unused rules.
     */
    inner class RemoveUnusedRuleFix : LocalQuickFix {
        override fun getFamilyName(): String = "Remove unused rule"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            
            // Get the parent rule element
            val rule = element.parent
            if (rule != null && rule.node.elementType == EbnfElementTypes.RULE) {
                // Remove the entire rule
                rule.delete()
            }
        }
    }
}