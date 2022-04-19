package gl.ky.adeyaka.old

object Interpreter {
    val scriptPool: MutableMap<String, ScriptGroup> = mutableMapOf()
    val verbPool: MutableMap<String, (Environment) -> Unit> = mutableMapOf()
}

class Environment(val bindings: MutableMap<String, Any>) {
    fun get(name: String): Any? = bindings[name]
    fun set(name: String, value: Any) {
        bindings[name] = value
    }
    fun <T> getAs(name: String): T? = bindings[name] as? T
}

