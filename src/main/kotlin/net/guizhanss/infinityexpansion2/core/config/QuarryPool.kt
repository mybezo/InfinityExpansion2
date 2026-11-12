package net.guizhanss.infinityexpansion2.core.config

import net.guizhanss.guizhanlib.kt.minecraft.extensions.loadIntMap
import org.bukkit.configuration.ConfigurationSection
import kotlin.random.Random

/**
 * A [ConfigurationSection] that represents a quarry pool.
 *
 * @param minTiers the minimum quarry tier (1 = basic, 2 = advanced, 3 = void, 4 = infinity)
 * required for a product to be produced. Products not listed are available to all tiers.
 */
data class QuarryPool(
    val baseProduct: String,
    val products: Map<String, Int>,
    val minTiers: Map<String, Int> = emptyMap(),
) : SerializableSection {

    /**
     * Get the minimum quarry tier required for the [product].
     */
    fun getMinTier(product: String) = minTiers[product] ?: 1

    /**
     * Check if the [product] can be produced by a quarry of the given [tier].
     */
    fun isAvailable(product: String, tier: Int) = (products[product] ?: 0) > 0 && getMinTier(product) <= tier

    /**
     * Get all the products (with weight) available to a quarry of the given [tier].
     */
    fun getProducts(tier: Int) = products.filter { (product, _) -> isAvailable(product, tier) }

    /**
     * Get a random product by weight, available to a quarry of the given [tier].
     * Returns null if no product is available.
     */
    fun getRandomProduct(tier: Int): String? {
        val available = getProducts(tier)
        val total = available.values.sum()
        if (total <= 0) return null

        var roll = Random.nextInt(total)
        for ((product, weight) in available) {
            roll -= weight
            if (roll < 0) return product
        }
        return null
    }

    override fun serialize(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["base-product"] = baseProduct
        products.forEach { (product, amount) ->
            map["products.$product"] = amount
        }
        minTiers.forEach { (product, tier) ->
            map["min-tiers.$product"] = tier
        }
        return map
    }

    companion object {

        @Suppress("unused")
        fun deserialize(section: ConfigurationSection): QuarryPool {
            val baseProduct = section.getString("base-product")!!
            val products = section.getConfigurationSection("products").loadIntMap()
            val minTiers = section.getConfigurationSection("min-tiers")?.loadIntMap() ?: emptyMap()
            return QuarryPool(baseProduct, products, minTiers)
        }
    }
}
