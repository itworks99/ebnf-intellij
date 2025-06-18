/**
 * UI component for EBNF plugin settings.
 */
package com.github.itworks99.ebnf.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.openapi.ui.ComboBox
import javax.swing.JPanel
import javax.swing.JComponent

/**
 * Component for displaying and editing EBNF plugin settings.
 */
class EbnfSettingsComponent {
    private val validateReferencesCheckbox = JBCheckBox("Validate rule references")
    private val validateRecursionCheckbox = JBCheckBox("Validate recursive rules")
    private val validateUnusedRulesCheckbox = JBCheckBox("Validate unused rules")

    private val showRailroadDiagramsAutomaticallyCheckbox = JBCheckBox("Show railroad diagrams automatically")
    private val railroadDiagramColorSchemeComboBox = ComboBox<String>(arrayOf("Default", "Dark", "Light", "Colorful"))
    private val syntaxTreeExpandDepthField = JBTextField()

    private val maxGeneratedStringLengthField = JBTextField()
    private val maxGeneratedTestCasesField = JBTextField()

    private val defaultTargetLanguageComboBox = ComboBox<String>(arrayOf("JAVA", "KOTLIN", "PYTHON", "CSHARP", "CPP"))
    private val defaultGeneratorTypeComboBox = ComboBox<String>(arrayOf("ANTLR", "JAVACC", "CUSTOM"))
    private val defaultOutputDirectoryField = JBTextField()
    private val defaultPackageNameField = JBTextField()
    private val defaultNamespaceField = JBTextField()
    private val generateVisitorByDefaultCheckbox = JBCheckBox("Generate visitor by default")
    private val generateListenerByDefaultCheckbox = JBCheckBox("Generate listener by default")
    private val generateParserOnSaveCheckbox = JBCheckBox("Generate parser on save")

    private val mainPanel: JPanel = FormBuilder.createFormBuilder()
        .addSeparator()
        .addComponent(JBLabel("Validation Settings"))
        .addComponent(validateReferencesCheckbox)
        .addComponent(validateRecursionCheckbox)
        .addComponent(validateUnusedRulesCheckbox)
        .addSeparator()
        .addComponent(JBLabel("Visualization Settings"))
        .addComponent(showRailroadDiagramsAutomaticallyCheckbox)
        .addLabeledComponent("Railroad diagram color scheme:", railroadDiagramColorSchemeComboBox)
        .addLabeledComponent("Syntax tree expand depth:", syntaxTreeExpandDepthField)
        .addSeparator()
        .addComponent(JBLabel("Test Data Generation Settings"))
        .addLabeledComponent("Maximum generated string length:", maxGeneratedStringLengthField)
        .addLabeledComponent("Maximum generated test cases:", maxGeneratedTestCasesField)
        .addSeparator()
        .addComponent(JBLabel("Build Integration Settings"))
        .addLabeledComponent("Default target language:", defaultTargetLanguageComboBox)
        .addLabeledComponent("Default generator type:", defaultGeneratorTypeComboBox)
        .addLabeledComponent("Default output directory:", defaultOutputDirectoryField)
        .addLabeledComponent("Default package name:", defaultPackageNameField)
        .addLabeledComponent("Default namespace:", defaultNamespaceField)
        .addComponent(generateVisitorByDefaultCheckbox)
        .addComponent(generateListenerByDefaultCheckbox)
        .addComponent(generateParserOnSaveCheckbox)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    /**
     * Gets the main panel containing all settings controls.
     *
     * @return The main panel.
     */
    fun getPanel(): JPanel = mainPanel

    /**
     * Gets the preferred focus component.
     *
     * @return The component that should receive focus when the settings page is opened.
     */
    fun getPreferredFocusComponent(): JComponent = validateReferencesCheckbox

    /**
     * Gets the current state of the UI controls.
     *
     * @return The settings state based on the current UI controls.
     */
    fun getSettings(): EbnfSettingsState {
        val settings = EbnfSettingsState()
        settings.validateReferences = validateReferencesCheckbox.isSelected
        settings.validateRecursion = validateRecursionCheckbox.isSelected
        settings.validateUnusedRules = validateUnusedRulesCheckbox.isSelected

        settings.showRailroadDiagramsAutomatically = showRailroadDiagramsAutomaticallyCheckbox.isSelected
        settings.railroadDiagramColorScheme = railroadDiagramColorSchemeComboBox.selectedItem as String
        settings.syntaxTreeExpandDepth = syntaxTreeExpandDepthField.text.toIntOrNull() ?: 3

        settings.maxGeneratedStringLength = maxGeneratedStringLengthField.text.toIntOrNull() ?: 100
        settings.maxGeneratedTestCases = maxGeneratedTestCasesField.text.toIntOrNull() ?: 10

        settings.defaultTargetLanguage = defaultTargetLanguageComboBox.selectedItem as String
        settings.defaultGeneratorType = defaultGeneratorTypeComboBox.selectedItem as String
        settings.defaultOutputDirectory = defaultOutputDirectoryField.text
        settings.defaultPackageName = defaultPackageNameField.text
        settings.defaultNamespace = defaultNamespaceField.text
        settings.generateVisitorByDefault = generateVisitorByDefaultCheckbox.isSelected
        settings.generateListenerByDefault = generateListenerByDefaultCheckbox.isSelected
        settings.generateParserOnSave = generateParserOnSaveCheckbox.isSelected

        return settings
    }

    /**
     * Sets the UI controls based on the provided settings state.
     *
     * @param settings The settings state to apply to the UI controls.
     */
    fun setSettings(settings: EbnfSettingsState) {
        validateReferencesCheckbox.isSelected = settings.validateReferences
        validateRecursionCheckbox.isSelected = settings.validateRecursion
        validateUnusedRulesCheckbox.isSelected = settings.validateUnusedRules

        showRailroadDiagramsAutomaticallyCheckbox.isSelected = settings.showRailroadDiagramsAutomatically
        railroadDiagramColorSchemeComboBox.selectedItem = settings.railroadDiagramColorScheme
        syntaxTreeExpandDepthField.text = settings.syntaxTreeExpandDepth.toString()

        maxGeneratedStringLengthField.text = settings.maxGeneratedStringLength.toString()
        maxGeneratedTestCasesField.text = settings.maxGeneratedTestCases.toString()

        defaultTargetLanguageComboBox.selectedItem = settings.defaultTargetLanguage
        defaultGeneratorTypeComboBox.selectedItem = settings.defaultGeneratorType
        defaultOutputDirectoryField.text = settings.defaultOutputDirectory
        defaultPackageNameField.text = settings.defaultPackageName
        defaultNamespaceField.text = settings.defaultNamespace
        generateVisitorByDefaultCheckbox.isSelected = settings.generateVisitorByDefault
        generateListenerByDefaultCheckbox.isSelected = settings.generateListenerByDefault
        generateParserOnSaveCheckbox.isSelected = settings.generateParserOnSave
    }
}
