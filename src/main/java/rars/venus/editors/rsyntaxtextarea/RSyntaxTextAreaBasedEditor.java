package rars.venus.editors.rsyntaxtextarea;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;
import rars.util.Pair;
import rars.venus.editors.EditorTheme;
import rars.venus.editors.TextEditingArea;
import rars.venus.editors.TokenStyle;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;

import static rars.settings.FontSettings.FONT_SETTINGS;

public final class RSyntaxTextAreaBasedEditor implements TextEditingArea {
    public static final @NotNull String SYNTAX_STYLE_RISCV = "text/riscv";
    private static final @NotNull Logger LOGGER = LogManager.getLogger(RSyntaxTextAreaBasedEditor.class);
    private static final Map<TextAttribute, Object> textAttributes = Map.of(
        TextAttribute.KERNING, TextAttribute.KERNING_ON
    );

    static {
        FoldParserManager.get().addFoldParserMapping(SYNTAX_STYLE_RISCV, new RVFoldParser());
        final var factory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        factory.putMapping(SYNTAX_STYLE_RISCV, RSTATokensProducer.class.getName());
    }

    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;
    private final Gutter gutter;
    private @NotNull EditorTheme theme;

    public RSyntaxTextAreaBasedEditor(final @NotNull EditorTheme theme) {
        textArea = new RSyntaxTextArea();
        scrollPane = new RTextScrollPane(textArea);
        gutter = scrollPane.getGutter();
        this.setFont(FONT_SETTINGS.getCurrentFont());
        this.setTheme(theme);
        textArea.setSyntaxEditingStyle(SYNTAX_STYLE_RISCV);
        textArea.setCodeFoldingEnabled(true);
        textArea.setMarkOccurrencesDelay(1);
//        textArea.getPosi
    }

    @Override
    public void copy() {
        textArea.copy();
    }

    @Override
    public void cut() {
        textArea.cut();
    }

    @Override
    public @NotNull FindReplaceResult doFindText(final String find, final boolean caseSensitive) {
        final var context = new SearchContext();
        context.setSearchFor(find);
        context.setMatchCase(caseSensitive);
        context.setSearchForward(true);

        final var found = SearchEngine.find(textArea, context);
        if (found.wasFound()) {
            return FindReplaceResult.TEXT_FOUND;
        } else {
            return FindReplaceResult.TEXT_NOT_FOUND;
        }
    }

    @Override
    public @NotNull FindReplaceResult doReplace(final String find, final String replace, final boolean caseSensitive) {
        final var context = new SearchContext();
        context.setSearchFor(find);
        context.setMatchCase(caseSensitive);
        context.setSearchForward(true);

        final var found = SearchEngine.replace(textArea, context);
        if (found.wasFound()) {
            return FindReplaceResult.TEXT_REPLACED_FOUND_NEXT;
        } else {
            return FindReplaceResult.TEXT_REPLACED_NOT_FOUND_NEXT;
        }
    }

    @Override
    public int doReplaceAll(final String find, final String replace, final boolean caseSensitive) {
        final var context = new SearchContext();
        context.setSearchFor(find);
        context.setReplaceWith(replace);
        context.setMatchCase(caseSensitive);
        final var result = SearchEngine.replaceAll(textArea, context);
        return result.getCount();
    }

    @Override
    public Document getDocument() {
        return textArea.getDocument();
    }

    @Override
    public void select(final int selectionStart, final int selectionEnd) {
        textArea.select(selectionStart, selectionEnd);
        textArea.grabFocus();
    }

    @Override
    public void selectLine(final int lineNumber) {
        try {
            final var start = textArea.getLineStartOffset(lineNumber);
            final int end = textArea.getLineEndOffset(lineNumber);
            textArea.select(start, end);
            textArea.grabFocus();
        } catch (final BadLocationException e) {
            LOGGER.warn("Failed to select line", e);
        }
    }

    @Override
    public String getText() {
        return textArea.getText();
    }

    @Override
    public void setText(final String text) {
        textArea.setText(text);
    }

    @Override
    public void paste() {
        textArea.paste();
    }

    @Override
    public void setEditable(final boolean editable) {
        textArea.setEditable(editable);
    }

