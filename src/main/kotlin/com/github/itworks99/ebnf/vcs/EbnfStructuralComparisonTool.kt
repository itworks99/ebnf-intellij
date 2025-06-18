/**
 * Tool for structural comparison of EBNF files.
 */
package com.github.itworks99.ebnf.vcs

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Factory for creating the EBNF structural comparison tool window.
 * This tool window allows users to compare the structure of two EBNF files.
 */
class EbnfStructuralComparisonToolFactory : ToolWindowFactory, DumbAware {
    
    /**
     * Creates the tool window content.
     *
     * @param project The project.
     * @param toolWindow The tool window.
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val comparisonTool = EbnfStructuralComparisonTool(project, toolWindow)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(comparisonTool.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
    
    /**
     * The EBNF structural comparison tool.
     *
     * @property project The project.
     * @property toolWindow The tool window.
     */
    class EbnfStructuralComparisonTool(private val project: Project, private val toolWindow: ToolWindow) {
        private val mainPanel = JPanel(BorderLayout())
        
        private val file1Tree = JTree()
        private val file2Tree = JTree()
        
        private val file1Label = JBLabel("File 1: Not selected")
        private val file2Label = JBLabel("File 2: Not selected")
        
        private val diffTextArea = JTextArea()
        
        private var file1: VirtualFile? = null
        private var file2: VirtualFile? = null
        
        init {
            setupUI()
        }
        
        /**
         * Sets up the UI.
         */
        private fun setupUI() {
            // Create the file selection panel
            val fileSelectionPanel = JPanel(GridLayout(2, 2, 5, 5))
            fileSelectionPanel.border = EmptyBorder(JBUI.insets(5))
            
            val selectFile1Button = JButton("Select File 1")
            selectFile1Button.addActionListener { selectFile(true) }
            
            val selectFile2Button = JButton("Select File 2")
            selectFile2Button.addActionListener { selectFile(false) }
            
            fileSelectionPanel.add(selectFile1Button)
            fileSelectionPanel.add(file1Label)
            fileSelectionPanel.add(selectFile2Button)
            fileSelectionPanel.add(file2Label)
            
            // Create the tree panel
            val treePanel = JPanel(GridLayout(1, 2, 5, 5))
            treePanel.border = EmptyBorder(JBUI.insets(5))
            
            val file1ScrollPane = JBScrollPane(file1Tree)
            file1ScrollPane.border = BorderFactory.createTitledBorder("File 1 Structure")
            
            val file2ScrollPane = JBScrollPane(file2Tree)
            file2ScrollPane.border = BorderFactory.createTitledBorder("File 2 Structure")
            
            treePanel.add(file1ScrollPane)
            treePanel.add(file2ScrollPane)
            
            // Create the diff panel
            diffTextArea.isEditable = false
            val diffScrollPane = JBScrollPane(diffTextArea)
            diffScrollPane.border = BorderFactory.createTitledBorder("Structural Differences")
            
            // Create the button panel
            val buttonPanel = JPanel()
            val compareButton = JButton("Compare Structures")
            compareButton.addActionListener { compareStructures() }
            buttonPanel.add(compareButton)
            
            // Add panels to the main panel
            mainPanel.add(fileSelectionPanel, BorderLayout.NORTH)
            mainPanel.add(treePanel, BorderLayout.CENTER)
            mainPanel.add(diffScrollPane, BorderLayout.SOUTH)
            mainPanel.add(buttonPanel, BorderLayout.SOUTH)
        }
        
        /**
         * Gets the content panel.
         *
         * @return The content panel.
         */
        fun getContent(): JComponent {
            return mainPanel
        }
        
        /**
         * Selects a file.
         *
         * @param isFile1 Whether to select file 1 or file 2.
         */
        private fun selectFile(isFile1: Boolean) {
            val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
                .withFileFilter { it.fileType == EbnfFileType }
                .withTitle(if (isFile1) "Select EBNF File 1" else "Select EBNF File 2")
            
            FileChooser.chooseFile(descriptor, project, null) { file ->
                if (isFile1) {
                    file1 = file
                    file1Label.text = "File 1: ${file.name}"
                    updateFileTree(file, true)
                } else {
                    file2 = file
                    file2Label.text = "File 2: ${file.name}"
                    updateFileTree(file, false)
                }
            }
        }
        
