package rars.util

import arrow.core.Either
import rars.ksoftfloat.Environment
import rars.ksoftfloat.RoundingMode
import rars.venus.editors.TokenStyle
import java.awt.Color
import java.awt.Font
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Unwraps the value from an [Either.Right] or invokes a callback
 * if the value is an [Either.Left].
 * By default, the callback throws an [IllegalStateException].
 */
@JvmOverloads
inline fun <Err, Ok> Either<Err, Ok>.unwrap(
    errorCallback: (Err) -> Nothing = { error("Expected Right, got Left. Left value: $it") }
): Ok = this.fold(errorCallback) { it }

/**
 * Unwraps the value from an [Either.Left] or invokes a callback
 * if the value is an [Either.Right].
 * By default, the callback throws an [IllegalStateException].
 */
inline fun <Err, Ok> Either<Err, Ok>.unwrapErr(
    okCallback: (Ok) -> Nothing = { error("Expected Left, got Right. Right value: $it") }
): Err = this.fold({ it }, okCallback)

/**
 * If the value is [Either.Right], maps it to [Unit]. Otherwise, returns the [Either.Left] value.
 */
fun <Err, Ok> Either<Err, Ok>.ignoreOk(): Either<Err, Unit> = this.map {}

/**
 * If the value is [Either.Left], maps it to [Unit]. Otherwise, returns the [Either.Right] value.
 */
fun <Err, Ok> Either<Err, Ok>.ignoreErr(): Either<Unit, Ok> = this.mapLeft {}

/**
 * If the value is [Either.Right], returns the value.
 * Otherwise, invokes the [guardFunc] with the value
 * stored in the [Either.Left] instance.
 */
inline fun <Err, Ok> Either<Err, Ok>.rightOrThrow(
    guardFunc: (Err) -> Nothing
): Either.Right<Ok> = this.fold(guardFunc) { Either.Right(it) }

/**
 * If the value is [Either.Left], returns the value.
 * Otherwise, invokes the [guardFunc] with the value
 * stored in the [Either.Right] instance.
 */
inline fun <Err, Ok> Either<Err, Ok>.leftOrThrow(
    guardFunc: (Ok) -> Nothing = { error("Expected Left, got Right") }
): Either.Left<Err> = this.fold(
    { Either.Left(it) }, guardFunc
)

inline fun <Ok> Either<*, Ok>.rightOr(defaultValue: Ok): Ok = fold({ defaultValue }, { it })
inline fun <Err, Ok> Either<Err, Ok>.rightOr(fromLeftFunc: (Err) -> Ok): Ok = fold(fromLeftFunc) { it }
inline fun <Err> Either<Err, *>.leftOr(defaultValue: Err): Err = fold({ it }, { defaultValue })
inline fun <Err, Ok> Either<Err, Ok>.leftOr(fromRightFunc: (Ok) -> Err): Err = fold({ it }, fromRightFunc)

fun Environment.flipRounding() {
    if (mode == RoundingMode.MAX) {
        mode = RoundingMode.MIN
    } else if (mode == RoundingMode.MIN) {
        mode = RoundingMode.MAX
    }
}

val Throwable.stacktraceString: String
    @JvmName("getStacktraceString")
    get() {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

fun <T : Comparable<T>> T.clamp(min: T, max: T): T = when {
    this < min -> min
    this > max -> max
    else -> this
}


fun <T : Comparable<T>> T.min(other: T): T = if (this < other) this else other

fun <T : Comparable<T>> T.max(other: T): T = if (this > other) this else other

fun <T> Iterable<T>.intersperseWith(separator: T): List<T> = buildList {
    for ((index, value) in this@intersperseWith.withIndex()) {
        add(value)
        if (index == lastIndex) break
        add(separator)
    }
}

val Collection<*>.lastIndex get() = size - 1

fun <T> Iterable<T>.intersperseWith(separatorFunc: () -> T): List<T> = when (this) {
    is Collection<*> -> buildList {
        for ((index, value) in this@intersperseWith.withIndex()) {
            add(value)
            if (index == this@intersperseWith.lastIndex) break
            add(separatorFunc())
        }
    }

    else -> {
        val iterator = this@intersperseWith.iterator()
        if (!iterator.hasNext()) emptyList()
        else buildList {
            while (true) {
                add(iterator.next())
                if (!iterator.hasNext()) break
                add(separatorFunc())
            }
        }
    }
}


fun <T> Array<out T>.intersperseWith(separator: T): List<T> = buildList {
    for ((index, value) in this@intersperseWith.withIndex()) {
        add(value)
        if (index == this@intersperseWith.lastIndex) break
        add(separator)
    }
}

fun <T> Array<out T>.intersperseWith(separatorFunc: () -> T): List<T> = buildList {
    for ((index, value) in this@intersperseWith.withIndex()) {
        add(value)
        if (index == this@intersperseWith.lastIndex) break
        add(separatorFunc())
    }
}

/**
 * Returns the color coded as Stringified 32-bit hex with
 * Red in bits 16-23, Green in bits 8-15, Blue in bits 0-7
 * e.g. "0x00FF3366" where Red is FF, Green is 33, Blue is 66.
 *
 * @return String containing hex-coded color second.
 */
fun Color.toHexString(): String = (this.red shl 16 or (this.green shl 8) or this.blue).toHexStringWithPrefix()

fun Font.applyStyle(style: TokenStyle): Font {
    var fontStyle = 0
    if (style.isBold) fontStyle = fontStyle or Font.BOLD
    if (style.isItalic) fontStyle = fontStyle or Font.ITALIC
    // noinspection MagicConstant
    return deriveFont(fontStyle)
}

fun unreachable(): Nothing = error("Unreachable code.")

object EmptyIterator : Iterator<Nothing> {
    override fun next(): Nothing = throw NoSuchElementException("Empty iterator has no next element.")
    override fun hasNext(): Boolean = false
}