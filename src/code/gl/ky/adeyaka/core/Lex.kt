package gl.ky.adeyaka.core

class LexRule(val regex: Boolean, val type: TokenType, val value: String) {
    companion object {
        @JvmStatic
        val STRING = LexRule(true, TokenType.STRING, "\"(.*?)\"")
        @JvmStatic
        val INTEGER = LexRule(true, TokenType.INTEGER, "\\d+")
        @JvmStatic
        val FLOAT = LexRule(true, TokenType.FLOAT, "\\d+\\.\\d+")
        @JvmStatic
        val TRUE = LexRule(true, TokenType.BOOLEAN, "true")
        @JvmStatic
        val FALSE = LexRule(true, TokenType.BOOLEAN, "false")
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

class Lexer(val rules: List<LexRule>) {
    fun lex(source: String): TokenStream {
        val len = source.length
        val tokens = mutableListOf<Token>()
        var index = 0
        while(index < len) {
        }
        return TokenStream(tokens)
    }
}
