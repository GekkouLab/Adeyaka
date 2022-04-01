package gl.ky.adeyaka.script

import gl.ky.adeyaka.script.Rule.Companion.toRule

fun main() {
    println(("{设置} <name:string> 为 <value:any>").toRule())
}

class Rule(val components: List<RuleComponent>) {
    override fun toString(): String {
        return "[Rule " + components.joinToString(" ") + "]"
    }

    companion object {
        /**
         * Rule example: {设置} <name:id> 为 <value:any>
         *     -> Rule(listOf(VerbMatch("设置"), GetAs("name", GetAs.Type.STRING), Match("为"), GetAs("value", GetAs.Type.ANY)))
         */
        @JvmStatic
        fun fromString(rule: String): Rule {
            var i = 0
            val components = mutableListOf<RuleComponent>()
            while(i < rule.length) {
                when(rule[i]) {
                    '{' -> {
                        val verb = rule.substring(i + 1, rule.indexOf('}', i))
                        components += RuleComponent.MatchAsVerb(verb)
                        i += verb.length + 2
                    }
                    '<' -> {
                        val get = rule.substring(i + 1, rule.indexOf('>', i))
                        get.split(':').let {
                            components += RuleComponent.MatchAsAdverb(it[0], RuleComponent.MatchAsAdverb.Type.of(it[1]))
                        }
                        i += get.length + 2
                    }
                    ' ' -> {
                        i++
                    }
                    else -> {
                        val e = rule.indexOf(' ', i)
                        val value = rule.substring(i, if(e == -1) rule.length else e)
                        components += RuleComponent.MatchAndIgnore(value)
                        i += value.length
                    }
                }
            }
            return Rule(components)
        }

        @JvmStatic
        fun fromStrings(vararg s: String): List<Rule> = s.map(this::fromString)

        @JvmStatic
        fun String.toRule(): Rule {
            return fromString(this)
        }
    }
}

sealed interface RuleComponent {
    class MatchAsAdverb(val name: String, val type: Type) : RuleComponent {
        enum class Type {
            ANY,
            ID,
            STRING,
            NUMBER,
            BOOL,
            POSITION,
            PLAYER,
            ;

            companion object {
                @JvmStatic
                fun of(s :String) = when(s.lowercase()) {
                    "string" -> STRING
                    "id" -> ID
                    "number" -> NUMBER
                    "bool" -> BOOL
                    "position" -> POSITION
                    "player" -> PLAYER
                    "any" -> ANY
                    else -> throw RuntimeException("Unknown match type: $s")
                }
            }
        }

        override fun toString(): String {
            return "[matchAsAdverb $name $type]"
        }
    }

    class MatchAsVerb(val verb: String) : RuleComponent {
        override fun toString(): String {
            return "[matchAsVerb $verb]"
        }
    }

    class MatchAndIgnore(val text: String) : RuleComponent {
        override fun toString(): String {
            return "[matchAndIgnore $text]"
        }
    }
}
