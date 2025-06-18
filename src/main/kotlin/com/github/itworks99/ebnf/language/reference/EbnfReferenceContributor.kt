package com.github.itworks99.ebnf.language.reference

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfLanguage
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

/**
 * Contributes references to the PSI tree for EBNF language.
 *
 * This class registers reference providers for rule references in EBNF files.
 */
class EbnfReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register reference provider for rule references
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement()
                .withElementType(EbnfElementTypes.REFERENCE)
                .withLanguage(EbnfLanguage),
            EbnfReferenceProvider()
        )
    }
}