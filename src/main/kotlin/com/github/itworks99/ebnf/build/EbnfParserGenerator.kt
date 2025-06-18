/**
 * Generator for parsers from EBNF files.
 */
package com.github.itworks99.ebnf.build

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Generates parsers from EBNF grammar files.
 * Supports multiple target languages and parser generators.
 */
class EbnfParserGenerator(private val project: Project) {
    
    /**
     * Available target languages for parser generation.
     */
    enum class TargetLanguage {
        JAVA,
        KOTLIN,
        PYTHON,
        CSHARP,
        CPP
    }
    
    /**
     * Available parser generator types.
     */
    enum class GeneratorType {
        ANTLR,
        JAVACC,
        CUSTOM
    }
    
    /**
     * Configuration for parser generation.
     *
     * @property targetLanguage The target language for the generated parser.
     * @property generatorType The type of parser generator to use.
     * @property outputDirectory The directory where generated files will be placed.
     * @property packageName The package name for the generated parser (for JVM languages).
     * @property namespace The namespace for the generated parser (for C# and C++).
     * @property generateVisitor Whether to generate a visitor pattern.
     * @property generateListener Whether to generate a listener pattern.
     */
    data class GeneratorConfig(
        val targetLanguage: TargetLanguage = TargetLanguage.JAVA,
        val generatorType: GeneratorType = GeneratorType.ANTLR,
        val outputDirectory: String = "generated",
        val packageName: String = "com.example.parser",
        val namespace: String = "Example.Parser",
        val generateVisitor: Boolean = true,
        val generateListener: Boolean = true
    )
    
    /**
     * Generates a parser from an EBNF file.
     *
     * @param ebnfFile The EBNF file to generate a parser from.
     * @param config The generator configuration.
     * @param listener A listener for generation progress events.
     * @return The result of the generation process.
     */
    fun generateParser(
        ebnfFile: VirtualFile,
        config: GeneratorConfig,
        listener: ParserGenerationListener
    ): GenerationResult {
        listener.onProgress("Starting parser generation from ${ebnfFile.name}")
        thisLogger().info("Generating parser from ${ebnfFile.name} with target ${config.targetLanguage}")
        
        val psiFile = PsiManager.getInstance(project).findFile(ebnfFile)
        if (psiFile !is EbnfFile) {
            return GenerationResult(false, "Not an EBNF file: ${ebnfFile.name}", emptyList()).also {
                listener.onComplete(false, it.message)
            }
        }
        
        // Create output directory if it doesn't exist
        val outputDir = File(config.outputDirectory)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        listener.onProgress("Created output directory: ${outputDir.absolutePath}")

        // Validate the EBNF grammar
        val validator = EbnfBuildValidator()
        val validationResult = validator.validateEbnfFile(psiFile)

        if (!validationResult.isValid) {
            listener.onProgress("Validation errors found:")
            validationResult.errors.forEach { listener.onProgress(" - $it") }
            return GenerationResult(false, "Validation failed: ${validationResult.errors.first()}", emptyList()).also {
                listener.onComplete(false, it.message)
            }
        }
        listener.onProgress("EBNF grammar validation successful")

        // Convert EBNF to the target parser generator format
        listener.onProgress("Converting EBNF to ${config.generatorType} format")
        val conversionResult = when (config.generatorType) {
            GeneratorType.ANTLR -> convertToAntlr(psiFile, config, listener)
            GeneratorType.JAVACC -> convertToJavaCC(psiFile, config, listener)
            GeneratorType.CUSTOM -> convertToCustomFormat(psiFile, config, listener)
        }
        
        if (!conversionResult.success) {
            return GenerationResult(
                false,
                "Conversion failed: ${conversionResult.message}",
                emptyList()
            ).also {
                listener.onComplete(false, it.message)
            }
        }
        listener.onProgress("EBNF successfully converted to ${config.generatorType} format")

        // Generate the parser using the appropriate generator
        listener.onProgress("Generating parser with ${config.generatorType}")
        val generationResult = when (config.generatorType) {
            GeneratorType.ANTLR -> generateWithAntlr(conversionResult.grammarFile, config, listener)
            GeneratorType.JAVACC -> generateWithJavaCC(conversionResult.grammarFile, config, listener)
            GeneratorType.CUSTOM -> generateWithCustomGenerator(conversionResult.grammarFile, config, listener)
        }

        if (generationResult.success) {
            listener.onComplete(true, "Parser generated successfully: ${generationResult.generatedFiles.size} files created")
        } else {
            listener.onComplete(false, "Parser generation failed: ${generationResult.message}")
        }
        
        return generationResult
    }
    
