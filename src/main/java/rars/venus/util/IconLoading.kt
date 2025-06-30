package rars.venus.util

import rars.Globals
import java.awt.Toolkit
import javax.swing.ImageIcon

fun loadIcon(name: String): ImageIcon {
    val resource = object {}.javaClass.getResource(Globals.IMAGES_PATH + name)
    return ImageIcon(Toolkit.getDefaultToolkit().getImage(resource))
}
