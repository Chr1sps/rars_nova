package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Theme;
import rars.Globals;
import rars.Settings;

public class SwingLAFTheme extends Theme {
    /**
     * Creates a theme from an RSyntaxTextArea.  It should be contained in
     * an <code>RTextScrollPane</code> to get all gutter color information.
     *
     * @param textArea The text area.
     */
    public SwingLAFTheme(final RSyntaxTextArea textArea) {
        super(textArea);

        final var settings = Globals.getSettings();
        baseFont = settings.getEditorFont();
        bgColor = settings.getColorSettingByPosition(Settings.EDITOR_BACKGROUND);
        caretColor = settings.getColorSettingByPosition(Settings.EDITOR_CARET_COLOR);
        useSelectionFG = false;
//        selectionFG = settings.getColorSettingByPosition(Settings.EDITOR_SELECTION_COLOR);
        selectionBG = settings.getColorSettingByPosition(Settings.EDITOR_SELECTION_COLOR);
        selectionRoundedEdges = false;
        currentLineHighlight = settings.getColorSettingByPosition(Settings.EDITOR_LINE_HIGHLIGHT);
        fadeCurrentLineHighlight = false;
//        tabLineColor = settings.getColorSettingByPosition(Settings.TAB);
//        marginLineColor = settings.getColorSettingByPosition(Settings.);
//        markAllHighlightColor = textArea.getMarkAllHighlightColor();
//        markOccurrencesColor = textArea.getMarkOccurrencesColor();
//        markOccurrencesBorder = textArea.getPaintMarkOccurrencesBorder();
//        matchedBracketBG = textArea.getMatchedBracketBGColor();
//        matchedBracketFG = textArea.getMatchedBracketBorderColor();
//        matchedBracketHighlightBoth = textArea.getPaintMatchedBracketPair();
//        matchedBracketAnimate = textArea.getAnimateBracketMatching();
//        hyperlinkFG = textArea.getHyperlinkForeground();

//        scheme = textArea.getSyntaxScheme();

        final var gutter = RSyntaxUtilities.getGutter(textArea);
        if (gutter != null) {
            gutterBackgroundColor = bgColor;
            gutterBorderColor = textArea.getForeground();
//            activeLineRangeColor = gutter.getActiveLineRangeColor();
//            iconRowHeaderInheritsGutterBG = gutter.getIconRowHeaderInheritsGutterBackground();
//            lineNumberColor = gutter.getLineNumberColor();
//            currentLineNumberColor = gutter.getCurrentLineNumberColor();
//            lineNumberFont = gutter.getLineNumberFont().getFamily();
//            lineNumberFontSize = gutter.getLineNumberFont().getSize();
            foldIndicatorFG = textArea.getForeground();
            foldIndicatorArmedFG = textArea.getForeground();
//            foldBG = gutter.getFoldBackground();
//            armedFoldBG = gutter.getArmedFoldBackground();
        }
    }
}
