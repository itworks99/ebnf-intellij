package com.github.itworks99.ebnf.language.structure

import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Model for the structure view of EBNF files.
 */
class EbnfStructureViewModel(psiFile: PsiFile, editor: Editor?) : 
    StructureViewModelBase(psiFile, editor, EbnfStructureViewElement(psiFile)),
    StructureViewModel.ElementInfoProvider {

    override fun getSorters(): Array<Sorter> = arrayOf(Sorter.ALPHA_SORTER)

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean {
        val value = element.value
        return value !is EbnfFile
    }
}