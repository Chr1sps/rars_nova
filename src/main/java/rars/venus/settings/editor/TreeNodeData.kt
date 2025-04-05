package rars.venus.settings.editor

import rars.settings.TokenSettingKey

open class TreeNodeData(private val display: String) {
    override fun toString(): String = display

    class Syntax(val type: TokenSettingKey, display: String) : TreeNodeData(display)
}
