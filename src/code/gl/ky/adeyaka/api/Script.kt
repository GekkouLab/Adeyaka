package gl.ky.adeyaka.api

import gl.ky.adeyaka.core.Rule
import gl.ky.adeyaka.core.Scope

interface Extension {
    fun registerVerbs(verbs: MutableList<(Scope) -> Unit>)
    fun registerRules(rules: MutableList<Rule>)
}