package rars.simulator

import rars.Globals
import rars.ProgramStatement
import rars.riscv.BasicInstruction
import rars.simulator.Backstepper.BackstepAction.*
import rars.util.unwrap

/**
 * Used to "step backward" through execution, undoing each instruction.
 *
 * @author Pete Sanderson
 * @version February 2006
 */
class Backstepper {
    private val backSteps: BackstepStack = BackstepStack(Globals.MAXIMUM_BACKSTEPS)

    /**
     * Determines whether the undo actions are being recorded by this object.
     */
    var isEnabled = true

    /**
     * Test whether there are steps that can be undone.
     *
     * @return true if there are no steps to be undone, false otherwise.
     */
    fun empty(): Boolean = backSteps.empty()

    /**
     * Carry out a "back step", which will undo the latest execution step.
     * Does nothing if backstepping not enabled or if there are no steps to undo.
     * Note that there may be more than one "step" in an instruction execution; for
     * instance the multiply, divide, and double-precision floating point operations
     * all store their result in register pairs which results in two store
     * operations.
     * Both must be undone transparently, so we need to detect that multiple steps
     * happen
     * together and carry out all of them here.
     * Use a do-while loop based on the backstep's program statement reference.
     */
    fun backStep() {
        if (isEnabled && !this.backSteps.empty()) {
            val statement = this.backSteps.peek().ps
            // GOTTA DO THIS SO METHOD CALL IN SWITCH WILL NOT RESULT IN NEW ACTION ON STACK!
            this.isEnabled = false
            do {
                val step = this.backSteps.pop()
                if (step.pc != NOT_PC_VALUE) {
                    Globals.REGISTER_FILE.setProgramCounter(step.pc)
                }
                val param1 = step.param1
                val param2 = step.param2
                when (step.action) {
                    MEMORY_RESTORE_RAW_WORD -> Globals.MEMORY_INSTANCE.setRawWord(
                        param1,
                        param2.toInt()
                    ).unwrap()

                    MEMORY_RESTORE_DOUBLE_WORD -> Globals.MEMORY_INSTANCE.setDoubleWord(
                        param1,
                        param2
                    ).unwrap()

                    MEMORY_RESTORE_WORD -> Globals.MEMORY_INSTANCE.setWord(
                        param1,
                        param2.toInt()
                    ).unwrap()

                    MEMORY_RESTORE_HALF -> Globals.MEMORY_INSTANCE.setHalf(
                        param1,
                        param2.toShort()
                    ).unwrap()

                    MEMORY_RESTORE_BYTE -> Globals.MEMORY_INSTANCE.setByte(
                        param1,
                        param2.toByte()
                    ).unwrap()

                    REGISTER_RESTORE -> Globals.REGISTER_FILE.updateRegisterByNumber(
                        param1,
                        param2
                    ).unwrap()

                    FLOATING_POINT_REGISTER_RESTORE -> Globals.FP_REGISTER_FILE.updateRegisterByNumber(
                        param1,
                        param2
                    ).unwrap()

                    CONTROL_AND_STATUS_REGISTER_RESTORE -> Globals.CS_REGISTER_FILE.updateRegisterByNumber(
                        param1,
                        param2
                    ).unwrap()

                    CONTROL_AND_STATUS_REGISTER_BACKDOOR -> Globals.CS_REGISTER_FILE.updateRegisterBackdoorByNumber(
                        param1,
                        param2
                    )

                    PC_RESTORE -> Globals.REGISTER_FILE.setProgramCounter(param1)
                    DO_NOTHING -> {}
                }
            } while (!this.backSteps.empty() && statement == this.backSteps.peek().ps)
            this.isEnabled = true // RESET IT (was disabled at top of loop -- see comment)
        }
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a raw memory word value (setRawWord).
     *
     * @param address
     * The affected memory address.
     * @param value
     * The "restore" value to be stored there.
     */
    fun addMemoryRestoreRawWord(address: Int, value: Int) {
        this.backSteps.push(MEMORY_RESTORE_RAW_WORD, pc(), address, value.toLong())
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a memory word value.
     *
     * @param address
     * The affected memory address.
     * @param value
     * The "restore" value to be stored there.
     * @return the argument value
     */
    fun addMemoryRestoreWord(address: Int, value: Int): Int {
        this.backSteps.push(MEMORY_RESTORE_WORD, pc(), address, value.toLong())
        return value
    }

    /**
     *
     * addMemoryRestoreDoubleWord.
     *
     * @param address
     * a int
     * @param value
     * a long
     * @return a long
     */
    fun addMemoryRestoreDoubleWord(address: Int, value: Long): Long {
        this.backSteps.push(MEMORY_RESTORE_DOUBLE_WORD, pc(), address, value)
        return value
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a memory half-word value.
     *
     * @param address
     * The affected memory address.
     * @param value
     * The "restore" value to be stored there, in low order half.
     * @return the argument value
     */
    fun addMemoryRestoreHalf(address: Int, value: Int): Int {
        this.backSteps.push(MEMORY_RESTORE_HALF, pc(), address, value.toLong())
        return value
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a memory byte value.
     *
     * @param address
     * The affected memory address.
     * @param value
     * The "restore" value to be stored there, in low order byte.
     * @return the argument value
     */
    fun addMemoryRestoreByte(address: Int, value: Int): Int {
        this.backSteps.push(MEMORY_RESTORE_BYTE, pc(), address, value.toLong())
        return value
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a register file register value.
     *
     * @param register
     * The affected register number.
     * @param value
     * The "restore" value to be stored there.
     */
    fun addRegisterFileRestore(register: Int, value: Long) {
        this.backSteps.push(REGISTER_RESTORE, pc(), register, value)
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore the program counter.
     *
     * @param value
     * The "restore" value to be stored there.
     * @return the argument value
     */
    fun addPCRestore(value: Int): Int {
        // adjust for value reflecting incremented PC.
        val newValue = value - BasicInstruction.BASIC_INSTRUCTION_LENGTH
        // Use "value" insead of "pc()" for value arg because
        // RegisterFile.getProgramCounter()
        // returns branch target address at this point.
        synchronized(this.backSteps) {
            this.backSteps.push(PC_RESTORE, newValue, newValue, 0)
        }
        return newValue
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a control and status register value.
     *
     * @param register
     * The affected register number.
     * @param value
     * The "restore" value to be stored there.
     */
    fun addControlAndStatusRestore(register: Int, value: Long) {
        this.backSteps.push(CONTROL_AND_STATUS_REGISTER_RESTORE, pc(), register, value)
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a control and status register value. This does not obey
     * read only restrictions and does not notify observers.
     *
     * @param register
     * The affected register number.
     * @param value
     * The "restore" value to be stored there.
     */
    fun addControlAndStatusBackdoor(register: Int, value: Long) {
        this.backSteps.push(CONTROL_AND_STATUS_REGISTER_BACKDOOR, pc(), register, value)
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to restore a floating point register value.
     *
     * @param register
     * The affected register number.
     * @param value
     * The "restore" value to be stored there.
     */
    fun addFloatingPointRestore(register: Int, value: Long) {
        this.backSteps.push(FLOATING_POINT_REGISTER_RESTORE, pc(), register, value)
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to do nothing! This is just a place holder so when user is backstepping
     * through the program no instructions will be skipped. Cosmetic. If the top of
     * the
     * stack has the same PC counter, the do-nothing action will not be added.
     *
     * @param pc
     * a int
     */
    fun addDoNothing(pc: Int) {
        if (this.backSteps.empty() || this.backSteps.peek().pc != pc) {
            synchronized(this.backSteps) {
                this.backSteps.push(DO_NOTHING, pc, 0, 0)
            }
        }
    }

    private enum class BackstepAction {
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

    /** Represents a "back step" (undo action) on the stack. */
    private class BackStep {
        /** what to do MEMORY_RESTORE_WORD, etc  */
        var action: BackstepAction = DO_NOTHING // some default value

        /** program counter value when original step occurred  */
        var pc: Int = 0

        /** statement whose action is being "undone" here  */
        var ps: ProgramStatement? = null

        /** first parameter required by that action  */
        var param1: Int = 0

        /** optional value parameter required by that action  */
        var param2: Long = 0

        /**
         * It is critical that BackStep object get its values by calling this method
         * rather than assigning to individual members, because of the technique used
         * to set its ps member (and possibly pc).
         */
        fun assign(
            act: BackstepAction,
            programCounter: Int,
            param1: Int,
            param2: Long
        ) {
            this.action = act
            val (statement, counter) = Globals.MEMORY_INSTANCE.silentMemoryView.getProgramStatement(programCounter)
                .fold(
                    {
                        // The only situation causing this so far: user modifies memory or register
                        // contents through direct manipulation on the GUI, after assembling the program
                        // but
                        // before starting to run it (or after backstepping all the way to the start).
                        // The action will not be associated with any instruction, but will be carried
                        // out
                        // when popped.
                        // Backstep method above will see this as flag to not set PC
                        Pair(null, NOT_PC_VALUE)
                    },
                    { statement ->
                        // Client does not have direct access to program statement, and rather than
                        // making all
                        // of them go through the methods below to obtain it, we will do it here.
                        // Want the program statement but do not want observers notified.
                        Pair(statement, programCounter)
                    }
                )
            this.ps = statement
            this.pc = counter
            this.param1 = param1
            this.param2 = param2
        }
    }

    /**
     * Special purpose stack class for backstepping. You've heard of circular queues
     * implemented with an array, right? This is a circular stack! When full, the
     * newly-pushed item overwrites the oldest item, with circular top! All
     * operations
     * are constant time. It's synchronized too, to be safe (is used by both the
     * simulation thread and the GUI thread for the back-step button).
     * Upon construction, it is filled with newly-created empty BackStep objects
     * which
     * will exist for the life of the stack. Push does not create a BackStep object
     * but instead overwrites the contents of the existing one. Thus during RISCV
     * program (simulated) execution, BackStep objects are never created or junked
     * regardless of how many steps are executed. This will speed things up a bit
     * and make life easier for the garbage collector.
     */
    private class BackstepStack(private val capacity: Int) {
        private var size = 0

        // Stack is created upon successful assembly or reset. The one-time overhead of
        // creating all the BackStep objects will not be noticed by the user, and
        // enhances
        // runtime performance by not having to create or recycle them during
        // program execution.
        private val stack = Array(capacity) { BackStep() }
        private var top = -1

        @Synchronized
        fun empty() = this.size == 0

        @Synchronized
        fun push(
            act: BackstepAction,
            programCounter: Int,
            param1: Int,
            param2: Long
        ) {
            if (this.size == 0) {
                this.top = 0
                this.size++
            } else if (this.size < this.capacity) {
                this.top = (this.top + 1) % this.capacity
                this.size++
            } else {
                // size == capacity. The top moves up one, replacing oldest entry (goodbye!)
                this.top = (this.top + 1) % this.capacity
            }
            // We'll re-use existing objects rather than create/discard each time.
            // Must use assign() method rather than series of assignment statements!
            this.stack[this.top].assign(act, programCounter, param1, param2)
        }

        /**
         * NO PROTECTION. This class is used only within this file so there is no excuse
         * for trying to pop from empty stack.
         */
        @Synchronized
        fun pop(): BackStep {
            val bs = this.stack[this.top]
            if (this.size == 1) {
                this.top = -1
            } else {
                this.top = (this.top + this.capacity - 1) % this.capacity
            }
            this.size--
            return bs
        }

        /**
         * NO PROTECTION. This class is used only within this file so there is no excuse
         * for trying to peek from empty stack.
         */
        @Synchronized
        fun peek() = this.stack[this.top]
    }

    companion object {

        /**
         * Flag to mark BackStep object as prepresenting specific situation: user manipulates
         * memory/register value via GUI after assembling program but before running it.
         */
        private const val NOT_PC_VALUE = -1

        // PC incremented prior to instruction simulation, so need to adjust for that.
        private fun pc(): Int = Globals.REGISTER_FILE.programCounter - BasicInstruction.BASIC_INSTRUCTION_LENGTH
    }
}
