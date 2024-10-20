package rars.riscv;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.assembler.TokenList;
import rars.exceptions.SimulationException;

public final class ExtendedInstructionNew implements InstructionNew {
    private final String description;
    private final String exampleFormat;
    private final String name;

    public ExtendedInstructionNew(final String exampleFormat, final String description, final String... translationLayers) {
        this.description = description;
        this.exampleFormat = exampleFormat;
        this.name = InstructionNew.extractOperator(exampleFormat);
    }

    @Override
    public @NotNull String getName() {
        return this.name;
    }

    @Override
    public int getInstructionLength() {
        return 0;
    }

    @Override
    public @NotNull TokenList getTokenList() {
        return new TokenList();
    }

    @Override
    public @NotNull String getDescription() {
        return this.description;
    }

    @Override
    public @NotNull String getExampleFormat() {
        return this.exampleFormat;
    }

    @Override
    public void simulate(final ProgramStatement statement) throws SimulationException {

    }

//    private final @Nullable TranslationLayer createTranslationLayer(@NotNull String translationLayerString) {
//       
//    }

    private sealed interface Operand {
        record ConcreteRegister(int number) implements Operand {
        }

        record Immediate(int value) implements Operand {
        }

        record ArgRegister(int number) implements Operand {
        }
    }

    private record TranslationLayer(BasicInstructionNew instruction, Operand[] operands) {
    }
}
