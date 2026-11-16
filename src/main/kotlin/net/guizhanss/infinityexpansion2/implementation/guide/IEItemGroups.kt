package net.guizhanss.infinityexpansion2.implementation.guide

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction
import net.guizhanss.guizhanlib.kt.slimefun.items.builder.MaterialType
import net.guizhanss.guizhanlib.kt.slimefun.items.builder.asMaterialType
import net.guizhanss.guizhanlib.kt.slimefun.items.toItem
import net.guizhanss.infinityexpansion2.InfinityExpansion2
import net.guizhanss.infinityexpansion2.core.menu.MenuItem
import net.guizhanss.infinityexpansion2.implementation.guide.groups.CompressedGroup
import net.guizhanss.infinityexpansion2.implementation.guide.groups.MainGroup
import net.guizhanss.infinityexpansion2.implementation.guide.groups.SubGroup
import net.guizhanss.infinityexpansion2.utils.bukkitext.ie2Key
import org.bukkit.Material
import org.bukkit.entity.Player

object IEItemGroups {

    val MAIN = MainGroup(
        ie2Key("main"),
        InfinityExpansion2.localization.getItemGroupItem(
            Material.NETHER_STAR.asMaterialType(),
            "main"
        ).toItem()
    )

    val MATERIALS = createSubGroup("materials", Material.NETHER_STAR.asMaterialType())
    val SINGULARITIES = createSubGroup("singularities", Material.NETHERITE_BLOCK.asMaterialType())
    val SLIMEFUN_EXPANSION = createSubGroup("slimefun_expansion", Material.SLIME_BLOCK.asMaterialType())
    val FOOD = createSubGroup("food", Material.COOKED_BEEF.asMaterialType())
    val TOOLS = createSubGroup("tools", Material.DIAMOND_PICKAXE.asMaterialType())
    val GEAR = createSubGroup("gear", Material.DIAMOND_CHESTPLATE.asMaterialType())
    val MACHINES = createSubGroup("machines", Material.LOOM.asMaterialType())
    val PLUS_MACHINES = createSubGroup("plus_machines", Material.SMOKER.asMaterialType())
    val COMPRESSED_MACHINES = createSubGroup("compressed_machines", Material.LOOM.asMaterialType())
    val COMPRESSED_GENERATORS = createSubGroup("compressed_generators", Material.BLAST_FURNACE.asMaterialType())

    /**
     * The "Compressed Items" menu, shown at a fixed slot of the main group.
     */
    val COMPRESSED = CompressedGroup(
        ie2Key("compressed_items"),
        InfinityExpansion2.localization.getItemGroupItem(
            COMPRESSED_ITEMS_TEXTURE.ifEmpty { null }?.asMaterialType() ?: Material.PLAYER_HEAD.asMaterialType(),
            "compressed_items"
        ).toItem()
    )
    val GENERATORS = createSubGroup("generators", Material.BLAST_FURNACE.asMaterialType())
    val MOB_SIMULATION = createSubGroup("mob_simulation", Material.BEACON.asMaterialType())
    val STORAGE = createSubGroup("storage", Material.BEEHIVE.asMaterialType())
    val HIDDEN = createSubGroup("hidden", Material.BARRIER.asMaterialType())

    init {
        // TODO: Guide group
        MAIN.addSubGroups(
            MATERIALS,
            SINGULARITIES,
            SLIMEFUN_EXPANSION,
            FOOD,
            TOOLS,
            GEAR,
            MACHINES,
            PLUS_MACHINES,
        )
        MAIN.addSubGroups(
            GENERATORS,
            MOB_SIMULATION,
            STORAGE,
        )
        if (InfinityExpansion2.configService.compressedConfig.getBoolean("enabled")) {
            COMPRESSED.addSubGroups(COMPRESSED_MACHINES, COMPRESSED_GENERATORS)
            MAIN.setFixedItem(COMPRESSED_ITEMS_SLOT, object : MenuItem {
                override fun getItem(p: Player, profile: PlayerProfile) = COMPRESSED.getItem(p)

                override fun onClick(p: Player, profile: PlayerProfile, mode: SlimefunGuideMode, action: ClickAction) {
                    SlimefunGuide.openItemGroup(profile, COMPRESSED, mode, 1)
                }
            })
        }
        MOB_SIMULATION.isCrossAddonItemGroup = true
        MAIN.register(InfinityExpansion2.instance)
    }

    private const val COMPRESSED_ITEMS_SLOT = 40

    // the head texture of the "Compressed Items" icon (Mybezo's skin), empty to use a default player head
    private const val COMPRESSED_ITEMS_TEXTURE = ""

    private fun createSubGroup(key: String, material: MaterialType) = SubGroup(
        ie2Key(key),
        InfinityExpansion2.localization.getItemGroupItem(
            material,
            key
        ).toItem()
    )
}
