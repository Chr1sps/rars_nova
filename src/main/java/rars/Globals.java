package rars;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.assembler.SymbolTable;
import rars.riscv.SyscallNumberOverride;
import rars.riscv.hardware.Memory;
import rars.util.PropertiesFile;
import rars.venus.VenusUI;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static rars.settings.OtherSettings.OTHER_SETTINGS;

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
    /// Lock variable used at head of synchronized block to guard memory and registers
    public static final @NotNull ReentrantLock memoryAndRegistersLock = new ReentrantLock();
    /// String to GUI's RunI/O text area when echoing user input from pop-up dialog.
    public static final @NotNull String userInputAlert = "**** user input : ";
    /// Path to folder that contains images.
    public static final @NotNull String imagesPath = "/images/";

    // The leading "/" in filepath prevents package name from being pre-pended.
    /// Path to folder that contains help text
    public static final @NotNull String helpPath = "/help/";
    /// The current version number.
    public static final @NotNull String version = "1.6";
    /// Copyright years
    public static final @NotNull String copyrightYears = "2003-2019";
    /// Copyright holders
    public static final @NotNull String copyrightHolders = "Pete Sanderson and Kenneth Vollmar";
    /// Symbol table for file currently being assembled.
    public static final @NotNull SymbolTable symbolTable = new SymbolTable("global");
    /// List of accepted file extensions for RISCV assembly source files.
    public static final @NotNull List<@NotNull String> fileExtensions = List.of("asm", "s", "S");
    /// Maximum length of scrolled message window (RARS Messages and Run I/O)
    public static final int maximumMessageCharacters = 1_000_000;
    /// Maximum number of assembler errors produced by one assemble operation
    public static final int maximumErrorMessages = 200;
    /// Maximum number of back-step operations to buffer
    public static final int maximumBacksteps = 2000;

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String syscallPropertiesFile = "Syscall";

    /// The program currently being worked with. Used by GUI only, not command line.
    public static RISCVProgram program;

    /// Flag to determine whether to produce internal debugging information.
    public static boolean debug = false;

    /// Exit code -- useful with SYSCALL 17 when running from command line (not GUI)
    public static int exitCode = 0;

    /// The GUI being used (if any) with this simulator.
    public static @Nullable VenusUI gui = null;

    static {
        Memory.setConfiguration(OTHER_SETTINGS.getMemoryConfiguration());
        Memory.getInstance().initialize(); // will establish memory configuration from setting
    }

    private Globals() {
    }

    /**
     * Read any syscall number assignment overrides from config file.
     *
     * @return ArrayList of SyscallNumberOverride objects
     */
    public static @NotNull List<@NotNull SyscallNumberOverride> getSyscallOverrides() {
        final var overrides = new ArrayList<SyscallNumberOverride>();
        final var properties = PropertiesFile.loadPropertiesFromFile(Globals.syscallPropertiesFile);
        for (final var key : properties.keySet()) {
            final String stringKey = (String) key;
            final String property = properties.getProperty(stringKey).trim();
            try {
                final int value = Integer.parseInt(property);
                overrides.add(new SyscallNumberOverride(stringKey, value));
            } catch (final NumberFormatException e) {
                LOGGER.fatal("Error processing Syscall number override: '{}' is not a valid integer", property);
                System.exit(0);
            }
        }
        return overrides;
    }

}
