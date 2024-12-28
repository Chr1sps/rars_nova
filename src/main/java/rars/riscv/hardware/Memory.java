package rars.riscv.hardware;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.exceptions.ExceptionReason;
import rars.notices.AccessNotice;
import rars.notices.MemoryAccessNotice;
import rars.riscv.BasicInstruction;
import rars.settings.BoolSetting;
import rars.settings.OtherSettings;
import rars.util.CustomPublisher;
import rars.util.SimpleSubscriber;

import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.Flow;

import static rars.settings.Settings.BOOL_SETTINGS;
import static rars.settings.Settings.OTHER_SETTINGS;


/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
 * Represents memory. Different segments are represented by different data
 * structs.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public class Memory extends CustomPublisher<MemoryAccessNotice> {
    /**
     * Word length in bytes.
     **/
    // NOTE: Much of the code is hardwired for 4 byte words. Refactoring this is low
    // priority.
    public static final int WORD_LENGTH_BYTES = 4;
    /**
     * Constant representing byte order of each memory word. Little-endian means
     * lowest
     * numbered byte is right most [3][2][1][0].
     */
    public static final boolean LITTLE_ENDIAN = true;
    private static final Logger LOGGER = LogManager.getLogger();
    /**
     * Current setting for endian (default LITTLE_ENDIAN)
     **/
    private static final boolean byteOrder = Memory.LITTLE_ENDIAN;
    private static final int BLOCK_LENGTH_WORDS = 1024; // allocated blocksize 1024 ints == 4K bytes
    private static final int BLOCK_TABLE_LENGTH = 1024; // Each entry of table points to a block.
    // not MIPS
    private static final int MMIO_TABLE_LENGTH = 16; // Each entry of table points to a 4K block.
    // SPIM not MIPS
    private static final int TEXT_BLOCK_LENGTH_WORDS = 1024; // allocated blocksize 1024 ints == 4K bytes
    private static final int TEXT_BLOCK_TABLE_LENGTH = 1024; // Each entry of table points to a block.
    // Helper method to store 1, 2 or 4 byte second in table that represents
    // memory. Originally used just for data segment, but now also used for stack.
    // Both use different tables but same storage method and same table size
    // and block size.
    // Modified 29 Dec 2005 to return old second of replaced bytes.
    private static final boolean STORE = true;
    private static final boolean FETCH = false;
    /**
     * base address for (user) text segment: 0x00400000
     **/
    public static int textBaseAddress = MemoryConfiguration.DEFAULT.textBaseAddress; // 0x00400000;
    /**
     * base address for (user) data segment: 0x10000000
     **/
    public static int dataSegmentBaseAddress = MemoryConfiguration.DEFAULT.dataSegmentBaseAddress; // 0x10000000;
    /**
     * base address for .extern directive: 0x10000000
     **/
    public static int externBaseAddress = MemoryConfiguration.DEFAULT.externBaseAddress; // 0x10000000;

    // TODO: remove this as RISCV is little endian and it is only used in like one
    // spot
    /**
     * base address for storing globals
     **/
    public static int globalPointer = MemoryConfiguration.DEFAULT.globalPointerAddress; // 0x10008000;
    /**
     * base address for storage of non-global static data in data segment:
     * 0x10010000 (from SPIM)
     **/
    public static int dataBaseAddress = MemoryConfiguration.DEFAULT.dataBaseAddress; // 0x10010000; // from SPIM
    /**
     * base address for heap: 0x10040000 (I think from SPIM not MIPS)
     **/
    public static int heapBaseAddress = MemoryConfiguration.DEFAULT.heapBaseAddress; // 0x10040000; // I think from

    // Memory will maintain a collection of observables. Each one is associated
    // with a specific memory address or address range, and each will have at least
    // one observer registered with it. When memory access is made, make sure only
    // observables associated with that address send notices to their observers.
    // This assures that observers are not bombarded with notices from memory
    // addresses they do not care about.
    // Would like a tree-like implementation, but that is complicated by this fact:
    // first for insertion into the tree would be based on Comparable using both low
    // and high end of address range, but retrieval from the tree has to be based
    // on target address being ANYWHERE IN THE RANGE (not an exact first match).
    /**
     * starting address for stack: 0x7fffeffc (this is from SPIM not MIPS)
     **/
    public static int stackPointer = MemoryConfiguration.DEFAULT.stackPointerAddress; // 0x7fffeffc;

    // The data segment is allocated in blocks of 1024 ints (4096 bytes). Each block
    // is
    // referenced by a "block table" entry, and the table has 1024 entries. The
    // capacity
    // is thus 1024 entries * 4096 bytes = 4 MB. Should be enough to cover most
    // programs!! Beyond that it would go to an "indirect" block (similar to Unix
    // i-nodes),
    // which is not implemented.
    // Although this scheme is an array of arrays, it is relatively space-efficient
    // since
    // only the table is created initially. A 4096-byte block is not allocated until
    // a second
    // is written to an address within it. Thus most small programs will use only 8K
    // bytes
    // of space (the table plus one block). The index into both arrays is easily
    // computed
    // from the address; access time is constant.
    // SPIM stores statically allocated data (following first .data directive)
    // starting
    // at location 0x10010000. This is the first Data Segment word beyond the reach
    // of $gp
    // used in conjunction with signed 16 bit immediate offset. $gp has second
    // 0x10008000
    // and with the signed 16 bit offset can reach from 0x10008000 - 0xFFFF =
    // 0x10000000
    // (Data Segment base) to 0x10008000 + 0x7FFF = 0x1000FFFF (the byte preceding
    // 0x10010000).
    // Using my scheme, 0x10010000 falls at the beginning of the 17'th block --
    // table entry 16.
    // SPIM uses a heap base address of 0x10040000 which is not part of the MIPS
    // specification.
    // (I don't have a reference for that offhand...) Using my scheme, 0x10040000
    // falls at
    // the start of the 65'th block -- table entry 64. That leaves (1024-64) * 4096
    // = 3,932,160
    // bytes of space available without going indirect.
    /**
     * base address for stack: 0x7ffffffc (this is mine - start of highest word
     * below kernel space)
     **/
    public static int stackBaseAddress = MemoryConfiguration.DEFAULT.stackBaseAddress; // 0x7ffffffc;
    /**
     * highest address accessible in user (not kernel) mode.
     **/
    public static int userHighAddress = MemoryConfiguration.DEFAULT.userHighAddress; // 0x7fffffff;
    /**
     * kernel boundary. Only OS can access this or higher address
     **/
    public static int kernelBaseAddress = MemoryConfiguration.DEFAULT.kernelBaseAddress; // 0x80000000;

    // The stack is modeled similarly to the data segment. It cannot share the same
    // data structure because the stack base address is very large. To store it in
    // the
    // same data structure would require implementation of indirect blocks, which
    // has not
    // been realized. So the stack gets its own table of blocks using the same
    // dimensions
    // and allocation scheme used for data segment.
    // The other major difference is the stack grows DOWNWARD from its base address,
    // not
    // upward. I.e., the stack base is the largest stack address. This turns the
    // whole
    // scheme for translating memory address to block-offset on its head! The
    // simplest
    // solution is to calculate relative address (offset from base) by subtracting
    // the
    // desired address from the stack base address (rather than subtracting base
    // address
    // from desired address). Thus as the address gets smaller the offset gets
    // larger.
    // Everything else works the same, so it shares some private helper methods with
    // data segment algorithms.
    /**
     * starting address for memory mapped I/O: 0xffff0000 (-65536)
     **/
    public static int memoryMapBaseAddress = MemoryConfiguration.DEFAULT.memoryMapBaseAddress; // 0xffff0000;

    // Memory mapped I/O is simulated with a separate table using the same structure
    // and
    // logic as data segment. Memory is allocated in 4K byte blocks. But since MMIO
    // address range is limited to 0xffff0000 to 0xfffffffc, there are only 64K
    // bytes
    // total. Thus there will be a maximum of 16 blocks, and I suspect never more
    // than
    // one since only the first few addresses are typically used. The only exception
    // may be a rogue program generating such addresses in a loop. Note that the
    // MMIO addresses are interpreted by Java as negative numbers since it does not
    // have unsigned types. As long as the absolute address is correctly translated
    // into a table offset, this is of no concern.
    /**
     * highest address acessible in kernel mode.
     **/
    public static int kernelHighAddress = MemoryConfiguration.DEFAULT.kernelHighAddress; // 0xffffffff;
    /**
     * Constant <code>heapAddress=</code>
     */
    public static int heapAddress;

    // I use a similar scheme for storing instructions. MIPS text segment ranges
    // from
    // 0x00400000 all the way to data segment (0x10000000) a range of about 250 MB!
    // So
    // I'll provide table of blocks with similar capacity. This differs from data
    // segment
    // somewhat in that the block entries do not contain int's, but instead contain
    // references to ProgramStatement objects.
    /**
     * Constant <code>dataSegmentLimitAddress=dataSegmentBaseAddress +
     * BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * WORD_LENGTH_BYTES</code>
     */
    public static int dataSegmentLimitAddress = Memory.dataSegmentBaseAddress +
        Memory.BLOCK_LENGTH_WORDS * Memory.BLOCK_TABLE_LENGTH * Memory.WORD_LENGTH_BYTES;
    /**
     * Constant <code>textLimitAddress=textBaseAddress +
     * TEXT_BLOCK_LENGTH_WORDS * TEXT_BLOCK_TABLE_LENGTH * WORD_LENGTH_BYTES</code>
     */
    public static int textLimitAddress = Memory.textBaseAddress +
        Memory.TEXT_BLOCK_LENGTH_WORDS * Memory.TEXT_BLOCK_TABLE_LENGTH * Memory.WORD_LENGTH_BYTES;
    /**
     * Constant <code>stackLimitAddress=stackBaseAddress -
     * BLOCK_LENGTH_WORDS * BLOCK_TABLE_LENGTH * WORD_LENGTH_BYTES</code>
     */
    public static int stackLimitAddress = Memory.stackBaseAddress -
        Memory.BLOCK_LENGTH_WORDS * Memory.BLOCK_TABLE_LENGTH * Memory.WORD_LENGTH_BYTES;

    // Set "top" address boundary to go with each "base" address. This determines
    // permissable
    // address range for user program. Currently limit is 4MB, or 1024 * 1024 * 4
    // bytes based
    // on the table structures described above (except memory mapped IO, limited to
    // 64KB by range).
    /**
     * Constant <code>memoryMapLimitAddress=memoryMapBaseAddress +
     * BLOCK_LENGTH_WORDS * MMIO_TABLE_LENGTH * WORD_LENGTH_BYTES</code>
     */
    public static int memoryMapLimitAddress = Memory.memoryMapBaseAddress +
        Memory.BLOCK_LENGTH_WORDS * Memory.MMIO_TABLE_LENGTH * Memory.WORD_LENGTH_BYTES;
    private static Memory uniqueMemoryInstance = new Memory();
    private final Collection<MemoryObservable> observables = Memory.getNewMemoryObserversCollection();
    private int[][] dataBlockTable;
    // This will be a Singleton class, only one instance is ever created. Since I
    // know the
    // Memory object is always needed, I'll go ahead and create it at the time of
    // class loading.
    // (greedy rather than lazy instantiation). The constructor is private and
    // getInstance()
    // always returns this instance.
    private int[][] stackBlockTable;

    /*
     * Private constructor for Memory. Separate data structures for text and data
     * segments.
     **/
    private int[][] memoryMapBlockTable;
    private ProgramStatement[][] textBlockTable;

    /**
     * <p>Constructor for Memory.</p>
     */
    public Memory() {
        this.initialize();
    }

    /**
     * <p>swapInstance.</p>
     *
     * @param mem a {@link Memory} object
     * @return a {@link Memory} object
     */
    public static Memory swapInstance(final Memory mem) {
        final Memory temp = Memory.uniqueMemoryInstance;
        Memory.uniqueMemoryInstance = mem;
        return temp;
    }

    /**
     * Returns the unique Memory instance, which becomes in essence global.
     *
     * @return a {@link Memory} object
     */
    public static Memory getInstance() {
        return Memory.uniqueMemoryInstance;
    }

    /**
     * Sets current memory configuration for simulation. Configuration is
     * collection of memory segment addresses. e.g. text segment starting at
     * address 0x00400000. Configuration can be modified starting with MARS 3.7.
     */
    public static void setConfiguration() {
        final var currentConfig = CurrentMemoryConfiguration.get();
        final var currentConfigFromPreferences = OTHER_SETTINGS.getMemoryConfiguration();
        Memory.textBaseAddress = currentConfig.textBaseAddress; // 0x00400000;
        Memory.dataSegmentBaseAddress = currentConfig.dataSegmentBaseAddress;
        // 0x10000000;
        Memory.externBaseAddress = currentConfig.externBaseAddress; // 0x10000000;
        Memory.globalPointer = currentConfig.globalPointerAddress; // 0x10008000;
        Memory.dataBaseAddress = currentConfig.dataBaseAddress; // 0x10010000; 
        // from
        // SPIM not MIPS
        Memory.heapBaseAddress = currentConfig.heapBaseAddress; // 0x10040000; 
        // I think
        // from SPIM not MIPS
        Memory.stackPointer = currentConfig.stackPointerAddress; // 0x7fffeffc;
        Memory.stackBaseAddress = currentConfig.stackBaseAddress; // 0x7ffffffc;
        Memory.userHighAddress = currentConfig.userHighAddress; // 0x7fffffff;
        Memory.kernelBaseAddress = currentConfig.kernelBaseAddress; // 0x80000000;
        Memory.memoryMapBaseAddress = currentConfig.memoryMapBaseAddress; // 
        // 0xffff0000;
        Memory.kernelHighAddress = currentConfig.kernelHighAddress; // 0xffffffff;
        Memory.dataSegmentLimitAddress =
            Math.min(currentConfig.dataSegmentLimitAddress,
                Memory.dataSegmentBaseAddress +
                    Memory.BLOCK_LENGTH_WORDS * Memory.BLOCK_TABLE_LENGTH * Memory.WORD_LENGTH_BYTES);
        Memory.textLimitAddress = Math.min(currentConfig.textLimitAddress,
            Memory.textBaseAddress +
                Memory.TEXT_BLOCK_LENGTH_WORDS * Memory.TEXT_BLOCK_TABLE_LENGTH * Memory.WORD_LENGTH_BYTES);
        Memory.stackLimitAddress = Math.max(currentConfig.stackLimitAddress,
            Memory.stackBaseAddress -
                Memory.BLOCK_LENGTH_WORDS * Memory.BLOCK_TABLE_LENGTH * Memory.WORD_LENGTH_BYTES);
        Memory.memoryMapLimitAddress =
            Math.min(currentConfig.memoryMapLimitAddress,
                Memory.memoryMapBaseAddress +
                    Memory.BLOCK_LENGTH_WORDS * Memory.MMIO_TABLE_LENGTH * Memory.WORD_LENGTH_BYTES);
    }

    /**
     * Returns the next available word-aligned heap address. There is no recycling
     * and
     * no heap management! There is however nearly 4MB of heap space available in
     * Rars.
     *
     * @param numBytes Number of bytes requested. Should be multiple of 4, otherwise
     *                 next higher multiple of 4 allocated.
     * @return address of allocated heap storage.
     * @throws java.lang.IllegalArgumentException if number of requested bytes is negative or
     *                                            exceeds available heap storage
     */
    public static int allocateBytesFromHeap(final int numBytes) throws IllegalArgumentException {
        final int result = Memory.heapAddress;
        if (numBytes < 0) {
            throw new IllegalArgumentException("request (" + numBytes + ") is negative heap amount");
        }
        int newHeapAddress = Memory.heapAddress + numBytes;
        if (newHeapAddress % 4 != 0) {
            newHeapAddress = newHeapAddress + (4 - newHeapAddress % 4); // next higher multiple of 4
        }
        if (newHeapAddress >= Memory.dataSegmentLimitAddress) {
            throw new IllegalArgumentException("request (" + numBytes + ") exceeds available heap storage");
        }
        Memory.heapAddress = newHeapAddress;
        return result;
    }

    // TODO: add some heap managment so programs can malloc and free

    /**
     * Utility to determine if given address is word-aligned.
     *
     * @param address the address to check
     * @return true if address is word-aligned, false otherwise
     */
    public static boolean wordAligned(final int address) {
        return (address % Memory.WORD_LENGTH_BYTES == 0);
    }

    /*
     * ******************************* THE SETTER METHODS
     ******************************/


    private static void checkLoadWordAligned(final int address) throws AddressErrorException {
        if (!Memory.wordAligned(address)) {
            throw new AddressErrorException(
                "Load address not aligned to word boundary ",
                ExceptionReason.LOAD_ADDRESS_MISALIGNED, address);
        }
    }


    private static void checkStoreWordAligned(final int address) throws AddressErrorException {
        if (!Memory.wordAligned(address)) {
            throw new AddressErrorException(
                "Store address not aligned to word boundary ",
                ExceptionReason.STORE_ADDRESS_MISALIGNED, address);
        }
    }


    /**
     * Utility to determine if given address is doubleword-aligned.
     *
     * @param address the address to check
     * @return true if address is doubleword-aligned, false otherwise
     */
    public static boolean doublewordAligned(final int address) {
        return (address % (Memory.WORD_LENGTH_BYTES + Memory.WORD_LENGTH_BYTES) == 0);
    }


    /**
     * Handy little utility to find out if given address is in the text
     * segment (starts at Memory.textBaseAddress).
     * Note that RARS does not implement the entire text segment space,
     * but it does implement enough for hundreds of thousands of lines
     * of code.
     *
     * @param address integer memory address
     * @return true if that address is within RARS-defined text segment,
     * false otherwise.
     */
    public static boolean inTextSegment(final int address) {
        return address >= Memory.textBaseAddress && address < Memory.textLimitAddress;
    }


    /**
     * Handy little utility to find out if given address is in RARS data
     * segment (starts at Memory.dataSegmentBaseAddress).
     *
     * @param address integer memory address
     * @return true if that address is within RARS-defined data segment,
     * false otherwise.
     */
    public static boolean inDataSegment(final int address) {
        return address >= Memory.dataSegmentBaseAddress && address < Memory.dataSegmentLimitAddress;
    }

    /**
     * Handy little utility to find out if given address is in the Memory Map area
     * starts at Memory.memoryMapBaseAddress, range 0xffff0000 to 0xffffffff.
     *
     * @param address integer memory address
     * @return true if that address is within RARS-defined memory map (MMIO) area,
     * false otherwise.
     */
    public static boolean inMemoryMapSegment(final int address) {
        return address >= Memory.memoryMapBaseAddress && address < Memory.kernelHighAddress;
    }


    private static Collection<MemoryObservable> getNewMemoryObserversCollection() {
        return new Vector<>(); // Vectors are thread-safe
    }


    // Returns result of substituting specified byte of source second into specified
    //////////////////////////////////////////////////////////////////////////////////// byte
    // of destination second. Byte positions are 0-1-2-3, listed from most to least
    // significant. No endian issues. This is a private helper method used by get()

    /// ///////////////////////////////////////////////////////////////////////////////// &
    /// ///////////////////////////////////////////////////////////////////////////////// set().
    private static int replaceByte(final int sourceValue, final int bytePosInSource, final int destValue,
                                   final int bytePosInDest) {
        return
            // Set source byte second into destination byte position; set other 24 bits to
            // 0's...
            ((sourceValue >> (24 - (bytePosInSource << 3)) & 0xFF) << (24 - (bytePosInDest << 3)))
                // and bitwise-OR it with...
                |
                // Set 8 bits in destination byte position to 0's, other 24 bits are unchanged.
                (destValue & ~(0xFF << (24 - (bytePosInDest << 3))));
    }

    /* *******************************
     * THE GETTER METHODS
     * *****************************/


    // Store a program statement at the given address. Address has already been

    /// //////////////////////////////////////////////////////////////////// verified
    /// //////////////////////////////////////////////////////////////////// as
    /// //////////////////////////////////////////////////////////////////// valid.
    private static void storeProgramStatement(final int address, final ProgramStatement statement,
                                              final int baseAddress, final ProgramStatement[][] blockTable) {
        final int relative = (address - baseAddress) >> 2; // convert byte address to words
        final int block = relative / Memory.BLOCK_LENGTH_WORDS;
        final int offset = relative % Memory.BLOCK_LENGTH_WORDS;
        if (block < Memory.TEXT_BLOCK_TABLE_LENGTH) {
            if (blockTable[block] == null) {
                // No instructions are stored in this block, so allocate the block.
                blockTable[block] = new ProgramStatement[Memory.BLOCK_LENGTH_WORDS];
            }
            blockTable[block][offset] = statement;
        }
    }

    /**
     * <p>copyFrom.</p>
     *
     * @param other a {@link Memory} object
     * @return a boolean
     */
    public boolean copyFrom(final Memory other) {
        if (this.textBlockTable.length != other.textBlockTable.length ||
            this.dataBlockTable.length != other.dataBlockTable.length ||
            this.stackBlockTable.length != other.stackBlockTable.length ||
            this.memoryMapBlockTable.length != other.memoryMapBlockTable.length) {
            // The memory configurations don't match up
            return false;
        }

        for (int i = 0; i < this.textBlockTable.length; i++) {
            if (other.textBlockTable[i] != null) {
                this.textBlockTable[i] = other.textBlockTable[i].clone(); // TODO: potentially make ProgramStatement 
                // clonable
            } else {
                this.textBlockTable[i] = null;
            }
        }
        for (int i = 0; i < this.dataBlockTable.length; i++) {
            if (other.dataBlockTable[i] != null) {
                this.dataBlockTable[i] = other.dataBlockTable[i].clone();
            } else {
                this.dataBlockTable[i] = null;
            }
        }
        for (int i = 0; i < this.stackBlockTable.length; i++) {
            if (other.stackBlockTable[i] != null) {
                this.stackBlockTable[i] = other.stackBlockTable[i].clone();
            } else {
                this.stackBlockTable[i] = null;
            }
        }
        for (int i = 0; i < this.memoryMapBlockTable.length; i++) {
            if (other.memoryMapBlockTable[i] != null) {
                this.memoryMapBlockTable[i] = other.memoryMapBlockTable[i].clone();
            } else {
                this.memoryMapBlockTable[i] = null;
            }
        }
        return true;
    }


    /**
     * Explicitly clear the contents of memory. Typically done at start of assembly.
     */
    public void clear() {
        Memory.setConfiguration();
        this.initialize();
    }


    private void initialize() {
        Memory.heapAddress = Memory.heapBaseAddress;
        this.textBlockTable = new ProgramStatement[Memory.TEXT_BLOCK_TABLE_LENGTH][];
        this.dataBlockTable = new int[Memory.BLOCK_TABLE_LENGTH][]; // array of null int[] references
        this.stackBlockTable = new int[Memory.BLOCK_TABLE_LENGTH][];
        this.memoryMapBlockTable = new int[Memory.MMIO_TABLE_LENGTH][];
        System.gc(); // call garbage collector on any Table memory just deallocated.
    }

    /**
     * Starting at the given address, write the given second over the given number of
     * bytes.
     * This one does not check for word boundaries, and copies one byte at a time.
     * If length == 1, takes second from low order byte. If 2, takes from low order
     * half-word.
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored starting at that address.
     * @param length  Number of bytes to be written.
     * @return old second that was replaced by the set operation
     * @throws AddressErrorException if any.
     */

    // Allocates blocks if necessary.
    public int set(final int address, int value, final int length) throws AddressErrorException {
        int oldValue = 0;
        if (Globals.debug)
            Memory.LOGGER.debug("memory[{}] set to {}({} bytes)", address, value, length);
        final int relativeByteAddress;
        if (Memory.inDataSegment(address)) {
            // in data segment. Will write one byte at a time, w/o regard to boundaries.
            relativeByteAddress = address - Memory.dataSegmentBaseAddress; // relative to data segment start, in bytes
            oldValue = this.storeBytesInTable(this.dataBlockTable, relativeByteAddress, length, value);
        } else if (address > Memory.stackLimitAddress && address <= Memory.stackBaseAddress) {
            // in stack. Handle similarly to data segment write, except relative byte
            // address calculated "backward" because stack addresses grow down from base.
            relativeByteAddress = Memory.stackBaseAddress - address;
            oldValue = this.storeBytesInTable(this.stackBlockTable, relativeByteAddress, length, value);
        } else if (Memory.inTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with call to setStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting

            if (BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) {
                if (address % 4 + length > 4) {
                    // TODO: add checks for halfword load not aligned to halfword boundary
                    throw new AddressErrorException(
                        "Load address crosses word boundary",
                        ExceptionReason.LOAD_ADDRESS_MISALIGNED, address);
                }
                final ProgramStatement oldStatement = this.getStatementNoNotify((address / 4) * 4);
                if (oldStatement != null) {
                    oldValue = oldStatement.getBinaryStatement();
                }

                // These manipulations set the bits in oldvalue to be like second was placed at
                // address.
                // TODO: like below, make this more clear
                value <<= (address % 4) * 8;
                int mask = length == 4 ? -1 : ((1 << (8 * length)) - 1);
                mask <<= (address % 4) * 8;
                value = (value & mask) | (oldValue & ~mask);
                oldValue = (oldValue & mask) >> (address % 4);
                this.setStatement((address / 4) * 4, new ProgramStatement(value, (address / 4) * 4));
            } else {
                throw new AddressErrorException(
                    "Cannot write directly to text segment!",
                    ExceptionReason.STORE_ACCESS_FAULT, address);
            }
        } else if (address >= Memory.memoryMapBaseAddress && address < Memory.memoryMapLimitAddress) {
            // memory mapped I/O.
            relativeByteAddress = address - Memory.memoryMapBaseAddress;
            oldValue = this.storeBytesInTable(this.memoryMapBlockTable, relativeByteAddress, length, value);
        } else {
            // falls outside addressing range
            throw new AddressErrorException("address out of range ",
                ExceptionReason.STORE_ACCESS_FAULT, address);
        }
        this.notifyAnyObservers(AccessNotice.AccessType.WRITE, address, length, value);
        return oldValue;
    }

    /**
     * Starting at the given word address, write the given second over 4 bytes (a
     * word).
     * It must be written as is, without adjusting for byte order (little vs big
     * endian).
     * Address must be word-aligned.
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored starting at that address.
     * @return old second that was replaced by the set operation.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int setRawWord(final int address, final int value) throws AddressErrorException {
        final int relative;
        int oldValue = 0;
        Memory.checkStoreWordAligned(address);
        if (Memory.inDataSegment(address)) {
            // in data segment
            relative = (address - Memory.dataSegmentBaseAddress) >> 2; // convert byte address to words
            oldValue = this.storeWordInTable(this.dataBlockTable, relative, value);
        } else if (address > Memory.stackLimitAddress && address <= Memory.stackBaseAddress) {
            // in stack. Handle similarly to data segment write, except relative
            // address calculated "backward" because stack addresses grow down from base.
            relative = (Memory.stackBaseAddress - address) >> 2; // convert byte address to words
            oldValue = this.storeWordInTable(this.stackBlockTable, relative, value);
        } else if (Memory.inTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with call to setStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            if (BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) {
                final ProgramStatement oldStatement = this.getStatementNoNotify(address);
                if (oldStatement != null) {
                    oldValue = oldStatement.getBinaryStatement();
                }
                this.setStatement(address, new ProgramStatement(value, address));
            } else {
                throw new AddressErrorException(
                    "Cannot write directly to text segment!",
                    ExceptionReason.STORE_ACCESS_FAULT, address);
            }
        } else if (address >= Memory.memoryMapBaseAddress && address < Memory.memoryMapLimitAddress) {
            // memory mapped I/O.
            relative = (address - Memory.memoryMapBaseAddress) >> 2; // convert byte address to word
            oldValue = this.storeWordInTable(this.memoryMapBlockTable, relative, value);
        } else {
            // falls outside addressing range
            throw new AddressErrorException("store address out of range ",
                ExceptionReason.STORE_ACCESS_FAULT, address);
        }
        this.notifyAnyObservers(AccessNotice.AccessType.WRITE, address, Memory.WORD_LENGTH_BYTES, value);
        if (OtherSettings.getBackSteppingEnabled()) {
            Globals.program.getBackStepper().addMemoryRestoreRawWord(address, oldValue);
        }
        return oldValue;
    }

    /**
     * Starting at the given word address, write the given second over 4 bytes (a
     * word).
     * The address must be word-aligned.
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored starting at that address.
     * @return old second that was replaced by setWord operation.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int setWord(final int address, final int value) throws AddressErrorException {
        Memory.checkStoreWordAligned(address);
        return (OtherSettings.getBackSteppingEnabled())
            ? Globals.program.getBackStepper().addMemoryRestoreWord(address, this.set(address, value,
            Memory.WORD_LENGTH_BYTES))
            : this.set(address, value, Memory.WORD_LENGTH_BYTES);
    }


    /**
     * Starting at the given halfword address, write the lower 16 bits of given
     * second
     * into 2 bytes (a halfword).
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored starting at that address. Only low order 16
     *                bits used.
     * @return old second that was replaced by setHalf operation.
     * @throws AddressErrorException If address is not on halfword boundary.
     */
    public int setHalf(final int address, final int value) throws AddressErrorException {
        if (address % 2 != 0) {
            throw new AddressErrorException("store address not aligned on halfword boundary ",
                ExceptionReason.STORE_ADDRESS_MISALIGNED, address);
        }
        return (OtherSettings.getBackSteppingEnabled())
            ? Globals.program.getBackStepper().addMemoryRestoreHalf(address, this.set(address, value, 2))
            : this.set(address, value, 2);
    }


    /**
     * Writes low order 8 bits of given second into specified Memory byte.
     *
     * @param address Address of Memory byte to be set.
     * @param value   Value to be stored at that address. Only low order 8 bits
     *                used.
     * @return old second that was replaced by setByte operation.
     * @throws AddressErrorException if any.
     */
    public int setByte(final int address, final int value) throws AddressErrorException {
        return (OtherSettings.getBackSteppingEnabled())
            ? Globals.program.getBackStepper().addMemoryRestoreByte(address, this.set(address, value, 1))
            : this.set(address, value, 1);
    }


    /**
     * Writes 64 bit doubleword second starting at specified Memory address. Note
     * that
     * high-order 32 bits are stored in higher (second) memory word regardless
     * of "endianness".
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored at that address.
     * @return old second that was replaced by setDouble operation.
     * @throws AddressErrorException if any.
     */
    public long setDoubleWord(final int address, final long value) throws AddressErrorException {
        final int oldHighOrder;
        final int oldLowOrder;
        oldHighOrder = this.set(address + 4, (int) (value >> 32), 4);
        oldLowOrder = this.set(address, (int) value, 4);
        final long old = ((long) oldHighOrder << 32) | (oldLowOrder & 0xFFFFFFFFL);
        return (OtherSettings.getBackSteppingEnabled())
            ? Globals.program.getBackStepper().addMemoryRestoreDoubleWord(address, old)
            : old;
    }


    /**
     * Writes 64 bit double second starting at specified Memory address. Note that
     * high-order 32 bits are stored in higher (second) memory word regardless
     * of "endianness".
     *
     * @param address Starting address of Memory address to be set.
     * @param value   Value to be stored at that address.
     * @return old second that was replaced by setDouble operation.
     * @throws AddressErrorException if any.
     */
    public double setDouble(final int address, final double value) throws AddressErrorException {
        final long longValue = Double.doubleToLongBits(value);
        return Double.longBitsToDouble(this.setDoubleWord(address, longValue));
    }


    /**
     * Stores ProgramStatement in Text Segment.
     *
     * @param address   Starting address of Memory address to be set. Must be word
     *                  boundary.
     * @param statement Machine code to be stored starting at that address -- for
     *                  simulation
     *                  purposes, actually stores reference to ProgramStatement
     *                  instead of 32-bit machine code.
     * @throws AddressErrorException If address is not on word boundary or is
     *                               outside Text Segment.
     * @see ProgramStatement
     */
    public void setStatement(final int address, final ProgramStatement statement) throws AddressErrorException {
        Memory.checkStoreWordAligned(address);
        if (!Memory.inTextSegment(address)) {
            throw new AddressErrorException(
                "Store address to text segment out of range",
                ExceptionReason.STORE_ACCESS_FAULT, address);
        }
        if (Globals.debug)
            Memory.LOGGER.debug("memory[{}] set to {}", address, statement.getBinaryStatement());
        Memory.storeProgramStatement(address, statement, Memory.textBaseAddress, this.textBlockTable);
    }


    /**
     * Starting at the given word address, read the given number of bytes (max 4).
     * This one does not check for word boundaries, and copies one byte at a time.
     * If length == 1, puts second in low order byte. If 2, puts into low order
     * half-word.
     *
     * @param address Starting address of Memory address to be read.
     * @param length  Number of bytes to be read.
     * @return Value stored starting at that address.
     * @throws AddressErrorException if any.
     */
    public int get(final int address, final int length) throws AddressErrorException {
        return this.get(address, length, true);
    }

    /*
    ********************************
    THE UTILITIES
    ********************************
    */

    // Does the real work, but includes option to NOT notify observers.
    private int get(final int address, final int length, final boolean notify) throws AddressErrorException {
        final int value;
        final int relativeByteAddress;
        if (Memory.inDataSegment(address)) {
            // in data segment. Will read one byte at a time, w/o regard to boundaries.
            relativeByteAddress = address - Memory.dataSegmentBaseAddress; // relative to data segment start, in bytes
            value = this.fetchBytesFromTable(this.dataBlockTable, relativeByteAddress, length);
        } else if (address > Memory.stackLimitAddress && address <= Memory.stackBaseAddress) {
            // in stack. Similar to data, except relative address computed "backward"
            relativeByteAddress = Memory.stackBaseAddress - address;
            value = this.fetchBytesFromTable(this.stackBlockTable, relativeByteAddress, length);
        } else if (address >= Memory.memoryMapBaseAddress && address < Memory.memoryMapLimitAddress) {
            // memory mapped I/O.
            relativeByteAddress = address - Memory.memoryMapBaseAddress;
            value = this.fetchBytesFromTable(this.memoryMapBlockTable, relativeByteAddress, length);
        } else if (Memory.inTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with calls to getStatementNoNotify &
            // getBinaryStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            if (BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) {
                if (address % 4 + length > 4) {
                    // TODO: add checks for halfword load not aligned to halfword boundary
                    throw new AddressErrorException(
                        "Load address not aligned to word boundary ",
                        ExceptionReason.LOAD_ADDRESS_MISALIGNED, address);
                }
                final ProgramStatement stmt = this.getStatementNoNotify((address / 4) * 4);
                // TODO: maybe find a way to make the bit manipulation more clear
                // It just selects the right bytes from the word loaded
                value = stmt == null ? 0
                    : length == 4 ? stmt.getBinaryStatement()
                    : stmt.getBinaryStatement() >> (8 * (address % 4)) & ((1 << length * 8) - 1);
            } else {
                throw new AddressErrorException(
                    "Cannot read directly from text segment!",
                    ExceptionReason.LOAD_ACCESS_FAULT, address);
            }
        } else {
            // falls outside addressing range
            throw new AddressErrorException("address out of range ",
                ExceptionReason.LOAD_ACCESS_FAULT, address);
        }
        if (notify)
            this.notifyAnyObservers(AccessNotice.AccessType.READ, address, length, value);
        return value;
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int.
     * It transfers the 32 bit second "raw" as stored in memory, and does not adjust
     * for byte order (big or little endian). Address must be word-aligned.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte second) stored starting at that address.
     * @throws AddressErrorException If address is not on word boundary.
     */

    // Note: the logic here is repeated in getRawWordOrNull() below. Logic is
    // simplified by having this method just call getRawWordOrNull() then
    // return either the int of its return second, or 0 if it returns null.
    // Doing so would be detrimental to simulation runtime performance, so
    // I decided to keep the duplicate logic.
    public int getRawWord(final int address) throws AddressErrorException {
        final int value;
        final int relative;
        Memory.checkLoadWordAligned(address);
        if (Memory.inDataSegment(address)) {
            // in data segment
            relative = (address - Memory.dataSegmentBaseAddress) >> 2; // convert byte address to words
            value = this.fetchWordFromTable(this.dataBlockTable, relative);
        } else if (address > Memory.stackLimitAddress && address <= Memory.stackBaseAddress) {
            // in stack. Similar to data, except relative address computed "backward"
            relative = (Memory.stackBaseAddress - address) >> 2; // convert byte address to words
            value = this.fetchWordFromTable(this.stackBlockTable, relative);
        } else if (address >= Memory.memoryMapBaseAddress && address < Memory.memoryMapLimitAddress) {
            // memory mapped I/O.
            relative = (address - Memory.memoryMapBaseAddress) >> 2;
            value = this.fetchWordFromTable(this.memoryMapBlockTable, relative);
        } else if (Memory.inTextSegment(address)) {
            // Burch Mod (Jan 2013): replace throw with calls to getStatementNoNotify &
            // getBinaryStatement
            // DPS adaptation 5-Jul-2013: either throw or call, depending on setting
            if (BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)) {
                final ProgramStatement stmt = this.getStatementNoNotify(address);
                value = stmt == null ? 0 : stmt.getBinaryStatement();
            } else {
                throw new AddressErrorException(
                    "Cannot read directly from text segment!",
                    ExceptionReason.LOAD_ACCESS_FAULT, address);
            }
        } else {
            // falls outside addressing range
            throw new AddressErrorException("address out of range ",
                ExceptionReason.LOAD_ACCESS_FAULT, address);
        }
        this.notifyAnyObservers(AccessNotice.AccessType.READ, address, Memory.WORD_LENGTH_BYTES, value);
        return value;
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int and return
     * Integer.
     * It transfers the 32 bit second "raw" as stored in memory, and does not adjust
     * for byte order (big or little endian). Address must be word-aligned.
     * <p>
     * Returns null if reading from text segment and there is no instruction at the
     * requested address. Returns null if reading from data segment and this is the
     * first reference to the 4K memory allocation block (i.e., an array to
     * hold the memory has not been allocated).
     * <p>
     * This method was developed by Greg Giberling of UC Berkeley to support the
     * memory
     * dump feature that he implemented in Fall 2007.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte second) stored starting at that address as an Integer.
     * Conditions
     * that cause return second null are described above.
     * @throws AddressErrorException If address is not on word boundary.
     */

    // See note above, with getRawWord(), concerning duplicated logic.
    public @Nullable Integer getRawWordOrNull(final int address) throws AddressErrorException {
        Memory.checkLoadWordAligned(address);
        if (Memory.inDataSegment(address)) {
            // in data segment
            final var relativeAddress = (address - Memory.dataSegmentBaseAddress) >> 2; // convert byte address to words
            return this.fetchWordOrNullFromTable(this.dataBlockTable, relativeAddress);
        } else if (address > Memory.stackLimitAddress && address <= Memory.stackBaseAddress) {
            // in stack. Similar to data, except relative address computed "backward"
            final var relativeAddress = (Memory.stackBaseAddress - address) >> 2; // convert byte address to words
            return this.fetchWordOrNullFromTable(this.stackBlockTable, relativeAddress);
        } else if (Memory.inTextSegment(address)) {
            try {
                return (this.getStatementNoNotify(address) == null) ? null
                        : this.getStatementNoNotify(address).getBinaryStatement();
            } catch (final AddressErrorException aee) {
                return null;
            }
        } else {
            // falls outside addressing range
            throw new AddressErrorException("address out of range ", ExceptionReason.LOAD_ACCESS_FAULT, address);
        }
        // Do not notify observers. This read operation is initiated by the
        // dump feature, not the executing program.
    }

    /**
     * Look for first "null" memory second in an address range. For text segment
     * (binary code), this
     * represents a word that does not contain an instruction. Normally use this to
     * find the end of
     * the program. For data segment, this represents the first block of simulated
     * memory (block length
     * currently 4K words) that has not been referenced by an assembled/executing
     * program.
     *
     * @param baseAddress  lowest address to be searched; the starting point
     * @param limitAddress highest address to be searched
     * @return lowest address within specified range that contains "null" second as
     * described above.
     * @throws AddressErrorException if the base address is not on a word boundary
     */
    public int getAddressOfFirstNull(final int baseAddress, final int limitAddress) throws AddressErrorException {
        int address = baseAddress;
        for (; address < limitAddress; address += Memory.WORD_LENGTH_BYTES) {
            if (this.getRawWordOrNull(address) == null) {
                break;
            }
        }
        return address;
    }

    /**
     * Reads 64 bit doubleword second starting at specified Memory address.
     *
     * @param address Starting address of Memory address to be read
     * @return Double Word (8-byte second) stored starting at that address.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public long getDoubleWord(final int address) throws AddressErrorException {
        Memory.checkLoadWordAligned(address);
        final int oldHighOrder;
        final int oldLowOrder;
        oldHighOrder = this.get(address + 4, 4);
        oldLowOrder = this.get(address, 4);
        return ((long) oldHighOrder << 32) | (oldLowOrder & 0xFFFFFFFFL);
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int.
     * Does not use "get()"; we can do it faster here knowing we're working only
     * with full words.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte second) stored starting at that address.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int getWord(final int address) throws AddressErrorException {
        Memory.checkLoadWordAligned(address);
        return this.get(address, Memory.WORD_LENGTH_BYTES, true);
    }

    /**
     * Starting at the given word address, read a 4 byte word as an int.
     * Does not use "get()"; we can do it faster here knowing we're working only
     * with full words. Observers are NOT notified.
     *
     * @param address Starting address of word to be read.
     * @return Word (4-byte second) stored starting at that address.
     * @throws AddressErrorException If address is not on word boundary.
     */
    public int getWordNoNotify(final int address) throws AddressErrorException {
        Memory.checkLoadWordAligned(address);
        return this.get(address, Memory.WORD_LENGTH_BYTES, false);
    }

    // ALL THE OBSERVABLE STUFF GOES HERE. FOR COMPATIBILITY, Memory IS STILL
    // EXTENDING OBSERVABLE, BUT WILL NOT USE INHERITED METHODS. WILL INSTEAD
    // USE A COLLECTION OF MemoryObserver OBJECTS, EACH OF WHICH IS COMBINATION
    // OF AN OBSERVER WITH AN ADDRESS RANGE.

    /**
     * Starting at the given word address, read a 2 byte word into lower 16 bits of
     * int.
     *
     * @param address Starting address of word to be read.
     * @return Halfword (2-byte second) stored starting at that address, stored in
     * lower 16 bits.
     * @throws AddressErrorException If address is not on halfword boundary.
     */
    public int getHalf(final int address) throws AddressErrorException {
        if (address % 2 != 0) {
            throw new AddressErrorException("Load address not aligned on halfword boundary ",
                ExceptionReason.LOAD_ADDRESS_MISALIGNED, address);
        }
        return this.get(address, 2);
    }

    /**
     * Reads specified Memory byte into low order 8 bits of int.
     *
     * @param address Address of Memory byte to be read.
     * @return Value stored at that address. Only low order 8 bits used.
     * @throws AddressErrorException if any.
     */
    public int getByte(final int address) throws AddressErrorException {
        return this.get(address, 1);
    }

    /**
     * Gets ProgramStatement from Text Segment.
     *
     * @param address Starting address of Memory address to be read. Must be word
     *                boundary.
     * @return reference to ProgramStatement object associated with that address, or
     * null if none.
     * @throws AddressErrorException If address is not on word boundary or is
     *                               outside Text Segment.
     * @see ProgramStatement
     */
    public ProgramStatement getStatement(final int address) throws AddressErrorException {
        return this.getStatement(address, true);
    }

    /**
     * Gets ProgramStatement from Text Segment without notifying observers.
     *
     * @param address Starting address of Memory address to be read. Must be word
     *                boundary.
     * @return reference to ProgramStatement object associated with that address, or
     * null if none.
     * @throws AddressErrorException If address is not on word boundary or is
     *                               outside Text Segment.
     * @see ProgramStatement
     */
    public ProgramStatement getStatementNoNotify(final int address) throws AddressErrorException {
        return this.getStatement(address, false);
    }

    private ProgramStatement getStatement(final int address, final boolean notify) throws AddressErrorException {
        Memory.checkLoadWordAligned(address);
        if (!BOOL_SETTINGS.getSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED)
            && !Memory.inTextSegment(address)) {
            throw new AddressErrorException(
                "fetch address for text segment out of range ",
                ExceptionReason.LOAD_ACCESS_FAULT, address);
        }
        if (Memory.inTextSegment(address))
            return this.readProgramStatement(address, Memory.textBaseAddress, this.textBlockTable, notify);
        else
            return new ProgramStatement(this.get(address, Memory.WORD_LENGTH_BYTES), address);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Method to accept registration from observer for any memory address. Overrides
     * inherited method. Note to observers: this class delegates Observable
     * operations
     * so notices will come from the delegate, not the memory object.
     */
    @Override
    public void subscribe(final Flow.Subscriber<? super MemoryAccessNotice> obs) {
        try { // split so start address always >= end address
            this.subscribe(obs, 0, 0x7ffffffc);
            this.subscribe(obs, 0x80000000, 0xfffffffc);
        } catch (final AddressErrorException aee) {
            Memory.LOGGER.error("Internal error in Memory.addObserver.", aee);
        }
    }

    /**
     * Method to accept registration from observer for specific address. This
     * includes
     * the memory word starting at the given address. Note to observers: this class
     * delegates Observable operations
     * so notices will come from the delegate, not the memory object.
     *
     * @param obs  the observer
     * @param addr the memory address which must be on word boundary
     * @throws AddressErrorException if any.
     */
    public void subscribe(final Flow.Subscriber<? super MemoryAccessNotice> obs, final int addr) throws AddressErrorException {
        this.subscribe(obs, addr, addr);
    }

    /**
     * Method to accept registration from observer for specific address range. The
     * last byte included in the address range is the last byte of the word
     * specified
     * by the ending address. Note to observers: this class delegates Observable
     * operations
     * so notices will come from the delegate, not the memory object.
     *
     * @param obs       the observer
     * @param startAddr the low end of memory address range, must be on word
     *                  boundary
     * @param endAddr   the high end of memory address range, must be on word
     *                  boundary
     * @throws AddressErrorException if any.
     */
    public void subscribe(final Flow.Subscriber<? super MemoryAccessNotice> obs, final int startAddr,
                          final int endAddr) throws AddressErrorException {
        Memory.checkLoadWordAligned(startAddr);
        Memory.checkLoadWordAligned(endAddr);
        // upper half of address space (above 0x7fffffff) has sign bit 1 thus is seen as
        // negative.
        if (startAddr >= 0 && endAddr < 0) {
            throw new AddressErrorException("range cannot cross 0x8000000; please split it up",
                ExceptionReason.LOAD_ACCESS_FAULT, startAddr);
        }
        if (endAddr < startAddr) {
            throw new AddressErrorException("end address of range < start address of range ",
                ExceptionReason.LOAD_ACCESS_FAULT, startAddr);
        }
        this.observables.add(new MemoryObservable(obs, startAddr, endAddr));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Remove specified memory observers
     */
    public void deleteSubscriber(final @NotNull SimpleSubscriber<? super MemoryAccessNotice> obs) {
        for (final MemoryObservable o : this.observables) {
            o.deleteSubscriber(obs);
        }
    }

    /*********************************
     * THE HELPERS
     *************************************/

    /// Method to notify any observers of memory operation that has just occurred.
    /// The `|| Globals.getGui()==null` is a hack added 19 July 2012 DPS. IF
    /// simulation
    /// is from command mode, Globals.program is null but still want ability to
    /// observe.
    private void notifyAnyObservers(final @NotNull AccessNotice.AccessType type, final int address, final int length,
                                    final int value) {
        if ((Globals.program != null || Globals.gui == null)) {
            this.observables.stream()
                .filter((mo) -> mo.match(address))
                .forEach((mo) -> mo.submit(new MemoryAccessNotice(type, address, length, value))
                );
        }
    }

    // Helper method to fetch 1, 2 or 4 byte second from table that represents
    // memory. Originally used just for data segment, but now also used for stack.
    // Both use different tables but same storage method and same table size
    // and block size.
    private int storeBytesInTable(final int[][] blockTable,
                                  final int relativeByteAddress, final int length, final int value) {
        return this.storeOrFetchBytesInTable(blockTable, relativeByteAddress, length, value, Memory.STORE);
    }

    private int fetchBytesFromTable(final int[][] blockTable, final int relativeByteAddress, final int length) {
        return this.storeOrFetchBytesInTable(blockTable, relativeByteAddress, length, 0, Memory.FETCH);
    }

    // Helper method to store 4 byte second in table that represents memory.
    // Originally used just for data segment, but now also used for stack.
    // Both use different tables but same storage method and same table size
    // and block size. Assumes address is word aligned, no endian processing.
    // Modified 29 Dec 2005 to return overwritten second.

    // The helper's helper. Works for either storing or fetching, little or big
    //////////////////////////////////////////////////////////////////////////////// endian.
    // When storing/fetching bytes, most of the work is calculating the correct
    //////////////////////////////////////////////////////////////////////////////// array
    //////////////////////////////////////////////////////////////////////////////// element(s)
    // and element byte(s). This method performs either store or fetch, as directed

    /// ///////////////////////////////////////////////////////////////////////////// by
    /// ///////////////////////////////////////////////////////////////////////////// its
    // client using STORE or FETCH in last arg.
    // Modified 29 Dec 2005 to return old second of replaced bytes, for STORE.
    private synchronized int storeOrFetchBytesInTable(final int[][] blockTable,
                                                      int relativeByteAddress, final int length, int value,
                                                      final boolean op) {
        int relativeWordAddress, block, offset, bytePositionInMemory, bytePositionInValue;
        int oldValue = 0; // for STORE, return old values of replaced bytes
        final int loopStopper = 3 - length;
        // IF added DPS 22-Dec-2008. NOTE: has NOT been tested with Big-Endian.
        // Fix provided by Saul Spatz; comments that follow are his.
        // If address in stack segment is 4k + m, with 0 < m < 4, then the
        // relativeByteAddress we want is stackBaseAddress - 4k + m, but the
        // address actually passed in is stackBaseAddress - (4k + m), so we
        // need to add 2m. Because of the change in sign, we get the
        // expression 4-delta below in place of m.
        if (blockTable == this.stackBlockTable) {
            final int delta = relativeByteAddress % 4;
            if (delta != 0) {
                relativeByteAddress += (4 - delta) << 1;
            }
        }
        for (bytePositionInValue = 3; bytePositionInValue > loopStopper; bytePositionInValue--) {
            bytePositionInMemory = relativeByteAddress % 4;
            relativeWordAddress = relativeByteAddress >> 2;
            block = relativeWordAddress / Memory.BLOCK_LENGTH_WORDS; // Block number
            offset = relativeWordAddress % Memory.BLOCK_LENGTH_WORDS; // Word within that block
            if (blockTable[block] == null) {
                if (op == Memory.STORE)
                    blockTable[block] = new int[Memory.BLOCK_LENGTH_WORDS];
                else
                    return 0;
            }
            if (Memory.byteOrder == Memory.LITTLE_ENDIAN)
                bytePositionInMemory = 3 - bytePositionInMemory;
            if (op == Memory.STORE) {
                oldValue = Memory.replaceByte(blockTable[block][offset], bytePositionInMemory,
                    oldValue, bytePositionInValue);
                blockTable[block][offset] = Memory.replaceByte(value, bytePositionInValue,
                    blockTable[block][offset], bytePositionInMemory);
            } else {// op == FETCH
                value = Memory.replaceByte(blockTable[block][offset], bytePositionInMemory,
                    value, bytePositionInValue);
            }
            relativeByteAddress++;
        }
        return (op == Memory.STORE) ? oldValue : value;
    }

    private synchronized int storeWordInTable(final int[][] blockTable, final int relative, final int value) {
        final int block;
        final int offset;
        final int oldValue;
        block = relative / Memory.BLOCK_LENGTH_WORDS;
        offset = relative % Memory.BLOCK_LENGTH_WORDS;
        if (blockTable[block] == null) {
            // First time writing to this block, so allocate the space.
            blockTable[block] = new int[Memory.BLOCK_LENGTH_WORDS];
        }
        oldValue = blockTable[block][offset];
        blockTable[block][offset] = value;
        return oldValue;
    }

    // Same as above, but doesn't set, just gets
    private synchronized int fetchWordFromTable(final int[][] blockTable, final int relative) {
        final int value;
        final int block;
        final int offset;
        block = relative / Memory.BLOCK_LENGTH_WORDS;
        offset = relative % Memory.BLOCK_LENGTH_WORDS;
        if (blockTable[block] == null) {
            // first reference to an address in this block. Assume initialized to 0.
            value = 0;
        } else {
            value = blockTable[block][offset];
        }
        return value;
    }

    // Same as above, but if it hasn't been allocated returns null.
    // Developed by Greg Gibeling of UC Berkeley, fall 2007.
    @Contract(pure = true)
    private synchronized @Nullable Integer fetchWordOrNullFromTable(final int[][] blockTable,
                                                                    final int relative) {
        final var block = relative / Memory.BLOCK_LENGTH_WORDS;
        final var offset = relative % Memory.BLOCK_LENGTH_WORDS;
        if (blockTable[block] == null) {
            // first reference to an address in this block. Assume initialized to 0.
            return null;
        } else {
            return blockTable[block][offset];
        }
    }

    /**
     * Read a program statement from the given address. Address has already been
     * verified
     * as valid.
     *
     * @param address     the address to read from
     * @param baseAddress the base address for the section being read from (.text)
     * @param blockTable  the internal table of program statements
     * @param notify      whether or not it notifies observers
     * @return associated ProgramStatement or null if none.
     */
    private @Nullable ProgramStatement readProgramStatement(final int address, final int baseAddress,
                                                            final ProgramStatement[][] blockTable,
                                                            final boolean notify) {
        final int relative = (address - baseAddress) >> 2; // convert byte address to words
        final int block = relative / Memory.TEXT_BLOCK_LENGTH_WORDS;
        final int offset = relative % Memory.TEXT_BLOCK_LENGTH_WORDS;
        if (block < Memory.TEXT_BLOCK_TABLE_LENGTH) {
            if (blockTable[block] == null || blockTable[block][offset] == null) {
                // No instructions are stored in this block or offset.
                if (notify)
                    this.notifyAnyObservers(AccessNotice.AccessType.READ, address,
                        BasicInstruction.BASIC_INSTRUCTION_LENGTH, 0);
                return null;
            } else {
                if (notify)
                    this.notifyAnyObservers(AccessNotice.AccessType.READ, address,
                        BasicInstruction.BASIC_INSTRUCTION_LENGTH,
                        blockTable[block][offset].getBinaryStatement());
                return blockTable[block][offset];
            }
        }
        if (notify)
            this.notifyAnyObservers(AccessNotice.AccessType.READ, address, BasicInstruction.BASIC_INSTRUCTION_LENGTH,
                0);
        return null;
    }

    // Private class whose objects will represent an observable-observer pair
    // for a given memory address or range.
    private static class MemoryObservable extends CustomPublisher<MemoryAccessNotice> implements Comparable<MemoryObservable> {
        private final int lowAddress;
        private final int highAddress;

        public MemoryObservable(final Flow.Subscriber<? super MemoryAccessNotice> subscriber, final int startAddr,
                                final int endAddr) {
            this.lowAddress = startAddr;
            this.highAddress = endAddr;
            this.subscribe(subscriber);
        }

        public boolean match(final int address) {
            return (address >= this.lowAddress && address <= this.highAddress - 1 + Memory.WORD_LENGTH_BYTES);
        }

        // Useful to have for future refactoring, if it actually becomes worthwhile to
        // sort
        // these or put 'em in a tree (rather than sequential search through list).
        @Override
        public int compareTo(final MemoryObservable mo) {
            if (this.lowAddress < mo.lowAddress
                || this.lowAddress == mo.lowAddress && this.highAddress < mo.highAddress) {
                return -1;
            }
            if (this.lowAddress > mo.lowAddress || this.highAddress > mo.highAddress) {
                return 1;
            }
            return 0; // they have to be equal at this point.
        }
    }

}
