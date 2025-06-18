package com.github.itworks99.ebnf.language.inspection

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.psi.EbnfFile

/**
 * Inspection for repetitive patterns in EBNF grammar.
 *
 * This inspection detects repetitive patterns like [a, a, a] and suggests
 * replacing them with more concise forms like {a}, a.
 */
class EbnfSimplifyRepetitionInspection : LocalInspectionTool() {
    override fun getDisplayName(): String = "Simplify repetition"
    
    override fun getGroupDisplayName(): String = "EBNF"
    
    override fun getShortName(): String = "EbnfSimplifyRepetition"
    
    override fun getStaticDescription(): String = 
        "Detects repetitive patterns and suggests replacing them with more concise forms using repetition operators."
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                // Check for terms with multiple identical factors
                if (element.node.elementType == EbnfElementTypes.TERM) {
                    checkForRepetitiveFactors(element, holder)
                }
                
                super.visitElement(element)
            }
        }
    }
    
    /**
     * Checks for repetitive factors in a term.
     */
    private fun checkForRepetitiveFactors(term: PsiElement, holder: ProblemsHolder) {
        // Find all factors in the term
        val factors = PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.FACTOR }
            .toList()
        
        // If there are at least 2 factors, check for repetition
        if (factors.size >= 2) {
            // Group factors by their text representation
            val factorGroups = factors.groupBy { it.text }
            
            // Find groups with multiple identical factors
            val repetitiveGroups = factorGroups.filter { it.value.size >= 2 }
            
            if (repetitiveGroups.isNotEmpty()) {
                // For simplicity, just handle the first repetitive group
                val (factorText, repetitiveFactors) = repetitiveGroups.entries.first()
                
                // If there are at least 2 identical factors, suggest simplification
                if (repetitiveFactors.size >= 2) {
                    holder.registerProblem(
                        term,
                        "Repetitive pattern can be simplified using repetition operator",
                        SimplifyRepetitionFix(factorText, repetitiveFactors.size)
                    )
                }
            }
        }
    }
    
    /**
     * Quick fix to simplify repetitive patterns.
     */
    private class SimplifyRepetitionFix(
        private val factorText: String,
        private val repetitionCount: Int
    ) : LocalQuickFix {
        override fun getName(): String = "Simplify repetition using repetition operator"
        
        override fun getFamilyName(): String = name
        
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val term = descriptor.psiElement
            
            // Create a simplified version of the term
            val simplified = when (repetitionCount) {
                2 -> "$factorText, $factorText" // Just two repetitions
                else -> "{$factorText}, $factorText" // Three or more repetitions
            }
            
            // Create a new term element
            val dummyFile = PsiFileFactory.getInstance(project)
                .createFileFromText("dummy.ebnf", EbnfFileType, "dummy = $simplified;") as EbnfFile
            
            // Find the term in the dummy file
            val newTerm = PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement::class.java)
                .firstOrNull { it.node.elementType == EbnfElementTypes.TERM }
            
            // Replace the old term with the new one
            if (newTerm != null) {
                term.replace(newTerm)
            }
        }
    }
}