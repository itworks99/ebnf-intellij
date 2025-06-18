package com.github.itworks99.ebnf.language.generator

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
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

/**
 * Factory for creating the Test Data Generator tool window.
 *
 * This tool window provides a UI for generating test data from EBNF grammars.
 */
class EbnfTestDataGeneratorToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(
            TestDataGeneratorPanel(project),
            "Test Data Generator",
            false
        )
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    /**
     * Panel that displays the test data generator UI.
     */
    private class TestDataGeneratorPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
        private val rulesList = JList<String>()
        private val rulesListModel = DefaultListModel<String>()
        private val generatedTextArea = JTextArea()
        private val generateButton = JButton("Generate")
        private val countSpinner = JSpinner(SpinnerNumberModel(5, 1, 100, 1))
        private val saveButton = JButton("Save to File")
        private val clearButton = JButton("Clear")
        private var currentFile: EbnfFile? = null

        init {
            // Set up the rules list
            rulesList.model = rulesListModel
            rulesList.selectionMode = ListSelectionModel.SINGLE_SELECTION

            // Set up the text area
            generatedTextArea.isEditable = false
            generatedTextArea.lineWrap = true
            generatedTextArea.wrapStyleWord = true

            // Set up the generate button
            generateButton.addActionListener { generateTestData() }
            generateButton.isEnabled = false

            // Set up the save button
            saveButton.addActionListener { saveTestData() }
            saveButton.isEnabled = false

            // Set up the clear button
            clearButton.addActionListener { clearGeneratedText() }
            clearButton.isEnabled = false

            // Set up the rule selection listener
            rulesList.addListSelectionListener { e: ListSelectionEvent ->
                if (!e.valueIsAdjusting) {
                    generateButton.isEnabled = rulesList.selectedValue != null
                }
            }

            // Create the control panel
            val controlPanel = JPanel(GridBagLayout())
            val gbc = GridBagConstraints()
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.insets = Insets(5, 5, 5, 5)

            gbc.gridx = 0
            gbc.gridy = 0
            controlPanel.add(JBLabel("Number of samples:"), gbc)

            gbc.gridx = 1
            gbc.weightx = 0.3
            controlPanel.add(countSpinner, gbc)

            gbc.gridx = 2
            gbc.weightx = 0.7
            controlPanel.add(generateButton, gbc)

            gbc.gridx = 0
            gbc.gridy = 1
            gbc.gridwidth = 2
            gbc.weightx = 1.0
            controlPanel.add(saveButton, gbc)

            gbc.gridx = 2
            gbc.gridwidth = 1
            controlPanel.add(clearButton, gbc)

            // Create the split pane
            val rulesScrollPane = JBScrollPane(rulesList)
            rulesScrollPane.border = BorderFactory.createTitledBorder("Rules")

            val generatedScrollPane = JBScrollPane(generatedTextArea)
            generatedScrollPane.border = BorderFactory.createTitledBorder("Generated Test Data")

            val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rulesScrollPane, generatedScrollPane)
            splitPane.dividerLocation = 250

            // Add components to the main panel
            add(JBLabel("Select a rule to generate test data for"), BorderLayout.NORTH)
            add(splitPane, BorderLayout.CENTER)
            add(controlPanel, BorderLayout.SOUTH)

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
                            clearRules()
                        }
                    }
                }
            )

            // Initial update
            val selectedFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull {
                it.fileType == EbnfFileType
            }
            if (selectedFile != null) {
                updateRules(selectedFile)
            }
        }

        /**
         * Updates the rules list with rules from the file.
         */
        private fun updateRules(file: VirtualFile) {
            val psiFile = PsiManager.getInstance(project).findFile(file) as? EbnfFile

            if (psiFile != null) {
                // Clear the existing rules
                rulesListModel.clear()

                // Add rule names to the list
                val ruleNames = psiFile.getRuleNames()
                for (ruleName in ruleNames) {
                    rulesListModel.addElement(ruleName)
                }

                // Save the current file
                currentFile = psiFile

                // Select the first rule if available
                if (rulesListModel.size() > 0) {
                    rulesList.selectedIndex = 0
                }
            } else {
                clearRules()
            }
        }

        /**
         * Clears the rules list.
         */
        private fun clearRules() {
            rulesListModel.clear()
            generateButton.isEnabled = false
            currentFile = null
        }

        /**
         * Generates test data for the selected rule.
         */
        private fun generateTestData() {
            val selectedRule = rulesList.selectedValue ?: return
            val countToGenerate = countSpinner.value as Int

            // Implement a simple test data generator
            val generator = TestDataGenerator()
            val testData = generator.generateTestData(selectedRule, countToGenerate, currentFile)

            // Display the generated test data
            generatedTextArea.text = testData.joinToString("\n")
            saveButton.isEnabled = true
            clearButton.isEnabled = true
        }

        /**
         * Saves the generated test data to a file.
         */
        private fun saveTestData() {
            val selectedRule = rulesList.selectedValue ?: return
            val testData = generatedTextArea.text

            if (testData.isNotEmpty()) {
                val fileChooser = JFileChooser()
                fileChooser.dialogTitle = "Save Test Data"
                fileChooser.selectedFile = File("${selectedRule}_test_data.txt")

                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    val file = fileChooser.selectedFile

                    // Write the test data to the file
                    file.writeText(testData)

                    // Show a confirmation message
                    JOptionPane.showMessageDialog(
                        this,
                        "Test data saved to ${file.absolutePath}",
                        "Save Successful",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }
        }

        /**
         * Clears the generated text.
         */
        private fun clearGeneratedText() {
            generatedTextArea.text = ""
            saveButton.isEnabled = false
            clearButton.isEnabled = false
        }

        override fun dispose() {
            // Clean up resources
        }
    }
}

/**
 * Simple test data generator for EBNF rules.
 */
class TestDataGenerator {
    /**
     * Generates test data for the given rule.
     *
     * @param ruleName The name of the rule to generate test data for
     * @param count The number of test data items to generate
     * @param ebnfFile The EBNF file containing the rule
     * @return A list of generated test data strings
     */
    fun generateTestData(ruleName: String, count: Int, ebnfFile: EbnfFile?): List<String> {
        if (ebnfFile == null) return emptyList()

        val rule = ebnfFile.findRuleByName(ruleName) ?: return emptyList()

        // This is a placeholder for actual test data generation logic
        // In a real implementation, you would parse the rule and generate valid strings
        return List(count) { index -> "Sample data $index for rule '$ruleName'" }
    }
}
