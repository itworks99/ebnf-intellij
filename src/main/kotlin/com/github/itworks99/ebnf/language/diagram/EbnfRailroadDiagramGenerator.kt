package com.github.itworks99.ebnf.language.diagram

import com.github.itworks99.ebnf.language.EbnfElementTypes
import com.github.itworks99.ebnf.language.psi.EbnfFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.Dimension
import java.awt.Graphics
import java.awt.BasicStroke
import java.awt.Font
import java.awt.FontMetrics
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/**
 * Generates railroad diagrams for EBNF grammar rules.
 *
 * This class converts EBNF rules into visual railroad diagrams that
 * represent the grammar structure in a graphical form.
 */
class EbnfRailroadDiagramGenerator {
    /**
     * Generates a railroad diagram component for a rule.
     */
    fun generateDiagram(rule: PsiElement): JComponent {
        // Find the rule name
        val ruleNameElement = PsiTreeUtil.findChildrenOfType(rule, PsiElement::class.java)
            .firstOrNull { it.node.elementType == EbnfElementTypes.RULE_NAME }
        
        // Find the rule body
        val ruleBodyElement = PsiTreeUtil.findChildrenOfType(rule, PsiElement::class.java)
            .firstOrNull { it.node.elementType == EbnfElementTypes.RULE_BODY }
        
        if (ruleNameElement == null || ruleBodyElement == null) {
            return createEmptyDiagram()
        }
        
        val ruleName = ruleNameElement.text
        
        // Create a diagram component
        return RailroadDiagramComponent(ruleName, ruleBodyElement)
    }
    
    /**
     * Generates railroad diagrams for all rules in a file.
     */
    fun generateDiagrams(file: EbnfFile): List<JComponent> {
        // Find all rules in the file
        val rules = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .filter { it.node.elementType == EbnfElementTypes.RULE }
            .toList()
        
        // Generate a diagram for each rule
        return rules.map { generateDiagram(it) }
    }
    
