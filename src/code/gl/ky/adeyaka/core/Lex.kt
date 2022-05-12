package gl.ky.adeyaka.core
/**
class LexRule(val kind: Kind, val type: TokenType, val value: String) {
    enum class Kind {
        FULL, REGEX,
    }

    companion object {
        @JvmStatic
        val STRING = LexRule(Kind.REGEX, TokenType.STRING, "\"(.*?)\"")
        @JvmStatic
        val INTEGER = LexRule(Kind.REGEX, TokenType.INTEGER, "\\d+")
        @JvmStatic
        val FLOAT = LexRule(Kind.REGEX, TokenType.FLOAT, "\\d+\\.\\d+")
        @JvmStatic
        val TRUE = LexRule(Kind.FULL, TokenType.BOOLEAN, "true")
        @JvmStatic
        val FALSE = LexRule(Kind.FULL, TokenType.BOOLEAN, "false")
    }
}
*/
class TokenType {
    companion object {
        @JvmStatic
        val STRING = TokenType()
        @JvmStatic
        val NUMBER = TokenType()
        @JvmStatic
        val BOOLEAN = TokenType()
        @JvmStatic
        val EDGE = TokenType()
        @JvmStatic
        val WORD = TokenType()
    }
}

class Token(val type: TokenType, val value: String)

class TokenStream(val tokens: List<Token>, var index: Int = 0) {
    fun clone() = TokenStream(tokens, index)
    fun jump(index: Int) { this.index = index }
    fun hasNext(): Boolean = index < tokens.size
    fun next(): Token = tokens[index++]
    @JvmOverloads
    fun peek(offset: Int = 0): Token = tokens[index + offset]
    @JvmOverloads
    fun skip(count: Int = 1) { index += count }
}

class Lexer(dict: List<String>) {
    val dict: List<String> = dict.sortedWith { a, b ->
            if(a.length > b.length) -1
            else 0
        }

    fun lex(source: String): TokenStream {
        val len = source.length
        val tokens = mutableListOf<Token>()
        var index = 0
        while(index < len) {
            when(source[index]) {
                ' ', '\t' -> {}
                '\n' -> tokens.add(Token(TokenType.EDGE, "\n"))
                '{' -> tokens.add(Token(TokenType.EDGE, "{"))
                '}' -> tokens.add(Token(TokenType.EDGE, "}"))
                in '0'..'9' -> {
                    "^\\d+".toRegex().find(source, index)!!.let {
                        tokens.add(Token(TokenType.NUMBER, it.value))
                        index += it.value.length
                    }
                    continue
                }
                '\'', '"', '“', '”' -> {
                    "^['\"“”].*?['\"“”]".toRegex().find(source, index)!!.let {
                        tokens.add(Token(TokenType.STRING, it.value))
                        index += it.value.length
                    }
                    continue
                }
                else -> {
                    when {
                        source.startsWith("true", index) -> {
                            tokens.add(Token(TokenType.BOOLEAN, "true"))
                            index += "true".length
                            continue
                        }
                        source.startsWith("false", index) -> {
                            tokens.add(Token(TokenType.BOOLEAN, "false"))
                            index += "false".length
                            continue
                        }
                    }
                    for(word in dict) if(source.startsWith(word, index)) {
                        tokens.add(Token(TokenType.WORD, word))
                        index += word.length
                        continue
                    }
                }
            }
            index++
        }
        return TokenStream(tokens)
    }
}
