package io.github.chr1sps.rars.assembler;

import io.github.chr1sps.rars.RISCVprogram;

import java.util.ArrayList;

/*
Copyright (c) 2013.

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Stores information of macros defined by now. <br>
 * Will be used in first pass of assembling RISCV source code. When reached
 * <code>.macro</code> directive, parser calls
 * {@link io.github.chr1sps.rars.assembler.MacroPool#beginMacro(Token)} and skips source code lines until
 * reaches <code>.end_macro</code> directive. then calls
 * {@link io.github.chr1sps.rars.assembler.MacroPool#commitMacro(Token)} and the macro information stored in a
 * {@link io.github.chr1sps.rars.assembler.Macro} instance will be added to {@link #macroList}. <br>
 * Each {@link io.github.chr1sps.rars.RISCVprogram} will have one {@link io.github.chr1sps.rars.assembler.MacroPool}<br>
 * NOTE: Forward referencing macros (macro expansion before its definition in
 * source code) and Nested macro definition (defining a macro inside other macro
 * definition) are not supported.
 *
 * @author M.H.Sekhavat sekhavat17@gmail.com
 */
public class MacroPool {
    private final RISCVprogram program;
    /**
     * List of macros defined by now
     */
    private final ArrayList<Macro> macroList;
    /**
     * @see #beginMacro(Token)
     */
    private Macro current;
    private final ArrayList<Integer> callStack;
    private final ArrayList<Integer> callStackOrigLines;
    /**
     * @see #getNextCounter()
     */
    private int counter;

    /**
     * Create an empty MacroPool for given program
     *
     * @param program associated program
     */
    public MacroPool(final RISCVprogram program) {
        this.program = program;
        this.macroList = new ArrayList<>();
        this.callStack = new ArrayList<>();
        this.callStackOrigLines = new ArrayList<>();
        this.current = null;
        this.counter = 0;
    }

    /**
     * This method will be called by parser when reached <code>.macro</code>
     * directive.<br>
     * Instantiates a new {@link io.github.chr1sps.rars.assembler.Macro} object and stores it in {@link #current}
     * . {@link #current} will be added to {@link #macroList} by
     * {@link #commitMacro(Token)}
     *
     * @param nameToken Token containing name of macro after <code>.macro</code>
     *                  directive
     */
    public void beginMacro(final Token nameToken) {
        this.current = new Macro();
        this.current.setName(nameToken.getValue());
        this.current.setFromLine(nameToken.getSourceLine());
        this.current.setOriginalFromLine(nameToken.getOriginalSourceLine());
        this.current.setProgram(this.program);
    }

    /**
     * This method will be called by parser when reached <code>.end_macro</code>
     * directive. <br>
     * Adds/Replaces {@link #current} macro into the {@link #macroList}.
     *
     * @param endToken Token containing <code>.end_macro</code> directive in source
     *                 code
     */
    public void commitMacro(final Token endToken) {
        this.current.setToLine(endToken.getSourceLine());
        this.current.setOriginalToLine(endToken.getOriginalSourceLine());
        this.current.readyForCommit();
        this.macroList.add(this.current);
        this.current = null;
    }

    /**
     * Will be called by parser when reaches a macro expansion call
     *
     * @param tokens     tokens passed to macro expansion call
     * @param callerLine a int
     * @return {@link io.github.chr1sps.rars.assembler.Macro} object matching the name and argument count of
     * tokens passed
     */
    public Macro getMatchingMacro(final TokenList tokens, final int callerLine) {
        if (tokens.isEmpty())
            return null;
        Macro ret = null;
        final Token firstToken = tokens.get(0);
        for (final Macro macro : this.macroList) {
            if (macro.getName().equals(firstToken.getValue())
                    && macro.getArgs().size() + 1 == tokens.size()
                    // && macro.getToLine() < callerLine // condition removed; doesn't work nicely
                    // in conjunction with .include, and does not seem necessary. DPS 8-MAR-2013
                    && (ret == null || ret.getFromLine() < macro.getFromLine()))
                ret = macro;
        }
        return ret;
    }

    /**
     * <p>matchesAnyMacroName.</p>
     *
     * @param value a {@link java.lang.String} object
     * @return true if any macros have been defined with name <code>value</code>
     * by now, not concerning arguments count.
     */
    public boolean matchesAnyMacroName(final String value) {
        for (final Macro macro : this.macroList)
            if (macro.getName().equals(value))
                return true;
        return false;
    }

    /**
     * <p>Getter for the field <code>current</code>.</p>
     *
     * @return a {@link io.github.chr1sps.rars.assembler.Macro} object
     */
    public Macro getCurrent() {
        return this.current;
    }

    /**
     * <p>Setter for the field <code>current</code>.</p>
     *
     * @param current a {@link io.github.chr1sps.rars.assembler.Macro} object
     */
    public void setCurrent(final Macro current) {
        this.current = current;
    }

    /**
     * {@link #counter} will be set to 0 on construction of this class and will
     * be incremented by each call. parser calls this method once for every
     * expansions. it will be a unique id for each expansion of macro in a file
     *
     * @return counter value
     */
    public int getNextCounter() {
        return this.counter++;
    }

    /**
     * <p>Getter for the field <code>callStack</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object
     */
    public ArrayList<Integer> getCallStack() {
        return this.callStack;
    }

    /**
     * <p>pushOnCallStack.</p>
     *
     * @param token a {@link io.github.chr1sps.rars.assembler.Token} object
     * @return a boolean
     */
    public boolean pushOnCallStack(final Token token) { // returns true if detected expansion loop
        final int sourceLine = token.getSourceLine();
        final int origSourceLine = token.getOriginalSourceLine();
        if (this.callStack.contains(sourceLine))
            return true;
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
    public String getExpansionHistory() {
        String ret = "";
        for (int i = 0; i < this.callStackOrigLines.size(); i++) {
            if (i > 0)
                ret += "->";
            ret += this.callStackOrigLines.get(i).toString();
        }
        return ret;
    }
}