    /**
     * Result of the EBNF to target grammar conversion process.
     */
    data class ConversionResult(
        val success: Boolean,
        val message: String,
        val grammarFile: File
    )

    /**
     * Result of the parser generation process.
     */
    data class GenerationResult(
        val success: Boolean,
        val message: String,
        val generatedFiles: List<File>
    )

    /**
     * Converts an EBNF grammar to ANTLR format.
     *
     * @param ebnfFile The EBNF file to convert.
     * @param config The generator configuration.
     * @param listener A listener for generation progress events.
     * @return The result of the conversion process.
     */
    private fun convertToAntlr(
        ebnfFile: EbnfFile,
        config: GeneratorConfig,
        listener: ParserGenerationListener
    ): ConversionResult {
        listener.onProgress("Converting EBNF to ANTLR grammar format")

        val grammarName = ebnfFile.name.substringBeforeLast(".").capitalize()
        val outputFile = File(config.outputDirectory, "$grammarName.g4")

        val grammarBuilder = StringBuilder()
        grammarBuilder.append("grammar $grammarName;\n\n")

        // Add package/header if needed
        if (config.targetLanguage == TargetLanguage.JAVA || config.targetLanguage == TargetLanguage.KOTLIN) {
            grammarBuilder.append("@header {\n")
            grammarBuilder.append("package ${config.packageName};\n")
            grammarBuilder.append("}\n\n")
        }

        // Find all rules in the EBNF file
        val rules = PsiTreeUtil.findChildrenOfType(ebnfFile, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE }

        listener.onProgress("Processing ${rules.size} grammar rules")

        // Determine the start rule (first rule in the file)
        val startRuleName = PsiTreeUtil.findChildOfType(rules.firstOrNull(), PsiElement::class.java) {
            it.node.elementType == EbnfElementTypes.RULE_NAME
        }?.text?.toLowerCase() ?: "start"

        // Process each rule
        for (rule in rules) {
            val ruleName = PsiTreeUtil.findChildOfType(rule, PsiElement::class.java) {
                it.node.elementType == EbnfElementTypes.RULE_NAME
            }?.text?.toLowerCase() ?: continue

            val ruleExpression = PsiTreeUtil.findChildOfType(rule, PsiElement::class.java) {
                it.node.elementType == EbnfElementTypes.EXPRESSION
            } ?: continue

            // Convert rule to ANTLR format
            val antlrRuleDefinition = convertEbnfRuleToAntlr(ruleName, ruleExpression)
            grammarBuilder.append(antlrRuleDefinition).append("\n\n")
        }

        // Add lexer rules for common tokens
        grammarBuilder.append("// Lexer Rules\n")
        grammarBuilder.append("WS : [ \\t\\r\\n]+ -> skip;\n")

        try {
            outputFile.writeText(grammarBuilder.toString())
            listener.onProgress("ANTLR grammar written to: ${outputFile.absolutePath}")
            return ConversionResult(true, "EBNF converted to ANTLR grammar", outputFile)
        } catch (e: Exception) {
            return ConversionResult(false, "Failed to write ANTLR grammar: ${e.message}", outputFile)
        }
    }