        /**
         * Updates the file tree.
         *
         * @param file The file.
         * @param isFile1 Whether to update the file 1 tree or the file 2 tree.
         */
        private fun updateFileTree(file: VirtualFile, isFile1: Boolean) {
            val psiFile = PsiManager.getInstance(project).findFile(file)
            if (psiFile !is EbnfFile) {
                thisLogger().warn("Not an EBNF file: ${file.name}")
                return
            }
            
            val rootNode = DefaultMutableTreeNode(file.name)
            val treeModel = DefaultTreeModel(rootNode)
            
            // Add rule nodes
            val rules = psiFile.findChildrenByType(EbnfElementTypes.RULE)
            for (rule in rules) {
                val ruleName = rule.findChildByType(EbnfElementTypes.ID)?.text ?: "Unknown"
                val ruleNode = DefaultMutableTreeNode(ruleName)
                rootNode.add(ruleNode)
            }
            
            if (isFile1) {
                file1Tree.model = treeModel
            } else {
                file2Tree.model = treeModel
            }
            
            TreeUtil.expandAll(if (isFile1) file1Tree else file2Tree)
        }
        
        /**
         * Compares the structures of the two files.
         */
        private fun compareStructures() {
            if (file1 == null || file2 == null) {
                diffTextArea.text = "Please select two EBNF files to compare."
                return
            }
            
            val psiFile1 = PsiManager.getInstance(project).findFile(file1!!)
            val psiFile2 = PsiManager.getInstance(project).findFile(file2!!)
            
            if (psiFile1 !is EbnfFile || psiFile2 !is EbnfFile) {
                diffTextArea.text = "One or both files are not valid EBNF files."
                return
            }
            
            // Get rules from both files
            val rules1 = psiFile1.findChildrenByType(EbnfElementTypes.RULE)
            val rules2 = psiFile2.findChildrenByType(EbnfElementTypes.RULE)
            
            // Create maps of rule names to rules
            val ruleMap1 = rules1.associateBy { it.findChildByType(EbnfElementTypes.ID)?.text ?: "Unknown" }
            val ruleMap2 = rules2.associateBy { it.findChildByType(EbnfElementTypes.ID)?.text ?: "Unknown" }
            
            // Find rules that are in file 1 but not in file 2
            val rulesOnlyInFile1 = ruleMap1.keys - ruleMap2.keys
            
            // Find rules that are in file 2 but not in file 1
            val rulesOnlyInFile2 = ruleMap2.keys - ruleMap1.keys
            
            // Find rules that are in both files but have different definitions
            val rulesInBoth = ruleMap1.keys.intersect(ruleMap2.keys)
            val rulesWithDifferentDefinitions = rulesInBoth.filter {
                val rule1 = ruleMap1[it]
                val rule2 = ruleMap2[it]
                rule1?.text != rule2?.text
            }
            
            // Build the diff text
            val diffText = StringBuilder()
            
            if (rulesOnlyInFile1.isNotEmpty()) {
                diffText.append("Rules only in ${file1!!.name}:\n")
                rulesOnlyInFile1.sorted().forEach { diffText.append("- $it\n") }
                diffText.append("\n")
            }
            
            if (rulesOnlyInFile2.isNotEmpty()) {
                diffText.append("Rules only in ${file2!!.name}:\n")
                rulesOnlyInFile2.sorted().forEach { diffText.append("- $it\n") }
                diffText.append("\n")
            }
            
            if (rulesWithDifferentDefinitions.isNotEmpty()) {
                diffText.append("Rules with different definitions:\n")
                rulesWithDifferentDefinitions.sorted().forEach { diffText.append("- $it\n") }
                diffText.append("\n")
            }
            
            if (diffText.isEmpty()) {
                diffText.append("No structural differences found between the files.")
            }
            
            diffTextArea.text = diffText.toString()
        }
    }
}