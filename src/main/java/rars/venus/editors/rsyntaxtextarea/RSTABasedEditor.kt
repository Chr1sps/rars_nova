package rars.venus.editors.rsyntaxtextarea

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager
import org.fife.ui.rtextarea.Gutter
import org.fife.ui.rtextarea.RTextScrollPane
import org.fife.ui.rtextarea.SearchContext
import org.fife.ui.rtextarea.SearchEngine
import rars.logging.RARSLogging
import rars.logging.error
import rars.logging.warning
import rars.riscv.lang.lexing.RVTokenType
import rars.settings.FontSettings
import rars.venus.editors.EditorTheme
import rars.venus.editors.TextEditingArea
import rars.venus.editors.TextEditingArea.FindReplaceResult
import rars.venus.editors.TokenStyle
import rars.venus.editors.rsyntaxtextarea.RSTASchemeConverter.convert
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.font.TextAttribute
import javax.swing.UIManager
import javax.swing.text.BadLocationException
import javax.swing.text.Caret
import javax.swing.text.Document

class RSTABasedEditor(
    theme: EditorTheme,
    fontSettings: FontSettings
) : TextEditingArea {
    private val textArea: RSyntaxTextArea = RSyntaxTextArea().apply {
        syntaxEditingStyle = SYNTAX_STYLE_RISCV
        isCodeFoldingEnabled = true
        markOccurrences = true
        markOccurrencesDelay = 1
    }
    private val scrollPane: RTextScrollPane = RTextScrollPane(textArea)
    private val gutter: Gutter = scrollPane.gutter

    override var font: Font
        get() = textArea.font
        set(font) {
            val derived: Font = font.deriveFont(TEXT_ATTRIBUTES)
            textArea.font = derived
            gutter.lineNumberFont = derived
        }

    override var theme: EditorTheme = theme
        set(newTheme) {
            field = newTheme
            foreground = newTheme.foregroundColor
            background = newTheme.backgroundColor
            selectionColor = newTheme.selectionColor
            caretColor = newTheme.caretColor
            lineHighlightColor = newTheme.lineHighlightColor
            applyColorScheme(newTheme.tokenStyles)
        }

    // Important: all delegated properties *must* be declared above the `init`
    // block, otherwise NPEs will occur.

    override var caretColor: Color by textArea::caretColor

    override var lineHighlightColor: Color by textArea::currentLineHighlightColor

    override val document: Document by textArea::document

    override val caret: Caret by textArea::caret

    override var lineHighlightEnabled: Boolean by textArea::highlightCurrentLine

    override var caretBlinkRate: Int by textArea.caret::blinkRate

    override var tabSize: Int by textArea::tabSize

    override var text: String by textArea::text

    init {
        font = fontSettings.currentFont
        this.theme = theme
    }

    override fun copy() = textArea.copy()

    override fun cut() = textArea.cut()

    override fun doFindText(
        find: String,
        caseSensitive: Boolean
    ): FindReplaceResult {
        val context = SearchContext().apply {
            markAll = true
            searchFor = find
            matchCase = caseSensitive
            searchForward = true
        }

        val found = SearchEngine.find(textArea, context)
        return if (found.wasFound()) {
            FindReplaceResult.TEXT_FOUND
        } else {
            FindReplaceResult.TEXT_NOT_FOUND
        }
    }

    override fun doReplace(
        find: String,
        replace: String,
        caseSensitive: Boolean
    ): FindReplaceResult {
        val context = SearchContext().apply {
            searchFor = find
            matchCase = caseSensitive
            searchForward = true
        }

        val found = SearchEngine.replace(textArea, context)
        return if (found.wasFound()) {
            FindReplaceResult.TEXT_REPLACED_FOUND_NEXT
        } else {
            FindReplaceResult.TEXT_REPLACED_NOT_FOUND_NEXT
        }
    }

    override fun doReplaceAll(
        find: String,
        replace: String,
        caseSensitive: Boolean
    ): Int {
        val context = SearchContext().apply {
            searchFor = find
            replaceWith = replace
            matchCase = caseSensitive
        }
        val result = SearchEngine.replaceAll(textArea, context)
        return result.count
    }

    override fun select(selectionStart: Int, selectionEnd: Int) {
        textArea.select(selectionStart, selectionEnd)
        textArea.grabFocus()
    }

    override fun selectLine(lineNumber: Int) {
        try {
            val start = textArea.getLineStartOffset(lineNumber)
            val end = textArea.getLineEndOffset(lineNumber)
            textArea.select(start, end)
            textArea.grabFocus()
        } catch (e: BadLocationException) {
            LOGGER.warning(e, "Failed to select line nr $lineNumber.")
        }
    }


    override fun paste() = textArea.paste()

    override var isEditable: Boolean
        get() = textArea.isEditable
        set(editable) {
            textArea.isEditable = editable
        }

    override fun requestFocusInWindow() {
        textArea.requestFocusInWindow()
    }

    override var foreground: Color
        get() = textArea.foreground
        set(color) {
            textArea.foreground = color
            gutter.apply {
                foreground = color
                foldIndicatorForeground = color
                foldIndicatorArmedForeground = color
                lineNumberColor = theme.foregroundColor
            }
        }

    override var background: Color
        get() = textArea.background
        set(color) {
            textArea.background = color
            gutter.background = color
            UIManager.put("ToolTip.background", color)
        }

    override var selectionColor: Color
        get() = textArea.selectionColor
        set(c) {
            textArea.selectionColor = c
            textArea.markOccurrencesColor = c
        }


    override var isEnabled: Boolean
        get() = textArea.isEnabled
        set(enabled) {
            textArea.isEnabled = enabled
        }

    override fun redo() {
        textArea.redoLastAction()
    }

    override fun setSourceCode(code: String, editable: Boolean) {
        isEnabled = editable
        textArea.apply {
            text = code
            isEditable = editable
            caretPosition = 0
            if (editable) {
                requestFocusInWindow()
            }
        }
    }

    override fun undo() {
        textArea.undoLastAction()
    }

    override fun discardAllUndoableEdits() {
        textArea.discardAllEdits()
    }

    override val outerComponent: Component
        get() = scrollPane

    override fun canUndo(): Boolean = textArea.canUndo()

    override fun canRedo(): Boolean = textArea.canRedo()

    override fun setTokenStyle(type: RVTokenType, style: TokenStyle) {
        theme.tokenStyles[type] = style
        applyColorScheme(theme.tokenStyles)
    }

    override val caretPosition: Pair<Int, Int>
        get() = try {
            val offset = textArea.caretPosition
            val line = textArea.getLineOfOffset(offset)
            val column = offset - textArea.getLineStartOffset(line)
            Pair(line, column)
        } catch (e: BadLocationException) {
            LOGGER.error(e, "Failed to get caret position")
            Pair(0, 0)
        }

    private fun applyColorScheme(tokenStyles: Map<RVTokenType, TokenStyle>) {
        val converted = convert(tokenStyles, textArea.font)
        textArea.syntaxScheme = converted
    }

    companion object {
        private const val SYNTAX_STYLE_RISCV = "text/riscv"
        private val LOGGER = RARSLogging.forClass(RSTABasedEditor::class)
        private val TEXT_ATTRIBUTES = mapOf(
            TextAttribute.KERNING to TextAttribute.KERNING_ON,
        )

        init {
            FoldParserManager.get()
                .addFoldParserMapping(SYNTAX_STYLE_RISCV, RVFoldParser)
            val factory =
                TokenMakerFactory.getDefaultInstance() as AbstractTokenMakerFactory
            factory.putMapping(
                SYNTAX_STYLE_RISCV,
                RSTATokensProducer::class.java.name
            )
        }
    }
}
