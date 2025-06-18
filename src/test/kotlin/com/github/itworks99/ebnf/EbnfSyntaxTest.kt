package com.github.itworks99.ebnf

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class EbnfSyntaxTest : BasePlatformTestCase() {

    fun testBasicEbnfSyntax() {
        // Create a simple EBNF file content
        val ebnfContent = """
            (* This is a simple EBNF grammar *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            letter = "A" | "B" | "C" | "D" | "E" | "F" | "G"
                   | "H" | "I" | "J" | "K" | "L" | "M" | "N"
                   | "O" | "P" | "Q" | "R" | "S" | "T" | "U"
                   | "V" | "W" | "X" | "Y" | "Z" ;
            identifier = letter , { letter | digit } ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        val file = myFixture.configureByText("test.ebnf", ebnfContent)

        // Basic verification that the file was created
        assertNotNull("File should be created", file)
        assertEquals("File content should match", ebnfContent, file.text)

        println("[DEBUG_LOG] EBNF test file created successfully")
    }

    fun testEbnfParsing() {
        // Create a simple EBNF file content with different grammar constructs
        val ebnfContent = """
            (* This tests various EBNF constructs *)

            (* Simple rule *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;

            (* Rule with concatenation *)
            number = digit , { digit } ;

            (* Rule with grouping *)
            factor = number | "(" , expression , ")" ;

            (* Rule with option *)
            term = factor , [ "*" , factor ] ;

            (* Rule with repetition *)
            expression = term , { "+" , term } ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        val file = myFixture.configureByText("test.ebnf", ebnfContent)

        // Verify that the file is an EbnfFile
        assertTrue("File should be an EbnfFile", file is EbnfFile)

        // Get the PSI tree
        val psiFile = file as EbnfFile

        // Verify that the parser created rule elements
        val rules = PsiTreeUtil.findChildrenOfType(psiFile, PsiElement::class.java)
            .filter { it.node?.elementType == EbnfElementTypes.RULE }
            .toList()

        // We should have 5 rules: digit, number, factor, term, expression
        println("[DEBUG_LOG] Found ${rules.size} rules in the EBNF file")
        assertEquals("Should have 5 rules", 5, rules.size)

        // Verify that each rule has a name and body
        for (rule in rules) {
            val ruleName = PsiTreeUtil.findChildrenOfType(rule, PsiElement::class.java)
                .find { it.node?.elementType == EbnfElementTypes.RULE_NAME }
            val ruleBody = PsiTreeUtil.findChildrenOfType(rule, PsiElement::class.java)
                .find { it.node?.elementType == EbnfElementTypes.RULE_BODY }

            assertNotNull("Rule should have a name", ruleName)
            assertNotNull("Rule should have a body", ruleBody)

            println("[DEBUG_LOG] Rule: ${ruleName?.text} has body: ${ruleBody?.text}")
        }

        // Verify that expressions are parsed correctly
        val expressions = PsiTreeUtil.findChildrenOfType(psiFile, PsiElement::class.java)
            .filter { it.node?.elementType == EbnfElementTypes.EXPRESSION }
            .toList()

        println("[DEBUG_LOG] Found ${expressions.size} expressions in the EBNF file")
        assertTrue("Should have expressions", expressions.isNotEmpty())

        // Verify that terms are parsed correctly
        val terms = PsiTreeUtil.findChildrenOfType(psiFile, PsiElement::class.java)
            .filter { it.node?.elementType == EbnfElementTypes.TERM }
            .toList()

        println("[DEBUG_LOG] Found ${terms.size} terms in the EBNF file")
        assertTrue("Should have terms", terms.isNotEmpty())
    }
}
