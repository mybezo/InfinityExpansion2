package net.guizhanss.infinityexpansion2.implementation.items.machines

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.ItemUtils
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu
import net.guizhanss.guizhanlib.kt.minecraft.extensions.toItem
import net.guizhanss.guizhanlib.kt.slimefun.extensions.isSlimefunItem
import net.guizhanss.guizhanlib.kt.slimefun.items.builder.asMaterialType
import net.guizhanss.infinityexpansion2.InfinityExpansion2
import net.guizhanss.infinityexpansion2.core.items.attributes.CustomWikiItem
import net.guizhanss.infinityexpansion2.core.items.attributes.InformationalRecipeDisplayItem
import net.guizhanss.infinityexpansion2.core.menu.MenuLayout
import net.guizhanss.infinityexpansion2.implementation.items.machines.abstracts.AbstractTickingActionMachine
import net.guizhanss.infinityexpansion2.utils.items.GuiItems
import net.guizhanss.infinityexpansion2.utils.tags.IETag
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

class GearTransformer(
    itemGroup: ItemGroup,
    itemStack: SlimefunItemStack,
    recipeType: RecipeType,
    recipe: Array<out ItemStack?>,
    energyPerUse: Int,
) : AbstractTickingActionMachine(itemGroup, itemStack, recipeType, recipe, MenuLayout.GEAR_TRANSFORMER, energyPerUse),
    InformationalRecipeDisplayItem, CustomWikiItem {

    override val wikiUrl = "machines/gear-transformer"

    override fun setup(preset: BlockMenuPreset) {
        super.setup(preset)
        preset.drawBackground(GEAR_BORDER_ITEM, GEAR_BORDER)
        preset.drawBackground(MATERIAL_BORDER_ITEM, MATERIAL_BORDER)
    }

    /**
     * Routes items inserted by cargo / networks to the correct slot.
     * Gears go to the gear slot, valid materials go to the material slot, everything else is rejected
     * so that the machine never gets clogged by unrelated items.
     */
    override fun getInputSlots(menu: DirtyChestMenu, item: ItemStack): IntArray {
        return when {
            isGear(item) -> {
                if (!InfinityExpansion2.configService.gearTransformerAllowSfItems.value && item.isSlimefunItem()) {
                    intArrayOf()
                } else {
                    intArrayOf(inputSlots[0])
                }
            }

            isMaterial(item) -> intArrayOf(inputSlots[1])
            else -> intArrayOf()
        }
    }

    private fun isGear(item: ItemStack) =
        IETag.UPGRADABLE_TOOL.isTagged(item.type) || IETag.UPGRADABLE_ARMOR.isTagged(item.type)

    private fun isMaterial(item: ItemStack) =
        !item.isSlimefunItem() && (TOOLS.values + ARMORS.values).any { ItemUtils.canStack(item, it) }

    override fun process(b: Block, menu: BlockMenu): Boolean {
        val gear = menu.getItemInSlot(inputSlots[0])
        val material = menu.getItemInSlot(inputSlots[1])

        if (gear == null || material == null) {
            menu.setStatus { GuiItems.INVALID_INPUT }
            return false
        }

        if (!InfinityExpansion2.configService.gearTransformerAllowSfItems.value && gear.isSlimefunItem()) {
            menu.setStatus { NO_SLIMEFUN_ITEM }
            return false
        }

        menu.setStatus { GuiItems.PRODUCING }

        // check if item is a tool/weapon
        if (IETag.UPGRADABLE_TOOL.isTagged(gear.type)) {
            // tool
            return process(menu, TOOLS, gear, material)
        } else if (IETag.UPGRADABLE_ARMOR.isTagged(gear.type)) {
            // armor
            return process(menu, ARMORS, gear, material)
        }

        return false
    }

    private fun process(menu: BlockMenu, map: Map<String, ItemStack>, gear: ItemStack, material: ItemStack): Boolean {
        // find the new material
        val new = map.entries.firstOrNull { ItemUtils.canStack(material, it.value) }
        if (new == null) {
            menu.setStatus { GuiItems.INVALID_INPUT }
            return false
        }

        // find the current gear material, if the gear is unknown or already the target material,
        // move it to output unchanged so cargo / networks do not get stuck
        val current = map.entries.firstOrNull { gear.type.name.startsWith(it.key) }
        if (current == null || current.key == new.key) {
            return passThrough(menu, gear)
        }

        // not enough material
        if (material.amount < new.value.amount) {
            menu.setStatus { GuiItems.INVALID_INPUT }
            return false
        }

        // change the material
        val newType = Material.getMaterial(gear.type.name.replace(current.key, new.key))
            ?: return passThrough(menu, gear)
        val newGear = newType.toItem(gear.amount)
        newGear.itemMeta = gear.itemMeta

        // update the menu
        if (!menu.fits(newGear, *outputSlots)) {
            menu.setStatus { GuiItems.NO_ROOM }
            return false
        }
        menu.consumeItem(inputSlots[0])
        menu.consumeItem(inputSlots[1], new.value.amount)
        menu.pushItem(newGear, *outputSlots)

        return true
    }

    private fun passThrough(menu: BlockMenu, gear: ItemStack): Boolean {
        if (!menu.fits(gear, *outputSlots)) {
            menu.setStatus { GuiItems.NO_ROOM }
            return false
        }
        val output = gear.clone()
        menu.consumeItem(inputSlots[0], gear.amount)
        menu.pushItem(output, *outputSlots)
        return false
    }

    override fun getInfoItems() = listOf(
        GuiItems.energyConsumptionPerUse(getEnergyConsumptionPerAction()),
        GuiItems.sfItem(InfinityExpansion2.configService.gearTransformerAllowSfItems.value)
    )

    override fun getDefaultDisplayRecipes(): List<ItemStack?> {
        val result = mutableListOf<ItemStack?>()

        TOOLS.values.forEach { result.add(it) }

        if (TOOLS.size % 2 != 0) result.add(null)
        repeat(2) { result.add(null) }

        ARMORS.values.forEach { result.add(it) }

        return result
    }

    companion object {

        private val GEAR_BORDER = intArrayOf(
            0, 1, 2,
            9, 11,
            18, 19, 20,
        )
        private val MATERIAL_BORDER = intArrayOf(
            6, 7, 8,
            15, 17,
            24, 25, 26
        )

        private val GEAR_BORDER_ITEM = InfinityExpansion2.localization.getGuiItem(
            Material.BLUE_STAINED_GLASS_PANE.asMaterialType(),
            "gt_gear"
        )
        private val MATERIAL_BORDER_ITEM = InfinityExpansion2.localization.getGuiItem(
            Material.BLUE_STAINED_GLASS_PANE.asMaterialType(),
            "gt_material"
        )
        private val NO_SLIMEFUN_ITEM = InfinityExpansion2.localization.getGuiItem(
            Material.BARRIER.asMaterialType(),
            "gt_no_slimefun"
        )

        private val TOOLS = mapOf(
            "WOODEN_" to Material.OAK_PLANKS.toItem(4),
            "STONE_" to Material.COBBLESTONE.toItem(4),
            "IRON_" to Material.IRON_INGOT.toItem(4),
            "GOLDEN_" to Material.GOLD_INGOT.toItem(4),
            "DIAMOND_" to Material.DIAMOND.toItem(4),
            "NETHERITE_" to Material.NETHERITE_INGOT.toItem(2),
        )
        private val ARMORS = mapOf(
            "LEATHER_" to Material.LEATHER.toItem(4),
            "CHAINMAIL_" to Material.CHAIN.toItem(4),
            "IRON_" to Material.IRON_INGOT.toItem(4),
            "GOLDEN_" to Material.GOLD_INGOT.toItem(4),
            "DIAMOND_" to Material.DIAMOND.toItem(4),
            "NETHERITE_" to Material.NETHERITE_INGOT.toItem(2),
        )
    }
}
