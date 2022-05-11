package gl.ky.adeyaka.core

import gl.ky.adeyaka.core.Parser.CharUtil.isEOS
import gl.ky.adeyaka.core.Parser.ParseUtil.match
import gl.ky.adeyaka.core.Parser.ParseUtil.matchIfPresent

interface ActionParser {
    /**
     * try to parse some tokens and return an action
     * @return the action if successful, null otherwise, if failed, tokens won't be really consumed.
     */
    fun parse(input: TokenStream): Action?
}

interface LiteralParser<T> {
    /**
     * try to parse some tokens and return a value
     * @return the value if successful, null otherwise, if failed, tokens won't be really consumed.
     */
    fun parse(input: TokenStream): T?

}


class Parser(val rules: List<Rule>) {
    class ParseException(message: String) : RuntimeException(message)

    class ParseState(var index: Int, val input: String) {
        fun jump(index: Int) { this.index = index }
        fun hasNext(): Boolean = index < input.length
        fun next(): Char = input[index++]
        @JvmOverloads
        fun peek(offset: Int = 0): Char = input[index + offset]
        fun peekString(length: Int): String = input.substring(index, index + length)
        @JvmOverloads
        fun skip(count: Int = 1) { index += count }
        fun skipWhiteSpace() { while(peek().isWhitespace()) skip() }
    }

    fun parse(input: String) = parse(ParseState(0, input))

    private fun preprocess(input: String): String {
        return input.replace('\r', '\n')
    }

    private fun parse(state: ParseState): List<AstNode.Segment> {
        val result = mutableListOf<AstNode.Segment>()
        while (state.hasNext()) result += parseSegment(state)
        return result
    }

    private fun parseSegment(state: ParseState): AstNode.Segment {
        val result = mutableListOf<AstNode.Sentence>()
        state.skipWhiteSpace()
        state.match("脚本段")
        state.match('{')
        while(state.peek() != '}') {
            result += parseSentence(state)
        }
        state.match('}')
        return AstNode.Segment(result)
    }

    private fun parseSentence(state: ParseState): AstNode.Sentence {
        val result = mutableListOf<AstNode.Component>()
        state.skipWhiteSpace()
        while(!(state.peek().isEOS() || state.peek() == '}')) {
            for(rule in rules) {
                state.skipWhiteSpace()
                val k = matchSeq(state, rule)
                if(k != null) {
                    result += k
                    break
                }
            }
        }
        return AstNode.Sentence(result)
    }

    /**
     * attempts to match a sequence.
     * (for every functions under this) recover by itself if it fails
     */
    private fun matchSeq(state: ParseState, seq: RuleNode.Seq): List<AstNode.Component>? {
        val result = mutableListOf<AstNode.Component>()
        val savepoint = state.index
        state.skipWhiteSpace()
        for(node in seq.nodes) {
            when(node) {
                is RuleNode.Text -> if(!matchText(state, node)) {
                    state.jump(savepoint)
                    return null
                }
                is RuleNode.Seq -> result += matchSeq(state, node) ?: run {
                    state.jump(savepoint)
                    return null
                }
                is RuleNode.AnyOf -> result += matchAnyOf(state, node) ?: run {
                    state.jump(savepoint)
                    return null
                }
                is RuleNode.Adverb -> TODO()
                is RuleNode.Verb -> TODO()
            }
        }
        return result
    }

    private fun matchAnyOf(state: ParseState, anyOf: RuleNode.AnyOf): List<AstNode.Component>? {
        state.skipWhiteSpace()
        for(node in anyOf.nodes) {
            when(node) {
                is RuleNode.Text -> return if(matchText(state, node)) listOf() else continue
                is RuleNode.Seq -> matchSeq(state, node)?. let { return it } ?: continue
                is RuleNode.AnyOf -> return matchAnyOf(state, node) ?: continue
                is RuleNode.Adverb -> TODO()
                is RuleNode.Verb -> TODO()
            }
        }
        return null
    }

    private fun matchText(state: ParseState, node: RuleNode.Text): Boolean = state.matchIfPresent(node.text)

    object ParseUtil {
        @JvmStatic
        fun ParseState.match(c: Char) = if(peek() == c) next()
        else throw ParseException("Expected '$c' but found '${peek()}'")
        @JvmStatic
        fun ParseState.match(s: String) = if(peekString(s.length) == s) skip(s.length)
        else throw ParseException("Expected '$s' but found '${peekString(s.length)}'")
        @JvmStatic
        fun ParseState.check(c: Char) = peek() == c
        @JvmStatic
        fun ParseState.check(s: String) = peekString(s.length) == s
        @JvmStatic
        fun ParseState.matchIfPresent(c: Char): Boolean = if(check(c)) {
            skip()
            true
        } else false
        @JvmStatic
        fun ParseState.matchIfPresent(s: String): Boolean = if(check(s)) {
            skip(s.length)
            true
        } else false
    }

    object CharUtil {
        @JvmStatic
        fun Char.isNumber() = this in '0'..'9'
        @JvmStatic
        fun Char.isIdStart() =
            this == '_' || this in 'a'..'z' || this in 'A'..'Z' || this in '\u4e00'..'\u9fff'
        @JvmStatic
        fun Char.isIdChar() = this == '_' ||
                this in '0'..'9' || this in 'a'..'z' || this in 'A'..'Z' || this in '\u4e00'..'\u9fff'
        @JvmStatic
        fun Char.isQuote() = this == '"' || this == '\'' || this == '“' || this == '”' || this == '‘' || this == '’'
        @JvmStatic
        fun Char.isComma() = this == ',' || this == '，'
        @JvmStatic
        fun Char.isLBrace() = this == '{' || this == '｛'
        @JvmStatic
        fun Char.isRBrace() = this == '}' || this == '｝'
        @JvmStatic
        fun Char.isLBracket() = this == '[' || this == '［' || this == '【'
        @JvmStatic
        fun Char.isRBracket() = this == ']' || this == '］' || this == '】'
        @JvmStatic
        fun Char.isLParen() = this == '(' || this == '（'
        @JvmStatic
        fun Char.isRParen() = this == ')' || this == '）'
        @JvmStatic
        fun Char.isSpace() = this == ' ' || this == '\t' || this == '\u00A0' || this == '\u3000'
        @JvmStatic
        fun Char.isEOS() = this == '\n' || this == '。'
    }

}
