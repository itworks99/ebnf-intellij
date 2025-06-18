/**
 * Data class representing the persistent state of EBNF plugin settings.
 */
package com.github.itworks99.ebnf.settings

/**
 * Represents the state of EBNF plugin settings.
 * This class holds all configurable options for the EBNF plugin.
 */
data class EbnfSettingsState(
    // Validation settings
    var validateReferences: Boolean = true,
    var validateRecursion: Boolean = true,
    var validateUnusedRules: Boolean = true,

    // Visualization settings
    var showRailroadDiagramsAutomatically: Boolean = true,
    var railroadDiagramColorScheme: String = "Default",
    var syntaxTreeExpandDepth: Int = 3,

    // Test data generation settings
    var maxGeneratedStringLength: Int = 100,
    var maxGeneratedTestCases: Int = 10,

    // Build integration settings
    var defaultTargetLanguage: String = "JAVA",
    var defaultGeneratorType: String = "ANTLR",
    var defaultOutputDirectory: String = "generated",
    var defaultPackageName: String = "com.example.parser",
    var defaultNamespace: String = "Example.Parser",
    var generateVisitorByDefault: Boolean = true,
    var generateListenerByDefault: Boolean = true,
    var generateParserOnSave: Boolean = false
)
