package net.guizhanss.infinityexpansion2.utils.slimefunext

import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.ItemUtils
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu
import org.bukkit.inventory.ItemStack

/**
 * Split the [total] amount of [item] into valid stacks (never larger than the max stack size).
 */
fun splitStacks(item: ItemStack, total: Long): List<ItemStack> {
    if (total <= 0) return emptyList()
    val max = item.maxStackSize.coerceAtLeast(1)
    val result = ArrayList<ItemStack>(((total + max - 1) / max).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    var remaining = total
    while (remaining > 0) {
        val amount = remaining.coerceAtMost(max.toLong()).toInt()
        result.add(item.clone().apply { this.amount = amount })
        remaining -= amount
    }
    return result
}

/**
 * Push all the outputs into the [slots], each output is a template item and the total amount.
 * The amount can be larger than the max stack size, it will be split into valid stacks.
 *
 * Nothing is pushed if not all the outputs fit, and false is returned.
 */
fun BlockMenu.pushAll(outputs: List<Pair<ItemStack, Long>>, slots: IntArray): Boolean {
    var stackCount = 0L
    for ((item, total) in outputs) {
        if (total <= 0) continue
        val max = item.maxStackSize.coerceAtLeast(1)
        stackCount += (total + max - 1) / max
        // fast fail: more stacks than slots can never fit
        if (stackCount > slots.size) return false
    }
    if (stackCount == 0L) return true

    val stacks = outputs.flatMap { (item, total) -> splitStacks(item, total) }
    if (stacks.size == 1) {
        if (!fits(stacks[0], *slots)) return false
    } else if (!InvUtils.fitAll(toInventory(), stacks.toTypedArray(), *slots)) {
        return false
    }
    stacks.forEach { pushItem(it, *slots) }
    return true
}

/**
 * Push the [total] amount of [item] into the [slots]. See [pushAll].
 */
fun BlockMenu.pushAll(item: ItemStack, total: Long, slots: IntArray) = pushAll(listOf(item to total), slots)

/**
 * Get the free space for the [item] in the [slots].
 */
fun BlockMenu.freeSpace(item: ItemStack, slots: IntArray): Long {
    val max = item.maxStackSize.coerceAtLeast(1)
    var space = 0L
    for (slot in slots) {
        val current = getItemInSlot(slot)
        if (current == null || current.type.isAir) {
            space += max
        } else if (current.amount < max && ItemUtils.canStack(current, item)) {
            space += max - current.amount
        }
    }
    return space
}

/**
 * Push as much as possible of the outputs into the [slots], the rest is not produced.
 * This is used by machines that do not consume inputs, so nothing is lost.
 *
 * Returns the amount of each output that is actually pushed, in the same order.
 */
fun BlockMenu.pushAvailable(outputs: List<Pair<ItemStack, Long>>, slots: IntArray): List<Long> =
    outputs.map { (item, total) ->
        if (total <= 0 || item.type.isAir) return@map 0L
        val amount = total.coerceAtMost(freeSpace(item, slots))
        splitStacks(item, amount).forEach { pushItem(it, *slots) }
        amount
    }

/**
 * Push as much as possible of the [total] amount of [item]. See [pushAvailable].
 */
fun BlockMenu.pushAvailable(item: ItemStack, total: Long, slots: IntArray): Long =
    pushAvailable(listOf(item to total), slots)[0]
