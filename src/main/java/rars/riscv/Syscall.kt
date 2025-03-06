package rars.riscv

import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import org.jetbrains.annotations.Range
import rars.Globals
import rars.exceptions.ExitingError
import rars.exceptions.ExitingEvent
import rars.riscv.syscalls.DisplayBitmapImpl
import rars.riscv.syscalls.ToneGenerator
import rars.riscv.syscalls.getRandomStream
import rars.riscv.syscalls.setRandomStreamSeed
import rars.util.*
import java.lang.Double
import java.lang.Float
import java.nio.charset.StandardCharsets
import java.util.*
import javax.swing.JOptionPane
import kotlin.ByteArray
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.NumberFormatException
import kotlin.String
import kotlin.Unit
import kotlin.code
import kotlin.error
import kotlin.let
import kotlin.math.min
import kotlin.takeIf
import kotlin.to
import kotlin.toUInt

enum class Syscall(
    /**
     * The name of this syscall. This can be used by a RARS
     * user to refer to the service when choosing to override its default service
     * number in the configuration file.
     */
    @JvmField val serviceName: String,
    /**
     * The assigned service number. This is the number the programmer
     * must store into a7 before issuing the ECALL instruction.
     */
    @JvmField val serviceNumber: Int,
    /** A string describing what the system call does  */
    @JvmField val description: String,
    /** A string documenting what registers should be set to before the system call runs.  */
    @JvmField val inputs: String,
    /** A string documenting what registers are set to after the system call runs  */
    @JvmField val outputs: String,
    private val callback: SimulationCallback
) : SimulationCallback by callback {
    Close(
        "Close", 57, "Close a file",
        "a0 = the file descriptor to close", "N/A",
        { stmt ->
            io.closeFile(
                registerFile.getIntValue(
                    registerFile.a0
                )
            ).right()
        }
    ),
    ConfirmDialog(
        "ConfirmDialog", 50, "Service to display a message to user",
        "a0 = address of null-terminated string that is the message to user",
        "a0 = Yes (0), No (1), or Cancel(2)",
        { stmt ->
            either {
                val message = readNullString(stmt).bind()
                var result = JOptionPane.showConfirmDialog(null, message)
                if (result == JOptionPane.CLOSED_OPTION) {
                    result = JOptionPane.CANCEL_OPTION
                }
                registerFile.updateRegisterByName("a0", result.toLong()).bind()
            }
        }
    ),
    DisplayBitmap(
        "DisplayBitmap", 61, "Bitmap displaying memory contents",
        """
            a0 = address of the bitmap to display
            a1 = width of the bitmap
            a2 = height of the bitmap
            """.trimIndent(),
        "N/A",
        { stmt ->
            DisplayBitmapImpl.INSTANCE.show(
                registerFile.getIntValue("a0")!!,
                registerFile.getIntValue("a1")!!,
                registerFile.getIntValue("a2")!!
            ).right()
        }
    ),
    Exit(
        "Exit", 10, "Exits the program with code 0",
        { stmt ->
            Globals.exitCode = 0
            ExitingEvent.left()
        }
    ),
    Exit2(
        "Exit2", 93, "Exits the program with a code",
        "a0 = the number to exit with", "N/A",
        { stmt ->
            Globals.exitCode = registerFile.getIntValue("a0")!!
            ExitingEvent.left()
        }
    ),
    GetCWD(
        "GetCWD", 17,
        "Writes the path of the current working directory into a buffer",
        """
            a0 = the buffer to write into
            a1 = the length of the buffer
            """.trimIndent(),
        "a0 = -1 if the path is longer than the buffer",
        { stmt ->
            either {
                val path = System.getProperty("user.dir")
                val buf = registerFile.getIntValue(registerFile.a0)
                val length = registerFile.getIntValue(registerFile.a0)

                val utf8BytesList = path.toByteArray(StandardCharsets.UTF_8)
                if (length < utf8BytesList.size + 1) {
                    // This should be -34 (ERANGE) for compatibility with spike, but until other
                    // syscalls are ready with compatable
                    // error codes, lets keep internal consitency.
                    registerFile.updateRegisterByName("a0", -1).bind()
                } else either {
                    for (index in utf8BytesList.indices) {
                        memory.setByte(
                            buf + index,
                            utf8BytesList[index]
                        ).bind()
                    }
                    memory.setByte(buf + utf8BytesList.size, 0).bind()
                }.onLeft { error ->

                }
                /* try {
                                } catch (e: MemoryError) {
                                    ExitingError(stmt, e).left()
                                }*/
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
            a2 = maximum number of characters to read (including the terminating null)
            """.trimIndent(),
        """
            a1 contains status value.
            0: Valid input data, correctly parsed.
            -1: Input data cannot be correctly parsed.
            -2: Cancel was chosen.
            -3: OK was chosen but no data had been input into field.
            """.trimIndent(),
        { stmt ->
            either {
                // Input arguments: $a0 = address of null-terminated string that is the message
                // to user
                // Outputs:
                // fa0 value of double read. $f1 contains high order word of the double.
                // $a1 contains status value
                // 0: valid input data, correctly parsed
                // -1: input data cannot be correctly parsed
                // -2: Cancel was chosen
                // -3: OK was chosen but no data had been input into field

                val prompt = readNullString(stmt, "tp").bind()

                val inputValue = JOptionPane.showInputDialog(prompt)

                var exitCode = 0
                var result = 0.0
                if (inputValue == null) {
                    // Cancel was chosen
                    exitCode = -2
                } else if (inputValue.isEmpty()) {
                    // OK was chosen but there was no input
                    exitCode = -3
                } else {
                    try {
                        result = inputValue.toDouble()
                    } catch (_: NumberFormatException) {
                        // Unsuccessful parse of input data
                        exitCode = -1
                    }
                }
                fpRegisterFile.updateRegisterByName("ft0", Double.doubleToRawLongBits(result)).bind()
                registerFile.updateRegisterByName("a1", exitCode.toLong()).bind()
            }
        }
    ),

    // TODO: improve and unify it
    InputDialogFloat(
        "InputDialogFloat",
        52,
        "TODO",
        "TODO",
        "TODO",
        { stmt ->
            either {
                val prompt = readNullString(stmt).bind()
                val input = JOptionPane.showInputDialog(prompt)

                var result = 0.0f
                var exitCode = 0
                if (input == null) {
                    // Cancel was chosen
                    exitCode = -2
                } else if (input.isEmpty()) {
                    // OK was chosen but there was no input
                    exitCode = -3
                } else try {
                    result = input.toFloat()
                } catch (_: NumberFormatException) {
                    // Unsuccessful parse of input data
                    exitCode = -1
                }

                fpRegisterFile.updateRegisterByNameInt("ft0", Float.floatToIntBits(result))
                registerFile.updateRegisterByName("a1", exitCode.toLong())
            }
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
              - 3 - OK was chosen but no data had been input into field.
              """.trimIndent(),
        { stmt ->
            either {
                val prompt = readNullString(stmt).bind()
                val inputValue = JOptionPane.showInputDialog(prompt)
                var exitCode = 0
                var result = 0
                if (inputValue == null) {
                    // Cancel was chosen
                    exitCode = -2
                } else if (inputValue.isEmpty()) {
                    // OK was chosen but there was no input
                    exitCode = -3
                } else {
                    try {
                        result = inputValue.toInt()
                    } catch (_: NumberFormatException) {
                        // Unsuccessful parse of input data
                        exitCode = -1
                    }
                }
                registerFile.updateRegisterByName("a0", result.toLong()).bind()
                registerFile.updateRegisterByName("a1", exitCode.toLong()).bind()
            }
        }
    ),
    InputDialogString(
        "InputDialogString",
        54,
        "Service to display a message to a user and request a string input",
        """
            a0 = address of null-terminated string that is the message to user
            a1 = address of input buffer
            a2 = maximum number of characters to read (including the terminating null)
            """.trimIndent(), """
        a1 contains status value.
        0: OK status. Buffer contains the input string.
        -2: Cancel was chosen. No change to buffer.
        -3: OK was chosen but no data had been input into field. No change to buffer.
        -4: length of the input string exceeded the specified maximum. Buffer contains the maximum allowable input string terminated with null.
        """.trimIndent(),
        { stmt ->
            either {
                val prompt = readNullString(stmt).bind()

                // Values returned by Java's InputDialog:
                // A null return value means that "Cancel" was chosen rather than OK.
                // An empty string returned (that is, inputString.length() of zero)
                // means that OK was chosen but no string was input.
                val inputString = JOptionPane.showInputDialog(prompt)
                // byteAddress of string is in a1
                val byteAddress = registerFile.getIntValue("a1")!!
                // input buffer size for input string is in a2
                val maxLength = registerFile.getIntValue("a2")!!
                val status: Int = if (inputString == null) {
                    // Cancel was chosen
                    -2
                } else if (inputString.isEmpty()) {
                    // OK was chosen but there was no input
                    -3
                } else {
                    val utf8BytesList = inputString.toByteArray(StandardCharsets.UTF_8)
                    // The buffer will contain characters, a '\n' character, and the null character
                    // Copy the input data to buffer as space permits
                    var stringLength = min((maxLength - 1).toDouble(), utf8BytesList.size.toDouble()).toInt()
                    either {
                        for (index in 0..<stringLength) {
                            memory.setByte(
                                byteAddress + index,
                                utf8BytesList[index]
                            ).bind()
                        }
                        if (stringLength < maxLength - 1) {
                            memory.setByte(byteAddress + stringLength, '\n'.code.toByte()).bind()
                            stringLength++
                        }
                        memory.setByte(byteAddress + stringLength, 0).bind()
                    }.onLeft { error ->
                        raise(ExitingError(stmt, error))
                    }
                    if (utf8BytesList.size > maxLength - 1) -4 else 0
                }
                registerFile.updateRegisterByName("a1", status.toLong()).bind()
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
            a2 is the beginning of the file (0), the current position (1), or the end of the file (2)
            """.trimIndent(),
        "a0 = the selected position from the beginning of the file or -1 is an error occurred",
        { stmt ->
            val result: Int = io.seek(
                registerFile.getIntValue("a0")!!,
                registerFile.getIntValue("a1")!!,
                registerFile.getIntValue("a2")!!
            )
            registerFile.updateRegisterByName("a0", result.toLong()).ignoreOk()
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
            - other: plain message
            """.trimIndent(),
        "N/A", { stmt ->
            var msgType: Int = registerFile.getIntValue("a1")!!
            if (msgType < JOptionPane.ERROR_MESSAGE || msgType > JOptionPane.ERROR_MESSAGE) {
                msgType = JOptionPane.PLAIN_MESSAGE
            }
            JOptionPane.showMessageDialog(
                null,
                readNullString(stmt),
                null,
                msgType
            )
            Unit.right()
        }
    ),
    MessageDialogDouble(
        "MessageDialogDouble",
        58,
        "Service to display message followed by a double",
        """
            a0 = address of null-terminated string that is the message to user
            fa0 = the double
            """.trimIndent(),
        "N/A",
        { stmt ->
            either {
                val message = readNullString(stmt).bind()
                JOptionPane.showMessageDialog(
                    null,
                    message + Double.longBitsToDouble(fpRegisterFile.fa0.getValue()),
                    null,
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
        }
    ),
    MessageDialogFloat(
        "MessageDialogFloat", 60, "Service to display a message followed by a float to user",
        """
            a0 = address of null-terminated string that is the message to user
            fa1 = the float to display
            """.trimIndent(),
        "N/A", { stmt ->
            either {
                val message = readNullString(stmt).bind()
                // Display the dialog.
                JOptionPane.showMessageDialog(
                    null,
                    message + fpRegisterFile.getFloatFromRegister(fpRegisterFile.fa1),
                    null,
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
        }
    ),
    MessageDialogInt(
        "MessageDialogInt", 56, "Service to display a message followed by a int to user",
        """
            a0 = address of null-terminated string that is the message to user
            a1 = the int to display
            """.trimIndent(),
        "N/A", { stmt ->
            either {
                val message = readNullString(stmt).bind()
                // Display the dialog.
                JOptionPane.showMessageDialog(
                    null,
                    message + registerFile.getIntValue("a1") as Int,
                    null,
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
        }
    ),
    MessageDialogString(
        "MessageDialogString", 59, "Service to display a message followed by a string to user",
        """
            a0 = address of null-terminated string that is the message to user
            a1 = address of the second string to display
            """.trimIndent(),
        "N/A", { stmt ->
            either {
                JOptionPane.showMessageDialog(
                    null,
                    readNullString(stmt).bind() +
                            readNullString(stmt, "a1").bind(),
                    null,
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
        }
    ),
    MidiOut(
        "MidiOut", 31, "Outputs simulated MIDI tone to sound card (does not wait for sound to end).",
        "See MIDI note below", "N/A", { stmt ->
            val rangeLowEnd = 0
            val rangeHighEnd = 127
            val pitch: Int = registerFile.getIntValue("a0")!!
                .takeIf { it in rangeLowEnd..rangeHighEnd }
                ?: ToneGenerator.DEFAULT_PITCH.toInt()
            val duration: Int = registerFile.getIntValue("a1")!!
                .takeIf { it > 0 }
                ?: ToneGenerator.DEFAULT_DURATION
            val instrument: Int = registerFile.getIntValue("a2")!!
                .takeIf { it in rangeLowEnd..rangeHighEnd }
                ?: ToneGenerator.DEFAULT_INSTRUMENT.toInt()
            val volume: Int = registerFile.getIntValue("a3")!!
                .takeIf { it in rangeLowEnd..rangeHighEnd }
                ?: ToneGenerator.DEFAULT_VOLUME.toInt()
            ToneGenerator.generateTone(pitch.toByte(), duration, instrument.toByte(), volume.toByte())
            Unit.right()
        }
    ),
    MidiOutSync(
        "MidiOutSync", 33, "Outputs simulated MIDI tone to sound card, then waits until the sound finishes playing.",
        "See MIDI note below", "N/A", { stmt ->
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
            val rangeLowEnd = 0
            val rangeHighEnd = 127
            val pitch: Int = registerFile.getIntValue("a0")!!
                .takeIf { it in rangeLowEnd..rangeHighEnd }
                ?: ToneGenerator.DEFAULT_PITCH.toInt()
            val duration: Int = registerFile.getIntValue("a1")!!
                .takeIf { it > 0 }
                ?: ToneGenerator.DEFAULT_DURATION
            val instrument: Int = registerFile.getIntValue("a2")!!
                .takeIf { it in rangeLowEnd..rangeHighEnd }
                ?: ToneGenerator.DEFAULT_INSTRUMENT.toInt()
            val volume: Int = registerFile.getIntValue("a3")!!
                .takeIf { it in rangeLowEnd..rangeHighEnd }
                ?: ToneGenerator.DEFAULT_VOLUME.toInt()
            ToneGenerator.generateToneSynchronously(pitch.toByte(), duration, instrument.toByte(), volume.toByte())
            Unit.right()
        }
    ),
    Open(
        "Open",
        1024,
        """
        Opens a file from a path
        Only supported flags (a1) are:
        - read-only (0),
        - write-only (1),
        - write-append (9).
        Write-only flag creates file if it does not exist, so it is technically write-create.
        Write-append will start writing at end of existing file.
        """.trimIndent(),
        "a0 = Null terminated string for the path \na1 = flags",
        "a0 = the file decriptor or -1 if an error occurred",
        { stmt ->
            either {
                val retValue: Int = io.openFile(
                    readNullString(stmt).bind(),
                    registerFile.getIntValue("a1")!!
                )
                // set returned fd value in register
                registerFile.updateRegisterByName("a0", retValue.toLong()).bind()
            }
        }
    ),
    PrintChar(
        "PrintChar",
        11,
        "Prints an ascii character",
        "a0 = character to print (only lowest byte is considered)",
        "N/A",
        { stmt ->
            val t = (registerFile.getIntValue("a0")!! and 0x000000ff).toChar()
            io.printString(t.toString())
            Unit.right()
        }
    ),
    PrintDouble(
        "PrintDouble",
        3,
        "Prints a double precision floating point number",
        "fa0 = double to print",
        "N/A",
        { stmt ->
            // Note: Higher numbered reg contains high order word so concat 13-12.
            val value = fpRegisterFile.fa0.getValue().toDoubleReinterpreted()
            io.printString(value.toString())
            Unit.right()
        }
    ),
    PrintFloat(
        "PrintFloat",
        2,
        "Prints a floating point number",
        "fa0 = float to print",
        "N/A",
        { stmt ->
            val registerValue = fpRegisterFile.getIntValue("fa0")!!
            io.printString(Float.intBitsToFloat(registerValue).toString())
            Unit.right()
        }
    ),
    PrintInt(
        "PrintInt",
        1,
        "Prints an integer",
        "a0 = integer to print",
        "N/A",
        { stmt ->
            io.printString(
                registerFile.getIntValue(
                    "a0"
                )!!.toString()
            )
            Unit.right()
        }
    ),
    PrintIntBinary(
        "PrintIntBinary",
        35,
        "Prints an integer (in binary format left-padded with zeroes) ",
        "a0 = integer to print",
        "N/A",
        { stmt ->
            io.printString(
                registerFile.getIntValue(
                    "a0"
                )!!.toBinaryString()
            )
            Unit.right()
        }
    ),
    PrintIntHex(
        "PrintIntHex",
        34,
        "Prints an integer (in hexdecimal format left-padded with zeroes)",
        "a0 = integer to print",
        "N/A",
        { stmt ->
            io.printString(
                registerFile.getIntValue(
                    "a0"
                )!!.toHexStringWithPrefix()
            )
            Unit.right()
        }
    ),
    PrintIntUnsigned(
        "PrintIntUnsigned",
        36,
        "Prints an integer (unsigned)",
        "a0 = integer to print",
        "N/A",
        { stmt ->
            val value = registerFile.getIntValue("a0")!!.toUInt()
            io.printString(value.toString())
            Unit.right()
        }
    ),
    PrintString(
        "PrintString",
        4,
        "Prints a null-terminated string to the console",
        "a0 = the address of the string",
        "N/A",
        { stmt ->
            either {
                io.printString(readNullString(stmt).bind())
                Unit.right()
            }
        }
    ),
    RandDouble(
        "RandDouble",
        44,
        "Get a random double from the range 0.0-1.0",
        "a0 = index of pseudorandom number generator",
        "fa0 = the next pseudorandom",
        { stmt ->
            val stream = getRandomStream("a0")
            fpRegisterFile.updateRegisterByName(
                "fa0", stream.nextDouble().toLongReinterpreted()
            ).onLeft { error(it.toString()) }
            Unit.right()
        }
    ),
    RandFloat(
        "RandFloat",
        43,
        "Get a random float",
        "a0 = index of pseudorandom number generator",
        "fa0 = uniformly randomly selected from from [0,1]",
        { stmt ->
            val stream: Random = getRandomStream("a0")
            fpRegisterFile.updateRegisterByNameInt(
                "fa0",
                stream.nextFloat().toIntReinterpreted()
            )
        }
    ),
    RandInt(
        "RandInt",
        41,
        "Get a random integer",
        "a0 = index of pseudorandom number generator",
        "a0 = random integer",
        { stmt ->
            val stream = getRandomStream("a0")
            registerFile.updateRegisterByName(
                "a0",
                stream.nextInt().toLong()
            ).ignoreOk()
        }
    ),
    RandIntRange(
        "RandIntRange",
        42,
        "Get a random bounded integer",
        """
            a0 = index of pseudorandom number generator
            a1 = upper bound for random number
            """.trimIndent(),
        "a0 = uniformly selected from [0,bound]",
        { stmt ->
            either {
                val stream = getRandomStream("a0")
                val value = try {
                    stream.nextInt(registerFile.getIntValue("a1")!!).toLong()
                } catch (_: IllegalArgumentException) {
                    raise(ExitingError(stmt, "Upper bound of range cannot be negative (syscall 42)"))
                }
                registerFile.updateRegisterByName(
                    "a0",
                    value
                ).bind()
            }
        }
    ),
    RandSeed(
        "RandSeed", 40, "Set seed for the underlying Java pseudorandom number generator",
        """
            a0 = index of pseudorandom number generator
            a1 = the seed
            """.trimIndent(), "N/A", { stmt ->
            val index = registerFile.getIntValue("a0")!!
            val seed = registerFile.getIntValue("a1")!!.toLong()
            setRandomStreamSeed(index, seed)
            Unit.right()
        }
    ),
    Read(
        "Read", 63, "Read from a file descriptor into a buffer",
        """
            a0 = the file descriptor
            a1 = address of the buffer
            a2 = maximum length to read
            """.trimIndent(),
        "a0 = the length read or -1 if error", { stmt ->
            val destinationAddress = registerFile.getIntValue("a1")!!
            val length = registerFile.getIntValue("a2")!!
            val myBuffer = ByteArray(length)
            // Call to io().xxxx.read(xxx,xxx,xxx) returns actual length
            val retLength = io.readFromFile(
                registerFile.getIntValue("a0")!!,
                myBuffer,
                length
            )
            either {
                // set returned value in register
                registerFile.updateRegisterByName(
                    "a0",
                    retLength.toLong()
                ).bind()

                // copy bytes from returned buffer into memory
                either {
                    for (index in 0..<retLength) {
                        memory.setByte(
                            destinationAddress + index,
                            myBuffer[index]
                        ).bind()
                    }
                }.onLeft { error ->
                    raise(ExitingError(stmt, error))
                }
            }
        }
    ),
    ReadChar(
        "ReadChar",
        12,
        "Reads a character from input console",
        "N/A",
        "a0 = the character",
        { stmt ->
            registerFile.updateRegisterByName(
                "a0",
                io.readChar().code.toLong()
            ).ignoreOk()
        }
    ),
    ReadDouble(
        "ReadDouble",
        7,
        "Reads a double from input console",
        "N/A",
        "fa0 = the double",
        { stmt ->
            val doubleValue = try {
                io.readDouble().right()
            } catch (_: NumberFormatException) {
                ExitingError(
                    stmt,
                    "invalid double input (syscall 7)"
                ).left()
            }
            doubleValue.map {
                fpRegisterFile.updateRegisterByNumber(10, it.toLongReinterpreted())
            }
        }
    ),
    ReadFloat(
        "ReadFloat",
        6,
        "Reads a float from input console",
        "N/A",
        "fa0 = the float",
        { stmt ->
            val floatValue = try {
                io.readFloat().right()
            } catch (_: NumberFormatException) {
                ExitingError(
                    stmt,
                    "invalid float input (syscall 6)"
                ).left()
            }
            floatValue.map {
                fpRegisterFile.updateRegisterByNumberInt(10, it.toIntReinterpreted())
            }
        }
    ),
    ReadInt(
        "ReadInt",
        5,
        "Reads an int from input console",
        "N/A",
        "a0 = the int",
        { stmt ->
            either {
                try {
                    val newValue = io.readInt().toLong()
                    registerFile.updateRegisterByName("a0", newValue)
                } catch (_: NumberFormatException) {
                    ExitingError(
                        stmt,
                        "invalid integer input (syscall 5)"
                    )
                }
            }
        }
    ),

    /**
     * Service to read console input string into buffer starting at address in a0
     * for a1-1 bytes.
     *
     *
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
            a1 = maximum number of characters to read
            """.trimIndent(), "N/A", { stmt ->
            val bufferAddress = registerFile.getIntValue("a0")!!
            val (maxLength, addNullByte) = registerFile.getIntValue("a1")!!.let {
                if (it < 0) 0 to false
                else it - 1 to true
            }
            val inputString = io.readString(maxLength)

            val utf8BytesList = inputString.toByteArray(StandardCharsets.UTF_8)
            // TODO: allow for utf-8 encoded strings
            var stringLength = min(maxLength.toDouble(), utf8BytesList.size.toDouble()).toInt()
            either {
                for (index in 0..<stringLength) {
                    memory.setByte(
                        bufferAddress + index,
                        utf8BytesList[index]
                    ).bind()
                }
                if (stringLength < maxLength) {
                    memory.setByte(bufferAddress + stringLength, '\n'.code.toByte()).bind()
                    stringLength++
                }
                if (addNullByte) {
                    memory.setByte(bufferAddress + stringLength, 0).bind()
                }
                Unit
            }.mapLeft { error ->
                ExitingError(stmt, error)
            }
        }
    ),
    Sbrk(
        "Sbrk",
        9,
        "Allocate heap memory",
        "a0 = amount of memory in bytes",
        "a0 = address to the allocated block",
        { stmt ->
            memory.allocateBytes(registerFile.getIntValue("a0")!!).fold(
                { errorMessage ->
                    ExitingError(
                        stmt,
                        "$errorMessage (syscall 9)"
                    ).left()
                },
                { address ->
                    registerFile.updateRegisterByName("a0", address.toLong())
                    Unit.right()
                }
            )
        }
    ),
    Sleep(
        "Sleep", 32, "Set the current thread to sleep for a time (not precise)", "a0 = time to sleep in milliseconds",
        "N/A", { stmt ->
            try {
                Thread.sleep(registerFile.getIntValue("a0")!!.toLong())
            } catch (_: InterruptedException) {
            }
            Unit.right()
        }
    ),
    Time(
        "Time", 30, "Get the current time (milliseconds since 1 January 1970)", "N/A",
        """
            a0 = low order 32 bits
            a1=high order 32 bits
            """.trimIndent(), { stmt ->
            either {
                val time = System.currentTimeMillis()
                registerFile.updateRegisterByName("a0", BinaryUtilsOld.lowOrderLongToInt(time).toLong()).bind()
                registerFile.updateRegisterByName("a1", BinaryUtilsOld.highOrderLongToInt(time).toLong()).bind()
            }
        }
    ),
    Write(
        "Write",
        64,
        "Write to a file descriptor from a buffer",
        """
            a0 = the file descriptor
            a1 = the buffer address
            a2 = the length to write
            """.trimIndent(),
        "a0 = the number of characters written",
        { stmt ->
            either {
                var byteAddress = registerFile.getIntValue("a1")!! // source of characters to write to file
                val reqLength = registerFile.getIntValue("a2")!! // user-requested length
                if (reqLength < 0) {
                    registerFile.updateRegisterByName("a0", -1).bind()
                    return@either
                }
                val myBuffer = ByteArray(reqLength)
                either {
                    var byteValue = memory.getByte(byteAddress).bind()
                    var index = 0
                    // Stop at requested length. Null bytes are included.
                    while (index < reqLength) {
                        myBuffer[index++] = byteValue
                        byteAddress++
                        byteValue = memory.getByte(byteAddress).bind()
                    }
                }.onLeft { error ->
                    raise(ExitingError(stmt, error))
                }
                val retValue: Int = io.writeToFile(
                    registerFile.getIntValue("a0")!!, // fd
                    myBuffer, // buffer
                    registerFile.getIntValue("a2")!! // length
                )
                registerFile.updateRegisterByName("a0", retValue.toLong()).bind()
            }
        }
    );

    /**
     *
     * Constructor for SyscallEnum.
     *
     * @param name
     * service name which may be used for reference independent of
     * number
     * @param description
     * a hort description of what the system calll does
     */
    constructor(
        name: String,
        number: Int,
        description: String,
        callback: SimulationCallback
    ) : this(name, number, description, "N/A", "N/A", callback)

    companion object {
        @JvmStatic
        fun findSyscall(number: @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int) =
            entries.find { it.serviceNumber == number }
    }
}
