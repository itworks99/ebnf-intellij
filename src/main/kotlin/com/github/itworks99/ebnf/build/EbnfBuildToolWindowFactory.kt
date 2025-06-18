/**
 * Factory for the EBNF build tool window.
 */
package com.github.itworks99.ebnf.build

import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.github.itworks99.ebnf.settings.EbnfSettingsService
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * Factory for creating the EBNF build tool window.
 * This tool window allows users to generate parsers from EBNF files.
 */
class EbnfBuildToolWindowFactory : ToolWindowFactory, DumbAware {

    /**
     * Creates the tool window content.
     *
     * @param project The project.
     * @param toolWindow The tool window.
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val buildToolWindow = EbnfBuildToolWindow(project, toolWindow)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(buildToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }

    /**
     * The EBNF build tool window.
     *
     * @property project The project.
     * @property toolWindow The tool window.
     */
    class EbnfBuildToolWindow(private val project: Project, private val toolWindow: ToolWindow) {
        private val parserGenerator = EbnfParserGenerator(project)
        private val mainPanel = JPanel(BorderLayout())

        private val targetLanguageComboBox = JComboBox(EbnfParserGenerator.TargetLanguage.values())
        private val generatorTypeComboBox = JComboBox(EbnfParserGenerator.GeneratorType.values())
        private val outputDirectoryField = JTextField()
        private val packageNameField = JTextField()
        private val namespaceField = JTextField()
        private val generateVisitorCheckBox = JCheckBox("Generate Visitor")
        private val generateListenerCheckBox = JCheckBox("Generate Listener")

        private val logTextArea = JTextArea()

        init {
            // Load default values from settings
            val settings = EbnfSettingsService.getInstance().state

            // Set target language
            try {
                val targetLanguage = EbnfParserGenerator.TargetLanguage.valueOf(settings.defaultTargetLanguage)
                targetLanguageComboBox.selectedItem = targetLanguage
            } catch (e: IllegalArgumentException) {
                targetLanguageComboBox.selectedItem = EbnfParserGenerator.TargetLanguage.JAVA
            }

            // Set generator type
            try {
                val generatorType = EbnfParserGenerator.GeneratorType.valueOf(settings.defaultGeneratorType)
                generatorTypeComboBox.selectedItem = generatorType
            } catch (e: IllegalArgumentException) {
                generatorTypeComboBox.selectedItem = EbnfParserGenerator.GeneratorType.ANTLR
            }

            // Set other fields
            outputDirectoryField.text = settings.defaultOutputDirectory
            packageNameField.text = settings.defaultPackageName
            namespaceField.text = settings.defaultNamespace
            generateVisitorCheckBox.isSelected = settings.generateVisitorByDefault
            generateListenerCheckBox.isSelected = settings.generateListenerByDefault

            setupUI()
        }

        /**
         * Sets up the UI.
         */
        private fun setupUI() {
            // Create the configuration panel
            val configPanel = JPanel(GridLayout(0, 2, 5, 5))
            configPanel.border = EmptyBorder(JBUI.insets(5))

            configPanel.add(JBLabel("Target Language:"))
            configPanel.add(targetLanguageComboBox)

            configPanel.add(JBLabel("Generator Type:"))
            configPanel.add(generatorTypeComboBox)

            configPanel.add(JBLabel("Output Directory:"))
            configPanel.add(outputDirectoryField)

            configPanel.add(JBLabel("Package Name:"))
            configPanel.add(packageNameField)

            configPanel.add(JBLabel("Namespace:"))
            configPanel.add(namespaceField)

            configPanel.add(JBLabel("Options:"))
            val optionsPanel = JPanel(GridLayout(1, 2))
            optionsPanel.add(generateVisitorCheckBox)
            optionsPanel.add(generateListenerCheckBox)
            configPanel.add(optionsPanel)

            // Create the button panel
            val buttonPanel = JPanel()
            val generateButton = JButton("Generate Parser")
            generateButton.addActionListener { generateParser() }
            buttonPanel.add(generateButton)

            // Create the log panel
            logTextArea.isEditable = false
            val logScrollPane = JBScrollPane(logTextArea)
            logScrollPane.border = BorderFactory.createTitledBorder("Log")

            // Add panels to the main panel
            mainPanel.add(configPanel, BorderLayout.NORTH)
            mainPanel.add(buttonPanel, BorderLayout.CENTER)
            mainPanel.add(logScrollPane, BorderLayout.SOUTH)
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
         * Generates a parser from the current EBNF file.
         */
        private fun generateParser() {
            val editor = FileEditorManager.getInstance(project).selectedEditor ?: run {
                logMessage("No file is currently open.")
                return
            }

            val file = editor.file
            if (file == null || file.fileType != EbnfFileType) {
                logMessage("The current file is not an EBNF file.")
                return
            }

            logMessage("Generating parser from ${file.name}...")

            val config = EbnfParserGenerator.GeneratorConfig(
                targetLanguage = targetLanguageComboBox.selectedItem as EbnfParserGenerator.TargetLanguage,
                generatorType = generatorTypeComboBox.selectedItem as EbnfParserGenerator.GeneratorType,
                outputDirectory = outputDirectoryField.text,
                packageName = packageNameField.text,
                namespace = namespaceField.text,
                generateVisitor = generateVisitorCheckBox.isSelected,
                generateListener = generateListenerCheckBox.isSelected
            )

            try {
                val result = parserGenerator.generateParser(file, config)
                if (result.success) {
                    logMessage("Parser generation successful: ${result.message}")
                    logMessage("Generated files:")
                    result.generatedFiles.forEach { logMessage("- ${it.absolutePath}") }
                } else {
                    logMessage("Parser generation failed: ${result.message}")
                }
            } catch (e: Exception) {
                logMessage("Error generating parser: ${e.message}")
                thisLogger().error("Error generating parser", e)
            }
        }

        /**
         * Logs a message to the log text area.
         *
         * @param message The message to log.
         */
        private fun logMessage(message: String) {
            logTextArea.append("$message\n")
            logTextArea.caretPosition = logTextArea.document.length
        }
    }
}
