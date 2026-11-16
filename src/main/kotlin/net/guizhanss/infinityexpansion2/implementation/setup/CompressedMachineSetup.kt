@file:Suppress("deprecation")

package net.guizhanss.infinityexpansion2.implementation.setup

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import net.guizhanss.guizhanlib.kt.slimefun.items.toItem
import net.guizhanss.guizhanlib.minecraft.utils.ChatUtil
import net.guizhanss.infinityexpansion2.InfinityExpansion2
import net.guizhanss.infinityexpansion2.core.items.attributes.Compressible
import net.guizhanss.infinityexpansion2.core.menu.MenuLayout
import net.guizhanss.infinityexpansion2.implementation.IEItems
import net.guizhanss.infinityexpansion2.implementation.guide.IEItemGroups
import net.guizhanss.infinityexpansion2.implementation.items.generators.EnergyGenerator
import net.guizhanss.infinityexpansion2.implementation.items.generators.InfinityReactor
import net.guizhanss.infinityexpansion2.implementation.items.generators.InfinitySingularityReactor
import net.guizhanss.infinityexpansion2.implementation.items.machines.FlowerGrower
import net.guizhanss.infinityexpansion2.implementation.items.machines.GeoQuarry
import net.guizhanss.infinityexpansion2.implementation.items.machines.PlusMachine
import net.guizhanss.infinityexpansion2.implementation.items.machines.Quarry
import net.guizhanss.infinityexpansion2.implementation.items.machines.StoneworksFactory
import net.guizhanss.infinityexpansion2.implementation.items.machines.TreeGrower
import net.guizhanss.infinityexpansion2.implementation.items.machines.UraniumExtractor
import net.guizhanss.infinityexpansion2.implementation.items.machines.UraniumIngotExtractor
import net.guizhanss.infinityexpansion2.implementation.items.machines.VirtualFarm
import net.guizhanss.infinityexpansion2.implementation.items.machines.VoidHarvester
import net.guizhanss.infinityexpansion2.implementation.items.machines.abstracts.AbstractTickingMachine
import net.guizhanss.infinityexpansion2.implementation.items.mobsim.MobSimulationChamber
import org.bukkit.inventory.ItemStack
import java.util.logging.Level

/**
 * Registers the compressed machines and generators (multiple machines combined into one),
 * configured by `compressed.yml`.
 *
 * Each item has a compressed version for every multiplier. The recipe of the first enabled multiplier
 * uses the original item, the others combine 2 of the previous multiplier in the Compressor.
 * Only the highest multiplier is listed in the guide.
 */
internal object CompressedMachineSetup {

    val MULTIPLIERS = intArrayOf(8, 16, 32, 64)

    // max values of the energy item settings
    private const val MAX_MACHINE_ENERGY = 1_000_000_000L
    private const val MAX_GENERATOR_ENERGY = 16_777_215L

    /**
     * The category of compressed items, decides the guide group and the config section.
     */
    private enum class Category(val configSection: String) {
        MACHINE("machines"),
        GENERATOR("generators");

        val itemGroup: ItemGroup
            get() = when (this) {
                MACHINE -> IEItemGroups.COMPRESSED_MACHINES
                GENERATOR -> IEItemGroups.COMPRESSED_GENERATORS
            }
    }

    /**
     * The arguments to construct a compressed item.
     */
    class Args(
        val itemGroup: ItemGroup,
        val itemStack: SlimefunItemStack,
        val recipeType: RecipeType,
        val recipe: Array<out ItemStack?>,
        val multiplier: Int,
    ) {

        /**
         * Multiply the [value] by the multiplier, capped at [max].
         */
        fun scale(value: Int, max: Long) = (value.toLong() * multiplier).coerceAtMost(max).toInt()
    }

    private class Entry(
        val base: SlimefunItemStack,
        val category: Category,
        val maxMultiplier: Int,
        val factory: (Args, SlimefunItem) -> SlimefunItem?,
    ) {

        val id: String = base.itemId.removePrefix(IEItems.prefix)
    }

    private val entries = mutableListOf<Entry>()

    private inline fun <reified T : SlimefunItem> add(
        base: SlimefunItemStack,
        category: Category = Category.MACHINE,
        maxMultiplier: Int = 64,
        crossinline factory: (Args, T) -> SlimefunItem,
    ) {
        entries.add(Entry(base, category, maxMultiplier) { args, sfItem -> (sfItem as? T)?.let { factory(args, it) } })
    }

    private fun Args.energy(machine: AbstractTickingMachine) = scale(machine.defaultEnergyPerTick, MAX_MACHINE_ENERGY)

