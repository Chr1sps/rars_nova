package rars.riscv;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.exceptions.ExitingException;
import rars.exceptions.SimulationException;
import rars.riscv.syscalls.DisplayBitmapImpl;
import rars.riscv.syscalls.RandomStreams;
import rars.riscv.syscalls.ToneGenerator;
import rars.simulator.SimulationContext;
import rars.util.BinaryUtils;
import rars.util.NullString;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

@SuppressWarnings("DataFlowIssue")
public enum Syscall implements SimulationCallback {
    Close(
        "Close",
        57,
        "Close a file",
        "a0 = the file descriptor to close",
        "N/A",
        (stmt, ctxt) -> ctxt.io().closeFile(ctxt.registerFile().getIntValue(ctxt.registerFile().a0))
    ),
    ConfirmDialog(
        "ConfirmDialog",
        50,
        "Service to display a message to user",
        "a0 = address of null-terminated string that is the message to user",
        "a0 = Yes (0), No (1), or Cancel(2)",
        (stmt, ctxt) -> {
            final var message = NullString.get(stmt);
            int result = JOptionPane.showConfirmDialog(null, message);
            if (result == JOptionPane.CLOSED_OPTION) {
                result = JOptionPane.CANCEL_OPTION;
            }
            ctxt.registerFile().updateRegisterByName("a0", result);
        }
    ),
    DisplayBitmap(
        "DisplayBitmap",
        61,
        "Bitmap displaying memory contents",
        """
            a0 = address of the bitmap to display
            a1 = width of the bitmap
            a2 = height of the bitmap
            """,
        "N/A",
        (stmt, ctxt) -> DisplayBitmapImpl.INSTANCE.show(
            ctxt.registerFile().getIntValue("a0"),
            ctxt.registerFile().getIntValue("a1"),
            ctxt.registerFile().getIntValue("a2")
        )
    ),
    Exit(
        "Exit", 10,
        "Exits the program with code 0",
        (stmt, ctxt) -> {
            Globals.exitCode = 0;
            throw new ExitingException();
        }
    ),
    Exit2(
        "Exit2", 93,
        "Exits the program with a code",
        "a0 = the number to exit with",
        "N/A",
        (stmt, ctxt) -> {
            Globals.exitCode = ctxt.registerFile().getIntValue("a0");
            throw new ExitingException(); // empty error list
        }
    ),
    GetCWD(
        "GetCWD", 17, "Writes the path of the current working directory into a buffer",
        """
            a0 = the buffer to write into
            a1 = the length of the buffer""",
        "a0 = -1 if the path is longer than the buffer", (stmt, ctxt) -> {
        final var registerFile = ctxt.registerFile();
        final var memory = ctxt.memory();

        final var path = System.getProperty("user.dir");
        final int buf = registerFile.getIntValue(registerFile.a0);
        final int length = registerFile.getIntValue(registerFile.a0);

        final byte[] utf8BytesList = path.getBytes(StandardCharsets.UTF_8);
        if (length < utf8BytesList.length + 1) {
            // This should be -34 (ERANGE) for compatibility with spike, but until other
            // syscalls are ready with compatable
            // error codes, lets keep internal consitency.
            ctxt.registerFile().updateRegisterByName("a0", -1);
            return;
        }
        try {
            for (int index = 0; index < utf8BytesList.length; index++) {
                memory.setByte(
                    buf + index,
                    utf8BytesList[index]
                );
            }
            memory.setByte(buf + utf8BytesList.length, 0);
        } catch (final AddressErrorException e) {
            throw new ExitingException(stmt, e);
        }
    }
    ),
    // TODO: improve and unify it
    InputDialogDouble(
        "InputDialogDouble",
        53,
        "Service to display a message to a user and request a double input",
        """
            a0 = address of null-terminated string that is the message to user
            a1 = address of input buffer
            a2 = maximum number of characters to read (including the terminating null)""",
        """
            a1 contains status value.
            0: Valid input data, correctly parsed.
            -1: Input data cannot be correctly parsed.
            -2: Cancel was chosen.
            -3: OK was chosen but no data had been input into field.""",
        (stmt, ctxt) -> {

            // Input arguments: $a0 = address of null-terminated string that is the message
            // to user
            // Outputs:
            // fa0 value of double read. $f1 contains high order word of the double.
            // $a1 contains status value
            // 0: valid input data, correctly parsed
            // -1: input data cannot be correctly parsed
            // -2: Cancel was chosen
            // -3: OK was chosen but no data had been input into field

            final var registerFile = ctxt.registerFile();
            final var fpRegisterFile = ctxt.fpRegisterFile();

            final var prompt = NullString.get(stmt, "tp");

            final var inputValue = JOptionPane.showInputDialog(prompt);

            var exitCode = 0;
            var result = 0.0;
            if (inputValue == null) {
                // Cancel was chosen
                exitCode = -2;
            } else if (inputValue.isEmpty()) {
                // OK was chosen but there was no input
                exitCode = -3;
            } else {
                try {
                    result = Double.parseDouble(inputValue);
                } catch (final NumberFormatException e) {
                    // Unsuccessful parse of input data
                    exitCode = -1;

                }
            }
            fpRegisterFile.updateRegisterByName("ft0", Double.doubleToRawLongBits(result));
            registerFile.updateRegisterByName("a1", exitCode);
        }
    ),
    // TODO: improve and unify it
    InputDialogFloat(
        "InputDialogFloat", 52, "TODO", "TODO", "TODO", (stmt, ctxt) -> {

        final var prompt = NullString.get(stmt);

        final var input = JOptionPane.showInputDialog(prompt);

        final var registerFile = ctxt.registerFile();
        final var fpRegisterFile = ctxt.fpRegisterFile();

        var result = 0.0f;
        var exitCode = 0;
        try {
            if (input == null) {
                // Cancel was chosen
                exitCode = -2;
            } else if (input.isEmpty()) {
                // OK was chosen but there was no input
                exitCode = -3;
            } else {
                result = Float.parseFloat(input);
            }

        } catch (final NumberFormatException e) {
            // Unsuccessful parse of input data
            exitCode = -1;
        }
        fpRegisterFile.updateRegisterByNameInt("ft0", Float.floatToIntBits(result));
        registerFile.updateRegisterByName("a1", exitCode);
    }
    ),
    InputDialogInt(
        "InputDialogInt", 51,
        "Service to display a message to a user and request a double input",
        "a0 - address of null-terminated string that is the message to user",
        """
            - a0 - the value of int read, if valid, otherwise 0.
            - a1 - the status value:
              - 0 - OK status.
              - 1 - Input data couldn't be correctly parsed.
              - 2 - Cancel was chosen.
              - 3 - OK was chosen but no data had been input into field.""",
        (stmt, ctxt) -> {
            final var prompt = NullString.get(stmt);
            final var inputValue = JOptionPane.showInputDialog(prompt);
            int exitCode = 0, result = 0;
            if (inputValue == null) {
                // Cancel was chosen
                exitCode = -2;
            } else if (inputValue.isEmpty()) {
                // OK was chosen but there was no input
                exitCode = -3;
            } else {
                try {
                    result = Integer.parseInt(inputValue);
                } catch (final NumberFormatException e) {
                    // Unsuccessful parse of input data
                    exitCode = -1;
                }
            }
            ctxt.registerFile().updateRegisterByName("a0", result);
            ctxt.registerFile().updateRegisterByName("a1", exitCode);
        }
    ),
    InputDialogString(
        "InputDialogString",
        54,
        "Service to display a message to a user and request a string input",
        """
            a0 = address of null-terminated string that is the message to user
            a1 = address of input buffer
            a2 = maximum number of characters to read (including the terminating null)""", """
        a1 contains status value.
        0: OK status. Buffer contains the input string.
        -2: Cancel was chosen. No change to buffer.
        -3: OK was chosen but no data had been input into field. No change to buffer.
        -4: length of the input string exceeded the specified maximum. Buffer contains the maximum allowable input string terminated with null.""",
        (stmt, ctxt) -> {
            final var prompt = NullString.get(stmt);

            final var registerFile = ctxt.registerFile();
            final var memory = ctxt.memory();

            // Values returned by Java's InputDialog:
            // A null return value means that "Cancel" was chosen rather than OK.
            // An empty string returned (that is, inputString.length() of zero)
            // means that OK was chosen but no string was input.
            final String inputString = JOptionPane.showInputDialog(prompt);
            final int byteAddress = registerFile.getIntValue("a1"); // byteAddress of string is in a1
            final int maxLength = registerFile.getIntValue("a2"); // input buffer size for input string is in a2

            try {
                if (inputString == null) // Cancel was chosen
                {
                    registerFile.updateRegisterByName("a1", -2);
                } else if (inputString.isEmpty()) // OK was chosen but there was no input
                {
                    registerFile.updateRegisterByName("a1", -3);
                } else {
                    final byte[] utf8BytesList = inputString.getBytes(StandardCharsets.UTF_8);
                    // The buffer will contain characters, a '\n' character, and the null character
                    // Copy the input data to buffer as space permits
                    int stringLength = Math.min(maxLength - 1, utf8BytesList.length);
                    for (int index = 0; index < stringLength; index++) {
                        memory.setByte(
                            byteAddress + index,
                            utf8BytesList[index]
                        );
                    }
                    if (stringLength < maxLength - 1) {
                        memory.setByte(byteAddress + stringLength, '\n');
                        stringLength++;
                    }
                    memory.setByte(byteAddress + stringLength, 0);

                    if (utf8BytesList.length > maxLength - 1) {
                        // length of the input string exceeded the specified maximum
                        registerFile.updateRegisterByName("a1", -4);
                    } else {
                        registerFile.updateRegisterByName("a1", 0);
                    }
                } // end else

            } // end try
            catch (final AddressErrorException e) {
                throw new ExitingException(stmt, e);
            }
        }
    ),
    LSeek(
        "LSeek",
        62,
        "Seek to a position in a file",
        """
            a0 = the file descriptor
            a1 = the offset for the base
            a2 is the beginning of the file (0), the current position (1), or the end of the file (2)""",
        "a0 = the selected position from the beginning of the file or -1 is an error occurred",
        (stmt, ctxt) -> {
            final var registerFile = ctxt.registerFile();
            final int result = ctxt.io().seek(
                registerFile.getIntValue("a0"),
                registerFile.getIntValue("a1"),
                registerFile.getIntValue("a2")
            );
            registerFile.updateRegisterByName("a0", result);
        }
    ),
    MessageDialog(
        "MessageDialog", 55, "Service to display a message to user",
        """
            a0 = address of null-terminated string that is the message to user
            a1 = the type of the message to the user, which is one of:
            - 0: error message
            - 1: information message
            - 2: warning message
            - 3: question message
            - other: plain message""",
        "N/A", (stmt, ctxt) -> {
        int msgType = ctxt.registerFile().getIntValue("a1");
        if (msgType < JOptionPane.ERROR_MESSAGE || msgType > JOptionPane.ERROR_MESSAGE) {
            msgType = JOptionPane.PLAIN_MESSAGE;
        }
        JOptionPane.showMessageDialog(
            null,
            NullString.get(stmt),
            null,
            msgType
        );
    }
    ),
    MessageDialogDouble(
        "MessageDialogDouble",
        58,
        "Service to display message followed by a double",
        """
            a0 = address of null-terminated string that is the message to user
            fa0 = the double""",
        "N/A",
        (stmt, ctxt) -> {

            // TODO: maybe refactor this, other null strings are handled in a central place
            // now
            String message = ""; // = "";
            int byteAddress = ctxt.registerFile().getIntValue("a0");
            try {
                // Need an array to convert to String
                final char[] ch = {' '};
                ch[0] = (char) ctxt.memory().getByte(byteAddress);
                while (ch[0] != 0) // only uses single location ch[0]
                {
                    message = message.concat(new String(ch)); // parameter to String constructor is a char[] array
                    byteAddress++;
                    ch[0] = (char) ctxt.memory().getByte(byteAddress);
                }
            } catch (final AddressErrorException e) {
                throw new ExitingException(stmt, e);
            }

            JOptionPane.showMessageDialog(
                null,
                message + Double.longBitsToDouble(ctxt.fpRegisterFile().fa0.getValue()),
                null,
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    ),
    MessageDialogFloat(
        "MessageDialogFloat", 60, "Service to display a message followed by a float to user",
        """
            a0 = address of null-terminated string that is the message to user
            fa1 = the float to display""",
        "N/A", (stmt, ctxt) -> {
        final String message = NullString.get(stmt);

        // Display the dialog.
        JOptionPane.showMessageDialog(
            null,
            message + ctxt.fpRegisterFile().getFloatFromRegister(ctxt.fpRegisterFile().fa1),
            null,
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    ),
    MessageDialogInt(
        "MessageDialogInt", 56, "Service to display a message followed by a int to user",
        """
            a0 = address of null-terminated string that is the message to user
            a1 = the int to display""",
        "N/A", (stmt, ctxt) -> {
        final String message = NullString.get(stmt);

        // Display the dialog.
        JOptionPane.showMessageDialog(
            null,
            message + (int) ctxt.registerFile().getIntValue("a1"),
            null,
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    ),
    MessageDialogString(
        "MessageDialogString", 59, "Service to display a message followed by a string to user",
        """
            a0 = address of null-terminated string that is the message to user
            a1 = address of the second string to display""",
        "N/A", (stmt, ctxt) -> JOptionPane.showMessageDialog(
        null,
        NullString.get(stmt) + NullString.get(stmt, "a1"),
        null,
        JOptionPane.INFORMATION_MESSAGE
    )
    ),
    MidiOut(
        "MidiOut", 31, "Outputs simulated MIDI tone to sound card (does not wait for sound to end).",
        "See MIDI note below", "N/A", (stmt, ctxt) -> {
        final int rangeLowEnd = 0;
        final int rangeHighEnd = 127;
        int pitch = ctxt.registerFile().getIntValue("a0");
        int duration = ctxt.registerFile().getIntValue("a1");
        int instrument = ctxt.registerFile().getIntValue("a2");
        int volume = ctxt.registerFile().getIntValue("a3");
        if (pitch < rangeLowEnd || pitch > rangeHighEnd) {
            pitch = ToneGenerator.DEFAULT_PITCH;
        }
        if (duration < 0) {
            duration = ToneGenerator.DEFAULT_DURATION;
        }
        if (instrument < rangeLowEnd || instrument > rangeHighEnd) {
            instrument = ToneGenerator.DEFAULT_INSTRUMENT;
        }
        if (volume < rangeLowEnd || volume > rangeHighEnd) {
            volume = ToneGenerator.DEFAULT_VOLUME;
        }
        ToneGenerator.generateTone((byte) pitch, duration, (byte) instrument, (byte) volume);
    }
    ),
    MidiOutSync(
        "MidiOutSync", 33, "Outputs simulated MIDI tone to sound card, then waits until the sound finishes playing.",
        "See MIDI note below", "N/A", (stmt, ctxt) -> {
        /*
         * Arguments:
         * a0 - pitch (note). Integer value from 0 to 127, with 60 being middle-C on a
         * piano.\n
         * a1 - duration. Integer value in milliseconds.\n
         * a2 - instrument. Integer value from 0 to 127, with 0 being acoustic grand
         * piano.\n
         * a3 - volume. Integer value from 0 to 127.\n
         * <p>
         * Default values, in case any parameters are outside the above ranges, are
         * a0=60, a1=1000,
         * a2=0, a3=100.\n
         * <p>
         * See MARS/RARS documentation elsewhere or www.midi.org for more information.
         * Note that the pitch,
         * instrument and volume value ranges 0-127 are from javax.sound.midi; actual
         * MIDI instruments
         * use the range 1-128.
         */
        final var RANGE_LOW_END = 0;
        final var RANGE_HIGH_END = 127;
        int pitch = ctxt.registerFile().getIntValue("a0");
        int duration = ctxt.registerFile().getIntValue("a1");
        int instrument = ctxt.registerFile().getIntValue("a2");
        int volume = ctxt.registerFile().getIntValue("a3");
        if (pitch < RANGE_LOW_END || pitch > RANGE_HIGH_END) {
            pitch = ToneGenerator.DEFAULT_PITCH;
        }
        if (duration < 0) {
            duration = ToneGenerator.DEFAULT_DURATION;
        }
        if (instrument < RANGE_LOW_END || instrument > RANGE_HIGH_END) {
            instrument = ToneGenerator.DEFAULT_INSTRUMENT;
        }
        if (volume < RANGE_LOW_END || volume > RANGE_HIGH_END) {
            volume = ToneGenerator.DEFAULT_VOLUME;
        }
        ToneGenerator.generateToneSynchronously((byte) pitch, duration, (byte) instrument, (byte) volume);
    }
    ),
    Open(
        "Open", 1024, """
        Opens a file from a path
        Only supported flags (a1) are read-only (0), write-only (1) and\
         write-append (9). write-only flag creates file if it does not exist, so it is technically\
         write-create. write-append will start writing at end of existing file.""",
        "a0 = Null terminated string for the path \na1 = flags",
        "a0 = the file decriptor or -1 if an error occurred", (stmt, ctxt) -> {

        final int retValue = ctxt.io().openFile(
            NullString.get(stmt),
            ctxt.registerFile().getIntValue("a1")
        );
        // set returned fd value in register
        ctxt.registerFile().updateRegisterByName("a0", retValue);
    }
    ),
    PrintChar(
        "PrintChar", 11, "Prints an ascii character",
        "a0 = character to print (only lowest byte is considered)", "N/A", (stmt, ctxt) -> {
        final char t = (char) (ctxt.registerFile().getIntValue("a0") & 0x000000ff);
        ctxt.io().printString(Character.toString(t));
    }
    ),
    PrintDouble(
        "PrintDouble",
        3,
        "Prints a double precision floating point number",
        "fa0 = double to print",
        "N/A",
        (stmt, ctxt) -> {
            // Note: Higher numbered reg contains high order word so concat 13-12.
            ctxt.io()
                .printString(Double.toString(Double.longBitsToDouble(ctxt.fpRegisterFile().fa0.getValue())));
        }
    ),
    PrintFloat(
        "PrintFloat", 2, "Prints a floating point number", "fa0 = float to print", "N/A", (stmt, ctxt) -> {
        final var registerValue = ctxt.fpRegisterFile().getIntValue("fa0");
        ctxt.io().printString(Float.toString(Float.intBitsToFloat(registerValue)));
    }
    ),
    PrintInt(
        "PrintInt",
        1,
        "Prints an integer",
        "a0 = integer to print",
        "N/A",
        (stmt, ctxt) -> ctxt.io().printString(Integer.toString(ctxt.registerFile().getIntValue("a0")))
    ),
    PrintIntBinary(
        "PrintIntBinary",
        35,
        "Prints an integer (in binary format left-padded with zeroes) ",
        "a0 = integer to print",
        "N/A",
        (stmt, ctxt) -> ctxt.io().printString(BinaryUtils.intToBinaryString(ctxt.registerFile().getIntValue("a0")))
    ),
    PrintIntHex(
        "PrintIntHex",
        34,
        "Prints an integer (in hexdecimal format left-padded with zeroes)",
        "a0 = integer to print",
        "N/A",
        (stmt, ctxt) -> ctxt.io().printString(BinaryUtils.intToHexString(ctxt.registerFile().getIntValue("a0")))
    ),
    PrintIntUnsigned(
        "PrintIntUnsigned",
        36,
        "Prints an integer (unsigned)",
        "a0 = integer to print",
        "N/A",
        (stmt, ctxt) -> ctxt.io().printString(
            BinaryUtils.unsignedIntToIntString(ctxt.registerFile().getIntValue("a0")))
    ),
    PrintString(
        "PrintString", 4, "Prints a null-terminated string to the console",
        "a0 = the address of the string", "N/A", (stmt, ctxt) -> ctxt.io().printString(NullString.get(stmt))
    ),
    RandDouble(
        "RandDouble", 44, "Get a random double from the range 0.0-1.0",
        "a0 = index of pseudorandom number generator", "fa0 = the next pseudorandom", (stmt, ctxt) -> {
        final Integer index = ctxt.registerFile().getIntValue("a0");
        Random stream = RandomStreams.randomStreams.get(index);
        if (stream == null) {
            stream = new Random(); // create a non-seeded stream
            RandomStreams.randomStreams.put(index, stream);
        }
        try {
            ctxt.fpRegisterFile().updateRegisterByName("fa0", Double.doubleToRawLongBits(stream.nextDouble())
            );
        } catch (rars.exceptions.SimulationException e) {
            throw new RuntimeException(e);
        }
    }

    ),
    RandFloat(
        "RandFloat", 43, "Get a random float", "a0 = index of pseudorandom number generator",
        "fa0 = uniformly randomly selected from from [0,1]", (stmt, ctxt) -> {
        final Random stream = RandomStreams.get("a0");
        ctxt.fpRegisterFile().updateRegisterByNameInt(
            "fa0",
            Float.floatToRawIntBits(stream.nextFloat())
        );
    }
    ),
    RandInt(
        "RandInt",
        41,
        "Get a random integer",
        "a0 = index of pseudorandom number generator",
        "a0 = random integer",
        (stmt, ctxt) -> {
            final Random stream = RandomStreams.get("a0");
            ctxt.registerFile().updateRegisterByName("a0", stream.nextInt());
        }
    ),
    RandIntRange(
        "RandIntRange", 42, "Get a random bounded integer",
        """
            a0 = index of pseudorandom number generator
            a1 = upper bound for random number""",
        "a0 = uniformly selectect from [0,bound]", (stmt, ctxt) -> {
        final var stream = RandomStreams.get("a0");
        try {
            ctxt.registerFile().updateRegisterByName("a0", stream.nextInt(ctxt.registerFile().getIntValue("a1")));
        } catch (final IllegalArgumentException iae) {
            throw new ExitingException(
                stmt,
                "Upper bound of range cannot be negative (syscall 42)"
            );
        }
    }
    ),
    RandSeed(
        "RandSeed", 40, "Set seed for the underlying Java pseudorandom number generator",
        """
            a0 = index of pseudorandom number generator
            a1 = the seed""", "N/A", (stmt, ctxt) -> {
        final var index = ctxt.registerFile().getIntValue("a0");
        final Random stream = RandomStreams.randomStreams.get(index);
        if (stream == null) {
            RandomStreams.randomStreams.put(index, new Random(ctxt.registerFile().getIntValue("a1")));
        } else {
            stream.setSeed(ctxt.registerFile().getIntValue("a1"));
        }
    }
    ),
    Read(
        "Read", 63, "Read from a file descriptor into a buffer",
        """
            a0 = the file descriptor
            a1 = address of the buffer
            a2 = maximum length to read""",
        "a0 = the length read or -1 if error", (stmt, ctxt) -> {

        int byteAddress = ctxt.registerFile().getIntValue("a1"); // destination of characters read from file
        final int length = ctxt.registerFile().getIntValue("a2");
        final byte[] myBuffer = new byte[length]; // specified length
        // Call to ctxt.io().xxxx.read(xxx,xxx,xxx) returns actual length
        final int retLength = ctxt.io().readFromFile(
            ctxt.registerFile().getIntValue("a0"), // fd
            myBuffer, // buffer
            length
        ); // length
        // set returned value in register
        ctxt.registerFile().updateRegisterByName("a0", retLength);

        // copy bytes from returned buffer into memory
        try {
            int index = 0;
            while (index < retLength) {
                ctxt.memory().setByte(
                    byteAddress++,
                    myBuffer[index++]
                );
            }
        } catch (final AddressErrorException e) {
            throw new ExitingException(stmt, e);
        }
    }
    ),
    ReadChar(
        "ReadChar", 12, "Reads a character from input console", "N/A", "a0 = the character", (stmt, ctxt) -> {
        try {
            ctxt.registerFile().updateRegisterByName("a0", ctxt.io().readChar());
        } catch (final IndexOutOfBoundsException e) {
            throw new ExitingException(
                stmt,
                "invalid char input (syscall 12)"
            );
        }
    }
    ),
    ReadDouble(
        "ReadDouble",
        7,
        "Reads a double from input console",
        "N/A",
        "fa0 = the double",
        (stmt, ctxt) -> {
            final double doubleValue;
            try {
                doubleValue = ctxt.io().readDouble();
            } catch (final NumberFormatException e) {
                throw new ExitingException(
                    stmt,
                    "invalid double input (syscall 7)"
                );
            }

            ctxt.fpRegisterFile().updateRegisterByNumber(10, Double.doubleToRawLongBits(doubleValue));
        }
    ),
    ReadFloat(
        "ReadFloat", 6, "Reads a float from input console", "N/A", "fa0 = the float", (stmt, ctxt) -> {
        final float floatValue;
        try {
            floatValue = ctxt.io().readFloat();
        } catch (final NumberFormatException e) {
            throw new ExitingException(
                stmt,
                "invalid float input (syscall 6)"
            );
        }
        ctxt.fpRegisterFile().updateRegisterByNumberInt(
            10,
            Float.floatToRawIntBits(floatValue)
        ); // TODO: update to string fa0
    }
    ),
    ReadInt(
        "ReadInt",
        5,
        "Reads an int from input console",
        "N/A",
        "a0 = the int",
        (stmt, ctxt) -> {
            try {
                ctxt.registerFile().updateRegisterByName("a0", ctxt.io().readInt());
            } catch (final NumberFormatException e) {
                throw new ExitingException(
                    stmt,
                    "invalid integer input (syscall 5)"
                );
            }
        }
    ),
    /**
     * Service to read console input string into buffer starting at address in a0
     * for a1-1 bytes.
     * <p>
     * Performs syscall function to read console input string into buffer starting
     * at address in $a0.
     * Follows semantics of UNIX 'fgets'. For specified length n,
     * string can be no longer than n-1. If less than that, add
     * newline to end. In either case, then pad with null byte.
     */
    ReadString(
        "ReadString", 8, "Reads a string from the console",
        """
            a0 = address of input buffer
            a1 = maximum number of characters to read""", "N/A", (stmt, ctxt) -> {
        final int buf = ctxt.registerFile().getIntValue("a0"); // buf addr
        int maxLength = ctxt.registerFile().getIntValue("a1") - 1;
        boolean addNullByte = true;
        // Guard against negative maxLength. DPS 13-July-2011
        if (maxLength < 0) {
            maxLength = 0;
            addNullByte = false;
        }
        final String inputString = ctxt.io().readString(maxLength);

        final byte[] utf8BytesList = inputString.getBytes(StandardCharsets.UTF_8);
        // TODO: allow for utf-8 encoded strings
        int stringLength = Math.min(maxLength, utf8BytesList.length);
        try {
            for (int index = 0; index < stringLength; index++) {
                ctxt.memory().setByte(
                    buf + index,
                    utf8BytesList[index]
                );
            }
            if (stringLength < maxLength) {
                ctxt.memory().setByte(buf + stringLength, '\n');
                stringLength++;
            }
            if (addNullByte) {
                ctxt.memory().setByte(buf + stringLength, 0);
            }
        } catch (final AddressErrorException e) {
            throw new ExitingException(stmt, e);
        }
    }
    ),
    Sbrk(
        "Sbrk",
        9,
        "Allocate heap memory",
        "a0 = amount of memory in bytes",
        "a0 = address to the allocated block",
        (stmt, ctxt) -> {
            try {
                ctxt.registerFile().updateRegisterByName(
                    "a0",
                    ctxt.memory().allocateBytesFromHeap(ctxt.registerFile().getIntValue("a0"))
                );
            } catch (final IllegalArgumentException iae) {
                throw new ExitingException(
                    stmt,
                    iae.getMessage() + " (syscall 9)"
                );
            }
        }
    ),
    Sleep(
        "Sleep", 32, "Set the current thread to sleep for a time (not precise)", "a0 = time to sleep in milliseconds",
        "N/A", (stmt, ctxt) -> {

        try {
            Thread.sleep(ctxt.registerFile().getIntValue("a0"));
        } catch (final InterruptedException ignored) {
        }
    }
    ),
    Time(
        "Time", 30, "Get the current time (milliseconds since 1 January 1970)", "N/A",
        """
            a0 = low order 32 bits
            a1=high order 32 bits""", (stmt, ctxt) -> {
        final var time = System.currentTimeMillis();
        ctxt.registerFile().updateRegisterByName("a0", BinaryUtils.lowOrderLongToInt(time));
        ctxt.registerFile().updateRegisterByName("a1", BinaryUtils.highOrderLongToInt(time));
    }
    ),
    Write(
        "Write", 64,
        "Write to a file descriptor from a buffer",
        """
            a0 = the file descriptor
            a1 = the buffer address
            a2 = the length to write""",
        "a0 = the number of characters written", (stmt, ctxt) -> {

        final var registerFile = ctxt.registerFile();
        final var memory = ctxt.memory();
        int byteAddress = registerFile.getIntValue("a1"); // source of characters to write to file
        final int reqLength = registerFile.getIntValue("a2"); // user-requested length
        if (reqLength < 0) {
            registerFile.updateRegisterByName("a0", -1);
            return;
        }
        final byte[] myBuffer = new byte[reqLength];
        try {
            var byteValue = memory.getByte(byteAddress);
            int index = 0;
            while (index < reqLength) // Stop at requested length. Null bytes are included.
            {
                myBuffer[index++] = byteValue;
                byteAddress++;
                byteValue = memory.getByte(byteAddress);
            }
        } catch (final AddressErrorException e) {
            throw new ExitingException(stmt, e);
        }
        final int retValue = ctxt.io().writeToFile(
            registerFile.getIntValue("a0"), // fd
            myBuffer, // buffer
            registerFile.getIntValue("a2")
        ); // length
        // set returned value in register
        registerFile.updateRegisterByName("a0", retValue);
    }
    );

    /**
     * The name of this syscall. This can be used by a RARS
     * user to refer to the service when choosing to override its default service
     * number in the configuration file.
     */
    public final @NotNull String serviceName;
    /** A string describing what the system call does */
    public final @NotNull String description;
    /** A string documenting what registers should be set to before the system call runs. */
    public final @NotNull String inputs;
    /** A string documenting what registers are set to after the system call runs */
    public final @NotNull String outputs;
    /**
     * The assigned service number. This is the number the programmer
     * must store into a7 before issuing the ECALL instruction.
     */
    public final int serviceNumber;
    private final @NotNull SimulationCallback callback;

    /**
     * <p>Constructor for SyscallEnum.</p>
     *
     * @param name
     *     service name which may be used for reference independent of
     *     number
     * @param description
     *     a hort description of what the system calll does
     */
    Syscall(
        final @NotNull String name,
        final int number,
        final @NotNull String description,
        final @NotNull SimulationCallback callback
    ) {
        this(name, number, description, "N/A", "N/A", callback);
    }

    /**
     * <p>Constructor for SyscallEnum.</p>
     *
     * @param name
     *     service name which may be used for reference independent of
     *     number
     * @param description
     *     a short description of what the system call does
     * @param in
     *     a description of what registers should be set to before the
     *     system call
     * @param out
     *     a description of what registers are set to after the system call
     */
    Syscall(
        final @NotNull String name,
        final int number,
        final @NotNull String description,
        final @NotNull String in,
        final @NotNull String out,
        final @NotNull SimulationCallback callback
    ) {
        this.serviceNumber = number;
        this.serviceName = name;
        this.description = description;
        this.inputs = in;
        this.outputs = out;
        this.callback = callback;
    }

    public static @Nullable Syscall findSyscall(final @Range(from = 0, to = Integer.MAX_VALUE) int number) {
        for (final var syscall : values()) {
            if (syscall.serviceNumber == number) {
                return syscall;
            }
        }
        return null;
    }

    @Override
    public void simulate(
        final @NotNull ProgramStatement statement,
        final @NotNull SimulationContext context
    ) throws SimulationException {
        this.callback.simulate(statement, context);
    }
}
