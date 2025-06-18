package com.github.itworks99.ebnf.language.diagram

import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File

/**
 * Factory for creating the Railroad Diagram tool window.
 *
 * This tool window displays railroad diagrams for EBNF grammar rules
 * in the currently open EBNF file.
 */
class EbnfRailroadDiagramToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(
            RailroadDiagramPanel(project, toolWindow),
            "Railroad Diagrams",
            false
        )
        toolWindow.contentManager.addContent(content)
    }
    
    override fun shouldBeAvailable(project: Project) = true
    
    /**
     * Panel that displays railroad diagrams for EBNF grammar rules.
     */
    private class RailroadDiagramPanel(
        private val project: Project,
        private val toolWindow: ToolWindow
    ) : JPanel(BorderLayout()) {
        private val diagramGenerator = EbnfRailroadDiagramGenerator()
        private val diagramsPanel = JPanel(GridLayout(0, 1, 0, 10))
        private val scrollPane = JBScrollPane(diagramsPanel)
        private val toolbarPanel = JPanel(BorderLayout())
        private val exportButton = JButton("Export Diagrams")
        private val refreshButton = JButton("Refresh")
        private val statusLabel = JLabel("No EBNF file open")
        
        init {
            // Set up the toolbar
            toolbarPanel.add(exportButton, BorderLayout.WEST)
            toolbarPanel.add(refreshButton, BorderLayout.EAST)
            toolbarPanel.add(statusLabel, BorderLayout.CENTER)
            toolbarPanel.border = JBUI.Borders.empty(5)
            
            // Set up the main panel
            add(toolbarPanel, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
            
            // Set up the diagrams panel
            diagramsPanel.border = JBUI.Borders.empty(10)
            
            // Add listeners
            exportButton.addActionListener { exportDiagrams() }
            refreshButton.addActionListener { updateDiagrams() }
            
            // Listen for file editor changes
            project.messageBus.connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                object : FileEditorManagerListener {
                    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                        if (file.fileType == EbnfFileType) {
                            updateDiagrams()
                        }
                    }
                    
                    override fun selectionChanged(event: FileEditorManagerEvent) {
                        val file = event.newFile
                        if (file?.fileType == EbnfFileType) {
                            updateDiagrams()
                        } else {
                            clearDiagrams()
                        }
                    }
                }
            )
            
            // Initial update
            updateDiagrams()
        }
        
        /**
         * Updates the diagrams based on the currently open EBNF file.
         */
        private fun updateDiagrams() {
            // Clear existing diagrams
            diagramsPanel.removeAll()
            
            // Get the current file
            val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
            
            if (currentFile?.fileType == EbnfFileType) {
                // Get the PSI file
                val psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(currentFile)
                
                if (psiFile is EbnfFile) {
                    // Generate diagrams for all rules
                    val diagrams = diagramGenerator.generateDiagrams(psiFile)
                    
                    if (diagrams.isNotEmpty()) {
                        // Add each diagram to the panel
                        for (diagram in diagrams) {
                            diagramsPanel.add(diagram)
                        }
                        
                        statusLabel.text = "${diagrams.size} rule(s) found in ${currentFile.name}"
                        exportButton.isEnabled = true
                    } else {
                        diagramsPanel.add(JLabel("No rules found in the file"))
                        statusLabel.text = "No rules found in ${currentFile.name}"
                        exportButton.isEnabled = false
                    }
                } else {
                    diagramsPanel.add(JLabel("Not an EBNF file"))
                    statusLabel.text = "Not an EBNF file"
                    exportButton.isEnabled = false
                }
            } else {
                diagramsPanel.add(JLabel("No EBNF file open"))
                statusLabel.text = "No EBNF file open"
                exportButton.isEnabled = false
            }
            
            // Refresh the UI
            diagramsPanel.revalidate()
            diagramsPanel.repaint()
        }
        
        /**
         * Clears all diagrams from the panel.
         */
        private fun clearDiagrams() {
            diagramsPanel.removeAll()
            diagramsPanel.add(JLabel("No EBNF file open"))
            statusLabel.text = "No EBNF file open"
            exportButton.isEnabled = false
            
            // Refresh the UI
            diagramsPanel.revalidate()
            diagramsPanel.repaint()
        }
        
        /**
         * Exports all diagrams as image files.
         */
        private fun exportDiagrams() {
            // Get the current file
            val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
            
            if (currentFile?.fileType == EbnfFileType) {
                // Get the PSI file
                val psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(currentFile)
                
                if (psiFile is EbnfFile) {
                    // Generate diagrams for all rules
                    val diagrams = diagramGenerator.generateDiagrams(psiFile)
                    
                    if (diagrams.isNotEmpty()) {
                        // Show a file chooser dialog
                        val fileChooser = JFileChooser()
                        fileChooser.dialogTitle = "Select Directory to Save Diagrams"
                        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        
                        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                            val directory = fileChooser.selectedFile
                            
                            // Get all rule names
                            val ruleNames = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(psiFile, com.intellij.psi.PsiElement::class.java)
                                .filter { it.node.elementType == com.github.itworks99.ebnf.language.EbnfElementTypes.RULE_NAME }
                                .map { it.text }
                                .toList()
                            
                            // Export each diagram
                            for (i in diagrams.indices) {
                                val ruleName = if (i < ruleNames.size) ruleNames[i] else "rule_$i"
                                val filePath = File(directory, "${ruleName}.png").absolutePath
                                diagramGenerator.exportDiagram(diagrams[i], filePath)
                            }
                            
                            statusLabel.text = "Exported ${diagrams.size} diagram(s) to ${directory.absolutePath}"
                        }
                    }
                }
            }
        }
    }
}