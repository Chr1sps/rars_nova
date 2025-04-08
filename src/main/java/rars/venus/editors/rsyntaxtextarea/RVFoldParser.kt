package rars.venus.editors.rsyntaxtextarea

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenUtils
import org.fife.ui.rsyntaxtextarea.folding.Fold
import org.fife.ui.rsyntaxtextarea.folding.FoldParser
import rars.assembler.Directive
import rars.riscv.lang.lexing.RVTokenType
import rars.util.RefCell
import rars.venus.editors.rsyntaxtextarea.RSTAUtils.tokenValue
import java.util.*
import javax.swing.text.BadLocationException

internal object RVFoldParser : FoldParser {
    override fun getFolds(textArea: RSyntaxTextArea): List<Fold> {
        val labelsFolds = getFoldsForLabels(textArea)
        val commentFolds = getCommentFolds(textArea)
        val macroFolds = getMacroFolds(textArea)
        val joined = join(labelsFolds, commentFolds, macroFolds)
        val regions = getRegionFolds(textArea)
        val merged = mergeWithRegions(joined, regions)
        return hierarchize(merged)
    }
}

private typealias FoldParserCallback = (Int, Token, MutableList<Fold>, RefCell<Fold?>) -> Unit

private data class FoldData(
    val baseFold: Fold,
    val children: MutableList<FoldData> = mutableListOf()
) {
    val startLine by baseFold::startLine
    val endLine by baseFold::endLine
    val startOffset by baseFold::startOffset
    val endOffset by baseFold::endOffset
    val lineCount get() = endLine - startLine
}

private fun canBeAChildInList(
    folds: List<FoldData>,
    element: FoldData
): Boolean = folds.none { fold ->
    fold.startLine >= element.startLine && fold.endLine <= element.endLine
}

private fun hierarchize(folds: List<Fold>): List<Fold> {
    val foldsData = folds.map { FoldData(it) }
    val sortedBySize = foldsData.sortedBy { it.lineCount }
    val hierarchized = ArrayList<FoldData>(folds.size)

    for (fold in sortedBySize) {
        if (canBeAChildInList(hierarchized, fold)) {
            hierarchized.add(fold)
        } else {
            // instead of going top-down, we go bottom-up
            // this allows us to sort the children before adding them to the parent
            // the caveat is that the resulting speed of the parser will be slower

            // first, find all the children that are within the current fold, and sort them

            val sortedChildren = hierarchized
                .filter { child -> child.startLine >= fold.startLine && child.endLine <= fold.endLine }
                .sortedBy { it.startOffset }
            // next, remove them from the result list
            hierarchized.removeAll(sortedChildren)
            // then, add them to the current fold
            fold.children.addAll(sortedChildren)
            // finally, add the current fold to the result list
            hierarchized.add(fold)
        }
    }
    return generateFoldsFromData(hierarchized)
}

private fun generateFoldsFromData(data: List<FoldData>): List<Fold> = data.sortedBy {
    it.startLine
}.map { foldData ->
    createFoldFromFoldDataRec(
        foldData.baseFold,
        foldData.children
    )
}

private fun createFoldFromFoldDataRec(
    baseFold: Fold,
    children: List<FoldData>
): Fold {
    children.sortedBy { it.startLine }
        .forEach { childData ->
            try {
                val childFold =
                    baseFold.createChild(childData.baseFold.foldType, childData.startOffset)
                childFold.endOffset = childData.endOffset
                createFoldFromFoldDataRec(childFold, childData.children)
            } catch (e: BadLocationException) {
                throw RuntimeException(e)
            }
        }
    return baseFold
}

private fun doFoldsIntersect(first: Fold, other: Fold): Boolean {
    val isFirstStartInOtherFold =
        other.startLine <= first.startLine
            && first.startLine <= other.endLine
    val isFirstEndInOtherFold =
        other.startLine <= first.endLine
            && first.endLine <= other.endLine
    return isFirstStartInOtherFold != isFirstEndInOtherFold
}

