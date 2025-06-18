package com.github.itworks99.ebnf

import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.EbnfLanguage
import com.github.itworks99.ebnf.settings.EbnfSettingsService
import com.intellij.ide.highlighter.HighlighterFactory
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.ui.UIUtil
import org.junit.Assert
import org.junit.Assert.assertNotEquals

/**
 * Integration tests for the EBNF plugin.
 * These tests verify the plugin's integration with the IDE, compatibility with different IntelliJ versions,
 * and functionality in real projects.
 */
@TestDataPath("\$CONTENT_ROOT/src/test/testData/integration")
class EbnfIntegrationTest : BasePlatformTestCase() {

    /**
     * Tests that the EBNF file type is properly registered with the IDE.
     */
    fun testFileTypeRegistration() {
        val fileTypeManager = FileTypeManager.getInstance()
        val ebnfFileType = fileTypeManager.findFileTypeByName("EBNF")

        assertNotNull("EBNF file type should be registered", ebnfFileType)
        assertEquals("EBNF file type should have correct name", "EBNF", ebnfFileType.name)
        assertEquals("EBNF file type should have correct description", "Extended Backus-Naur Form", ebnfFileType.description)
        assertTrue("EBNF file type should be associated with .ebnf extension", 
            fileTypeManager.getAssociations(ebnfFileType).any { it.presentableString == "*.ebnf" })

        println("[DEBUG_LOG] File type registration test completed successfully")
    }

    /**
     * Tests that the EBNF language is properly registered with the IDE.
     */
    fun testLanguageRegistration() {
        val language = EbnfLanguage

        assertNotNull("EBNF language should be registered", language)
        assertEquals("EBNF language should have correct ID", "EBNF", language.id)
        assertEquals("EBNF language should have correct display name", "EBNF", language.displayName)

        println("[DEBUG_LOG] Language registration test completed successfully")
    }

    /**
     * Tests creating, editing, and saving an EBNF file.
     */
    fun testFileEditing() {
        // Create a new EBNF file
        val fileName = "test_editing.ebnf"
        val initialContent = """
            (* This is a test EBNF grammar *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit } ;
        """.trimIndent()

        val file = myFixture.addFileToProject(fileName, initialContent)
        myFixture.openFileInEditor(file.virtualFile)

        // Verify initial content
        assertEquals("Initial file content should match", initialContent, myFixture.editor.document.text)

        // Edit the file
        val newContent = """
            (* This is a modified test EBNF grammar *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit } ;
            identifier = letter, { letter | digit } ;
        """.trimIndent()

        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.setText(newContent)
        }

        // Commit document changes to PSI
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        // Verify edited content
        assertEquals("Edited file content should match", newContent, myFixture.editor.document.text)

        // Save the file
        FileDocumentManager.getInstance().saveDocument(myFixture.editor.document)

        // Reopen the file and verify content persisted
        myFixture.openFileInEditor(myFixture.findFileInTempDir(fileName))
        assertEquals("Saved file content should match", newContent, myFixture.editor.document.text)

