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

    private final String name, commandDescriptor, description, extension;

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
     * @param extension         Standard file extension for this format. Null if
     *                          none.
     */
    public AbstractDumpFormat(final String name, final String commandDescriptor,
                              final String description, final String extension) {
        this.name = name;
        this.commandDescriptor = (commandDescriptor == null) ? null : commandDescriptor.replaceAll(" ", "");
        this.description = description;
        this.extension = extension;
    }

    /**
     * Get the file extension associated with this format.
     *
     * @return String containing file extension -- without the leading "." -- or
     * null if there is no standard extension.
     */
    public String getFileExtension() {
        return extension;
    }

    /**
     * Get a short description of the format, suitable for displaying along with
     * the extension, in the file save dialog, or as a tool tip.
     *
     * @return String containing short description to go with the extension
     * or for use as tool tip. Possibly null.
     */
    public @NotNull String getDescription() {
        return description;
    }

    /**
     * String representing this object.
     *
     * @return Name given for this object.
     */
    public @NotNull String toString() {
        return name;
    }

    /**
     * One-word description of format to be used by RARS command mode parser
     * and user in conjunction with the "dump" option.
     *
     * @return One-word String describing the format.
     */
    public @NotNull String getCommandDescriptor() {
        return commandDescriptor;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Write memory contents according to the
     * specification for this format.
     */
    public abstract void dumpMemoryRange(@NotNull File file, int firstAddress, int lastAddress, @NotNull Memory memory)
            throws AddressErrorException, IOException;

}
