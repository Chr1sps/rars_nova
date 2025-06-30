package rars.venus.util

import rars.assembler.Symbol

private const val ASCENDING_SYMBOL = '▲'
private const val DESCENDING_SYMBOL = '▼'

private val LabelAddressAscendingComparator: Comparator<Symbol> =
    compareBy { it.address.toULong() }

private val LabelNameAscendingComparator: Comparator<Symbol> =
    compareBy { it.name.lowercase() }

enum class LabelsSortState(val value: Byte) {
    LabelsAscending(0b00) {
        override fun stateOnAddressClick(): LabelsSortState = AddressesAscending
        override fun stateOnLabelClick(): LabelsSortState = LabelsDescending
        override val comparator: Comparator<Symbol> =
            LabelNameAscendingComparator
    },
    LabelsDescending(0b01) {
        override fun stateOnAddressClick(): LabelsSortState = AddressesAscending
        override fun stateOnLabelClick(): LabelsSortState = LabelsAscending
        override val comparator: Comparator<Symbol> =
            LabelNameAscendingComparator.reversed()
    },
    AddressesAscending(0b10) {
        override fun stateOnAddressClick(): LabelsSortState =
            AddressesDescending

        override fun stateOnLabelClick(): LabelsSortState = LabelsAscending
        override val comparator: Comparator<Symbol> =
            LabelAddressAscendingComparator
    },
    AddressesDescending(0b11) {
        override fun stateOnAddressClick(): LabelsSortState = AddressesAscending
        override fun stateOnLabelClick(): LabelsSortState = LabelsAscending
        override val comparator: Comparator<Symbol> =
            LabelAddressAscendingComparator.reversed()
    };

    abstract fun stateOnAddressClick(): LabelsSortState
    abstract fun stateOnLabelClick(): LabelsSortState
    abstract val comparator: Comparator<Symbol>

    companion object {
        @JvmStatic
        fun fromInt(value: Int) =
            entries.firstOrNull { it.value.toInt() == value }
    }

}

internal fun columnNamesFor(state: LabelsSortState) = when (state) {
    LabelsSortState.LabelsAscending -> arrayOf(
        "Label  $ASCENDING_SYMBOL",
        "Address"
    )
    LabelsSortState.LabelsDescending -> arrayOf(
        "Label  $DESCENDING_SYMBOL",
        "Address"
    )
    LabelsSortState.AddressesAscending -> arrayOf(
        "Label",
        "Address  $ASCENDING_SYMBOL"
    )
    LabelsSortState.AddressesDescending -> arrayOf(
        "Label",
        "Address  $DESCENDING_SYMBOL"
    )
}
