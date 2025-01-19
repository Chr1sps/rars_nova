package rars.api;

import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import rars.riscv.hardware.MemoryConfiguration;
import rars.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@Command(name = "rars_nova", showEndOfOptionsDelimiterInUsageHelp = true)
public final class ProgramOptions implements Runnable {
    static {
        System.setProperty(
            "picocli.endofoptions.description",
            "Separates options from arguments passed to the executed program."
        );
    }

    @Option(names = "--use-pseudo-instructions", negatable = true, description = "Enable/disable use of pseudo instructions and formats.", defaultValue = "true", fallbackValue = "true")
    public boolean usePseudoInstructions = true;

    @Option(names = "--assemble-only", description = "Assemble only, do not run the program.")
    public boolean assembleOnly = false;

    @Option(names = {"--brief", "-b"}, description = "Do not display register/memory address along with contents.")
    public boolean brief = false;

    @Option(names = {"--debug", "-d"}, description = "Display RARS debugging statements.")
    public boolean debug = false;

    @Option(names = "--assembly-error-code", description = "Error code to return if assembly fails.")
    public int assemblyErrorCode = 1;

    @Option(names = "--simulation-error-code", description = "Error code to return if simulation fails.")
    public int simulationErrorCode = 1;

    @Option(names = "--gui", negatable = true, description = "Explicitly enable/disable GUI mode.", defaultValue = "true", fallbackValue = "true")
    public boolean gui = true;

    @Option(names = "--acknowledgements", description = "Display copyright notice and acknowledgements.")
    public boolean acknowledgements = false;

    @Option(names = {"--help", "-h"}, usageHelp = true, description = "Display this help message.")
    public boolean showHelp = false;

    @Option(names = "--warnings-are-errors", description = "Treat assembly warnings as errors.")
    public boolean warningsAreErrors = false;

    @Option(names = "--display-format", description = "Display memory or register contents in the specified format. Valid values: ${COMPLETION-CANDIDATES}.")
    public @NotNull DisplayFormat displayFormat = DisplayFormat.HEX;

    @Option(names = "--display-instruction-count", description = "Display count of basic instructions executed.")
    public boolean displayInstructionCount = false;

    @Option(names = "--64-bit", description = "Enable 64-bit assembly and executables (Not fully compatible with rv32).")
    public boolean isRV64 = false;

    @Option(names = "--self-modifying-code", description = "Allow self-modifying code - program can write and branch to either text or data segment.")
    public boolean selfModifyingCode = false;

    @Option(names = "--start-at-main", description = "Start execution at statement with global label main, if defined.")
    public boolean startAtMain = false;

    @Option(names = "--memory-configuration", description = "Set memory configuration. Valid values: ${COMPLETION-CANDIDATES}.")
    public @NotNull MemoryConfiguration memoryConfiguration = MemoryConfiguration.DEFAULT;

    @Option(names = "--registers", arity = "1..*", description = "List of register names or numbers whose content to display at end of run.")
    public @NotNull List<@NotNull String> registers = List.of();

    @Option(names = "--max-steps", description = "Maximum count of steps to simulate. If 0, negative or not specified, there is no maximum.")
    public int maxSteps = -1;

    @Option(names = {
        "--project-mode", "-p"
    }, description = "Project mode - assemble all files in the same directory as the given file.")
    public boolean isProjectMode = false;

    @Option(names = "--add-memory-range", arity = "2", description = "Adds a memory range from lower-inclusive to upper-exclusive bounds whose contents to display at the end of a run.", parameterConsumer = MemoryRangeListConverter.class)
    public @NotNull List<@NotNull Pair<@NotNull Integer, @NotNull Integer>> memoryRanges = List.of();

    @Option(names = "--print-to-stderr", description = "Print RARS messages to standard error instead of standard output.")
    public boolean printToStdErr = false;

    @Option(names = {
        "--files", "-f"
    }, arity = "1..*", description = "Files to be assembled. The first file is assumed to be the main file unless the global statement label 'main' is defined in one of the files.")
    public @NotNull List<@NotNull File> files = List.of();

    @Parameters(description = "Arguments to be passed to the executed program.")
    public @NotNull List<@NotNull String> programArgs = List.of();

    @SuppressWarnings("InstanceVariableMayNotBeInitialized")
    @Spec
    private CommandSpec spec;

    @Override
    public String toString() {
        return "ProgramOptions{" +
            "programArgs=" + programArgs +
            ", files=" + files +
            ", printToStdErr=" + printToStdErr +
            ", memoryRanges=" + memoryRanges +
            ", isProjectMode=" + isProjectMode +
            ", maxSteps=" + maxSteps +
            ", registers=" + registers +
            ", memoryConfiguration=" + memoryConfiguration +
            ", startAtMain=" + startAtMain +
            ", selfModifyingCode=" + selfModifyingCode +
            ", isRV64=" + isRV64 +
            ", displayInstructionCount=" + displayInstructionCount +
            ", displayFormat=" + displayFormat +
            ", warningsAreErrors=" + warningsAreErrors +
            ", showHelp=" + showHelp +
            ", acknowledgements=" + acknowledgements +
            ", gui=" + gui +
            ", simulationErrorCode=" + simulationErrorCode +
            ", assemblyErrorCode=" + assemblyErrorCode +
            ", debug=" + debug +
            ", brief=" + brief +
            ", assembleOnly=" + assembleOnly +
            ", usePseudoInstructions=" + usePseudoInstructions +
            '}';
    }

    @Override
    public void run() {
        final var parseResult = spec.commandLine().getParseResult();

        if (!parseResult.originalArgs().isEmpty()) {
            if (!(parseResult.hasMatchedOption("--gui") || parseResult.hasMatchedOption("--no-gui"))) {
                gui = false;
            }
        }
    }

    private static class MemoryRangeListConverter implements CommandLine.IParameterConsumer {

        @Override
        public void consumeParameters(
            final @NotNull Stack<String> args,
            final CommandLine.Model.ArgSpec argSpec,
            final CommandSpec commandSpec
        ) {
            if (args.size() != 2) {
                throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    "Expected two parameters for memory range, but got: " + args
                );
            }
            final var start = Integer.parseInt(args.pop());
            final var end = Integer.parseInt(args.pop());
            final var pair = Pair.of(start, end);
            final var currentList = argSpec.<List<Pair<Integer, Integer>>>getValue();
            final var result = new ArrayList<>(currentList);
            result.add(pair);
            argSpec.setValue(result);
        }
    }
}

// private boolean parseCommandArgs(final String @NotNull [] args) {
//     var argsOK = true;
//     for (int i = 0; i < args.length; i++) {
//         if (args[i].equalsIgnoreCase("dump")) {
//             if (args.length <= (i + 3)) {
//                 this.out.println("Dump command line argument requires a segment, format and file name.");
//                 argsOK = false;
//             } else {
//                 if (this.dumpTriples == null) {
//                     this.dumpTriples = new ArrayList<>();
//                 }
//                 this.dumpTriples.add(new String[]{args[++i], args[++i], args[++i]});
//                 // simulate = false;
//             }
//         }
//     }
//     return argsOK;
// }
