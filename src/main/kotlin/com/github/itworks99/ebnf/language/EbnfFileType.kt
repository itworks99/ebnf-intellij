package com.github.itworks99.ebnf.language

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * Defines the EBNF file type for IntelliJ IDEA.
 * 
 * This file type is associated with the .ebnf extension and provides
 * basic information about EBNF files.
 */
object EbnfFileType : LanguageFileType(EbnfLanguage) {
    override fun getName(): String = "EBNF"
    override fun getDescription(): String = "Extended Backus-Naur Form file"
    override fun getDefaultExtension(): String = "ebnf"
    
    // TODO: Replace with a custom icon for EBNF files
    override fun getIcon(): Icon? = null
}