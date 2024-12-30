package rars.riscv.dump.formats;

import org.jetbrains.annotations.NotNull;
import rars.exceptions.AddressErrorException;
import rars.riscv.dump.DumpFormat;
import rars.riscv.hardware.Memory;

import java.io.File;
import java.io.IOException;

/**
 * Abstract class for memory dump file formats. Provides constructors and
 * defaults for everything except the dumpMemoryRange method itself.
 *
 * @author Pete Sanderson
 * @version December 2007
 */
public abstract class AbstractDumpFormat implements DumpFormat {

    private final String name, commandDescriptor, description;

    /**
     * Typical constructor. Note you cannot creates objects from this
     * class but subclass constructor can call this one.
     *
     * @param name              Brief descriptive name to be displayed in selection
     *                          list.
     * @param commandDescriptor One-word descriptive name to be used by RARS command
     *                          mode parser and user.
     *                          Any spaces in this string will be removed.
     * @param description       Description to go with standard file extension for
     *                          display in file save dialog or to be used as tool
     *                          tip.
     */
    public AbstractDumpFormat(
        final @NotNull String name,
        final @NotNull String commandDescriptor,
        final @NotNull String description
    ) {
        this.name = name;
        this.commandDescriptor = commandDescriptor.replaceAll(" ", "");
        this.description = description;
    }

    @Override
    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull String toString() {
        return name;
    }

    /**
     * One-word description of format to be used by RARS command mode parser
     * and user in conjunction with the "dump" option.
     *
     * @return One-word String describing the format.
     */
    @Override
    public @NotNull String getCommandDescriptor() {
        return commandDescriptor;
    }

    @Override
    public abstract void dumpMemoryRange(final @NotNull File file, int firstAddress, int lastAddress,
                                         @NotNull Memory memory)
        throws AddressErrorException, IOException;

}
