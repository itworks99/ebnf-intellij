package com.github.itworks99.ebnf.language.completion

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfLanguage
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.ASTNode
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class EbnfCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(EbnfLanguage),
            RuleReferenceCompletionProvider()
        )
    }

    private inner class RuleReferenceCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val position = parameters.position
            val file = position.containingFile

            if (file is EbnfFile) {
                val ruleElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
                    .filter { it.node?.elementType == EbnfElementTypes.RULE_NAME }

                val containingRule = PsiTreeUtil.getParentOfType(position, PsiElement::class.java, true)
                    ?.takeIf { it.node?.elementType == EbnfElementTypes.RULE }

                val containingRuleName = containingRule?.let { rule ->
                    PsiTreeUtil.findChildrenOfType(rule, PsiElement::class.java)
                        .firstOrNull { child -> child.node?.elementType == EbnfElementTypes.RULE_NAME }
                        ?.text
                }

                val suggestedRules = ruleElements.mapNotNull { ruleElement ->
                    val ruleName = ruleElement.text
                    if (ruleName != containingRuleName) ruleName else null
                }

                for (ruleName in suggestedRules) {
                    result.addElement(
                        LookupElementBuilder.create(ruleName)
                            .withTypeText("rule")
                            .withTailText(" (EBNF rule)", true)
                    )
                }
            }
        }
    }
}