package com.github.itworks99.ebnf

import com.github.itworks99.ebnf.language.EbnfFileType
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for EBNF rule renaming functionality.
 */
@TestDataPath("\$CONTENT_ROOT/src/test/testData/rename")
class EbnfRenameTest : BasePlatformTestCase() {

    /**
     * Tests renaming a rule and its references.
     */
    fun testRenameRule() {
        // Create a simple EBNF file with rule references
        val ebnfContent = """
            (* This is a simple EBNF grammar with rule references *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit } ;
            expression = number | expression, "+" , number ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Position the caret at the rule name to be renamed
        myFixture.editor.caretModel.moveToOffset(ebnfContent.indexOf("digit"))

        // Perform the rename
        myFixture.renameElementAtCaret("numeral")

        // Verify the result
        val expectedContent = """
            (* This is a simple EBNF grammar with rule references *)
            numeral = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = numeral, { numeral } ;
            expression = number | expression, "+" , number ;
        """.trimIndent()

        // Check that the file content matches the expected content after rename
        assertEquals("File content should match after rename", expectedContent, myFixture.editor.document.text)
        
        println("[DEBUG_LOG] Rule rename test completed successfully")
    }

    /**
     * Tests that renaming a rule doesn't affect unrelated rules.
     */
    fun testRenameRuleNoFalsePositives() {
        // Create an EBNF file with similar rule names
        val ebnfContent = """
            (* This tests that renaming doesn't affect similar names *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            digits = digit, { digit } ;
            digital = "digital" ;
            digitize = "digitize" ;
        """.trimIndent()

        // Configure the test fixture with the EBNF content
        myFixture.configureByText("test.ebnf", ebnfContent)

        // Position the caret at the rule name to be renamed
        myFixture.editor.caretModel.moveToOffset(ebnfContent.indexOf("digit ="))

        // Perform the rename
        myFixture.renameElementAtCaret("numeral")

        // Verify the result
        val expectedContent = """
            (* This tests that renaming doesn't affect similar names *)
            numeral = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            digits = numeral, { numeral } ;
            digital = "digital" ;
            digitize = "digitize" ;
        """.trimIndent()

        // Check that the file content matches the expected content after rename
        assertEquals("File content should match after rename", expectedContent, myFixture.editor.document.text)
        
        println("[DEBUG_LOG] Rule rename with similar names test completed successfully")
    }

    /**
     * Tests renaming a rule across multiple files.
     */
    fun testRenameRuleAcrossFiles() {
        // Create a main EBNF file
        val mainEbnfContent = """
            (* Main grammar file *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit } ;
        """.trimIndent()

        // Create a secondary EBNF file that references the main file
        val secondaryEbnfContent = """
            (* Secondary grammar file that references the main file *)
            expression = number | expression, "+" , number ;
            identifier = letter, { letter | digit } ;
        """.trimIndent()

        // Add both files to the project
        myFixture.configureByText("main.ebnf", mainEbnfContent)
        myFixture.addFileToProject("secondary.ebnf", secondaryEbnfContent)

        // Open the main file and position the caret at the rule name to be renamed
        myFixture.openFileInEditor(myFixture.findFileInTempDir("main.ebnf"))
        myFixture.editor.caretModel.moveToOffset(mainEbnfContent.indexOf("digit"))

        // Perform the rename
        myFixture.renameElementAtCaret("numeral")

        // Verify the result in the main file
        val expectedMainContent = """
            (* Main grammar file *)
            numeral = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = numeral, { numeral } ;
        """.trimIndent()

        // Check that the main file content matches the expected content after rename
        assertEquals("Main file content should match after rename", expectedMainContent, myFixture.editor.document.text)

        // Open the secondary file and check its content
        myFixture.openFileInEditor(myFixture.findFileInTempDir("secondary.ebnf"))
        
        // The secondary file should also have references updated
        val expectedSecondaryContent = """
            (* Secondary grammar file that references the main file *)
            expression = number | expression, "+" , number ;
            identifier = letter, { letter | numeral } ;
        """.trimIndent()

        // Check that the secondary file content matches the expected content after rename
        assertEquals("Secondary file content should match after rename", expectedSecondaryContent, myFixture.editor.document.text)
        
        println("[DEBUG_LOG] Rule rename across files test completed successfully")
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}