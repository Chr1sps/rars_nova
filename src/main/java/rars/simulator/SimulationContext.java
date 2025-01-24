package rars.simulator;

import org.jetbrains.annotations.NotNull;
import rars.io.AbstractIO;
import rars.riscv.hardware.Memory;
import rars.riscv.hardware.registerFiles.CSRegisterFile;
import rars.riscv.hardware.registerFiles.FloatingPointRegisterFile;
import rars.riscv.hardware.registerFiles.RegisterFile;

public record SimulationContext(
    @NotNull RegisterFile registerFile,
    @NotNull FloatingPointRegisterFile fpRegisterFile,
    @NotNull CSRegisterFile csrRegisterFile,
    @NotNull Memory memory,
    @NotNull AbstractIO io
) {
}
