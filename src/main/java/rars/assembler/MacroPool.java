package rars.assembler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.RISCVProgram;

import java.util.ArrayList;

/**
 * Stores information of macros defined by now. <br>
 * Will be used in first pass of assembling RISCV source code. When reached
 * {@code .macro} directive, parser calls
 * {@link MacroPool#beginMacro(Token)} and skips source code lines until
 * reaches {@code .end_macro} directive. then calls
 * {@link MacroPool#commitMacro(Token)} and the macro information stored in a
 * {@link Macro} instance will be added to {@link #macroList}. <br>
 * Each {@link RISCVProgram} will have one {@link MacroPool}<br>
 * NOTE: Forward referencing macros (macro expansion before its definition in
 * source code) and Nested macro definition (defining a macro inside other macro
 * definition) are not supported.
 *
 * @author M.H.Sekhavat sekhavat17@gmail.com
 */
public final class MacroPool {
    private final @NotNull RISCVProgram program;
    /**
     * List of macros defined by now
     */
    private final @NotNull ArrayList<Macro> macroList;
    private final @NotNull ArrayList<Integer> callStack;
    private final @NotNull ArrayList<Integer> callStackOrigLines;
    /**
     * @see #beginMacro(Token)
     */
    private @Nullable Macro current;
    /**
     * @see #getNextCounter()
     */
    private int counter;

    /**
     * Create an empty MacroPool for given program
     *
     * @param program
     *     associated program
     */
    public MacroPool(final @NotNull RISCVProgram program) {
        this.program = program;
        this.macroList = new ArrayList<>();
        this.callStack = new ArrayList<>();
        this.callStackOrigLines = new ArrayList<>();
        this.current = null;
        this.counter = 0;
    }

    /**
     * This method will be called by parser when reached {@code .macro}
     * directive.<br>
     * Instantiates a new {@link Macro} object and stores it in {@link #current}.
     * {@link #current} will be added to {@link #macroList} by
     * {@link #commitMacro(Token)}
     *
     * @param nameToken
     *     Token containing name of macro after {@code .macro}
     *     directive
     */
    public void beginMacro(final @NotNull Token nameToken) {
        this.current = new Macro();
        this.current.setName(nameToken.getText());
        this.current.setFromLine(nameToken.getSourceLine());
        this.current.setOriginalFromLine(nameToken.getOriginalSourceLine());
        this.current.setProgram(this.program);
    }

    /**
     * This method will be called by parser when reached {@code .end_macro}
     * directive. <br>
     * Adds/Replaces {@link #current} macro into the {@link #macroList}.
     *
     * @param endToken
     *     Token containing {@code .end_macro} directive in source
     *     code
     */
    public void commitMacro(final @NotNull Token endToken) {
        assert this.current != null : "commitMacro called without beginMacro";
        this.current.setToLine(endToken.getSourceLine());
        this.current.setOriginalToLine(endToken.getOriginalSourceLine());
        this.current.readyForCommit();
        this.macroList.add(this.current);
        this.current = null;
    }

    /**
     * Will be called by parser when reaches a macro expansion call
     *
     * @param tokens
     *     tokens passed to macro expansion call
     * @return {@link Macro} object matching the name and argument count of
     * tokens passed
     */
    public @Nullable Macro getMatchingMacro(final @NotNull TokenList tokens) {
        if (tokens.isEmpty()) {
            return null;
        }
        Macro ret = null;
        final Token firstToken = tokens.get(0);
        for (final Macro macro : this.macroList) {
            if (macro.getName().equals(firstToken.getText())
                && macro.getArgs().size() + 1 == tokens.size()
                && (ret == null || ret.getFromLine() < macro.getFromLine())) {
                ret = macro;
            }
        }
        return ret;
    }

    /**
     * <p>matchesAnyMacroName.</p>
     *
     * @param value
     *     a {@link java.lang.String} object
     * @return true if any macros have been defined with name {@code value}
     * by now, not concerning arguments count.
     */
    public boolean matchesAnyMacroName(final @NotNull String value) {
        for (final Macro macro : this.macroList) {
            if (macro.getName().equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Getter for the field {@code current}.</p>
     *
     * @return a {@link Macro} object
     */
    public @Nullable Macro getCurrent() {
        return this.current;
    }

    /**
     * {@link #counter} will be set to 0 on construction of this class and will
     * be incremented by each call. parser calls this method once for every
     * expansions. it will be a unique id for each expansion of macro in a file
     *
     * @return counter value
     */
    public int getNextCounter() {
        final int i = this.counter;
        this.counter++;
        return i;
    }

    /**
     * returns true if detected expansion loop
     */
    public boolean pushOnCallStack(final @NotNull Token token) {
        final int sourceLine = token.getSourceLine();
        final int origSourceLine = token.getOriginalSourceLine();
        if (this.callStack.contains(sourceLine)) {
            return true;
        }
        this.callStack.add(sourceLine);
        this.callStackOrigLines.add(origSourceLine);
        return false;
    }

    /**
     * <p>popFromCallStack.</p>
     */
    public void popFromCallStack() {
        this.callStack.removeLast();
        this.callStackOrigLines.removeLast();
    }

    /**
     * <p>getExpansionHistory.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public @NotNull String getExpansionHistory() {
        final StringBuilder ret = new StringBuilder();
        for (int i = 0; i < this.callStackOrigLines.size(); i++) {
            if (i > 0) {
                ret.append("->");
            }
            ret.append(this.callStackOrigLines.get(i).toString());
        }
        return ret.toString();
    }
}
