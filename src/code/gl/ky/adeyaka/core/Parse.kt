package gl.ky.adeyaka.core

interface ActionParser {
    /**
     * try to parse some tokens and return an action
     * @return the action if successful, null otherwise, if failed, tokens won't be really consumed.
     */
    fun parse(input: TokenStream): Action?
}

interface LiteralParser<T> {
    /**
     * try to parse tokens and return a CONSTANT VALUE
     * @return the value if successful, null otherwise, if failed, tokens won't be really consumed.
     */
    fun parse(input: TokenStream): T?
}

interface ValueParser<T> {
    /**
     * try to parse tokens and return a runtime-sure value
     * @return the value if successful, null otherwise, if failed, tokens won't be really consumed.
     */
    fun parse(input: TokenStream): RuntimeValue<T>?
}

interface PrefixParser<T> {
    /**
     * try to parse tokens before a value and return a transformer
     * @return the value if successful, null otherwise, if failed, tokens won't be really consumed.
     */
    fun parse(input: TokenStream): RuntimeValue.Transformer<T>?
}

interface SuffixParser<T> {
    /**
     * try to parse tokens after a value and return a transformer
     * @return the value if successful, null otherwise, if failed, tokens won't be really consumed.
     */
    fun parse(input: TokenStream): RuntimeValue.Transformer<T>?
}

class Parser(val actionParsers: List<ActionParser>, val literalParsers: Map<*, List<LiteralParser<*>>>) {
    private fun parse(source: TokenStream): List<Segment> {
        val result = mutableListOf<Segment>()
        while (source.hasNext()) result += parseSegment(source)
        return result
    }

    private fun parseSegment(source: TokenStream): Segment {
        val result = mutableListOf<Sentence>()
        source.match("脚本段")
        val name = source.next().value
        source.match("{")
        while(source.peek().value != "}") {
            result += parseSentence(source)
        }
        source.match("}")
        return Segment(name, result)
    }

    private fun parseSentence(source: TokenStream): Sentence {
        val result = mutableListOf<Action>()
        while(!source.peek().isEOS()) {
            result += parseAction(source)
        }
        return Sentence(result)
    }

    private fun parseAction(source: TokenStream): Action {
        for(parser in actionParsers) {
            return parser.parse(source) ?: continue
        }
        throw RuntimeException("unable to parse")
    }

    private fun parseValue(source: TokenStream): RuntimeValue<*> {
        TODO()
    }

    private fun parsePrefix(source: TokenStream): RuntimeValue<*> {
        TODO()
    }

    private fun parseSuffix(source: TokenStream): RuntimeValue<*> {
        TODO()
    }

    private fun parseString(source: TokenStream): String {
        TODO()
    }

    private fun parseBoolean(source: TokenStream): Boolean {
        TODO()
    }

    private fun parseNumber(source: TokenStream): Double {
        TODO()
    }



}

    object ParseUtil {
        @JvmStatic
        fun TokenStream.match(c: Char) = if(peek() == c) next()
        else throw ParseException("Expected '$c' but found '${peek()}'")
        @JvmStatic
        fun TokenStream.match(s: String) = if(peekString(s.length) == s) skip(s.length)
        else throw ParseException("Expected '$s' but found '${peekString(s.length)}'")
        @JvmStatic
        fun TokenStream.check(c: Char) = peek() == c
        @JvmStatic
        fun TokenStream.check(s: String) = peekString(s.length) == s
        @JvmStatic
        fun TokenStream.matchIfPresent(c: Char): Boolean = if(check(c)) {
            skip()
            true
        } else false
        @JvmStatic
        fun TokenStream.matchIfPresent(s: String): Boolean = if(check(s)) {
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

