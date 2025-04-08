package rars.venus.editors

import rars.riscv.lang.lexing.RVTokenType
import rars.settings.AllSettings
import rars.venus.editors.rsyntaxtextarea.RSyntaxTextAreaBasedEditor
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.text.Caret
import javax.swing.text.Document

/**
 * Specifies capabilities that any test editor used in MARS must have.
 */
interface TextEditingArea {
    val document: Document

    var text: String

    var isEditable: Boolean

    var font: Font

    var theme: EditorTheme

    var foreground: Color

    var background: Color

    var selectionColor: Color

    var caretColor: Color

    var lineHighlightColor: Color

    val caret: Caret

    var isEnabled: Boolean

    fun setSourceCode(code: String, editable: Boolean)

    var lineHighlightEnabled: Boolean

    var caretBlinkRate: Int

    var tabSize: Int

    val caretPosition: Pair<Int, Int>

    val outerComponent: Component

    fun copy()

    fun cut()

    fun paste()

    fun undo()

    fun redo()

    fun canUndo(): Boolean

    fun canRedo(): Boolean

    fun discardAllUndoableEdits()

    fun doFindText(find: String, caseSensitive: Boolean): FindReplaceResult

    fun doReplace(find: String, replace: String, caseSensitive: Boolean): FindReplaceResult

    fun doReplaceAll(find: String, replace: String, caseSensitive: Boolean): Int

    fun select(selectionStart: Int, selectionEnd: Int)

    fun selectLine(lineNumber: Int)

    fun requestFocusInWindow()


    fun setTokenStyle(type: RVTokenType, style: TokenStyle)

    // Used by Find/Replace
    enum class FindReplaceResult {
        TEXT_NOT_FOUND,
        TEXT_FOUND,
        TEXT_REPLACED_FOUND_NEXT,
        TEXT_REPLACED_NOT_FOUND_NEXT
    }
}

fun createTextEditingArea(allSettings: AllSettings): TextEditingArea = RSyntaxTextAreaBasedEditor(
    allSettings.editorThemeSettings.currentTheme.toEditorTheme(),
    allSettings.fontSettings
)