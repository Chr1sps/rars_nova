package rars.settings

import rars.util.FontWeight
import java.awt.Font

interface FontSettings {
    val fontWeight: FontWeight
    val isLigaturized: Boolean
    val currentFont: Font
    val fontFamily: String
    val fontSize: Int
}