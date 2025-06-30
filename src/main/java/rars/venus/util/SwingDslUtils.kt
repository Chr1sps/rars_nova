package rars.venus.util

import java.awt.event.KeyEvent
import javax.swing.*

class MenuBarBuilder {
    private val menuBar = JMenuBar()

    fun Menu(
        text: String,
        mnemonic: Int = KeyEvent.VK_UNDEFINED,
        content: MenuBuilder.() -> Unit,
    ) {
        menuBar.add(MenuBuilder(text, mnemonic).apply(content).build())
    }

    internal fun build() = menuBar
}

class MenuBuilder(
    text: String,
    mnemonic: Int = KeyEvent.VK_UNDEFINED
) {
    private val menu = JMenu().apply {
        this.text = text
        this.mnemonic = mnemonic
    }

    @JvmOverloads
    fun Item(
        text: String,
        icon: Icon? = null,
        isEnabledState: State<Boolean> = stateOf(true),
        mnemonic: Int = KeyEvent.VK_UNDEFINED,
        shortcut: KeyStroke? = null,
        onClick: () -> Unit,
    ) {
        val item = object : JMenuItem() {
            init {
                this.icon = icon
                this.text = text
                this.mnemonic = mnemonic
                accelerator = shortcut
                addActionListener { onClick() }
            }

            override fun isEnabled() = isEnabledState.value
        }
        menu.add(item)
    }

    @JvmOverloads
    fun CheckboxItem(
        text: String,
        isChecked: Boolean,
        icon: Icon? = null,
        isEnabledState: State<Boolean> = stateOf(true),
        mnemonic: Int = KeyEvent.VK_UNDEFINED,
        shortcut: KeyStroke? = null,
        onChecked: (Boolean) -> Unit,
    ) {
        val item = object : JCheckBoxMenuItem() {
            init {
                this.text = text
                isSelected = isChecked
                this.icon = icon
                this.mnemonic = mnemonic
                accelerator = shortcut
                addActionListener { onChecked(isSelected) }
            }

            override fun isEnabled() = isEnabledState.value
        }
        menu.add(item)
    }

    fun Separator() {
        menu.addSeparator()
    }

    internal fun build() = menu
}

fun JFrame.MenuBar(builderFunc: MenuBarBuilder.() -> Unit) {
    jMenuBar = MenuBarBuilder().apply(builderFunc).build()
}

interface State<T> {
    val value: T
}

private data class StateImpl<T>(override val value: T) : State<T>

interface MutableState<T> : State<T> {
    override var value: T
}

private data class MutableStateImpl<T>(override var value: T) : MutableState<T>

fun <T> stateOf(value: T): State<T> = StateImpl(value)
fun <T> mutableStateOf(value: T): MutableState<T> = MutableStateImpl(value)

private class DelegateState<T>(private val provider: () -> T) : State<T> {
    override val value: T get() = provider()
}

fun <T> delegateState(provider: () -> T): State<T> = object : State<T> {
    override val value: T get() = provider()
}
