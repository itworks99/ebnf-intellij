package com.github.itworks99.ebnf.language.psi.impl

import com.github.itworks99.ebnf.language.psi.EbnfPsiElement
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

/**
 * Base implementation for all PSI elements in EBNF language.
 * 
 * This class provides common functionality for all EBNF PSI elements
 * and serves as the base class for more specific element implementations.
 */
open class EbnfPsiElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), EbnfPsiElement {
    override fun toString(): String = "EbnfElement:" + node.elementType
}