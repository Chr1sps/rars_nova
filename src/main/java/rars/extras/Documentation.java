package rars.extras;

import org.jetbrains.annotations.NotNull;
import rars.assembler.Directive;
import rars.riscv.Instruction;
import rars.riscv.SyscallLoader;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static rars.riscv.InstructionsRegistry.BASIC_INSTRUCTIONS;
import static rars.riscv.InstructionsRegistry.EXTENDED_INSTRUCTIONS;

/**
 * Small class for automatically generating documentation.
 * <p>
 * Currently it makes some Markdown tables, but in the future it could do
 * something
 * with javadocs or generate a website with all the help information
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class Documentation {

    private Documentation() {
    }

    public static void main(final String[] args) {
        System.out.println(createDirectiveMarkdown());
        System.out.println(createSyscallMarkdown());
        System.out.println(createInstructionMarkdown(BASIC_INSTRUCTIONS.r32All));
        System.out.println(createInstructionMarkdown(BASIC_INSTRUCTIONS.r64Only));
        System.out.println(createInstructionMarkdown(EXTENDED_INSTRUCTIONS.r32All));
        System.out.println(createInstructionMarkdown(EXTENDED_INSTRUCTIONS.r64Only));
    }

    private static @NotNull String createDirectiveMarkdown() {
        final var sortedDirectives = Arrays
            .stream(Directive.values())
            .sorted(Comparator.comparing(
                Directive::getName
            )).toList();
        final var builder = new StringBuilder("""
            | Name | Description|
            |------|------------|""");
        for (final var directive : sortedDirectives) {
            builder.append("\n|%s|%s|".formatted(
                directive.getName(),
                directive.getDescription()
            ));
        }
        return builder.toString();
    }

    private static @NotNull String createSyscallMarkdown() {
        final var list = SyscallLoader.getSyscallList();
        final var sorted = list.stream().sorted().toList();
        final var builder = new StringBuilder(
            """
                | Name | Call Number (a7) | Description | Inputs | Outputs |
                |------|------------------|-------------|--------|---------|""");
        for (final var syscall : sorted) {
            builder.append("\n|%s|%s|%s|%s|%s|".formatted(
                syscall.getName(),
                syscall.getNumber(),
                syscall.getDescription(),
                syscall.getInputs(),
                syscall.getOutputs()
            ));
        }

        return builder.toString();
    }

    private static @NotNull String createInstructionMarkdown(final @NotNull List<? extends Instruction> instructionList) {
        final var sorted = instructionList
            .stream()
            .sorted(Comparator.comparing(instruction -> instruction.exampleFormat))
            .toList();
        final StringBuilder output = new StringBuilder("""
            | Example Usage | Description |
            |---------------|-------------|""");
        for (final var instr : sorted) {
            output.append("\n|%s|%s|".formatted(instr.exampleFormat, instr.description));
        }
        return output.toString();
    }
}
