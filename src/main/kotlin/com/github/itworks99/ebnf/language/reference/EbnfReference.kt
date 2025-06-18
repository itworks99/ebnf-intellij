package com.github.itworks99.ebnf.language.reference

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil

/**
 * Represents a reference to a rule in EBNF language.
 *
 * This class handles reference resolution, find usages, and rename refactoring for rule references.
 */
class EbnfReference(element: PsiElement) : 
    PsiReferenceBase<PsiElement>(element), PsiPolyVariantReference {

    private val referenceName: String = element.text

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val file = element.containingFile as? EbnfFile ?: return emptyArray()

        // Find all rule name elements in the file
        val ruleNameElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE_NAME }

        // Find rule names that match the reference name
        val matchingRules = ruleNameElements.filter { it.text == referenceName }

        // Create resolve results for each matching rule
        return matchingRules.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        val file = element.containingFile as? EbnfFile ?: return emptyArray()

        // Find all rule name elements in the file
        val ruleNameElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE_NAME }

        // Return rule names as variants
        return ruleNameElements.map { it.text }.toTypedArray()
    }

    override fun getRangeInElement(): TextRange {
        return TextRange(0, element.textLength)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.replace(createRuleReference(newElementName))
    }

    private fun createRuleReference(name: String): PsiElement {
        // Create a new reference element with the given name
        // This is a simplified implementation; in a real plugin, you would use a factory method
        val dummyFile = PsiFileFactory.getInstance(element.project)
            .createFileFromText("dummy.ebnf", EbnfFileType, name) as EbnfFile
        return PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement::class.java)
            .first { it.node.elementType == EbnfElementTypes.REFERENCE }
    }
}
