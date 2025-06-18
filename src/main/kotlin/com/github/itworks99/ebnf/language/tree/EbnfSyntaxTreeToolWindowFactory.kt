package com.github.itworks99.ebnf.language.tree

import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiManager
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.ui.treeStructure.SimpleTreeStructure
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

/**
 * Factory for creating the Syntax Tree tool window.
 *
 * This tool window displays the PSI structure of EBNF files as a tree,
 * allowing navigation between the tree and source code.
 */
class EbnfSyntaxTreeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(
            SyntaxTreePanel(project),
            "Syntax Tree",
            false
        )
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    /**
     * Panel that displays the syntax tree for EBNF files.
     */
    private class SyntaxTreePanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
        private val tree = SimpleTree()
        private val rootNode = DefaultMutableTreeNode("No EBNF file open")
        private val treeModel = DefaultTreeModel(rootNode)
        private val messageLabel = JLabel("No EBNF file open")
        private var rootTreeNode: EbnfSyntaxTreeNode? = null
        private var currentFile: VirtualFile? = null
        private var caretListener: CaretListener? = null

        init {
            // Set up the tree
            tree.model = treeModel
            tree.isRootVisible = true
            tree.showsRootHandles = true
            tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

            // Add tree selection listener
            tree.addTreeSelectionListener(object : TreeSelectionListener {
                override fun valueChanged(e: TreeSelectionEvent) {
                    val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
                    val userObject = node.userObject

                    if (userObject is EbnfSyntaxTreeNode) {
                        userObject.navigate()
                    }
                }
            })

            // Set up the panel
            val scrollPane = ScrollPaneFactory.createScrollPane(tree)
            scrollPane.border = JBUI.Borders.empty()

            add(messageLabel, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)

            // Listen for file editor changes
            project.messageBus.connect(this).subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                object : FileEditorManagerListener {
                    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                        if (file.fileType == EbnfFileType) {
                            updateTree(file)
                        }
                    }

                    override fun selectionChanged(event: FileEditorManagerEvent) {
                        val file = event.newFile
                        if (file?.fileType == EbnfFileType) {
                            updateTree(file)
                        } else {
                            clearTree()
                        }
                    }
                }
            )

            // Initial update
            val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
            if (currentFile?.fileType == EbnfFileType) {
                updateTree(currentFile)
            }
        }

        /**
         * Updates the tree based on the current file.
         */
        private fun updateTree(file: VirtualFile) {
            // Remove previous caret listener
            removeCaretListener()

            // Get the PSI file
            val psiFile = PsiManager.getInstance(project).findFile(file) as? EbnfFile

            if (psiFile != null) {
                // Create the root node
                rootTreeNode = EbnfSyntaxTreeNode(project, psiFile, null)

                // Create a tree structure
                val treeStructure = object : SimpleTreeStructure() {
                    override fun getRootElement() = rootTreeNode!!
                }

                // Update the tree model
                val model = DefaultTreeModel(createNode(rootTreeNode!!))
                tree.model = model

                // Expand the root node
                tree.expandRow(0)

                // Update the message
                messageLabel.text = "Syntax tree for ${file.name}"

                // Add caret listener
                addCaretListener(file)

                // Save the current file
                currentFile = file
            } else {
                clearTree()
            }
        }

        /**
         * Clears the tree.
         */
        private fun clearTree() {
            // Remove caret listener
            removeCaretListener()

            // Clear the tree
            rootNode.removeAllChildren()
            rootNode.userObject = "No EBNF file open"
            treeModel.reload()

            // Update the message
            messageLabel.text = "No EBNF file open"

            // Clear the current file
            currentFile = null
            rootTreeNode = null
        }

        /**
         * Creates a tree node for the given syntax tree node.
         */
        private fun createNode(syntaxTreeNode: EbnfSyntaxTreeNode): DefaultMutableTreeNode {
            val node = DefaultMutableTreeNode(syntaxTreeNode)

            for (child in syntaxTreeNode.children) {
                node.add(createNode(child as EbnfSyntaxTreeNode))
            }

            return node
        }

        /**
         * Adds a caret listener to the editor for the given file.
         */
        private fun addCaretListener(file: VirtualFile) {
            val editor = FileEditorManager.getInstance(project).getSelectedTextEditor() ?: return

            caretListener = object : CaretListener {
                override fun caretPositionChanged(event: CaretEvent) {
                    val offset = editor.caretModel.offset

                    // Find the node at the caret position
                    val node = rootTreeNode?.findNodeAtOffset(offset)

                    if (node != null) {
                        // Find the tree path for the node
                        val path = findTreePath(node)

                        if (path != null) {
                            // Select the node in the tree
                            tree.selectionPath = path

                            // Ensure the node is visible
                            tree.scrollPathToVisible(path)
                        }
                    }
                }
            }

            editor.caretModel.addCaretListener(caretListener!!)
        }

        /**
         * Removes the caret listener.
         */
        private fun removeCaretListener() {
            if (caretListener != null) {
                val editor = FileEditorManager.getInstance(project).getSelectedTextEditor()
                editor?.caretModel?.removeCaretListener(caretListener!!)
                caretListener = null
            }
        }

        /**
         * Finds the tree path for the given syntax tree node.
         */
        private fun findTreePath(syntaxTreeNode: EbnfSyntaxTreeNode): javax.swing.tree.TreePath? {
            // Get the root node
            val root = tree.model.root as DefaultMutableTreeNode

            // Find the node in the tree
            return findNodeInTree(root, syntaxTreeNode)
        }

        /**
         * Recursively finds a node in the tree.
         */
        private fun findNodeInTree(treeNode: DefaultMutableTreeNode, syntaxTreeNode: EbnfSyntaxTreeNode): javax.swing.tree.TreePath? {
            // Check if this node matches
            val userObject = treeNode.userObject
            if (userObject === syntaxTreeNode) {
                return javax.swing.tree.TreePath(treeNode.path)
            }

            // Check children
            for (i in 0 until treeNode.childCount) {
                val child = treeNode.getChildAt(i) as DefaultMutableTreeNode
                val path = findNodeInTree(child, syntaxTreeNode)
                if (path != null) {
                    return path
                }
            }

            return null
        }

        override fun dispose() {
            // Remove caret listener
            removeCaretListener()
        }
    }
}
