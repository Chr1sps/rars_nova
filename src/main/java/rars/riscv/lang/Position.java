package rars.riscv.lang;

/**
 * Represents a position within a text document.
 * <p>
 * A Position object is defined by three properties:
 * - The line number (line), starting from 1.
 * - The column number (column), starting from 0.
 * - The offset from the beginning of the document (offset), starting from 0.
 * <p>
 * Instances of this record are immutable and can be used to pinpoint specific
 * locations in a text document, useful for editors, compilers, or any tool
 * that processes text and needs to report errors or positions within the text.
 *
 * @author Chr1sps
 */
public record Position(int line, int column, int offset) {
}
