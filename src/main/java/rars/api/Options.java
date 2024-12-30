package rars.api;

public final class Options {
    public boolean usePseudoInstructions; // pseudo instructions allowed in source code or not.
    public boolean warningsAreErrors; // Whether assembler warnings should be considered errors.
    public boolean startAtMain; // Whether to start execution at statement labeled 'main'
    public boolean selfModifyingCode; // Whether to allow self-modifying code (e.g. write to text segment)
    public int maxSteps;

    public Options() {
        this.usePseudoInstructions = true;
        this.warningsAreErrors = false;
        this.startAtMain = false;
        this.selfModifyingCode = false;
        this.maxSteps = -1;
    }
}