package net.guizhanss.infinityexpansion2.implementation.items.machines

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu
import net.guizhanss.guizhanlib.kt.minecraft.extensions.toItem
import net.guizhanss.guizhanlib.kt.slimefun.items.edit
import net.guizhanss.infinityexpansion2.core.items.attributes.InformationalRecipeDisplayItem
import net.guizhanss.infinityexpansion2.core.menu.MenuLayout
import net.guizhanss.infinityexpansion2.implementation.items.machines.abstracts.AbstractTickingMachine
import net.guizhanss.infinityexpansion2.utils.items.GuiItems
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/**
 * A "Plus" machine is a combination of a machine and a material generator.
 * It requires no input and directly produces one of the [PlusMachineType.products] every operation,
 * using a quarry-like menu (3 rows of output).
 */
class PlusMachine(
    itemGroup: ItemGroup,
    itemStack: SlimefunItemStack,
    recipeType: RecipeType,
    recipe: Array<out ItemStack?>,
    val machineType: PlusMachineType,
    val outputAmount: Int,
    energyPerTick: Int,
) : AbstractTickingMachine(itemGroup, itemStack, recipeType, recipe, MenuLayout.OUTPUT_ONLY, energyPerTick),
    InformationalRecipeDisplayItem {

    private val products: List<ItemStack> by lazy {
        machineType.products().map { it.clone().apply { amount = outputAmount } }
    }

    override fun process(b: Block, menu: BlockMenu): Boolean {
        val output = products.random().clone()
        if (!menu.fits(output, *outputSlots)) {
            menu.setStatus { GuiItems.NO_ROOM }
            return false
        }

        menu.setStatus { GuiItems.PRODUCING }
        menu.pushItem(output, *outputSlots)
        return true
    }

    override fun getDefaultDisplayRecipes(): List<ItemStack?> = products.map { it.clone() }

    override fun getInfoItems() = listOf(
        GuiItems.tickRate(getCustomTickRate()),
        GuiItems.energyConsumptionPerTick(getEnergyConsumptionPerTick()),
    )

    override fun getDividerItem() = GuiItems.PRODUCES
}

enum class PlusMachineType(val products: () -> List<ItemStack>) {

    DUST({
        listOf(
            SlimefunItems.COPPER_DUST,
            SlimefunItems.ZINC_DUST,
            SlimefunItems.TIN_DUST,
            SlimefunItems.ALUMINUM_DUST,
            SlimefunItems.LEAD_DUST,
            SlimefunItems.SILVER_DUST,
            SlimefunItems.GOLD_DUST,
            SlimefunItems.IRON_DUST,
            SlimefunItems.MAGNESIUM_DUST,
        ).map { it.edit { amount(1) } }
    }),

    URANIUM_INGOT({
        listOf(SlimefunItems.URANIUM.edit { amount(1) })
    }),

    INGOT({
        listOf(
            SlimefunItems.COPPER_INGOT.edit { amount(1) },
            SlimefunItems.ZINC_INGOT.edit { amount(1) },
            SlimefunItems.TIN_INGOT.edit { amount(1) },
            SlimefunItems.ALUMINUM_INGOT.edit { amount(1) },
            SlimefunItems.LEAD_INGOT.edit { amount(1) },
            SlimefunItems.SILVER_INGOT.edit { amount(1) },
            SlimefunItems.GOLD_4K.edit { amount(1) },
            Material.IRON_INGOT.toItem(),
            SlimefunItems.MAGNESIUM_INGOT.edit { amount(1) },
        )
    }),
}
