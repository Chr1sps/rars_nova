package rars.venus.actions

import java.util.concurrent.atomic.AtomicInteger

interface ActionsRegistry {
    operator fun get(key: ActionKey): GuiAction?
    fun getFromMnemonic(mnemonic: Int): GuiAction?
    fun getFromAccel(accel: Int): GuiAction?
}

//class ActionsRegistry {
//    private val actions = mutableMapOf<ActionKey, GuiAction>()
//
////    fun createAction(
////
////    ): ActionKey {
////        val actionKey = ActionKey.create()
////        return actionKey
////    }
//
//    fun registerAction(
//        action: GuiAction
//    ): ActionKey = ActionKey.create().also {
//        actions[it] = action
//    }
//
//    fun enableOnly(vararg actionKeys: ActionKey) {
//        val actionKeysSet = actionKeys.toSet()
//        actions.entries.forEach { (key, action) ->
//            action.isEnabled = key in actionKeysSet
//        }
//    }
//
//    operator fun get(key: ActionKey): GuiAction? = actions[key]
//}
//
//fun ActionsRegistry.createAction(
//    name: String,
//    description: String,
//    icon: Icon? = null,
//    mnemonic: Int? = null,
//    accel: Int? = null,
//    isEnabled: Boolean = true,
//    actionPerformed: (GuiAction) -> Unit = {},
//): ActionKey = object : GuiAction(
//    name = name,
//    icon = icon,
//    description = description,
//    mnemonic = mnemonic,
//    accel = accel?.let { KeyStroke.getKeyStroke(it, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx) },
//    mainUI = TODO()
//) {
//    init {
//        this.isEnabled = isEnabled
//    }
//
//    override fun actionPerformed(e: ActionEvent) = actionPerformed(this)
//}.let { registerAction(it) }

interface ActionKey {
    companion object {
        fun create(): ActionKey = ActionKeyImpl()
    }
}

private class ActionKeyImpl : ActionKey {
    private val id = counter.getAndIncrement()

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = id

    companion object {
        private val counter = AtomicInteger(0)
    }
}
