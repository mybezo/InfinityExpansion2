package net.guizhanss.infinityexpansion2.implementation.guide.groups

import io.github.thebusybiscuit.slimefun4.libraries.dough.items.ItemUtils
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * The "Compressed Items" menu inside the main group, lists the compressed [SubGroup]s.
 */
class CompressedGroup(
    key: NamespacedKey,
    item: ItemStack,
) : AbstractItemGroup(key, item) {

    override fun getGuideTitle(p: Player): String = ItemUtils.getItemName(item)
}
