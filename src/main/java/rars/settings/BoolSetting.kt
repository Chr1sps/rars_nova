package rars.settings;

import org.jetbrains.annotations.NotNull;

public enum BoolSetting {
    /**
     * Flag to determine whether program being assembled is limited to
     * basic instructions and formats.
     */
    EXTENDED_ASSEMBLER_ENABLED("ExtendedAssembler", true),
    /**
     * Flag to determine whether a file is immediately and automatically
     * assembled
     * upon opening. Handy when using externa editor like mipster.
     */
    ASSEMBLE_ON_OPEN("AssembleOnOpen", false),
    /**
     * Flag to determine whether all files open currently source file will be
     * assembled when assembly is selected.
     */
    ASSEMBLE_OPEN("AssembleOpen", false),
    /**
     * Flag to determine whether files in the directory of the current source file
     * will be assembled when assembly is selected.
     */
    ASSEMBLE_ALL("AssembleAll", false),

    /**
     * Default visibilty of label window (symbol table). Default only, dynamic
     * status
     * maintained by ExecutePane
     */
    LABEL_WINDOW_VISIBILITY("LabelWindowVisibility", false),
    /**
     * Default setting for displaying addresses and values in hexidecimal in the
     * Execute
     * pane.
     */
    DISPLAY_ADDRESSES_IN_HEX("DisplayAddressesInHex", true),
    DISPLAY_VALUES_IN_HEX("DisplayValuesInHex", true),
    /**
     * Flag to determine whether the currently selected exception handler source
     * file will
     * be included in each assembly operation.
     */
    EXCEPTION_HANDLER_ENABLED("LoadExceptionHandler", false),
    /**
     * Flag to determine whether assembler warnings are considered errors.
     */
    WARNINGS_ARE_ERRORS("WarningsAreErrors", false),
    /**
     * Flag to determine whether to display and use program arguments
     */
    PROGRAM_ARGUMENTS("ProgramArguments", false),
    /**
     * Flag to control whether highlighting is applied to data segment window
     */
    DATA_SEGMENT_HIGHLIGHTING("DataSegmentHighlighting", true),
    /**
     * Flag to control whether highlighting is applied to register windows
     */
    REGISTERS_HIGHLIGHTING("RegistersHighlighting", true),
    /**
     * Flag to control whether assembler automatically initializes program
     * counter to 'main's address
     */
    START_AT_MAIN("StartAtMain", false),
    /**
     * Flag to control whether editor will highlight the line currently being
     * edited
     */
    EDITOR_CURRENT_LINE_HIGHLIGHTING("EditorCurrentLineHighlighting", true),
    /**
     * Flag to control whether editor will provide popup instruction guidance
     * while typing
     */
    POPUP_INSTRUCTION_GUIDANCE("PopupInstructionGuidance", true),
    /**
     * Flag to control whether simulator will use popup dialog for input
     * syscalls
     */
    POPUP_SYSCALL_INPUT("PopupSyscallInput", false),
    /**
     * Flag to control whether language-aware editor will use auto-indent
     * feature
     */
    AUTO_INDENT("AutoIndent", true),
    /**
     * Flag to determine whether a program can write binary code to the text or data
     * segment and
     * execute that code.
     */
    SELF_MODIFYING_CODE_ENABLED("SelfModifyingCode", false),
    /**
     * Flag to determine whether a program uses rv64i instead of rv32i
     */
    RV64_ENABLED("rv64Enabled", false),
    /**
     * Flag to determine whether to calculate relative paths from the current
     * working directory
     * or from the RARS executable path.
     */
    DERIVE_CURRENT_WORKING_DIRECTORY("DeriveCurrentWorkingDirectory", false),
    /**
     * Flag to determine whether to use the FlatLaf dark or light look and feel.
     */
    DARK_MODE("dark_mode", false);

    // TODO: add option for turning off user trap handling and interrupts

    private final @NotNull String name;
    private final boolean value;

    BoolSetting(final @NotNull String n, final boolean v) {
        this.name = n;
        this.value = v;
    }

    public boolean getDefault() {
        return this.value;
    }

    public @NotNull String getName() {
        return this.name;
    }
}
