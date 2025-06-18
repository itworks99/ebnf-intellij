package com.github.itworks99.ebnf

import com.github.itworks99.ebnf.language.diagram.EbnfRailroadDiagramGenerator
import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import javax.swing.JComponent

/**
 * Tests for the EBNF Railroad Diagram Generator.
 */
class EbnfRailroadDiagramGeneratorTest : BasePlatformTestCase() {

    /**
     * Tests that the diagram generator creates diagrams for simple rules.
     */
    fun testSimpleRuleDiagramGeneration() {
        // Create a simple EBNF file with a single rule
        val ebnfContent = """
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        val file = myFixture.configureByText("test.ebnf", ebnfContent) as EbnfFile

        // Find the rule element
        val rule = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .firstOrNull { it.node.elementType == EbnfElementTypes.RULE }

        // Generate the diagram
        val generator = EbnfRailroadDiagramGenerator()
        assertNotNull("Rule should not be null", rule)

        if (rule != null) {
            val diagram = generator.generateDiagram(rule)
            assertNotNull("Diagram should not be null", diagram)
            assertTrue("Diagram should be a JComponent", diagram is JComponent)
            assertTrue("Diagram should have a non-zero size",
                diagram.preferredSize.width > 0 && diagram.preferredSize.height > 0)
        }
    }

    /**
     * Tests that the diagram generator handles complex rules with nested expressions.
     */
    fun testComplexRuleDiagramGeneration() {
        // Create an EBNF file with nested grammar constructs
        val ebnfContent = """
            expression = term , { ("+" | "-") , term } ;
            term = factor , { ("*" | "/") , factor } ;
            factor = number | "(" , expression , ")" ;
            number = digit , { digit } ;
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        val file = myFixture.configureByText("complex.ebnf", ebnfContent) as EbnfFile

        // Generate diagrams for all rules
        val generator = EbnfRailroadDiagramGenerator()
        val diagrams = generator.generateDiagrams(file)

        // Verify we have the right number of diagrams
        assertEquals("Should generate diagrams for all 5 rules", 5, diagrams.size)

        // Verify each diagram is valid
        for (diagram in diagrams) {
            assertNotNull("Diagram should not be null", diagram)
            assertTrue("Diagram should have a non-zero size",
                diagram.preferredSize.width > 0 && diagram.preferredSize.height > 0)
        }
    }

    /**
     * Tests that the diagram generator properly handles rules with repetition.
     */
    fun testRepetitionRuleDiagramGeneration() {
        // Create an EBNF file with repetition
        val ebnfContent = """
            identifier = letter , { letter | digit } ;
            letter = "a" | "b" | "c" ; (* Simplified for testing *)
            digit = "0" | "1" | "2" ; (* Simplified for testing *)
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        val file = myFixture.configureByText("repetition.ebnf", ebnfContent) as EbnfFile

        // Find the identifier rule
        val idRule = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .firstOrNull {
                it.node.elementType == EbnfElementTypes.RULE &&
                it.text.startsWith("identifier")
            }

        // Generate and verify the diagram
        val generator = EbnfRailroadDiagramGenerator()
        assertNotNull("Rule should not be null", idRule)

        if (idRule != null) {
            val diagram = generator.generateDiagram(idRule)
            assertNotNull("Diagram should not be null", diagram)
            assertTrue("Diagram should be a JComponent", diagram is JComponent)
        }
    }

    /**
     * Tests that the diagram generator handles empty or invalid input gracefully.
     */
    fun testInvalidInputHandling() {
        // Create an empty file
        val file = myFixture.configureByText("empty.ebnf", "") as EbnfFile

        // Attempt to generate diagrams
        val generator = EbnfRailroadDiagramGenerator()
        val diagrams = generator.generateDiagrams(file)

        // Verify result
        assertTrue("Should return empty list for empty file", diagrams.isEmpty())
    }
}
