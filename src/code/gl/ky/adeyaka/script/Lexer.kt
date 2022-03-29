package gl.ky.adeyaka.script

object Lexer {
    fun lex(input: String): TokenStream {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            when (input[i]) {
                '(', '（' -> tokens.add(Token(TokenType.LPAREN, "("))
                ')', '）' -> tokens.add(Token(TokenType.RPAREN, ")"))
                '[', '【' -> tokens.add(Token(TokenType.LBRACKET, "["))
                ']', '】' -> tokens.add(Token(TokenType.RBRACKET, "]"))
                '{' -> tokens.add(Token(TokenType.LBRACE, "{"))
                '}' -> tokens.add(Token(TokenType.RBRACE, "}"))
                '#' -> tokens.add(Token(TokenType.HASH, "#"))
                ';', '；' -> tokens.add(Token(TokenType.SEMICOLON, ";"))
                ',', '，' -> tokens.add(Token(TokenType.COMMA, ","))
                ' ', '\t' -> {}
                '\n', '。' -> tokens.add(Token(TokenType.EOS, ""))
                '"', '\'', '“', '”' -> {
                    val sb = StringBuilder()
                    i++
                    while (input[i] !in listOf('"', '\'', '“', '”')) {
                        sb.append(input[i])
                        i++
                    }
                    tokens.add(Token(TokenType.SYMBOL, sb.toString()))
                }
                in '0'..'9' -> {
                    "[0-9]+(\\.[0-9]+)?".toRegex().find(input, i)?.let {
                        tokens.add(Token(TokenType.NUMBER, it.value))
                        i += it.value.length - 1
                    }
                }
                else -> {
                    if(input.startsWith(Keyword.group, i)) {
                        tokens.add(Token(TokenType.KW_GROUP, Keyword.group))
                        i += Keyword.group.length - 1
                    }
                    else if(input.startsWith(Keyword.set, i)) {
                        tokens.add(Token(TokenType.KW_SET, Keyword.set))
                        i += Keyword.set.length - 1
                    }
                    else if(input.startsWith(Keyword.assign, i)) {
                        tokens.add(Token(TokenType.KW_ASSIGN, Keyword.assign))
                        i += Keyword.assign.length - 1
                    }
                    else "[A-Za-z0-9_][A-Za-z0-9_]*".toRegex().find(input, i)?.let {
                        tokens.add(Token(TokenType.SYMBOL, it.value))
                        i += it.value.length - 1
                    } ?: run {
                        throw IllegalArgumentException("Invalid character: '${input[i]}' at $i")
                    }
                }
            }
            i++
        }
        tokens.add(Token(TokenType.EOF, ""))
        return TokenStream(tokens)
    }
}

object Keyword {
    const val group = "特效组"
    const val set = "设置"
    const val assign = "为"
}

enum class TokenType {
    SYMBOL,
    NUMBER,
    BOOL,

    COMMA,
    SEMICOLON,
    EOS, // end of sentence
    EOF, // end of file

    LBRACE,
    RBRACE,
    LBRACKET,
    RBRACKET,
    LPAREN,
    RPAREN,
    HASH,

    KW_GROUP,
    KW_SET,
    KW_ASSIGN,
}

class Token(val type: TokenType, val value: String) {
    override fun toString(): String {
        return "[$type: $value]"
    }
}

class TokenStream(val tokens: List<Token>) {
    var index = 0
    fun next() = tokens[index++]
    fun peek() = tokens[index]
    fun peek(offset: Int) = tokens[index + offset]
    fun hasNext() = index < tokens.size

    override fun toString(): String {
        return tokens.joinToString(", ")
    }
    companion object {
        @JvmStatic
        fun TokenStream.expect(s: String): Token {
            if (peek().value != s) throw IllegalArgumentException("expect $s, but got ${peek().value}")
            return next()
        }
        @JvmStatic
        fun TokenStream.expect(t: TokenType): Token {
            if (peek().type != t) throw IllegalArgumentException("expect token of type $t, but got ${peek().type}")
            return next()
        }
        @JvmStatic
        fun TokenStream.isNext(s: String): Boolean {
            return peek().value == s
        }
        @JvmStatic
        fun TokenStream.isNext(t: TokenType): Boolean {
            return peek().type == t
        }
        @JvmStatic
        fun TokenStream.eatIfPresent(s: String): Boolean {
            if (peek().value == s) {
                next()
                return true
            }
            return false
        }
        @JvmStatic
        fun TokenStream.eatIfPresent(t: TokenType): Boolean {
            if (peek().type == t) {
                next()
                return true
            }
            return false
        }
    }
}
