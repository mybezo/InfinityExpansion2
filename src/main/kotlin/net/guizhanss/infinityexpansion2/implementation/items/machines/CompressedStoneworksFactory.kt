package net.guizhanss.infinityexpansion2.implementation.items.machines

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu
import net.guizhanss.guizhanlib.kt.minecraft.extensions.toItem
import net.guizhanss.guizhanlib.kt.slimefun.extensions.isSlimefunItem
import net.guizhanss.infinityexpansion2.core.items.attributes.CustomWikiItem
import net.guizhanss.infinityexpansion2.core.items.attributes.InformationalRecipeDisplayItem
import net.guizhanss.infinityexpansion2.core.menu.MenuLayout
import net.guizhanss.infinityexpansion2.implementation.items.machines.abstracts.AbstractTickingMachine
import net.guizhanss.infinityexpansion2.utils.items.GuiItems
import net.guizhanss.infinityexpansion2.utils.slimefunext.pushAvailable
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/**
 * The compressed version of [StoneworksFactory], with a quarry-like menu.
 *
 * Instead of choosing the 3 processes, the player puts the wanted block into the selector slot
 * (it is not consumed), and the factory produces it directly from cobblestone.
 * An empty selector produces cobblestone.
 */
class CompressedStoneworksFactory(
    itemGroup: ItemGroup,
    itemStack: SlimefunItemStack,
    recipeType: RecipeType,
    recipe: Array<out ItemStack?>,
    val speed: Int,
    energyPerTick: Int,
) : AbstractTickingMachine(itemGroup, itemStack, recipeType, recipe, MenuLayout.SINGLE_INPUT, energyPerTick),
    InformationalRecipeDisplayItem, CustomWikiItem {

    override val wikiUrl = "machines/stoneworks-factory"

    override fun setup(preset: BlockMenuPreset) {
        super.setup(preset)
        preset.drawBackground(GuiItems.SW_SELECTOR_SLOT, layout.inputBorder)
    }

    // the selector is set by players only, cargo cannot insert into it
    override fun getInputSlots(menu: DirtyChestMenu, item: ItemStack) = intArrayOf()

    override fun process(b: Block, menu: BlockMenu): Boolean {
        val selector = menu.getItemInSlot(inputSlots[0])
        val product = when {
            selector == null || selector.type.isAir -> Material.COBBLESTONE
            !selector.isSlimefunItem() && selector.type in PRODUCTS -> selector.type
            else -> {
                menu.setStatus { GuiItems.INVALID_INPUT }
                return false
            }
        }

        // same cadence as the normal factory: produces once every 4 operations
        val step = tickCount / getCustomTickRate() % CYCLE
        if (step != CYCLE - 1) {
            menu.setStatus { GuiItems.PRODUCING }
            return true
        }

        if (menu.pushAvailable(product.toItem(), speed.toLong() * multiplier, outputSlots) == 0L) {
            menu.setStatus { GuiItems.NO_ROOM }
            return false
        }
        menu.setStatus { GuiItems.PRODUCING }
        return true
    }

    override fun getDefaultDisplayRecipes(): List<ItemStack?> =
        PRODUCTS.map { it.toItem(speed.coerceIn(1, 64)) }

    override fun getInfoItems() = listOf(
        GuiItems.tickRate(getCustomTickRate()),
        GuiItems.energyConsumptionPerTick(getEnergyConsumptionPerTick()),
    )

    override fun getDividerItem() = GuiItems.PRODUCES

    companion object {

        // the normal factory generates cobblestone and processes it in 3 steps, 1 operation each
        private const val CYCLE = 4
        private const val MAX_STEPS = 3

        /**
         * All the materials the normal factory can make from cobblestone within 3 processes.
         */
        val PRODUCTS: Set<Material> by lazy {
            val edges = mutableMapOf<Material, MutableSet<Material>>()
            StoneworksFactory.Choice.entries.forEach { choice ->
                choice.map.forEach { (from, to) -> edges.getOrPut(from) { mutableSetOf() }.add(to) }
            }

            val result = linkedSetOf(Material.COBBLESTONE)
            var current = setOf(Material.COBBLESTONE)
            repeat(MAX_STEPS) {
                current = current.flatMap { edges[it].orEmpty() }.filter { it !in result }.toSet()
                result.addAll(current)
            }
            result
        }
    }
}
