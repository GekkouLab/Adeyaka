package gl.ky.adeyaka

import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin

class Adeyaka : JavaPlugin() {
    override fun onEnable() {
        main = this
        log = logger
        Metrics(this, 14769)
        info("Plugin [Adeyaka] has been enabled.")

    }

    override fun onDisable() {
    }

    fun reload() {
    }

}
