package com.github.itworks99.ebnf.language.structure

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Element in the structure view tree for EBNF files.
 */
class EbnfStructureViewElement(private val element: PsiElement) : 
    StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = element

    override fun navigate(requestFocus: Boolean) {
        if (element is NavigatablePsiElement) {
            element.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean = element is NavigatablePsiElement && (element as NavigatablePsiElement).canNavigate()

    override fun canNavigateToSource(): Boolean = canNavigate()

    override fun getAlphaSortKey(): String = element.text

    override fun getPresentation(): ItemPresentation {
        if (element is EbnfFile) {
            return element.presentation ?: PresentationData("EBNF Grammar", null, null, null)
        }
        
        // For rule elements, show the rule name
        if (element.node.elementType == EbnfElementTypes.RULE) {
            val ruleName = element.node.findChildByType(EbnfElementTypes.RULE_NAME)?.text ?: "Unnamed Rule"
            return PresentationData(ruleName, null, null, null)
        }
        
        return PresentationData(element.text, null, null, null)
    }

    override fun getChildren(): Array<TreeElement> {
        if (element !is EbnfFile) {
            return emptyArray()
        }
        
        // Find all rule elements in the file
        val ruleElements = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE }
            .toList()
        
        // Create structure view elements for each rule
        return ruleElements.map { EbnfStructureViewElement(it) }.toTypedArray()
    }
}