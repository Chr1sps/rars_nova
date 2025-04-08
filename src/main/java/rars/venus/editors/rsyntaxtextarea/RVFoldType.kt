package rars.venus.editors.rsyntaxtextarea

import org.fife.ui.rsyntaxtextarea.folding.FoldType

object RVFoldType {
    const val CODE = FoldType.CODE
    const val COMMENT = FoldType.COMMENT
    const val IMPORTS = FoldType.IMPORTS

    const val REGION: Int = FoldType.FOLD_TYPE_USER_DEFINED_MIN + 1
    const val LABEL: Int = FoldType.FOLD_TYPE_USER_DEFINED_MIN + 2
    const val INSTRUCTION_BLOCK: Int = FoldType.FOLD_TYPE_USER_DEFINED_MIN + 3
    const val MACRO: Int = FoldType.FOLD_TYPE_USER_DEFINED_MIN + 4
}
