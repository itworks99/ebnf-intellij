package com.github.itworks99.ebnf.language.intention

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfFileType
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

/**
 * Intention action to extract a part of an EBNF rule into a new rule.
 *
 * This intention allows extracting expressions, terms, or factors into a new rule,
 * which can improve readability and reusability of the grammar.
 */
class EbnfExtractRuleIntention : PsiElementBaseIntentionAction(), IntentionAction {
    override fun getText(): String = "Extract to new rule"
    
    override fun getFamilyName(): String = "EBNF"
    
    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        // Check if the element is an expression, term, or factor
        return when (element.node.elementType) {
            EbnfElementTypes.EXPRESSION, EbnfElementTypes.TERM, EbnfElementTypes.FACTOR -> true
            else -> false
        }
    }
    
    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        // Generate a unique rule name
        val ruleName = generateUniqueName(element)
        
        // Create the new rule
        val newRule = createNewRule(project, ruleName, element.text)
        
        // Replace the selected element with a reference to the new rule
        replaceWithReference(project, element, ruleName)
        
        // Add the new rule to the file
        addRuleToFile(element, newRule)
    }
    
    /**
     * Generates a unique rule name based on the element type.
     */
    private fun generateUniqueName(element: PsiElement): String {
        val baseName = when (element.node.elementType) {
            EbnfElementTypes.EXPRESSION -> "extracted_expression"
            EbnfElementTypes.TERM -> "extracted_term"
            EbnfElementTypes.FACTOR -> "extracted_factor"
            else -> "extracted_rule"
        }
        
        // Find all rule names in the file to ensure uniqueness
        val file = element.containingFile as EbnfFile
        val existingRuleNames = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE_NAME }
            .map { it.text }
            .toSet()
        
        // Add a number suffix if necessary to make the name unique
        var counter = 1
        var uniqueName = baseName
        
        while (existingRuleNames.contains(uniqueName)) {
            uniqueName = "${baseName}_$counter"
            counter++
        }
        
        return uniqueName
    }
    
    /**
     * Creates a new rule with the given name and body.
     */
    private fun createNewRule(project: Project, ruleName: String, ruleBody: String): PsiElement {
        // Create a dummy file with the new rule
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.ebnf", EbnfFileType, "$ruleName = $ruleBody;") as EbnfFile
        
        // Find the rule element in the dummy file
        return PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement::class.java)
            .first { it.node.elementType == EbnfElementTypes.RULE }
    }
    
    /**
     * Replaces the element with a reference to the new rule.
     */
    private fun replaceWithReference(project: Project, element: PsiElement, ruleName: String) {
        // Create a dummy file with a reference to the new rule
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.ebnf", EbnfFileType, "dummy = $ruleName;") as EbnfFile
        
        // Find the reference element in the dummy file
        val reference = PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement::class.java)
            .first { it.node.elementType == EbnfElementTypes.REFERENCE }
        
        // Replace the element with the reference
        element.replace(reference)
    }
    
    /**
     * Adds the new rule to the file.
     */
    private fun addRuleToFile(element: PsiElement, newRule: PsiElement) {
        // Find the containing rule
        var current: PsiElement? = element
        while (current != null && current.node.elementType != EbnfElementTypes.RULE) {
            current = current.parent
        }
        
        // Add the new rule after the containing rule
        if (current != null) {
            current.parent.addAfter(newRule, current)
        } else {
            // If no containing rule found, add to the end of the file
            element.containingFile.add(newRule)
        }
    }
}