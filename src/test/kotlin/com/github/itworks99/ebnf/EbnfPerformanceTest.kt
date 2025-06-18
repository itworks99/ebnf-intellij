package com.github.itworks99.ebnf

import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.system.measureTimeMillis

/**
 * Performance tests for the EBNF plugin with large grammars.
 */
class EbnfPerformanceTest : BasePlatformTestCase() {

    /**
     * Tests parsing performance with a large grammar.
     */
    fun testLargeGrammarParsing() {
        // Create a large EBNF grammar with many rules
        val ebnfContent = generateLargeGrammar(500)

        // Measure the time it takes to parse the grammar
        val parsingTime = measureTimeMillis {
            val file = myFixture.configureByText("large_grammar.ebnf", ebnfContent)
            assertNotNull("File should be created", file)
            assertTrue("File should be an EbnfFile", file is EbnfFile)
        }

        println("[DEBUG_LOG] Parsing time for large grammar (500 rules): $parsingTime ms")

        // The parsing time should be reasonable (adjust threshold as needed)
        assertTrue("Parsing time should be reasonable", parsingTime < 10000) // 10 seconds max
    }

    /**
     * Tests syntax highlighting performance with a large grammar.
     */
    fun testLargeGrammarHighlighting() {
        // Create a large EBNF grammar with many rules
        val ebnfContent = generateLargeGrammar(200)

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("large_grammar.ebnf", ebnfContent)

        // Measure the time it takes to highlight the grammar
        val highlightingTime = measureTimeMillis {
            myFixture.checkHighlighting(false, false, false)
        }

        println("[DEBUG_LOG] Highlighting time for large grammar (200 rules): $highlightingTime ms")

        // The highlighting time should be reasonable (adjust threshold as needed)
        assertTrue("Highlighting time should be reasonable", highlightingTime < 10000) // 10 seconds max
    }

    /**
     * Tests code completion performance with a large grammar.
     */
    fun testLargeGrammarCompletion() {
        // Create a large EBNF grammar with many rules
        val ebnfBuilder = StringBuilder()
        ebnfBuilder.append("(* Large grammar for completion performance testing *)\n")

        // Add many rules
        for (i in 1..200) {
            ebnfBuilder.append("rule$i = \"value$i\" ;\n")
        }

        // Add a rule with a caret for completion
        ebnfBuilder.append("test_rule = <caret> ;\n")

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("large_grammar.ebnf", ebnfBuilder.toString())

        // Measure the time it takes to get completion suggestions
        val completionTime = measureTimeMillis {
            myFixture.completeBasic()
            val lookupElements = myFixture.lookupElementStrings ?: emptyList()
            assertTrue("Should have completion suggestions", lookupElements.isNotEmpty())
        }

        println("[DEBUG_LOG] Completion time for large grammar (200 rules): $completionTime ms")

        // The completion time should be reasonable (adjust threshold as needed)
        assertTrue("Completion time should be reasonable", completionTime < 5000) // 5 seconds max
    }

    /**
     * Tests find usages performance with a large grammar.
     */
    fun testLargeGrammarFindUsages() {
        // Create a large EBNF grammar with many rules that reference a common rule
        val ebnfBuilder = StringBuilder()
        ebnfBuilder.append("(* Large grammar for find usages performance testing *)\n")
        ebnfBuilder.append("common_rule = \"common value\" ;\n")

        // Add many rules that reference the common rule
        for (i in 1..200) {
            ebnfBuilder.append("rule$i = common_rule, \"value$i\" ;\n")
        }

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("large_grammar.ebnf", ebnfBuilder.toString())

        // Position the caret at the common rule
        myFixture.editor.caretModel.moveToOffset(ebnfBuilder.indexOf("common_rule"))

        // Measure the time it takes to find usages
        val findUsagesTime = measureTimeMillis {
            val usages = myFixture.findUsages(myFixture.elementAtCaret)
            assertTrue("Should find usages", usages.isNotEmpty())
            assertEquals("Should find 200 usages", 200, usages.size)
        }

        println("[DEBUG_LOG] Find usages time for large grammar (200 references): $findUsagesTime ms")

        // The find usages time should be reasonable (adjust threshold as needed)
        assertTrue("Find usages time should be reasonable", findUsagesTime < 5000) // 5 seconds max
    }

    /**
     * Tests file loading performance with a large grammar.
     */
    fun testLargeGrammarLoading() {
        // Create a large EBNF grammar with many rules
        val ebnfContent = generateLargeGrammar(300)

        // Measure the time it takes to load and parse the grammar
        val loadingTime = measureTimeMillis {
            val file = myFixture.configureByText("large_grammar.ebnf", ebnfContent)
            assertNotNull("File should be created", file)
            assertTrue("File should be an EbnfFile", file is EbnfFile)

            // Force PSI tree building
            file.children
        }

        println("[DEBUG_LOG] Loading time for large grammar (300 rules): $loadingTime ms")

        // The loading time should be reasonable (adjust threshold as needed)
        assertTrue("Loading time should be reasonable", loadingTime < 5000) // 5 seconds max
    }

    /**
     * Generates a large EBNF grammar with the specified number of rules.
     *
     * @param ruleCount The number of rules to generate.
     * @return The generated EBNF grammar as a string.
     */
    private fun generateLargeGrammar(ruleCount: Int): String {
        val builder = StringBuilder()
        builder.append("(* Large grammar with $ruleCount rules *)\n")

        // Add a basic set of rules that will be referenced by other rules
        builder.append("digit = \"0\" | \"1\" | \"2\" | \"3\" | \"4\" | \"5\" | \"6\" | \"7\" | \"8\" | \"9\" ;\n")
        builder.append("letter = \"A\" | \"B\" | \"C\" | \"D\" | \"E\" | \"F\" | \"G\" | \"H\" | \"I\" | \"J\" | \"K\" | \"L\" | \"M\" | \"N\" | \"O\" | \"P\" | \"Q\" | \"R\" | \"S\" | \"T\" | \"U\" | \"V\" | \"W\" | \"X\" | \"Y\" | \"Z\" ;\n")
        builder.append("identifier = letter, { letter | digit } ;\n")

        // Generate many rules that reference the basic rules
        for (i in 1..ruleCount) {
            val ruleType = i % 5
            when (ruleType) {
                0 -> builder.append("rule$i = digit, { digit } ;\n")
                1 -> builder.append("rule$i = letter, { letter } ;\n")
                2 -> builder.append("rule$i = identifier, \"_\", identifier ;\n")
                3 -> builder.append("rule$i = \"(\", rule${(i % ruleCount) + 1}, \")\" ;\n")
                4 -> builder.append("rule$i = rule${(i % ruleCount) + 1}, \"+\", rule${((i + 1) % ruleCount) + 1} ;\n")
            }
        }

        return builder.toString()
    }
}