private fun join(vararg lists: List<Fold>): List<Fold> {
    val foldMap = mutableMapOf<Int, Fold>()
    for (list in lists) {
        for (fold in list) {
            foldMap.merge(
                fold.startLine, fold
            ) { existing, replacement ->
                if (replacement.endLine > existing.endLine) {
                    replacement
                } else {
                    existing
                }
            }
        }
    }
    return foldMap.values.toList()
}

private fun mergeWithRegions(
    folds: List<Fold>,
    regions: List<Fold>
): List<Fold> = folds.toMutableList().apply {
    for (region in regions) {
        val intersectingRegions = filter { fold ->
            doFoldsIntersect(fold, region)
        }
        if (intersectingRegions.none { it.foldType != RVFoldType.COMMENT }) {
            // the remaining conflicting folds are all comments and, thus, have lower priority
            // we can safely remove them
            removeAll(intersectingRegions)
            add(region)
        }
    }
}

// region getFolds methods
private fun getFoldsBase(
    textArea: RSyntaxTextArea,
    callback: FoldParserCallback
): List<Fold> {
    val folds = ArrayList<Fold>()
    val foldRef = RefCell<Fold?>(null)
    val lineCount = textArea.lineCount
    for (i in 0..<lineCount) {
        val tokens = textArea.getTokenListForLine(i)
        try {
            callback(i, tokens, folds, foldRef)
        } catch (e: BadLocationException) {
            error(e)
        }
    }
    return folds
}

private fun getFoldsForLabels(textArea: RSyntaxTextArea): List<Fold> {
    return getFoldsBase(
        textArea
    ) { lineNumber, tokens, folds, fold ->
        when {
            hasALabel(tokens) -> {
                if (fold.value != null && fold.value!!.isOnSingleLine) {
                    folds.remove(fold.value)
                }
                fold.value = Fold(RVFoldType.LABEL, textArea, tokens.offset)
                fold.value!!.endOffset = tokens.offset
                folds.add(fold.value!!)
            }
            isMacroStartLine(tokens) || isMacroEndLine(tokens) -> {
                if (fold.value != null) {
                    fold.value!!.endOffset = tokens.offset - 1
                    if (fold.value!!.isOnSingleLine) {
                        folds.remove(fold.value)
                    }
                    fold.value = null
                }
            }
            fold.value != null && !TokenUtils.isBlankOrAllWhiteSpace(tokens) -> {
                fold.value!!.endOffset = tokens.offset
            }
        }
    }
}

private fun getCommentFolds(textArea: RSyntaxTextArea): List<Fold> = getFoldsBase(
    textArea
) { lineNumber, tokens, folds, fold ->
    when {
        isCommentLine(tokens) -> {
            if (fold.value == null) {
                fold.value = Fold(RVFoldType.COMMENT, textArea, tokens.offset)
                folds.add(fold.value!!)
            }
        }
        fold.value != null -> {
            fold.value!!.endOffset = textArea.getLineStartOffset(lineNumber) - 1
            if (fold.value!!.isOnSingleLine) {
                folds.removeLast()
            }
            fold.value = null
        }
    }
}

private fun getFoldsForInstructionBlocks(textArea: RSyntaxTextArea): List<Fold> = getFoldsBase(
    textArea
) { lineNumber, tokens, folds, fold ->
    when {
        isInstructionLine(tokens) -> {
            if (fold.value == null) {
                fold.value = Fold(RVFoldType.INSTRUCTION_BLOCK, textArea, tokens.offset)
                folds.add(fold.value!!)
            }
        }
        fold.value != null -> {
            fold.value!!.endOffset = textArea.getLineStartOffset(lineNumber) - 1
            if (fold.value!!.isOnSingleLine) {
                folds.removeLast()
            }
            fold.value = null
        }
    }
}

