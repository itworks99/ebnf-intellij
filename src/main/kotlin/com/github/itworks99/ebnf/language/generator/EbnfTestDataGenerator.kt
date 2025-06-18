package com.github.itworks99.ebnf.language.generator

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfTokenTypes
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

/**
 * Generator for creating test data from EBNF grammar.
 *
 * This class analyzes an EBNF grammar and generates sample strings
 * that conform to the grammar rules. It can be used for testing
 * parsers or other tools that process the language defined by the grammar.
 */
class EbnfTestDataGenerator(private val file: EbnfFile) {

    // Maximum recursion depth to prevent infinite recursion
    private val maxRecursionDepth = 5

    // Random number generator for making choices
    private val random = Random()

    /**
     * Generates test strings for all rules in the grammar.
     *
     * @param count The number of strings to generate per rule
     * @return A map of rule names to lists of generated strings
     */
    fun generateForAllRules(count: Int = 5): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()

        // Find all rules in the file
        val rules = findAllRules()

        // Generate strings for each rule
        for (rule in rules) {
            val ruleName = getRuleName(rule) ?: continue

            val strings = mutableListOf<String>()
            for (i in 0 until count) {
                // For this simplified implementation, just generate placeholder strings
                strings.add(generatePlaceholderForRule(ruleName, i))
            }

            if (strings.isNotEmpty()) {
                result[ruleName] = strings
            }
        }

        return result
    }

    /**
     * Generates a test string for the specified rule.
     *
     * @param ruleName The name of the rule to generate a string for
     * @return A string that conforms to the rule, or null if generation fails
     */
    fun generateForRule(ruleName: String): String? {
        // Find the rule in the file
        val rule = findRuleByName(ruleName) ?: return null

        // For this simplified implementation, just generate a placeholder string
        return generatePlaceholderForRule(ruleName, 0)
    }

    /**
     * Finds all rules in the file.
     */
    private fun findAllRules(): List<PsiElement> {
        return PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE }
            .toList()
    }

    /**
     * Finds a rule by name.
     */
    private fun findRuleByName(ruleName: String): PsiElement? {
        val rules = findAllRules()

        for (rule in rules) {
            if (getRuleName(rule) == ruleName) {
                return rule
            }
        }

        return null
    }

    /**
     * Gets the name of a rule.
     */
    private fun getRuleName(rule: PsiElement): String? {
        val nameElement = rule.node.findChildByType(EbnfElementTypes.RULE_NAME)
        return nameElement?.psi?.text
    }

    /**
     * Generates a placeholder string for a rule.
     * 
     * This is a simplified implementation that doesn't actually parse the grammar.
     * A full implementation would recursively traverse the grammar and generate
     * strings based on the rule definitions.
     */
    private fun generatePlaceholderForRule(ruleName: String, index: Int): String {
        return "Sample for $ruleName (${index + 1})"
    }
}
