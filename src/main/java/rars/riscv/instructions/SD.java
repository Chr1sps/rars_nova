package rars.riscv.instructions;

import org.jetbrains.annotations.NotNull;
import rars.exceptions.AddressErrorException;
import rars.riscv.hardware.Memory;

public final class SD extends Store {
    public static final SD INSTANCE = new SD();

    private SD() {
        super(
            "sd t1, -100(t2)", "Store double word : Store contents of t1 into effective memory double word address",
            "011"
        );
    }

    @Override
    public void store(final int address, final long data, final @NotNull Memory memory) throws AddressErrorException {
        memory.setDoubleWord(address, data);
    }
}