    /**
     * Converts an EBNF rule expression to ANTLR format.
     *
     * @param ruleName The name of the rule.
     * @param ruleExpression The rule expression to convert.
     * @return The ANTLR representation of the rule.
     */
    private fun convertEbnfRuleToAntlr(ruleName: String, ruleExpression: PsiElement): String {
        // Build ANTLR rule
        val builder = StringBuilder()
        builder.append(ruleName).append("\n    : ")

        // Convert the expression to ANTLR format
        builder.append(convertExpressionToAntlr(ruleExpression))

        // Finalize rule
        builder.append("\n    ;")

        return builder.toString()
    }

    /**
     * Converts an EBNF expression to ANTLR format.
     *
     * @param expression The expression to convert.
     * @return The ANTLR representation of the expression.
     */
    private fun convertExpressionToAntlr(expression: PsiElement): String {
        // This is a simplified conversion - a full implementation would handle all EBNF constructs
        // and translate them to their ANTLR equivalents

        // Handle different expression types
        when (expression.node.elementType) {
            EbnfElementTypes.EXPRESSION -> {
                // Handle alternation (|)
                val terms = PsiTreeUtil.findChildrenOfType(expression, PsiElement::class.java)
                    .filter { it.node.elementType == EbnfElementTypes.TERM }
                    .toList()

                return if (terms.size > 1) {
                    terms.joinToString("\n    | ") { convertExpressionToAntlr(it) }
                } else if (terms.size == 1) {
                    convertExpressionToAntlr(terms[0])
                } else {
                    "/* Empty rule */"
                }
            }
            EbnfElementTypes.TERM -> {
                // Handle concatenation (,)
                val factors = PsiTreeUtil.findChildrenOfType(expression, PsiElement::class.java)
                    .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                    .toList()

                return factors.joinToString(" ") { convertExpressionToAntlr(it) }
            }
            EbnfElementTypes.FACTOR -> {
                // Handle repetition, option, grouping
                val primary = PsiTreeUtil.findChildOfType(expression, PsiElement::class.java) {
                    it.node.elementType == EbnfElementTypes.PRIMARY
                }

                val text = expression.text
                return when {
                    text.startsWith("[") && text.endsWith("]") -> {
                        // Option [x] -> x?
                        "(${convertExpressionToAntlr(primary!!)})" + "?"
                    }
                    text.startsWith("{") && text.endsWith("}") -> {
                        // Repetition {x} -> x*
                        "(${convertExpressionToAntlr(primary!!)})" + "*"
                    }
                    text.startsWith("(") && text.endsWith(")") -> {
                        // Grouping (x) -> (x)
                        "(${convertExpressionToAntlr(primary!!)})"
                    }
                    else -> {
                        // Simple factor
                        convertExpressionToAntlr(primary!!)
                    }
                }
            }
            EbnfElementTypes.PRIMARY -> {
                // Terminal or non-terminal
                val text = expression.text
                return if (text.startsWith("'") || text.startsWith("\"")) {
                    // Terminal
                    text
                } else {
                    // Non-terminal (rule reference)
                    text.toLowerCase()
                }
            }
            else -> return expression.text
        }
    }
    
