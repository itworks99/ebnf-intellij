package com.github.itworks99.ebnf

import com.github.itworks99.ebnf.language.completion.EbnfCompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

/**
 * Tests for the EBNF code completion functionality.
 */
class EbnfCompletionContributorTest : BasePlatformTestCase() {

    /**
     * Tests that basic rule references are suggested in completion.
     */
    fun testRuleReferenceCompletion() {
        // Create a file with multiple rules
        val ebnfContent = """
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            letter = "a" | "b" | "c" ; (* simplified *)
            identifier = letter , { letter | digit } ;
            
            expression = <caret> ; (* cursor position *)
        """.trimIndent()

        // Configure the fixture with the content
        myFixture.configureByText("completion_test.ebnf", ebnfContent)

        // Trigger completion
        myFixture.complete(CompletionType.BASIC)

        // Check that rule references are suggested
        val lookupElements = myFixture.lookupElements ?: emptyArray()

        // We should see suggestions for all defined rules
        assertTrue("Should suggest 'digit' rule",
            lookupElements.any { it.lookupString == "digit" })
        assertTrue("Should suggest 'letter' rule",
            lookupElements.any { it.lookupString == "letter" })
        assertTrue("Should suggest 'identifier' rule",
            lookupElements.any { it.lookupString == "identifier" })
    }

    /**
     * Tests that EBNF operators are suggested in appropriate contexts.
     */
    fun testOperatorCompletion() {
        // Create a file with a rule that needs an operator
        val ebnfContent = """
            expression = term <caret> ; (* cursor position after term, expecting operator *)
            term = "a" | "b" ;
        """.trimIndent()

        // Configure the fixture with the content
        myFixture.configureByText("operator_completion.ebnf", ebnfContent)

        // Trigger completion
        myFixture.complete(CompletionType.BASIC)

        // Check that appropriate operators are suggested
        val lookupElements = myFixture.lookupElements ?: emptyArray()

        // We should see suggestions for operators like | and ,
        assertTrue("Should suggest '|' alternation operator",
            lookupElements.any { it.lookupString == "|" })
        assertTrue("Should suggest ',' concatenation operator",
            lookupElements.any { it.lookupString == "," })
    }

    /**
     * Tests that EBNF pattern templates are suggested at appropriate positions.
     */
    fun testPatternTemplateCompletion() {
        // Create an empty file where we'd expect pattern suggestions at the start
        val ebnfContent = "<caret>"

        // Configure the fixture with the content
        myFixture.configureByText("pattern_completion.ebnf", ebnfContent)

        // Trigger completion
        myFixture.complete(CompletionType.BASIC)

        // Check that pattern templates are suggested
        val lookupElements = myFixture.lookupElements ?: emptyArray()
        val lookupStrings = lookupElements.map { it.lookupString }

        // We should see suggestions for common EBNF patterns
        assertTrue("Should suggest some pattern templates",
            lookupStrings.any { it.contains("=") && it.contains(";") })
    }

    /**
     * Tests that completions are properly filtered based on context.
     */
    fun testContextAwareCompletion() {
        // Create a file with multiple contexts to test
        val ebnfContent = """
            rule1 = <caret1> ; (* expecting rule reference or terminal *)
            rule2 = rule1 <caret2> ; (* expecting operator *)
            <caret3> (* expecting new rule *)
        """.trimIndent()

        // Test completion at the first position (inside rule definition)
        testCompletionAtCaret(ebnfContent, "<caret1>") { fixture, elements ->
            // Should suggest rule references and possibly terminals/operators
            val lookupStrings = elements.map { it.lookupString }
            assertTrue("Should suggest rule references",
                lookupStrings.contains("rule1") || lookupStrings.contains("rule2"))
        }

        // Test completion at the second position (after rule reference)
        testCompletionAtCaret(ebnfContent, "<caret2>") { fixture, elements ->
            // Should prioritize operators
            val operatorCount = elements.count {
                it.lookupString == "," || it.lookupString == "|" || it.lookupString == ";"
            }
            assertTrue("Should suggest operators preferentially", operatorCount > 0)
        }

        // Test completion at the third position (at start of new rule)
        testCompletionAtCaret(ebnfContent, "<caret3>") { fixture, elements ->
            // Should suggest pattern templates
            val hasPatterns = elements.any {
                it.lookupString.contains("=") && it.lookupString.contains(";")
            }
            assertTrue("Should suggest pattern templates at file/rule start", hasPatterns)
        }
    }

    /**
     * Helper method to test completion at a specific caret position in the given content.
     */
    private fun testCompletionAtCaret(
        content: String,
        caretMarker: String,
        assertions: (CodeInsightTestFixture, Array<out com.intellij.codeInsight.lookup.LookupElement>) -> Unit
    ) {
        // Replace the specific caret marker with the actual caret character
        val contentWithActiveCaret = content.replace(caretMarker, "<caret>")

        // Configure the fixture with the content
        myFixture.configureByText("context_test_${caretMarker.replace("<", "").replace(">", "")}.ebnf",
            contentWithActiveCaret)

        // Trigger completion
        myFixture.complete(CompletionType.BASIC)

        // Run the assertions with the lookup elements
        val lookupElements = myFixture.lookupElements ?: emptyArray()
        assertions(myFixture, lookupElements)
    }
}
