package com.github.itworks99.ebnf.language

import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Factory for creating EBNF syntax highlighters.
 * 
 * This class is registered in plugin.xml to provide syntax highlighting for EBNF files.
 */
class EbnfSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return EbnfSyntaxHighlighter()
    }
}