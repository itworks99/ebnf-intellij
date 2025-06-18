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
        
        // We need at least 2 factors to detect repetition
        if (factors.size < 2) return

        // Group factors by their text representation
        val factorGroups = factors.groupBy { it.text }

        // Look for repeated factors with at least 2 repetitions
        factorGroups.forEach { (factorText, factorsOfSameType) ->
            if (factorsOfSameType.size >= 2) {
                // Check if they are consecutive
                if (areConsecutive(factorsOfSameType, factors)) {
                    // Register the problem
                    holder.registerProblem(
                        term,
                        "Repetitive pattern can be simplified using a repetition operator",
                        SimplifyRepetitionFix(factorText, factorsOfSameType.size)
                    )
                }
            }
        }
    }
    
    /**
     * Checks if a list of factors are consecutive in another list.
     */
    private fun areConsecutive(subList: List<PsiElement>, fullList: List<PsiElement>): Boolean {
        // If there's only one occurrence, it's not repetition
        if (subList.size <= 1) return false

        // Get the indices of the factors in the full list
        val indices = subList.map { fullList.indexOf(it) }.sorted()

        // Check if the indices form a continuous sequence
        return indices.zipWithNext().all { (a, b) -> b - a == 1 }
    }

    /**
     * Quick fix for simplifying repetitive patterns.
     */
    inner class SimplifyRepetitionFix(
        private val factorText: String,
        private val repetitionCount: Int
    ) : LocalQuickFix {
        override fun getFamilyName(): String = "Simplify repetition"

        override fun getName(): String = if (repetitionCount > 1) {
            "Replace with '{$factorText}'"
        } else {
            "Replace with repetition"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val term = descriptor.psiElement
            
            // Create the simplified expression using repetition notation
            val simplifiedText = "{$factorText}"

            // Create a new element with the simplified expression
            val psiFileFactory = PsiFileFactory.getInstance(project)

            // Create a dummy file to parse the simplified expression
            val dummyFile = psiFileFactory.createFileFromText(
                "dummy.ebnf",
                EbnfFileType,
                "dummy = $simplifiedText;"
            )

            // Find the factor in the dummy file
            val newFactor = PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement::class.java)
                .firstOrNull { it.node.elementType == EbnfElementTypes.FACTOR }

            // Replace the original term with the new simplified factor
            if (newFactor != null) {
                term.replace(newFactor)
            }
        }
    }
}