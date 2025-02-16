package rars.io

internal const val SYSCALL_MAXFILES = 32

internal const val STDIN = 0
internal const val STDOUT = 1
internal const val STDERR = 2

internal const val O_RDONLY = 0x00000000
internal const val O_WRONLY = 0x00000001
internal const val O_RDWR = 0x00000002
internal const val O_APPEND = 0x00000008
internal const val O_CREAT = 0x00000200 // 512
internal const val O_TRUNC = 0x00000400 // 1024
internal const val O_EXCL = 0x00000800 // 2048

internal const val SEEK_SET = 0
internal const val SEEK_CUR = 1
internal const val SEEK_END = 2
