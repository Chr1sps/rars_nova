package rars.riscv.lang

/**
 * Represents a position within a text document.
 *
 *
 * A Position object is defined by three properties:
 * - The line number (line), starting from 1.
 * - The column number (column), starting from 0.
 * - The offset from the beginning of the document (offset), starting from 0.
 *
 *
 * Instances of this record are immutable and can be used to pinpoint specific
 * locations in a text document, useful for editors, compilers, or any tool
 * that processes text and needs to report errors or positions within the text.
 *
 * @author Chr1sps
 */
data class Position(val line: Int, val column: Int, val offset: Int)
