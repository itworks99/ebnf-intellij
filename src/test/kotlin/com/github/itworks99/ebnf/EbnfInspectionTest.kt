package com.github.itworks99.ebnf

import com.github.itworks99.ebnf.language.inspection.EbnfRedundantParenthesesInspection
import com.github.itworks99.ebnf.language.inspection.EbnfUnusedRuleInspection
import com.github.itworks99.ebnf.language.inspection.EbnfSimplifyRepetitionInspection
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for EBNF inspections and their quick fixes.
 */
class EbnfInspectionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    /**
     * Tests that redundant parentheses are detected and can be removed.
     */
    fun testRedundantParenthesesInspection() {
        // Register the inspection
        myFixture.enableInspections(EbnfRedundantParenthesesInspection::class.java)

        // Create a file with redundant parentheses
        val ebnfContent = """
            digit = ("0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9") ;
            redundantGroup = ("simple expression") ; (* redundant *)
        """.trimIndent()

        myFixture.configureByText("redundant_parentheses.ebnf", ebnfContent)

        // Check for highlights
        val highlights = myFixture.doHighlighting()
        assertFalse("Should detect redundant parentheses", highlights.isEmpty())

        // Find the quickfix
        val intentions = myFixture.getAllQuickFixes()
        val removeParensFix = intentions.firstOrNull {
            it.familyName.contains("Remove redundant parentheses")
        }

        assertNotNull("Quick fix should be available", removeParensFix)

        // Apply the quickfix
        if (removeParensFix != null) {
            myFixture.launchAction(removeParensFix)

            // Check that parentheses were removed
            val textAfterFix = myFixture.editor.document.text
            assertFalse("Parentheses should be removed",
                textAfterFix.contains("redundantGroup = (\"simple expression\")"))
            assertTrue("Content should be preserved",
                textAfterFix.contains("redundantGroup = \"simple expression\""))
        }
    }

    /**
     * Tests that unused rules are detected and can be removed.
     */
    fun testUnusedRuleInspection() {
        // Register the inspection
        myFixture.enableInspections(EbnfUnusedRuleInspection::class.java)

        // Create a file with unused rules
        val ebnfContent = """
            main = digit | letter ;
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            letter = "a" | "b" | "c" ; (* simplified *)
            unusedRule = "x" | "y" | "z" ; (* This rule is never referenced *)
        """.trimIndent()

        myFixture.configureByText("unused_rule.ebnf", ebnfContent)

        // Check for highlights
        val highlights = myFixture.doHighlighting()
        assertFalse("Should detect unused rule", highlights.isEmpty())

        // Find the quickfix
        val intentions = myFixture.getAllQuickFixes()
        val removeUnusedFix = intentions.firstOrNull {
            it.familyName.contains("Remove unused rule")
        }

        assertNotNull("Quick fix should be available", removeUnusedFix)

        // Apply the quickfix
        if (removeUnusedFix != null) {
            myFixture.launchAction(removeUnusedFix)

            // Check that unused rule was removed
            val textAfterFix = myFixture.editor.document.text
            assertFalse("Unused rule should be removed",
                textAfterFix.contains("unusedRule"))
            assertTrue("Used rules should be preserved",
                textAfterFix.contains("digit") && textAfterFix.contains("letter"))
        }
    }

    /**
     * Tests that repetitive patterns are detected and can be simplified.
     */
    fun testSimplifyRepetitionInspection() {
        // Register the inspection
        myFixture.enableInspections(EbnfSimplifyRepetitionInspection::class.java)

        // Create a file with repetitive patterns
        val ebnfContent = """
            main = sequence ;
            sequence = "a", "a", "a" ; (* Could be simplified to "a", {"a"} *)
        """.trimIndent()

        myFixture.configureByText("repetitive_pattern.ebnf", ebnfContent)

        // Check for highlights
        val highlights = myFixture.doHighlighting()
        assertFalse("Should detect repetitive pattern", highlights.isEmpty())

        // Find the quickfix
        val intentions = myFixture.getAllQuickFixes()
        val simplifyRepetitionFix = intentions.firstOrNull {
            it.familyName.contains("Simplify repetition")
        }

        assertNotNull("Quick fix should be available", simplifyRepetitionFix)

        // Apply the quickfix
        if (simplifyRepetitionFix != null) {
            myFixture.launchAction(simplifyRepetitionFix)

            // Check that repetition was simplified
            val textAfterFix = myFixture.editor.document.text
            assertFalse("Repetition should be simplified",
                textAfterFix.contains("\"a\", \"a\", \"a\""))
            assertTrue("Should use repetition syntax",
                textAfterFix.contains("{\"a\"}"))
        }
    }
}
