package gl.ky.adeyaka.old

import gl.ky.adeyaka.old.LexUtil.isComma
import gl.ky.adeyaka.old.LexUtil.isEOS
import gl.ky.adeyaka.old.LexUtil.isIdChar
import gl.ky.adeyaka.old.LexUtil.isIdStart
import gl.ky.adeyaka.old.LexUtil.isLBrace
import gl.ky.adeyaka.old.LexUtil.isLBracket
import gl.ky.adeyaka.old.LexUtil.isLParen
import gl.ky.adeyaka.old.LexUtil.isNumber
import gl.ky.adeyaka.old.LexUtil.isQuote
import gl.ky.adeyaka.old.LexUtil.isRBrace
import gl.ky.adeyaka.old.LexUtil.isRBracket
import gl.ky.adeyaka.old.LexUtil.isRParen
import gl.ky.adeyaka.old.LexUtil.isSpace

fun main() {
    val script = """
        脚本组 示例脚本 {
            设置“甲”为 1，设置 乙 为 真，设置'丙'为 甲
            设置 丁 为 “你好，世界！”
        }
    """.trimIndent()
    val ts = Lexer(script).get()
    println(ts)
}


object Keywords {
    const val SCRIPT_GROUP = "脚本组"
    val BOOL_TRUE = listOf("true", "yes", "真", "是")
    val BOOL_FALSE = listOf("false", "no", "假", "否")
    val BOOL = BOOL_TRUE + BOOL_FALSE
}

class Token(var type: Type, val value: String) {
    enum class Type {
        EOF,
        EOS, // \n
        COMMA,

        ID,
        NUMBER,
        STRING,
        BOOL_TRUE,
        BOOL_FALSE,

        LBRACE,
        RBRACE,
        LBRACKET,
        RBRACKET,
        LPAREN,
        RPAREN,

        KW_GROUP,
    }

    override fun toString(): String {
        return "[$type : $value]"
    }
}

class Lexer(input: String) {
    fun get() = lex()

    private val input = input.trim()
        .replace("\r", "\n")

    private var offset = 0

    private inline fun hasMore() = offset < input.length
    private inline fun skipWhitespace() { while (input[offset].isSpace()) offset++ }
    private inline fun skipLine() { while (input[offset] != '\n') offset++ }
    private inline fun skip() { offset++ }
    private inline fun skip(i: Int) { offset += i }

    private fun lex(): TokenStream {
        val tokens = mutableListOf<Token>()
        do {
            skipWhitespace()
            val c = input[offset]
            when {
                c.isEOS() -> { tokens += Token(Token.Type.EOS, "\n"); skip() }
                c.isComma() -> { tokens += Token(Token.Type.COMMA, ","); skip() }
                c.isLParen() -> { tokens += Token(Token.Type.LPAREN, "("); skip() }
                c.isRParen() -> { tokens += Token(Token.Type.RPAREN, ")"); skip() }
                c.isLBracket() -> { tokens += Token(Token.Type.LBRACKET, "["); skip() }
                c.isRBracket() -> { tokens += Token(Token.Type.RBRACKET, "]"); skip() }
                c.isLBrace() -> { tokens += Token(Token.Type.LBRACE, "{"); skip() }
                c.isRBrace() -> { tokens += Token(Token.Type.RBRACE, "}"); skip() }
                c == '#' -> skipLine()
                c in '0'..'9' -> tokens += Token(Token.Type.NUMBER, readNumber())
                c.isQuote() -> tokens += Token(Token.Type.STRING, readString())
                c.isIdStart() -> tokens += Token(Token.Type.ID, readId())
                else ->  throw RuntimeException("Unexpected character: $c")
            }
        } while (hasMore())
        //tokens += Token(Token.Type.EOF, "")

        tokens.map {
            when (it.type) {
                Token.Type.ID -> {
                    when (it.value) {
                        Keywords.SCRIPT_GROUP -> it.type = Token.Type.KW_GROUP
                        in Keywords.BOOL_TRUE -> it.type = Token.Type.BOOL_TRUE
                        in Keywords.BOOL_FALSE -> it.type = Token.Type.BOOL_FALSE
                    }
                }
                else -> {}
            }
        }

        return TokenStream(tokens)
    }

    private fun readString(): String {
        skip()
        val start = offset
        while (!input[offset].isQuote()) {
            if (input[offset] == '\\' && input[offset + 1].isQuote()) {
                skip(2)
            } else {
                skip()
            }
        }
        skip()
        return input.substring(start, offset - 1)
    }

    private fun readNumber(): String {
        val start = offset
        while (input[offset].isNumber()) {
            skip()
        }
        if(input[offset] == '.') {
            skip()
            while (input[offset].isNumber()) {
                skip()
            }
        }
        return input.substring(start, offset)
    }

    private fun readId(): String {
        val start = offset
        while (input[offset].isIdChar()) {
            skip()
        }
        return input.substring(start, offset)
    }
}

object LexUtil {
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
    fun Char.isEOS() = this == '\n'
}

class TokenStream(private val tokens: List<Token>) {
    var index = 0

    operator fun get(i: Int = index) = tokens[i]
    operator fun invoke(offset: Int = 0) = peek(offset)
    fun peek(offset: Int = 0) = tokens[index + offset]
    fun hasNext() = index < tokens.size
    fun next() = tokens[index++]
    fun skip(count: Int = 1) { index += count }

    override fun toString(): String {
        return "[TokenStream " + tokens.joinToString(" ") + "]"
    }
}

object TokenStreamUtil {
    @JvmStatic
    fun TokenStream.skipType(vararg types: Token.Type) {
        while(peek().type in types) skip()
    }
    @JvmStatic
    fun TokenStream.match(type: Token.Type) : Token {
        return if (peek().type == type) next() else throw RuntimeException("Expected a $type token, but got ${peek().type}")
    }
    @JvmStatic
    fun TokenStream.match(s: String) : Token {
        return if (peek().value == s) next() else throw RuntimeException("Expected a $s, but got ${peek().value}")
    }
    @JvmStatic
    fun TokenStream.match(vararg types: Token.Type) : List<Token> {
        val tokens = mutableListOf<Token>()
        for (type in types) if(peek().type == type) tokens += next()
        else throw RuntimeException("Expected a $type token, but got ${peek().type}")
        return tokens
    }
    @JvmStatic
    fun TokenStream.testMatch(type: Token.Type, offset: Int = 0) = peek(offset).type == type
    @JvmStatic
    fun TokenStream.testMatch(s: String, offset: Int = 0) = peek(offset).value == s
    @JvmStatic
    fun TokenStream.testMatch(vararg types: Token.Type, offset: Int = 0): Boolean {
        var count = 0
        for (type in types) if(peek(offset + count).type == type) count++
        else return false
        return true
    }
    @JvmStatic
    fun TokenStream.tryMatch(type: Token.Type) : Boolean {
        if (testMatch(type)) {
            skip()
            return true
        }
        return false
    }
    @JvmStatic
    fun TokenStream.tryMatch(s: String) : Boolean {
        if (testMatch(s)) {
            skip()
            return true
        }
        return false
    }
    @JvmStatic
    fun TokenStream.tryMatch(vararg types: Token.Type) : Boolean {
        if (testMatch(*types)) {
            skip(types.size)
            return true
        }
        return false
    }

}