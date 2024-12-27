package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenUtils;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.assembler.Directive;
import rars.riscv.lang.lexing.RVTokenType;
import rars.util.RefCell;

import javax.swing.text.BadLocationException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import static rars.venus.editors.rsyntaxtextarea.RSTAUtils.tokenValue;

public final class RVFoldParser implements FoldParser {
    private static boolean canBeAChildInList(final @NotNull List<@NotNull FoldData> folds,
                                             final @NotNull FoldData element) {
        return folds
            .stream()
            .noneMatch(fold -> fold.startLine() >= element.startLine() && fold.endLine() <= element.endLine());
    }

    private static @NotNull List<@NotNull Fold> hierarchize(final @NotNull List<@NotNull Fold> folds) {
        final var foldsData = folds
            .stream()
            .map(FoldData::new)
            .toList();
        final var sortedBySize = foldsData
            .stream()
            .sorted(Comparator.comparingInt(FoldData::lineCount))
            .toList();
        final var hierarchized = new ArrayList<FoldData>();

        for (final var fold : sortedBySize) {
            if (canBeAChildInList(hierarchized, fold)) {
                hierarchized.add(fold);
            } else {
                // instead of going top-down, we go bottom-up
                // this allows us to sort the children before adding them to the parent
                // the caveat is that the resulting speed of the parser will be slower

                // first, find all the children that are within the current fold, and sort them
                final var sortedChildren = hierarchized
                    .stream()
                    .filter(child -> child.startLine() >= fold.startLine() && child.endLine() <= fold.endLine())
                    .sorted(Comparator.comparingInt(FoldData::startOffset))
                    .toList();
                // next, remove them from the result list
                hierarchized.removeAll(sortedChildren);
                // then, add them to the current fold
                fold.children.addAll(sortedChildren);
                // finally, add the current fold to the result list
                hierarchized.add(fold);
            }
        }
        return generateFoldsFromData(hierarchized);
    }

    private static @NotNull List<Fold> generateFoldsFromData(final @NotNull List<FoldData> data) {
        return data.stream()
            .sorted(Comparator.comparingInt(FoldData::startLine))
            .map(foldData -> createFoldFromFoldDataRec(foldData.baseFold(), foldData.children()))
            .toList();
    }

    private static @NotNull Fold createFoldFromFoldDataRec(final @NotNull Fold baseFold,
                                                           final @NotNull List<FoldData> children) {
        children.stream().sorted(Comparator.comparingInt(FoldData::startLine)).forEach(childData -> {
            try {
                final var childFold = baseFold.createChild(childData.baseFold().getFoldType(), childData.startOffset());
                childFold.setEndOffset(childData.endOffset());
                createFoldFromFoldDataRec(childFold, childData.children());
            } catch (final BadLocationException e) {
                throw new RuntimeException(e);
            }
        });
        return baseFold;
    }

    private static boolean doFoldsIntersect(final @NotNull Fold first, final @NotNull Fold other) {
        final var isFirstStartInOtherFold =
            other.getStartLine() <= first.getStartLine() && first.getStartLine() <= other.getEndLine();
        final var isFirstEndInOtherFold =
            other.getStartLine() <= first.getEndLine() && first.getEndLine() <= other.getEndLine();
        return isFirstStartInOtherFold != isFirstEndInOtherFold;
    }

    @SafeVarargs
    private static @NotNull List<@NotNull Fold> join(final List<@NotNull Fold> @NotNull ... lists) {
        final var foldMap = new HashMap<Integer, Fold>();
        for (final var list : lists) {
            for (final var fold : list) {
                foldMap.merge(fold.getStartLine(), fold,
                    (existing, replacement) -> replacement.getEndLine() > existing.getEndLine() ? replacement :
                        existing);
            }
        }
        return foldMap.values().stream().toList();
    }

    private static @NotNull List<@NotNull Fold> mergeWithRegions(final @NotNull List<Fold> folds,
                                                                 final @NotNull List<Fold> regions) {
        final var result = new ArrayList<>(folds);
        for (final var region : regions) {
            final var intersectingRegions = result
                .stream()
                .filter(fold -> doFoldsIntersect(fold, region)).toList();
            if (intersectingRegions.stream().noneMatch(fold -> fold.getFoldType() != RVFoldType.COMMENT)) {
                // the remaining conflicting folds are all comments and, thus, have lower priority
                // we can safely remove them
                result.removeAll(intersectingRegions);
                result.add(region);
            }
        }
        return result;
    }

