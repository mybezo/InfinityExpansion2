package net.guizhanss.infinityexpansion2.core.items.attributes

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
}
