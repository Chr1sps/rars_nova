package rars.util

import arrow.core.Either

/**
 * Unwraps the value from an [Either.Right] or throws an exception if the value is an [Either.Left].
 */
fun <Err, Ok> Either<Err, Ok>.unwrap(): Ok = this.fold({ error("Expected Right, got Left") }, { it })

/**
 * Unwraps the error from an [Either.Left] or throws an exception if the value is an [Either.Right].
 */
fun <Err, Ok> Either<Err, Ok>.unwrapErr(): Err = this.fold({ it }, { error("Expected Left, got Right") })

fun <Err, Ok> Either<Err, Ok>.ignoreOk(): Either<Err, Unit> = this.map {}
fun <Err, Ok> Either<Err, Ok>.ignoreErr(): Either<Unit, Ok> = this.mapLeft {}