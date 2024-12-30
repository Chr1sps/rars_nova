package rars.riscv;

public enum CompressedInstructionFormat {
    CR, // 2 src / 1 src, 1 dst register
    CI, // 1 src/dst register + small immediate
    CSS, // 1 src register + small immediate
    CIW, // 1 C-specific dst register + wider immediate
    CL, // 1 src, 1 dst C-specific register + small immediate
    CS, // 2 src C-specific registers + small immediate
    CA, // 2 src / 1 src, 1 dst C-specific register
    CB, // 1 src/dst C-specific register + offset
    CJ, // large immediate for jumping
}
