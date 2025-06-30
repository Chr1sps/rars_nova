package rars.logging

private class WasmJsConsoleAppender(
    private val format: LoggingFormat
) : Appender {
    override fun append(context: LogContext) {
        val message = format.run { context.format() }
        val jsMessage = message.toJsString()
        when (context.level) {
            LogLevel.NONE -> Unit
            LogLevel.FATAL,
            LogLevel.ERROR -> JsConsole.error(jsMessage)
            LogLevel.WARNING -> JsConsole.warn(jsMessage)
            LogLevel.INFO -> JsConsole.info(jsMessage)
            LogLevel.DEBUG -> JsConsole.debug(jsMessage)
            LogLevel.TRACE -> JsConsole.trace(jsMessage)
        }
    }
}

fun LoggerFactoryBuilder.jsConsoleAppender(format: LoggingFormat): Unit =
    appender(WasmJsConsoleAppender(format))

@JsName("console")
@Suppress("unused")
private external object JsConsole {
    fun error(vararg message: JsAny?): Unit
    fun warn(vararg message: JsAny?): Unit
    fun info(vararg message: JsAny?): Unit
    fun debug(vararg message: JsAny?): Unit
    fun trace(vararg message: JsAny?): Unit
}