    // region getFolds methods
    private static @NotNull List<Fold> getFoldsBase(final @NotNull RSyntaxTextArea textArea,
                                                    final @NotNull FoldParserCallback callback) {
        final var folds = new ArrayList<Fold>();
        final var foldRef = new RefCell<Fold>(null);
        final var lineCount = textArea.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            final var tokens = textArea.getTokenListForLine(i);
            try {
                callback.processLine(i, tokens, folds, foldRef);
            } catch (final BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        return folds;
    }

    private static @NotNull List<Fold> getFoldsForLabels(final @NotNull RSyntaxTextArea textArea) {
        return getFoldsBase(textArea, (lineNumber, tokens, folds, fold) -> {
            if (hasALabel(tokens)) {
                if (fold.value != null && fold.value.isOnSingleLine()) {
                    folds.remove(fold.value);
                }
                fold.value = new Fold(RVFoldType.LABEL, textArea, tokens.getOffset());
                fold.value.setEndOffset(tokens.getOffset());
                folds.add(fold.value);
            } else if (isMacroStartLine(tokens) || isMacroEndLine(tokens)) {
                if (fold.value != null) {
                    fold.value.setEndOffset(tokens.getOffset() - 1);
                    if (fold.value.isOnSingleLine()) {
                        folds.remove(fold.value);
                    }
                    fold.value = null;
                }
            } else if (fold.value != null && !TokenUtils.isBlankOrAllWhiteSpace(tokens)) {
                fold.value.setEndOffset(tokens.getOffset());
            }
        });
    }

    private static @NotNull List<Fold> getCommentFolds(final @NotNull RSyntaxTextArea textArea) {
        return getFoldsBase(textArea, (lineNumber, tokens, folds, fold) -> {
            if (isCommentLine(tokens)) {
                if (fold.value == null) {
                    fold.value = new Fold(RVFoldType.COMMENT, textArea, tokens.getOffset());
                    folds.add(fold.value);
                }
            } else if (fold.value != null) {
                fold.value.setEndOffset(textArea.getLineStartOffset(lineNumber) - 1);
                if (fold.value.isOnSingleLine()) {
                    folds.removeLast();
                }
                fold.value = null;
            }
        });
    }

    private static @NotNull List<Fold> getFoldsForInstructionBlocks(final @NotNull RSyntaxTextArea textArea) {
        return getFoldsBase(textArea, (lineNumber, tokens, folds, fold) -> {
            if (isInstructionLine(tokens)) {
                if (fold.value == null) {
                    fold.value = new Fold(RVFoldType.INSTRUCTION_BLOCK, textArea, tokens.getOffset());
                    folds.add(fold.value);
                }
            } else if (fold.value != null) {
                fold.value.setEndOffset(textArea.getLineStartOffset(lineNumber) - 1);
                if (fold.value.isOnSingleLine()) {
                    folds.removeLast();
                }
                fold.value = null;
            }

        });
    }

    private static @NotNull List<Fold> getRegionFolds(final @NotNull RSyntaxTextArea textArea) {
        final var regionStack = new ArrayList<Fold>();
        return getFoldsBase(textArea, ((lineNumber, tokens, folds, currentFold) -> {
            if (isCommentLine(tokens)) {
                final var commentText = findToken(tokens, token -> token.getType() == tokenValue(RVTokenType.COMMENT));
                if (commentText != null) {
                    final var comment = commentText.getLexeme();
                    // strip the leading '#' and any whitespaces after it
                    final var commentTextStripped = comment.substring(1).strip().toLowerCase();
                    // check if the comment begins with the `region` keyword
                    if (commentTextStripped.startsWith("region")) {
                        final var regionFold = new Fold(RVFoldType.REGION, textArea, tokens.getOffset());
                        regionStack.add(regionFold);
                        folds.add(regionFold);
                    } else if (commentTextStripped.startsWith("endregion")) {
                        if (!regionStack.isEmpty()) {
                            final var regionFold = regionStack.removeLast();
                            regionFold.setEndOffset(tokens.getOffset());
                            folds.add(regionFold);
                        }
                    }
                }
            }
        }));
    }

    private static @NotNull List<Fold> getMacroFolds(final @NotNull RSyntaxTextArea textArea) {
        return getFoldsBase(textArea, (lineNumber, tokens, folds, fold) -> {
            if (isMacroStartLine(tokens)) {
                if (fold.value == null) {
                    fold.value = new Fold(RVFoldType.MACRO, textArea, tokens.getOffset());
                }
            } else if (isMacroEndLine(tokens)) {
                if (fold.value != null) {
                    fold.value.setEndOffset(tokens.getOffset());
                    if (!fold.value.isOnSingleLine()) {
                        folds.add(fold.value);
                    }
                    fold.value = null;
                }
            }
        });
    }
    // endregion getFolds methods

    // region Token line predicates
    private static boolean hasALabel(final Token tokens) {
        return lineContainsToken(tokens, (token) -> token.getType() == tokenValue(RVTokenType.LABEL));
    }

    private static boolean isInstructionLine(final Token tokens) {
        var currentToken = tokens;
        while (currentToken != null && currentToken.isPaintable()) {
            final var type = currentToken.getType();
            if (type == tokenValue(RVTokenType.INSTRUCTION)) {
                return true;
            } else if (!currentToken.isWhitespace()) {
                return false;
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
            if (type == tokenValue(RVTokenType.COMMENT)) {
                result = true;
            } else if (type == tokenValue(RVTokenType.NULL)) {
                return result;
            } else if (type != tokenValue(RVTokenType.WHITESPACE)) {
                return false;
            }
            currentToken = currentToken.getNextToken();
        }
        return result;
    }

    private static boolean isMacroStartLine(final Token tokens) {
        return lineContainsToken(tokens, (token) -> token.getType() == tokenValue(RVTokenType.DIRECTIVE) &&
            token.getLexeme().equalsIgnoreCase(Directive.MACRO.getName()));
    }

    private static boolean isMacroEndLine(final Token tokens) {
        return lineContainsToken(tokens, (token) -> token.getType() == tokenValue(RVTokenType.DIRECTIVE) &&
            token.getLexeme().equalsIgnoreCase(Directive.END_MACRO.getName()));
    }

    private static boolean lineContainsToken(final @NotNull Token tokens, final @NotNull Predicate<Token> predicate) {
        var currentToken = tokens;
        while (currentToken != null && currentToken.isPaintable()) {
            if (predicate.test(currentToken)) {
                return true;
            }
            currentToken = currentToken.getNextToken();
        }
        return false;
    }
    // endregion Token line predicates

    private static @Nullable Token findToken(final @NotNull Token startToken,
                                             final @NotNull Predicate<Token> predicate) {
        var currentToken = startToken;
        while (currentToken != null && currentToken.isPaintable()) {
            if (predicate.test(currentToken)) {
                return currentToken;
            }
            currentToken = currentToken.getNextToken();
        }
        return null;
    }

    @Override
    public @NotNull List<Fold> getFolds(final @NotNull RSyntaxTextArea rSyntaxTextArea) {
        final var labelsFolds = getFoldsForLabels(rSyntaxTextArea);
        final var commentFolds = getCommentFolds(rSyntaxTextArea);
//        final var instructionBlocksFolds = getFoldsForInstructionBlocks(rSyntaxTextArea);
        final var macroFolds = getMacroFolds(rSyntaxTextArea);
        final var joined = join(labelsFolds, /*instructionBlocksFolds, */commentFolds, macroFolds);
        final var regions = getRegionFolds(rSyntaxTextArea);
        final var merged = mergeWithRegions(joined, regions);
        return hierarchize(merged);
    }

    @FunctionalInterface
    private interface FoldParserCallback {
        void processLine(final int lineNumber, final @NotNull Token tokens, final @NotNull ArrayList<Fold> folds,
                         final @NotNull RefCell<@Nullable Fold> currentFold) throws BadLocationException;
    }

    private record FoldData(@NotNull Fold baseFold, @NotNull List<FoldData> children) {
        public FoldData(@NotNull final Fold baseFold) {
            this(baseFold, new ArrayList<>());
        }

        public int startLine() {
            return baseFold.getStartLine();
        }

        public int endLine() {
            return baseFold.getEndLine();
        }

        public int startOffset() {
            return baseFold.getStartOffset();
        }

        public int endOffset() {
            return baseFold.getEndOffset();
        }

        public int lineCount() {
            return endLine() - startLine();
        }
    }
}
