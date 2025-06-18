package com.github.itworks99.ebnf.language

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.github.itworks99.ebnf.language.psi.impl.EbnfPsiElementImpl

/**
 * Parser definition for EBNF language.
 * 
 * This class connects the lexer, parser, and PSI elements for the EBNF language.
 * It defines how the source code is tokenized, parsed, and converted into a PSI tree.
 */
class EbnfParserDefinition : ParserDefinition {
    companion object {
        val FILE = IFileElementType(EbnfLanguage)
    }

    override fun createLexer(project: Project): Lexer = EbnfLexer()

    override fun createParser(project: Project): PsiParser = EbnfParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = EbnfTokenTypes.COMMENTS

    override fun getStringLiteralElements(): TokenSet = EbnfTokenTypes.STRING_LITERALS

    override fun createElement(node: ASTNode): PsiElement = EbnfPsiElementImpl(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = EbnfFile(viewProvider)

    override fun getWhitespaceTokens(): TokenSet = EbnfTokenTypes.WHITESPACES
}
