package com.github.itworks99.ebnf.language.tree

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.intellij.psi.PsiElement
import com.intellij.ui.treeStructure.SimpleNode
import javax.swing.Icon
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange

/**
 * Represents a node in the EBNF syntax tree viewer.
 *
 * This class is used to build a tree representation of the EBNF PSI structure
 * for display in the syntax tree viewer tool window.
 */
class EbnfSyntaxTreeNode(
    private val project: Project,
    private val psiElement: PsiElement,
    parent: EbnfSyntaxTreeNode?
) : SimpleNode(parent) {
    
    private val elementType = psiElement.node.elementType
    private val elementText = psiElement.text
    private val textRange = psiElement.textRange
    
    init {
        // Set the icon based on the element type
        val icon = when (elementType) {
            EbnfElementTypes.RULE -> AllIcons.Nodes.Method
            EbnfElementTypes.RULE_NAME -> AllIcons.Nodes.Field
            EbnfElementTypes.REFERENCE -> AllIcons.Nodes.MethodReference
            EbnfElementTypes.EXPRESSION -> AllIcons.Nodes.Lambda
            EbnfElementTypes.TERM -> AllIcons.Nodes.Parameter
            EbnfElementTypes.FACTOR -> AllIcons.Nodes.Variable
            EbnfElementTypes.PRIMARY -> AllIcons.Nodes.Variable
            EbnfElementTypes.GROUP -> AllIcons.Nodes.Folder
            EbnfElementTypes.OPTION -> AllIcons.Nodes.Parameter
            EbnfElementTypes.REPETITION -> AllIcons.Nodes.Parameter
            else -> AllIcons.Nodes.Tag
        }
        
        // Set the icon in the presentation
        presentation.setIcon(icon)
    }
    
    override fun getChildren(): Array<SimpleNode> {
        // Get all child PSI elements
        val children = psiElement.children
        
        // Convert each child to a tree node
        return children.map { EbnfSyntaxTreeNode(project, it, this) }.toTypedArray()
    }
    
    override fun getName(): String {
        // For rule names, show the name
        if (elementType == EbnfElementTypes.RULE_NAME) {
            return "RULE_NAME: $elementText"
        }
        
        // For references, show the reference name
        if (elementType == EbnfElementTypes.REFERENCE) {
            return "REFERENCE: $elementText"
        }
        
        // For string literals, show the string
        if (elementText.startsWith("\"") && elementText.endsWith("\"")) {
            return "STRING: $elementText"
        }
        
        // For other elements, show the element type and a preview of the text
        val textPreview = if (elementText.length > 30) {
            elementText.substring(0, 27) + "..."
        } else {
            elementText
        }
        
        return "$elementType: $textPreview"
    }
    
    /**
     * Navigates to this node's corresponding source code in the editor.
     */
    fun navigate() {
        // Get the editor for the file
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        
        // Move the caret to the start of this element
        editor.caretModel.moveToOffset(textRange.startOffset)
        
        // Scroll to make the element visible
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        
        // Select the text range of this element
        editor.selectionModel.setSelection(textRange.startOffset, textRange.endOffset)
    }
    
    /**
     * Checks if this node corresponds to the element at the given offset.
     */
    fun containsOffset(offset: Int): Boolean {
        return textRange.containsOffset(offset)
    }
    
    /**
     * Finds the node that corresponds to the element at the given offset.
     */
    fun findNodeAtOffset(offset: Int): EbnfSyntaxTreeNode? {
        // Check if this node contains the offset
        if (!containsOffset(offset)) {
            return null
        }
        
        // Check if any child contains the offset
        for (child in children) {
            val childNode = child as? EbnfSyntaxTreeNode ?: continue
            val foundNode = childNode.findNodeAtOffset(offset)
            if (foundNode != null) {
                return foundNode
            }
        }
        
        // If no child contains the offset, return this node
        return this
    }
    
    /**
     * Gets the text range of this node.
     */
    fun getTextRange(): TextRange {
        return textRange
    }
}