package rars.riscv;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.ExitingException;

/**
 * Abstract class that a syscall system service must extend. A qualifying
 * service
 * must be a class in the com.chrisps.rars.riscv.syscalls package, must be
 * compiled into a .class file.
 * Rars will detect a qualifying syscall upon startup, create an instance
 * using its no-argument constructor and add it to its syscall list.
 * When its service is invoked at runtime ("ecall" instruction
 * with its service number stored in register a7), its simulate()
 * method will be invoked.
 */
public abstract class AbstractSyscall implements Comparable<AbstractSyscall> {
    private final String serviceName;
    private final String description;
    private final String inputs;
    private final String outputs;
    private int serviceNumber;

    /**
     * Constructor is provided so subclass may initialize instance variables.
     *
     * @param name
     *     service name which may be used for reference independent of
     *     number
     */
    protected AbstractSyscall(final String name) {
        this(name, "N/A");
    }

    /**
     * <p>Constructor for AbstractSyscall.</p>
     *
     * @param name
     *     service name which may be used for reference independent of
     *     number
     * @param descr
     *     a hort description of what the system calll does
     */
    protected AbstractSyscall(final String name, final String descr) {
        this(name, descr, "N/A", "N/A");
    }

    /**
     * <p>Constructor for AbstractSyscall.</p>
     *
     * @param name
     *     service name which may be used for reference independent of
     *     number
     * @param descr
     *     a short description of what the system call does
     * @param in
     *     a description of what registers should be set to before the
     *     system call
     * @param out
     *     a description of what registers are set to after the system call
     */
    protected AbstractSyscall(final String name, final String descr, final String in, final String out) {
        serviceNumber = -1;
        serviceName = name;
        description = descr;
        inputs = in;
        outputs = out;
    }

    /**
     * Return the name you have chosen for this syscall. This can be used by a RARS
     * user to refer to the service when choosing to override its default service
     * number in the configuration file.
     *
     * @return service name as a string
     */
    public @NotNull String getName() {
        return serviceName;
    }

    /**
     * <p>Getter for the field {@code description}.</p>
     *
     * @return a string describing what the system call does
     */
    public @NotNull String getDescription() {
        return description;
    }

    /**
     * <p>Getter for the field {@code inputs}.</p>
     *
     * @return a string documenting what registers should be set to before the
     * system call runs
     */
    public @NotNull String getInputs() {
        return inputs;
    }

    /**
     * <p>Getter for the field {@code outputs}.</p>
     *
     * @return a string documenting what registers are set to after the system call
     * runs
     */
    public @NotNull String getOutputs() {
        return outputs;
    }

    /**
     * Return the assigned service number. This is the number the programmer
     * must store into a7 before issuing the ECALL instruction.
     *
     * @return assigned service number
     */
    public int getNumber() {
        return serviceNumber;
    }

    /**
     * Set the service number. This is provided to allow MARS implementer or user
     * to override the default service number.
     *
     * @param num
     *     specified service number to override the default.
     */
    public void setNumber(final int num) {
        serviceNumber = num;
    }

    /**
     * Performs syscall function. It will be invoked when the service is invoked
     * at simulation time. Service is identified by value stored in a7.
     *
     * @throws ExitingException
     *     if any.
     */
    public abstract void simulate(@NotNull ProgramStatement statement)
        throws ExitingException;

    /**
     * <p>compareTo.</p>
     *
     * @param other
     *     a {@link AbstractSyscall} object
     * @return a int
     */
    @Override
    public int compareTo(@NotNull final AbstractSyscall other) {
        if (this == other) {
            return 0;
        }
        assert serviceNumber != other.serviceNumber : "Different syscalls have to have different numbers";
        return serviceNumber > other.serviceNumber ? 1 : -1;
    }

}
