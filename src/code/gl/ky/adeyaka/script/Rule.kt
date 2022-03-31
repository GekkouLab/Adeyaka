package gl.ky.adeyaka.script

fun main() {
    println(parseRule("{设置} <name:string> 为 <value:any>"))
}

class Rule(val components: List<RuleComponent>) {
    override fun toString(): String {
        return components.joinToString(" ")
    }
}

sealed interface RuleComponent

class GetAs(val name: String, val type: Type) : RuleComponent {
    enum class Type {
        ANY,
        STRING,
        NUMBER,
        BOOL,
        POSITION,
        PLAYER,
    }

    override fun toString(): String {
        return "[getAs $name $type]"
    }
}

class VerbMatch(val verb: String) : RuleComponent {
    override fun toString(): String {
        return "[verbMatch $verb]"
    }
}

class Match(val s: String) : RuleComponent {
    override fun toString(): String {
        return "[match $s]"
    }
}

/**
 * Rule example: {设置} <name:string> 为 <value:any>
 *     -> Rule(listOf(VerbMatch("设置"), GetAs("name", GetAs.Type.STRING), Match("为"), GetAs("value", GetAs.Type.ANY)))
 */
fun parseRule(rule: String): Rule {
    var i = 0
    val components = mutableListOf<RuleComponent>()
    while(i < rule.length) {
        when(rule[i]) {
            '{' -> {
                val verb = rule.substring(i + 1, rule.indexOf('}', i))
                components.add(VerbMatch(verb))
                i += verb.length + 2
            }
            '<' -> {
                val get = rule.substring(i + 1, rule.indexOf('>', i))
                get.split(':').let { components.add(GetAs(it[0], GetAs.Type.valueOf(it[1]))) }
                i += get.length + 2
            }
            ' ' -> {
                i++
            }
            else -> {
                val value = rule.substring(i, rule.indexOf(' ', i))
                components.add(Match(value))
                i += value.length
            }
        }
    }
    return Rule(components)
}
