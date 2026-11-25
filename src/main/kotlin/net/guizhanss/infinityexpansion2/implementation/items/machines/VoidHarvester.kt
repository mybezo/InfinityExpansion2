package net.guizhanss.infinityexpansion2.implementation.items.machines

import io.github.schntgaispock.slimehud.util.HudBuilder
import io.github.schntgaispock.slimehud.waila.HudRequest
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu
import net.guizhanss.guizhanlib.kt.minecraft.items.edit
import net.guizhanss.guizhanlib.kt.slimefun.items.toItem
import net.guizhanss.guizhanlib.kt.slimefun.utils.getInt
import net.guizhanss.guizhanlib.kt.slimefun.utils.setInt
import net.guizhanss.infinityexpansion2.core.items.annotations.HudProvider
import net.guizhanss.infinityexpansion2.core.items.attributes.CustomWikiItem
import net.guizhanss.infinityexpansion2.core.items.attributes.InformationalRecipeDisplayItem
import net.guizhanss.infinityexpansion2.core.menu.MenuLayout
import net.guizhanss.infinityexpansion2.implementation.IEItems
import net.guizhanss.infinityexpansion2.implementation.items.machines.abstracts.AbstractTickingMachine
import net.guizhanss.infinityexpansion2.utils.items.GuiItems
import net.guizhanss.infinityexpansion2.utils.slimefunext.pushAll
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

@HudProvider
class VoidHarvester(
    itemGroup: ItemGroup,
    itemStack: SlimefunItemStack,
    recipeType: RecipeType,
    recipe: Array<out ItemStack?>,
    val speed: Int,
    energyPerTick: Int,
) : AbstractTickingMachine(itemGroup, itemStack, recipeType, recipe, MenuLayout.OUTPUT_ONLY_ONE_ROW, energyPerTick),
    InformationalRecipeDisplayItem, CustomWikiItem {

    override val wikiUrl = "machines/void-harvester"

    private val totalProgressSetting = IntRangeSetting(this, "total-progress", 1, 1024, 1_000_000_000)

    init {
        addItemSetting(totalProgressSetting)
    }

    // compressed harvesters use the total progress of the original harvester
    val totalProgress: Int get() = ((timeSource as? VoidHarvester)?.totalProgressSetting ?: totalProgressSetting).value

    override fun process(b: Block, menu: BlockMenu): Boolean {
        val progress = b.getInt(PROGRESS)

        // has reached the total progress
        if (progress >= totalProgress) {
            val count = progress.toLong() / totalProgress

            // compressed harvesters produce multiplier times the void bits per completion
            if (!menu.pushAll(IEItems.VOID_BIT.toItem(), count * multiplier, layout.outputSlots)) {
                menu.setStatus { GuiItems.NO_ROOM }
                return false
            }

            menu.setProgress(progress - count * totalProgress + speed)
        } else {
            menu.setProgress(progress.toLong() + speed)
        }
        return true
    }

    private fun BlockMenu.setProgress(value: Long) {
        val progress = value.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        location.setInt(PROGRESS, progress)
        setStatus { GuiItems.progressBar(progress.coerceAtMost(totalProgress), totalProgress) }
    }

    override fun getDefaultDisplayRecipes() = listOf(IEItems.VOID_BIT.toItem().apply { amount = multiplier.coerceIn(1, 64) })

    override fun getInfoItems() = listOf(
        GuiItems.tickRate(getCustomTickRate()),
        GuiItems.energyConsumptionPerTick(getEnergyConsumptionPerTick()),
        GuiItems.increaseProgress(speed).edit { amount(speed.coerceIn(1, 64)) },
        GuiItems.totalProgress(totalProgress),
    )

    override fun getDividerItem() = GuiItems.PRODUCES

    companion object {

        // block storage keys
        private const val PROGRESS = "progress"

        @Suppress("unused")
        fun hudHandler(request: HudRequest): String {
            val loc = request.location
            val machine = request.slimefunItem as VoidHarvester

            val progress = loc.getInt(PROGRESS)
            val totalProgress = machine.totalProgress

            return buildString {
                append("$progress / $totalProgress")
                append("&7 | ")

                append(HudBuilder.getProgressBar(progress, totalProgress))
                append("&7 | ")

                append(HudBuilder.formatEnergyStored(machine.getCharge(loc), machine.capacity))
            }
        }
    }
}