private fun getRegionFolds(textArea: RSyntaxTextArea): List<Fold> {
    val regionStack = ArrayList<Fold>()
    return getFoldsBase(
        textArea
    ) { lineNumber, tokens, folds, currentFold ->
        if (isCommentLine(tokens)) {
            val commentText: Token? = findToken(
                tokens
            ) { token -> token.type == RVTokenType.COMMENT.tokenValue }
            if (commentText != null) {
                val comment = commentText.lexeme
                // strip the leading '#' and any whitespaces after it
                val commentTextStripped = comment.substring(1).trim().lowercase(Locale.getDefault())
                // check if the comment begins with the `region` keyword
                when {
                    commentTextStripped.startsWith("region") -> {
                        val regionFold = Fold(RVFoldType.REGION, textArea, tokens.offset)
                        regionStack.add(regionFold)
                        folds.add(regionFold)
                    }
                    commentTextStripped.startsWith("endregion") -> {
                        if (!regionStack.isEmpty()) {
                            val regionFold = regionStack.removeLast()
                            regionFold.endOffset = tokens.offset
                            folds.add(regionFold)
                        }
                    }
                }
            }
        }
    }
}

private fun getMacroFolds(textArea: RSyntaxTextArea): List<Fold> = getFoldsBase(
    textArea
) { lineNumber, tokens, folds, fold ->
    when {
        isMacroStartLine(tokens) -> {
            if (fold.value == null) {
                fold.value = Fold(RVFoldType.MACRO, textArea, tokens.offset)
            }
        }
        isMacroEndLine(tokens) -> {
            if (fold.value != null) {
                fold.value!!.endOffset = tokens.offset
                if (!fold.value!!.isOnSingleLine) {
                    folds.add(fold.value!!)
                }
                fold.value = null
            }
        }
    }
}

// endregion getFolds methods

// region Token line predicates

private fun hasALabel(tokens: Token): Boolean = lineContainsToken(
    tokens
) { token -> token.type == RVTokenType.LABEL.tokenValue }

private fun isInstructionLine(tokens: Token?): Boolean {
    var currentToken = tokens
    while (currentToken != null && currentToken.isPaintable) {
        val type = currentToken.type
        when {
            type == RVTokenType.INSTRUCTION.tokenValue -> {
                return true
            }
            !currentToken.isWhitespace -> {
                return false
            }
            else -> currentToken = currentToken.nextToken
        }
    }
    return false
}

private fun isCommentLine(tokens: Token): Boolean {
    var currentToken: Token? = tokens
    var result = false
    while (currentToken != null && currentToken.isPaintable) {
        val type = currentToken.type
        when {
            type == RVTokenType.COMMENT.tokenValue -> {
                result = true
            }
            type == RVTokenType.NULL.tokenValue -> {
                return result
            }
            type != RVTokenType.WHITESPACE.tokenValue -> {
                return false
            }
        }
        currentToken = currentToken.nextToken
    }
    return result
}

private fun isMacroStartLine(tokens: Token): Boolean = lineContainsToken(
    tokens
) { token ->
    token.type == RVTokenType.DIRECTIVE.tokenValue &&
        token.lexeme.equals(Directive.MACRO.directiveName, ignoreCase = true)
}

private fun isMacroEndLine(tokens: Token): Boolean = lineContainsToken(
    tokens
) { token ->
    token.type == RVTokenType.DIRECTIVE.tokenValue &&
        token.lexeme.equals(Directive.END_MACRO.directiveName, ignoreCase = true)
}

private fun lineContainsToken(tokens: Token, predicate: (Token) -> Boolean): Boolean {
    var currentToken: Token? = tokens
    while (currentToken != null && currentToken.isPaintable) {
        if (predicate(currentToken)) {
            return true
        }
        currentToken = currentToken.nextToken
    }
    return false
}

// endregion Token line predicates

private fun findToken(
    startToken: Token,
    predicate: (Token) -> Boolean
): Token? {
    var currentToken: Token? = startToken
    while (currentToken != null && currentToken.isPaintable) {
        if (predicate(currentToken)) {
            return currentToken
        }
        currentToken = currentToken.nextToken
    }
    return null
}
