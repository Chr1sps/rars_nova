package rars.venus.util

import java.awt.event.MouseEvent
import java.awt.event.MouseListener

typealias MouseListenerCallback = (MouseEvent) -> Unit

private val NOOP: MouseListenerCallback = {}

interface MouseListenerBuilder {
    fun onMouseClicked(action: (MouseEvent) -> Unit): MouseListenerBuilder
    fun onMousePressed(action: (MouseEvent) -> Unit): MouseListenerBuilder
    fun onMouseReleased(action: (MouseEvent) -> Unit): MouseListenerBuilder
    fun onMouseEntered(action: (MouseEvent) -> Unit): MouseListenerBuilder
    fun onMouseExited(action: (MouseEvent) -> Unit): MouseListenerBuilder
    fun build(): MouseListener
    companion object {
        @JvmStatic
        fun create(): MouseListenerBuilder = MouseListenerBuilderImpl()
    }
}

private class MouseListenerBuilderImpl : MouseListenerBuilder {
    var mouseClickedCallback: (MouseEvent) -> Unit = NOOP
    var mousePressedCallback: (MouseEvent) -> Unit = NOOP
    var mouseReleasedCallback: (MouseEvent) -> Unit = NOOP
    var mouseEnteredCallback: (MouseEvent) -> Unit = NOOP
    var mouseExitedCallback: (MouseEvent) -> Unit = NOOP
    override fun onMouseClicked(action: (MouseEvent) -> Unit): MouseListenerBuilder = apply {
        mouseClickedCallback = action
    }

    override fun onMousePressed(action: (MouseEvent) -> Unit): MouseListenerBuilder = apply {
        mousePressedCallback = action
    }

    override fun onMouseReleased(action: (MouseEvent) -> Unit): MouseListenerBuilder = apply {
        mouseReleasedCallback = action
    }

    override fun onMouseEntered(action: (MouseEvent) -> Unit): MouseListenerBuilder = apply {
        mouseEnteredCallback = action
    }

    override fun onMouseExited(action: (MouseEvent) -> Unit): MouseListenerBuilder = apply {
        mouseExitedCallback = action
    }

    override fun build(): MouseListener = DelegatedMouseListener(
        mouseClickedCallback,
        mousePressedCallback,
        mouseReleasedCallback,
        mouseEnteredCallback,
        mouseExitedCallback,
    )
}

private class DelegatedMouseListener(
    private val _mouseClicked: MouseListenerCallback,
    private val _mousePressed: MouseListenerCallback,
    private val _mouseReleased: MouseListenerCallback,
    private val _mouseEntered: MouseListenerCallback,
    private val _mouseExited: MouseListenerCallback,
) : MouseListener {
    override fun mouseClicked(e: MouseEvent) = _mouseClicked(e)
    override fun mousePressed(e: MouseEvent) = _mousePressed(e)
    override fun mouseReleased(e: MouseEvent) = _mouseReleased(e)
    override fun mouseEntered(e: MouseEvent) = _mouseEntered(e)
    override fun mouseExited(e: MouseEvent) = _mouseExited(e)
}

fun MouseListener(builderFunc: MouseListenerBuilder.() -> Unit): MouseListener =
    MouseListenerBuilderImpl().apply(builderFunc).build()

val EmptyMouseListener: MouseListener = DelegatedMouseListener(NOOP, NOOP, NOOP, NOOP, NOOP)
