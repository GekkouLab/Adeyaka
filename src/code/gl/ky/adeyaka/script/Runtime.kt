package gl.ky.adeyaka.script

class Scope private constructor(val parent: Scope? = null) {
    private val bindings: MutableMap<String, Any?> = mutableMapOf()

    operator fun get(name: String): Any? = bindings[name] ?: parent?.get(name)
    operator fun set(name: String, value: Any?) { bindings[name] = value }

    companion object {
        @JvmStatic
        fun create(parent: Scope) = Scope(parent)
        @JvmStatic
        internal fun globalScope() = Scope()
    }
}
