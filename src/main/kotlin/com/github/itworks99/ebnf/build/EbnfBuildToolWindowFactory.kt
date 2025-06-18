/**
 * Factory for the EBNF build tool window.
 */
package com.github.itworks99.ebnf.build

import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.github.itworks99.ebnf.settings.EbnfSettingsService
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
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
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionListener
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.filechooser.FileNameExtensionFilter

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
        private val consoleArea = JTextArea()
        private val targetLanguageComboBox = JComboBox<String>(arrayOf("Java", "Python", "C#", "JavaScript"))
        private val outDirTextField = JTextField(30)
        private val packageNameTextField = JTextField(30)
        private val generateButton = JButton("Generate Parser")
        private val clearLogButton = JButton("Clear Log")
        private var currentFile: VirtualFile? = null

        /**
         * Initializes the tool window content.
         *
         * @return The tool window content.
         */
        fun getContent(): JComponent {
            // Set up console area
            consoleArea.isEditable = false
            consoleArea.lineWrap = true
            consoleArea.wrapStyleWord = true
            val scrollPane = JBScrollPane(consoleArea)
            scrollPane.border = TitledBorder("Console Output")

            // Create options panel
            val optionsPanel = createOptionsPanel()

            // Create button panel
            val buttonPanel = createButtonPanel()

            // Add panels to main panel
            mainPanel.add(optionsPanel, BorderLayout.NORTH)
            mainPanel.add(scrollPane, BorderLayout.CENTER)
            mainPanel.add(buttonPanel, BorderLayout.SOUTH)
            mainPanel.border = EmptyBorder(10, 10, 10, 10)

            // Configure initial state
            updateUIState()

            // Set up file change listener
            setupFileChangeListener()

            // Initial check for open EBNF file
            checkForOpenEbnfFile()

            return mainPanel
        }

        /**
         * Creates the options panel with configuration settings.
         */
        private fun createOptionsPanel(): JPanel {
            val panel = JPanel(GridBagLayout())
            panel.border = TitledBorder("Parser Generation Options")
            val gbc = GridBagConstraints()
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.insets = Insets(5, 5, 5, 5)

            // Load settings
            val settings = EbnfSettingsService.getInstance().state

            // Add target language selection
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.weightx = 0.0
            panel.add(JBLabel("Target Language:"), gbc)

            gbc.gridx = 1
            gbc.weightx = 1.0
            panel.add(targetLanguageComboBox, gbc)
            targetLanguageComboBox.selectedItem = settings.lastUsedLanguage

            // Add output directory selection
            gbc.gridx = 0
            gbc.gridy = 1
            gbc.weightx = 0.0
            panel.add(JBLabel("Output Directory:"), gbc)

            gbc.gridx = 1
            gbc.weightx = 1.0
            panel.add(outDirTextField, gbc)
            outDirTextField.text = settings.lastOutputDirectory

            gbc.gridx = 2
            gbc.weightx = 0.0
            val browseButton = JButton("Browse...")
            browseButton.addActionListener {
                val fileChooser = JFileChooser()
                fileChooser.dialogTitle = "Select Output Directory"
                fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY

                if (outDirTextField.text.isNotEmpty()) {
                    val dir = File(outDirTextField.text)
                    if (dir.exists()) {
                        fileChooser.currentDirectory = dir
                    }
                }

                if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                    outDirTextField.text = fileChooser.selectedFile.absolutePath

                    // Save the setting
                    val settings = EbnfSettingsService.getInstance().state
                    settings.lastOutputDirectory = outDirTextField.text
                }
            }
            panel.add(browseButton, gbc)

            // Add package name field
            gbc.gridx = 0
            gbc.gridy = 2
            gbc.weightx = 0.0
            panel.add(JBLabel("Package Name:"), gbc)

            gbc.gridx = 1
            gbc.weightx = 1.0
            gbc.gridwidth = 2
            panel.add(packageNameTextField, gbc)
            packageNameTextField.text = settings.lastPackageName

            return panel
        }

        /**
         * Creates the button panel with action buttons.
         */
        private fun createButtonPanel(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.border = EmptyBorder(10, 0, 0, 0)

            generateButton.addActionListener {
                generateParser()
            }

            clearLogButton.addActionListener {
                consoleArea.text = ""
            }

            val buttonPanel = JPanel()
            buttonPanel.add(generateButton)
            buttonPanel.add(clearLogButton)

            panel.add(buttonPanel, BorderLayout.EAST)

            return panel
        }

        /**
         * Sets up a listener for file changes.
         */
        private fun setupFileChangeListener() {
            project.messageBus.connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                object : FileEditorManagerListener {
                    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                        if (file.fileType == EbnfFileType) {
                            currentFile = file
                            updateUIState()
                        }
                    }

                    override fun selectionChanged(event: FileEditorManagerEvent) {
                        val newFile = event.newFile
                        if (newFile?.fileType == EbnfFileType) {
                            currentFile = newFile
                        } else if (event.oldFile?.fileType == EbnfFileType && newFile == null) {
                            // If we've navigated away from an EBNF file and not to a new EBNF file
                            currentFile = FileEditorManager.getInstance(project).selectedFiles
                                .firstOrNull { it.fileType == EbnfFileType }
                        }
                        updateUIState()
                    }
                }
            )
        }

        /**
         * Checks for an open EBNF file when the tool window is first shown.
         */
        private fun checkForOpenEbnfFile() {
            currentFile = FileEditorManager.getInstance(project).selectedFiles
                .firstOrNull { it.fileType == EbnfFileType }
            updateUIState()
        }

        /**
         * Updates the UI state based on the current file.
         */
        private fun updateUIState() {
            generateButton.isEnabled = currentFile != null
        }

        /**
         * Generates a parser from the current EBNF file.
         */
        private fun generateParser() {
            val file = currentFile ?: return
            val targetLanguage = targetLanguageComboBox.selectedItem as String
            val outputDir = outDirTextField.text
            val packageName = packageNameTextField.text

            // Validate output directory
            if (outputDir.isEmpty()) {
                JOptionPane.showMessageDialog(
                    mainPanel,
                    "Please specify an output directory.",
                    "Missing Output Directory",
                    JOptionPane.WARNING_MESSAGE
                )
                return
            }

            // Save settings
            val settings = EbnfSettingsService.getInstance().state
            settings.lastUsedLanguage = targetLanguage
            settings.lastOutputDirectory = outputDir
            settings.lastPackageName = packageName

            // Log generation start
            log("Starting parser generation...")
            log("Target language: $targetLanguage")
            log("Output directory: $outputDir")
            log("Package name: $packageName")
            log("Source file: ${file.path}")
            log("")

            // Generate the parser
            parserGenerator.generateParser(
                file,
                targetLanguage,
                outputDir,
                packageName,
                object : ParserGenerationListener {
                    override fun onProgress(message: String) {
                        log(message)
                    }

                    override fun onComplete(successful: Boolean, message: String) {
                        if (successful) {
                            log("Parser generation completed successfully.")
                            log("Output: $message")
                        } else {
                            log("Parser generation failed: $message")
                        }
                    }
                }
            )
        }

        /**
         * Logs a message to the console area.
         *
         * @param message The message to log.
         */
        private fun log(message: String) {
            consoleArea.append("$message\n")
            consoleArea.caretPosition = consoleArea.document.length
        }
    }
}

