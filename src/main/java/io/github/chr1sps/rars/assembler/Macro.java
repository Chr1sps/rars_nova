package io.github.chr1sps.rars.assembler;

import java.util.ArrayList;
import java.util.Collections;

import io.github.chr1sps.rars.ErrorList;
import io.github.chr1sps.rars.ErrorMessage;
import io.github.chr1sps.rars.RISCVprogram;
import io.github.chr1sps.rars.riscv.hardware.FloatingPointRegisterFile;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

/*
Copyright (c) 2013-2014.

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
 * Stores information of a macro definition.
 *
 * @author M.H.Sekhavat sekhavat17@gmail.com
 */
public class Macro {
    private String name;
    private RISCVprogram program;
    private ArrayList<String> labels;

    /**
     * first and last line number of macro definition. first line starts with
     * .macro directive and last line is .end_macro directive.
     */
    private int fromLine, toLine;
    private int origFromLine, origToLine;
    /**
     * arguments like <code>%arg</code> will be substituted by macro expansion
     */
    private ArrayList<String> args;

    /**
     * <p>Constructor for Macro.</p>
     */
    public Macro() {
        name = "";
        program = null;
        fromLine = toLine = 0;
        origFromLine = origToLine = 0;
        args = new ArrayList<>();
        labels = new ArrayList<>();
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param name a {@link java.lang.String} object
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>Getter for the field <code>program</code>.</p>
     *
     * @return a {@link io.github.chr1sps.rars.RISCVprogram} object
     */
    public RISCVprogram getProgram() {
        return program;
    }

    /**
     * <p>Setter for the field <code>program</code>.</p>
     *
     * @param program a {@link io.github.chr1sps.rars.RISCVprogram} object
     */
    public void setProgram(RISCVprogram program) {
        this.program = program;
    }

    /**
     * <p>Getter for the field <code>fromLine</code>.</p>
     *
     * @return a int
     */
    public int getFromLine() {
        return fromLine;
    }

    /**
     * <p>getOriginalFromLine.</p>
     *
     * @return a int
     */
    public int getOriginalFromLine() {
        return this.origFromLine;
    }

    /**
     * <p>Setter for the field <code>fromLine</code>.</p>
     *
     * @param fromLine a int
     */
    public void setFromLine(int fromLine) {
        this.fromLine = fromLine;
    }

    /**
     * <p>setOriginalFromLine.</p>
     *
     * @param origFromLine a int
     */
    public void setOriginalFromLine(int origFromLine) {
        this.origFromLine = origFromLine;
    }

    /**
     * <p>Getter for the field <code>toLine</code>.</p>
     *
     * @return a int
     */
    public int getToLine() {
        return toLine;
    }

    /**
     * <p>getOriginalToLine.</p>
     *
     * @return a int
     */
    public int getOriginalToLine() {
        return this.origToLine;
    }

    /**
     * <p>Setter for the field <code>toLine</code>.</p>
     *
     * @param toLine a int
     */
    public void setToLine(int toLine) {
        this.toLine = toLine;
    }

    /**
     * <p>setOriginalToLine.</p>
     *
     * @param origToLine a int
     */
    public void setOriginalToLine(int origToLine) {
        this.origToLine = origToLine;
    }

    /**
     * <p>Getter for the field <code>args</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object
     */
    public ArrayList<String> getArgs() {
        return args;
    }

    /**
     * <p>Setter for the field <code>args</code>.</p>
     *
     * @param args a {@link java.util.ArrayList} object
     */
    public void setArgs(ArrayList<String> args) {
        this.args = args;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Macro) {
            Macro macro = (Macro) obj;
            return macro.getName().equals(name) && (macro.args.size() == args.size());
        }
        return super.equals(obj);
    }

    /**
     * <p>addArg.</p>
     *
     * @param value a {@link java.lang.String} object
     */
    public void addArg(String value) {
        args.add(value);
    }

