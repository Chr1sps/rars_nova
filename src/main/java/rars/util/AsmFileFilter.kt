package rars.util

import rars.Globals
import java.io.File
import javax.swing.filechooser.FileFilter

object AsmFileFilter : FileFilter() {
    override fun accept(f: File): Boolean =
        f.isDirectory || f.extension in Globals.FILE_EXTENSIONS

    override fun getDescription(): String =
        """Assembler files (${Globals.FILE_EXTENSIONS.joinToString()})"""
}