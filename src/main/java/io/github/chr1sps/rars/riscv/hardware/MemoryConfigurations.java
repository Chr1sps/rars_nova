package io.github.chr1sps.rars.riscv.hardware;

import io.github.chr1sps.rars.Globals;

import java.util.ArrayList;
import java.util.Iterator;

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
 * Models the collection of MIPS memory configurations.
 * The default configuration is based on SPIM. Starting with MARS 3.7,
 * the configuration can be changed.
 *
 * @author Pete Sanderson
 * @version August 2009
 */
public class MemoryConfigurations {

    private static ArrayList<MemoryConfiguration> configurations = null;
    private static MemoryConfiguration defaultConfiguration;
    private static MemoryConfiguration currentConfiguration;

    // Be careful, these arrays are parallel and position-sensitive.
    // The getters in this and in MemoryConfiguration depend on this
    // sequence. Should be refactored... The order comes from the
    // original listed order in Memory.java, where most of these were
    // "final" until Mars 3.7 and changeable memory configurations.
    private static final String[] configurationItemNames = {
            ".text base address",
            "data segment base address",
            ".extern base address",
            "global pointer (gp)",
            ".data base address",
            "heap base address",
            "stack pointer (sp)",
            "stack base address",
            "user space high address",
            "kernel space base address",
            "MMIO base address",
            "kernel space high address",
            "data segment limit address",
            "text limit address",
            "stack limit address",
            "memory map limit address"
    };

    // Default configuration comes from SPIM
    private static final int[] defaultConfigurationItemValues = {
            0x00400000, // .text Base Address
            0x10000000, // Data Segment base address
            0x10000000, // .extern Base Address
            0x10008000, // Global Pointer $gp)
            0x10010000, // .data base Address
            0x10040000, // heap base address
            0x7fffeffc, // stack pointer $sp (from SPIM not MIPS)
            0x7ffffffc, // stack base address
            0x7fffffff, // highest address in user space
            0x80000000, // lowest address in kernel space
            0xffff0000, // MMIO base address
            0xffffffff, // highest address in kernel (and memory)
            0x7fffffff, // data segment limit address
            0x0ffffffc, // text limit address
            0x10040000, // stack limit address
            0xffffffff // memory map limit address
    };

    // Compact allows 16 bit addressing, data segment starts at 0
    private static final int[] dataBasedCompactConfigurationItemValues = {
            0x00003000, // .text Base Address
            0x00000000, // Data Segment base address
            0x00001000, // .extern Base Address
            0x00001800, // Global Pointer $gp)
            0x00000000, // .data base Address
            0x00002000, // heap base address
            0x00002ffc, // stack pointer $sp
            0x00002ffc, // stack base address
            0x00003fff, // highest address in user space
            0x00004000, // lowest address in kernel space
            0x00007f00, // MMIO base address
            0x00007fff, // highest address in kernel (and memory)
            0x00002fff, // data segment limit address
            0x00003ffc, // text limit address
            0x00002000, // stack limit address
            0x00007fff // memory map limit address
    };

    // Compact allows 16 bit addressing, text segment starts at 0
    private static final int[] textBasedCompactConfigurationItemValues = {
            0x00000000, // .text Base Address
            0x00001000, // Data Segment base address
            0x00001000, // .extern Base Address
            0x00001800, // Global Pointer $gp)
            0x00002000, // .data base Address
            0x00003000, // heap base address
            0x00003ffc, // stack pointer $sp
            0x00003ffc, // stack base address
            0x00003fff, // highest address in user space
            0x00004000, // lowest address in kernel space
            0x00007f00, // MMIO base address
            0x00007fff, // highest address in kernel (and memory)
            0x00003fff, // data segment limit address
            0x00000ffc, // text limit address
            0x00003000, // stack limit address
            0x00007fff // memory map limit address
    };

    /**
     * <p>Constructor for MemoryConfigurations.</p>
     */
    public MemoryConfigurations() {

    }

    /**
     * <p>buildConfigurationCollection.</p>
     */
    public static void buildConfigurationCollection() {
        if (configurations == null) {
            configurations = new ArrayList<>();
            configurations.add(new MemoryConfiguration("Default", "Default", configurationItemNames,
                    defaultConfigurationItemValues));
            configurations.add(new MemoryConfiguration("CompactDataAtZero", "Compact, Data at Address 0",
                    configurationItemNames, dataBasedCompactConfigurationItemValues));
            configurations.add(new MemoryConfiguration("CompactTextAtZero", "Compact, Text at Address 0",
                    configurationItemNames, textBasedCompactConfigurationItemValues));
            defaultConfiguration = configurations.get(0);
            currentConfiguration = defaultConfiguration;
            // Get current config from settings
            // String currentConfigurationIdentifier =
            // Globals.getSettings().getMemoryConfiguration();
            setCurrentConfiguration(getConfigurationByName(Globals.getSettings().getMemoryConfiguration()));
            // Iterator configurationsIterator = getConfigurationsIterator();
            // while (configurationsIterator.hasNext()) {
            // MemoryConfiguration config =
            // (MemoryConfiguration)configurationsIterator.next();
            // if
            // (currentConfigurationIdentifier.equals(config.getConfigurationIdentifier()))
            // {
            // setCurrentConfiguration(config);
            // }
            // }
        }
    }

    /**
     * <p>getConfigurationsIterator.</p>
     *
     * @return a {@link java.util.Iterator} object
     */
    public static Iterator<MemoryConfiguration> getConfigurationsIterator() {
        if (configurations == null) {
            buildConfigurationCollection();
        }
        return configurations.iterator();

    }

