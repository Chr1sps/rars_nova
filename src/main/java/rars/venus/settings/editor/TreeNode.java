package rars.venus.settings.editor;

import org.jetbrains.annotations.NotNull;
import rars.settings.TokenSettingKey;

public sealed interface TreeNode {
    @NotNull String display();

    record Normal(@NotNull String display) implements TreeNode {
        @Override
        public @NotNull String toString() {
            return display;
        }
    }

    record Syntax(
        @NotNull TokenSettingKey type,
        @NotNull String display
    ) implements TreeNode {
        @Override
        public @NotNull String toString() {
            return display;
        }
    }
}