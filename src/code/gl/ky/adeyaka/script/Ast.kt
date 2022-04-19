package gl.ky.adeyaka.script

sealed interface AstNode {
    class Segment(val sentences: Sentence) : AstNode
    class Sentence(val words: List<Component>) : AstNode
    sealed interface Component : AstNode
    class Verb(val verb: String, val attr: Map<String, Any?>) : Component
    class Adverb(val attr: Map<String, Any?>)
}
