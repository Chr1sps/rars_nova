package rars.venus.util;

import org.jetbrains.annotations.NotNull;
import rars.Globals;

import javax.swing.*;
import java.awt.*;

public final class IconLoading {
    private IconLoading() {
    }

    public static @NotNull ImageIcon loadIcon(final @NotNull String name) {
        final var resource = IconLoading.class.getResource(Globals.IMAGES_PATH + name);
        return new ImageIcon(Toolkit.getDefaultToolkit().getImage(resource));
    }
}
