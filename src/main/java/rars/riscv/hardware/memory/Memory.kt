package rars.riscv.hardware.memory

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.annotations.CheckReturnValue
import org.jetbrains.annotations.Contract
import rars.Globals
import rars.ProgramStatement
import rars.assembler.DataTypes
import rars.events.EventReason
import rars.events.MemoryError
import rars.notices.AccessType
import rars.notices.MemoryAccessNotice
import rars.riscv.BasicInstruction
import rars.settings.BoolSetting
import rars.settings.OtherSettings
import rars.util.Listener
import rars.util.toLongReinterpreted
import kotlin.math.max
import kotlin.math.min

/**
 * Represents memory. Different segments are represented by different data
 * structs.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
class Memory(
    private var _memoryConfiguration: AbstractMemoryConfiguration<Int>,
) : SubscribableMemory<Int> {
    override val memoryConfiguration by ::_memoryConfiguration

    /**
     * Memory will maintain a collection of observables.  Each one is associated
     * with a specific memory address or address range, and each will have at least
     * one observer registered with it.  When memory access is made, make sure only
     * observables associated with that address send notices to their observers.
     * This assures that observers are not bombarded with notices from memory
     * addresses they do not care about.
     * Would like a tree-like implementation, but that is complicated by this fact:
     * key for insertion into the tree would be based on Comparable using both low
     * and high end of address range, but retrieval from the tree has to be based
     * on target address being ANYWHERE IN THE RANGE (not an exact key match).
     */
    private val observables = mutableListOf<MemoryObservable>()

    override val silentMemoryView = object : ReadableMemory<Int> {
        override val memoryConfiguration get() = this@Memory.memoryConfiguration

        override fun getByte(address: Int) = get(address, 1, false).map {
            (it and 0xFF).toByte()
        }

        override fun getHalf(address: Int) = if (address % 2 != 0) {
            MemoryError(
                "Load address not aligned on halfword boundary ",
                EventReason.LOAD_ADDRESS_MISALIGNED,
                address
            ).left()
        } else get(address, 2, false).map {
            (it and 0xFFFF).toShort()
        }

        override fun getWord(address: Int) = checkLoadWordAligned(address).flatMap {
            get(address, DataTypes.WORD_SIZE, false)
        }

        override fun getDoubleWord(address: Int) = either {
            checkLoadWordAligned(address).bind()
            val oldHighOrder = get(address + 4, 4, false).bind()
            val oldLowOrder = get(address, 4, false).bind()
            (oldHighOrder.toLong() shl 32) or (oldLowOrder.toLong() and 0xFFFFFFFFL)
        }

        override fun getProgramStatement(address: Int) = this@Memory.getStatementNoNotify(address)

    }

    private var actualDataSegmentLimitAddress: Int = min(
        this.memoryConfiguration.dataSegmentLimitAddress.toDouble(),
        this.memoryConfiguration.dataSegmentBaseAddress.plus(
            BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * DataTypes.WORD_SIZE
        ).toDouble()
    ).toInt()
    private var actualTextLimitAddress: Int = min(
        this.memoryConfiguration.textSegmentLimitAddress.toDouble(),
        this.memoryConfiguration.textSegmentBaseAddress.plus(
            TEXT_BLOCK_LENGTH_WORDS * TEXT_BLOCK_TABLE_LENGTH * DataTypes.WORD_SIZE
        ).toDouble()
    ).toInt()
    private var actualStackLimitAddress: Int = max(
        this.memoryConfiguration.heapBaseAddress.toDouble(),
        this.memoryConfiguration.stackBaseAddress.minus(
            BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * DataTypes.WORD_SIZE
        ).toDouble()
    ).toInt()
    private var actualMemoryMapLimitAddress: Int = min(
        this.memoryConfiguration.memoryMapLimitAddress.toDouble(),
        this.memoryConfiguration.memoryMapBaseAddress.plus(
            BLOCK_LENGTH_WORDS * MMIO_TABLE_LENGTH * DataTypes.WORD_SIZE
        ).toDouble()
    ).toInt()

    /** Tracks the current heap address during {de,}allocations.  */
    private var currentHeapAddress: Int = memoryConfiguration.heapBaseAddress

    /**
     * The data segment is allocated in blocks of 1024 ints (4096 bytes).  Each block is
     * referenced by a "block table" entry, and the table has 1024 entries.  The capacity
     * is thus 1024 entries * 4096 bytes = 4 MB.  Should be enough to cover most
     * programs!!  Beyond that it would go to an "indirect" block (similar to Unix i-nodes),
     * which is not implemented.
     * Although this scheme is an array of arrays, it is relatively space-efficient since
     * only the table is created initially. A 4096-byte block is not allocated until a value
     * is written to an address within it.  Thus most small programs will use only 8K bytes
     * of space (the table plus one block).  The index into both arrays is easily computed
     * from the address; access time is constant.
     * SPIM stores statically allocated data (following first .data directive) starting
     * at location 0x10010000.  This is the first Data Segment word beyond the reach of $gp
     * used in conjunction with signed 16 bit immediate offset.  $gp has value 0x10008000
     * and with the signed 16 bit offset can reach from 0x10008000 - 0xFFFF = 0x10000000
     * (Data Segment base) to 0x10008000 + 0x7FFF = 0x1000FFFF (the byte preceding 0x10010000).
     * Using my scheme, 0x10010000 falls at the beginning of the 17'th block -- table entry 16.
     * SPIM uses a heap base address of 0x10040000 which is not part of the MIPS specification.
     * (I don't have a reference for that offhand...)  Using my scheme, 0x10040000 falls at
     * the start of the 65'th block -- table entry 64.  That leaves (1024-64) * 4096 = 3,932,160
     * bytes of space available without going indirect.
     */
    private var dataBlockTable: Array<IntArray?> = arrayOfNulls(BLOCK_TABLE_LENGTH)

    /**
     * The stack is modeled similarly to the data segment. It cannot share the same
     * data structure because the stack base address is very large. To store it in
     * the
     * same data structure would require implementation of indirect blocks, which
     * has not
     * been realized. So the stack gets its own table of blocks using the same
     * dimensions
     * and allocation scheme used for data segment.
     * The other major difference is the stack grows DOWNWARD from its base address,
     * not
     * upward. I.e., the stack base is the largest stack address. This turns the
     * whole
     * scheme for translating memory address to block-offset on its head! The
     * simplest
     * solution is to calculate relative address (offset from base) by subtracting
     * the
     * desired address from the stack base address (rather than subtracting base
     * address
     * from desired address). Thus as the address gets smaller the offset gets
     * larger.
     * Everything else works the same, so it shares some private helper methods with
     * data segment algorithms.
     */
    private var stackBlockTable: Array<IntArray?> = arrayOfNulls(BLOCK_TABLE_LENGTH)

    /**
     * Memory mapped I/O is simulated with a separate table using the same structure
     * and
     * logic as data segment. Memory is allocated in 4K byte blocks. But since MMIO
     * address range is limited to 0xffff0000 to 0xfffffffc, there are only 64K
     * bytes
     * total. Thus there will be a maximum of 16 blocks, and I suspect never more
     * than
     * one since only the first few addresses are typically used. The only exception
     * may be a rogue program generating such addresses in a loop. Note that the
     * MMIO addresses are interpreted by Java as negative numbers since it does not
     * have unsigned types. As long as the absolute address is correctly translated
     * into a table offset, this is of no concern.
     */
    private var memoryMapBlockTable: Array<IntArray?> = arrayOfNulls(MMIO_TABLE_LENGTH)

    /**
     * I use a similar scheme for storing instructions. MIPS text segment ranges
     * from
     * 0x00400000 all the way to data segment (0x10000000) a range of about 250 MB!
     * So
     * I'll provide table of blocks with similar capacity. This differs from data
     * segment
     * somewhat in that the block entries do not contain int's, but instead contain
     * references to ProgramStatement objects.
     */
    private var textBlockTable: Array<Array<ProgramStatement?>?> = arrayOfNulls(TEXT_BLOCK_TABLE_LENGTH)

    /**
     * Returns the next available word-aligned heap address. There is no recycling
     * and no heap management! There is however nearly 4MB of heap space available in Rars.
     *
     * @param numBytes
     * Number of bytes requested. Should be multiple of 4, otherwise
     * next higher multiple of 4 allocated.
     * @return address of allocated heap storage.
     * @throws IllegalArgumentException
     * if number of requested bytes is negative or
     * exceeds available heap storage
     */
    override fun allocateBytes(numBytes: Int): Either<String, Int> = either {
        val result = currentHeapAddress
        ensure(numBytes != 0) { "request ($numBytes) is negative heap amount" }
        var newHeapAddress = currentHeapAddress + numBytes
        if (newHeapAddress % 4 != 0) {
            newHeapAddress = newHeapAddress + (4 - newHeapAddress % 4) // next higher multiple of 4
        }
        ensure(newHeapAddress < actualDataSegmentLimitAddress) { "request ($numBytes) exceeds available heap storage" }
        currentHeapAddress = newHeapAddress
        result
    }

    // region Address checking utils
    fun isAddressInStackRange(address: Int): Boolean =
        address > this.actualStackLimitAddress && address <= this.memoryConfiguration.stackBaseAddress

    /**
     * Handy little utility to find out if given address is in the text
     * segment (starts at Memory.currentConfiguration.textSegmentBaseAddress).
     * Note that RARS does not implement the entire text segment space,
     * but it does implement enough for hundreds of thousands of lines
     * of code.
     *
     * @param address
     * integer memory address
     * @return true if that address is within RARS-defined text segment,
     * false otherwise.
     */
    fun isAddressInTextSegment(address: Int): Boolean =
        address in memoryConfiguration.textSegmentBaseAddress..<actualTextLimitAddress

    /**
     * Handy little utility to find out if given address is in RARS data
     * segment (starts at Memory.currentConfiguration.dataSegmentBaseAddress).
     *
     * @param address
     * integer memory address
     * @return true if that address is within RARS-defined data segment,
     * false otherwise.
     */
    fun isAddressInDataSegment(address: Int): Boolean =
        address in memoryConfiguration.dataSegmentBaseAddress..<actualDataSegmentLimitAddress

    /**
     * Handy little utility to find out if given address is in the Memory Map area
     * starts at Memory.currentConfiguration.memoryMapBaseAddress, range 0xffff0000 to 0xffffffff.
     *
     * @param address
     * integer memory address
     * @return true if that address is within RARS-defined memory map (MMIO) area,
     * false otherwise.
     */
    fun isAddressInMemorySegment(address: Int): Boolean =
        address in memoryConfiguration.memoryMapBaseAddress..<memoryConfiguration.kernelHighAddress

    // endregion Address checking utils

    fun copyFrom(other: Memory) {
        if (this.textBlockTable.size != other.textBlockTable.size || this.dataBlockTable.size != other.dataBlockTable.size || this.stackBlockTable.size != other.stackBlockTable.size || this.memoryMapBlockTable.size != other.memoryMapBlockTable.size) {
            // The memory configurations don't match up
            return
        }

        for (i in this.textBlockTable.indices) {
            if (other.textBlockTable[i] != null) {
                this.textBlockTable[i] = other.textBlockTable[i]!!.clone() // TODO: potentially make ProgramStatement 
                // clonable
            } else {
                this.textBlockTable[i] = null
            }
        }
        for (i in this.dataBlockTable.indices) {
            if (other.dataBlockTable[i] != null) {
                this.dataBlockTable[i] = other.dataBlockTable[i]!!.clone()
            } else {
                this.dataBlockTable[i] = null
            }
        }
        for (i in this.stackBlockTable.indices) {
            if (other.stackBlockTable[i] != null) {
                this.stackBlockTable[i] = other.stackBlockTable[i]!!.clone()
            } else {
                this.stackBlockTable[i] = null
            }
        }
        for (i in this.memoryMapBlockTable.indices) {
            if (other.memoryMapBlockTable[i] != null) {
                this.memoryMapBlockTable[i] = other.memoryMapBlockTable[i]!!.clone()
            } else {
                this.memoryMapBlockTable[i] = null
            }
        }
    }

    fun reset() {
        this.currentHeapAddress = this.memoryConfiguration.heapBaseAddress
        this.textBlockTable = arrayOfNulls<Array<ProgramStatement?>>(TEXT_BLOCK_TABLE_LENGTH)
        this.dataBlockTable = arrayOfNulls<IntArray>(BLOCK_TABLE_LENGTH) // array of null int[] references
        this.stackBlockTable = arrayOfNulls<IntArray>(BLOCK_TABLE_LENGTH)
        this.memoryMapBlockTable = arrayOfNulls<IntArray>(MMIO_TABLE_LENGTH)
    }

    /**
     * Starting at the given address, write the given value over the given number of
     * bytes.
     * This one does not check for word boundaries, and copies one byte at a time.
     * If length == 1, takes value from low order byte. If 2, takes from low order
     * half-word.
     *
     *
     * Allocates blocks if necessary.
     *
     * @param address
     * Starting address of Memory address to be set.
     * @param value
     * Value to be stored starting at that address.
     * @param length
     * Number of bytes to be written.
     * @return old value that was replaced by the set operation
     * @throws MemoryError
     * if any.
     */
    @CheckReturnValue
    fun set(address: Int, value: Int, length: Int): Either<MemoryError, Int> = either {
        var value = value
        if (Globals.debug) {
            LOGGER.debug("memory[{}] set to {}({} bytes)", address, value, length)
        }
        var oldValue = 0
        when {
            isAddressInDataSegment(address) -> {
                // in data segment. Will write one byte at a time, w/o regard to boundaries.
                val relativeByteAddress = (address
                    - memoryConfiguration.dataSegmentBaseAddress) // relative to data segment start, in bytes
                oldValue = storeBytesInTable(dataBlockTable, relativeByteAddress, length, value)
            }
            isAddressInStackRange(address) -> {
                // in stack. Handle similarly to data segment write, except relative byte
                // address calculated "backward" because stack addresses grow down from base.
                val relativeByteAddress = memoryConfiguration.stackBaseAddress - address
                oldValue = storeBytesInTable(stackBlockTable, relativeByteAddress, length, value)
            }
            isAddressInTextSegment(address) -> {
                ensure(Globals.BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) {
                    MemoryError(
                        "Cannot write directly to text segment!",
                        EventReason.STORE_ACCESS_FAULT,
                        address
                    )
                }
                ensure(address % 4 + length <= 4) {
                    // TODO: add checks for halfword load not aligned to halfword boundary
                    MemoryError(
                        "Load address crosses word boundary",
                        EventReason.LOAD_ADDRESS_MISALIGNED,
                        address
                    )
                }
                val oldStatement = getStatementImpl((address / 4) * 4, false).bind()
                if (oldStatement != null) {
                    oldValue = oldStatement.binaryStatement
                }
                value = value shl (address % 4) * 8
                var mask = if (length == 4) -1 else ((1 shl (8 * length)) - 1)
                mask = mask shl (address % 4) * 8
                value = (value and mask) or (oldValue and mask.inv())
                oldValue = (oldValue and mask) shr (address % 4)
                setProgramStatement(
                    (address / 4) * 4,
                    ProgramStatement(value, (address / 4) * 4)
                ).bind()
            }
            else -> {
                ensure(address in memoryConfiguration.memoryMapBaseAddress..<actualMemoryMapLimitAddress) {
                    MemoryError(
                        "address out of range ",
                        EventReason.STORE_ACCESS_FAULT,
                        address
                    )
                }
                val relativeByteAddress = address - memoryConfiguration.memoryMapBaseAddress
                oldValue = storeBytesInTable(memoryMapBlockTable, relativeByteAddress, length, value)
            }
        }
        notifyAnyObservers(AccessType.WRITE, address, length, value)
        oldValue
    }

    /**
     * Starting at the given word address, write the given value over 4 bytes (a
     * word).
     * It must be written as is, without adjusting for byte order (little vs big
     * endian).
     * Address must be word-aligned.
     *
     * @param address
     * Starting address of Memory address to be set.
     * @param value
     * Value to be stored starting at that address.
     * @return old value that was replaced by the set operation.
     * @throws MemoryError
     * If address is not on word boundary.
     */
    @CheckReturnValue
    fun setRawWord(address: Int, value: Int): Either<MemoryError, Int> = either {
        checkStoreWordAligned(address).bind()
        var oldValue = 0
        val relative: Int
        @Suppress("Ensure")
        if (isAddressInDataSegment(address)) {
            // in data segment
            relative = ((address - memoryConfiguration.dataSegmentBaseAddress)
                shr 2) // convert byte address to words
            oldValue = storeWordInTable(dataBlockTable, relative, value)
        } else if (isAddressInStackRange(address)) {
            // in stack. Handle similarly to data segment write, except relative
            // address calculated "backward" because stack addresses grow down from base.
            relative = (memoryConfiguration.stackBaseAddress - address) shr 2 // convert byte address to words
            oldValue = storeWordInTable(stackBlockTable, relative, value)
        } else if (isAddressInTextSegment(address)) {
            ensure(Globals.BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) {
                MemoryError(
                    "Cannot write directly to text segment!",
                    EventReason.STORE_ACCESS_FAULT,
                    address
                )
            }
            val oldStatement = getStatementNoNotify(address).bind()
            if (oldStatement != null) {
                oldValue = oldStatement.binaryStatement
            }
            setProgramStatement(address, ProgramStatement(value, address)).bind()
        } else if (address >= memoryConfiguration.memoryMapBaseAddress
            && address < actualMemoryMapLimitAddress
        ) {
            // memory mapped I/O.
            relative = (address - memoryConfiguration.memoryMapBaseAddress) shr 2 // convert byte address to word
            oldValue = storeWordInTable(memoryMapBlockTable, relative, value)
        } else raise(MemoryError("store address out of range ", EventReason.STORE_ACCESS_FAULT, address))
        notifyAnyObservers(AccessType.WRITE, address, DataTypes.WORD_SIZE, value)
        if (OtherSettings.isBacksteppingEnabled) {
            Globals.PROGRAM!!.backStepper!!.addMemoryRestoreRawWord(address, oldValue)
        }
        oldValue
    }

    override fun setWord(address: Int, value: Int): Either<MemoryError, Unit> = either {
        checkStoreWordAligned(address).bind()
        if (OtherSettings.isBacksteppingEnabled)
            Globals.PROGRAM!!.backStepper!!.addMemoryRestoreWord(
                address,
                set(address, value, DataTypes.WORD_SIZE).bind()
            ) else set(address, value, DataTypes.WORD_SIZE).bind()
    }

    override fun setHalf(address: Int, value: Short): Either<MemoryError, Unit> = either {
        ensure(address % 2 == 0) {
            MemoryError(
                "store address not aligned on halfword boundary ",
                EventReason.STORE_ADDRESS_MISALIGNED,
                address
            )
        }
        if (OtherSettings.isBacksteppingEnabled) Globals.PROGRAM!!.backStepper!!.addMemoryRestoreHalf(
            address,
            set(address, value.toInt(), 2).bind()
        ) else set(address, value.toInt(), 2).bind()
    }

    override fun setByte(address: Int, value: Byte): Either<MemoryError, Unit> = either {
        if (OtherSettings.isBacksteppingEnabled)
            Globals.PROGRAM!!.backStepper!!.addMemoryRestoreByte(
                address,
                set(address, value.toInt(), 1).bind()
            )
        else set(address, value.toInt(), 1).bind()
    }

    /**
     * Writes 64 bit doubleword value starting at specified Memory address. Note
     * that
     * high-order 32 bits are stored in higher (value) memory word regardless
     * of "endianness".
     *
     * @param address
     * Starting address of Memory address to be set.
     * @param value
     * Value to be stored at that address.
     * @return old value that was replaced by setDouble operation.
     * @throws MemoryError
     * if any.
     */
    @CheckReturnValue
    override fun setDoubleWord(address: Int, value: Long): Either<MemoryError, Unit> = either {
        val oldHighOrder = set(address + 4, (value shr 32).toInt(), 4).bind()
        val oldLowOrder = set(address, value.toInt(), 4).bind()
        val old = (oldHighOrder.toLong() shl 32) or (oldLowOrder.toLong() and 0xFFFFFFFFL)
        if (OtherSettings.isBacksteppingEnabled)
            Globals.PROGRAM!!.backStepper!!.addMemoryRestoreDoubleWord(address, old)
        else old
    }

    /**
     * Writes 64 bit double value starting at specified Memory address. Note that
     * high-order 32 bits are stored in higher (value) memory word regardless
     * of "endianness".
     *
     * @param address
     * Starting address of Memory address to be set.
     * @param value
     * Value to be stored at that address.
     * @return old value that was replaced by setDouble operation.
     * @throws MemoryError
     * if any.
     */
    @CheckReturnValue
    fun setDouble(address: Int, value: Double): Either<MemoryError, Unit> {
        val longValue = value.toLongReinterpreted()
        return setDoubleWord(address, longValue)
    }

    /**
     * Stores ProgramStatement in Text Segment.
     *
     * @param address
     * Starting address of Memory address to be set. Must be word
     * boundary.
     * @param statement
     * Machine code to be stored starting at that address -- for
     * simulation
     * purposes, actually stores reference to ProgramStatement
     * instead of 32-bit machine code.
     * @throws MemoryError
     * If address is not on word boundary or is
     * outside Text Segment.
     * @see ProgramStatement
     */
    @CheckReturnValue
    override fun setProgramStatement(address: Int, statement: ProgramStatement): Either<MemoryError, Unit> = either {
        checkStoreWordAligned(address).bind()
        ensure(isAddressInTextSegment(address)) {
            MemoryError(
                "Store address to text segment out of range",
                EventReason.STORE_ACCESS_FAULT,
                address
            )
        }
        if (Globals.debug) {
            LOGGER.debug("memory[{}] set to {}", address, statement.binaryStatement)
        }
        storeProgramStatement(
            address,
            statement,
            memoryConfiguration.textSegmentBaseAddress,
            textBlockTable
        )
    }

    /**
     * Starting at the given word address, read the given number of bytes (max 4).
     * This one does not check for word boundaries, and copies one byte at a time.
     * If length == 1, puts value in low order byte. If 2, puts into low order
     * half-word.
     *
     * @param address
     * Starting address of Memory address to be read.
     * @param length
     * Number of bytes to be read.
     * @return Value stored starting at that address.
     * @throws MemoryError
     * if any.
     */
    @CheckReturnValue
    fun get(address: Int, length: Int): Either<MemoryError, Int> = get(address, length, true)

    /** Does the real work, but includes option to NOT notify observers.  */
    @CheckReturnValue
    private fun get(address: Int, length: Int, notify: Boolean): Either<MemoryError, Int> = either {
        val value: Int
        val relativeByteAddress: Int
        when {
            isAddressInDataSegment(address) -> {
                // in data segment. Will read one byte at a time, w/o regard to boundaries.
                relativeByteAddress =
                    (address - memoryConfiguration.dataSegmentBaseAddress) // relative to data segment start, in bytes
                value = fetchBytesFromTable(dataBlockTable, relativeByteAddress, length)
            }
            isAddressInStackRange(address) -> {
                // in stack. Similar to data, except relative address computed "backward"
                relativeByteAddress = memoryConfiguration.stackBaseAddress - address
                value = fetchBytesFromTable(stackBlockTable, relativeByteAddress, length)
            }
            address in memoryConfiguration.memoryMapBaseAddress..<actualMemoryMapLimitAddress -> {
                // memory mapped I/O.
                relativeByteAddress = address - memoryConfiguration.memoryMapBaseAddress
                value = fetchBytesFromTable(memoryMapBlockTable, relativeByteAddress, length)
            }
            isAddressInTextSegment(address) -> {
                ensure(Globals.BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) {
                    MemoryError(
                        "Cannot read directly from text segment!",
                        EventReason.LOAD_ACCESS_FAULT,
                        address
                    )
                }
                ensure(address % 4 + length <= 4) {
                    // TODO: add checks for halfword load not aligned to halfword boundary
                    MemoryError(
                        "Load address not aligned to word boundary ",
                        EventReason.LOAD_ADDRESS_MISALIGNED,
                        address
                    )
                }
                val stmt = getStatementNoNotify((address / 4) * 4).bind()
                value = when {
                    stmt == null -> 0
                    length == 4 -> stmt.binaryStatement
                    else -> (stmt.binaryStatement shr (8 * (address % 4)) and ((1 shl length * 8) - 1))
                }
            }
            else -> {
                // falls outside addressing range
                raise(MemoryError("address out of range ", EventReason.LOAD_ACCESS_FAULT, address))
            }
        }
        if (notify) {
            notifyAnyObservers(AccessType.READ, address, length, value)
        }
        value
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int.
     * It transfers the 32 bit value "raw" as stored in memory, and does not adjust
     * for byte order (big or little endian). Address must be word-aligned.
     *
     * @param address
     * Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address.
     * @throws MemoryError
     * If address is not on word boundary.
     */
    @CheckReturnValue
    fun getRawWord(address: Int): Either<MemoryError, Int> = either {
        // Note: the logic here is repeated in getRawWordOrNull() below. Logic is
        // simplified by having this method just call getRawWordOrNull() then
        // return either the int of its return value, or 0 if it returns null.
        // Doing so would be detrimental to simulation runtime performance, so
        // I decided to keep the duplicate logic.
        checkLoadWordAligned(address).bind()
        val relative: Int
        val value: Int
        when {
            isAddressInDataSegment(address) -> {
                relative = ((address - memoryConfiguration.dataSegmentBaseAddress) shr 2)
                value = fetchWordFromTable(dataBlockTable, relative)
            }
            isAddressInStackRange(address) -> {
                relative = (memoryConfiguration.stackBaseAddress - address) shr 2
                value = fetchWordFromTable(stackBlockTable, relative)
            }
            address in memoryConfiguration.memoryMapBaseAddress..<actualMemoryMapLimitAddress -> {
                relative = (address - memoryConfiguration.memoryMapBaseAddress) shr 2
                value = fetchWordFromTable(memoryMapBlockTable, relative)
            }
            isAddressInTextSegment(address) -> {
                ensure(Globals.BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) {
                    MemoryError(
                        "Cannot read directly from text segment!",
                        EventReason.LOAD_ACCESS_FAULT,
                        address
                    )
                }
                val stmt = getStatementNoNotify(address).bind()
                value = stmt?.binaryStatement ?: 0
            }
            else -> raise(MemoryError("address out of range ", EventReason.LOAD_ACCESS_FAULT, address))
        }
        notifyAnyObservers(AccessType.READ, address, DataTypes.WORD_SIZE, value)
        value
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int and return
     * Integer.
     * It transfers the 32 bit value "raw" as stored in memory, and does not adjust
     * for byte order (big or little endian). Address must be word-aligned.
     *
     *
     * Returns null if reading from text segment and there is no instruction at the
     * requested address. Returns null if reading from data segment and this is the
     * first reference to the 4K memory allocation block (i.e., an array to
     * hold the memory has not been allocated).
     *
     *
     * This method was developed by Greg Giberling of UC Berkeley to support the
     * memory
     * dump feature that he implemented in Fall 2007.
     *
     * @param address
     * Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address as an Integer.
     * Conditions
     * that cause return value null are described above.
     * @throws MemoryError
     * If address is not on word boundary.
     */
    @CheckReturnValue
    fun getRawWordOrNull(address: Int): Either<MemoryError, Int?> = either {
        // See note above, with getRawWord(), concerning duplicated logic.
        checkLoadWordAligned(address).bind()
        when {
            isAddressInDataSegment(address) -> {
                val relativeAddress = ((address - memoryConfiguration.dataSegmentBaseAddress) shr 2)
                fetchWordOrNullFromTable(dataBlockTable, relativeAddress)
            }
            isAddressInStackRange(address) -> {
                val relativeAddress = ((memoryConfiguration.stackBaseAddress - address) shr 2)
                fetchWordOrNullFromTable(stackBlockTable, relativeAddress)
            }
            isAddressInTextSegment(address) -> {
                val statement = getStatementNoNotify(address).bind()
                statement?.binaryStatement ?: 0
            }
            else -> raise(
                MemoryError(
                    "address out of range ",
                    EventReason.LOAD_ACCESS_FAULT,
                    address
                )
            )
        }
        // Do not notify observers. This read operation is initiated by the
        // dump feature, not the executing program.
    }

    /**
     * Look for first "null" memory value in an address range. For text segment
     * (binary code), this
     * represents a word that does not contain an instruction. Normally use this to
     * find the end of
     * the program. For data segment, this represents the first block of simulated
     * memory (block length
     * currently 4K words) that has not been referenced by an assembled/executing
     * program.
     *
     * @param baseAddress
     * lowest address to be searched; the starting point
     * @param limitAddress
     * highest address to be searched
     * @return lowest address within specified range that contains "null" value as
     * described above.
     * @throws MemoryError
     * if the base address is not on a word boundary
     */
    fun getAddressOfFirstNull(baseAddress: Int, limitAddress: Int): Either<MemoryError, Int?> {
        var address = baseAddress
        while (address < limitAddress) {
            val value = this.getRawWordOrNull(address)
            when (value) {
                is Either.Left -> return value
                is Either.Right -> if (value.value == null) return address.right()
            }
            address += DataTypes.WORD_SIZE
        }
        return null.right()
    }

    /**
     * Reads 64 bit dword value starting at specified Memory address.
     *
     * @param address
     * Starting address of Memory address to be read
     * @return Double Word (8-byte value) stored starting at that address.
     * @throws MemoryError
     * If address is not on word boundary.
     */
    @CheckReturnValue
    override fun getDoubleWord(address: Int): Either<MemoryError, Long> = either {
        checkLoadWordAligned(address).bind()
        val oldHighOrder = get(address + 4, 4).bind()
        val oldLowOrder = get(address, 4).bind()
        (oldHighOrder.toLong() shl 32) or (oldLowOrder.toLong() and 0xFFFFFFFFL)
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int.
     * Does not use "get()"; we can do it faster here knowing we're working only
     * with full words.
     *
     * @param address
     * Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address.
     * @throws MemoryError
     * If address is not on word boundary.
     */
    @CheckReturnValue
    override fun getWord(address: Int): Either<MemoryError, Int> = checkLoadWordAligned(address).flatMap {
        get(address, DataTypes.WORD_SIZE, true)
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int.
     * Does not use "get()"; we can do it faster here knowing we're working only
     * with full words. Observers are NOT notified.
     *
     * @param address
     * Starting address of word to be read.
     * @return Word (4-byte value) stored starting at that address.
     * @throws MemoryError
     * If address is not on word boundary.
     */
    @CheckReturnValue
    private fun getWordNoNotify(address: Int): Either<MemoryError, Int> = checkLoadWordAligned(address).flatMap {
        get(address, DataTypes.WORD_SIZE, false)
    }

    /**
     * Starting at the given word address, read a 2 byte word as a short value.
     *
     * @param address
     * Starting address of word to be read.
     * @return Halfword (2-byte value) stored starting at that address, stored in
     * lower 16 bits.
     * @throws MemoryError
     * If address is not on halfword boundary.
     */
    @CheckReturnValue
    override fun getHalf(address: Int): Either<MemoryError, Short> = if (address % 2 != 0) {
        MemoryError(
            "Load address not aligned on halfword boundary ",
            EventReason.LOAD_ADDRESS_MISALIGNED,
            address
        ).left()
    } else this.get(address, 2).map {
        (it and 0xFFFF).toShort()
    }

    @CheckReturnValue
    override fun getByte(address: Int): Either<MemoryError, Byte> = this.get(address, 1).map {
        (it and 0xFF).toByte()
    }

    /**
     * Gets ProgramStatement from Text Segment.
     *
     * @param address
     * Starting address of Memory address to be read. Must be word
     * boundary.
     * @return reference to ProgramStatement object associated with that address, or
     * null if none.
     * @throws MemoryError
     * If address is not on word boundary or is
     * outside Text Segment.
     * @see ProgramStatement
     */
    @CheckReturnValue
    override fun getProgramStatement(address: Int): Either<MemoryError, ProgramStatement?> =
        this.getStatementImpl(address, true)

    /**
     * Gets ProgramStatement from Text Segment without notifying observers.
     *
     * @param address
     * Starting address of Memory address to be read. Must be word
     * boundary.
     * @return reference to ProgramStatement object associated with that address, or
     * null if none.
     * @throws MemoryError
     * If address is not on word boundary or is
     * outside Text Segment.
     * @see ProgramStatement
     */
    @CheckReturnValue
    private fun getStatementNoNotify(address: Int): Either<MemoryError, ProgramStatement?> =
        this.getStatementImpl(address, false)

    @CheckReturnValue
    private fun getStatementImpl(address: Int, notify: Boolean): Either<MemoryError, ProgramStatement?> = either {
        checkLoadWordAligned(address).bind()
        ensure(
            Globals.BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)
                || isAddressInTextSegment(address)
        ) {
            MemoryError(
                "fetch address for text segment out of range ",
                EventReason.LOAD_ACCESS_FAULT,
                address
            )
        }
        return if (isAddressInTextSegment(address)) {
            readProgramStatement(
                address,
                memoryConfiguration.textSegmentBaseAddress,
                textBlockTable,
                notify
            ).right()
        } else {
            get(address, DataTypes.WORD_SIZE).map { binaryStatement ->
                ProgramStatement(binaryStatement, address)
            }
        }
    }

    /**
     * Method to accept registration from observer for any memory address.
     */
    fun subscribe(listener: Listener<MemoryAccessNotice>): Either<MemoryError, MemoryListenerHandle<Int>> =
        subscribe(listener, 0, -0x4).onLeft {
            LOGGER.error("Internal error in Memory#subscribe: {}", it.toString())
        }

    /**
     * Method to accept registration from observer for specific address. This
     * includes
     * the memory word starting at the given address.
     *
     * @param obs
     * the observer
     * @param addr
     * the memory address which must be on word boundary
     * @throws MemoryError
     * if any.
     */
    @CheckReturnValue
    fun subscribe(
        obs: (MemoryAccessNotice) -> Unit,
        addr: Int
    ): Either<MemoryError, MemoryListenerHandle<Int>> = this.subscribe(obs, addr, addr)

    /**
     * Method to accept registration from observer for specific address range. The
     * last byte included in the address range is the last byte of the word
     * specified
     * by the ending address. Note to observers: this class delegates Observable
     * operations
     * so notices will come from the delegate, not the memory object.
     *
     * @param listener
     * the observer
     * @param startAddr
     * the low end of memory address range, must be on word
     * boundary
     * @param endAddr
     * the high end of memory address range, must be on word
     * boundary
     * @throws MemoryError
     * if any.
     */
    @CheckReturnValue
    override fun subscribe(
        listener: (MemoryAccessNotice) -> Unit,
        startAddress: Int,
        endAddress: Int
    ): Either<MemoryError, MemoryListenerHandle<Int>> = either {
        checkLoadWordAligned(startAddress).bind()
        ensure(startAddress.toUInt() <= endAddress.toUInt()) {
            MemoryError(
                "end address of range < start address of range ",
                EventReason.LOAD_ACCESS_FAULT,
                startAddress
            )
        }
        MemoryListenerHandle(listener, startAddress, endAddress).also {
            observables.add(MemoryObservable(it))
        }
    }

    override fun unsubscribe(handle: MemoryListenerHandle<Int>) {
        this.observables.removeIf { it.handle == handle }
    }

    /**
     * Method to notify any observers of memory operation that has just occurred.
     */
    private fun notifyAnyObservers(
        type: AccessType,
        address: Int,
        length: Int,
        value: Int
    ) {
        this.observables
            .filter { it.match(address) }
            .forEach {
                it.dispatcher.dispatch(
                    MemoryAccessNotice(type, address, length, value)
                )
            }
    }

    /**
     * Helper method to store 1, 2 or 4 byte value in table that represents
     * memory. Originally used just for data segment, but now also used for stack.
     * Both use different tables but same storage method and same table size
     * and block size.
     * Modified 29 Dec 2005 to return old value of replaced bytes.
     */
    private fun storeBytesInTable(
        blockTable: Array<IntArray?>,
        relativeByteAddress: Int,
        length: Int,
        value: Int
    ): Int {
        // If address in stack segment is 4k + m, with 0 < m < 4, then the
        // relativeByteAddress we want is stackBaseAddress - 4k + m, but the
        // address actually passed in is stackBaseAddress - (4k + m), so we
        // need to add 2m. Because of the change in sign, we get the
        // expression 4-delta below in place of m.
        synchronized(this) {
            var relativeByteAddress1 = relativeByteAddress
            if (blockTable == this.stackBlockTable) {
                val delta = relativeByteAddress1 % 4
                if (delta != 0) {
                    relativeByteAddress1 += (4 - delta) shl 1
                }
            }
            // for STORE, return old values of replaced bytes
            var oldValue = 0
            val value1 = value
            for (bytePositionInValue in 3 downTo 3 - length + 1) {
                val relativeWordAddress = relativeByteAddress1 shr 2
                val block: Int = relativeWordAddress / BLOCK_LENGTH_WORDS // Block number
                if (blockTable[block] == null) {
                    blockTable[block] = IntArray(BLOCK_LENGTH_WORDS)
                }
                val bytePositionInMemory = 3 - relativeByteAddress1 % 4
                // Word within that block
                val offset: Int = relativeWordAddress % BLOCK_LENGTH_WORDS
                oldValue = replaceByte(
                    blockTable[block]!![offset],
                    bytePositionInMemory,
                    oldValue,
                    bytePositionInValue
                )
                blockTable[block]!![offset] = replaceByte(
                    value1,
                    bytePositionInValue,
                    blockTable[block]!![offset],
                    bytePositionInMemory
                )
                relativeByteAddress1++
            }
            return oldValue
        }
    }

    /**
     * Helper method to fetch 1, 2 or 4 byte value from table that represents
     * memory. Originally used just for data segment, but now also used for stack.
     * Both use different tables but same storage method and same table size
     * and block size.
     */
    private fun fetchBytesFromTable(
        blockTable: Array<IntArray?>,
        relativeByteAddress: Int,
        length: Int
    ): Int {
        // If address in stack segment is 4k + m, with 0 < m < 4, then the
        // relativeByteAddress we want is stackBaseAddress - 4k + m, but the
        // address actually passed in is stackBaseAddress - (4k + m), so we
        // need to add 2m. Because of the change in sign, we get the
        // expression 4-delta below in place of m.
        synchronized(this) {
            var relativeByteAddress1 = relativeByteAddress
            if (blockTable == this.stackBlockTable) {
                val delta = relativeByteAddress1 % 4
                if (delta != 0) {
                    relativeByteAddress1 += (4 - delta) shl 1
                }
            }
            val loopStopper = 3 - length
            var result = 0
            for (bytePositionInValue in 3 downTo loopStopper + 1) {
                val bytePositionInMemory = 3 - relativeByteAddress1 % 4
                val relativeWordAddress = relativeByteAddress1 shr 2
                val blockIndex: Int = relativeWordAddress / BLOCK_LENGTH_WORDS // Block number
                if (blockTable[blockIndex] == null) {
                    return 0
                }
                // noinspection DataFlowIssue
                // Word within that block
                val offset: Int = relativeWordAddress % BLOCK_LENGTH_WORDS
                result = replaceByte(
                    blockTable[blockIndex]!![offset],
                    bytePositionInMemory,
                    result,
                    bytePositionInValue
                )
                relativeByteAddress1++
            }
            return result
        }
    }

    /**
     * Helper method to store 4 byte value in table that represents memory.
     * Originally used just for data segment, but now also used for stack.
     * Both use different tables but same storage method and same table size
     * and block size. Assumes address is word aligned, no endian processing.
     * Modified 29 Dec 2005 to return overwritten value.
     */
    @Synchronized
    private fun storeWordInTable(
        blockTable: Array<IntArray?>,
        relative: Int,
        value: Int
    ): Int {
        val blockIndex: Int = relative / BLOCK_LENGTH_WORDS
        if (blockTable[blockIndex] == null) {
            // First time writing to this block, so allocate the space.
            blockTable[blockIndex] = IntArray(BLOCK_LENGTH_WORDS)
        }
        val offset: Int = relative % BLOCK_LENGTH_WORDS
        val oldValue = blockTable[blockIndex]!![offset]
        // noinspection DataFlowIssue
        blockTable[blockIndex]!![offset] = value
        return oldValue
    }

    /**
     * Same as [Memory.storeWordInTable], but doesn't set, just gets
     */
    @Contract(pure = true)
    @Synchronized
    private fun fetchWordFromTable(blockTable: Array<IntArray?>, relative: Int): Int {
        val result = fetchWordOrNullFromTable(blockTable, relative)
        return result ?: 0
    }

    /**
     * Same as [Memory.fetchWordFromTable], but if it hasn't been allocated returns null.
     */
    @Contract(pure = true)
    @Synchronized
    private fun fetchWordOrNullFromTable(
        blockTable: Array<IntArray?>,
        relative: Int
    ): Int? {
        // Developed by Greg Gibeling of UC Berkeley, fall 2007.
        val block: Int = relative / BLOCK_LENGTH_WORDS
        if (blockTable[block] == null) {
            // first reference to an address in this block. Assume initialized to 0.
            return null
        } else {
            // noinspection DataFlowIssue
            val offset: Int = relative % BLOCK_LENGTH_WORDS
            return blockTable[block]!![offset]
        }
    }

    /**
     * Read a program statement from the given address. Address has already been
     * verified
     * as valid.
     *
     * @param address
     * the address to read from
     * @param baseAddress
     * the base address for the section being read from (.text)
     * @param blockTable
     * the internal table of program statements
     * @param notify
     * whether or not it notifies observers
     * @return associated ProgramStatement or null if none.
     */
    private fun readProgramStatement(
        address: Int,
        baseAddress: Int,
        blockTable: Array<Array<ProgramStatement?>?>,
        notify: Boolean
    ): ProgramStatement? {
        val relative = (address - baseAddress) shr 2 // convert byte address to words
        val block: Int = relative / TEXT_BLOCK_LENGTH_WORDS
        if (block < TEXT_BLOCK_TABLE_LENGTH) {
            val offset: Int = relative % TEXT_BLOCK_LENGTH_WORDS
            if (blockTable[block] == null || blockTable[block]!![offset] == null) {
                // No instructions are stored in this block or offset.
                if (notify) {
                    this.notifyAnyObservers(
                        AccessType.READ,
                        address,
                        BasicInstruction.BASIC_INSTRUCTION_LENGTH,
                        0
                    )
                }
                return null
            } else {
                if (notify) {
                    this.notifyAnyObservers(
                        AccessType.READ,
                        address,
                        BasicInstruction.BASIC_INSTRUCTION_LENGTH,
                        blockTable[block]!![offset]!!.binaryStatement
                    )
                }
                return blockTable[block]!![offset]
            }
        }
        if (notify) {
            this.notifyAnyObservers(
                AccessType.READ,
                address,
                BasicInstruction.BASIC_INSTRUCTION_LENGTH,
                0
            )
        }
        return null
    }

    fun setMemoryConfigurationAndReset(newConfiguration: AbstractMemoryConfiguration<Int>) {
        this._memoryConfiguration = newConfiguration
        this.actualDataSegmentLimitAddress = min(
            this.memoryConfiguration.dataSegmentLimitAddress.toDouble(),
            (this.memoryConfiguration.dataSegmentBaseAddress
                + BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * DataTypes.WORD_SIZE).toDouble()
        ).toInt()
        this.actualTextLimitAddress = min(
            this.memoryConfiguration.textSegmentLimitAddress.toDouble(),
            (this.memoryConfiguration.textSegmentBaseAddress
                + TEXT_BLOCK_LENGTH_WORDS * TEXT_BLOCK_TABLE_LENGTH * DataTypes.WORD_SIZE).toDouble()
        ).toInt()
        this.actualStackLimitAddress = max(
            this.memoryConfiguration.heapBaseAddress.toDouble(),
            (this.memoryConfiguration.stackBaseAddress
                - BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * DataTypes.WORD_SIZE).toDouble()
        ).toInt()
        this.actualMemoryMapLimitAddress = min(
            this.memoryConfiguration.memoryMapLimitAddress.toDouble(),
            (this.memoryConfiguration.memoryMapBaseAddress
                + BLOCK_LENGTH_WORDS * MMIO_TABLE_LENGTH * DataTypes.WORD_SIZE).toDouble()
        ).toInt()
        this.reset()
    }
}

private val LOGGER: Logger = LogManager.getLogger(Memory::class.java)

// TODO: add some heap managment so programs can malloc and free
