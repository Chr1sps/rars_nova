@file:OptIn(ExperimentalContracts::class)

package rars.venus.util

import org.intellij.lang.annotations.MagicConstant
import java.awt.*
import java.io.Serializable
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class BorderLayoutScope internal constructor(
    private val container: Container,
    vgap: Int,
    hgap: Int,
) {
    init {
        container.layout = BorderLayout(hgap, vgap)
    }

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
        component: Component
    ) {
        index.assertValid()
        container.add(component, index)
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

fun Container.BorderLayout(
    vgap: Int = 0,
    hgap: Int = 0,
    builder: BorderLayoutScope.() -> Unit,
) {
    BorderLayoutScope(this, vgap, hgap).apply(builder)
}

interface AppendableLayoutBuilder {
    operator fun Component.unaryPlus()
}

class BoxLayoutBuilder(
    private val container: Container,
    @MagicConstant(valuesFromClass = BoxLayout::class)
    axis: Int
) : AppendableLayoutBuilder {
    init {
        BoxLayout(container, axis).also {
            container.layout = it
        }
    }

    override operator fun Component.unaryPlus() {
        container.add(this@unaryPlus)
    }

    fun horizontalStrut(width: Int) {
        container.add(Box.createHorizontalStrut(width))
    }

    fun horizontalGlue() {
        container.add(Box.createHorizontalGlue())
    }

    fun verticalStrut(height: Int) {
        container.add(Box.createVerticalStrut(height))
    }

    fun verticalGlue() {
        container.add(Box.createVerticalGlue())
    }
}

inline fun Container.BoxLayout(
    @MagicConstant(valuesFromClass = BoxLayout::class)
    axis: Int,
    builder: BoxLayoutBuilder.() -> Unit,
) {
    BoxLayoutBuilder(this, axis).apply(builder)
}

class FlowLayoutBuilder(
    private val container: Container,
    vgap: Int,
    hgap: Int,
    alignment: Int,
) : AppendableLayoutBuilder {
    init {
        container.layout = FlowLayout(alignment, hgap, vgap)
    }

    override operator fun Component.unaryPlus() {
        container.add(this@unaryPlus)
    }
}

inline fun Container.FlowLayout(
    @MagicConstant(valuesFromClass = FlowLayout::class)
    alignment: Int = FlowLayout.CENTER,
    vgap: Int = 5,
    hgap: Int = 5,
    builderFunc: FlowLayoutBuilder.() -> Unit
) {
    FlowLayoutBuilder(this, vgap, hgap, alignment).apply(builderFunc)
}

class GridLayoutBuilder(
    private val container: Container,
    private val rows: Int,
    private val cols: Int,
    private val hgap: Int,
    private val vgap: Int,
) {
    private data object EmptyPanel : JPanel(), Serializable {
        private fun readResolve(): Any = EmptyPanel
    }

    private val data = Array(rows) {
        Array<Component>(cols) { EmptyPanel }
    }

    operator fun set(row: Int, col: Int, component: Component) {
        require(row in 0 until rows) { "Row index out of bounds: $row" }
        require(col in 0 until cols) { "Column index out of bounds: $col" }
        data[row][col] = component
    }

    internal fun build() {
        container.layout = GridLayout(rows, cols, hgap, vgap)
        for (row in data) {
            for (component in row) {
                container.add(component)
            }
        }
    }
}

fun Container.GridLayout(
    rows: Int,
    cols: Int,
    hgap: Int = 0,
    vgap: Int = 0,
    builderFunc: GridLayoutBuilder.() -> Unit
) {
    GridLayoutBuilder(this, rows, cols, hgap, vgap)
        .apply(builderFunc)
        .build()
}

inline fun JPanel(builderFunc: JPanel.() -> Unit): JPanel {
    contract {
        callsInPlace(builderFunc, InvocationKind.EXACTLY_ONCE)
    }
    return javax.swing.JPanel().apply(builderFunc)
}

private fun GridBagConstraints.fork(
    modifyFunc: GridBagConstraints.() -> Unit
): GridBagConstraints = (clone() as GridBagConstraints).apply(modifyFunc)

class GridBagLayoutBuilder(
    private val container: Container,
    private val baseConstraints: GridBagConstraints,
) {
    init {
        container.layout = GridBagLayout()
    }

    fun add(
        component: Component,
        gridx: Int = baseConstraints.gridx,
        gridy: Int = baseConstraints.gridy,
        gridwidth: Int = baseConstraints.gridwidth,
        gridheight: Int = baseConstraints.gridheight,
        weightx: Double = baseConstraints.weightx,
        weighty: Double = baseConstraints.weighty,
        anchor: Int = baseConstraints.anchor,
        fill: Int = baseConstraints.fill,
        insets: Insets = baseConstraints.insets,
        ipadx: Int = baseConstraints.ipadx,
        ipady: Int = baseConstraints.ipady,
    ) {
        val newConstraints = baseConstraints.fork {
            this.gridx = gridx
            this.gridy = gridy
            this.gridwidth = gridwidth
            this.gridheight = gridheight
            this.weightx = weightx
            this.weighty = weighty
            this.anchor = anchor
            this.fill = fill
            this.insets = insets
            this.ipadx = ipadx
            this.ipady = ipady
        }
        container.add(component, newConstraints)
    }
}

fun Container.GridBagLayout(
    baseConstraints: GridBagConstraints,
    builderFunc: GridBagLayoutBuilder.() -> Unit
) {
    GridBagLayoutBuilder(this, baseConstraints).apply(builderFunc)
}

class CardLayoutBuilder(
    hgap: Int,
    vgap: Int,
    private val container: Container,
) {
    internal val layout = CardLayout(hgap, vgap)

    init {
        container.layout = layout
    }

    operator fun set(cardName: String, component: Component) {
        container.add(component, cardName)
    }
}

fun Container.CardLayout(
    hgap: Int = 0,
    vgap: Int = 0,
    builderFunc: CardLayoutBuilder.() -> Unit
): CardLayout {
    contract {
        callsInPlace(builderFunc, InvocationKind.EXACTLY_ONCE)
    }
    return CardLayoutBuilder(hgap, vgap, this).apply(builderFunc).layout
}