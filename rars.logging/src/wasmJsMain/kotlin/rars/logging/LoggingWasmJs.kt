package rars.logging

object WasmJsConsoleAppender : Appender {
    override fun append(message: String, logLevel: LogLevel) {
        val jsMessage = message.toJsString()
        when (logLevel) {
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

@JsName("console")
@Suppress("unused")
private external object JsConsole {
    fun error(vararg message: JsAny?): Unit
    fun warn(vararg message: JsAny?): Unit
    fun info(vararg message: JsAny?): Unit
    fun debug(vararg message: JsAny?): Unit
    fun trace(vararg message: JsAny?): Unit
}
