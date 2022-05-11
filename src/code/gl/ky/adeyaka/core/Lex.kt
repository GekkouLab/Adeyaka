package gl.ky.adeyaka.core

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

class TokenType {
    companion object {
        @JvmStatic
        val STRING = TokenType()
        @JvmStatic
        val INTEGER = TokenType()
        @JvmStatic
        val FLOAT = TokenType()
        @JvmStatic
        val BOOLEAN = TokenType()
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

class Lexer(val rules: List<LexRule>) {
    fun lex(source: String): TokenStream {
        val tokens = mutableListOf<Token>()
        return TokenStream(tokens)
    }
}


