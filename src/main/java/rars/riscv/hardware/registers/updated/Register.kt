package rars.riscv.hardware.registers.updated

import rars.notices.AccessType
import rars.util.Listener
import rars.util.Subscribable
import rars.util.SubscriptionHandle
import rars.util.SubscriptionManager

interface Register<T> {
    val number: Int?
    val name: String
    var value: T
}

abstract class ReadOnlyRegister<T> : Register<T> {
    final override var value: T
        get() = readOnlyValue
        set(_) {}
    protected abstract val readOnlyValue: T
}

interface SubscribableRegister<T, D> : Register<T>, Subscribable<D> {
    val silentView: Register<T>
}

class SubscribableRegisterWrapper<T, R : Register<T>, D>(
    override val silentView: R,
) : SubscribableRegister<T, D>, Register<T> by silentView {
    init {
        require(silentView !is SubscribableRegister<*, *>) {
            "Cannot wrap a SubscribableRegister."
        }
    }

    override var value: T
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun subscribe(listener: Listener<D>): SubscriptionHandle<D> {
        TODO("Not yet implemented")
    }

    override fun unsubscribe(handle: SubscriptionHandle<D>) {
        TODO("Not yet implemented")
    }

}

abstract class AbstractSubscribableRegister<T, D> : SubscribableRegister<T, D> {
    protected abstract val manager: SubscriptionManager<D>
    protected abstract var silentValue: T
    override fun subscribe(listener: Listener<D>): SubscriptionHandle<D> = manager.addListener(listener)
    override fun unsubscribe(handle: SubscriptionHandle<D>) = manager.removeListener(handle)
    protected abstract fun createData(accessType: AccessType): D
    final override var value: T
        get() {
            manager.dispatch(createData(AccessType.READ))
            return silentValue
        }
        set(value) {
            silentValue = value
            manager.dispatch(createData(AccessType.WRITE))
        }

    private inner class SilentView : Register<T> {
        override val number: Int? by this@AbstractSubscribableRegister::number
        override val name: String by this@AbstractSubscribableRegister::name
        override var value by ::silentValue
    }

    final override val silentView: Register<T> = SilentView()
}

data class BasicRegister<T>(
    override val number: Int?,
    override val name: String,
    override var value: T,
) : Register<T>

class LinkedLongRegister(
    override val name: String,
    override val number: Int?,
    private val base: Register<Long>,
    private val mask: Long,
) : Register<Long> {
    private val shift = if (mask == 0L) 0 else 64 - mask.countTrailingZeroBits()

    override var value
        get() = (base.value and mask) ushr shift
        set(value) {
            val old = base.value
            base.value = ((value shl shift) and mask) or (old and mask.inv())
        }
}

class LinkedIntRegister(
    override val name: String,
    override val number: Int?,
    private val base: Register<Int>,
    private val mask: Int,
) : Register<Int> {
    private val shift = if (mask == 0) 0 else 64 - mask.countTrailingZeroBits()

    override var value
        get() = (base.value and mask) ushr shift
        set(value) {
            val old = base.value
            base.value = ((value shl shift) and mask) or (old and mask.inv())
        }
}

class MaskedLongRegister(
    override val name: String,
    override val number: Int?,
    initialValue: Long,
    private val mask: Long,
) : Register<Long> {
    override var value = initialValue
        set(value) {
            field = (value and mask) or (field and mask.inv())
        }
}
