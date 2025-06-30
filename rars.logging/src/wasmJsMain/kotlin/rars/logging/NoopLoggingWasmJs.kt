package rars.logging

import kotlin.reflect.KClass

internal actual object NoopLoggerFactory : LoggerFactory {
    actual override fun forClass(cls: KClass<*>): Logger = NoopLogger
    actual override fun forName(name: String): Logger = NoopLogger
}