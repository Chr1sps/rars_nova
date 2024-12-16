package rars.venus.settings.editor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import rars.riscv.lang.lexing.RVTokenType;

public sealed interface TreeNode
        permits TreeNode.Base, TreeNode.Empty, TreeNode.Font, TreeNode.Syntax
{
    final class Base implements TreeNode {
        public static final Base INSTANCE = new Base();
        
        private Base() {
        }
        
        @Contract(pure = true)
        @Override
        public @NotNull String toString() {
            return "General";
        }
    }

    record Syntax(RVTokenType type) implements TreeNode {
        @Override
        public @NotNull String toString() {
            return "Style %s".formatted(type.name());
        }
    }

    final class Font implements TreeNode {
        public static final Font INSTANCE = new Font();
        
        private Font() {
        }
        
        @Contract(pure = true)
        @Override
        public @NotNull String toString() {
            return "Font";
        }
    }
    
    record Empty(String display) implements TreeNode {
        @Override
        public String toString() {
            return display;
        }
    }
}
