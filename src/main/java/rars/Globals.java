package rars;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.assembler.SymbolTable;
import rars.riscv.hardware.InterruptController;
import rars.riscv.hardware.Memory;
import rars.riscv.hardware.MemoryConfiguration;
import rars.riscv.hardware.registerFiles.CSRegisterFile;
import rars.riscv.hardware.registerFiles.FloatingPointRegisterFile;
import rars.riscv.hardware.registerFiles.RegisterFile;
import rars.settings.*;
import rars.simulator.Simulator;
import rars.venus.VenusUI;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.prefs.Preferences;

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

// TODO: Remove all the mutable state from this class, leave only all the constants.

/**
 * Since the original RARS code contained *a lot* of globally accessible mutable state,
 * it became necessary to encapsulate it in a single class to make it easier to reason about
 * and to potentially make it easier to refactor in the future. This class serves as a place
 * to put all the global mutable state that was previously scattered throughout the codebase.
 */
public final class Globals {

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
    /// Maximum number of back-step operations to buffer
    public static final int maximumBacksteps = 2000;

    /// Lock variable used at head of synchronized block to guard memory and registers
    @Deprecated
    public static final @NotNull ReentrantLock MEMORY_REGISTERS_LOCK = new ReentrantLock();

    /// Symbol table for file currently being assembled.
    @Deprecated
    public static final @NotNull SymbolTable GLOBAL_SYMBOL_TABLE;
    ///  Register file for the RARS simulator.
    @Deprecated(forRemoval = true)
    public static final @NotNull RegisterFile REGISTER_FILE;
    /// Control and status register file for the RARS simulator.
    @Deprecated(forRemoval = true)
    public static final @NotNull CSRegisterFile CS_REGISTER_FILE;
    @Deprecated
    public static final @NotNull Simulator SIMULATOR;
    @Deprecated(forRemoval = true)
    public static final @NotNull InterruptController INTERRUPT_CONTROLLER;
    @Deprecated(forRemoval = true)
    public static final @NotNull OtherSettingsImpl OTHER_SETTINGS;
    @Deprecated(forRemoval = true)
    public static final @NotNull BoolSettingsImpl BOOL_SETTINGS;
    @Deprecated(forRemoval = true)
    public static final @NotNull EditorThemeSettingsImpl EDITOR_THEME_SETTINGS;
    @Deprecated(forRemoval = true)
    public static final @NotNull FontSettingsImpl FONT_SETTINGS;
    @Deprecated(forRemoval = true)
    public static final @NotNull HighlightingSettingsImpl HIGHLIGHTING_SETTINGS;
    @Deprecated(forRemoval = true)
    public static final @NotNull AllSettings ALL_SETTINGS;
    ///  Floating point register file for the RARS simulator.
    @Deprecated(forRemoval = true)
    public static @NotNull FloatingPointRegisterFile FP_REGISTER_FILE;
    /// Flag to determine whether to produce internal debugging information.
    public static boolean debug = false;
    /// Exit code -- useful with SYSCALL 17 when running from command line (not GUI)
    public static int exitCode = 0;
    /// The GUI being used (if any) with this simulator.
    @Deprecated(forRemoval = true)
    public static @Nullable VenusUI GUI = null;
    /// The program currently being worked with. Used by GUI only, not command line.
    @Deprecated(forRemoval = true)
    public static @Nullable RISCVProgram PROGRAM;
    public static @NotNull Memory MEMORY_INSTANCE;

    static {
        SIMULATOR = new Simulator();

        final var settingsPreferences = Preferences.userRoot().node("/rars/settings");

        OTHER_SETTINGS = new OtherSettingsImpl(settingsPreferences);
        BOOL_SETTINGS = new BoolSettingsImpl(settingsPreferences);
        EDITOR_THEME_SETTINGS = new EditorThemeSettingsImpl(settingsPreferences);
        FONT_SETTINGS = new FontSettingsImpl(settingsPreferences);
        HIGHLIGHTING_SETTINGS = new HighlightingSettingsImpl(settingsPreferences);

        ALL_SETTINGS = new AllSettings(
            BOOL_SETTINGS,
            FONT_SETTINGS,
            EDITOR_THEME_SETTINGS,
            HIGHLIGHTING_SETTINGS,
            OTHER_SETTINGS
        );

        final var initialMemoryConfiguration = OTHER_SETTINGS.getMemoryConfiguration();

        MEMORY_INSTANCE = new Memory(initialMemoryConfiguration);

        GLOBAL_SYMBOL_TABLE = new SymbolTable();
        REGISTER_FILE = new RegisterFile(GLOBAL_SYMBOL_TABLE, initialMemoryConfiguration);
        FP_REGISTER_FILE = new FloatingPointRegisterFile();
        CS_REGISTER_FILE = new CSRegisterFile();

        INTERRUPT_CONTROLLER = new InterruptController(SIMULATOR, REGISTER_FILE);
    }

    private Globals() {
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
