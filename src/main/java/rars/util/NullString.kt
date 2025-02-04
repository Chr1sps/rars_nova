package rars.util

import arrow.core.Either
import arrow.core.raise.either
import rars.ProgramStatement
import rars.exceptions.AddressErrorException
import rars.exceptions.ExitingError
import rars.simulator.SimulationContext
import java.nio.charset.StandardCharsets

/*
Copyright (c) 2003-2017,  Pete Sanderson,Benjamin Landers and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu),
Benjamin Landers (benjaminrlanders@gmail.com),
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
 * Just a wrapper around #String get(ProgramStatement, String) which passes in
 * the default "a0"
 *
 * @param statement
 * a [ProgramStatement] object
 * @return a [java.lang.String] object
 * @throws ExitingError
 * if any.
 */
fun SimulationContext.readNullString(statement: ProgramStatement) = readNullString(statement, "a0")

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
    var byteAddress = registerFile.getIntValue(registerName)!!
    val utf8BytesList = mutableListOf<Byte>() // Need an array to hold bytes
    try {
        utf8BytesList.add(memory.getByte(byteAddress))
        while (utf8BytesList.last() != 0.toByte())  // until null terminator
        {
            byteAddress++
            utf8BytesList.add(memory.getByte(byteAddress))
        }
    } catch (e: AddressErrorException) {
        raise(ExitingError(statement, e))
    }

    val utf8Bytes = utf8BytesList.dropLast(1).toByteArray()
    String(utf8Bytes, StandardCharsets.UTF_8)
}
