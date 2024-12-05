package rars.extras;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.assembler.Directive;
import rars.riscv.AbstractSyscall;
import rars.riscv.Instruction;
import rars.riscv.Instructions;
import rars.riscv.SyscallLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Small class for automatically generating documentation.
 * <p>
 * Currently it makes some Markdown tables, but in the future it could do
 * something
 * with javadocs or generate a website with all of the help information
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class Documentation {

    private Documentation() {
    }

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects
     */
    public static void main(final String[] args) {
        Globals.initialize();
        System.out.println(Documentation.createDirectiveMarkdown());
        System.out.println(Documentation.createSyscallMarkdown());
        System.out.println(Documentation.createInstructionMarkdownNew(false, false));
        System.out.println(Documentation.createInstructionMarkdownNew(true, false));
        System.out.println(Documentation.createInstructionMarkdownNew(false, true));
        System.out.println(Documentation.createInstructionMarkdownNew(true, true));
    }

    /**
     * <p>createDirectiveMarkdown.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public static @NotNull String createDirectiveMarkdown() {
        final var directives = Directive.getDirectiveList();
        directives.sort(Comparator.comparing(Directive::getName));
        final StringBuilder output = new StringBuilder("| Name | Description|\n|------|------------|");
        for (final var direct : directives) {
            output.append("\n|");
            output.append(direct.getName());
            output.append('|');
            output.append(direct.getDescription());
            output.append('|');
        }
        return output.toString();
    }

    /**
     * <p>createSyscallMarkdown.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public static @NotNull String createSyscallMarkdown() {
        final var list = SyscallLoader.getSyscallList();
        final var sorted = list.stream().sorted().toList();
        final StringBuilder output = new StringBuilder(
                "| Name | Call Number (a7) | Description | Inputs | Outputs |\n|------|------------------|-------------|--------|---------|");
        for (final AbstractSyscall syscall : sorted) {
            output.append("\n|");
            output.append(syscall.getName());
            output.append('|');
            output.append(syscall.getNumber());
            output.append('|');
            output.append(syscall.getDescription());
            output.append('|');
            output.append(syscall.getInputs());
            output.append('|');
            output.append(syscall.getOutputs());
            output.append('|');
        }

        return output.toString();
    }


    public static @NotNull String createInstructionMarkdownNew(final boolean is64, final boolean isExtended) {
        final List<? extends Instruction> instructionList;
        if (is64) {
            if (isExtended)
                instructionList = Instructions.INSTRUCTIONS_R64_EXTENDED;
            else
                instructionList = Instructions.INSTRUCTIONS_R64;
        } else {
            if (isExtended)
                instructionList = Instructions.INSTRUCTIONS_R32_EXTENDED;
            else
                instructionList = Instructions.INSTRUCTIONS_R32;
        }
        final var sorted = instructionList
                .stream()
                .sorted(Comparator.comparing(Instruction::getExampleFormat))
                .toList();
        final StringBuilder output = new StringBuilder("| Example Usage | Description |\n|---------------|-------------|");
        for (final var instr : sorted) {
            output.append("\n|");
            output.append(instr.getExampleFormat());
            output.append('|');
            output.append(instr.getDescription());
            output.append('|');

        }
        return output.toString();
    }
}
