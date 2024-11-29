package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.Settings;
import rars.venus.editors.ColorScheme;
import rars.venus.editors.TextEditingArea;

import javax.swing.text.Document;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;

public class RSyntaxTextAreaBasedEditor implements TextEditingArea {
    private static final Map<TextAttribute, Object> ligatureAttributes = Map.of(
            TextAttribute.KERNING, TextAttribute.KERNING_ON,
            TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON
    );

    static {
        FoldParserManager.get().addFoldParserMapping(RVSyntax.SYNTAX_STYLE_RISCV, new RVFoldParser());
        final var factory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        factory.putMapping(RVSyntax.SYNTAX_STYLE_RISCV, RSTATokensProducer.class.getName());
    }

    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;
    private final Gutter gutter;
    private @NotNull ColorScheme colorScheme;

    public RSyntaxTextAreaBasedEditor() {
        textArea = new RSyntaxTextArea();
        this.setColorScheme(ColorScheme.getDefaultScheme());
//        textArea.setSyntaxScheme(new RVSyntaxScheme());
        textArea.setSyntaxEditingStyle(RVSyntax.SYNTAX_STYLE_RISCV);
        textArea.setCodeFoldingEnabled(true);
        textArea.setMarkOccurrencesDelay(1);
        scrollPane = new RTextScrollPane(textArea);
        gutter = scrollPane.getGutter();
//        this.updateEditorColours();
        this.setFont(Globals.getSettings().getEditorFont());
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
    public String getSelectedText() {
        return textArea.getSelectedText();
    }

    @Override
    public int getSelectionEnd() {
        return textArea.getSelectionEnd();
    }

    @Override
    public void setSelectionEnd(final int pos) {
        textArea.setSelectionEnd(pos);
    }

    @Override
    public int getSelectionStart() {
        return textArea.getSelectionStart();
    }

    @Override
    public void setSelectionStart(final int pos) {
        textArea.setSelectionStart(pos);
    }

    @Override
    public void select(final int selectionStart, final int selectionEnd) {
        textArea.select(selectionStart, selectionEnd);
    }

    @Override
    public void selectAll() {
        textArea.selectAll();
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
    public void replaceSelection(final String str) {
        textArea.replaceSelection(str);
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
        final var derived = f.deriveFont(ligatureAttributes);
        textArea.setFont(derived);
        gutter.setFont(derived);
    }

    @Override
    public boolean requestFocusInWindow() {
        return textArea.requestFocusInWindow();
    }

    @Override
    public FontMetrics getFontMetrics(final Font f) {
        return textArea.getFontMetrics(f);
    }

    @Override
    public void setBackground(final Color c) {
        textArea.setBackground(c);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        textArea.setEnabled(enabled);
    }

    @Override
    public void grabFocus() {
        textArea.grabFocus();
    }

    @Override
    public void redo() {
        textArea.redoLastAction();
    }

    @Override
    public void revalidate() {
        textArea.revalidate();
    }

    @Override
    public void setSourceCode(final String code, final boolean editable) {
        textArea.setText(code);
        textArea.setEditable(editable);
        textArea.setEnabled(editable);
        textArea.setCaretPosition(0);
        if (editable)
            textArea.requestFocusInWindow();
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
    }

    @Override
    public void setTabSize(final int chars) {
        textArea.setTabSize(chars);
    }

    private void updateEditorColours() {
        final var isEditable = textArea.isEditable();
        final var settings = Globals.getSettings();
        final var background = settings.getColorSettingByPosition(Settings.EDITOR_BACKGROUND);
        final var foreground = settings.getColorSettingByPosition(Settings.EDITOR_FOREGROUND);
        textArea.setBackground(background);
        textArea.setCurrentLineHighlightColor(settings.getColorSettingByPosition(Settings.EDITOR_LINE_HIGHLIGHT));
        textArea.setSelectionColor(settings.getColorSettingByPosition(Settings.EDITOR_SELECTION_COLOR));
        textArea.setCaretColor(settings.getColorSettingByPosition(Settings.EDITOR_CARET_COLOR));
        textArea.setForeground(isEditable ? foreground : foreground.darker());
        gutter.setBackground(background);
        gutter.setForeground(foreground);
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
    public int getCaretPosition() {
        return textArea.getCaretPosition();
    }

    @Override
    public void setCaretPosition(final int position) {
        textArea.setCaretPosition(position);
    }

    @Override
    public void requestFocus() {
        textArea.requestFocus();
    }

    @Override
    public @NotNull ColorScheme getColorScheme() {
        return colorScheme;
    }

    @Override
    public void setColorScheme(final @NotNull ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
        this.applyColorScheme(colorScheme);
    }

    private void applyColorScheme(final @NotNull ColorScheme colorScheme) {
        final var converted = RSTASchemeConverter.INSTANCE.convert(colorScheme, textArea.getFont());
        this.textArea.setSyntaxScheme(converted);
    }
}