    /**
     * <p>getConfigurationByName.</p>
     *
     * @param name a {@link java.lang.String} object
     * @return a {@link io.github.chr1sps.rars.riscv.hardware.MemoryConfiguration} object
     */
    public static MemoryConfiguration getConfigurationByName(String name) {
        Iterator<MemoryConfiguration> configurationsIterator = getConfigurationsIterator();
        while (configurationsIterator.hasNext()) {
            MemoryConfiguration config = configurationsIterator.next();
            if (name.equals(config.getConfigurationIdentifier())) {
                return config;
            }
        }
        return null;
    }

    /**
     * <p>Getter for the field <code>defaultConfiguration</code>.</p>
     *
     * @return a {@link io.github.chr1sps.rars.riscv.hardware.MemoryConfiguration} object
     */
    public static MemoryConfiguration getDefaultConfiguration() {
        if (defaultConfiguration == null) {
            buildConfigurationCollection();
        }
        return defaultConfiguration;
    }

    /**
     * <p>Getter for the field <code>currentConfiguration</code>.</p>
     *
     * @return a {@link io.github.chr1sps.rars.riscv.hardware.MemoryConfiguration} object
     */
    public static MemoryConfiguration getCurrentConfiguration() {
        if (currentConfiguration == null) {
            buildConfigurationCollection();
        }
        return currentConfiguration;
    }

    /**
     * <p>Setter for the field <code>currentConfiguration</code>.</p>
     *
     * @param config a {@link io.github.chr1sps.rars.riscv.hardware.MemoryConfiguration} object
     * @return a boolean
     */
    public static boolean setCurrentConfiguration(MemoryConfiguration config) {
        if (config == null)
            return false;
        if (config != currentConfiguration) {
            currentConfiguration = config;
            Globals.memory.clear();
            RegisterFile.getRegister("gp").changeResetValue(config.getGlobalPointer());
            RegisterFile.getRegister("sp").changeResetValue(config.getStackPointer());
            RegisterFile.getProgramCounterRegister().changeResetValue(config.getTextBaseAddress());
            RegisterFile.initializeProgramCounter(config.getTextBaseAddress());
            RegisterFile.resetRegisters();
            return true;
        } else {
            return false;
        }
    }

    //// Use these to intialize Memory static variables at launch

    /**
     * <p>getDefaultTextBaseAddress.</p>
     *
     * @return a int
     */
    public static int getDefaultTextBaseAddress() {
        return defaultConfigurationItemValues[0];
    }

    /**
     * <p>getDefaultDataSegmentBaseAddress.</p>
     *
     * @return a int
     */
    public static int getDefaultDataSegmentBaseAddress() {
        return defaultConfigurationItemValues[1];
    }

    /**
     * <p>getDefaultExternBaseAddress.</p>
     *
     * @return a int
     */
    public static int getDefaultExternBaseAddress() {
        return defaultConfigurationItemValues[2];
    }

    /**
     * <p>getDefaultGlobalPointer.</p>
     *
     * @return a int
     */
    public static int getDefaultGlobalPointer() {
        return defaultConfigurationItemValues[3];
    }

    /**
     * <p>getDefaultDataBaseAddress.</p>
     *
     * @return a int
     */
    public static int getDefaultDataBaseAddress() {
        return defaultConfigurationItemValues[4];
    }

    /**
     * <p>getDefaultHeapBaseAddress.</p>
     *
     * @return a int
     */
    public static int getDefaultHeapBaseAddress() {
        return defaultConfigurationItemValues[5];
    }

    /**
     * <p>getDefaultStackPointer.</p>
     *
     * @return a int
     */
    public static int getDefaultStackPointer() {
        return defaultConfigurationItemValues[6];
    }

    /**
     * <p>getDefaultStackBaseAddress.</p>
     *
     * @return a int
     */
    public static int getDefaultStackBaseAddress() {
        return defaultConfigurationItemValues[7];
    }

    /**
     * <p>getDefaultUserHighAddress.</p>
     *
     * @return a int
     */
    public static int getDefaultUserHighAddress() {
        return defaultConfigurationItemValues[8];
    }

    /**
     * <p>getDefaultKernelBaseAddress.</p>
     *
     * @return a int
     */
    public static int getDefaultKernelBaseAddress() {
        return defaultConfigurationItemValues[9];
    }

    /**
     * <p>getDefaultMemoryMapBaseAddress.</p>
     *
     * @return a int
     */
    public static int getDefaultMemoryMapBaseAddress() {
        return defaultConfigurationItemValues[10];
    }

    /**
     * <p>getDefaultKernelHighAddress.</p>
     *
     * @return a int
     */
    public static int getDefaultKernelHighAddress() {
        return defaultConfigurationItemValues[11];
    }

    /**
     * <p>getDefaultDataSegmentLimitAddress.</p>
     *
     * @return a int
     */
    public int getDefaultDataSegmentLimitAddress() {
        return defaultConfigurationItemValues[12];
    }

    /**
     * <p>getDefaultTextLimitAddress.</p>
     *
     * @return a int
     */
    public int getDefaultTextLimitAddress() {
        return defaultConfigurationItemValues[13];
    }

    /**
     * <p>getDefaultStackLimitAddress.</p>
     *
     * @return a int
     */
    public int getDefaultStackLimitAddress() {
        return defaultConfigurationItemValues[14];
    }

    /**
     * <p>getMemoryMapLimitAddress.</p>
     *
     * @return a int
     */
    public int getMemoryMapLimitAddress() {
        return defaultConfigurationItemValues[15];
    }

}