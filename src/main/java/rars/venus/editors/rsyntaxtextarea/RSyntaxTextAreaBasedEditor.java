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
import rars.venus.editors.ColorScheme;
import rars.venus.editors.TextEditingArea;
import rars.venus.editors.Theme;

import javax.swing.*;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextAttribute;
import java.util.Map;

import static rars.settings.FontSettings.FONT_SETTINGS;

public class RSyntaxTextAreaBasedEditor implements TextEditingArea {
    private static final Map<TextAttribute, Object> ligatureAttributes = Map.of(
        TextAttribute.KERNING, TextAttribute.KERNING_ON
//        TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON
    );

    static {
        FoldParserManager.get().addFoldParserMapping(RVSyntax.SYNTAX_STYLE_RISCV, new RVFoldParser());
        final var factory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        factory.putMapping(RVSyntax.SYNTAX_STYLE_RISCV, RSTATokensProducer.class.getName());
    }

    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;
    private final Gutter gutter;
    private @NotNull Theme theme;
    private @NotNull ColorScheme colorScheme;

    public RSyntaxTextAreaBasedEditor(final @NotNull Theme theme) {
        textArea = new RSyntaxTextArea();
        scrollPane = new RTextScrollPane(textArea);
        gutter = scrollPane.getGutter();
        this.setFont(FONT_SETTINGS.getCurrentFont());
        this.setTheme(theme);
        textArea.setSyntaxEditingStyle(RVSyntax.SYNTAX_STYLE_RISCV);
        textArea.setCodeFoldingEnabled(true);
        textArea.setMarkOccurrencesDelay(1);
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
    public int getSelectionStart() {
        return textArea.getSelectionStart();
    }

    @Override
    public void select(final int selectionStart, final int selectionEnd) {
        textArea.select(selectionStart, selectionEnd);
        textArea.grabFocus();
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
    public void setFocusable(final boolean focusable) {
        textArea.setFocusable(focusable);
    }

    @Override
    public Font getFont() {
        return textArea.getFont();
    }

    @Override
    public void setFont(final @NotNull Font f) {
//        final var derived = f.deriveFont(ligatureAttributes);
        textArea.setFont(f);
        gutter.setLineNumberFont(f);
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
    public void setEnabled(final boolean enabled) {
        textArea.setEnabled(enabled);
    }

    @Override
    public void disableFully() {
        scrollPane.setEnabled(false);
        scrollPane.setFocusable(false);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                e.consume();
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                e.consume();
            }
        });
        textArea.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseMoved(final MouseEvent e) {
                e.consume();
            }
        });
    }

    @Override
    public void grabFocus() {
        scrollPane.grabFocus();
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
        setEnabled(editable);
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
    public void setCaret(@NotNull final Caret caret) {
        textArea.setCaret(caret);
    }

    @Override
    public void setCaretBlinkRate(final int rate) {
        this.textArea.getCaret().setBlinkRate(rate);
    }

    @Override
    public void setTabSize(final int chars) {
        textArea.setTabSize(chars);
    }

//    private void updateEditorColours() {
//        final var isEditable = textArea.isEditable();
//        final var settings = Globals.getSettings();
//        final var background = settings.getColorSettingByPosition(Settings.EDITOR_BACKGROUND);
//        final var foreground = settings.getColorSettingByPosition(Settings.EDITOR_FOREGROUND);
//        textArea.setBackground(background);
//        textArea.setCurrentLineHighlightColor(settings.getColorSettingByPosition(Settings.EDITOR_LINE_HIGHLIGHT));
//        textArea.setSelectionColor(settings.getColorSettingByPosition(Settings.EDITOR_SELECTION_COLOR));
//        textArea.setCaretColor(settings.getColorSettingByPosition(Settings.EDITOR_CARET_COLOR));
//        textArea.setForeground(isEditable ? foreground : foreground.darker());
//        gutter.setBackground(background);
//        gutter.setForeground(foreground);
//    }

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

    @Override
    public @NotNull Theme getTheme() {
        return theme;
    }

    @Override
    public void setTheme(final @NotNull Theme theme) {
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
        this.setColorScheme(theme.colorScheme);
    }

    private void applyColorScheme(final @NotNull ColorScheme colorScheme) {
        final var converted = RSTASchemeConverter.INSTANCE.convert(colorScheme, textArea.getFont());
        this.textArea.setSyntaxScheme(converted);
    }
}
