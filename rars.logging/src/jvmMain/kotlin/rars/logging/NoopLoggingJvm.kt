package rars.logging

internal actual object NoopLoggerFactory : LoggerFactory {
    actual override fun forClass(cls: kotlin.reflect.KClass<*>): Logger =
        NoopLogger

    actual override fun forName(name: String): Logger = NoopLogger
    override fun forJavaClass(cls: Class<*>): Logger = NoopLogger
}