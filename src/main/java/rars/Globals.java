package rars;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rars.assembler.SymbolTable;
import rars.riscv.InstructionSet;
import rars.riscv.SyscallNumberOverride;
import rars.riscv.hardware.Memory;
import rars.util.PropertiesFile;
import rars.venus.VenusUI;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * Collection of globally-available data structures.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public final class Globals {
    /**
     * Lock variable used at head of synchronized block to guard memory and
     * registers
     **/
    public static final ReentrantLock memoryAndRegistersLock = new ReentrantLock();
    /**
     * String to GUI's RunI/O text area when echoing user input from pop-up dialog.
     */
    public static final String userInputAlert = "**** user input : ";
    /**
     * Path to folder that contains images
     */
    // The leading "/" in filepath prevents package name from being pre-pended.
    public static final String imagesPath = "/images/";
    /**
     * Path to folder that contains help text
     */
    public static final String helpPath = "/help/";
    /**
     * The current version number. Can't wait for "initialize()" call to get it.
     */
    public static final String version = "1.6";
    /**
     * Copyright years
     */
    public static final String copyrightYears = Globals.getCopyrightYears();
    /**
     * Copyright holders
     */
    public static final String copyrightHolders = Globals.getCopyrightHolders();
    private static final Logger LOGGER = LogManager.getLogger();
    // List these first because they are referenced by methods called at
    // initialization.
    private static final String configPropertiesFile = "Config";
    /**
     * List of accepted file extensions for RISCV assembly source files.
     */
    public static final ArrayList<String> fileExtensions = Globals.getFileExtensions();
    /**
     * Maximum length of scrolled message window (RARS Messages and Run I/O)
     */
    public static final int maximumMessageCharacters = Globals.getMessageLimit();
    /**
     * Maximum number of assembler errors produced by one assemble operation
     */
    public static final int maximumErrorMessages = Globals.getErrorLimit();
    /**
     * Maximum number of back-step operations to buffer
     */
    public static final int maximumBacksteps = Globals.getBackstepLimit();
    /**
     * Placeholder for non-printable ASCII codes
     */
    public static final String ASCII_NON_PRINT = Globals.getAsciiNonPrint();
    /**
     * Array of strings to display for ASCII codes in ASCII display of data segment.
     * ASCII code 0-255 is array index.
     */
    public static final String[] ASCII_TABLE = Globals.getAsciiStrings();
    private static final String syscallPropertiesFile = "Syscall";
    /**
     * The set of implemented instructions.
     **/
    public static InstructionSet instructionSet;
    /**
     * the program currently being worked with. Used by GUI only, not command line.
     **/
    public static RISCVprogram program;
    /**
     * Symbol table for file currently being assembled.
     **/
    public static SymbolTable symbolTable;
    /**
     * Simulated memory component.
     **/
    public static Memory memory;
    /**
     * Flag to determine whether or not to produce internal debugging information.
     **/
    public static boolean debug = false;
    /**
     * Exit code -- useful with SYSCALL 17 when running from command line (not GUI)
     */
    public static int exitCode = 0;
    /**
     * Constant <code>runSpeedPanelExists=false</code>
     */
    public static boolean runSpeedPanelExists = false;
    /**
     * Object that contains various settings that can be accessed modified
     * internally.
     **/
    static Settings settings;
    /* The GUI being used (if any) with this simulator. */
    static VenusUI gui = null;
    /* Flag that indicates whether or not instructionSet has been initialized. */
    private static boolean initialized = false;

    private Globals() {
    }

    private static String getCopyrightYears() {
        return "2003-2019";
    }

    private static String getCopyrightHolders() {
        return "Pete Sanderson and Kenneth Vollmar";
    }

    /**
     * <p>Getter for the field <code>gui</code>.</p>
     *
     * @return a {@link VenusUI} object
     */
    public static VenusUI getGui() {
        return Globals.gui;
    }

    /**
     * <p>Setter for the field <code>gui</code>.</p>
     *
     * @param g a {@link VenusUI} object
     */
    public static void setGui(final VenusUI g) {
        Globals.gui = g;
    }

    /**
     * <p>Getter for the field <code>settings</code>.</p>
     *
     * @return a {@link Settings} object
     */
    public static Settings getSettings() {
        return Globals.settings;
    }

    /**
     * Method called once upon system initialization to create the global data
     * structures.
     */
    public static void initialize() {
        if (!Globals.initialized) {
            Globals.memory = Memory.getInstance(); // clients can use Memory.getInstance instead of Globals.memory
            Globals.symbolTable = new SymbolTable("global");
            Globals.settings = new Settings();
            Globals.instructionSet = new InstructionSet();
            Globals.instructionSet.populate();
            Globals.initialized = true;
            Globals.debug = false;
            Globals.memory.clear(); // will establish memory configuration from setting
        }
    }

    // Read byte limit of Run I/O or RARS Messages text to buffer.
    private static int getMessageLimit() {
        return Globals.getIntegerProperty(Globals.configPropertiesFile, "MessageLimit", 1000000);
    }

    // Read limit on number of error messages produced by one assemble operation.
    private static int getErrorLimit() {
        return Globals.getIntegerProperty(Globals.configPropertiesFile, "ErrorLimit", 200);
    }

    // Read backstep limit (number of operations to buffer) from properties file.
    private static int getBackstepLimit() {
        return Globals.getIntegerProperty(Globals.configPropertiesFile, "BackstepLimit", 1000);
    }

    // Read ASCII default display character for non-printing characters, from
    // properties file.

    /**
     * <p>getAsciiNonPrint.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public static String getAsciiNonPrint() {
        final String anp = Globals.getPropertyEntry(Globals.configPropertiesFile, "AsciiNonPrint");
        return (anp == null) ? "." : ((anp.equals("space")) ? " " : anp);
    }

    // Read ASCII strings for codes 0-255, from properties file. If string
    // second is "null", substitute second of ASCII_NON_PRINT. If string is
    // "space", substitute string containing one space character.

    /**
     * <p>getAsciiStrings.</p>
     *
     * @return an array of {@link java.lang.String} objects
     */
    public static String[] getAsciiStrings() {
        final String let = Globals.getPropertyEntry(Globals.configPropertiesFile, "AsciiTable");
        final String placeHolder = Globals.getAsciiNonPrint();
        if (let == null) {
            // If config isn't loaded, give a decent default second.
            final String[] table = new String[((int) '~') + 1];
            for (int i = 0; i < table.length; i++) {
                if (i == 0)
                    table[i] = "\0";
                else if (i == '\n')
                    table[i] = "\n";
                else if (i < ' ')
                    table[i] = placeHolder;
                else
                    table[i] = " " + (char) i;
            }
            return table;
        } else {
            final String[] lets = let.split(" +");
            int maxLength = 0;
            for (int i = 0; i < lets.length; i++) {
                if (lets[i].equals("null"))
                    lets[i] = placeHolder;
                if (lets[i].equals("space"))
                    lets[i] = " ";
                if (lets[i].length() > maxLength)
                    maxLength = lets[i].length();
            }
            final String padding = "        ";
            maxLength++;
            for (int i = 0; i < lets.length; i++) {
                lets[i] = padding.substring(0, maxLength - lets[i].length()) + lets[i];
            }
            return lets;
        }
    }

    // Read and return integer property second for given file and property name.
    // Default second is returned if property file or name not found.
    private static int getIntegerProperty(final String propertiesFile, final String propertyName, final int defaultValue) {
        int limit = defaultValue; // just in case no entry is found
        final Properties properties = PropertiesFile.loadPropertiesFromFile(propertiesFile);
        try {
            limit = Integer.parseInt(properties.getProperty(propertyName, Integer.toString(defaultValue)));
        } catch (final NumberFormatException ignored) {
        } // do nothing, I already have a default
        return limit;
    }

    // Read assembly language file extensions from properties file. Resulting
    // string is tokenized into array list (assume StringTokenizer default
    // delimiters).
    private static ArrayList<String> getFileExtensions() {
        final ArrayList<String> extensionsList = new ArrayList<>();
        final String extensions = Globals.getPropertyEntry(Globals.configPropertiesFile, "Extensions");
        if (extensions != null) {
            final StringTokenizer st = new StringTokenizer(extensions);
            while (st.hasMoreTokens()) {
                extensionsList.add(st.nextToken());
            }
        }
        return extensionsList;
    }

    /**
     * Read and return property file second (if any) for requested property.
     *
     * @param propertiesFile name of properties file (do NOT include filename
     *                       extension,
     *                       which is assumed to be ".properties")
     * @param propertyName   String containing desired property name
     * @return String containing associated second; null if property not found
     */
    public static String getPropertyEntry(final String propertiesFile, final String propertyName) {
        return PropertiesFile.loadPropertiesFromFile(propertiesFile).getProperty(propertyName);
    }

    /**
     * Read any syscall number assignment overrides from config file.
     *
     * @return ArrayList of SyscallNumberOverride objects
     */
    public static ArrayList<SyscallNumberOverride> getSyscallOverrides() {
        final ArrayList<SyscallNumberOverride> overrides = new ArrayList<>();
        final Properties properties = PropertiesFile.loadPropertiesFromFile(Globals.syscallPropertiesFile);
        final Enumeration<Object> keys = properties.keys();
        while (keys.hasMoreElements()) {
            final String key = (String) keys.nextElement();
            final String property = properties.getProperty(key).trim();
            try {
                final int value = Integer.parseInt(property);
                overrides.add(new SyscallNumberOverride(key, value));
            } catch (final NumberFormatException e) {
                LOGGER.fatal("Error processing Syscall number override: '{}' is not a valid integer", property);
                System.exit(0);
            }
        }
        return overrides;
    }

}
