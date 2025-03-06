package rars.util

import arrow.core.Either
import rars.ksoftfloat.Environment
import rars.ksoftfloat.RoundingMode
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

inline fun <Ok> Either<*, Ok>.rightOr(defaultValue: Ok): Ok = this.fold({ defaultValue }, { it })
inline fun <Err> Either<Err, *>.leftOr(defaultValue: Err): Err = this.fold({ it }, { defaultValue })

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