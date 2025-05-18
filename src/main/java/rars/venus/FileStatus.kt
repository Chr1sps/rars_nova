package rars.venus

import java.io.File

sealed interface FileStatus {
    sealed interface New : FileStatus {
        val tmpName: String

        data class NotEdited(override val tmpName: String) : New
        data class Edited(override val tmpName: String) : New
    }

    sealed interface Existing : FileStatus {
        val file: File

        data class NotEdited(override val file: File) : Existing
        data class Edited(override val file: File) : Existing
        data class Runnable(override val file: File) : Existing
        data class Running(override val file: File) : Existing
        data class Terminated(override val file: File) : Existing
    }
}

fun FileStatus.copy() = when (this) {
    is FileStatus.New.NotEdited -> this.copy()
    is FileStatus.New.Edited -> this.copy()
    is FileStatus.Existing.NotEdited -> this.copy()
    is FileStatus.Existing.Edited -> this.copy()
    is FileStatus.Existing.Runnable -> this.copy()
    is FileStatus.Existing.Running -> this.copy()
    is FileStatus.Existing.Terminated -> this.copy()
}

fun FileStatus.isNew() = this is FileStatus.New
fun FileStatus.isEdited() = when (this) {
    is FileStatus.New.Edited,
    is FileStatus.Existing.Edited,
        -> true
    else -> false
}

fun FileStatus.isAssembled() = when (this) {
    is FileStatus.Existing.Runnable,
    is FileStatus.Existing.Running,
        -> true
    else -> false
}

val FileStatus.nameForEditorTab: String
    get() = when (this) {
        is FileStatus.New -> tmpName
        is FileStatus.Existing -> file.name
    }

val FileStatus.nameForTitleBar: String
    get() = when (this) {
        is FileStatus.New -> tmpName
        is FileStatus.Existing -> file.absolutePath
    }

fun FileStatus.New.toNotEdited() = FileStatus.New.NotEdited(tmpName)
fun FileStatus.New.toEdited() = FileStatus.New.Edited(tmpName)

fun FileStatus.Existing.toNotEdited() = FileStatus.Existing.NotEdited(file)
fun FileStatus.Existing.toEdited() = FileStatus.Existing.Edited(file)
fun FileStatus.Existing.toRunnable() = FileStatus.Existing.Runnable(file)
fun FileStatus.Existing.toRunning() = FileStatus.Existing.Running(file)
fun FileStatus.Existing.toTerminated() = FileStatus.Existing.Terminated(file)

/*
 * Valid states for a single edit panel:
 * 1. New file, not edited - after creating a new file, but before making
 *    any changes to it.
 * 2. New file, edited - new file, but after making changes to it.
 * 3. Not edited - after opening a file, but before making any changes to it.
 * 4. Edited - after making changes to a file.
 * 
 * What happens when a new file button is clicked?
 * We don't have an actual file associated with such a tab...
 */
