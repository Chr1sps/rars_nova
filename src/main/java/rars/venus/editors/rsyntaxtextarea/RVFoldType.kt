package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.rsyntaxtextarea.folding.FoldType;

public final class RVFoldType implements FoldType {
    public static final int REGION = FoldType.FOLD_TYPE_USER_DEFINED_MIN + 1;
    public static final int LABEL = FoldType.FOLD_TYPE_USER_DEFINED_MIN + 2;
    public static final int INSTRUCTION_BLOCK = FoldType.FOLD_TYPE_USER_DEFINED_MIN + 3;
    public static final int MACRO = FoldType.FOLD_TYPE_USER_DEFINED_MIN + 4;

    private RVFoldType() {
    }
}