    @Override
    public Font getFont() {
        return textArea.getFont();
    }

    @Override
    public void setFont(final @NotNull Font f) {
        final var derived = f.deriveFont(textAttributes);
        textArea.setFont(derived);
        gutter.setLineNumberFont(derived);
    }

    @Override
    public void requestFocusInWindow() {
        textArea.requestFocusInWindow();
    }

    @Override
    public void setForeground(final Color c) {
        textArea.setForeground(c);
    }

    @Override
    public void setBackground(final Color c) {
        textArea.setBackground(c);
    }

    @Override
    public void setSelectionColor(final Color c) {
        textArea.setSelectionColor(c);
    }

    @Override
    public void setLineHighlightColor(final Color c) {
        textArea.setCurrentLineHighlightColor(c);
    }

    @Override
    public void setCaretColor(final Color c) {
        textArea.setCaretColor(c);
    }

    @Override
    public @NotNull Caret getCaret() {
        return textArea.getCaret();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        textArea.setEnabled(enabled);
    }

    @Override
    public void redo() {
        textArea.redoLastAction();
    }

    @Override
    public void setSourceCode(final String code, final boolean editable) {
        textArea.setText(code);
        textArea.setEditable(editable);
        setEnabled(editable);
        textArea.setCaretPosition(0);
        if (editable) {
            textArea.requestFocusInWindow();
        }
    }

    @Override
    public void undo() {
        textArea.undoLastAction();
    }

    @Override
    public void discardAllUndoableEdits() {
        textArea.discardAllEdits();
    }

    @Override
    public void setLineHighlightEnabled(final boolean highlight) {
        textArea.setHighlightCurrentLine(highlight);
    }

    @Override
    public void setCaretBlinkRate(final int rate) {
        this.textArea.getCaret().setBlinkRate(rate);
    }

    @Override
    public void setTabSize(final int chars) {
        textArea.setTabSize(chars);
    }

    @Override
    public Component getOuterComponent() {
        return scrollPane;
    }

    @Override
    public boolean canUndo() {
        return textArea.canUndo();
    }

    @Override
    public boolean canRedo() {
        return textArea.canRedo();
    }

    @Override
    public @NotNull EditorTheme getTheme() {
        return theme;
    }

    @Override
    public void setTheme(final @NotNull EditorTheme theme) {
        this.theme = theme;
        this.textArea.setBackground(theme.backgroundColor);
        this.textArea.setForeground(theme.foregroundColor);
        this.textArea.setCurrentLineHighlightColor(theme.lineHighlightColor);
        this.textArea.setCaretColor(theme.caretColor);
        this.textArea.setSelectionColor(theme.selectionColor);
        this.gutter.setBackground(theme.backgroundColor);
        this.gutter.setForeground(theme.foregroundColor);
        this.gutter.setFoldIndicatorForeground(theme.foregroundColor);
        this.gutter.setFoldIndicatorArmedForeground(theme.foregroundColor);
        this.gutter.setLineNumberColor(theme.foregroundColor);
        UIManager.put("ToolTip.background", theme.backgroundColor);
        this.applyColorScheme(theme.tokenStyles);
    }

    @Override
    public void setTokenStyle(@NotNull final RVTokenType type, @NotNull final TokenStyle style) {
        this.theme.tokenStyles.put(type, style);
        this.applyColorScheme(theme.tokenStyles);
    }

    @Override
    public @NotNull Pair<Integer, Integer> getCaretPosition() {
        final var offset = textArea.getCaretPosition();
        try {
            final var line = textArea.getLineOfOffset(offset);
            final var column = offset - textArea.getLineStartOffset(line);
            return Pair.of(line, column);
        } catch (final BadLocationException e) {
            LOGGER.error("Failed to get caret position", e);
            return Pair.of(0, 0);
        }
    }

    private void applyColorScheme(final @NotNull Map<@NotNull RVTokenType, @NotNull TokenStyle> tokenStyles) {
        final var converted = RSTASchemeConverter.convert(tokenStyles, textArea.getFont());
        this.textArea.setSyntaxScheme(converted);
    }
}
