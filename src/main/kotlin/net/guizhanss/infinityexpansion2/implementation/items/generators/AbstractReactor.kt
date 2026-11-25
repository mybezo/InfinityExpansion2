package net.guizhanss.infinityexpansion2.implementation.items.generators

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu
import net.guizhanss.guizhanlib.kt.slimefun.utils.getInt
import net.guizhanss.guizhanlib.kt.slimefun.utils.setInt
import net.guizhanss.guizhanlib.slimefun.machines.MenuBlock
import net.guizhanss.infinityexpansion2.core.items.attributes.Compressible
import net.guizhanss.infinityexpansion2.core.items.attributes.CustomTickRateMachine
import net.guizhanss.infinityexpansion2.core.items.attributes.EnergyProducer
import net.guizhanss.infinityexpansion2.core.items.attributes.InformationalRecipeDisplayItem
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

abstract class AbstractReactor(
    itemGroup: ItemGroup,
    itemStack: SlimefunItemStack,
    recipeType: RecipeType,
    recipe: Array<out ItemStack?>,
    val defaultProduction: Int,
) : MenuBlock(itemGroup, itemStack, recipeType, recipe), EnergyNetProvider, CustomTickRateMachine, EnergyProducer,
    InformationalRecipeDisplayItem, Compressible {

    /**
     * Compressed reactors produce [multiplier] times the energy and consume [multiplier] times the fuel.
     */
    override var multiplier: Int = 1

    override var timeSource: SlimefunItem? = null

    private val tickRateSetting = IntRangeSetting(this, "tick-rate", 1, 1, 3600)
    private val energyProductionSetting =
        IntRangeSetting(this, "energy-production", 1, defaultProduction, Int.MAX_VALUE)

    init {
        addItemSetting(tickRateSetting, energyProductionSetting)
    }

    override fun onNewInstance(menu: BlockMenu, b: Block) {
        b.location.setProgress(0)
    }

    override fun getOutputSlots() = intArrayOf()

    // compressed items use the tick rate of the original item
    override fun getCustomTickRate(): Int = (timeSource as? AbstractReactor)?.getCustomTickRate() ?: tickRateSetting.value

    override fun getCapacity() = getEnergyProduction()

    override fun getEnergyProduction() = energyProductionSetting.value

    protected fun Location.setProgress(progress: Int) {
        setInt(PROGRESS_KEY, progress)
    }

    protected fun Location.getProgress() = getInt(PROGRESS_KEY)

    /**
     * The amount of combined reactors running in the current cycle (compressed reactors only),
     * decided by the fuel available when the cycle starts.
     */
    protected fun Location.setUnits(units: Int) {
        setInt(UNITS_KEY, units)
    }

    protected fun Location.getUnits() = getInt(UNITS_KEY).let { if (it <= 0) multiplier else it.coerceAtMost(multiplier) }

    /**
     * The energy produced by the reactors running in the current cycle.
     */
    protected fun Location.getUnitsProduction(): Int =
        if (multiplier <= 1) getEnergyProduction()
        else (getEnergyProduction().toLong() / multiplier * getUnits()).toInt()

    companion object {

        private const val PROGRESS_KEY = "progress"
        private const val UNITS_KEY = "units"
    }
}