    init {
        registerMachines()
        registerGenerators()
        register()
    }

    private fun registerMachines() {
        // quarries
        listOf(IEItems.QUARRY, IEItems.QUARRY_2, IEItems.QUARRY_3, IEItems.QUARRY_4).forEach { base ->
            add<Quarry>(base) { a, m ->
                Quarry(a.itemGroup, a.itemStack, a.recipeType, a.recipe, a.energy(m), m.speed, m.defaultChance, m.tier)
            }
        }
        listOf(IEItems.GEO_QUARRY, IEItems.GEO_QUARRY_2).forEach { base ->
            add<GeoQuarry>(base) { a, m ->
                GeoQuarry(a.itemGroup, a.itemStack, a.recipeType, a.recipe, a.energy(m), m.defaultOutputInterval, m.speed)
            }
        }

        // growing machines
        listOf(IEItems.VIRTUAL_FARM, IEItems.VIRTUAL_FARM_2, IEItems.VIRTUAL_FARM_3, IEItems.VIRTUAL_FARM_4)
            .forEach { base ->
                add<VirtualFarm>(base) { a, m ->
                    VirtualFarm(a.itemGroup, a.itemStack, a.recipeType, a.recipe, a.energy(m), m.defaultOutputInterval)
                }
            }
        listOf(IEItems.TREE_GROWER, IEItems.TREE_GROWER_2, IEItems.TREE_GROWER_3, IEItems.TREE_GROWER_4)
            .forEach { base ->
                add<TreeGrower>(base) { a, m ->
                    TreeGrower(a.itemGroup, a.itemStack, a.recipeType, a.recipe, a.energy(m), m.defaultOutputInterval)
                }
            }
        listOf(IEItems.FLOWER_GROWER, IEItems.FLOWER_GROWER_2, IEItems.FLOWER_GROWER_3, IEItems.FLOWER_GROWER_4)
            .forEach { base ->
                add<FlowerGrower>(base) { a, m ->
                    FlowerGrower(a.itemGroup, a.itemStack, a.recipeType, a.recipe, a.energy(m), m.defaultOutputInterval)
                }
            }

        // void harvesters
        listOf(IEItems.VOID_HARVESTER, IEItems.VOID_HARVESTER_2, IEItems.VOID_HARVESTER_3).forEach { base ->
            add<VoidHarvester>(base) { a, m ->
                VoidHarvester(a.itemGroup, a.itemStack, a.recipeType, a.recipe, m.speed, a.energy(m))
            }
        }

        // stoneworks factories
        listOf(
            IEItems.STONEWORKS_FACTORY, IEItems.STONEWORKS_FACTORY_2,
            IEItems.STONEWORKS_FACTORY_3, IEItems.STONEWORKS_FACTORY_4
        ).forEach { base ->
            add<StoneworksFactory>(base) { a, m ->
                StoneworksFactory(
                    a.itemGroup, a.itemStack, a.recipeType, a.recipe, m.speed, a.energy(m),
                    MenuLayout.STONEWORKS_FACTORY_COMPRESSED
                )
            }
        }

        // mob simulation
        add<MobSimulationChamber>(IEItems.MOB_SIMULATION_CHAMBER) { a, m ->
            MobSimulationChamber(a.itemGroup, a.itemStack, a.recipeType, a.recipe, a.energy(m))
        }

        // uranium
        add<UraniumExtractor>(IEItems.URANIUM_EXTRACTOR) { a, m ->
            UraniumExtractor(a.itemGroup, a.itemStack, a.recipeType, a.recipe, a.energy(m), m.inputAmount, m.outputAmount)
        }
        add<UraniumIngotExtractor>(IEItems.URANIUM_INGOT_EXTRACTOR) { a, m ->
            UraniumIngotExtractor(
                a.itemGroup, a.itemStack, a.recipeType, a.recipe, a.energy(m), m.inputAmount, m.outputAmount
            )
        }

        // machine plus
        listOf(
            IEItems.DUST_EXTRACTOR_PLUS, IEItems.DUST_EXTRACTOR_PLUS_2,
            IEItems.DUST_EXTRACTOR_PLUS_3, IEItems.DUST_EXTRACTOR_PLUS_4,
            IEItems.INGOT_FORMER_PLUS, IEItems.INGOT_FORMER_PLUS_2,
            IEItems.INGOT_FORMER_PLUS_3, IEItems.INGOT_FORMER_PLUS_4,
        ).forEach { base ->
            add<PlusMachine>(base) { a, m ->
                PlusMachine(a.itemGroup, a.itemStack, a.recipeType, a.recipe, m.machineType, m.outputAmount, a.energy(m))
            }
        }
    }

