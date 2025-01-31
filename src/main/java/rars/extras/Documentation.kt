@file:Suppress("ReplacePrintlnWithLogging")

package rars.extras

import rars.assembler.Directive
import rars.riscv.Instruction
import rars.riscv.InstructionsRegistry
import rars.riscv.Syscall

/**
 * Small file for automatically generating documentation.
 *
 * Currently, it makes some Markdown tables, but in the future it could do something
 * with javadocs or generate a website with all the help information
 */
fun main() {
    println(createDirectiveMarkdown())
    println(createSyscallMarkdown())
    println(createInstructionMarkdown(InstructionsRegistry.BASIC_INSTRUCTIONS.r32All))
    println(createInstructionMarkdown(InstructionsRegistry.BASIC_INSTRUCTIONS.r64Only))
    println(createInstructionMarkdown(InstructionsRegistry.EXTENDED_INSTRUCTIONS.r32All))
    println(createInstructionMarkdown(InstructionsRegistry.EXTENDED_INSTRUCTIONS.r64Only))
}

private fun createDirectiveMarkdown(): String = buildString {
    append(
        """
        | Name | Description|
        |------|------------|
        """.trimIndent()
    )
    Directive.entries.sortedBy { it.name }.forEach {
        append("\n|${it.name}|${it.description}|")
    }
}

private fun createSyscallMarkdown(): String = buildString {
    append(
        """
        | Name | Call Number (a7) | Description | Inputs | Outputs |
        |------|------------------|-------------|--------|---------|
        """.trimIndent()
    )
    Syscall.entries.sortedBy { it.serviceName }.forEach {
        append("\n|${it.serviceName}|${it.serviceNumber}|${it.description}|${it.inputs}|${it.outputs}|")
    }
}

private fun createInstructionMarkdown(instructionList: MutableList<out Instruction>): String = buildString {
    append(
        """
        | Example Usage | Description |
        |---------------|-------------|
        """.trimIndent()
    )
    instructionList.sortedBy { it.exampleFormat }.forEach {
        append("\n|${it.exampleFormat}|${it.description}|")
    }
}
