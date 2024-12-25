package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;

public class TreeNode {
    final @NotNull String display;

    public TreeNode(final @NotNull String display) {
        this.display = display;
    }

    @Override
    public @NotNull String toString() {
        return display;
    }

    public static final class Syntax extends TreeNode {
        public final @NotNull TokenSettingKey type;

        public Syntax(@NotNull final TokenSettingKey type, @NotNull final String display) {
            super(display);
            this.type = type;
        }
    }
}
