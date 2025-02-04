/*
Copyright (c) 2009,  Ingo Kofler, ITEC, Klagenfurt University, Austria

Developed by Ingo Kofler (ingo.kofler@itec.uni-klu.ac.at)
Based on the Instruction Counter tool by Felipe Lessa (felipe.lessa@gmail.com)

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
package rars.tools

import rars.Globals
import rars.exceptions.AddressErrorException
import rars.notices.AccessNotice
import rars.notices.MemoryAccessNotice
import rars.riscv.Instruction
import rars.riscv.instructions.*
import rars.venus.VenusUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.*
import javax.swing.*

/**
 * A RARS tool for obtaining instruction statistics by instruction category.
 *
 *
 * The code of this tools is initially based on the Instruction counter tool by
 * Felipe Lassa.
 *
 * @author Ingo Kofler &lt;ingo.kofler@itec.uni-klu.ac.at&gt;
 */
class InstructionStatistics(mainUI: VenusUI) : AbstractTool("$NAME, Version 1.0 (Ingo Kofler)", "", mainUI) {
    /** array of counter variables - one for each instruction category */
    private val counters = IntArray(InstructionCategory.count)

    /** names of the instruction categories as array */
    private val categoryLabels = arrayOf("ALU", "Jump", "Branch", "Memory", "Other")

    /**
     * The last address we saw. We ignore it because the only way for a
     * program to execute twice the same instruction is to enter an infinite
     * loop, which is not insteresting in the POV of counting instructions.
     */
    private var lastAddress: Int = -1

    /** text field for visualizing the total number of instructions processed */
    private lateinit var tfTotalCounter: JTextField

    /** array of text field - one for each instruction category */
    private lateinit var tfCounters: List<JTextField>

    /** array of progress pars - one for each instruction category */
    private lateinit var pbCounters: List<JProgressBar>

    // From Felipe Lessa's instruction counter. Prevent double-counting of
    // instructions
    // which happens because 2 read events are generated.
    /** counter for the total number of instructions processed */
    private var totalCounter = 0

    override fun getName(): String = NAME

    /**
     * creates the display area for the tool as required by the API
     *
     * @return a panel that holds the GUI of the tool
     */
    override fun buildMainDisplayArea(): JComponent {
        // Create GUI elements for the tool

        val panel = JPanel(GridBagLayout())

        this.tfTotalCounter = JTextField("0", 10)
        this.tfTotalCounter.isEditable = false


//        this.pbCounters = arrayOfNulls<JProgressBar>(InstructionCategory.count)

        this.tfCounters = buildList {
            InstructionCategory.entries.forEach {
                add(JTextField("0", 10).apply {
                    isEditable = false
                })
            }
        }
        this.pbCounters = buildList {
            InstructionCategory.entries.forEach {
                add(JProgressBar(JProgressBar.HORIZONTAL).apply {
                    setStringPainted(true)
                })
            }
        }

        val c = GridBagConstraints()
        c.anchor = GridBagConstraints.LINE_START
        c.gridwidth = 1
        c.gridheight = c.gridwidth

        // create the label and text field for the total instruction counter
        c.apply {
            gridx = 1
            gridy = 1
            insets = Insets(0, 0, 17, 0)
        }
        panel.add(JLabel("Total: "), c)
        c.gridx = 3
        panel.add(this.tfTotalCounter, c)

        c.insets = Insets(3, 3, 3, 3)

        // create label, text field and progress bar for each category
        for (i in 0..<InstructionCategory.count) {
            c.gridy++
            c.gridx = 2
            panel.add(JLabel(this.categoryLabels[i] + ":   "), c)
            c.gridx = 3
            panel.add(this.tfCounters[i], c)
            c.gridx = 4
            panel.add(this.pbCounters[i], c)
        }

        return panel
    }

    /**
     * registers the tool as observer for the text segment of the program
     */
    override fun addAsObserver() {
        val memoryConfiguration = Globals.MEMORY_INSTANCE.memoryConfiguration
        this.addAsObserver(memoryConfiguration.textBaseAddress, memoryConfiguration.textLimitAddress)
    }

