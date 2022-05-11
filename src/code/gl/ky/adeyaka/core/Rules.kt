package gl.ky.adeyaka.core

typealias Rule = RuleNode.Seq

sealed interface RuleNode {
    class Seq(val nodes: List<RuleNode>) : RuleNode
    class AnyOf(val nodes: List<RuleNode>) : RuleNode
    class Verb(val verb: String) : RuleNode
    class Text(val text: String) : RuleNode
    class Adverb(val key: String, val type: Type) : RuleNode {
        enum class Type {
            STRING,
            NUMBER,
            BOOLEAN,
            POSITION,
        }
    }
}

