package rars.venus.settings.editor;

import javax.swing.*;
import java.awt.*;

public final class PickerCardView extends JPanel {
    public final BaseStylePickerPanel baseStylePicker;
    public final SyntaxStylePickerPanel syntaxStylePicker;
    public final FontPickerPanel fontPicker;
    public final OtherSettingsPanel otherSettingsPanel;
    private final JPanel upperPanel;
    private final CardLayout upperLayout;

    public PickerCardView() {
        super();
        setLayout(new BorderLayout());
        upperLayout = new CardLayout();
        upperPanel = new JPanel(upperLayout);

        setPreferredSize(new Dimension(450, 450));
        setMinimumSize(new Dimension(50, 50));

        final var emptyPanel = new JPanel();
        baseStylePicker = new BaseStylePickerPanel();
        syntaxStylePicker = new SyntaxStylePickerPanel();
        fontPicker = new FontPickerPanel();
        otherSettingsPanel = new OtherSettingsPanel();

        upperPanel.add(emptyPanel, "empty");
        upperPanel.add(fontPicker, "font");
        upperPanel.add(baseStylePicker, "base");
        upperPanel.add(syntaxStylePicker, "syntax");
        upperPanel.add(otherSettingsPanel, "other");

        this.add(upperPanel, BorderLayout.NORTH);
    }

    public void showBasePicker() {
        upperLayout.show(upperPanel, "base");
    }

    public void showFontPicker() {
        upperLayout.show(upperPanel, "font");
    }

    public void showSyntaxStylePicker() {
        upperLayout.show(upperPanel, "syntax");
    }

    public void showEmpty() {
        upperLayout.show(upperPanel, "empty");
    }
    
    public void showOtherSettings() {
        upperLayout.show(upperPanel, "other");
    }
}