    /**
     * Converts an EBNF grammar to JavaCC format.
     *
     * @param ebnfFile The EBNF file to convert.
     * @param config The generator configuration.
     * @param listener A listener for generation progress events.
     * @return The result of the conversion process.
     */
    private fun convertToJavaCC(
        ebnfFile: EbnfFile,
        config: GeneratorConfig,
        listener: ParserGenerationListener
    ): ConversionResult {
        listener.onProgress("Converting EBNF to JavaCC format")

        val grammarName = ebnfFile.name.substringBeforeLast(".").capitalize()
        val outputFile = File(config.outputDirectory, "${grammarName}Parser.jj")

        val grammarBuilder = StringBuilder()

        // Add JavaCC options
        grammarBuilder.append("options {\n")
        grammarBuilder.append("    STATIC = false;\n")
        if (config.targetLanguage == TargetLanguage.JAVA || config.targetLanguage == TargetLanguage.KOTLIN) {
            grammarBuilder.append("    JAVA_UNICODE_ESCAPE = true;\n")
        }
        grammarBuilder.append("}\n\n")

        // Add parser declaration
        grammarBuilder.append("PARSER_BEGIN(${grammarName}Parser)\n")
        if (config.targetLanguage == TargetLanguage.JAVA) {
            grammarBuilder.append("package ${config.packageName};\n\n")
            grammarBuilder.append("public class ${grammarName}Parser {\n")
            grammarBuilder.append("    public static void main(String[] args) throws ParseException {\n")
            grammarBuilder.append("        ${grammarName}Parser parser = new ${grammarName}Parser(System.in);\n")
            grammarBuilder.append("        try {\n")
            grammarBuilder.append("            parser.start();\n")
            grammarBuilder.append("            System.out.println(\"Parsing completed successfully.\");\n")
            grammarBuilder.append("        } catch (ParseException e) {\n")
            grammarBuilder.append("            System.out.println(\"Parsing failed: \" + e.getMessage());\n")
            grammarBuilder.append("        }\n")
            grammarBuilder.append("    }\n")
            grammarBuilder.append("}\n")
        }
        grammarBuilder.append("PARSER_END(${grammarName}Parser)\n\n")

        // Add token definitions
        grammarBuilder.append("SKIP : { \" \" | \"\\t\" | \"\\n\" | \"\\r\" }\n\n")

        // Find all rules in the EBNF file
        val rules = PsiTreeUtil.findChildrenOfType(ebnfFile, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE }

        listener.onProgress("Processing ${rules.size} grammar rules")

        // Process each rule
        for (rule in rules) {
            val ruleName = PsiTreeUtil.findChildOfType(rule, PsiElement::class.java) {
                it.node.elementType == EbnfElementTypes.RULE_NAME
            }?.text ?: continue

            val ruleExpression = PsiTreeUtil.findChildOfType(rule, PsiElement::class.java) {
                it.node.elementType == EbnfElementTypes.EXPRESSION
            } ?: continue

            // Convert rule to JavaCC format
            val javaCCRuleDefinition = convertEbnfRuleToJavaCC(ruleName, ruleExpression)
            grammarBuilder.append(javaCCRuleDefinition).append("\n\n")
        }

        try {
            outputFile.writeText(grammarBuilder.toString())
            listener.onProgress("JavaCC grammar written to: ${outputFile.absolutePath}")
            return ConversionResult(true, "EBNF converted to JavaCC grammar", outputFile)
        } catch (e: Exception) {
            return ConversionResult(false, "Failed to write JavaCC grammar: ${e.message}", outputFile)
        }
    }

    /**
     * Converts an EBNF rule to JavaCC format.
     *
     * @param ruleName The name of the rule.
     * @param ruleExpression The rule expression to convert.
     * @return The JavaCC representation of the rule.
     */
    private fun convertEbnfRuleToJavaCC(ruleName: String, ruleExpression: PsiElement): String {
        val builder = StringBuilder()

        // Start rule definition
        builder.append("void ").append(ruleName).append("() : {}\n{\n    ")

        // Convert expression
        builder.append(convertExpressionToJavaCC(ruleExpression))

        // End rule definition
        builder.append("\n}")

        return builder.toString()
    }

