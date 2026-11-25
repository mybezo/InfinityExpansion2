package net.guizhanss.infinityexpansion2.core.items.attributes

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem

/**
 * A machine or generator that can have compressed versions (multiple machines combined into one).
 */
interface Compressible {

    /**
     * The amount of machines combined into this one. It is 1 for normal machines.
     * Every operation produces (and consumes) this many times the normal amount.
     * It must only be changed before the item is registered.
     */
    var multiplier: Int

    /**
     * The original item of a compressed item. Compressed items use the time settings
     * (tick rate, output interval, durations, machine time) of the original item.
     * It is null for normal items, and must only be changed before the item is registered.
     */
    var timeSource: SlimefunItem?
        get() = null
        set(@Suppress("UNUSED_PARAMETER") value) {}
}
