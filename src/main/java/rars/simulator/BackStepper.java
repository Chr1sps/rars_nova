package rars.simulator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rars.Globals;
import rars.ProgramStatement;
import rars.riscv.BasicInstruction;
import rars.riscv.hardware.ControlAndStatusRegisterFile;
import rars.riscv.hardware.FloatingPointRegisterFile;
import rars.riscv.hardware.RegisterFile;

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

/**
 * Used to "step backward" through execution, undoing each instruction.
 *
 * @author Pete Sanderson
 * @version February 2006
 */
public class BackStepper {
    private static final Logger LOGGER = LogManager.getLogger(BackStepper.class);
    // Flag to mark BackStep object as prepresenting specific situation: user
    // manipulates
    // memory/register second via GUI after assembling program but before running it.
    private static final int NOT_PC_VALUE = -1;
    private final BackstepStack backSteps;
    private boolean engaged;

    /**
     * Create a fresh BackStepper. It is enabled, which means all
     * subsequent instruction executions will have their "undo" action
     * recorded here.
     */
    public BackStepper() {
        this.engaged = true;
        this.backSteps = new BackstepStack(Globals.maximumBacksteps);
    }

    // One can argue using java.util.Stack, given its clumsy implementation.
    // A homegrown linked implementation will be more streamlined, but
    // I anticipate that backstepping will only be used during timed
    // (currently max 30 instructions/second) or stepped execution, where
    // performance is not an issue. Its Vector implementation may result
    // in quicker garbage collection than a pure linked list implementation.

    private static int pc() {
        // PC incremented prior to instruction simulation, so need to adjust for that.
        return RegisterFile.INSTANCE.getProgramCounter() - BasicInstruction.BASIC_INSTRUCTION_LENGTH;
    }

    /**
     * Determine whether execution "undo" steps are currently being recorded.
     *
     * @return true if undo steps being recorded, false if not.
     */
    public boolean enabled() {
        return this.engaged;
    }

    /**
     * Set enable status.
     *
     * @param state
     *     If true, will begin (or continue) recoding "undo" steps. If
     *     false, will stop.
     */
    public void setEnabled(final boolean state) {
        this.engaged = state;
    }

    /**
     * Test whether there are steps that can be undone.
     *
     * @return true if there are no steps to be undone, false otherwise.
     */
    public boolean empty() {
        return this.backSteps.empty();
    }

    /**
     * Carry out a "back step", which will undo the latest execution step.
     * Does nothing if backstepping not enabled or if there are no steps to undo.
     */

    // Note that there may be more than one "step" in an instruction execution; for
    // instance the multiply, divide, and double-precision floating point operations
    // all store their result in register pairs which results in two store
    // operations.
    // Both must be undone transparently, so we need to detect that multiple steps
    // happen
    // together and carry out all of them here.
    // Use a do-while loop based on the backstep's program statement reference.
    public void backStep() {
        if (this.engaged && !this.backSteps.empty()) {
            final ProgramStatement statement = this.backSteps.peek().ps;
            this.engaged = false; // GOTTA DO THIS SO METHOD CALL IN SWITCH WILL NOT RESULT IN NEW ACTION ON
            // STACK!
            do {
                final BackStep step = this.backSteps.pop();
                /*
                 * System.out.println("backstep POP: action "+step.action+" pc "+rars.util.
                 * Binary.intToHexString(step.pc)+
                 * " source "+((step.ps==null)? "none":step.ps.getSource())+
                 * " parm1 "+step.param1+" parm2 "+step.param2);
                 */
                if (step.pc != BackStepper.NOT_PC_VALUE) {
                    RegisterFile.INSTANCE.setProgramCounter(step.pc);
                }
                try {
                    switch (step.action) {
                        case MEMORY_RESTORE_RAW_WORD:
                            Globals.MEMORY_INSTANCE.setRawWord(step.param1, (int) step.param2);
                            break;
                        case MEMORY_RESTORE_DOUBLE_WORD:
                            Globals.MEMORY_INSTANCE.setDoubleWord(step.param1, step.param2);
                            break;
                        case MEMORY_RESTORE_WORD:
                            Globals.MEMORY_INSTANCE.setWord(step.param1, (int) step.param2);
                            break;
                        case MEMORY_RESTORE_HALF:
                            Globals.MEMORY_INSTANCE.setHalf(step.param1, (int) step.param2);
                            break;
                        case MEMORY_RESTORE_BYTE:
                            Globals.MEMORY_INSTANCE.setByte(step.param1, (int) step.param2);
                            break;
                        case REGISTER_RESTORE:
                            RegisterFile.INSTANCE.updateRegisterByNumber(step.param1, step.param2);
                            break;
                        case FLOATING_POINT_REGISTER_RESTORE:
                            FloatingPointRegisterFile.updateRegister(step.param1, step.param2);
                            break;
                        case CONTROL_AND_STATUS_REGISTER_RESTORE:
                            ControlAndStatusRegisterFile.updateRegister(step.param1, step.param2);
                            break;
                        case CONTROL_AND_STATUS_REGISTER_BACKDOOR:
                            ControlAndStatusRegisterFile.updateRegisterBackdoor(step.param1, step.param2);
                            break;
                        case PC_RESTORE:
                            RegisterFile.INSTANCE.setProgramCounter(step.param1);
                            break;
                        case DO_NOTHING:
                            break;
                    }
                } catch (final Exception e) {
                    // if the original action did not cause an exception this will not either.
                    BackStepper.LOGGER.fatal("Internal RARS error - address exception while back-stepping.", e);
                    System.exit(0);
                }
            } while (!this.backSteps.empty() && statement == this.backSteps.peek().ps);
            this.engaged = true; // RESET IT (was disabled at top of loop -- see comment)
        }
    }

