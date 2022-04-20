package gl.ky.adeyaka

import gl.ky.adeyaka.script.Rule
import gl.ky.adeyaka.script.Scope

object ScriptManager {
    val verbs: MutableList<(Scope) -> Unit> = mutableListOf()
    val rules: MutableList<Rule> = mutableListOf()
}