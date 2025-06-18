package com.github.itworks99.ebnf.language.inspection

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil

/**
 * Inspection for redundant parentheses in EBNF grammar.
 *
 * This inspection detects and provides a quick fix for unnecessary parentheses
 * around expressions that don't need grouping.
 */
class EbnfRedundantParenthesesInspection : LocalInspectionTool() {
    override fun getDisplayName(): String = "Redundant parentheses"

    override fun getGroupDisplayName(): String = "EBNF"

    override fun getShortName(): String = "EbnfRedundantParentheses"

    override fun getStaticDescription(): String = 
        "Detects and provides a quick fix for unnecessary parentheses around expressions that don't need grouping."
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.node.elementType == EbnfElementTypes.GROUP) {
                    // Check if this group is unnecessary
                    if (isRedundantGroup(element)) {
                        holder.registerProblem(
                            element,
                            "Redundant parentheses",
                            RemoveRedundantParenthesesFix()
                        )
                    }
                }
                super.visitElement(element)
            }
        }
    }

    /**
     * Determines if a group (parentheses) is redundant.
     *
     * A group is considered redundant if:
     * 1. It contains exactly one expression
     * 2. The expression contains exactly one term
     * 3. The group is not part of a concatenation or exception
     */
    private fun isRedundantGroup(group: PsiElement): Boolean {
        // Find expressions inside the group
        val expressions = PsiTreeUtil.findChildrenOfType(group, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.EXPRESSION }

        // If there's not exactly one expression, the group is necessary
        if (expressions.size != 1) {
            return false
        }

        val expression = expressions.first()

        // Find terms inside the expression
        val terms = PsiTreeUtil.findChildrenOfType(expression, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.TERM }

        // If there's not exactly one term, the group is necessary (it's an alternation)
        if (terms.size != 1) {
            return false
        }

        // Check if the group is part of a concatenation
        val parent = group.parent
        if (parent != null && parent.node.elementType == EbnfElementTypes.TERM) {
            // If the term has multiple factors, the group is necessary
            val factors = PsiTreeUtil.findChildrenOfType(parent, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.FACTOR }

            if (factors.size > 1) {
                return false
            }
        }

        // If we got here, the group is redundant
        return true
    }

    /**
     * Quick fix to remove redundant parentheses.
     */
    private class RemoveRedundantParenthesesFix : LocalQuickFix {
        override fun getName(): String = "Remove redundant parentheses"

        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val group = descriptor.psiElement

            // Find the expression inside the group
            val expressions = PsiTreeUtil.findChildrenOfType(group, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.EXPRESSION }

            if (expressions.isEmpty()) {
                return
            }

            val expression = expressions.first()

            // Replace the group with the expression
            group.replace(expression)
        }
    }
}
