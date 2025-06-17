package com.github.itworks99.ebnf.language

import com.intellij.lang.Language

/**
 * Defines the EBNF language for IntelliJ IDEA.
 * 
 * Extended Backus-Naur Form (EBNF) is a family of metasyntax notations used to express context-free grammars.
 */
object EbnfLanguage : Language("EBNF") {
    override fun getDisplayName(): String = "EBNF"
    override fun getMimeTypes(): Array<String> = arrayOf("text/ebnf")
    
    override fun toString(): String = displayName
}