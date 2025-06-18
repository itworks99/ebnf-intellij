package com.github.itworks99.ebnf.language.diagram

import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

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
            RailroadDiagramPanel(project),
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
        private val project: Project
    ) : JPanel(BorderLayout()), Disposable {
        private val diagramGenerator = EbnfRailroadDiagramGenerator()
        private val diagramPanel = JPanel(GridLayout(0, 1, 10, 10))
        private val ruleSelector = JComboBox<String>()
        private val messageLabel = JLabel("No EBNF file open")
        private var currentFile: VirtualFile? = null

        init {
            // Set up the rule selector
            ruleSelector.addActionListener {
                updateDiagram()
            }

            // Set up the panel
            val controlPanel = JPanel(BorderLayout())
            controlPanel.add(JLabel("Select rule: "), BorderLayout.WEST)
            controlPanel.add(ruleSelector, BorderLayout.CENTER)

            val exportButton = JButton("Export diagram")
            exportButton.addActionListener {
                exportDiagram()
            }
            controlPanel.add(exportButton, BorderLayout.EAST)
            controlPanel.border = JBUI.Borders.empty(5)

            val scrollPane = JBScrollPane(diagramPanel)
            scrollPane.border = JBUI.Borders.empty()

            add(messageLabel, BorderLayout.NORTH)
            add(controlPanel, BorderLayout.SOUTH)
            add(scrollPane, BorderLayout.CENTER)
            
            // Listen for file editor changes
            project.messageBus.connect(this).subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                object : FileEditorManagerListener {
                    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                        if (file.fileType == EbnfFileType) {
                            updateRules(file)
                        }
                    }
                    
                    override fun selectionChanged(event: FileEditorManagerEvent) {
                        val file = event.newFile
                        if (file?.fileType == EbnfFileType) {
                            updateRules(file)
                        } else {
                            clearDiagrams()
                        }
                    }
                }
            )
            
            // Initial update
            val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
            if (currentFile?.fileType == EbnfFileType) {
                updateRules(currentFile)
            }
        }

        /**
         * Updates the rule selector with rules from the file.
         */
        private fun updateRules(file: VirtualFile) {
            val psiFile = PsiManager.getInstance(project).findFile(file) as? EbnfFile

            if (psiFile != null) {
                // Get rule names from the file
                val ruleNames = psiFile.getRuleNames()

                // Update the rule selector
                ruleSelector.removeAllItems()
                ruleNames.forEach { ruleSelector.addItem(it) }

                // Update the message
                messageLabel.text = "Railroad diagrams for ${file.name}"

                // Save the current file
                currentFile = file

                // Update the diagram if there are rules
                if (ruleNames.isNotEmpty()) {
                    updateDiagram()
                } else {
                    clearDiagrams()
                    messageLabel.text = "No rules found in ${file.name}"
                }
            } else {
                clearDiagrams()
            }
        }

        /**
         * Updates the diagram based on the selected rule.
         */
        private fun updateDiagram() {
            val selectedRule = ruleSelector.selectedItem as? String ?: return
            val psiFile = currentFile?.let { PsiManager.getInstance(project).findFile(it) as? EbnfFile } ?: return

            // Clear existing diagrams
            diagramPanel.removeAll()

            // Get the rule definition from the file
            val rule = psiFile.findRuleByName(selectedRule)

            if (rule != null) {
                // Generate the diagram component
                val diagram = diagramGenerator.generateDiagram(rule)

                // Add the diagram to the panel
                diagramPanel.add(diagram)

                // Update the UI
                diagramPanel.revalidate()
                diagramPanel.repaint()
            }
        }
        
        /**
         * Clears all diagrams.
         */
        private fun clearDiagrams() {
            diagramPanel.removeAll()
            ruleSelector.removeAllItems()
            messageLabel.text = "No EBNF file open"
            diagramPanel.revalidate()
            diagramPanel.repaint()
            currentFile = null
        }
        
        /**
         * Exports the current diagram to an image file.
         */
        private fun exportDiagram() {
            val selectedRule = ruleSelector.selectedItem as? String ?: return

            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "Export Railroad Diagram"
            fileChooser.selectedFile = File("${selectedRule}_diagram.png")
            fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("PNG Images", "png")

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                val diagram = diagramPanel.components.firstOrNull() ?: return

                // Create a buffered image
                val image = BufferedImage(
                    diagram.width, diagram.height, BufferedImage.TYPE_INT_ARGB
                )

                // Paint the diagram to the image
                diagram.paint(image.graphics)

                // Save the image
                ImageIO.write(image, "png", file)

                // Show a confirmation message
                JOptionPane.showMessageDialog(
                    this,
                    "Diagram exported to ${file.absolutePath}",
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
        }

        override fun dispose() {
            // Clean up resources
        }
    }
}

/**
 * Generator for railroad diagrams from EBNF grammar rules.
 */
class EbnfRailroadDiagramGenerator {
    /**
     * Generates a diagram component for the rule.
     */
    fun generateDiagram(rule: Any): JComponent {
        // This is a placeholder implementation - in a real system,
        // you would use a proper railroad diagram library
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(600, 200)
        panel.background = Color.WHITE
        panel.border = BorderFactory.createLineBorder(Color.BLACK)

        val ruleName = (rule as? com.github.itworks99.ebnf.language.psi.impl.EbnfRuleImpl)?.name ?: "Unknown"

        val label = JLabel("Railroad diagram for rule: $ruleName")
        label.horizontalAlignment = JLabel.CENTER

        panel.add(label, BorderLayout.NORTH)

        // Add a note indicating this is a placeholder
        val note = JLabel("This is a placeholder. Implement actual diagram rendering here.")
        note.horizontalAlignment = JLabel.CENTER
        panel.add(note, BorderLayout.CENTER)

        return panel
    }
}