    /**
     * Substitutes macro arguments in a line of source code inside macro
     * definition to be parsed after macro expansion. <br>
     * Also appends "_M#" to all labels defined inside macro body where # is value
     * of <code>counter</code>
     *
     * @param line    source line number in macro definition to be substituted
     * @param args    a {@link io.github.chr1sps.rars.assembler.TokenList} object
     * @param counter unique macro expansion id
     * @param errors  a {@link io.github.chr1sps.rars.ErrorList} object
     * @return <code>line</code>-th line of source code, with substituted
     * arguments
     */
    public String getSubstitutedLine(int line, TokenList args, long counter, ErrorList errors) {
        TokenList tokens = program.getTokenList().get(line - 1);
        String s = program.getSourceLine(line);

        for (int i = tokens.size() - 1; i >= 0; i--) {
            Token token = tokens.get(i);
            if (tokenIsMacroParameter(token.getValue(), true)) {
                int repl = -1;
                for (int j = 0; j < this.args.size(); j++) {
                    if (this.args.get(j).equals(token.getValue())) {
                        repl = j;
                        break;
                    }
                }
                String substitute = token.getValue();
                if (repl != -1)
                    substitute = args.get(repl + 1).toString();
                else {
                    errors.add(new ErrorMessage(program, token.getSourceLine(),
                            token.getStartPos(), "Unknown macro parameter"));
                }
                s = replaceToken(s, token, substitute);
            } else if (tokenIsMacroLabel(token.getValue())) {
                String substitute = token.getValue() + "_M" + counter;
                s = replaceToken(s, token, substitute);
            }
        }
        return s;
    }

    /**
     * returns true if <code>value</code> is name of a label defined in this macro's
     * body.
     *
     * @param value
     * @return
     */
    private boolean tokenIsMacroLabel(String value) {
        return (Collections.binarySearch(labels, value) >= 0);
    }

    /**
     * replaces token <code>tokenToBeReplaced</code> which is occurred in
     * <code>source</code> with <code>substitute</code>.
     *
     * @param source
     * @param tokenToBeReplaced
     * @param substitute
     * @return
     */
    // Initially the position of the substitute was based on token position but that
    // proved problematic
    // in that the source string does not always match the token list from which the
    // token comes. The
    // token list has already had .eqv equivalences applied whereas the source may
    // not. This is because
    // the source comes from a macro definition? That has proven to be a tough
    // question to answer.
    // DPS 12-feb-2013
    private String replaceToken(String source, Token tokenToBeReplaced, String substitute) {
        String stringToBeReplaced = tokenToBeReplaced.getValue();
        int pos = source.indexOf(stringToBeReplaced);
        return (pos < 0) ? source
                : source.substring(0, pos) + substitute + source.substring(pos + stringToBeReplaced.length());
    }

    /**
     * returns whether <code>tokenValue</code> is macro parameter or not
     *
     * @param tokenValue                a {@link java.lang.String} object
     * @param acceptSpimStyleParameters accepts SPIM-style parameters which begin
     *                                  with '$' if true
     * @return a boolean
     */
    public static boolean tokenIsMacroParameter(String tokenValue, boolean acceptSpimStyleParameters) {
        if (acceptSpimStyleParameters) {
            // Bug fix: SPIM accepts parameter names that start with $ instead of %. This
            // can
            // lead to problems since register names also start with $. This IF condition
            // should filter out register names. Originally filtered those from regular set
            // but not
            // from ControlAndStatusRegisterFile or FloatingPointRegisterFile register sets.
            // Expanded the condition.
            // DPS 7-July-2014.
            if (tokenValue.length() > 0 && tokenValue.charAt(0) == '$' &&
                    RegisterFile.getRegister(tokenValue) == null &&
                    FloatingPointRegisterFile.getRegister(tokenValue) == null) // added 7-July-2014
            {
                return true;
            }
        }
        return tokenValue.length() > 1 && tokenValue.charAt(0) == '%';
    }

    /**
     * <p>addLabel.</p>
     *
     * @param value a {@link java.lang.String} object
     */
    public void addLabel(String value) {
        labels.add(value);
    }

    /**
     * Operations to be done on this macro before it is committed in macro pool.
     */
    public void readyForCommit() {
        Collections.sort(labels);
    }
}
