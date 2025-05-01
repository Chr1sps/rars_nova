package rars.venus

import java.io.File

sealed interface FileStatusNew {
    val file: File

    data class New(override val file: File) : FileStatusNew
    data class NewEdited(override val file: File) : FileStatusNew
    data class NotEdited(override val file: File) : FileStatusNew
    data class Edited(override val file: File) : FileStatusNew
    data class Assembled(override val file: File) : FileStatusNew
    data class Running(override val file: File) : FileStatusNew
}

fun FileStatusNew.isNew(): Boolean = when (this) {
    is FileStatusNew.New,
    is FileStatusNew.NewEdited,
        -> true
    else -> false
}

fun FileStatusNew.hasUnsavedEdits() = when (this) {
    is FileStatusNew.NewEdited,
    is FileStatusNew.Edited,
        -> true
    else -> false
}

fun FileStatusNew.isAssembled() = when (this) {
    is FileStatusNew.Assembled,
    is FileStatusNew.Running,
        -> true
    else -> false
}

/*
 * Valid states for a single edit panel:
 * 1. New file, not edited - after creating a new file, but before making
 *    any changes to it.
 * 2. New file, edited - new file, but after making changes to it.
 * 3. Not edited - after opening a file, but before making any changes to it.
 * 4. Edited - after making changes to a file.
 */
