package net.guizhanss.infinityexpansion2.implementation.items.sfextension

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricSmeltery
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu
import net.guizhanss.infinityexpansion2.core.items.attributes.Compressible
import net.guizhanss.infinityexpansion2.utils.slimefunext.splitStacks
import org.bukkit.inventory.ItemStack

class Smeltery(
    itemGroup: ItemGroup,
    itemStack: SlimefunItemStack,
    recipeType: RecipeType,
    recipe: Array<out ItemStack?>,
    speed: Int,
    energyPerTick: Int,
) : ElectricSmeltery(itemGroup, itemStack, recipeType, recipe), Compressible {

    /**
     * Compressed smelteries process up to [multiplier] recipes in one operation.
     */
    override var multiplier: Int = 1

    init {
        capacity = energyPerTick
        energyConsumption = energyPerTick
        setProcessingSpeed(speed)
    }

    override fun findNextRecipe(inv: BlockMenu): MachineRecipe? {
        if (multiplier <= 1) return super.findNextRecipe(inv)

        for (recipe in machineRecipes) {
            val required = mergeInputs(recipe.input)
            if (required.isEmpty()) continue

            // how many times the recipe can be done with the available input, up to the multiplier
            var units = multiplier.toLong()
            for ((input, amount) in required) {
                val available = inputSlots.sumOf { slot ->
                    val item = inv.getItemInSlot(slot)
                    if (item != null && SlimefunUtils.isItemSimilar(item, input, true)) item.amount.toLong() else 0L
                }
                units = minOf(units, available / amount)
                if (units <= 0) break
            }
            if (units <= 0) continue

            val outputs = recipe.output.flatMap { splitStacks(it, it.amount.toLong() * units) }
            if (!InvUtils.fitAll(inv.toInventory(), outputs.toTypedArray(), *outputSlots)) {
                // no room for this recipe, the normal machine would also wait
                return null
            }

            for ((input, amount) in required) {
                var remaining = amount * units
                for (slot in inputSlots) {
                    if (remaining <= 0) break
                    val item = inv.getItemInSlot(slot) ?: continue
                    if (!SlimefunUtils.isItemSimilar(item, input, true)) continue
                    val consume = minOf(item.amount.toLong(), remaining).toInt()
                    inv.consumeItem(slot, consume)
                    remaining -= consume
                }
            }

            // same duration as one recipe (ticks = seconds * 2)
            return MachineRecipe(recipe.ticks / 2, recipe.input, outputs.toTypedArray())
        }
        return null
    }

    /**
     * Merge the similar recipe inputs, returns the input item and the total amount required for one recipe.
     */
    private fun mergeInputs(inputs: Array<out ItemStack?>): List<Pair<ItemStack, Long>> {
        val result = mutableListOf<Pair<ItemStack, Long>>()
        for (input in inputs) {
            if (input == null || input.type.isAir) continue
            val index = result.indexOfFirst { SlimefunUtils.isItemSimilar(it.first, input, true) }
            if (index >= 0) {
                result[index] = result[index].first to result[index].second + input.amount
            } else {
                result.add(input to input.amount.toLong())
            }
        }
        return result
    }
}
