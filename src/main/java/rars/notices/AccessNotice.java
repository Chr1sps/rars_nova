package rars.notices;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

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

import org.jetbrains.annotations.NotNull;

/**
 * Object provided to Observers of runtime access to memory or registers.
 * The access types READ and WRITE defined here; use subclasses defined for
 * MemoryAccessNotice and RegisterAccessNotice. This is abstract class.
 *
 * @author Pete Sanderson
 * @version July 2005
 */
public abstract sealed class AccessNotice implements Notice permits MemoryAccessNotice, RegisterAccessNotice {

    private final AccessType accessType;
    private final Thread thread;

    /**
     * <p>Constructor for AccessNotice.</p>
     *
     * @param type a int
     */
    protected AccessNotice(final @NotNull AccessType type) {
        this.accessType = type;
        this.thread = Thread.currentThread();
    }

    /**
     * Get the access type: READ or WRITE.
     *
     * @return Access type, either AccessNotice.READ or AccessNotice.WRITE
     */
    public @NotNull AccessType getAccessType() {
        return this.accessType;
    }

    /**
     * Query whether the access originated from executing program
     *
     * @return true if this access originated from executing program, false
     * otherwise
     */
    // Thread to execute the MIPS program is instantiated in SwingWorker.java.
    // There it is given the name "RISCV" to replace the default "Thread-x".
    // TODO: there should be a better way than this; I think that this is always
    // true or should be for all usages
    public boolean accessIsFromRISCV() {
        return this.thread.getName().startsWith("RISCV");
    }

    public enum AccessType {
        READ, WRITE
    }

}