    /**
     * Converts an EBNF expression to JavaCC format.
     *
     * @param expression The expression to convert.
     * @return The JavaCC representation of the expression.
     */
    private fun convertExpressionToJavaCC(expression: PsiElement): String {
        // Similar to ANTLR conversion but with JavaCC syntax
        when (expression.node.elementType) {
            EbnfElementTypes.EXPRESSION -> {
                val terms = PsiTreeUtil.findChildrenOfType(expression, PsiElement::class.java)
                    .filter { it.node.elementType == EbnfElementTypes.TERM }
                    .toList()

                return if (terms.size > 1) {
                    "(" + terms.joinToString("\n    | ") { convertExpressionToJavaCC(it) } + ")"
                } else if (terms.size == 1) {
                    convertExpressionToJavaCC(terms[0])
                } else {
                    "/* Empty rule */"
                }
            }
            EbnfElementTypes.TERM -> {
                val factors = PsiTreeUtil.findChildrenOfType(expression, PsiElement::class.java)
                    .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                    .toList()

                return factors.joinToString(" ") { convertExpressionToJavaCC(it) }
            }
            EbnfElementTypes.FACTOR -> {
                val primary = PsiTreeUtil.findChildOfType(expression, PsiElement::class.java) {
                    it.node.elementType == EbnfElementTypes.PRIMARY
                }

                val text = expression.text
                return when {
                    text.startsWith("[") && text.endsWith("]") -> {
                        // Option [x] -> [ x ]
                        "[ ${convertExpressionToJavaCC(primary!!)} ]"
                    }
                    text.startsWith("{") && text.endsWith("}") -> {
                        // Repetition {x} -> ( x )*
                        "( ${convertExpressionToJavaCC(primary!!)} )*"
                    }
                    text.startsWith("(") && text.endsWith(")") -> {
                        // Grouping (x) -> ( x )
                        "( ${convertExpressionToJavaCC(primary!!)} )"
                    }
                    else -> {
                        // Simple factor
                        convertExpressionToJavaCC(primary!!)
                    }
                }
            }
            EbnfElementTypes.PRIMARY -> {
                val text = expression.text
                return if (text.startsWith("'") || text.startsWith("\"")) {
                    // Terminal
                    "<TOKEN: { $text }>"
                } else {
                    // Non-terminal (rule reference)
                    "${text.toLowerCase()}()"
                }
            }
            else -> return expression.text
        }
    }
    
    /**
     * Converts an EBNF grammar to a custom format.
     *
     * @param ebnfFile The EBNF file to convert.
     * @param config The generator configuration.
     * @param listener A listener for generation progress events.
     * @return The result of the conversion process.
     */
    private fun convertToCustomFormat(
        ebnfFile: EbnfFile,
        config: GeneratorConfig,
        listener: ParserGenerationListener
    ): ConversionResult {
        // In a real implementation, this would convert to a custom format
        // For this example, we'll just write the EBNF as-is to a file
        listener.onProgress("Writing EBNF to custom format")

        val outputFile = File(config.outputDirectory, "${ebnfFile.name}.custom")
        try {
            outputFile.writeText(ebnfFile.text)
            return ConversionResult(true, "EBNF copied to custom format", outputFile)
        } catch (e: Exception) {
            return ConversionResult(false, "Failed to write custom format: ${e.message}", outputFile)
        }
    }
    
