package rars.riscv.dump.formats

import rars.riscv.dump.DumpFormat

/**
 * Abstract class for memory dump file formats. Provides constructors and
 * defaults for everything except the dumpMemoryRange method itself.
 *
 * @author Pete Sanderson
 * @version December 2007
 */
abstract class AbstractDumpFormat(
    override val name: String,
    commandDescriptor: String,
    override val description: String
) : DumpFormat {
    override val commandDescriptor: String = commandDescriptor.replace(" ".toRegex(), "")
}
