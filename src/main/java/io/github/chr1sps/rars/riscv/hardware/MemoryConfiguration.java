package io.github.chr1sps.rars.riscv.hardware;

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
 * Models the memory configuration for the simulated MIPS machine.
 * "configuration" refers to the starting memory addresses for
 * the various memory segments.
 * The default configuration is based on SPIM. Starting with MARS 3.7,
 * the configuration can be changed.
 *
 * @author Pete Sanderson
 * @version August 2009
 */
public class MemoryConfiguration {
    // TODO: remove kernel mode maybe?
    // TODO: move away from a multi-array approach to array of ranges approach
    // Identifier is used for saving setting; name is used for display
    private final String configurationIdentifier;
    private final String configurationName;
    private final String[] configurationItemNames;
    private final int[] configurationItemValues;

    /**
     * <p>Constructor for MemoryConfiguration.</p>
     *
     * @param ident  a {@link java.lang.String} object
     * @param name   a {@link java.lang.String} object
     * @param items  an array of {@link java.lang.String} objects
     * @param values an array of {@link int} objects
     */
    public MemoryConfiguration(String ident, String name, String[] items, int[] values) {
        this.configurationIdentifier = ident;
        this.configurationName = name;
        this.configurationItemNames = items;
        this.configurationItemValues = values;
    }

    /**
     * <p>Getter for the field <code>configurationIdentifier</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getConfigurationIdentifier() {
        return configurationIdentifier;
    }

    /**
     * <p>Getter for the field <code>configurationName</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getConfigurationName() {
        return configurationName;
    }

    /**
     * <p>Getter for the field <code>configurationItemValues</code>.</p>
     *
     * @return an array of {@link int} objects
     */
    public int[] getConfigurationItemValues() {
        return configurationItemValues;
    }

    /**
     * <p>Getter for the field <code>configurationItemNames</code>.</p>
     *
     * @return an array of {@link java.lang.String} objects
     */
    public String[] getConfigurationItemNames() {
        return configurationItemNames;
    }

    /**
     * <p>getTextBaseAddress.</p>
     *
     * @return a int
     */
    public int getTextBaseAddress() {
        return configurationItemValues[0];
    }

    /**
     * <p>getDataSegmentBaseAddress.</p>
     *
     * @return a int
     */
    public int getDataSegmentBaseAddress() {
        return configurationItemValues[1];
    }

    /**
     * <p>getExternBaseAddress.</p>
     *
     * @return a int
     */
    public int getExternBaseAddress() {
        return configurationItemValues[2];
    }

    /**
     * <p>getGlobalPointer.</p>
     *
     * @return a int
     */
    public int getGlobalPointer() {
        return configurationItemValues[3];
    }

    /**
     * <p>getDataBaseAddress.</p>
     *
     * @return a int
     */
    public int getDataBaseAddress() {
        return configurationItemValues[4];
    }

    /**
     * <p>getHeapBaseAddress.</p>
     *
     * @return a int
     */
    public int getHeapBaseAddress() {
        return configurationItemValues[5];
    }

    /**
     * <p>getStackPointer.</p>
     *
     * @return a int
     */
    public int getStackPointer() {
        return configurationItemValues[6];
    }

    /**
     * <p>getStackBaseAddress.</p>
     *
     * @return a int
     */
    public int getStackBaseAddress() {
        return configurationItemValues[7];
    }

    /**
     * <p>getUserHighAddress.</p>
     *
     * @return a int
     */
    public int getUserHighAddress() {
        return configurationItemValues[8];
    }

    /**
     * <p>getKernelBaseAddress.</p>
     *
     * @return a int
     */
    public int getKernelBaseAddress() {
        return configurationItemValues[9];
    }

    /**
     * <p>getMemoryMapBaseAddress.</p>
     *
     * @return a int
     */
    public int getMemoryMapBaseAddress() {
        return configurationItemValues[10];
    }

    /**
     * <p>getKernelHighAddress.</p>
     *
     * @return a int
     */
    public int getKernelHighAddress() {
        return configurationItemValues[11];
    }

    /**
     * <p>getDataSegmentLimitAddress.</p>
     *
     * @return a int
     */
    public int getDataSegmentLimitAddress() {
        return configurationItemValues[12];
    }

    /**
     * <p>getTextLimitAddress.</p>
     *
     * @return a int
     */
    public int getTextLimitAddress() {
        return configurationItemValues[13];
    }

    /**
     * <p>getStackLimitAddress.</p>
     *
     * @return a int
     */
    public int getStackLimitAddress() {
        return configurationItemValues[14];
    }

    /**
     * <p>getMemoryMapLimitAddress.</p>
     *
     * @return a int
     */
    public int getMemoryMapLimitAddress() {
        return configurationItemValues[15];
    }

}
