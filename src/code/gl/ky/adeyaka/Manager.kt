package gl.ky.adeyaka

import gl.ky.adeyaka.core.Rule
import gl.ky.adeyaka.core.Scope

object ScriptManager {
    val verbs: MutableList<(Scope) -> Unit> = mutableListOf()
    val rules: MutableList<Rule> = mutableListOf()
}