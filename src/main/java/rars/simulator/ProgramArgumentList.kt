package rars.simulator

import arrow.core.raise.either
import rars.Globals
import rars.assembler.DataTypes
import rars.logging.RARSLogging
import rars.logging.fatal
import rars.riscv.hardware.memory.stackBaseAddress
import java.util.*
import kotlin.system.exitProcess

private fun buildArgsFromString(args: String): List<String> {
    val st = StringTokenizer(args)
    return buildList(st.countTokens()) {
        while (st.hasMoreTokens()) {
            add(st.nextToken())
        }
    }
}

private val LOGGER = RARSLogging.forClass(object {}::class)

fun storeProgramArguments(programArgumentList: List<String>) {
    if (programArgumentList.isEmpty()) {
        return
    }

    /*
    Runtime stack initialization from stack top-down (each is 4 bytes) :
    programArgumentList.size()
    address of first character of first program argument
    address of first character of second program argument
    ....repeat for all program arguments
    0x00000000 (null terminator for list of string pointers)
    $sp will be set to the address holding the arg list size
    $a0 will be set to the arg list size (argc)
    $a1 will be set to stack address just "below" arg list size (argv)
    Each of the arguments themselves will be stored starting at
    Memory.stackBaseAddress (0x7ffffffc) and working down from there:
    0x7ffffffc will contain null terminator for first arg
    0x7ffffffb will contain last character of first arg
    0x7ffffffa will contain next-to-last character of first arg
    Etc down to first character of first arg.
    Previous address will contain null terminator for second arg
    Previous-to-that contains last character of second arg
    Etc down to first character of second arg.
    Follow this pattern for all remaining arguments.
    */
    val memoryConfiguration = Globals.MEMORY_INSTANCE.memoryConfiguration
    var highAddress =
        memoryConfiguration.stackBaseAddress // highest non-kernel address, sits "under" stack
    val argStartAddress = IntArray(programArgumentList.size)
    either {
        // needed for all memory writes
        for (i in programArgumentList.indices) {
            val programArgument = programArgumentList[i]
            Globals.MEMORY_INSTANCE.set(highAddress, 0, 1)
                .bind() // trailing null byte for each argument
            highAddress--
            for (j in programArgument.length - 1 downTo 0) {
                Globals.MEMORY_INSTANCE.set(
                    highAddress,
                    programArgument[j].code,
                    1
                ).bind()
                highAddress--
            }
            argStartAddress[i] = highAddress + 1
        }
        // now place a null word, the arg starting addresses, and arg count onto stack.
        var stackAddress =
            memoryConfiguration.stackPointerAddress // base address for runtime stack.
        if (highAddress < memoryConfiguration.stackPointerAddress) {
            // Based on current values for stackBaseAddress and stackPointer, this will
            // only happen if the combined lengths of program arguments is greater than
            // 0x7ffffffc - 0x7fffeffc = 0x00001000 = 4096 bytes. In this case, set
            // stackAddress to next lower word boundary minus 4 for clearance (since every
            // byte from highAddress+1 is filled).
            stackAddress =
                highAddress - (highAddress % DataTypes.WORD_SIZE) - DataTypes.WORD_SIZE
        }
        Globals.MEMORY_INSTANCE.set(stackAddress, 0, DataTypes.WORD_SIZE)
            .bind() // null word for end of argv array
        stackAddress -= DataTypes.WORD_SIZE
        for (i in argStartAddress.indices.reversed()) {
            Globals.MEMORY_INSTANCE.set(
                stackAddress,
                argStartAddress[i],
                DataTypes.WORD_SIZE
            ).bind()
            stackAddress -= DataTypes.WORD_SIZE
        }
        Globals.MEMORY_INSTANCE.set(
            stackAddress,
            argStartAddress.size,
            DataTypes.WORD_SIZE
        ).bind() // argc
        stackAddress -= DataTypes.WORD_SIZE

        // Need to set $sp register to stack address, $a0 to argc, $a1 to argv
        // Need to by-pass the backstepping mechanism so go directly to Register instead
        // of RegisterFile
        Globals.REGISTER_FILE.sp.setValue((stackAddress + DataTypes.WORD_SIZE).toLong())
        Globals.REGISTER_FILE.a0.setValue(argStartAddress.size.toLong()) // argc
        Globals.REGISTER_FILE.a1.setValue((stackAddress + DataTypes.WORD_SIZE + DataTypes.WORD_SIZE).toLong()) // argv
    }.onLeft { error ->
        LOGGER.fatal("Error while writing program arguments to memory: $error")
        exitProcess(0)
    }
}

/**
 * Place any program arguments into memory and registers
 * Arguments are stored starting at highest word of non-kernel
 * memory and working back toward runtime stack (there is a 4096
 * byte gap in between). The argument count (argc) and pointers
 * to the arguments are stored on the runtime stack. The stack
 * pointer register $sp is adjusted accordingly and $a0 is set
 * to the argument count (argc), and $a1 is set to the stack
 * address holding the first argument pointer (argv).
 */
fun storeProgramArguments(args: String) {
    storeProgramArguments(buildArgsFromString(args))
}