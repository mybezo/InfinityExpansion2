package net.guizhanss.infinityexpansion2.implementation.items.machines

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu
import net.guizhanss.guizhanlib.kt.minecraft.items.edit
import net.guizhanss.infinityexpansion2.core.items.attributes.InformationalRecipeDisplayItem
import net.guizhanss.infinityexpansion2.core.menu.MenuLayout
import net.guizhanss.infinityexpansion2.implementation.items.machines.abstracts.AbstractTickingMachine
import net.guizhanss.infinityexpansion2.utils.items.GuiItems
import net.guizhanss.infinityexpansion2.utils.slimefunext.pushAvailable
import org.bukkit.World.Environment
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

class GeoQuarry(
    itemGroup: ItemGroup,
    itemStack: SlimefunItemStack,
    recipeType: RecipeType,
    recipe: Array<out ItemStack?>,
    energyPerTick: Int,
    val defaultOutputInterval: Int,
    val speed: Int, // the amount of output
) : AbstractTickingMachine(itemGroup, itemStack, recipeType, recipe, MenuLayout.OUTPUT_ONLY, energyPerTick),
    InformationalRecipeDisplayItem {

    private val outputIntervalSetting = IntRangeSetting(this, "output-interval", 1, defaultOutputInterval, 3600)

    init {
        addItemSetting(outputIntervalSetting)
    }

    // compressed machines use the output interval of the original machine
    private val outputInterval: Int
        get() = ((timeSource as? GeoQuarry)?.outputIntervalSetting ?: outputIntervalSetting).value

    override fun process(b: Block, menu: BlockMenu): Boolean {
        menu.setStatus { GuiItems.PRODUCING }
        if (!shouldProduce()) return true

        val product = produce(menu) ?: return true
        if (menu.pushAvailable(product, speed.toLong() * multiplier, outputSlots) == 0L) {
            menu.setStatus { GuiItems.NO_ROOM }
            return false
        }

        return true
    }

    private fun shouldProduce() =
        tickCount % (getCustomTickRate() * outputInterval) == 0

    private fun produce(menu: BlockMenu): ItemStack? {
        val biome = menu.location.block.biome
        val env = menu.location.world.environment

        return getProductPool(biome, env).randomOrNull()
    }

    private fun getProductPool(biome: Biome, env: Environment): List<ItemStack> =
        geoRecipes.getOrPut(Pair(biome, env)) {
            val pool = mutableListOf<ItemStack>()
            Slimefun.getRegistry().geoResources.values().filter { it.isObtainableFromGEOMiner }.forEach { resource ->
                val supply = resource.getDefaultSupply(env, biome)
                if (supply > 0) {
                    repeat(supply) {
                        pool.add(resource.item)
                    }
                }
            }
            pool
        }

    override fun getDefaultDisplayRecipes() =
        Slimefun.getRegistry().geoResources.values().filter { it.isObtainableFromGEOMiner }
            .map { it.item.edit { amount(speed) } }

    override fun getInfoItems() = listOf(
        GuiItems.tickRate(getCustomTickRate()),
        GuiItems.energyConsumptionPerTick(getEnergyConsumptionPerTick()),
        GuiItems.outputInterval(outputInterval)
    )

    override fun getDividerItem() = GuiItems.RECIPES

    companion object {

        private val geoRecipes = mutableMapOf<Pair<Biome, Environment>, List<ItemStack>>()
    }
}
