package rars.util

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.events.ExitingError
import rars.simulator.SimulationContext
import java.nio.charset.StandardCharsets

/**
 * Just a wrapper around #String get(ProgramStatement, String) which passes in
 * the default "a0"
 *
 * @param statement
 * a [ProgramStatement] object
 * @return a [java.lang.String] object
 * @throws ExitingError
 * if any.
 */
fun SimulationContext.readNullString(statement: ProgramStatement) =
    readNullString(statement, "a0")

/**
 * Reads a NULL terminated string from memory starting at the address in reg
 *
 * @param statement
 * the program statement this was called from (used for error
 * handling)
 * @param registerName
 * The name of the register for the address of the string
 * @return the string read from memory
 * @throws ExitingError
 * if it hits a #AddressErrorException
 */
fun SimulationContext.readNullString(
    statement: ProgramStatement,
    registerName: String
): Either<ExitingError, String> = either {
    var byteAddress = registerFile.getInt(registerName)!!
    val utf8BytesList = mutableListOf<Byte>() // Need an array to hold bytes
    either {
        utf8BytesList.add(memory.getByte(byteAddress).bind())
        // until null terminator
        while (utf8BytesList.last() != 0.toByte()) {
            byteAddress++
            utf8BytesList.add(memory.getByte(byteAddress).bind())
        }
    }.onLeft { error ->
        raise(ExitingError(statement, error))
    }

    val utf8Bytes = utf8BytesList.dropLast(1).toByteArray()
    String(utf8Bytes, StandardCharsets.UTF_8)
}
