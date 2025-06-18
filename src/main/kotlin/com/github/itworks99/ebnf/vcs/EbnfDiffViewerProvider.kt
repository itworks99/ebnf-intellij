/**
 * Provider for EBNF diff viewer.
 */
package com.github.itworks99.ebnf.vcs

import com.github.itworks99.ebnf.language.EbnfFileType
import com.intellij.diff.DiffContext
import com.intellij.diff.DiffTool
import com.intellij.diff.DiffToolPredictor
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.ContentDiffRequest
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.tools.fragmented.UnifiedDiffTool
import com.intellij.diff.tools.simple.SimpleDiffTool
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

/**
 * Provider for EBNF diff viewer.
 * This class is responsible for providing a custom diff viewer for EBNF files.
 */
class EbnfDiffViewerProvider : DiffToolPredictor {
    
    /**
     * Predicts whether this diff tool should be used for the given request.
     *
     * @param context The diff context.
     * @param request The diff request.
     * @return The diff tool to use, or null if this provider doesn't handle the request.
     */
    override fun predict(context: DiffContext, request: DiffRequest): DiffTool? {
        if (request !is ContentDiffRequest) return null
        
        val contents = request.contents
        if (contents.size != 2) return null
        
        // Check if both contents are EBNF files
        val isEbnfFile1 = isEbnfContent(contents[0])
        val isEbnfFile2 = isEbnfContent(contents[1])
        
        if (isEbnfFile1 && isEbnfFile2) {
            thisLogger().info("Using EBNF diff viewer for EBNF files")
            return EbnfDiffTool.INSTANCE
        }
        
        return null
    }
    
    /**
     * Checks if the given content is an EBNF file.
     *
     * @param content The diff content.
     * @return True if the content is an EBNF file, false otherwise.
     */
    private fun isEbnfContent(content: DiffContent): Boolean {
        val fileType = content.contentType
        return fileType == EbnfFileType
    }
    
    /**
     * Custom diff tool for EBNF files.
     */
    private class EbnfDiffTool : SimpleDiffTool() {
        
        /**
         * Creates a viewer for the given request.
         *
         * @param context The diff context.
         * @param request The diff request.
         * @return The diff viewer.
         */
        override fun createComponent(context: DiffContext, request: DiffRequest): EbnfDiffViewer {
            return EbnfDiffViewer(context, request as ContentDiffRequest)
        }
        
        /**
         * Gets whether this diff tool can handle the given request.
         *
         * @param context The diff context.
         * @param request The diff request.
         * @return True if this diff tool can handle the request, false otherwise.
         */
        override fun canShow(context: DiffContext, request: DiffRequest): Boolean {
            if (request !is ContentDiffRequest) return false
            
            val contents = request.contents
            if (contents.size != 2) return false
            
            // Check if both contents are EBNF files
            val isEbnfFile1 = isEbnfContent(contents[0])
            val isEbnfFile2 = isEbnfContent(contents[1])
            
            return isEbnfFile1 && isEbnfFile2
        }
        
        /**
         * Checks if the given content is an EBNF file.
         *
         * @param content The diff content.
         * @return True if the content is an EBNF file, false otherwise.
         */
        private fun isEbnfContent(content: DiffContent): Boolean {
            val fileType = content.contentType
            return fileType == EbnfFileType
        }
        
        companion object {
            val INSTANCE = EbnfDiffTool()
        }
    }
    
    /**
     * Custom diff viewer for EBNF files.
     *
     * @property context The diff context.
     * @property request The diff request.
     */
    private class EbnfDiffViewer(context: DiffContext, request: ContentDiffRequest) : SimpleDiffViewer(context, request) {
        
        /**
         * Gets the project.
         *
         * @return The project.
         */
        override fun getProject(): Project? {
            return myContext.project
        }
        
        /**
         * Initializes the viewer.
         */
        override fun init() {
            super.init()
            
            // Add custom initialization here
            thisLogger().info("Initializing EBNF diff viewer")
            
            // Example: Add a custom header
            val headerPanel = DiffUtil.createMessagePanel("EBNF Grammar Diff")
            myPanel.add(headerPanel, "North")
        }
    }
}