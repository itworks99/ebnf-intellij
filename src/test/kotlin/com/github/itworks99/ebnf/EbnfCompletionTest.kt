package com.github.itworks99.ebnf

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for EBNF code completion functionality.
 */
class EbnfCompletionTest : BasePlatformTestCase() {

    /**
     * Tests basic rule reference completion.
     */
    fun testRuleReferenceCompletion() {
        // Create a simple EBNF file with multiple rules
        val ebnfContent = """
            (* This is a simple EBNF grammar with multiple rules *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit } ;
            identifier = letter, { letter | digit } ;
            expression = number | expression, "+" , number ;
            
            statement = <caret> ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Invoke completion at caret position
        myFixture.complete(CompletionType.BASIC)

        // Get the lookup elements
        val lookupElements = myFixture.lookupElementStrings ?: emptyList()

        // Verify that all defined rules are suggested
        assertTrue("Should suggest 'digit'", lookupElements.contains("digit"))
        assertTrue("Should suggest 'number'", lookupElements.contains("number"))
        assertTrue("Should suggest 'identifier'", lookupElements.contains("identifier"))
        assertTrue("Should suggest 'expression'", lookupElements.contains("expression"))
        
        println("[DEBUG_LOG] Rule reference completion test completed successfully")
        println("[DEBUG_LOG] Completion suggestions: ${lookupElements.joinToString(", ")}")
    }

    /**
     * Tests completion of EBNF operators.
     */
    fun testOperatorCompletion() {
        // Create an EBNF file with a rule that needs an operator
        val ebnfContent = """
            (* This tests completion of EBNF operators *)
            digit = "0" <caret> "1" ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Invoke completion at caret position
        myFixture.complete(CompletionType.BASIC)

        // Get the lookup elements
        val lookupElements = myFixture.lookupElementStrings ?: emptyList()

        // Verify that EBNF operators are suggested
        assertTrue("Should suggest '|' (alternation)", lookupElements.contains("|"))
        assertTrue("Should suggest ',' (concatenation)", lookupElements.contains(","))
        
        println("[DEBUG_LOG] Operator completion test completed successfully")
        println("[DEBUG_LOG] Completion suggestions: ${lookupElements.joinToString(", ")}")
    }

    /**
     * Tests completion inside repetition brackets.
     */
    fun testRepetitionCompletion() {
        // Create an EBNF file with a rule that has a repetition
        val ebnfContent = """
            (* This tests completion inside repetition brackets *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { <caret> } ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Invoke completion at caret position
        myFixture.complete(CompletionType.BASIC)

        // Get the lookup elements
        val lookupElements = myFixture.lookupElementStrings ?: emptyList()

        // Verify that rule names are suggested inside repetition
        assertTrue("Should suggest 'digit'", lookupElements.contains("digit"))
        assertTrue("Should suggest 'number'", lookupElements.contains("number"))
        
        println("[DEBUG_LOG] Repetition completion test completed successfully")
        println("[DEBUG_LOG] Completion suggestions: ${lookupElements.joinToString(", ")}")
    }

    /**
     * Tests completion inside optional brackets.
     */
    fun testOptionalCompletion() {
        // Create an EBNF file with a rule that has an optional part
        val ebnfContent = """
            (* This tests completion inside optional brackets *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            sign = "+" | "-" ;
            number = [<caret>], digit, { digit } ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Invoke completion at caret position
        myFixture.complete(CompletionType.BASIC)

        // Get the lookup elements
        val lookupElements = myFixture.lookupElementStrings ?: emptyList()

        // Verify that rule names are suggested inside optional brackets
        assertTrue("Should suggest 'digit'", lookupElements.contains("digit"))
        assertTrue("Should suggest 'sign'", lookupElements.contains("sign"))
        
        println("[DEBUG_LOG] Optional completion test completed successfully")
        println("[DEBUG_LOG] Completion suggestions: ${lookupElements.joinToString(", ")}")
    }

    /**
     * Tests completion of rule names in a large grammar.
     */
    fun testCompletionInLargeGrammar() {
        // Create a larger EBNF grammar with many rules
        val ebnfContent = StringBuilder()
        ebnfContent.append("(* This tests completion in a larger grammar *)\n")
        
        // Add 20 rules with different names
        for (i in 1..20) {
            ebnfContent.append("rule$i = \"value$i\" ;\n")
        }
        
        // Add a rule with a caret for completion
        ebnfContent.append("test_rule = <caret> ;\n")

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent.toString())

        // Invoke completion at caret position
        myFixture.complete(CompletionType.BASIC)

        // Get the lookup elements
        val lookupElements = myFixture.lookupElementStrings ?: emptyList()

        // Verify that all rule names are suggested
        for (i in 1..20) {
            assertTrue("Should suggest 'rule$i'", lookupElements.contains("rule$i"))
        }
        
        println("[DEBUG_LOG] Large grammar completion test completed successfully")
        println("[DEBUG_LOG] Found ${lookupElements.size} completion suggestions")
    }
}