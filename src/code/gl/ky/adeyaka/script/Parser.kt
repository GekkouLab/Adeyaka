package gl.ky.adeyaka.script

import gl.ky.adeyaka.script.TokenStream.Companion.eatIfPresent
import gl.ky.adeyaka.script.TokenStream.Companion.expect
import gl.ky.adeyaka.script.TokenStream.Companion.isNext

fun main() {
    val t = Lexer.lex("""
        特效组"adeyaka"{
            设置a为1；b为#b；c为{Kouyou};d为“d”
        }
        """.trimIndent())
    println(t)
    println(Parser.parseFile(t).toString())
}

// AST

sealed interface ASTNode

class ScriptFile(val groups: List<ScriptGroup>) : ASTNode {
    override fun toString() = buildString {
        append("脚本文件 {\n")
        groups.forEach(::append)
        append("}")
    }
}
class ScriptGroup(val name: String, val sens: List<Sentence>) : ASTNode {
    override fun toString() = buildString {
        append("  特效组 $name {\n")
        sens.forEach(::append)
        append("  }\n")
    }
}
class Sentence(val clauses: List<Clause>) : ASTNode {
    override fun toString() = buildString {
        append("    句子 {\n")
        clauses.forEach(::append)
        append("    }\n")
    }
}

sealed interface Clause : ASTNode

/**
 * 动词，一个句中要执行的动作
 * 可以自带一些参数，优先于别的副词提供的参数
 */
class Verb(val name: String, val data: Map<String, Expr>) : Clause {
    override fun toString() = buildString {
        append("      动词 $name 带有参数 {\n")
        data.forEach { (k, v) ->
            append("        $k = ")
            append(v)
            append("\n")
        }
        append("      }\n")
    }
}

/**
 * 副词，为一个句中的动作提供参数
 */
class Adverb(val data: Map<String, Expr>) : Clause {
    override fun toString() = buildString {
        append("      副词 {\n")
        data.forEach { (k, v) ->
            append("        $k = ")
            append(v)
            append("\n")
        }
        append("      }\n")
    }
}

sealed interface Expr : ASTNode

class Variable(val name: String) : Expr {
    override fun toString() = "变量 [name]"
}
class Bool(val value: Boolean) : Expr {
    override fun toString() = "布尔值 [$value]"
}
class Number(val value: Double) : Expr {
    override fun toString() = "数值 [$value]"
}
class Str(val value: String) : Expr {
    override fun toString() = "字符串 [$value]"
}
class Player(val name: String) : Expr {
    override fun toString() = "玩家 [$name]"
}
class Position(val x: Double, val y: Double, val z: Double) : Expr {
    override fun toString() = "坐标 [$x, $y, $z]"
}

// Parser

/**
 * 解析整个文件的 Parser
 */
object Parser {
    val parsers = mutableListOf<ClauseParser>(
        SetClauseParser,
    )

    fun register(name: String, parser: ClauseParser) {
        parsers.add(parser)
    }

    fun parseFile(input: TokenStream): ScriptFile {
        val groups = mutableListOf<ScriptGroup>()
        while (!input.isNext(TokenType.EOF)) {
            groups.add(parseGroup(input))
        }
        return ScriptFile(groups)
    }

    fun parseGroup(input: TokenStream): ScriptGroup {
        input.expect(TokenType.KW_GROUP)
        val name = input.expect(TokenType.SYMBOL).value
        input.expect(TokenType.LBRACE)
        val sens = mutableListOf<Sentence>()
        while(input.eatIfPresent(TokenType.EOS));
        while (!input.isNext(TokenType.RBRACE)) {
            sens.add(parseSentence(input))
            while(input.eatIfPresent(TokenType.EOS));
        }
        input.expect(TokenType.RBRACE)
        return ScriptGroup(name, sens)
    }

    fun parseSentence(input: TokenStream): Sentence {
        val clauses = mutableListOf<Clause>()
        while (!input.isNext(TokenType.EOS) && !input.isNext(TokenType.RBRACE)) {
            clauses.addAll(parseClause(input))
        }
        return Sentence(clauses)
    }

    fun parseClause(input: TokenStream): List<Clause> {
        return parsers.first { it.canParse(input) }.parse(input)
    }

    fun parseExpr(input: TokenStream): Expr {
        return when (input.peek().type) {
            TokenType.HASH -> {
                input.expect(TokenType.HASH)
                val name = input.expect(TokenType.SYMBOL).value
                Variable(name)
            }
            TokenType.SYMBOL -> {
                Str(input.expect(TokenType.SYMBOL).value)
            }
            TokenType.NUMBER -> {
                Number(input.expect(TokenType.NUMBER).value.toDouble())
            }
            TokenType.BOOL -> {
                Bool(when(input.next().value) {
                    "true", "yes", "真", "是" -> true
                    "false", "no", "假", "否" -> false
                    else -> throw IllegalArgumentException("Invalid bool value: ${input.peek().value}")
                })
            }
            TokenType.LBRACKET -> {
                input.expect(TokenType.LBRACKET)
                val x = input.expect(TokenType.NUMBER).value.toDouble()
                input.expect(TokenType.COMMA)
                val y = input.expect(TokenType.NUMBER).value.toDouble()
                input.expect(TokenType.COMMA)
                val z = input.expect(TokenType.NUMBER).value.toDouble()
                input.expect(TokenType.RBRACKET)
                Position(x, y, z)
            }
            TokenType.LBRACE -> {
                input.expect(TokenType.LBRACE)
                val name = input.expect(TokenType.SYMBOL).value
                input.expect(TokenType.RBRACE)
                Player(name)
            }
            else -> throw IllegalArgumentException("Expected expression, got ${input.peek().type}")
        }
    }
}

/**
 * 将一段分句转换为一些词
 * 一个分句一般为一个 EOC 到 下一个 EOC 的区间
 */
interface ClauseParser {
    fun canParse(tokenStream: TokenStream): Boolean
    fun parse(input: TokenStream): List<Clause>
}

object SetClauseParser : ClauseParser {
    override fun canParse(tokenStream: TokenStream) = tokenStream.isNext(TokenType.KW_SET)
    override fun parse(input: TokenStream): List<Clause> {
        input.expect(TokenType.KW_SET)
        val assigns = mutableMapOf<String, Expr>()
        do {
            input.eatIfPresent(TokenType.SEMICOLON)
            val name = input.expect(TokenType.SYMBOL).value
            input.expect(TokenType.KW_ASSIGN)
            val expr = Parser.parseExpr(input)
            assigns[name] = expr
        } while (input.isNext(TokenType.SEMICOLON))
        if(input.eatIfPresent(TokenType.COMMA)) input.eatIfPresent(TokenType.EOS)
        return listOf(Verb("set", assigns))
    }
}