    /**
     * Exports a diagram as an image file.
     */
    fun exportDiagram(diagram: JComponent, filePath: String) {
        val width = diagram.preferredSize.width
        val height = diagram.preferredSize.height
        
        // Create a buffered image using UIUtil
        val image = UIUtil.createImage(diagram, width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        // Paint the diagram on the image
        diagram.paint(g2d)
        
        // Save the image to a file
        ImageIO.write(image, "PNG", File(filePath))
        
        // Dispose the graphics context
        g2d.dispose()
    }
    
    /**
     * Creates an empty diagram component.
     */
    private fun createEmptyDiagram(): JComponent {
        return JPanel().apply {
            preferredSize = Dimension(200, 100)
        }
    }
    
    /**
     * Component that renders a railroad diagram for an EBNF rule.
     */
    private inner class RailroadDiagramComponent(
        private val ruleName: String,
        private val ruleBody: PsiElement
    ) : JComponent() {
        // Diagram layout constants
        private val padding = 20
        private val nodeHeight = 30
        private val nodeSpacing = 20
        private val trackHeight = 20
        private val cornerRadius = 10
        private val arrowSize = 5
        
        // Colors
        private val backgroundColor = JBColor.WHITE
        private val trackColor = Gray._100
        private val nodeColor = JBColor(0xE0E0E0, 0xE0E0E0)
        private val textColor = JBColor.BLACK
        private val ruleNameColor = JBColor(0x3F51B5, 0x3F51B5)
        
        // Font
        private val font = Font("SansSerif", Font.PLAIN, 12)
        private val titleFont = Font("SansSerif", Font.BOLD, 14)
        
        init {
            // Set the preferred size based on the complexity of the rule
            val width = calculateWidth(ruleBody) + 2 * padding
            val height = calculateHeight(ruleBody) + 2 * padding
            preferredSize = Dimension(width, height)
        }
        
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D
            
            // Set rendering hints for better quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            
            // Fill background
            g2d.color = backgroundColor
            g2d.fillRect(0, 0, width, height)
            
            // Draw rule name
            g2d.font = titleFont
            g2d.color = ruleNameColor
            g2d.drawString(ruleName, padding, padding + g2d.fontMetrics.ascent)
            
            // Draw the diagram
            g2d.translate(padding, padding + g2d.fontMetrics.height + 10)
            drawDiagram(g2d, ruleBody)
        }
        
        /**
         * Draws the railroad diagram for a rule body.
         */
        private fun drawDiagram(g2d: Graphics2D, element: PsiElement) {
            g2d.font = font
            g2d.color = trackColor
            g2d.stroke = BasicStroke(2f)
            
            // Start with a horizontal line
            val startX = 0
            val startY = nodeHeight / 2
            val endX = calculateWidth(element)
            
            // Draw the main track
            g2d.drawLine(startX, startY, startX + 20, startY)
            g2d.drawLine(endX - 20, startY, endX, startY)
            
            // Draw an arrow at the end
            drawArrow(g2d, endX, startY)
            
            // Draw the rule body
            when (element.node.elementType) {
                EbnfElementTypes.RULE_BODY -> {
                    // Find the expression in the rule body
                    val expression = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
                        .firstOrNull { it.node.elementType == EbnfElementTypes.EXPRESSION }
                    
                    if (expression != null) {
                        drawExpression(g2d, expression, startX + 20, startY, endX - 20)
                    }
                }
                else -> {
                    // Draw a simple node for other elements
                    drawNode(g2d, element.text, startX + 20, startY, endX - 40)
                }
            }
        }
        
        /**
         * Draws an expression (alternation of terms).
         */
        private fun drawExpression(g2d: Graphics2D, expression: PsiElement, startX: Int, startY: Int, endX: Int) {
            // Find all terms in the expression
            val terms = PsiTreeUtil.findChildrenOfType(expression, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.TERM }
                .toList()
            
            if (terms.isEmpty()) {
                return
            }
            
            if (terms.size == 1) {
                // If there's only one term, draw it directly
                drawTerm(g2d, terms[0], startX, startY, endX)
            } else {
                // If there are multiple terms, draw them as alternatives
                val trackSpacing = nodeHeight + 10
                val totalHeight = terms.size * trackSpacing
                
                // Draw vertical lines at the start and end
                g2d.drawLine(startX, startY, startX, startY + totalHeight / 2)
                g2d.drawLine(endX, startY, endX, startY + totalHeight / 2)
                
                // Draw each term on its own track
                for (i in terms.indices) {
                    val trackY = startY + i * trackSpacing
                    
                    // Draw horizontal connector to the term
                    g2d.drawLine(startX, trackY, startX + 20, trackY)
                    g2d.drawLine(endX - 20, trackY, endX, trackY)
                    
                    // Draw the term
                    drawTerm(g2d, terms[i], startX + 20, trackY, endX - 20)
                }
            }
        }
        
        /**
         * Draws a term (concatenation of factors).
         */
        private fun drawTerm(g2d: Graphics2D, term: PsiElement, startX: Int, startY: Int, endX: Int) {
            // Find all factors in the term
            val factors = PsiTreeUtil.findChildrenOfType(term, PsiElement::class.java)
                .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                .toList()
            
            if (factors.isEmpty()) {
                return
            }
            
            // Calculate the width for each factor
            val factorWidth = (endX - startX) / factors.size
            
            // Draw each factor
            for (i in factors.indices) {
                val factorStartX = startX + i * factorWidth
                val factorEndX = factorStartX + factorWidth
                
                drawFactor(g2d, factors[i], factorStartX, startY, factorEndX)
            }
        }
        
        /**
         * Draws a factor (primary with optional exception).
         */
        private fun drawFactor(g2d: Graphics2D, factor: PsiElement, startX: Int, startY: Int, endX: Int) {
            // Find the primary in the factor
            val primary = PsiTreeUtil.findChildrenOfType(factor, PsiElement::class.java)
                .firstOrNull { it.node.elementType == EbnfElementTypes.PRIMARY }
            
            if (primary != null) {
                drawPrimary(g2d, primary, startX, startY, endX)
            }
        }
        
        /**
         * Draws a primary (identifier, string literal, group, option, or repetition).
         */
        private fun drawPrimary(g2d: Graphics2D, primary: PsiElement, startX: Int, startY: Int, endX: Int) {
            // Find what's inside the primary
            val reference = PsiTreeUtil.findChildrenOfType(primary, PsiElement::class.java)
                .firstOrNull { it.node.elementType == EbnfElementTypes.REFERENCE }
            
            val group = PsiTreeUtil.findChildrenOfType(primary, PsiElement::class.java)
                .firstOrNull { it.node.elementType == EbnfElementTypes.GROUP }
            
            val option = PsiTreeUtil.findChildrenOfType(primary, PsiElement::class.java)
                .firstOrNull { it.node.elementType == EbnfElementTypes.OPTION }
            
            val repetition = PsiTreeUtil.findChildrenOfType(primary, PsiElement::class.java)
                .firstOrNull { it.node.elementType == EbnfElementTypes.REPETITION }
            
            when {
                reference != null -> {
                    // Draw a reference node
                    drawNode(g2d, reference.text, startX, startY, endX - startX)
                }
                group != null -> {
                    // Draw a group (expression in parentheses)
                    val expression = PsiTreeUtil.findChildrenOfType(group, PsiElement::class.java)
                        .firstOrNull { it.node.elementType == EbnfElementTypes.EXPRESSION }
                    
                    if (expression != null) {
                        drawExpression(g2d, expression, startX, startY, endX)
                    }
                }
                option != null -> {
                    // Draw an option (expression in square brackets)
                    val expression = PsiTreeUtil.findChildrenOfType(option, PsiElement::class.java)
                        .firstOrNull { it.node.elementType == EbnfElementTypes.EXPRESSION }
                    
                    if (expression != null) {
                        // Draw a bypass track
                        val midY = startY + nodeHeight
                        g2d.drawLine(startX, startY, startX, midY)
                        g2d.drawLine(startX, midY, endX, midY)
                        g2d.drawLine(endX, midY, endX, startY)
                        
                        // Draw the optional expression
                        drawExpression(g2d, expression, startX + 20, startY, endX - 20)
                    }
                }
                repetition != null -> {
                    // Draw a repetition (expression in curly braces)
                    val expression = PsiTreeUtil.findChildrenOfType(repetition, PsiElement::class.java)
                        .firstOrNull { it.node.elementType == EbnfElementTypes.EXPRESSION }
                    
                    if (expression != null) {
                        // Draw a loop back
                        val midY = startY + nodeHeight
                        g2d.drawLine(startX, startY, startX, midY)
                        g2d.drawLine(startX, midY, endX, midY)
                        g2d.drawLine(endX, midY, endX, startY)
                        
                        // Draw an arrow on the loop back
                        drawArrow(g2d, startX, midY, -arrowSize, 0)
                        
                        // Draw the repeating expression
                        drawExpression(g2d, expression, startX + 20, startY, endX - 20)
                    }
                }
                else -> {
                    // Draw a simple node for other elements
                    drawNode(g2d, primary.text, startX, startY, endX - startX)
                }
            }
        }
        
        /**
         * Draws a node with text.
         */
        private fun drawNode(g2d: Graphics2D, text: String, x: Int, y: Int, width: Int) {
            val metrics = g2d.fontMetrics
            val textWidth = metrics.stringWidth(text)
            val nodeWidth = Math.max(textWidth + 20, width)
            
            // Draw the node background
            g2d.color = nodeColor
            val shape = RoundRectangle2D.Float(
                x.toFloat(), (y - nodeHeight / 2).toFloat(),
                nodeWidth.toFloat(), nodeHeight.toFloat(),
                cornerRadius.toFloat(), cornerRadius.toFloat()
            )
            g2d.fill(shape)
            
            // Draw the node border
            g2d.color = trackColor
            g2d.draw(shape)
            
            // Draw the text
            g2d.color = textColor
            val textX = x + (nodeWidth - textWidth) / 2
            val textY = y + metrics.ascent / 2
            g2d.drawString(text, textX, textY)
        }
        
        /**
         * Draws an arrow.
         */
        private fun drawArrow(g2d: Graphics2D, x: Int, y: Int, dx: Int = arrowSize, dy: Int = 0) {
            val oldColor = g2d.color
            g2d.color = trackColor
            
            // Draw the arrowhead
            val x1 = x - dx - arrowSize
            val y1 = y - arrowSize
            val x2 = x - dx
            val y2 = y
            val x3 = x - dx - arrowSize
            val y3 = y + arrowSize
            
            val xPoints = intArrayOf(x1, x2, x3)
            val yPoints = intArrayOf(y1, y2, y3)
            
            g2d.fillPolygon(xPoints, yPoints, 3)
            g2d.color = oldColor
        }
        
        /**
         * Calculates the width needed for a diagram element.
         */
        private fun calculateWidth(element: PsiElement): Int {
            return when (element.node.elementType) {
                EbnfElementTypes.RULE_BODY -> {
                    val expression = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
                        .firstOrNull { it.node.elementType == EbnfElementTypes.EXPRESSION }
                    
                    if (expression != null) {
                        calculateWidth(expression)
                    } else {
                        200 // Default width
                    }
                }
                EbnfElementTypes.EXPRESSION -> {
                    val terms = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
                        .filter { it.node.elementType == EbnfElementTypes.TERM }
                        .toList()
                    
                    if (terms.isEmpty()) {
                        200 // Default width
                    } else if (terms.size == 1) {
                        calculateWidth(terms[0])
                    } else {
                        // For alternation, use the width of the widest term
                        terms.maxOf { calculateWidth(it) } + 40
                    }
                }
                EbnfElementTypes.TERM -> {
                    val factors = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
                        .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                        .toList()
                    
                    if (factors.isEmpty()) {
                        200 // Default width
                    } else {
                        // For concatenation, sum the widths of all factors
                        factors.sumOf { calculateWidth(it) }
                    }
                }
                EbnfElementTypes.FACTOR, EbnfElementTypes.PRIMARY -> {
                    // For factors and primaries, use a fixed width based on the text length
                    val metrics = getFontMetrics(font)
                    metrics.stringWidth(element.text) + 40
                }
                else -> {
                    // For other elements, use a default width
                    200
                }
            }
        }
        
        /**
         * Calculates the height needed for a diagram element.
         */
        private fun calculateHeight(element: PsiElement): Int {
            return when (element.node.elementType) {
                EbnfElementTypes.RULE_BODY -> {
                    val expression = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
                        .firstOrNull { it.node.elementType == EbnfElementTypes.EXPRESSION }
                    
                    if (expression != null) {
                        calculateHeight(expression) + 40 // Add space for the rule name
                    } else {
                        nodeHeight + 40 // Default height plus space for the rule name
                    }
                }
                EbnfElementTypes.EXPRESSION -> {
                    val terms = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
                        .filter { it.node.elementType == EbnfElementTypes.TERM }
                        .toList()
                    
                    if (terms.isEmpty()) {
                        nodeHeight // Default height
                    } else if (terms.size == 1) {
                        calculateHeight(terms[0])
                    } else {
                        // For alternation, sum the heights of all terms
                        terms.sumOf { calculateHeight(it) } + (terms.size - 1) * nodeSpacing
                    }
                }
                EbnfElementTypes.TERM -> {
                    val factors = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
                        .filter { it.node.elementType == EbnfElementTypes.FACTOR }
                        .toList()
                    
                    if (factors.isEmpty()) {
                        nodeHeight // Default height
                    } else {
                        // For concatenation, use the height of the tallest factor
                        factors.maxOf { calculateHeight(it) }
                    }
                }
                EbnfElementTypes.FACTOR, EbnfElementTypes.PRIMARY -> {
                    // For factors and primaries, use a fixed height
                    nodeHeight
                }
                else -> {
                    // For other elements, use a default height
                    nodeHeight
                }
            }
        }
    }
}
