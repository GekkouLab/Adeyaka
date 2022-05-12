package gl.ky.adeyaka.core

import org.bukkit.Location
import org.bukkit.OfflinePlayer

class Environment() {
    constructor(parent: Environment) : this() {
        player = parent.player
        location = parent.location
    }

    var player: OfflinePlayer? = null
    var location: Location? = null
}

class Segment(val name: String, val sentences: List<Sentence>) {
    fun execute(env: Environment) {
        for(sentence in sentences) sentence.execute(Environment(env))
    }
}

class Sentence(val actions: List<Action>) {
    fun execute(env: Environment) {
        for(action in actions) action.execute(env)
    }
}

interface Action {
    fun execute(env: Environment): Unit
}
