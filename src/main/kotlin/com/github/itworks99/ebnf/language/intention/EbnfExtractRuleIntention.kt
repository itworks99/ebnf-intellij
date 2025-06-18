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
 */
class EbnfExtractRuleIntention : PsiElementBaseIntentionAction(), IntentionAction {
    override fun getText(): String = "Extract to new rule"

    override fun getFamilyName(): String = "EBNF"

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        return when (element.node.elementType) {
            EbnfElementTypes.EXPRESSION, EbnfElementTypes.TERM, EbnfElementTypes.FACTOR -> true
            else -> false
        }
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val ruleName = generateUniqueName(element)
        val newRule = createNewRule(project, ruleName, element.text)
        replaceWithReference(project, element, ruleName)
        addRuleToFile(element, newRule)
    }

    private fun generateUniqueName(element: PsiElement): String {
        val baseName = when (element.node.elementType) {
            EbnfElementTypes.EXPRESSION -> "extracted_expression"
            EbnfElementTypes.TERM -> "extracted_term"
            EbnfElementTypes.FACTOR -> "extracted_factor"
            else -> "extracted_rule"
        }
        val file = element.containingFile as EbnfFile
        val existingRuleNames = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE_NAME }
            .map { it.text }
            .toSet()
        var counter = 1
        var uniqueName = baseName
        while (existingRuleNames.contains(uniqueName)) {
            uniqueName = "${baseName}_$counter"
            counter++
        }
        return uniqueName
    }

    private fun createNewRule(project: Project, ruleName: String, ruleBody: String): PsiElement {
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.ebnf", EbnfFileType, "$ruleName = $ruleBody;") as EbnfFile
        return PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement::class.java)
            .first { it.node.elementType == EbnfElementTypes.RULE }
    }

    private fun replaceWithReference(project: Project, element: PsiElement, ruleName: String) {
        val dummyFile = PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.ebnf", EbnfFileType, "dummy = $ruleName;") as EbnfFile
        val reference = PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement::class.java)
            .first { it.node.elementType == EbnfElementTypes.ID && it.text == ruleName }
        element.replace(reference)
    }

    private fun addRuleToFile(originalElement: PsiElement, newRule: PsiElement) {
        val file = originalElement.containingFile
        val containingRule = PsiTreeUtil.getParentOfType(originalElement, PsiElement::class.java)
            ?.takeIf { it.node.elementType == EbnfElementTypes.RULE }
        if (containingRule != null) {
            file.addAfter(newRule, containingRule)
            file.addAfter(
                PsiFileFactory.getInstance(file.project)
                    .createFileFromText("dummy.ebnf", EbnfFileType, "\n")
                    .firstChild,
                containingRule
            )
        } else {
            file.add(newRule)
        }
    }
}