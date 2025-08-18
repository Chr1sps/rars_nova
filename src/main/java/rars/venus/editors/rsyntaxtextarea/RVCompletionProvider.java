package rars.venus.editors.rsyntaxtextarea;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;
import org.jetbrains.annotations.NotNull;
import rars.assembler.Directive;
import rars.riscv.InstructionsRegistry;
import rars.riscv.hardware.registerFiles.CSRegisterFile;
import rars.riscv.hardware.registerFiles.FloatingPointRegisterFile;
import rars.riscv.hardware.registerFiles.RegisterFile;
import rars.riscv.hardware.registerFiles.RegisterFileBase;

import java.awt.event.MouseEvent;

public final class RVCompletionProvider extends DefaultCompletionProvider implements ToolTipSupplier {

    public RVCompletionProvider(
        final @NotNull RegisterFile intRegisters,
        final @NotNull FloatingPointRegisterFile fpRegisters,
        final @NotNull CSRegisterFile csRegisters
    ) {
        super();
        this.setAutoActivationRules(true, ".");

        addCompletionsForRegisterFile(this, intRegisters, true);
        addCompletionsForRegisterFile(this, fpRegisters, true);
        addCompletionsForRegisterFile(this, csRegisters, false);

        for (final var directive : Directive.values()) {
            this.addCompletion(new CompletionWithToolTip(
                this,
                directive.getName(),
                "Directive " + directive.getName(),
                directive.getDescription()
            ));
        }

        InstructionsRegistry.ALL_INSTRUCTIONS.allInstructions.forEach(
            instruction -> this.addCompletion(new CompletionWithToolTip(
                this,
                instruction.mnemonic,
                instruction.exampleFormat,
                instruction.description
            ))
        );
    }

    @Override
    public String getToolTipText(RTextArea textArea, MouseEvent e) {
        final var completions = getCompletionsAt(textArea, e.getPoint());
        if (completions != null && !completions.isEmpty()) {
            return completions.getFirst().getToolTipText();
        } else {
            return null;
        }
    }

    @Override
    protected boolean isValidChar(char ch) {
        return super.isValidChar(ch) || ch == '.';
    }

    private static void addCompletionsForRegisterFile(
        final @NotNull DefaultCompletionProvider provider,
        final @NotNull RegisterFileBase registerFile,
        final boolean addNumberedCompletions
    ) {
        for (final var register : registerFile.getRegisters()) {
            provider.addCompletion(new CompletionWithToolTip(
                provider,
                register.name,
                "Register " + register.name
            ));
            // pc has it's number set as -1 hence the additional check
            if (addNumberedCompletions && register.number >= 0) {
                provider.addCompletion(new CompletionWithToolTip(
                    provider,
                    String.valueOf(registerFile.registerNumberPrefix) + register.number,
                    "Register " + register.name
                ));
            }
        }
    }

    private static final class CompletionWithToolTip extends BasicCompletion {

        public CompletionWithToolTip(
            CompletionProvider provider,
            String replacementText,
            String shortDesc,
            String summary
        ) {
            super(provider, replacementText, shortDesc, summary);
        }

        public CompletionWithToolTip(
            CompletionProvider provider,
            String replacementText,
            String shortDesc
        ) {
            super(provider, replacementText, shortDesc);
        }

        @Override
        public @NotNull String getToolTipText() {
            final var builder= new StringBuilder();
            builder.append(getShortDescription());
            final var summary = getSummary();
            if (summary != null) {
                builder.append("\n\n");
                builder.append(summary);
            }
            return builder.toString();
        }
    }
}