    /**
     * Generates a parser using ANTLR.
     *
     * @param grammarFile The ANTLR grammar file.
     * @param config The generator configuration.
     * @param listener A listener for generation progress events.
     * @return The result of the generation process.
     */
    private fun generateWithAntlr(
        grammarFile: File,
        config: GeneratorConfig,
        listener: ParserGenerationListener
    ): GenerationResult {
        listener.onProgress("Generating parser with ANTLR")

        // In a real implementation, this would invoke ANTLR as an external process or via its API
        // For this example, we'll simulate ANTLR outputs
        val generatedFiles = mutableListOf<File>()

        val grammarName = grammarFile.nameWithoutExtension
        val outputDir = config.outputDirectory

        try {
            // Create parser file
            val targetExt = when (config.targetLanguage) {
                TargetLanguage.JAVA -> "java"
                TargetLanguage.KOTLIN -> "kt"
                TargetLanguage.PYTHON -> "py"
                TargetLanguage.CSHARP -> "cs"
                TargetLanguage.CPP -> "cpp"
            }

            // Generate parser class
            val parserFile = File(outputDir, "${grammarName}Parser.$targetExt")
            parserFile.writeText(generateParserStub(grammarName, config))
            generatedFiles.add(parserFile)
            listener.onProgress("Generated parser class: ${parserFile.name}")

            // Generate lexer class
            val lexerFile = File(outputDir, "${grammarName}Lexer.$targetExt")
            lexerFile.writeText(generateLexerStub(grammarName, config))
            generatedFiles.add(lexerFile)
            listener.onProgress("Generated lexer class: ${lexerFile.name}")

            // Generate visitor class if requested
            if (config.generateVisitor) {
                val visitorFile = File(outputDir, "${grammarName}Visitor.$targetExt")
                visitorFile.writeText(generateVisitorStub(grammarName, config))
                generatedFiles.add(visitorFile)
                listener.onProgress("Generated visitor interface: ${visitorFile.name}")
            }

            // Generate listener class if requested
            if (config.generateListener) {
                val listenerFile = File(outputDir, "${grammarName}Listener.$targetExt")
                listenerFile.writeText(generateListenerStub(grammarName, config))
                generatedFiles.add(listenerFile)
                listener.onProgress("Generated listener interface: ${listenerFile.name}")
            }

            return GenerationResult(true, "Generated ${generatedFiles.size} files with ANTLR", generatedFiles)
        } catch (e: Exception) {
            return GenerationResult(false, "Failed to generate files with ANTLR: ${e.message}", generatedFiles)
        }
    }
    
    /**
     * Generates a parser using JavaCC.
     *
     * @param grammarFile The JavaCC grammar file.
     * @param config The generator configuration.
     * @param listener A listener for generation progress events.
     * @return The result of the generation process.
     */
    private fun generateWithJavaCC(
        grammarFile: File,
        config: GeneratorConfig,
        listener: ParserGenerationListener
    ): GenerationResult {
        listener.onProgress("Generating parser with JavaCC")

        // In a real implementation, this would invoke JavaCC as an external process
        // For this example, we'll simulate JavaCC outputs
        val generatedFiles = mutableListOf<File>()
        
        val grammarName = grammarFile.nameWithoutExtension.removeSuffix("Parser")
        val outputDir = config.outputDirectory

        try {
            // Create parser file
            val parserFile = File(outputDir, "${grammarName}Parser.java")
            parserFile.writeText("// Generated parser file for $grammarName\n")
            generatedFiles.add(parserFile)
            listener.onProgress("Generated parser class: ${parserFile.name}")

            // Create token manager
            val tokenManagerFile = File(outputDir, "${grammarName}TokenManager.java")
            tokenManagerFile.writeText("// Generated token manager for $grammarName\n")
            generatedFiles.add(tokenManagerFile)
            listener.onProgress("Generated token manager: ${tokenManagerFile.name}")

            // Create constants
            val constantsFile = File(outputDir, "${grammarName}Constants.java")
            constantsFile.writeText("// Generated constants for $grammarName\n")
            generatedFiles.add(constantsFile)
            listener.onProgress("Generated constants: ${constantsFile.name}")

            return GenerationResult(true, "Generated ${generatedFiles.size} files with JavaCC", generatedFiles)
        } catch (e: Exception) {
            return GenerationResult(false, "Failed to generate files with JavaCC: ${e.message}", generatedFiles)
        }
    }
    
