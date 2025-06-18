package com.github.itworks99.ebnf.language.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

/**
 * Provides references for PSI elements in EBNF language.
 *
 * This class creates references for rule references in EBNF files.
 */
class EbnfReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        // Create a reference for the element
        return arrayOf(EbnfReference(element))
    }
}