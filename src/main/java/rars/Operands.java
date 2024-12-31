package rars;

import java.util.stream.Stream;

sealed interface Operand {
    enum RoundingMode implements Operand {
        RNE(0), RTZ(1), RDN(2), RUP(3), RMM(4), DYN(7);
        public final int value;

        RoundingMode(int value) {
            this.value = value;
        }
    }

    record Register(int registerNumber) implements Operand {
    }

    record FloatingRegister(int registerNumber) implements Operand {
    }

    record CsrRegister(int registerNumber) implements Operand {
    }

    record Identifier(int address) implements Operand {
    }

    record Integral(long value) implements Operand {
    }

    record Floating(double value) implements Operand {
    }

    record Void() implements Operand {
    }
}

public record Operands<
    First extends Operand,
    Second extends Operand,
    Third extends Operand,
    Fourth extends Operand,
    Fifth extends Operand>(First first, Second second, Third third, Fourth fourth, Fifth fifth) {
    public int getLength() {
        return (int) Stream.of(first, second, third, fourth, fifth)
            .takeWhile(operand -> !(operand instanceof Operand.Void))
            .count();
    }
}
