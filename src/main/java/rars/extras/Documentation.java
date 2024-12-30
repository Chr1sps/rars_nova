package rars.extras;

import org.jetbrains.annotations.NotNull;
import rars.assembler.Directive;
import rars.riscv.Instruction;
import rars.riscv.Instructions;
import rars.riscv.SyscallLoader;

import java.util.Arrays;
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

    public static void main(final String[] args) {
        System.out.println(Documentation.createDirectiveMarkdown());
        System.out.println(Documentation.createSyscallMarkdown());
        System.out.println(Documentation.createInstructionMarkdown(false, false));
        System.out.println(Documentation.createInstructionMarkdown(true, false));
        System.out.println(Documentation.createInstructionMarkdown(false, true));
        System.out.println(Documentation.createInstructionMarkdown(true, true));
    }

    public static @NotNull String createDirectiveMarkdown() {
        final var sortedDirectives = Arrays
            .stream(Directive.values())
            .sorted(Comparator.comparing(
                Directive::getName
            )).toList();
        final var builder = new StringBuilder("| Name | Description|\n|------|------------|");
        for (final var directive : sortedDirectives) {
            builder.append("\n|");
            builder.append(directive.getName());
            builder.append('|');
            builder.append(directive.getDescription());
            builder.append('|');
        }
        return builder.toString();
    }

    public static @NotNull String createSyscallMarkdown() {
        final var list = SyscallLoader.getSyscallList();
        final var sorted = list.stream().sorted().toList();
        final var builder = new StringBuilder(
            "| Name | Call Number (a7) | Description | Inputs | Outputs " +
                "|\n|------|------------------|-------------|--------|---------|");
        for (final var syscall : sorted) {
            builder.append("\n|");
            builder.append(syscall.getName());
            builder.append('|');
            builder.append(syscall.getNumber());
            builder.append('|');
            builder.append(syscall.getDescription());
            builder.append('|');
            builder.append(syscall.getInputs());
            builder.append('|');
            builder.append(syscall.getOutputs());
            builder.append('|');
        }

        return builder.toString();
    }

    public static @NotNull String createInstructionMarkdown(final boolean is64, final boolean isExtended) {
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
        final StringBuilder output = new StringBuilder("| Example Usage | Description " +
            "|\n|---------------|-------------|");
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
