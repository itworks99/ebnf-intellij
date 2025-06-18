package com.github.itworks99.ebnf

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for EBNF syntax highlighting and error detection.
 * Focuses on ISO EBNF standard compliance and various EBNF syntax variations.
 */
class EbnfHighlightingTest : BasePlatformTestCase() {

    fun testIsoStandardHighlighting() {
        // Test ISO standard EBNF syntax highlighting
        val isoEbnfContent = """
            (* This is ISO standard EBNF *)
            letter = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" | "J" | "K" | "L" | "M" |
                     "N" | "O" | "P" | "Q" | "R" | "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z" ;
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            identifier = letter , { letter | digit } ;
            terminal = "'" , character , { character } , "'" | '"' , character , { character } , '"' ;
        """.trimIndent()

        myFixture.configureByText("iso_standard.ebnf", isoEbnfContent)

        // Verify no highlighting errors
        val highlightInfos = myFixture.doHighlighting()
        val errors = highlightInfos.filter { it.severity.name == "ERROR" }
        assertEquals("ISO standard EBNF should not have errors", 0, errors.size)
    }

    fun testSyntaxErrorHighlighting() {
        // Test error detection for common syntax errors
        val invalidEbnfContent = """
            (* This EBNF contains syntax errors *)
            letter = "A" | "B" | "C" ; (* Valid rule *)
            digit = "0" | "1" | "2" | "3" (* Missing semicolon *)
            identiFIER = letter { letter digit } ; (* Missing comma *)
        """.trimIndent()

        myFixture.configureByText("invalid.ebnf", invalidEbnfContent)

        // Verify syntax errors are detected
        val highlightInfos = myFixture.doHighlighting()
        val errors = highlightInfos.filter { it.severity.name == "ERROR" }
        assertFalse("Syntax errors should be detected", errors.isEmpty())
    }

    fun testEbnfVariants() {
        // Test support for EBNF variants
        val variantEbnfContent = """
            (* Testing various EBNF notation variants *)
            
            (* ISO standard *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            
            (* Variant with := instead of = *)
            letter := "A" | "B" | "C" | "D" | "E" ;
            
            (* Variant with different option syntax *)
            optional = [ "optional" ] ;
            
            (* Variant with different repetition syntax *)
            repetition = { "repeated" } ;
        """.trimIndent()

        myFixture.configureByText("variants.ebnf", variantEbnfContent)

        // This test passes if the file is parsed without errors
        val highlightInfos = myFixture.doHighlighting()
        val errors = highlightInfos.filter { it.severity.name == "ERROR" }

        // Assertion depends on which variants are actually supported
        // If all variants above are supported:
        assertEquals("EBNF variants should be recognized", 0, errors.size)

        // If some variants aren't supported yet, comment the above and use this instead:
        // assertNotNull("EBNF parsing should complete", highlightInfos)
    }
}
