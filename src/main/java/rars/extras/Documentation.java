package rars.extras;

import rars.Globals;
import rars.Settings;
import rars.assembler.Directive;
import rars.riscv.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

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
        System.out.println(Documentation.createInstructionMarkdown(BasicInstruction.class));
        System.out.println(Documentation.createInstructionMarkdown(ExtendedInstruction.class));
        System.out.println(Documentation.createInstructionMarkdown64Only(BasicInstruction.class));
        System.out.println(Documentation.createInstructionMarkdown64Only(ExtendedInstruction.class));
    }

    /**
     * <p>createDirectiveMarkdown.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public static String createDirectiveMarkdown() {
        final ArrayList<Directive> directives = Directive.getDirectiveList();
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
    public static String createSyscallMarkdown() {
        final ArrayList<AbstractSyscall> list = SyscallLoader.getSyscallList();
        Collections.sort(list);
        final StringBuilder output = new StringBuilder(
                "| Name | Call Number (a7) | Description | Inputs | Outputs |\n|------|------------------|-------------|--------|---------|");
        for (final AbstractSyscall syscall : list) {
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

    /**
     * <p>createInstructionMarkdown.</p>
     *
     * @param instructionClass a {@link java.lang.Class} object
     * @return a {@link java.lang.String} object
     */
    public static String createInstructionMarkdown(final Class<? extends Instruction> instructionClass) {
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.RV64_ENABLED, false);
        InstructionSet.rv64 = false;
        Globals.instructionSet.populate();

        final ArrayList<Instruction> instructionList = Globals.instructionSet.getInstructionList();
        instructionList.sort(Comparator.comparing(Instruction::getExampleFormat));

        final StringBuilder output = new StringBuilder("| Example Usage | Description |\n|---------------|-------------|");
        for (final Instruction instr : instructionList) {
            if (instructionClass.isInstance(instr)) {
                output.append("\n|");
                output.append(instr.getExampleFormat());
                output.append('|');
                output.append(instr.getDescription());
                output.append('|');
            }
        }
        return output.toString();
    }

    /**
     * <p>createInstructionMarkdown64Only.</p>
     *
     * @param instructionClass a {@link java.lang.Class} object
     * @return a {@link java.lang.String} object
     */
    public static String createInstructionMarkdown64Only(final Class<? extends Instruction> instructionClass) {

        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.RV64_ENABLED, false);
        InstructionSet.rv64 = false;
        Globals.instructionSet.populate();

        final HashSet<String> set = new HashSet<>();
        for (final Instruction i : Globals.instructionSet.getInstructionList()) {
            set.add(i.getExampleFormat());
        }

        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.RV64_ENABLED, true);
        InstructionSet.rv64 = true;
        Globals.instructionSet.populate();

        final ArrayList<Instruction> instructionList64 = Globals.instructionSet.getInstructionList();
        instructionList64.sort(Comparator.comparing(Instruction::getExampleFormat));
        final var builder = new StringBuilder("| Example Usage | Description |\n|---------------|-------------|");
        for (final Instruction instr : instructionList64) {
            if (instructionClass.isInstance(instr) && !set.contains(instr.getExampleFormat())) {
                builder.append("\n|");
                builder.append(instr.getExampleFormat());
                builder.append('|');
                builder.append(instr.getDescription());
                builder.append('|');
            }
        }
        return builder.toString();
    }
}
