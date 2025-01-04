package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class GenericOptionSection<T extends JComponent> extends JPanel {
    public final @NotNull T component;

    public GenericOptionSection(
        final @NotNull String title,
        final @NotNull T component
    ) {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.add(new JLabel(title));
        this.add(Box.createHorizontalStrut(10));
        this.component = component;
        this.add(component);
    }
}