    /**
     * {@inheritDoc}
     *
     *
     * method that is called each time the simulator accesses the text segment.
     * Before an instruction is executed by the simulator, the instruction is
     * fetched from the program memory.
     * This memory access is observed and the corresponding instruction is decoded
     * and categorized by the tool.
     * According to the category the counter values are increased and the display
     * gets updated.
     */
    override fun processRISCVUpdate(notice: AccessNotice) {
        if (!notice.isAccessFromRISCV) {
            return
        }

        // check for a read access in the text segment
        if (notice.accessType == AccessNotice.AccessType.READ && notice is MemoryAccessNotice) {
            // now it is safe to make a cast of the notice

            // The next three statments are from Felipe Lessa's instruction counter.
            // Prevents double-counting.

            val a = notice.address
            if (a == this.lastAddress) {
                return
            }
            this.lastAddress = a

            try {
                // access the statement in the text segment without notifying other tools etc.

                val stmt = Globals.MEMORY_INSTANCE.getStatementNoNotify(notice.address)

                // necessary to handle possible null pointers at the end of the program
                // (e.g., if the simulator tries to execute the next instruction after the last
                // instruction in the text segment)
                if (stmt != null) {
                    val category = stmt.instruction!!.getInstructionCategory()

                    this.totalCounter++
                    this.counters[category.ordinal]++
                    this.updateDisplay()
                }
            } catch (_: AddressErrorException) {
                // silently ignore these exceptions
            }
        }
    }

    /**
     * performs initialization tasks of the counters before the GUI is created.
     */
    override fun initializePreGUI() {
        this.totalCounter = 0
        this.lastAddress = -1 // from Felipe Lessa's instruction counter tool
        Arrays.fill(this.counters, 0)
    }

    /**
     * resets the counter values of the tool and updates the display.
     */
    override fun reset() {
        this.totalCounter = 0
        this.lastAddress = -1 // from Felipe Lessa's instruction counter tool
        Arrays.fill(this.counters, 0)
        this.updateDisplay()
    }

    /**
     * updates the text fields and progress bars according to the current counter
     * values.
     */
    override fun updateDisplay() {
        this.tfTotalCounter.text = this.totalCounter.toString()

        for (i in 0..<InstructionCategory.count) {
            this.tfCounters[i].text = this.counters[i].toString()
            this.pbCounters[i].maximum = this.totalCounter
            this.pbCounters[i].setValue(this.counters[i])
        }
    }

    enum class InstructionCategory {
        ALU, JUMP, BRANCH, MEM, OTHER;

        companion object {
            val count = entries.size
        }
    }

    companion object {
        /** name of the tool */
        private const val NAME = "Instruction Statistics"

        /**
         * decodes the instruction and determines the category of the instruction.
         *
         * The instruction is decoded by checking the java instance of the instruction.
         * Only the most relevant instructions are decoded and categorized.
         *
         * @param this@getInstructionCategory
         * the instruction to decode
         * @return the category of the instruction
         * @author Giancarlo Pernudi Segura
         * @author Chr1sps
         * @see InstructionCategory
         */
        private fun Instruction.getInstructionCategory() = when (this) {
            is Arithmetic, is ArithmeticW, // add, addw, sub, subw, and, or, xor, slt, sltu, m extension
            is ImmediateInstruction, // addi, addiw, andi, ori, xori, slti, sltiu
            BasicInstructions.LUI,
            BasicInstructions.AUIPC, // addi, addiw, andi, ori, xori, slti, sltiu, lui, auipc
            BasicInstructions.SLLI32, BasicInstructions.SLLIW, // slli, slliw
            BasicInstructions.SRLI32, BasicInstructions.SRLIW, // srli, srliw
            BasicInstructions.SRAI32, BasicInstructions.SRAIW -> InstructionCategory.ALU // srai, sraiw
            BasicInstructions.JAL, BasicInstructions.JALR -> InstructionCategory.JUMP // jal, jalr
            is Branch -> InstructionCategory.BRANCH // beq, bge, bgeu, blt, bltu, bne
            is Load, // lb, lh, lwl, lw, lbu, lhu, lwr
            is Store -> InstructionCategory.MEM // sb, sh, swl, sw, swr
            else -> InstructionCategory.OTHER
        }
    }
}
