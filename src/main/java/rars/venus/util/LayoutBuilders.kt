package rars.venus.util

import org.intellij.lang.annotations.MagicConstant
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class BorderLayoutScope internal constructor(
    private val panel: JPanel,
    initialVgap: Int,
    initialHgap: Int,
) {
    val layoutManager = BorderLayout(initialHgap, initialVgap).also {
        panel.layout = it
    }

    var vgap by layoutManager::vgap
    var hgap by layoutManager::hgap

    operator fun set(
        @MagicConstant(
            stringValues = [
                BorderLayout.NORTH,
                BorderLayout.SOUTH,
                BorderLayout.WEST,
                BorderLayout.EAST,
                BorderLayout.CENTER
            ]
        )
        index: String,
        component: JComponent
    ) {
        index.assertValid()
        panel.add(component, index)
    }

    private fun String.assertValid() {
        val isValidId = when (this) {
            BorderLayout.NORTH,
            BorderLayout.SOUTH,
            BorderLayout.WEST,
            BorderLayout.EAST,
            BorderLayout.CENTER,
                -> true
            else -> false
        }
        assert(isValidId)
    }
}

fun JPanel.BorderLayout(
    vgap: Int = 0,
    hgap: Int = 0,
    builder: BorderLayoutScope.() -> Unit,
) {
    BorderLayoutScope(this, vgap, hgap).apply(builder)
}

class BoxLayoutBuilder(
    private val panel: JPanel,
    @MagicConstant(valuesFromClass = BoxLayout::class)
    axis: Int
) {
    init {
        BoxLayout(panel, axis).also {
            panel.layout = it
        }
    }

    operator fun Component.unaryPlus() {
        panel.add(this@unaryPlus)
    }

    fun horizontalStrut(width: Int) {
        panel.add(Box.createHorizontalStrut(width))
    }

    fun horizontalGlue() {
        panel.add(Box.createHorizontalGlue())
    }

    fun verticalStrut(height: Int) {
        panel.add(Box.createVerticalStrut(height))
    }

    fun verticalGlue() {
        panel.add(Box.createVerticalGlue())
    }
}

inline fun JPanel.BoxLayout(
    @MagicConstant(valuesFromClass = BoxLayout::class)
    axis: Int,
    builder: BoxLayoutBuilder.() -> Unit,
) {
    BoxLayoutBuilder(this, axis).apply(builder)
}