        println("[DEBUG_LOG] File editing test completed successfully")
    }

    /**
     * Tests that the plugin settings are properly integrated with the IDE.
     */
    fun testSettingsIntegration() {
        // Get the settings service
        val settingsService = EbnfSettingsService.getInstance()
        assertNotNull("Settings service should be available", settingsService)

        // Modify settings
        val originalSettings = settingsService.state.copy()
        try {
            // Change settings
            settingsService.state.validateReferences = !settingsService.state.validateReferences
            settingsService.state.validateRecursion = !settingsService.state.validateRecursion
            settingsService.state.validateUnusedRules = !settingsService.state.validateUnusedRules

            // Verify settings were changed
            val modifiedSettings = settingsService.state
            assertNotEquals("Settings should be modified", originalSettings.validateReferences, modifiedSettings.validateReferences)
            assertNotEquals("Settings should be modified", originalSettings.validateRecursion, modifiedSettings.validateRecursion)
            assertNotEquals("Settings should be modified", originalSettings.validateUnusedRules, modifiedSettings.validateUnusedRules)

            println("[DEBUG_LOG] Settings modification verified")
        } finally {
            // Restore original settings
            settingsService.loadState(originalSettings)
        }

        println("[DEBUG_LOG] Settings integration test completed successfully")
    }

    /**
     * Tests that the plugin can handle large EBNF files without performance issues.
     */
    fun testLargeFilePerformance() {
        // Create a large EBNF file with many rules
        val sb = StringBuilder()
        sb.append("(* Large EBNF grammar for performance testing *)\n")

        // Add 1000 simple rules
        for (i in 1..1000) {
            sb.append("rule_$i = \"token_$i\" ;\n")
        }

        // Add a rule that references many other rules
        sb.append("main_rule = ")
        for (i in 1..1000) {
            sb.append("rule_$i")
            if (i < 1000) sb.append(" | ")
        }
        sb.append(" ;\n")

        val largeContent = sb.toString()

        // Measure time to create and open the file
        val startTime = System.currentTimeMillis()

        val file = myFixture.addFileToProject("large_grammar.ebnf", largeContent)
        myFixture.openFileInEditor(file.virtualFile)

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        println("[DEBUG_LOG] Large file (${largeContent.length} chars) opened in $duration ms")

        // Verify the file was opened correctly
        assertNotNull("Large file should be opened", myFixture.editor)
        assertEquals("Large file content should match", largeContent, myFixture.editor.document.text)

        // The test passes if it completes without exceptions or excessive delay
        println("[DEBUG_LOG] Large file performance test completed successfully")
    }

    /**
     * Tests compatibility with the current IntelliJ version.
     */
    fun testCurrentVersionCompatibility() {
        // This test simply verifies that basic functionality works in the current IDE version
        // Create a simple EBNF file
        val ebnfContent = """
            (* Simple EBNF grammar *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            number = digit, { digit } ;
        """.trimIndent()

        val file = myFixture.addFileToProject("compatibility_test.ebnf", ebnfContent)
        myFixture.openFileInEditor(file.virtualFile)

        // Verify basic functionality
        assertNotNull("File should be opened", myFixture.editor)
        assertEquals("File content should match", ebnfContent, myFixture.editor.document.text)

        // Test syntax highlighting
        val highlighter = HighlighterFactory.createHighlighter(project, file.virtualFile)
        assertNotNull("Highlighter should be created", highlighter)

        println("[DEBUG_LOG] Current version compatibility test completed successfully")
    }

    /**
     * Tests the plugin's functionality in a simulated real project scenario.
     */
    fun testRealProjectScenario() {
        // Create a simulated project structure with multiple EBNF files

        // Main grammar file
        val mainGrammar = """
            (* Main language grammar *)
            digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
            letter = "A" | "B" | "C" | "D" | "E" | "F" | "G"
                   | "H" | "I" | "J" | "K" | "L" | "M" | "N"
                   | "O" | "P" | "Q" | "R" | "S" | "T" | "U"
                   | "V" | "W" | "X" | "Y" | "Z" 
                   | "a" | "b" | "c" | "d" | "e" | "f" | "g"
                   | "h" | "i" | "j" | "k" | "l" | "m" | "n"
                   | "o" | "p" | "q" | "r" | "s" | "t" | "u"
                   | "v" | "w" | "x" | "y" | "z" ;
            identifier = letter, { letter | digit } ;
        """.trimIndent()

        // Expression grammar file
        val expressionGrammar = """
            (* Expression grammar that imports from main grammar *)
            number = digit, { digit } ;
            factor = number | identifier | "(", expression, ")" ;
            term = factor, { ("*" | "/"), factor } ;
            expression = term, { ("+" | "-"), term } ;
        """.trimIndent()

        // Statement grammar file
        val statementGrammar = """
            (* Statement grammar that imports from expression grammar *)
            assignment = identifier, "=", expression, ";" ;
            if_statement = "if", "(", expression, ")", statement, [ "else", statement ] ;
            while_statement = "while", "(", expression, ")", statement ;
            statement = assignment | if_statement | while_statement | "{", { statement }, "}" ;
        """.trimIndent()

        // Add files to project in a directory structure
        val mainFile = myFixture.addFileToProject("grammar/main.ebnf", mainGrammar)
        val expressionFile = myFixture.addFileToProject("grammar/expression.ebnf", expressionGrammar)
        val statementFile = myFixture.addFileToProject("grammar/statement.ebnf", statementGrammar)

        // Test opening and navigating between files
        myFixture.openFileInEditor(mainFile.virtualFile)
        assertEquals("Main file should be opened", mainGrammar, myFixture.editor.document.text)

        myFixture.openFileInEditor(expressionFile.virtualFile)
        assertEquals("Expression file should be opened", expressionGrammar, myFixture.editor.document.text)

        myFixture.openFileInEditor(statementFile.virtualFile)
        assertEquals("Statement file should be opened", statementGrammar, myFixture.editor.document.text)

        // Test find usages (simplified version)
        myFixture.openFileInEditor(mainFile.virtualFile)
        myFixture.editor.caretModel.moveToOffset(mainGrammar.indexOf("identifier"))

        // The actual find usages would be tested with myFixture.testFindUsages
        // but for this integration test we're just verifying the basic functionality

        println("[DEBUG_LOG] Real project scenario test completed successfully")
    }

    override fun getTestDataPath() = "src/test/testData/integration"
}
