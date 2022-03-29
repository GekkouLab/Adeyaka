package gl.ky.adeyaka.api

import org.bukkit.Location
import org.bukkit.entity.Entity

interface Effect {
    fun apply(pos: Location)
    fun apply(obj: Entity)
}
