/**
 * Configurable for EBNF plugin settings.
 */
package com.github.itworks99.ebnf.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

/**
 * Configurable for EBNF plugin settings.
 * This class connects the settings UI component with the settings service.
 */
class EbnfSettingsConfigurable : Configurable {
    private var settingsComponent: EbnfSettingsComponent? = null

    /**
     * Returns the display name of the configurable.
     *
     * @return The display name.
     */
    override fun getDisplayName(): String {
        return "EBNF Settings"
    }

    /**
     * Creates the settings component and returns its UI.
     *
     * @return The settings component UI.
     */
    override fun createComponent(): JComponent {
        settingsComponent = EbnfSettingsComponent()
        return settingsComponent!!.getPanel()
    }

    /**
     * Checks if the settings have been modified.
     *
     * @return True if the settings have been modified, false otherwise.
     */
    override fun isModified(): Boolean {
        val settings = EbnfSettingsService.getInstance().state
        val uiSettings = settingsComponent!!.getSettings()

        return settings.validateReferences != uiSettings.validateReferences ||
               settings.validateRecursion != uiSettings.validateRecursion ||
               settings.validateUnusedRules != uiSettings.validateUnusedRules ||
               settings.showRailroadDiagramsAutomatically != uiSettings.showRailroadDiagramsAutomatically ||
               settings.railroadDiagramColorScheme != uiSettings.railroadDiagramColorScheme ||
               settings.syntaxTreeExpandDepth != uiSettings.syntaxTreeExpandDepth ||
               settings.maxGeneratedStringLength != uiSettings.maxGeneratedStringLength ||
               settings.maxGeneratedTestCases != uiSettings.maxGeneratedTestCases ||
               settings.defaultTargetLanguage != uiSettings.defaultTargetLanguage ||
               settings.defaultGeneratorType != uiSettings.defaultGeneratorType ||
               settings.defaultOutputDirectory != uiSettings.defaultOutputDirectory ||
               settings.defaultPackageName != uiSettings.defaultPackageName ||
               settings.defaultNamespace != uiSettings.defaultNamespace ||
               settings.generateVisitorByDefault != uiSettings.generateVisitorByDefault ||
               settings.generateListenerByDefault != uiSettings.generateListenerByDefault ||
               settings.generateParserOnSave != uiSettings.generateParserOnSave
    }

    /**
     * Applies the settings from the UI to the settings service.
     */
    override fun apply() {
        val settings = settingsComponent!!.getSettings()
        EbnfSettingsService.getInstance().loadState(settings)
    }

    /**
     * Resets the UI to the current settings.
     */
    override fun reset() {
        val settings = EbnfSettingsService.getInstance().state
        settingsComponent!!.setSettings(settings)
    }

    /**
     * Disposes the settings component.
     */
    override fun disposeUIResources() {
        settingsComponent = null
    }
}
