package com.github.itworks99.ebnf.language.completion

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.EbnfLanguage
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext

/**
 * Provides code completion for EBNF language.
 *
 * This class contributes completion suggestions for rule references, EBNF syntax,
 * common patterns, and context-aware completions.
 */
class EbnfCompletionContributor : CompletionContributor() {

    companion object {
        // Common EBNF patterns with descriptions
        private val EBNF_PATTERNS = listOf(
            PatternTemplate("identifier = letter { letter | digit } ;",
                "Basic identifier rule pattern", "identifier"),
            PatternTemplate("quoted_string = '\"' { any_character - '\"' } '\"' ;",
                "String literal pattern", "quoted_string"),
            PatternTemplate("whitespace = ' ' | '\\t' | '\\n' | '\\r' ;",
                "Whitespace pattern", "whitespace"),
            PatternTemplate("letter = 'a' | 'b' | ... | 'z' | 'A' | 'B' | ... | 'Z' ;",
                "Letter pattern", "letter"),
            PatternTemplate("digit = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ;",
                "Digit pattern", "digit"),
            PatternTemplate("number = digit { digit } [ '.' digit { digit } ] ;",
                "Number pattern (integer or decimal)", "number"),
            PatternTemplate("comment = '(*' { any_character - '*' | '*' any_character - ')' } '*)' ;",
                "Comment pattern", "comment")
        )
    }

    init {
        // Add context-aware rule reference completion
        extend(
            CompletionType.BASIC,
            ruleReferencePattern(),
            RuleReferenceCompletionProvider()
        )

        // Add EBNF operators and syntax with better documentation
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(EbnfLanguage),
            EbnfSyntaxCompletionProvider()
        )

