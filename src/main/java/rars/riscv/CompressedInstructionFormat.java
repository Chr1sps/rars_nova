package rars.riscv;

public enum CompressedInstructionFormat {
    CR_FORMAT, // 2 src / 1 src, 1 dst register
    CI_FORMAT, // 1 src/dst register + small immediate
    CSS_FORMAT, // 1 src register + small immediate
    CIW_FORMAT, // 1 C-specific dst register + wider immediate
    CL_FORMAT, // 1 src, 1 dst C-specific register + small immediate
    CS_FORMAT, // 2 src C-specific registers + small immediate
    CA_FORMAT, // 2 src / 1 src, 1 dst C-specific register
    CB_FORMAT, // 1 src/dst C-specific register + offset
    CJ_FORMAT, // large immediate for jumping
}
