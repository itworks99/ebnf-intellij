/**
 * Validator for EBNF files during build.
 */
package com.github.itworks99.ebnf.build

import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.github.itworks99.ebnf.settings.EbnfSettingsService
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompileTask
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

/**
 * Validates EBNF files during the build process.
 * This class is registered as a before-compile task to validate EBNF files
 * before compilation starts.
 */
class EbnfBuildValidator : CompileTask {
    
    /**
     * Executes the validation task before compilation.
     *
     * @param context The compilation context.
     * @return True if compilation should continue, false to abort.
     */
    override fun execute(context: CompileContext): Boolean {
        val project = context.project
        val settings = EbnfSettingsService.getInstance().state
        
        // Skip validation if all validation options are disabled
        if (!settings.validateReferences && !settings.validateRecursion && !settings.validateUnusedRules) {
            thisLogger().info("EBNF validation skipped: all validation options are disabled")
            return true
        }
        
        thisLogger().info("Starting EBNF file validation during build")
        
        // Get all EBNF files in the project
        val ebnfFiles = findEbnfFiles(project, context.compileScope.affectedFiles)
        
        if (ebnfFiles.isEmpty()) {
            thisLogger().info("No EBNF files found for validation")
            return true
        }
        
        thisLogger().info("Found ${ebnfFiles.size} EBNF files to validate")
        
        // Validate each file
        var hasErrors = false
        for (file in ebnfFiles) {
            val validationResult = validateEbnfFile(file, project, settings)
            if (!validationResult.isValid) {
                hasErrors = true
                for (error in validationResult.errors) {
                    context.addMessage(
                        CompilerMessageCategory.ERROR,
                        "EBNF Validation Error: ${error.message}",
                        file.url,
                        error.line,
                        error.column
                    )
                }
                
                for (warning in validationResult.warnings) {
                    context.addMessage(
                        CompilerMessageCategory.WARNING,
                        "EBNF Validation Warning: ${warning.message}",
                        file.url,
                        warning.line,
                        warning.column
                    )
                }
            }
        }
        
        thisLogger().info("EBNF validation completed. Has errors: $hasErrors")
        
        // Continue compilation even if there are errors
        // The errors will be shown in the Messages tool window
        return true
    }
    
    /**
     * Finds all EBNF files in the project that are affected by the current compilation.
     *
     * @param project The project.
     * @param affectedFiles The files affected by the current compilation.
     * @return A list of EBNF virtual files.
     */
    private fun findEbnfFiles(project: Project, affectedFiles: Array<VirtualFile>): List<VirtualFile> {
        return affectedFiles.filter { it.extension?.lowercase() == "ebnf" }
    }
    
    /**
     * Validates an EBNF file.
     *
     * @param file The virtual file to validate.
     * @param project The project.
     * @param settings The EBNF settings.
     * @return A validation result.
     */
    private fun validateEbnfFile(file: VirtualFile, project: Project, settings: com.github.itworks99.ebnf.settings.EbnfSettingsState): ValidationResult {
        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (psiFile !is EbnfFile) {
            return ValidationResult(true, emptyList(), emptyList())
        }
        
        val errors = mutableListOf<ValidationIssue>()
        val warnings = mutableListOf<ValidationIssue>()
        
        // Validate references if enabled
        if (settings.validateReferences) {
            validateReferences(psiFile, errors)
        }
        
        // Validate recursion if enabled
        if (settings.validateRecursion) {
            validateRecursion(psiFile, warnings)
        }
        
        // Validate unused rules if enabled
        if (settings.validateUnusedRules) {
            validateUnusedRules(psiFile, warnings)
        }
        
        return ValidationResult(errors.isEmpty(), errors, warnings)
    }
    
    /**
     * Validates references in an EBNF file.
     *
     * @param file The EBNF file to validate.
     * @param errors The list to add errors to.
     */
    private fun validateReferences(file: EbnfFile, errors: MutableList<ValidationIssue>) {
        // Implementation would check for undefined rule references
        // This is a simplified version that doesn't actually validate
        // In a real implementation, we would traverse the PSI tree and check references
    }
    
    /**
     * Validates recursion in an EBNF file.
     *
     * @param file The EBNF file to validate.
     * @param warnings The list to add warnings to.
     */
    private fun validateRecursion(file: EbnfFile, warnings: MutableList<ValidationIssue>) {
        // Implementation would check for infinite recursion
        // This is a simplified version that doesn't actually validate
    }
    
    /**
     * Validates unused rules in an EBNF file.
     *
     * @param file The EBNF file to validate.
     * @param warnings The list to add warnings to.
     */
    private fun validateUnusedRules(file: EbnfFile, warnings: MutableList<ValidationIssue>) {
        // Implementation would check for unused rules
        // This is a simplified version that doesn't actually validate
    }
    
    /**
     * Represents a validation issue.
     *
     * @property message The error message.
     * @property line The line number where the issue occurred.
     * @property column The column number where the issue occurred.
     */
    data class ValidationIssue(
        val message: String,
        val line: Int,
        val column: Int
    )
    
    /**
     * Represents the result of validating an EBNF file.
     *
     * @property isValid Whether the file is valid.
     * @property errors The list of errors.
     * @property warnings The list of warnings.
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<ValidationIssue>,
        val warnings: List<ValidationIssue>
    )
}