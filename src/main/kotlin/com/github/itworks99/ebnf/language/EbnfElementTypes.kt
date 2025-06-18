package com.github.itworks99.ebnf.language

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Defines the PSI element types for EBNF language.
 * 
 * These element types represent the structure of EBNF grammar files
 * and are used by the parser to build the PSI tree.
 */
object EbnfElementTypes {
    // Note: The file element type is defined in EbnfParserDefinition.FILE

    val ID = EbnfElementType("ID")

    // Rule definition
    val RULE = EbnfElementType("RULE")
    val RULE_NAME = EbnfElementType("RULE_NAME")
    val RULE_BODY = EbnfElementType("RULE_BODY")

    // Expressions
    val EXPRESSION = EbnfElementType("EXPRESSION")
    val TERM = EbnfElementType("TERM")
    val FACTOR = EbnfElementType("FACTOR")
    val PRIMARY = EbnfElementType("PRIMARY")

    // References
    val REFERENCE = EbnfElementType("REFERENCE")

    // Groups
    val GROUP = EbnfElementType("GROUP")
    val OPTION = EbnfElementType("OPTION")
    val REPETITION = EbnfElementType("REPETITION")

    // Operators
    val ALTERNATION = EbnfElementType("ALTERNATION")
    val CONCATENATION = EbnfElementType("CONCATENATION")
    val EXCEPTION = EbnfElementType("EXCEPTION")

    // Token sets for easier handling
    val EXPRESSIONS = TokenSet.create(EXPRESSION, TERM, FACTOR, PRIMARY)
    val GROUPS = TokenSet.create(GROUP, OPTION, REPETITION)
    val OPERATORS = TokenSet.create(ALTERNATION, CONCATENATION, EXCEPTION)
}

/**
 * Represents a PSI element type in the EBNF language.
 */
class EbnfElementType(debugName: String) : IElementType(debugName, EbnfLanguage)
