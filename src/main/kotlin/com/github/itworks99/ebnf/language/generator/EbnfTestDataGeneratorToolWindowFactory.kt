package com.github.itworks99.ebnf.language.generator

import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.psi.EbnfFile
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
    private class TestDataGeneratorPanel(private val project: Project) : JPanel(BorderLayout()) {
        private val rulesList = JList<String>()
        private val rulesListModel = DefaultListModel<String>()
        private val generatedTextArea = JTextArea()
        private val generateButton = JButton("Generate")
        private val countSpinner = JSpinner(SpinnerNumberModel(5, 1, 100, 1))
        private var currentFile: EbnfFile? = null

        init {
            // Set up the rules list
            rulesList.model = rulesListModel
            rulesList.selectionMode = ListSelectionModel.SINGLE_SELECTION
            rulesList.addListSelectionListener(object : ListSelectionListener {
                override fun valueChanged(e: ListSelectionEvent) {
                    updateGenerateButtonState()
                }
            })

            // Set up the generated text area
            generatedTextArea.isEditable = false
            generatedTextArea.lineWrap = true
            generatedTextArea.wrapStyleWord = true

            // Set up the generate button
            generateButton.isEnabled = false
            generateButton.addActionListener {
                generateTestData()
            }

            // Set up the count spinner
            val countPanel = JPanel(BorderLayout())
            countPanel.add(JBLabel("Count: "), BorderLayout.WEST)
            countPanel.add(countSpinner, BorderLayout.CENTER)
            countPanel.border = JBUI.Borders.empty(5)

            // Set up the controls panel
            val controlsPanel = JPanel(GridBagLayout())
            val gbc = GridBagConstraints()
            
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.weightx = 1.0
            gbc.fill = GridBagConstraints.HORIZONTAL
            controlsPanel.add(countPanel, gbc)
            
            gbc.gridx = 1
            gbc.gridy = 0
            gbc.weightx = 0.0
            controlsPanel.add(generateButton, gbc)

            // Set up the main layout
            val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
            splitPane.topComponent = JBScrollPane(rulesList)
            splitPane.bottomComponent = JBScrollPane(generatedTextArea)
            splitPane.dividerLocation = 200
            splitPane.resizeWeight = 0.3

            add(JBLabel("Select a rule to generate test data:"), BorderLayout.NORTH)
            add(splitPane, BorderLayout.CENTER)
            add(controlsPanel, BorderLayout.SOUTH)

            // Listen for file editor changes
            project.messageBus.connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                object : FileEditorManagerListener {
                    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                        if (file.fileType == EbnfFileType) {
                            updateForFile(file)
                        }
                    }

                    override fun selectionChanged(event: FileEditorManagerEvent) {
                        val file = event.newFile
                        if (file?.fileType == EbnfFileType) {
                            updateForFile(file)
                        } else {
                            clearUI()
                        }
                    }
                }
            )

            // Initial update
            val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
            if (currentFile?.fileType == EbnfFileType) {
                updateForFile(currentFile)
            }
        }

        /**
         * Updates the UI for the given file.
         */
        private fun updateForFile(file: VirtualFile) {
            val psiFile = PsiManager.getInstance(project).findFile(file) as? EbnfFile
            if (psiFile != null) {
                currentFile = psiFile
                updateRulesList(psiFile)
                generatedTextArea.text = "Select a rule and click Generate to create test data."
            } else {
                clearUI()
            }
        }

        /**
         * Updates the rules list for the given file.
         */
        private fun updateRulesList(file: EbnfFile) {
            rulesListModel.clear()
            
            // Create a generator to find all rules
            val generator = EbnfTestDataGenerator(file)
            
            // Get all rules and add them to the list
            val rules = generator.generateForAllRules(0).keys.sorted()
            for (rule in rules) {
                rulesListModel.addElement(rule)
            }
            
            updateGenerateButtonState()
        }

        /**
         * Clears the UI.
         */
        private fun clearUI() {
            rulesListModel.clear()
            generatedTextArea.text = "No EBNF file open."
            currentFile = null
            updateGenerateButtonState()
        }

        /**
         * Updates the state of the generate button.
         */
        private fun updateGenerateButtonState() {
            generateButton.isEnabled = currentFile != null && rulesList.selectedIndex != -1
        }

        /**
         * Generates test data for the selected rule.
         */
        private fun generateTestData() {
            val selectedRule = rulesList.selectedValue ?: return
            val file = currentFile ?: return
            val count = countSpinner.value as Int
            
            // Create a generator
            val generator = EbnfTestDataGenerator(file)
            
            // Generate test data
            val result = StringBuilder()
            
            if (selectedRule == "All Rules") {
                // Generate for all rules
                val allData = generator.generateForAllRules(count)
                for ((rule, samples) in allData) {
                    result.append("Rule: $rule\n")
                    for (sample in samples) {
                        result.append("  $sample\n")
                    }
                    result.append("\n")
                }
            } else {
                // Generate for the selected rule
                result.append("Rule: $selectedRule\n")
                for (i in 0 until count) {
                    val sample = generator.generateForRule(selectedRule)
                    if (sample != null) {
                        result.append("  $sample\n")
                    }
                }
            }
            
            // Update the text area
            generatedTextArea.text = result.toString()
            generatedTextArea.caretPosition = 0
        }
    }
}