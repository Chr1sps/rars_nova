package io.github.chr1sps.rars.api;

/**
 * <p>Options class.</p>
 *
 */
public class Options {
    public boolean pseudo; // pseudo instructions allowed in source code or not.
    public boolean warningsAreErrors; // Whether assembler warnings should be considered errors.
    public boolean startAtMain; // Whether to start execution at statement labeled 'main'
    public boolean selfModifyingCode; // Whether to allow self-modifying code (e.g. write to text segment)
    public int maxSteps;

    /**
     * <p>Constructor for Options.</p>
     */
    public Options() {
        pseudo = true;
        warningsAreErrors = false;
        startAtMain = false;
        selfModifyingCode = false;
        maxSteps = -1;
    }
}