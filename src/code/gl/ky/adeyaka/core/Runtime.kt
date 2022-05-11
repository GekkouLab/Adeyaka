package gl.ky.adeyaka.core

import org.bukkit.Location
import org.bukkit.OfflinePlayer

class Environment {
    var player: OfflinePlayer? = null
    var location: Location? = null
}

interface Action {
    fun execute(env: Environment): Unit
}

typealias Verb = (Scope) -> Unit

interface AValue {
    class ANumber(val value: Double) : AValue
    class AString(val value: String) : AValue
    class ABoolean(val value: Boolean) : AValue

    companion object {
        /**
        @JvmStatic
        fun wrap(value: AstNode.Value): AValue {
            return when (value) {
                is AstNode.NumberValue -> ANumber(value.value)
                is AstNode.StringValue -> AString(value.value)
                is AstNode.BooleanValue -> ABoolean(value.value)
                is AstNode.LocationValue -> TODO()
            }
        }
        */
        @JvmStatic
        fun wrap(value: AstNode.Value): Any {
            return when (value) {
                is AstNode.NumberValue -> value.value
                is AstNode.StringValue -> value.value
                is AstNode.BooleanValue -> value.value
                is AstNode.LocationValue -> TODO()
            }
        }

    }
}

class Scope private constructor(val parent: Scope? = null) {
    val bindings: MutableMap<String, Any?> = mutableMapOf()

    operator fun get(name: String): Any? = bindings[name] ?: parent?.get(name)
    operator fun set(name: String, value: Any?) { bindings[name] = value }

    companion object {
        @JvmStatic
        fun create(parent: Scope) = Scope(parent)
        @JvmStatic
        internal fun globalScope() = Scope()
        @JvmStatic
        fun Scope.runScript(segment: AstNode.Segment) = AstEvaluator(this, mapOf()).runSegment(segment)
    }
}

class AstEvaluator(val global: Scope = Scope.globalScope(), val verbPool: Map<String, Verb>) {
    fun runSegment(segment: AstNode.Segment, scope: Scope = Scope.create(this.global)) {
        for(sentence in segment.sentences) runSentence(sentence, scope)
    }

    fun runSentence(sentence: AstNode.Sentence, scope: Scope) {
        for(component in sentence.components) when(component) {
            is AstNode.Adverb -> scope.bindings += component.name to AValue.wrap(component.attr)
            is AstNode.Verb -> verbPool[component.verb]?.invoke(scope)
        }
    }
}
