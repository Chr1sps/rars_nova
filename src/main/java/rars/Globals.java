package rars;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.assembler.SymbolTable;
import rars.riscv.SyscallNumberOverride;
import rars.riscv.hardware.Memory;
import rars.riscv.hardware.MemoryConfiguration;
import rars.riscv.hardware.registerFiles.CSRegisterFile;
import rars.riscv.hardware.registerFiles.FloatingPointRegisterFile;
import rars.riscv.hardware.registerFiles.RegisterFile;
import rars.settings.*;
import rars.util.PropertiesFile;
import rars.venus.VenusUI;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.prefs.Preferences.userNodeForPackage;

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
 * Since the original RARS code contained *a lot* of globally accessible mutable state,
 * it became necessary to encapsulate it in a single class to make it easier to reason about
 * and to potentially make it easier to refactor in the future. This class serves as a place
 * to put all the global mutable state that was previously scattered throughout the codebase.
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
    /// List of accepted file extensions for RISCV assembly source files.
    public static final @NotNull List<@NotNull String> fileExtensions = List.of("asm", "s", "S");
    /// Maximum length of scrolled message window (RARS Messages and Run I/O)
    public static final int maximumMessageCharacters = 1_000_000;
    /// Maximum number of assembler errors produced by one assemble operation
    public static final int maximumErrorMessages = 200;
    /// Maximum number of back-step operations to buffer
    public static final int maximumBacksteps = 2000;
    /// Symbol table for file currently being assembled.
    public static final @NotNull SymbolTable GLOBAL_SYMBOL_TABLE;
    ///  Register file for the RARS simulator.
    public static final @NotNull RegisterFile REGISTER_FILE;
    /// Control and status register file for the RARS simulator.
    public static final @NotNull CSRegisterFile CS_REGISTER_FILE;
    private static final @NotNull Logger LOGGER = LogManager.getLogger(Globals.class);
    private static final String syscallPropertiesFile = "Syscall";
    ///  Floating point register file for the RARS simulator.
    public static @NotNull FloatingPointRegisterFile FP_REGISTER_FILE;
    /// Flag to determine whether to produce internal debugging information.
    public static boolean debug = false;
    /// Exit code -- useful with SYSCALL 17 when running from command line (not GUI)
    public static int exitCode = 0;
    /// The GUI being used (if any) with this simulator.
    public static @Nullable VenusUI gui = null;
    /// The program currently being worked with. Used by GUI only, not command line.
    public static RISCVProgram program;
    public static @NotNull OtherSettings OTHER_SETTINGS;
    public static @NotNull BoolSettings BOOL_SETTINGS;
    public static @NotNull EditorThemeSettings EDITOR_THEME_SETTINGS;
    public static @NotNull FontSettings FONT_SETTINGS;
    public static @NotNull HighlightingSettings HIGHLIGHTING_SETTINGS;

    public static @NotNull Memory MEMORY_INSTANCE;

    static {
        final var settingsPreferences = userNodeForPackage(SettingsBase.class);

        OTHER_SETTINGS = new OtherSettings(settingsPreferences);
        BOOL_SETTINGS = new BoolSettings(settingsPreferences);
        EDITOR_THEME_SETTINGS = new EditorThemeSettings(settingsPreferences);
        FONT_SETTINGS = new FontSettings(settingsPreferences);
        HIGHLIGHTING_SETTINGS = new HighlightingSettings(settingsPreferences);

        final var initialMemoryConfiguration = OTHER_SETTINGS.getMemoryConfiguration();

        MEMORY_INSTANCE = new Memory(initialMemoryConfiguration);

        GLOBAL_SYMBOL_TABLE = new SymbolTable();
        REGISTER_FILE = new RegisterFile(GLOBAL_SYMBOL_TABLE, initialMemoryConfiguration);
        FP_REGISTER_FILE = new FloatingPointRegisterFile();
        CS_REGISTER_FILE = new CSRegisterFile();
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

    public static void setupGlobalMemoryConfiguration(final @NotNull MemoryConfiguration newConfiguration) {
        MEMORY_INSTANCE.setMemoryConfigurationAndReset(newConfiguration);
        REGISTER_FILE.setValuesFromConfiguration(newConfiguration);
    }

    public static @NotNull Memory swapMemoryInstance(final @NotNull Memory mem) {
        final var previous = MEMORY_INSTANCE;
        MEMORY_INSTANCE = mem;
        return previous;
    }
}