    private fun registerGenerators() {
        listOf(
            IEItems.HYDRO_GENERATOR, IEItems.HYDRO_GENERATOR_2,
            IEItems.GEOTHERMAL_GENERATOR, IEItems.GEOTHERMAL_GENERATOR_2,
            IEItems.SOLAR_PANEL, IEItems.SOLAR_PANEL_2, IEItems.SOLAR_PANEL_3,
            IEItems.VOID_PANEL, IEItems.INFINITY_PANEL,
        ).forEach { base ->
            add<EnergyGenerator>(base, Category.GENERATOR) { a, g ->
                EnergyGenerator(
                    a.itemGroup, a.itemStack, a.recipeType, a.recipe, g.type,
                    a.scale(g.defaultProduction, MAX_GENERATOR_ENERGY)
                )
            }
        }

        add<InfinityReactor>(IEItems.INFINITY_REACTOR, Category.GENERATOR) { a, g ->
            InfinityReactor(a.itemGroup, a.itemStack, a.recipeType, a.recipe, a.scale(g.defaultProduction, Int.MAX_VALUE.toLong()))
        }
        add<InfinitySingularityReactor>(IEItems.INFINITY_SINGULARITY_REACTOR, Category.GENERATOR, maxMultiplier = 32) { a, g ->
            InfinitySingularityReactor(
                a.itemGroup, a.itemStack, a.recipeType, a.recipe, a.scale(g.defaultProduction, Int.MAX_VALUE.toLong())
            )
        }
    }

    private fun register() {
        val cfg = InfinityExpansion2.configService.compressedConfig

        // write the missing options into the file, existing values are kept
        cfg.setDefaultValue("enabled", true)
        MULTIPLIERS.forEach { cfg.setDefaultValue("multipliers.x$it", true) }
        entries.forEach { cfg.setDefaultValue("${it.category.configSection}.${it.id}", true) }
        cfg.save()

        if (!cfg.getBoolean("enabled")) return

        // a multiplier is only available when all the lower multipliers are enabled
        val enabledMultipliers = MULTIPLIERS.takeWhile { cfg.getBoolean("multipliers.x$it") }
        if (enabledMultipliers.isEmpty()) return

        InfinityExpansion2.log(Level.INFO, "Registering compressed machines...")
        var count = 0
        entries.forEach entry@{ entry ->
            if (!cfg.getBoolean("${entry.category.configSection}.${entry.id}")) return@entry
            val baseItem = SlimefunItem.getById(entry.base.itemId) ?: return@entry
            if (baseItem.isDisabled) return@entry

            val multipliers = enabledMultipliers.filter { it <= entry.maxMultiplier }
            var previous: SlimefunItemStack = entry.base
            var previousMultiplier = 1
            for (multiplier in multipliers) {
                val sfis = createItemStack(entry, multiplier)
                val recipe = arrayOfNulls<ItemStack>(9).apply {
                    this[0] = previous.toItem().apply { amount = multiplier / previousMultiplier }
                }
                // only the highest multiplier is listed in the guide, the lower ones are still craftable
                val group = if (multiplier == multipliers.last()) entry.category.itemGroup else IEItemGroups.HIDDEN
                val args = Args(group, sfis, RecipeType.COMPRESSOR, recipe, multiplier)

                val item = entry.factory(args, baseItem) ?: return@entry
                (item as? Compressible)?.multiplier = multiplier
                item.register(InfinityExpansion2.instance)
                count++

                previous = sfis
                previousMultiplier = multiplier
            }
        }
        InfinityExpansion2.log(Level.INFO, "Registered $count compressed machines.")
    }

    /**
     * The color of the multiplier text in the item name.
     */
    private fun multiplierColor(multiplier: Int) = when (multiplier) {
        8 -> "&a"
        16 -> "&e"
        32 -> "&6"
        else -> "&d"
    }

    private fun createItemStack(entry: Entry, multiplier: Int): SlimefunItemStack {
        val localization = InfinityExpansion2.localization
        val name = ChatUtil.color(
            "${localization.getItemName(entry.id)} ${multiplierColor(multiplier)}(x$multiplier)"
        )
        val lore = (localization.getItemLore(entry.id) + "" + localization.getLore("compressed", multiplier))
            .map { ChatUtil.color(it) }

        val item = entry.base.toItem()
        item.itemMeta = item.itemMeta?.apply {
            setDisplayName(name)
            this.lore = lore
        }
        return SlimefunItemStack("${IEItems.prefix}${entry.id}_X$multiplier", item)
    }
}
