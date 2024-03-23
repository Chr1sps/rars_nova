package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.riscv.hardware.AddressErrorException;

public class LWU extends Load {
    public LWU() {
        super("lwu t1, -100(t2)", "Set t1 to contents of effective memory word address without sign-extension", "110",
                true);
    }

    public long load(int address) throws AddressErrorException {
        return Globals.memory.getWord(address) & 0xFFFF_FFFFL;
    }
}
