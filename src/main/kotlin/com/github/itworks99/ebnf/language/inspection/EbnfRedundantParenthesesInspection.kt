package com.github.itworks99.ebnf.language.inspection

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfFileType
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
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
                if (element.node.elementType == EbnfElementTypes.FACTOR) {
                    // Check if this is a group factor (has parentheses)
                    val text = element.text
                    if (text.startsWith("(") && text.endsWith(")")) {
                        // Check if this group is unnecessary
                        if (isRedundantGroup(element)) {
                            holder.registerProblem(
                                element,
                                "Redundant parentheses",
                                RemoveRedundantParenthesesFix()
                            )
                        }
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
     * 2. The expression is not a complex expression (e.g., not an alternation)
     * 3. It's not required for precedence
     */
    private fun isRedundantGroup(element: PsiElement): Boolean {
        // Get the content inside the parentheses
        val text = element.text
        
        // Skip if not a proper group
        if (!text.startsWith("(") || !text.endsWith(")")) {
            return false
        }
        
        // Check the content inside
        val innerContent = text.substring(1, text.length - 1).trim()
        
        // Check if this is a simple expression (not containing alternation)
        return !innerContent.contains('|') && 
               element.parent?.node?.elementType != EbnfElementTypes.REPETITION &&
               element.parent?.node?.elementType != EbnfElementTypes.OPTION
    }

    /**
     * Quick fix for removing redundant parentheses.
     */
    inner class RemoveRedundantParenthesesFix : LocalQuickFix {
        override fun getFamilyName(): String = "Remove redundant parentheses"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            
            // Get the content inside the parentheses
            val text = element.text
            if (!text.startsWith("(") || !text.endsWith(")")) return
            val innerContent = text.substring(1, text.length - 1)
            
            // Create a new element without the parentheses
            val psiFileFactory = PsiFileFactory.getInstance(project)
            
            // Create a dummy file to parse the expression without parentheses
            val dummyFile = psiFileFactory.createFileFromText(
                "dummy.ebnf",
                EbnfFileType,
                "dummy = $innerContent;"
            )
            
            // Find the expression in the dummy file
            val innerExpression = PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement::class.java)
                .firstOrNull { it.node.elementType == EbnfElementTypes.EXPRESSION }
                ?.firstChild
                
            // Replace the original element with the inner expression
            if (innerExpression != null) {
                element.replace(innerExpression)
            }
        }
    }
}
