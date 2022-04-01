package gl.ky.adeyaka.script

import gl.ky.adeyaka.script.LexUtil.isComma
import gl.ky.adeyaka.script.LexUtil.isEOS
import gl.ky.adeyaka.script.LexUtil.isIdChar
import gl.ky.adeyaka.script.LexUtil.isIdStart
import gl.ky.adeyaka.script.LexUtil.isLBrace
import gl.ky.adeyaka.script.LexUtil.isLBracket
import gl.ky.adeyaka.script.LexUtil.isLParen
import gl.ky.adeyaka.script.LexUtil.isNumber
import gl.ky.adeyaka.script.LexUtil.isQuote
import gl.ky.adeyaka.script.LexUtil.isRBrace
import gl.ky.adeyaka.script.LexUtil.isRBracket
import gl.ky.adeyaka.script.LexUtil.isRParen
import gl.ky.adeyaka.script.LexUtil.isSpace

fun main() {
    println(Lexer("""
        "a"
    """.trimIndent()).readString())
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
}

class TokenStream(val tokens: List<Token>) {
    private var index = 0
    private val savepoint = mutableListOf<Int>()

    fun peek() = tokens[index]
    fun hasNext() = index < tokens.size
    fun next() = tokens[index++]

    fun save() { savepoint.add(index) }
    fun restore() { index = savepoint.removeLast() }
    fun cancel() { savepoint.removeLast() }
}

class Lexer(input: String) {
    val input = input.trim()
        .replace("\r", "\n")
        .replace("\t", " ")

    fun get() = lex()

    var offset = 0

    private inline fun hasMore() = offset < input.length
    private inline fun skipWhitespace() { while (input[offset].isSpace()) offset++ }
    private inline fun skipLine() { while (input[offset] != '\n') offset++ }
    private inline fun skip() { offset++ }
    private inline fun skip(i: Int) { offset += i }

    fun lex(): TokenStream {
        val tokens = mutableListOf<Token>()
        do {
            skipWhitespace()
            val c = input[offset]
            when {
                c == '#' -> skipLine()
                c.isEOS() -> tokens += Token(Token.Type.EOS, "\n")
                c in '0'..'9' -> tokens += Token(Token.Type.NUMBER, readNumber())
                c.isComma() -> tokens += Token(Token.Type.COMMA, ",")
                c.isLBrace() -> tokens += Token(Token.Type.LBRACE, "{")
                c.isRBrace() -> tokens += Token(Token.Type.RBRACE, "}")
                c.isLBracket() -> tokens += Token(Token.Type.LBRACKET, "[")
                c.isRBracket() -> tokens += Token(Token.Type.RBRACKET, "]")
                c.isLParen() -> tokens += Token(Token.Type.LPAREN, "(")
                c.isRParen() -> tokens += Token(Token.Type.RPAREN, ")")
                c.isQuote() -> tokens += Token(Token.Type.STRING, readString())
                c.isIdStart() -> tokens += Token(Token.Type.ID, readId())
                else ->  throw RuntimeException("Unexpected character: $c")
            }
        } while (hasMore())
        tokens += Token(Token.Type.EOF, "")

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

    fun readString(): String {
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
        return input.substring(start, offset)
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
    fun Char.isSpace() = this == ' ' || this == '\t' || this == '\n' || this == '\r' || this == '\u00A0' || this == '\u3000'
    @JvmStatic
    fun Char.isEOS() = this == '\n'
}

object TokenStreamUtil {
    @JvmStatic
    fun TokenStream.expect(type: Token.Type) : Token {
        if (peek().type != type) {
            throw RuntimeException("Expected a token of type $type, but got ${peek().type}")
        }
        return next()
    }
    @JvmStatic
    fun TokenStream.expect(s: String) : Token {
        if (peek().value != s) {
            throw RuntimeException("Expected a $s, but got ${peek().value}")
        }
        return next()
    }
    @JvmStatic
    fun TokenStream.expect(s: String, type: Token.Type): Token {
        if (peek().value != s || peek().type != type) {
            throw RuntimeException("Expected a token $s of type $type, but got ${peek().value} of type ${peek().type}")
        }
        return next()
    }
    @JvmStatic
    fun TokenStream.expect(vararg types: Token.Type) : Token {
        if (peek().type !in types) {
            throw RuntimeException("Expected a token of type one of ${types.joinToString()}, but got ${peek().type}")
        }
        return next()
    }
    @JvmStatic
    fun TokenStream.isNext(type: Token.Type) = peek().type == type
    @JvmStatic
    fun TokenStream.isNext(s: String) = peek().value == s
    @JvmStatic
    fun TokenStream.isNext(s: String, type: Token.Type) = peek().value == s && peek().type == type
    @JvmStatic
    fun TokenStream.isNext(vararg types: Token.Type) = peek().type in types
    @JvmStatic
    fun TokenStream.eatIfPresent(type: Token.Type) : Boolean {
        if (isNext(type)) {
            next()
            return true
        }
        return false
    }
    @JvmStatic
    fun TokenStream.eatIfPresent(s: String) : Boolean {
        if (isNext(s)) {
            next()
            return true
        }
        return false
    }
    @JvmStatic
    fun TokenStream.eatIfPresent(s: String, type: Token.Type) : Boolean {
        if (isNext(s, type)) {
            next()
            return true
        }
        return false
    }
}