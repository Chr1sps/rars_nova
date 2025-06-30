package rars.logging

internal fun LogLevel.isEnabledFor(level: LogLevel): Boolean =
    this != LogLevel.NONE && level != LogLevel.NONE && this <= level