    /**
     * Generates a parser using a custom generator.
     *
     * @param grammarFile The grammar file in custom format.
     * @param config The generator configuration.
     * @param listener A listener for generation progress events.
     * @return The result of the generation process.
     */
    private fun generateWithCustomGenerator(
        grammarFile: File,
        config: GeneratorConfig,
        listener: ParserGenerationListener
    ): GenerationResult {
        listener.onProgress("Generating parser with custom generator")

        // In a real implementation, this would use a custom parser generator
        // For this example, we'll create stub files
        val generatedFiles = mutableListOf<File>()
        
        val grammarName = grammarFile.nameWithoutExtension.removeSuffix(".custom")
        val outputDir = config.outputDirectory

        try {
            val targetExt = when (config.targetLanguage) {
                TargetLanguage.JAVA -> "java"
                TargetLanguage.KOTLIN -> "kt"
                TargetLanguage.PYTHON -> "py"
                TargetLanguage.CSHARP -> "cs"
                TargetLanguage.CPP -> "cpp"
            }

            // Create parser file
            val parserFile = File(outputDir, "${grammarName}Parser.$targetExt")
            parserFile.writeText("// Custom parser for $grammarName\n")
            generatedFiles.add(parserFile)
            listener.onProgress("Generated custom parser: ${parserFile.name}")

            // Create lexer file
            val lexerFile = File(outputDir, "${grammarName}Lexer.$targetExt")
            lexerFile.writeText("// Custom lexer for $grammarName\n")
            generatedFiles.add(lexerFile)
            listener.onProgress("Generated custom lexer: ${lexerFile.name}")

            return GenerationResult(true, "Generated ${generatedFiles.size} files with custom generator", generatedFiles)
        } catch (e: Exception) {
            return GenerationResult(false, "Failed to generate files with custom generator: ${e.message}", generatedFiles)
        }
    }
    
    /**
     * Generates a stub parser class.
     */
    private fun generateParserStub(grammarName: String, config: GeneratorConfig): String {
        return when (config.targetLanguage) {
            TargetLanguage.JAVA -> """
                package ${config.packageName};
                
                public class ${grammarName}Parser {
                    // Generated parser for $grammarName grammar
                    public void parse(String input) {
                        // Parsing implementation
                    }
                }
            """.trimIndent()

            TargetLanguage.KOTLIN -> """
                package ${config.packageName}
                
                class ${grammarName}Parser {
                    // Generated parser for $grammarName grammar
                    fun parse(input: String) {
                        // Parsing implementation
                    }
                }
            """.trimIndent()

            TargetLanguage.PYTHON -> """
                # Generated parser for $grammarName grammar
                class ${grammarName}Parser:
                    def parse(self, input):
                        # Parsing implementation
                        pass
            """.trimIndent()

            TargetLanguage.CSHARP -> """
                namespace ${config.namespace}
                {
                    public class ${grammarName}Parser
                    {
                        // Generated parser for $grammarName grammar
                        public void Parse(string input)
                        {
                            // Parsing implementation
                        }
                    }
                }
            """.trimIndent()

            TargetLanguage.CPP -> """
                // Generated parser for $grammarName grammar
                class ${grammarName}Parser {
                public:
                    void parse(const std::string& input) {
                        // Parsing implementation
                    }
                };
            """.trimIndent()
        }
    }

    /**
     * Generates a stub lexer class.
     */
    private fun generateLexerStub(grammarName: String, config: GeneratorConfig): String {
        return when (config.targetLanguage) {
            TargetLanguage.JAVA -> """
                package ${config.packageName};
                
                public class ${grammarName}Lexer {
                    // Generated lexer for $grammarName grammar
                    public void tokenize(String input) {
                        // Lexing implementation
                    }
                }
            """.trimIndent()

            TargetLanguage.KOTLIN -> """
                package ${config.packageName}
                
                class ${grammarName}Lexer {
                    // Generated lexer for $grammarName grammar
                    fun tokenize(input: String) {
                        // Lexing implementation
                    }
                }
            """.trimIndent()

            TargetLanguage.PYTHON -> """
                # Generated lexer for $grammarName grammar
                class ${grammarName}Lexer:
                    def tokenize(self, input):
                        # Lexing implementation
                        pass
            """.trimIndent()

            TargetLanguage.CSHARP -> """
                namespace ${config.namespace}
                {
                    public class ${grammarName}Lexer
                    {
                        // Generated lexer for $grammarName grammar
                        public void Tokenize(string input)
                        {
                            // Lexing implementation
                        }
                    }
                }
            """.trimIndent()

            TargetLanguage.CPP -> """
                // Generated lexer for $grammarName grammar
                class ${grammarName}Lexer {
                public:
                    void tokenize(const std::string& input) {
                        // Lexing implementation
                    }
                };
            """.trimIndent()
        }
    }

