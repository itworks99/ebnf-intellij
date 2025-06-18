package com.github.itworks99.ebnf.language.psi

import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.EbnfLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

/**
 * Represents an EBNF file in the PSI tree.
 * 
 * This class is the root of the PSI tree for EBNF files and provides
 * access to the file's content and structure.
 */
class EbnfFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, EbnfLanguage) {
    override fun getFileType(): FileType = EbnfFileType
    
    override fun toString(): String = "EBNF File"
}