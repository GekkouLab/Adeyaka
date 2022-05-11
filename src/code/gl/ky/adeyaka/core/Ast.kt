package gl.ky.adeyaka.core

sealed interface AstNode {
    class Segment(val sentences: List<Sentence>) : AstNode
    class Sentence(val components: List<Component>) : AstNode
    sealed interface Component : AstNode
    class Verb(val verb: String) : Component
    class Adverb(val name: String, val attr: Value) : Component
    sealed interface Value : AstNode
    class StringValue(val value: String) : Value
    class NumberValue(val value: Double) : Value
    class BooleanValue(val value: Boolean) : Value
    class LocationValue(val x: Int, val y: Int, val z: Int) : Value
}

