package com.github.itworks99.ebnf

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for EBNF syntax validation (annotator) functionality.
 */
class EbnfAnnotatorTest : BasePlatformTestCase() {

    /**
     * Tests that valid EBNF syntax doesn't produce errors.
     */
    fun testValidSyntax() {
        // Create a valid EBNF file
        val ebnfContent = """
            (* This is a valid EBNF grammar *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit } ;
            expression = number | expression, "+" , number ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Highlight the file to trigger the annotator
        myFixture.checkHighlighting(false, false, false)

        // Verify that there are no errors
        val highlights = myFixture.doHighlighting(HighlightSeverity.ERROR)
        assertTrue("Valid syntax should not produce errors", highlights.isEmpty())
        
        println("[DEBUG_LOG] Valid syntax test completed successfully")
    }

    /**
     * Tests that missing semicolons are detected.
     */
    fun testMissingSemicolon() {
        // Create an EBNF file with a missing semicolon
        val ebnfContent = """
            (* This EBNF grammar has a missing semicolon *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit }
            expression = number | expression, "+" , number ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Highlight the file to trigger the annotator
        myFixture.checkHighlighting(false, false, false)

        // Verify that there is an error for the missing semicolon
        val highlights = myFixture.doHighlighting(HighlightSeverity.ERROR)
        assertFalse("Missing semicolon should produce an error", highlights.isEmpty())
        
        println("[DEBUG_LOG] Missing semicolon test completed successfully")
        println("[DEBUG_LOG] Found ${highlights.size} errors")
    }

    /**
     * Tests that undefined rule references are detected.
     */
    fun testUndefinedRuleReference() {
        // Create an EBNF file with an undefined rule reference
        val ebnfContent = """
            (* This EBNF grammar has an undefined rule reference *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit } ;
            expression = undefined_rule | expression, "+" , number ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Highlight the file to trigger the annotator
        myFixture.checkHighlighting(false, false, false)

        // Verify that there is an error for the undefined rule reference
        val highlights = myFixture.doHighlighting(HighlightSeverity.ERROR)
        assertFalse("Undefined rule reference should produce an error", highlights.isEmpty())
        
        // Check that the error is at the undefined rule reference
        val errorAtUndefinedRule = highlights.any { 
            it.text == "undefined_rule" && it.severity == HighlightSeverity.ERROR 
        }
        assertTrue("Error should be at the undefined rule reference", errorAtUndefinedRule)
        
        println("[DEBUG_LOG] Undefined rule reference test completed successfully")
        println("[DEBUG_LOG] Found ${highlights.size} errors")
    }

    /**
     * Tests that unbalanced brackets are detected.
     */
    fun testUnbalancedBrackets() {
        // Create an EBNF file with unbalanced brackets
        val ebnfContent = """
            (* This EBNF grammar has unbalanced brackets *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit ;
            expression = number | expression, "+" , number ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Highlight the file to trigger the annotator
        myFixture.checkHighlighting(false, false, false)

        // Verify that there is an error for the unbalanced brackets
        val highlights = myFixture.doHighlighting(HighlightSeverity.ERROR)
        assertFalse("Unbalanced brackets should produce an error", highlights.isEmpty())
        
        println("[DEBUG_LOG] Unbalanced brackets test completed successfully")
        println("[DEBUG_LOG] Found ${highlights.size} errors")
    }

    /**
     * Tests that unclosed comments are detected.
     */
    fun testUnclosedComment() {
        // Create an EBNF file with an unclosed comment
        val ebnfContent = """
            (* This EBNF grammar has an unclosed comment
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit } ;
            expression = number | expression, "+" , number ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Highlight the file to trigger the annotator
        myFixture.checkHighlighting(false, false, false)

        // Verify that there is an error for the unclosed comment
        val highlights = myFixture.doHighlighting(HighlightSeverity.ERROR)
        assertFalse("Unclosed comment should produce an error", highlights.isEmpty())
        
        println("[DEBUG_LOG] Unclosed comment test completed successfully")
        println("[DEBUG_LOG] Found ${highlights.size} errors")
    }

    /**
     * Tests that duplicate rule definitions are detected.
     */
    fun testDuplicateRuleDefinition() {
        // Create an EBNF file with duplicate rule definitions
        val ebnfContent = """
            (* This EBNF grammar has duplicate rule definitions *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit } ;
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            expression = number | expression, "+" , number ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Highlight the file to trigger the annotator
        myFixture.checkHighlighting(false, false, false)

        // Verify that there is an error for the duplicate rule definition
        val highlights = myFixture.doHighlighting(HighlightSeverity.ERROR)
        assertFalse("Duplicate rule definition should produce an error", highlights.isEmpty())
        
        // Check that the error is at the second digit rule definition
        val errorAtSecondDigitRule = highlights.any { 
            it.text.contains("digit") && it.severity == HighlightSeverity.ERROR 
        }
        assertTrue("Error should be at the duplicate rule definition", errorAtSecondDigitRule)
        
        println("[DEBUG_LOG] Duplicate rule definition test completed successfully")
        println("[DEBUG_LOG] Found ${highlights.size} errors")
    }
}