package rars.venus.util

import java.awt.Component
import java.awt.Window
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

fun Component.onMouseClicked(action: (MouseEvent) -> Unit) =
    addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent) {
            action(e)
        }

        override fun mousePressed(e: MouseEvent) = Unit
        override fun mouseReleased(e: MouseEvent) = Unit
        override fun mouseEntered(e: MouseEvent) = Unit
        override fun mouseExited(e: MouseEvent) = Unit
    })

fun Window.onWindowClosing(action: (WindowEvent) -> Unit): Unit =
    addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) = action(e)
    })

fun Window.onWindowOpened(action: (WindowEvent) -> Unit): Unit =
    addWindowListener(object : WindowAdapter() {
        override fun windowOpened(e: WindowEvent) = action(e)
    })
    