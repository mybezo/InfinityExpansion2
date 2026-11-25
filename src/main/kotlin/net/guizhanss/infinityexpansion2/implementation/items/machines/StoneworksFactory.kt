package net.guizhanss.infinityexpansion2.implementation.items.machines

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu
import net.guizhanss.guizhanlib.kt.minecraft.extensions.toItem
import net.guizhanss.guizhanlib.kt.slimefun.extensions.isSlimefunItem
import net.guizhanss.infinityexpansion2.core.config.MachineTime
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
 * Generates cobblestone and processes it into various materials, with a quarry-like menu.
 *
 * The player puts the wanted block into the selector slot (it is not consumed),
 * and the factory produces it directly from cobblestone. An empty selector produces cobblestone.
 * The products are the materials that can be made from cobblestone within 3 [Process]es.
 */
class StoneworksFactory(
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

        // produces once every cycle (machine-time in config.yml, default 4 operations: generate + 3 processes)
        val cycle = outputOperations
        val step = tickCount / getCustomTickRate() % cycle
        if (step != cycle - 1) {
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

    /**
     * The amount of operations between outputs, from machine-time in config.yml.
     */
    private val outputOperations: Int
        get() = secondsToOperations(MachineTime.get(timeId, CYCLE * MachineTime.secondsPerTick))

    override fun getDefaultDisplayRecipes(): List<ItemStack?> =
        PRODUCTS.map { it.toItem(speed.coerceIn(1, 64)) }

    override fun getInfoItems() = listOf(
        GuiItems.tickRate(getCustomTickRate()),
        GuiItems.energyConsumptionPerTick(getEnergyConsumptionPerTick()),
        GuiItems.outputInterval(outputOperations),
    )

    override fun getDividerItem() = GuiItems.PRODUCES

    companion object {

        // cobblestone is generated and processed in 3 steps, 1 operation each
        private const val CYCLE = 4
        private const val MAX_STEPS = 3

        /**
         * All the materials that can be made from cobblestone within 3 processes.
         */
        val PRODUCTS: Set<Material> by lazy {
            val edges = mutableMapOf<Material, MutableSet<Material>>()
            Process.entries.forEach { process ->
                process.map.forEach { (from, to) -> edges.getOrPut(from) { mutableSetOf() }.add(to) }
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

    /**
     * The processes of the factory, used to decide which materials can be produced.
     */
    private enum class Process(val map: Map<Material, Material>) {
        FURNACE(
            mapOf(
                Material.COBBLESTONE to Material.STONE,
                Material.STONE to Material.SMOOTH_STONE,
                Material.SAND to Material.GLASS,
                Material.STONE_BRICKS to Material.CRACKED_STONE_BRICKS,
            )
        ),
        CRUSH(
            mapOf(
                Material.COBBLESTONE to Material.GRAVEL,
                Material.GRAVEL to Material.SAND,
            )
        ),
        COMPACT(
            mapOf(
                Material.STONE to Material.STONE_BRICKS,
                Material.GRANITE to Material.POLISHED_GRANITE,
                Material.DIORITE to Material.POLISHED_DIORITE,
                Material.ANDESITE to Material.POLISHED_ANDESITE,
                Material.SAND to Material.SANDSTONE,
            )
        ),
        TRANSFORM(
            mapOf(
                Material.COBBLESTONE to Material.ANDESITE,
                Material.ANDESITE to Material.DIORITE,
                Material.DIORITE to Material.GRANITE,
                Material.STONE to Material.DEEPSLATE,
                Material.SAND to Material.SUSPICIOUS_SAND,
                Material.GRAVEL to Material.SUSPICIOUS_GRAVEL,
            )
        ),
    }
}
