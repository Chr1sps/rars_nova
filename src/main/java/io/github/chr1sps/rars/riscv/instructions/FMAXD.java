package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.jsoftfloat.Environment;
import io.github.chr1sps.jsoftfloat.types.Float64;

public class FMAXD extends Double {
    public FMAXD() {
        super("fmax.d", "Floating MAXimum (64 bit): assigns f1 to the larger of f1 and f3", "0010101", "001");
    }

    public Float64 compute(Float64 f1, Float64 f2, Environment env) {
        return io.github.chr1sps.jsoftfloat.operations.Comparisons.maximumNumber(f1, f2, env);
    }
}