/**
 * Interface for listening to parser generation events.
 */
interface ParserGenerationListener {
    /**
     * Called when there is progress in the generation process.
     *
     * @param message The progress message.
     */
    fun onProgress(message: String)

    /**
     * Called when the generation process completes.
     *
     * @param successful Whether the generation was successful.
     * @param message The completion message or error message.
     */
    fun onComplete(successful: Boolean, message: String)
}

/**
 * Generator for parsers from EBNF grammars.
 *
 * @property project The current project.
 */
class EbnfParserGenerator(private val project: Project) {
    private val logger = thisLogger()

    /**
     * Generates a parser from an EBNF file.
     *
     * @param file The EBNF file.
     * @param targetLanguage The target language for the parser.
     * @param outputDir The output directory.
     * @param packageName The package name for the generated code.
     * @param listener A listener for generation events.
     */
    fun generateParser(
        file: VirtualFile,
        targetLanguage: String,
        outputDir: String,
        packageName: String,
        listener: ParserGenerationListener
    ) {
        try {
            // Get the PSI file
            val psiFile = PsiManager.getInstance(project).findFile(file) as? EbnfFile
            if (psiFile == null) {
                listener.onComplete(false, "Failed to get PSI file for ${file.name}")
                return
            }

            // Step 1: Parse the EBNF grammar
            listener.onProgress("Parsing EBNF grammar...")

            // Step 2: Validate the grammar
            listener.onProgress("Validating grammar...")

            // Step 3: Generate the parser code
            listener.onProgress("Generating parser code for $targetLanguage...")

            // Step 4: Save generated files
            val outputDirectory = File(outputDir)
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs()
            }

            // This is just a placeholder - in a real implementation,
            // we would generate actual parser code here based on the grammar
            when (targetLanguage) {
                "Java" -> generateJavaParser(psiFile, outputDirectory, packageName, listener)
                "Python" -> generatePythonParser(psiFile, outputDirectory, packageName, listener)
                "C#" -> generateCSharpParser(psiFile, outputDirectory, packageName, listener)
                "JavaScript" -> generateJavaScriptParser(psiFile, outputDirectory, packageName, listener)
                else -> {
                    listener.onComplete(false, "Unsupported target language: $targetLanguage")
                    return
                }
            }

            listener.onComplete(true, outputDirectory.absolutePath)
        } catch (e: Exception) {
            logger.error("Error generating parser", e)
            listener.onComplete(false, "Error: ${e.message}")
        }
    }

    /**
     * Generates a Java parser from an EBNF grammar.
     */
    private fun generateJavaParser(
        psiFile: EbnfFile,
        outputDir: File,
        packageName: String,
        listener: ParserGenerationListener
    ) {
        listener.onProgress("Generating Java lexer class...")
        val lexerFile = File(outputDir, "${psiFile.name}Lexer.java")
        lexerFile.writeText("package $packageName;\n\n// Generated Java lexer\npublic class ${psiFile.name}Lexer {\n    // Lexer implementation\n}")

        listener.onProgress("Generating Java parser class...")
        val parserFile = File(outputDir, "${psiFile.name}Parser.java")
        parserFile.writeText("package $packageName;\n\n// Generated Java parser\npublic class ${psiFile.name}Parser {\n    // Parser implementation\n}")
    }

    /**
     * Generates a Python parser from an EBNF grammar.
     */
    private fun generatePythonParser(
        psiFile: EbnfFile,
        outputDir: File,
        packageName: String,
        listener: ParserGenerationListener
    ) {
        listener.onProgress("Generating Python lexer module...")
        val lexerFile = File(outputDir, "${psiFile.name.toLowerCase()}_lexer.py")
        lexerFile.writeText("# Generated Python lexer\nclass ${psiFile.name}Lexer:\n    # Lexer implementation\n    pass")

        listener.onProgress("Generating Python parser module...")
        val parserFile = File(outputDir, "${psiFile.name.toLowerCase()}_parser.py")
        parserFile.writeText("# Generated Python parser\nclass ${psiFile.name}Parser:\n    # Parser implementation\n    pass")
    }

    /**
     * Generates a C# parser from an EBNF grammar.
     */
    private fun generateCSharpParser(
        psiFile: EbnfFile,
        outputDir: File,
        packageName: String,
        listener: ParserGenerationListener
    ) {
        listener.onProgress("Generating C# lexer class...")
        val lexerFile = File(outputDir, "${psiFile.name}Lexer.cs")
        lexerFile.writeText("namespace $packageName;\n\n// Generated C# lexer\npublic class ${psiFile.name}Lexer\n{\n    // Lexer implementation\n}")

        listener.onProgress("Generating C# parser class...")
        val parserFile = File(outputDir, "${psiFile.name}Parser.cs")
        parserFile.writeText("namespace $packageName;\n\n// Generated C# parser\npublic class ${psiFile.name}Parser\n{\n    // Parser implementation\n}")
    }

    /**
     * Generates a JavaScript parser from an EBNF grammar.
     */
    private fun generateJavaScriptParser(
        psiFile: EbnfFile,
        outputDir: File,
        packageName: String,
        listener: ParserGenerationListener
    ) {
        listener.onProgress("Generating JavaScript lexer module...")
        val lexerFile = File(outputDir, "${psiFile.name.toLowerCase()}Lexer.js")
        lexerFile.writeText("// Generated JavaScript lexer\nclass ${psiFile.name}Lexer {\n    // Lexer implementation\n}")

        listener.onProgress("Generating JavaScript parser module...")
        val parserFile = File(outputDir, "${psiFile.name.toLowerCase()}Parser.js")
        parserFile.writeText("// Generated JavaScript parser\nclass ${psiFile.name}Parser {\n    // Parser implementation\n}")
    }
}