    /**
     * Generates a stub visitor interface.
     */
    private fun generateVisitorStub(grammarName: String, config: GeneratorConfig): String {
        return when (config.targetLanguage) {
            TargetLanguage.JAVA -> """
                package ${config.packageName};
                
                public interface ${grammarName}Visitor<T> {
                    // Visit methods for each rule
                    T visitStart(${grammarName}Parser.StartContext ctx);
                    // More visit methods would be generated here
                }
            """.trimIndent()

            TargetLanguage.KOTLIN -> """
                package ${config.packageName}
                
                interface ${grammarName}Visitor<T> {
                    // Visit methods for each rule
                    fun visitStart(ctx: ${grammarName}Parser.StartContext): T
                    // More visit methods would be generated here
                }
            """.trimIndent()

            TargetLanguage.PYTHON -> """
                # Generated visitor for $grammarName grammar
                class ${grammarName}Visitor:
                    def visit_start(self, ctx):
                        # Visit implementation
                        pass
                    # More visit methods would be generated here
            """.trimIndent()

            TargetLanguage.CSHARP -> """
                namespace ${config.namespace}
                {
                    public interface ${grammarName}Visitor<T>
                    {
                        // Visit methods for each rule
                        T VisitStart(${grammarName}Parser.StartContext ctx);
                        // More visit methods would be generated here
                    }
                }
            """.trimIndent()

            TargetLanguage.CPP -> """
                // Generated visitor for $grammarName grammar
                template <class T>
                class ${grammarName}Visitor {
                public:
                    virtual T visitStart(${grammarName}Parser::StartContext* ctx) = 0;
                    // More visit methods would be generated here
                };
            """.trimIndent()
        }
    }

    /**
     * Generates a stub listener interface.
     */
    private fun generateListenerStub(grammarName: String, config: GeneratorConfig): String {
        return when (config.targetLanguage) {
            TargetLanguage.JAVA -> """
                package ${config.packageName};
                
                public interface ${grammarName}Listener {
                    // Enter/exit methods for each rule
                    void enterStart(${grammarName}Parser.StartContext ctx);
                    void exitStart(${grammarName}Parser.StartContext ctx);
                    // More methods would be generated here
                }
            """.trimIndent()

            TargetLanguage.KOTLIN -> """
                package ${config.packageName}
                
                interface ${grammarName}Listener {
                    // Enter/exit methods for each rule
                    fun enterStart(ctx: ${grammarName}Parser.StartContext)
                    fun exitStart(ctx: ${grammarName}Parser.StartContext)
                    // More methods would be generated here
                }
            """.trimIndent()

            TargetLanguage.PYTHON -> """
                # Generated listener for $grammarName grammar
                class ${grammarName}Listener:
                    def enter_start(self, ctx):
                        pass
                        
                    def exit_start(self, ctx):
                        pass
                    # More methods would be generated here
            """.trimIndent()

            TargetLanguage.CSHARP -> """
                namespace ${config.namespace}
                {
                    public interface ${grammarName}Listener
                    {
                        // Enter/exit methods for each rule
                        void EnterStart(${grammarName}Parser.StartContext ctx);
                        void ExitStart(${grammarName}Parser.StartContext ctx);
                        // More methods would be generated here
                    }
                }
            """.trimIndent()

            TargetLanguage.CPP -> """
                // Generated listener for $grammarName grammar
                class ${grammarName}Listener {
                public:
                    virtual void enterStart(${grammarName}Parser::StartContext* ctx) {}
                    virtual void exitStart(${grammarName}Parser::StartContext* ctx) {}
                    // More methods would be generated here
                };
            """.trimIndent()
        }
    }
}