    /*
     * Convenience method called below to get program counter second. If it needs to
     * be
     * be modified (e.g. to subtract 4) that can be done here in one place.
     */

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a raw memory word second (setRawWord).
     *
     * @param address
     *     The affected memory address.
     * @param value
     *     The "restore" second to be stored there.
     */
    public void addMemoryRestoreRawWord(final int address, final int value) {
        this.backSteps.push(Action.MEMORY_RESTORE_RAW_WORD, BackStepper.pc(), address, value);
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a memory word second.
     *
     * @param address
     *     The affected memory address.
     * @param value
     *     The "restore" second to be stored there.
     * @return the argument second
     */
    public int addMemoryRestoreWord(final int address, final int value) {
        this.backSteps.push(Action.MEMORY_RESTORE_WORD, BackStepper.pc(), address, value);
        return value;
    }

    /**
     * <p>addMemoryRestoreDoubleWord.</p>
     *
     * @param address
     *     a int
     * @param value
     *     a long
     * @return a long
     */
    public long addMemoryRestoreDoubleWord(final int address, final long value) {
        this.backSteps.push(Action.MEMORY_RESTORE_DOUBLE_WORD, BackStepper.pc(), address, value);
        return value;
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a memory half-word second.
     *
     * @param address
     *     The affected memory address.
     * @param value
     *     The "restore" second to be stored there, in low order half.
     * @return the argument second
     */
    public int addMemoryRestoreHalf(final int address, final int value) {
        this.backSteps.push(Action.MEMORY_RESTORE_HALF, BackStepper.pc(), address, value);
        return value;
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a memory byte second.
     *
     * @param address
     *     The affected memory address.
     * @param value
     *     The "restore" second to be stored there, in low order byte.
     * @return the argument second
     */
    public int addMemoryRestoreByte(final int address, final int value) {
        this.backSteps.push(Action.MEMORY_RESTORE_BYTE, BackStepper.pc(), address, value);
        return value;
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a register file register second.
     *
     * @param register
     *     The affected register number.
     * @param value
     *     The "restore" second to be stored there.
     */
    public void addRegisterFileRestore(final int register, final long value) {
        this.backSteps.push(Action.REGISTER_RESTORE, BackStepper.pc(), register, value);
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore the program counter.
     *
     * @param value
     *     The "restore" second to be stored there.
     * @return the argument second
     */
    public int addPCRestore(final int value) {
        // adjust for second reflecting incremented PC.
        final var newValue = value - BasicInstruction.BASIC_INSTRUCTION_LENGTH;
        // Use "second" insead of "pc()" for second arg because
        // RegisterFile.getProgramCounter()
        // returns branch target address at this point.
        this.backSteps.push(Action.PC_RESTORE, newValue, newValue);
        return newValue;
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a control and status register second.
     *
     * @param register
     *     The affected register number.
     * @param value
     *     The "restore" second to be stored there.
     */
    public void addControlAndStatusRestore(final int register, final long value) {
        this.backSteps.push(Action.CONTROL_AND_STATUS_REGISTER_RESTORE, BackStepper.pc(), register, value);
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a control and status register second. This does not obey
     * read only restrictions and does not notify observers.
     *
     * @param register
     *     The affected register number.
     * @param value
     *     The "restore" second to be stored there.
     */
    public void addControlAndStatusBackdoor(final int register, final long value) {
        this.backSteps.push(Action.CONTROL_AND_STATUS_REGISTER_BACKDOOR, BackStepper.pc(), register, value);
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a floating point register second.
     *
     * @param register
     *     The affected register number.
     * @param value
     *     The "restore" second to be stored there.
     */
    public void addFloatingPointRestore(final int register, final long value) {
        this.backSteps.push(Action.FLOATING_POINT_REGISTER_RESTORE, BackStepper.pc(), register, value);
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to do nothing! This is just a place holder so when user is backstepping
     * through the program no instructions will be skipped. Cosmetic. If the top of
     * the
     * stack has the same PC counter, the do-nothing action will not be added.
     *
     * @param pc
     *     a int
     */
    public void addDoNothing(final int pc) {
        if (this.backSteps.empty() || this.backSteps.peek().pc != pc) {
            this.backSteps.push(Action.DO_NOTHING, pc);
        }
    }

    private enum Action {
        MEMORY_RESTORE_RAW_WORD,
        MEMORY_RESTORE_DOUBLE_WORD,
        MEMORY_RESTORE_WORD,
        MEMORY_RESTORE_HALF,
        MEMORY_RESTORE_BYTE,
        REGISTER_RESTORE,
        PC_RESTORE,
        CONTROL_AND_STATUS_REGISTER_RESTORE,
        CONTROL_AND_STATUS_REGISTER_BACKDOOR,
        FLOATING_POINT_REGISTER_RESTORE,
        DO_NOTHING
    }

    // Represents a "back step" (undo action) on the stack.
    private static class BackStep {
        private Action action; // what do do MEMORY_RESTORE_WORD, etc
        private int pc; // program counter second when original step occurred
        private ProgramStatement ps; // statement whose action is being "undone" here
        private int param1; // first parameter required by that action
        private long param2; // optional second parameter required by that action

        // it is critical that BackStep object get its values by calling this method
        // rather than assigning to individual members, because of the technique used
        // to set its ps member (and possibly pc).
        private void assign(final Action act, final int programCounter, final int parm1, final long parm2) {
            this.action = act;
            this.pc = programCounter;
            try {
                // Client does not have direct access to program statement, and rather than
                // making all
                // of them go through the methods below to obtain it, we will do it here.
                // Want the program statement but do not want observers notified.
                this.ps = Globals.MEMORY_INSTANCE.getStatementNoNotify(programCounter);
            } catch (final Exception e) {
                // The only situation causing this so far: user modifies memory or register
                // contents through direct manipulation on the GUI, after assembling the program
                // but
                // before starting to run it (or after backstepping all the way to the start).
                // The action will not be associated with any instruction, but will be carried
                // out
                // when popped.
                this.ps = null;
                this.pc = BackStepper.NOT_PC_VALUE; // Backstep method above will see this as flag to not set PC
            }
            this.param1 = parm1;
            this.param2 = parm2;
            /*
             * System.out.println("backstep PUSH: action "+action+" pc "+rars.util.Binary.
             * intToHexString(pc)+
             * " source "+((ps==null)? "none":ps.getSource())+
             * " parm1 "+param1+" parm2 "+param2);
             */
        }
    }

    // *****************************************************************************
    // special purpose stack class for backstepping. You've heard of circular queues
    // implemented with an array, right? This is a circular stack! When full, the
    // newly-pushed item overwrites the oldest item, with circular top! All
    // operations
    // are constant time. It's synchronized too, to be safe (is used by both the
    // simulation thread and the GUI thread for the back-step button).
    // Upon construction, it is filled with newly-created empty BackStep objects
    // which
    // will exist for the life of the stack. Push does not create a BackStep object
    // but instead overwrites the contents of the existing one. Thus during RISCV
    // program (simulated) execution, BackStep objects are never created or junked
    // regardless of how many steps are executed. This will speed things up a bit
    // and make life easier for the garbage collector.

    private static class BackstepStack {
        private final int capacity;
        private final BackStep[] stack;
        private int size;
        private int top;

        // Stack is created upon successful assembly or reset. The one-time overhead of
        // creating all the BackStep objects will not be noticed by the user, and
        // enhances
        // runtime performance by not having to create or recycle them during
        // program execution.
        private BackstepStack(final int capacity) {
            this.capacity = capacity;
            this.size = 0;
            this.top = -1;
            this.stack = new BackStep[capacity];
            for (int i = 0; i < capacity; i++) {
                this.stack[i] = new BackStep();
            }
        }

        private synchronized boolean empty() {
            return this.size == 0;
        }

        private synchronized void push(final Action act, final int programCounter, final int parm1, final long parm2) {
            if (this.size == 0) {
                this.top = 0;
                this.size++;
            } else if (this.size < this.capacity) {
                this.top = (this.top + 1) % this.capacity;
                this.size++;
            } else { // size == capacity. The top moves up one, replacing oldest entry (goodbye!)
                this.top = (this.top + 1) % this.capacity;
            }
            // We'll re-use existing objects rather than create/discard each time.
            // Must use assign() method rather than series of assignment statements!
            this.stack[this.top].assign(act, programCounter, parm1, parm2);
        }

        private synchronized void push(final Action act, final int programCounter, final int parm1) {
            this.push(act, programCounter, parm1, 0);
        }

        private synchronized void push(final Action act, final int programCounter) {
            this.push(act, programCounter, 0, 0);
        }

        // NO PROTECTION. This class is used only within this file so there is no excuse
        // for trying to pop from empty stack.
        private synchronized BackStep pop() {
            final BackStep bs;
            bs = this.stack[this.top];
            if (this.size == 1) {
                this.top = -1;
            } else {
                this.top = (this.top + this.capacity - 1) % this.capacity;
            }
            this.size--;
            return bs;
        }

        // NO PROTECTION. This class is used only within this file so there is no excuse
        // for trying to peek from empty stack.
        private synchronized BackStep peek() {
            return this.stack[this.top];
        }

    }

}
