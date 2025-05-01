package rars.simulator

import rars.Globals
import rars.ProgramStatement
import rars.riscv.BasicInstruction
import rars.util.CircularStack
import rars.util.unwrap

/**
 * Used to "step backward" through execution, undoing each instruction.
 *
 * @author Pete Sanderson
 * @version February 2006
 */
class Backstepper {
    private val backSteps = CircularStack<BackStep>(Globals.MAXIMUM_BACKSTEPS)

    /**
     * Determines whether the undo actions are being recorded by this object.
     */
    var isEnabled = true

    /**
     * Test whether there are steps that can be undone.
     *
     * @return true if there are no steps to be undone, false otherwise.
     */
    fun isEmpty(): Boolean = backSteps.isEmpty()

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
        if (isEnabled && !backSteps.isEmpty()) {
            val statement = backSteps.peek()!!.statement
            // GOTTA DO THIS SO METHOD CALL IN SWITCH WILL NOT RESULT IN NEW ACTION ON STACK!
            isEnabled = false
            do {
                val step = backSteps.pop()!!
                if (step.programCounter != NOT_PC_VALUE) {
                    Globals.REGISTER_FILE.programCounter = step.programCounter
                }
                val param1 = step.param1
                val param2 = step.param2
                when (step.action) {
                    BackStepAction.MEMORY_RESTORE_RAW_WORD -> Globals.MEMORY_INSTANCE.setRawWord(
                        param1,
                        param2.toInt()
                    ).unwrap()

                    BackStepAction.MEMORY_RESTORE_DOUBLE_WORD -> Globals.MEMORY_INSTANCE.setDoubleWord(
                        param1,
                        param2
                    ).unwrap()

                    BackStepAction.MEMORY_RESTORE_WORD -> Globals.MEMORY_INSTANCE.setWord(
                        param1,
                        param2.toInt()
                    ).unwrap()

                    BackStepAction.MEMORY_RESTORE_HALF -> Globals.MEMORY_INSTANCE.setHalf(
                        param1,
                        param2.toShort()
                    ).unwrap()

                    BackStepAction.MEMORY_RESTORE_BYTE -> Globals.MEMORY_INSTANCE.setByte(
                        param1,
                        param2.toByte()
                    ).unwrap()

                    BackStepAction.REGISTER_RESTORE -> Globals.REGISTER_FILE.updateRegisterByNumber(
                        param1,
                        param2
                    ).unwrap()

                    BackStepAction.FLOATING_POINT_REGISTER_RESTORE -> Globals.FP_REGISTER_FILE.updateRegisterByNumber(
                        param1,
                        param2
                    ).unwrap()

                    BackStepAction.CONTROL_AND_STATUS_REGISTER_RESTORE -> Globals.CS_REGISTER_FILE.updateRegisterByNumber(
                        param1,
                        param2
                    ).unwrap()

                    BackStepAction.CONTROL_AND_STATUS_REGISTER_BACKDOOR -> Globals.CS_REGISTER_FILE.updateRegisterBackdoorByNumber(
                        param1,
                        param2
                    )

                    BackStepAction.PC_RESTORE -> Globals.REGISTER_FILE.programCounter =
                        param1
                    BackStepAction.DO_NOTHING -> {}
                }
            } while (!backSteps.isEmpty() && statement == backSteps.peek()!!.statement)
            // RESET IT (was disabled at top of loop -- see comment)
            isEnabled = true
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
        this.backSteps.push(
            BackStep(
                BackStepAction.MEMORY_RESTORE_RAW_WORD,
                pc(),
                address,
                value.toLong()
            )
        )
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
        this.backSteps.push(
            BackStep(
                BackStepAction.MEMORY_RESTORE_WORD,
                pc(),
                address,
                value.toLong()
            )
        )
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
        this.backSteps.push(
            BackStep(
                BackStepAction.MEMORY_RESTORE_DOUBLE_WORD,
                pc(),
                address,
                value
            )
        )
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
        this.backSteps.push(
            BackStep(
                BackStepAction.MEMORY_RESTORE_HALF,
                pc(),
                address,
                value.toLong()
            )
        )
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
        this.backSteps.push(
            BackStep(
                BackStepAction.MEMORY_RESTORE_BYTE,
                pc(),
                address,
                value.toLong()
            )
        )
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
        this.backSteps.push(
            BackStep(
                BackStepAction.REGISTER_RESTORE,
                pc(),
                register,
                value
            )
        )
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
            this.backSteps.push(
                BackStep(
                    BackStepAction.PC_RESTORE,
                    newValue,
                    newValue,
                    0
                )
            )
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
        this.backSteps.push(
            BackStep(
                BackStepAction.CONTROL_AND_STATUS_REGISTER_RESTORE,
                pc(),
                register,
                value
            )
        )
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
        this.backSteps.push(
            BackStep(
                BackStepAction.CONTROL_AND_STATUS_REGISTER_BACKDOOR,
                pc(),
                register,
                value
            )
        )
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
        this.backSteps.push(
            BackStep(
                BackStepAction.FLOATING_POINT_REGISTER_RESTORE,
                pc(),
                register,
                value
            )
        )
    }

    /**
     * Add a new "back step" (the undo action) to the stack. The action here
     * is to do nothing! This is just a placeholder so when user is backstepping
     * through the program no instructions will be skipped. Cosmetic. If the top of
     * the
     * stack has the same PC counter, the do-nothing action will not be added.
     *
     * @param pc
     * a int
     */
    fun addDoNothing(pc: Int) {
        if (this.backSteps.isEmpty() || this.backSteps.peek()!!.programCounter != pc) {
            synchronized(this.backSteps) {
                this.backSteps.push(
                    BackStep(
                        BackStepAction.DO_NOTHING,
                        pc,
                        0,
                        0
                    )
                )
            }
        }
    }

    private enum class BackStepAction {
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

    private class BackStep(
        val action: BackStepAction,
        programCounter: Int,
        val param1: Int,
        val param2: Long
    ) {
        val programCounter: Int
        val statement: ProgramStatement?

        init {
            val (statement, counter) = Globals.MEMORY_INSTANCE
                .silentMemoryView
                .getProgramStatement(programCounter)
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
            this.statement = statement
            this.programCounter = counter
        }

        override fun toString(): String =
            """BackStep[statement = $statement, action = $action, param1 = $param1, param2 = $param2, pc = $programCounter]"""
    }

    companion object {

        /**
         * Flag to mark BackStep object as prepresenting specific situation: user manipulates
         * memory/register value via GUI after assembling program but before running it.
         */
        private const val NOT_PC_VALUE = -1

        // PC incremented prior to instruction simulation, so need to adjust for that.
        private fun pc(): Int =
            Globals.REGISTER_FILE.programCounter - BasicInstruction.BASIC_INSTRUCTION_LENGTH
    }

    override fun toString(): String = buildString {
        appendLine("Backstepper@${hashCode().toString(radix = 16)}[")
        for (backstep in backSteps) {
            appendLine("    $backstep")
        }
        append("]")
    }
}
