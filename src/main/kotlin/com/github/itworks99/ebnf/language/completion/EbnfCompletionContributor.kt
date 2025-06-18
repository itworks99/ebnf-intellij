package com.github.itworks99.ebnf.language.completion

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfLanguage
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

/**
 * Provides code completion for EBNF language.
 *
 * This class contributes completion suggestions for rule references and EBNF syntax.
 */
class EbnfCompletionContributor : CompletionContributor() {

    init {
        // Add completion for rule references
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(EbnfLanguage),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val position = parameters.position
                    val file = position.containingFile

                    if (file is EbnfFile) {
                        // Find all rule names in the file
                        val ruleElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
                            .filter { it.node.elementType == EbnfElementTypes.RULE_NAME }

                        // Add each rule name as a completion suggestion
                        for (ruleElement in ruleElements) {
                            val ruleName = ruleElement.text
                            result.addElement(
                                LookupElementBuilder.create(ruleName)
                                    .withTypeText("rule")
                                    .withIcon(EbnfLanguage.associatedFileType?.icon)
                            )
                        }
                    }
                }
            }
        )

        // Add completion for EBNF operators and syntax
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(EbnfLanguage),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    // Add common EBNF operators and syntax
                    val operators = listOf(
                        LookupElementBuilder.create("=").withTypeText("equals"),
                        LookupElementBuilder.create(";").withTypeText("semicolon"),
                        LookupElementBuilder.create("|").withTypeText("alternation"),
                        LookupElementBuilder.create(",").withTypeText("concatenation"),
                        LookupElementBuilder.create("-").withTypeText("exception"),
                        LookupElementBuilder.create("(").withTypeText("group start"),
                        LookupElementBuilder.create(")").withTypeText("group end"),
                        LookupElementBuilder.create("[").withTypeText("option start"),
                        LookupElementBuilder.create("]").withTypeText("option end"),
                        LookupElementBuilder.create("{").withTypeText("repetition start"),
                        LookupElementBuilder.create("}").withTypeText("repetition end"),
                        LookupElementBuilder.create("(*").withTypeText("comment start"),
                        LookupElementBuilder.create("*)").withTypeText("comment end")
                    )

                    result.addAllElements(operators)
                }
            }
        )
    }
}