        // Add common EBNF pattern templates
        extend(
            CompletionType.BASIC,
            newRulePattern(),
            EbnfPatternCompletionProvider()
        )
    }

    /**
     * Matches positions where rule references are appropriate
     */
    private fun ruleReferencePattern(): PsiElementPattern.Capture<PsiElement> {
        return PlatformPatterns.psiElement()
            .withLanguage(EbnfLanguage)
            .andNot(PlatformPatterns.psiElement().inside(PlatformPatterns.psiElement(EbnfElementTypes.RULE_NAME)))
    }

    /**
     * Matches positions where a new rule could be defined
     */
    private fun newRulePattern(): PsiElementPattern.Capture<PsiElement> {
        return PlatformPatterns.psiElement()
            .withLanguage(EbnfLanguage)
            .atStartOf(PlatformPatterns.psiFile())
            .andOr(
                PlatformPatterns.psiElement().afterLeaf(";"),
                PlatformPatterns.psiElement().andNot(
                    PlatformPatterns.psiElement().inside(PlatformPatterns.psiElement(EbnfElementTypes.RULE))
                )
            )
    }

    /**
     * Provides completion for rule references.
     * Prioritizes rules that match the current prefix and are semantically related.
     */
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
                    .filter { it.node.elementType == EbnfElementTypes.RULE_NAME }

                // Get containing rule to provide more relevant suggestions
                val containingRule = PsiTreeUtil.getParentOfType(position, PsiElement::class.java) {
                    it.node.elementType == EbnfElementTypes.RULE
                }
                val containingRuleName = containingRule?.let {
                    PsiTreeUtil.findChildOfType(it, PsiElement::class.java) {
                        child -> child.node.elementType == EbnfElementTypes.RULE_NAME
                    }?.text
                }

                val suggestedRules = mutableListOf<RuleSuggestion>()

                // Add each rule name as a completion suggestion with priority information
                for (ruleElement in ruleElements) {
                    val ruleName = ruleElement.text

                    // Skip suggesting the rule itself to avoid recursion
                    if (ruleName == containingRuleName) continue

                    // Get rule definition for documentation
                    val ruleDefinition = findRuleDefinition(ruleElement)

                    // Calculate semantic priority (for sorting)
                    val priority = calculatePriority(ruleName, containingRuleName)

                    suggestedRules.add(RuleSuggestion(ruleName, ruleDefinition, priority))
                }

                // Sort suggestions by priority
                suggestedRules.sortByDescending { it.priority }

                // Add all elements to the result
                for (suggestion in suggestedRules) {
                    result.addElement(
                        LookupElementBuilder.create(suggestion.name)
                            .withTypeText("rule")
                            .withIcon(EbnfLanguage.associatedFileType?.icon)
                            .withTailText(" (EBNF rule)", true)
                            .withPresentableText(suggestion.name)
                            .bold()
                            .withInsertHandler(RuleInsertHandler())
                    )
                }
            }
        }

        private fun findRuleDefinition(ruleNameElement: PsiElement): String {
            val rule = PsiTreeUtil.getParentOfType(ruleNameElement, PsiElement::class.java) {
                it.node.elementType == EbnfElementTypes.RULE
            }
            return rule?.text?.take(50)?.let {
                if (it.length == 50) "$it..." else it
            } ?: ""
        }

        private fun calculatePriority(ruleName: String, containingRuleName: String?): Int {
            // Rules with similar names to the containing rule get higher priority
            if (containingRuleName != null) {
                // Check for common name patterns (singular/plural, prefix/suffix)
                if (containingRuleName.startsWith(ruleName) || ruleName.startsWith(containingRuleName))
                    return 3

                // Check for relatedness (e.g., "statement" and "expression")
                val relatedPairs = listOf(
                    setOf("expression", "term", "factor", "primary"),
                    setOf("statement", "declaration"),
                    setOf("type", "class", "interface", "enum"),
                    setOf("identifier", "name", "id")
                )

                for (relatedSet in relatedPairs) {
                    if (relatedSet.contains(ruleName.toLowerCase()) &&
                        relatedSet.contains(containingRuleName.toLowerCase()))
                        return 2
                }
            }

            // Default priority
            return 1
        }
    }

    /**
     * Inserts a rule reference with proper formatting
     */
    private inner class RuleInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            // Add spaces around the rule reference if appropriate
            val document = context.document
            val offset = context.tailOffset

            // Check if we're in an expression where spaces might be needed
            val elementAtCaret = context.file.findElementAt(offset - 1)
            val needsSpace = elementAtCaret?.let {
                val parent = it.parent
                parent.elementType != EbnfElementTypes.RULE_NAME &&
                !parent.text.endsWith(" ")
            } ?: false

            if (needsSpace) {
                document.insertString(offset, " ")
            }
        }
    }

    /**
     * Provides completion for EBNF syntax elements.
     */
    private inner class EbnfSyntaxCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            // Add context-aware EBNF operators and syntax
            val operators = listOf(
                LookupElementBuilder.create("=")
                    .withTypeText("definition")
                    .withTailText(" (defines a rule)")
                    .withInsertHandler(SpaceAfterInsertHandler()),

                LookupElementBuilder.create(";")
                    .withTypeText("terminator")
                    .withTailText(" (ends a rule definition)")
                    .withInsertHandler(NewlineAfterInsertHandler()),

                LookupElementBuilder.create("|")
                    .withTypeText("alternation")
                    .withTailText(" (alternative definition)")
                    .withInsertHandler(SpaceAfterInsertHandler()),

                LookupElementBuilder.create(",")
                    .withTypeText("concatenation")
                    .withTailText(" (sequence of definitions)")
                    .withInsertHandler(SpaceAfterInsertHandler()),

                LookupElementBuilder.create("-")
                    .withTypeText("exception")
                    .withTailText(" (except definition)")
                    .withInsertHandler(SpaceAfterInsertHandler()),

                LookupElementBuilder.create("(")
                    .withTypeText("group start")
                    .withTailText(" (groups definitions)"),

                LookupElementBuilder.create(")")
                    .withTypeText("group end")
                    .withTailText(" (ends grouped definitions)"),

                LookupElementBuilder.create("[")
                    .withTypeText("option start")
                    .withTailText(" (optional definition)"),

                LookupElementBuilder.create("]")
                    .withTypeText("option end")
                    .withTailText(" (ends optional definition)"),

                LookupElementBuilder.create("{")
                    .withTypeText("repetition start")
                    .withTailText(" (repeated definition)"),

                LookupElementBuilder.create("}")
                    .withTypeText("repetition end")
                    .withTailText(" (ends repeated definition)"),

                LookupElementBuilder.create("(*")
                    .withTypeText("comment start")
                    .withTailText(" (starts a comment)"),

                LookupElementBuilder.create("*)")
                    .withTypeText("comment end")
                    .withTailText(" (ends a comment)"),

                LookupElementBuilder.create("'")
                    .withTypeText("terminal string")
                    .withTailText(" (literal text)"),

                LookupElementBuilder.create("\"")
                    .withTypeText("terminal string")
                    .withTailText(" (literal text)")
            )

            // Filter operators based on context
            val position = parameters.position
            val filteredOperators = filterOperatorsByContext(operators, position)

            result.addAllElements(filteredOperators)
        }

        private fun filterOperatorsByContext(operators: List<LookupElementBuilder>, position: PsiElement): List<LookupElementBuilder> {
            // Get parent elements to understand the context
            val parents = generateSequence(position) { it.parent }.take(5).toList()

            // Check for specific contexts and filter operators accordingly
            val insideRule = parents.any { it.node.elementType == EbnfElementTypes.RULE }
            val afterRuleName = parents.any {
                it.node.elementType == EbnfElementTypes.RULE &&
                PsiTreeUtil.findChildOfType(it, PsiElement::class.java) { child ->
                    child.node.elementType == EbnfElementTypes.RULE_NAME
                } != null &&
                PsiTreeUtil.findChildOfType(it, PsiElement::class.java) { child ->
                    child.node.elementType == EbnfElementTypes.EXPRESSION
                } == null
            }
            val insideExpression = parents.any { it.node.elementType == EbnfElementTypes.EXPRESSION }

            return when {
                // After rule name, suggest '=' operator with highest priority
                afterRuleName -> operators.filter { it.lookupString == "=" }

                // Inside expression, prioritize |, ,, (, [, {
                insideExpression -> operators.sortedByDescending {
                    when (it.lookupString) {
                        "|", ",", "(", "[", "{" -> 2
                        else -> 1
                    }
                }

                // Inside rule but after expression, prioritize ;
                insideRule && !afterRuleName && !insideExpression ->
                    operators.filter { it.lookupString == ";" }

                // Outside any rule, all operators available but prioritize rule patterns
                else -> operators
            }
        }
    }

    /**
     * Inserts a space after the operator
     */
    private inner class SpaceAfterInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val document = context.document
            val offset = context.tailOffset

            // Check if there's already a space
            if (offset < document.textLength && document.charsSequence[offset] != ' ') {
                document.insertString(offset, " ")
                context.editor.caretModel.moveToOffset(offset + 1)
            }
        }
    }

    /**
     * Inserts a newline after the character
     */
    private inner class NewlineAfterInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val document = context.document
            val offset = context.tailOffset

            // Insert newline
            document.insertString(offset, "\n")
            context.editor.caretModel.moveToOffset(offset + 1)
        }
    }

    /**
     * Provides completion for common EBNF patterns.
     */
    private inner class EbnfPatternCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            // Add pattern templates for common EBNF constructs
            for (pattern in EBNF_PATTERNS) {
                result.addElement(
                    LookupElementBuilder.create(pattern.template)
                        .withPresentableText(pattern.name)
                        .withTypeText("pattern")
                        .withTailText(" (${pattern.description})", true)
                        .withIcon(EbnfLanguage.associatedFileType?.icon)
                        .withInsertHandler(PatternInsertHandler())
                        .bold()
                )
            }
        }
    }

    /**
     * Formats the pattern properly when inserted
     */
    private inner class PatternInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            // Move cursor to the beginning of the pattern
            val document = context.document
            val editor = context.editor
            val offset = context.startOffset

            // Move cursor to a good editing position (typically after the rule name)
            val template = item.lookupString
            val ruleNameEndPos = template.indexOf('=')
            if (ruleNameEndPos > 0) {
                editor.caretModel.moveToOffset(offset + ruleNameEndPos - 1)
            }
        }
    }

    /**
     * Represents a rule suggestion with priority information for smarter completion
     */
    private data class RuleSuggestion(
        val name: String,
        val definition: String,
        val priority: Int
    )

    /**
     * Represents a common EBNF pattern template
     */
    private data class PatternTemplate(
        val template: String,
        val description: String,
        val name: String
    )
}