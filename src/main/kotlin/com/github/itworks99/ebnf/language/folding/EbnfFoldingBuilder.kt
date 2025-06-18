package com.github.itworks99.ebnf.language.folding

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfTokenTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.tree.TokenSet

/**
 * Provides code folding for EBNF language.
 *
 * This class implements folding for rule definitions and comment blocks in EBNF files.
 */
class EbnfFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        // Add folding for rule definitions
        val ruleElements = PsiTreeUtil.findChildrenOfType(root, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE }

        for (rule in ruleElements) {
            // Find the rule body
            val ruleBody = rule.node.findChildByType(EbnfElementTypes.RULE_BODY)
            if (ruleBody != null) {
                // Create a folding region for the rule body
                descriptors.add(
                    FoldingDescriptor(
                        rule.node,
                        TextRange(rule.textRange.startOffset, rule.textRange.endOffset),
                        null
                    )
                )
            }
        }

        // Add folding for comment blocks
        val commentElements = PsiTreeUtil.findChildrenOfType(root, PsiElement::class.java)
            .filter { it.node.elementType == EbnfTokenTypes.COMMENT }

        for (comment in commentElements) {
            // Only fold comments that span multiple lines
            val commentText = comment.text
            if (commentText.contains("\n")) {
                descriptors.add(
                    FoldingDescriptor(
                        comment.node,
                        comment.textRange,
                        null
                    )
                )
            }
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        return when (node.elementType) {
            EbnfElementTypes.RULE -> {
                // Get the rule name
                val ruleName = node.findChildByType(EbnfElementTypes.RULE_NAME)?.text ?: "rule"
                "$ruleName = ... ;"
            }
            EbnfTokenTypes.COMMENT -> "(* ... *)"
            else -> null
        }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        // Collapse comments by default, but not rules
        return node.elementType == EbnfTokenTypes.COMMENT
    }
}
