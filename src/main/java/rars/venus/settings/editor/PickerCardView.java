package rars.venus.settings.editor;

import javax.swing.*;
import java.awt.*;

public final class PickerCardView extends JPanel {
    private final CardLayout layout;
    private final BaseStylePickerPanel baseStylePicker;
    private final SyntaxStylePickerPanel syntaxStylePicker;
    private final FontPickerPanel fontPicker;

    public PickerCardView() {
        super(new CardLayout());
        layout = (CardLayout) getLayout();

        JPanel emptyPanel = new JPanel();
        baseStylePicker = new BaseStylePickerPanel();
        syntaxStylePicker = new SyntaxStylePickerPanel();
        fontPicker = new FontPickerPanel();

        add(emptyPanel, "empty");
        add(fontPicker, "font");
        add(baseStylePicker, "base");
        add(syntaxStylePicker, "syntax");
    }

    public FontPickerPanel getFontPicker() {
        return fontPicker;
    }

    public SyntaxStylePickerPanel getSyntaxStylePicker() {
        return syntaxStylePicker;
    }

    public BaseStylePickerPanel getBaseStylePicker() {
        return baseStylePicker;
    }

    public void showBasePicker() {
        layout.show(this, "base");
    }

    public void showFontPicker() {
        layout.show(this, "font");
    }

    public void showSyntaxStylePicker() {
        layout.show(this, "syntax");
    }

    public void showEmpty() {
        layout.show(this, "empty");
    }
}
