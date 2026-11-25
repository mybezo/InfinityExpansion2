package net.guizhanss.infinityexpansion2.core.config

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config
import net.guizhanss.infinityexpansion2.InfinityExpansion2
import java.util.logging.Level

/**
 * The `machine-time` section of config.yml: the time (in real seconds) between products of some machines,
 * for the machines that do not have a time setting of their own.
 *
 * Missing entries are written into the file on startup, existing values are kept.
 * Changes require a server restart.
 */
object MachineTime {

    private const val SECTION = "machine-time"
    private const val MIN_SECONDS = 0.5

    private val times = mutableMapOf<String, Double>()

    /**
     * Real seconds of one Slimefun tick (10 game ticks = 0.5 seconds by default).
     */
    val secondsPerTick: Double
        get() = (Slimefun.getTickerTask()?.tickRate ?: 10) / 20.0

    /**
     * Load the machine times from config.yml, the [defaults] are the times used now (machine id to seconds).
     */
    internal fun load(plugin: InfinityExpansion2, defaults: Map<String, Double>) {
        // read the actual file, so the defaults never hide a missing entry
        val config = Config(plugin, "config.yml")
        defaults.forEach { (id, seconds) -> config.setDefaultValue("$SECTION.$id", seconds) }
        config.save()

        times.clear()
        defaults.forEach { (id, default) ->
            val value = config.configuration.get("$SECTION.$id")
            val seconds = (value as? Number)?.toDouble() ?: value?.toString()?.toDoubleOrNull()
            times[id] = when {
                seconds == null || seconds <= 0 -> {
                    InfinityExpansion2.log(
                        Level.WARNING,
                        "Invalid $SECTION.$id in config.yml: \"$value\", it must be larger than 0. Using $default."
                    )
                    default
                }

                seconds < MIN_SECONDS -> {
                    InfinityExpansion2.log(
                        Level.WARNING,
                        "$SECTION.$id in config.yml is less than $MIN_SECONDS seconds. Using $MIN_SECONDS."
                    )
                    MIN_SECONDS
                }

                else -> seconds
            }
        }
    }

    /**
     * Get the time in real seconds of the machine [id] (without prefix), or the [default] if not loaded.
     */
    fun get(id: String, default: Double): Double = times[id] ?: default
}
