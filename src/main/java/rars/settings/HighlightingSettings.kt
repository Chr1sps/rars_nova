package rars.settings

import rars.venus.editors.TokenStyle

interface HighlightingSettings {
    val textSegmentHighlightingStyle: TokenStyle
    val registerHighlightingStyle: TokenStyle?
    val dataSegmentHighlightingStyle: TokenStyle?
}