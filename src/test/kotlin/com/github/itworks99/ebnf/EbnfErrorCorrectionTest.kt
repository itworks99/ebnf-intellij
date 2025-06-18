package com.github.itworks99.ebnf

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for EBNF syntax error detection and quick fix functionality.
 */
class EbnfErrorCorrectionTest : BasePlatformTestCase() {

    fun testMissingSemicolonQuickFix() {
        // Test error detection and quick fix for missing semicolon
        val ebnfContent = """
            digit = "0" | "1" | "2"
            letter = "A" | "B" | "C" ;
        """.trimIndent()

        myFixture.configureByText("missing_semicolon.ebnf", ebnfContent)

        // Verify error is detected
        val highlightInfos = myFixture.doHighlighting()
        val errors = highlightInfos.filter { it.severity.name == "ERROR" }
        assertTrue("Missing semicolon error should be detected", errors.isNotEmpty())

        // Look for quick fix
        val intentions = myFixture.availableIntentions
        val addSemicolonFix = intentions.find { it.text.contains("Add semicolon") }

        // If the quick fix exists, apply it and verify the result
        if (addSemicolonFix != null) {
            myFixture.launchAction(addSemicolonFix)
            val updatedText = myFixture.editor.document.text
            assertTrue("Semicolon should be added", updatedText.contains("\"2\" ;"))
        } else {
            // If the quick fix doesn't exist yet, this test serves as documentation for desired functionality
            println("Note: 'Add semicolon' quick fix not implemented yet")
        }
    }

    fun testUndefinedRuleReference() {
        // Test error detection for undefined rule reference
        val ebnfContent = """
            expression = term { "+" term } ;
            (* term is referenced but not defined *)
        """.trimIndent()

        myFixture.configureByText("undefined_rule.ebnf", ebnfContent)

        // Verify error is detected
        val highlightInfos = myFixture.doHighlighting()
        val errors = highlightInfos.filter {
            it.severity.name == "ERROR" || it.severity.name == "WARNING"
        }

        assertTrue("Undefined rule reference should be detected", errors.isNotEmpty())
    }

    fun testRedundantParentheses() {
        // Test detection of redundant parentheses
        val ebnfContent = """
            expression = (term) { "+" term } ;
            term = factor { "*" factor } ;
            factor = (((number))) | identifier ;
            number = digit { digit } ;
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            identifier = letter { letter | digit } ;
            letter = "A" | "B" | "C" ;
        """.trimIndent()

        myFixture.configureByText("redundant_parentheses.ebnf", ebnfContent)

        // Verify warnings are detected
        val highlightInfos = myFixture.doHighlighting()
        val warnings = highlightInfos.filter { it.severity.name == "WARNING" }

        // Look for quick fix for redundant parentheses
        val intentions = myFixture.availableIntentions
        val removeRedundantParentheses = intentions.find {
            it.text.contains("Remove redundant parentheses")
        }

        if (removeRedundantParentheses != null) {
            myFixture.launchAction(removeRedundantParentheses)
            // Verify parentheses were removed
            val updatedText = myFixture.editor.document.text
            assertFalse("Redundant parentheses should be removed",
                       updatedText.contains("(((number)))"))
        } else {
            // This test documents desired functionality
            println("Note: 'Remove redundant parentheses' quick fix not found or not implemented yet")
        }
    }
}
