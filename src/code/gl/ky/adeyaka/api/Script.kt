package gl.ky.adeyaka.api

import gl.ky.adeyaka.script.Rule
import gl.ky.adeyaka.script.Scope

interface Extension {
    fun registerVerbs(verbs: MutableList<(Scope) -> Unit>)
    fun registerRules(rules: MutableList<Rule>)
}