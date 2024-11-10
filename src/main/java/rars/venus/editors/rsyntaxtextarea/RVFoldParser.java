package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldType;
import org.fife.ui.rsyntaxtextarea.folding.LinesWithContentFoldParser;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.BadLocationException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class RVFoldParser implements FoldParser {
    private static final LinesWithContentFoldParser baseParser = new LinesWithContentFoldParser();

    private static boolean hasALabel(final Token tokens) {
        var currentToken = tokens;
        while (currentToken != null && currentToken.isPaintable()) {
            if (currentToken.getType() == RVToken.Type.LABEL.value) {
                return true;
            }
            currentToken = currentToken.getNextToken();
        }
        return false;
    }

    private static boolean startsWithAnInstruction(final Token tokens) {
        var currentToken = tokens;
        while (currentToken != null && currentToken.isPaintable()) {
            if (currentToken.getType() == RVToken.Type.INSTRUCTION.value) {
                return true;
            }
            currentToken = currentToken.getNextToken();
        }
        return false;
    }

    private static boolean isCommentLine(final Token tokens) {
        var currentToken = tokens;
        var result = false;
        while (currentToken != null && currentToken.isPaintable()) {
            final var type = currentToken.getType();
            if (type == RVToken.Type.COMMENT.value) {
                result = true;
            } else if (type == RVToken.Type.NULL.value) {
                return result;
            } else if (type != RVToken.Type.WHITESPACE.value) {
                return false;
            }
            currentToken = currentToken.getNextToken();
        }
        return result;
    }

    private static int lineCount(final @NotNull Fold fold) {
        return fold.getEndLine() - fold.getStartLine();
    }

    private static boolean canBeAChildInList(final @NotNull List<Fold> folds, final @NotNull Fold element) {
        return folds
                .stream()
                .allMatch(fold -> fold.getStartLine() > element.getEndLine() || fold.getEndLine() < element.getEndLine());
    }

    private static int findConflictingChildFold(final @NotNull Fold parent, final @NotNull Fold element) {
        final var childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final var child = parent.getChild(i);
            if (child.getStartLine() <= element.getStartLine() && child.getEndLine() >= element.getEndLine()) {
                return i;
            }
        }
        return -1;
    }

    private static @NotNull Fold findParentFold(final @NotNull List<Fold> folds, final @NotNull Fold element) {
        return folds
                .stream()
                .filter(fold -> fold.getStartLine() <= element.getStartLine() && fold.getEndLine() >= element.getEndLine())
                .findAny()
                .orElseThrow();
    }

    private static @NotNull List<Fold> hierarchize(@NotNull final List<Fold> folds) {

        final var sortedBySize = folds
                .stream()
                .sorted((first, other) -> lineCount(other) - lineCount(first))
                .toList();
        final var hierarchized = new ArrayList<Fold>();

        for (final var fold : sortedBySize) {
            if (canBeAChildInList(hierarchized, fold)) {
                hierarchized.add(fold);
            } else {
                while (true) {
                    var parent = findParentFold(hierarchized, fold);
                    final var conflictingChildIndex = findConflictingChildFold(parent, fold);
                    if (conflictingChildIndex != -1) {
                        parent = parent.getChild(conflictingChildIndex);
                    } else {
                        try {
                            final var newFold = parent.createChild(fold.getFoldType(), fold.getStartOffset());
                            newFold.setEndOffset(fold.getEndOffset());
                            break;
                        } catch (final BadLocationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        hierarchized.sort(Comparator.comparingInt(Fold::getStartLine));
        return hierarchized;
    }

    @SafeVarargs
    private static @NotNull List<Fold> join(final List<Fold> @NotNull ... lists) {
        final var foldMap = new HashMap<Integer, Fold>();
        for (final var list : lists) {
            for (final var fold : list) {
                foldMap.merge(fold.getStartLine(), fold, (existing, replacement) -> replacement.getEndLine() > existing.getEndLine() ? replacement : existing);
            }
        }
        return foldMap.values().stream().toList();
    }

    private static @NotNull List<Fold> getFoldsForLabels(final @NotNull RSyntaxTextArea textArea) {
        final var folds = new ArrayList<Fold>();
        Fold fold = null;
        final var lineCount = textArea.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            final var tokens = textArea.getTokenListForLine(i);
            try {
                if (hasALabel(tokens)) {
                    if (fold != null && fold.isOnSingleLine()) {
                        folds.removeLast();
                    }
                    fold = new Fold(FoldType.CODE, textArea, tokens.getOffset());
                    folds.add(fold);
                } else if (tokens.getType() != RVToken.Type.NULL.value) {
                    if (fold != null) {
                        fold.setEndOffset(tokens.getOffset());
                    }
                }
            } catch (final BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        return folds;
    }

    private static @NotNull List<Fold> getCommentFolds(final @NotNull RSyntaxTextArea textArea) {
        final var folds = new ArrayList<Fold>();
        Fold fold = null;
        final var lineCount = textArea.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            final var tokens = textArea.getTokenListForLine(i);
            try {
                if (isCommentLine(tokens)) {
                    if (fold == null) {
                        fold = new Fold(FoldType.COMMENT, textArea, tokens.getOffset());
                        folds.add(fold);
                    }
                } else if (fold != null) {
                    fold.setEndOffset(textArea.getLineStartOffset(i) - 1);
                    if (fold.isOnSingleLine()) {
                        folds.removeLast();
                    }
                    fold = null;
                }
            } catch (final BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        return folds;
    }

    private static @NotNull List<Fold> getFoldsForInstructionBlocks(final @NotNull RSyntaxTextArea textArea) {
        final var folds = new ArrayList<Fold>();
        Fold fold = null;
        final var lineCount = textArea.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            final var tokens = textArea.getTokenListForLine(i);
            try {
                if (hasALabel(tokens)) {
                    if (fold != null && fold.isOnSingleLine()) {
                        folds.removeLast();
                    }
                    fold = new Fold(FoldType.CODE, textArea, tokens.getOffset());
                    folds.add(fold);
                } else {
                    if (fold != null) {
                        fold.setEndOffset(tokens.getOffset());
                    }
                }
            } catch (final BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        return folds;
    }

    @Override
    public @NotNull List<Fold> getFolds(final @NotNull RSyntaxTextArea rSyntaxTextArea) {
        final var labelsFolds = getFoldsForLabels(rSyntaxTextArea);
        final var commentFolds = getCommentFolds(rSyntaxTextArea);
        final var instructionBlocksFolds = baseParser.getFolds(rSyntaxTextArea);
        final var joined = join(labelsFolds, instructionBlocksFolds, commentFolds);
        return hierarchize(joined);
    }
}
