package com.github.itworks99.ebnf.language.psi

import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.EbnfLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Represents an EBNF file in the PSI tree.
 * 
 * This class is the root of the PSI tree for EBNF files and provides
 * access to the file's content and structure.
 */
class EbnfFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, EbnfLanguage) {
    override fun getFileType(): FileType = EbnfFileType
    
    override fun toString(): String = "EBNF File"

    /**
     * Finds a rule in the file by name.
     *
     * @param ruleName the name of the rule to find
     * @return the PSI element representing the rule, or null if not found
     */
    fun findRuleByName(ruleName: String): PsiElement? {
        val rules = PsiTreeUtil.findChildrenOfType(this, PsiElement::class.java)
            .filter { it.text.startsWith("$ruleName =") }
        return rules.firstOrNull()
    }

    /**
     * Gets all rule names defined in the file.
     *
     * @return a list of rule names
     */
    fun getRuleNames(): List<String> {
        val rules = PsiTreeUtil.findChildrenOfType(this, PsiElement::class.java)
            .filter { it.text.contains("=") && !it.text.startsWith("(*") }

        return rules.mapNotNull { element ->
            val text = element.text
            val equalsIndex = text.indexOf('=')
            if (equalsIndex > 0) {
                text.substring(0, equalsIndex).trim()
            } else {
                null
            }
        }
    }
}