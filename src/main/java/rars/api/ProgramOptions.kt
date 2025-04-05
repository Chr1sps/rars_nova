package rars.api

import picocli.CommandLine
import picocli.CommandLine.Model.CommandSpec
import rars.riscv.hardware.memory.MemoryConfiguration
import java.io.File
import java.util.*

@CommandLine.Command(name = "rars_nova", showEndOfOptionsDelimiterInUsageHelp = true)
class ProgramOptions : Runnable {
    @JvmField
    @CommandLine.Option(
        names = ["--use-pseudo-instructions"],
        negatable = true,
        description = ["Enable/disable use of pseudo instructions and formats."],
        defaultValue = "true",
        fallbackValue = "true"
    )
    var usePseudoInstructions: Boolean = true

    @JvmField
    @CommandLine.Option(names = ["--assemble-only"], description = ["Assemble only, do not run the program."])
    var assembleOnly: Boolean = false

    @JvmField
    @CommandLine.Option(
        names = ["--brief", "-b"],
        description = ["Do not display register/memory address along with contents."]
    )
    var brief: Boolean = false

    @CommandLine.Option(names = ["--debug", "-d"], description = ["Display RARS debugging statements."])
    var debug: Boolean = false

    @JvmField
    @CommandLine.Option(names = ["--assembly-error-code"], description = ["Error code to return if assembly fails."])
    var assemblyErrorCode: Int = 1

    @JvmField
    @CommandLine.Option(
        names = ["--simulation-error-code"],
        description = ["Error code to return if simulation fails."]
    )
    var simulationErrorCode: Int = 1

    @JvmField
    @CommandLine.Option(
        names = ["--gui"],
        negatable = true,
        description = ["Explicitly enable/disable GUI mode."],
        defaultValue = "true",
        fallbackValue = "true"
    )
    var gui: Boolean = true

    @CommandLine.Option(
        names = ["--acknowledgements"],
        description = ["Display copyright notice and acknowledgements."]
    )
    var acknowledgements: Boolean = false

    @JvmField
    @CommandLine.Option(names = ["--help", "-h"], usageHelp = true, description = ["Display this help message."])
    var showHelp: Boolean = false

    @JvmField
    @CommandLine.Option(names = ["--warnings-are-errors"], description = ["Treat assembly warnings as errors."])
    var warningsAreErrors: Boolean = false

    @JvmField
    @CommandLine.Option(
        names = ["--display-format"],
        description = ["Display memory or register contents in the specified format. Valid values: \${COMPLETION-CANDIDATES}."]
    )
    var displayFormat: DisplayFormat = DisplayFormat.HEX

    @JvmField
    @CommandLine.Option(
        names = ["--display-instruction-count"],
        description = ["Display count of basic instructions executed."]
    )
    var displayInstructionCount: Boolean = false

    @JvmField
    @CommandLine.Option(
        names = ["--64-bit"],
        description = ["Enable 64-bit assembly and executables (Not fully compatible with rv32)."]
    )
    var isRV64: Boolean = false

    @JvmField
    @CommandLine.Option(
        names = ["--self-modifying-code"],
        description = ["Allow self-modifying code - program can write and branch to either text or data segment."]
    )
    var selfModifyingCode: Boolean = false

    @JvmField
    @CommandLine.Option(
        names = ["--start-at-main"],
        description = ["Start execution at statement with global label main, if defined."]
    )
    var startAtMain: Boolean = false

    @JvmField
    @CommandLine.Option(
        names = ["--memory-configuration"],
        description = ["Set memory configuration. Valid values: \${COMPLETION-CANDIDATES}."]
    )
    var memoryConfiguration: MemoryConfiguration = MemoryConfiguration.DEFAULT

    @JvmField
    @CommandLine.Option(
        names = ["--registers"],
        arity = "1..*",
        description = ["List of register names or numbers whose content to display at end of run."]
    )
    var registers: List<String> = listOf<String>()

    @JvmField
    @CommandLine.Option(
        names = ["--max-steps"],
        description = ["Maximum count of steps to simulate. If 0, negative or not specified, there is no maximum."]
    )
    var maxSteps: Int = -1

    @JvmField
    @CommandLine.Option(
        names = ["--project-mode", "-p"
        ], description = ["Project mode - assemble all files in the same directory as the given file."]
    )
    var isProjectMode: Boolean = false

    @JvmField
    @CommandLine.Option(
        names = ["--add-memory-range"],
        arity = "2",
        description = ["Adds a memory range from lower-inclusive to upper-exclusive bounds whose contents to display at the end of a run."],
        parameterConsumer = MemoryRangeListConverter::class
    )
    var memoryRanges: List<Pair<Int, Int>> = listOf<Pair<Int, Int>>()

    @JvmField
    @CommandLine.Option(
        names = ["--print-to-stderr"],
        description = ["Print RARS messages to standard error instead of standard output."]
    )
    var printToStdErr: Boolean = false

    @JvmField
    @CommandLine.Option(
        names = ["--files", "-f"],
        arity = "1..*",
        description = ["Files to be assembled. The first file is assumed to be the main file unless the global statement label 'main' is defined in one of the files."]
    )
    var files: List<File> = listOf<File>()

    @JvmField
    @CommandLine.Parameters(description = ["Arguments to be passed to the executed program."])
    var programArgs: List<String> = listOf<String>()

    @CommandLine.Spec
    lateinit var spec: CommandSpec

    override fun toString(): String {
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
            '}'
    }

    override fun run() {
        val parseResult = spec.commandLine().parseResult

        if (parseResult.originalArgs().isNotEmpty()) {
            if (!(parseResult.hasMatchedOption("--gui") || parseResult.hasMatchedOption("--no-gui"))) {
                gui = false
            }
        }
    }

    private class MemoryRangeListConverter : CommandLine.IParameterConsumer {
        override fun consumeParameters(
            args: Stack<String?>,
            argSpec: CommandLine.Model.ArgSpec,
            commandSpec: CommandSpec
        ) {
            if (args.size != 2) {
                throw CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    "Expected two parameters for memory range, but got: $args"
                )
            }
            val start = args.pop()!!.toInt()
            val end = args.pop()!!.toInt()
            val pair = Pair(start, end)
            val currentList = argSpec.getValue<List<Pair<Int, Int>>>() ?: emptyList()
            val result = buildList {
                addAll(currentList)
                add(pair)
            }
            argSpec.setValue<List<Pair<Int, Int>>>(result)
        }
    }

    companion object {
        init {
            System.setProperty(
                "picocli.endofoptions.description",
                "Separates options from arguments passed to the executed program."
            )
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

