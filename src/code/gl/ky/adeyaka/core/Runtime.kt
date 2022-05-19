package gl.ky.adeyaka.core

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import kotlin.math.abs

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

/**
 * values unable to get actually at parse time
 */
interface RuntimeValue<T> {
    fun get(env: Environment): T

    companion object {
        fun <T> of(value: RuntimeValue<T>) = value
        fun <T> of(value: T) = ConstValue(value)
    }

    /**
     * transform value at runtime
     */
    abstract class Transformer<T>(val value: RuntimeValue<*>): RuntimeValue<T> {
        abstract override fun get(env: Environment): T
    }

    class ConstValue<T>(val value: T) : RuntimeValue<T> {
        override fun get(env: Environment) = value
    }
}
