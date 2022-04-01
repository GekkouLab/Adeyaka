package gl.ky.adeyaka.script

import gl.ky.adeyaka.script.RuleComponent.MatchAsAdverb.Type.*
import gl.ky.adeyaka.script.TokenStreamUtil.match
import gl.ky.adeyaka.script.TokenStreamUtil.testMatch
import gl.ky.adeyaka.script.TokenStreamUtil.tryMatch
import gl.ky.adeyaka.script.TokenStreamUtil.skipType

fun main() {
    val rules = Rule.fromStrings(
        "把 <name:string> {设为} <value:any>",
        "{设置} <name:string> 为 <value:any>",
        "在 <target:position> {释放粒子} <name:string>",
        "{释放粒子} <name:string>",
        "在 <target:player> 处"
    )
    println(rules)
    val script = """
        脚本组 示例脚本 {
            设置“一个变量”为 1，在{KouyouX}处，释放粒子“水花”
            在[0,1,2]释放粒子“火焰”
        }
    """.trimIndent()
    val ts = Lexer(script).get()
    println(ts)
    val ast = Parser(ts, rules).get()
    println(ast)
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
        append("  脚本组 $name {\n")
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

class Clause(val words: List<Word>) : ASTNode {
    override fun toString() = buildString {
        append("      分句 {")
        words.forEach(::append)
        append("}\n")
    }
}

sealed interface Word : ASTNode

/**
 * 动词，一个句中要执行的动作
 */
class Verb(val name: String) : Word {
    override fun toString() = "[动词 $name] "
}

/**
 * 副词，为一个句中的动作提供参数
 */
class Adverb(val name: String, val value: Expr) : Word {
    override fun toString() = "[副词 $name $value] "
}

sealed interface Expr : ASTNode {
    class Var(val name: String) : Expr {
        override fun toString() = "[变量 $name]"
    }
    class Bool private constructor(val value: Boolean) : Expr {
        companion object {
            @JvmStatic
            val TRUE = Bool(true)
            @JvmStatic
            val FALSE = Bool(false)
        }

        override fun toString() = "[布尔值 $value]"
    }
    class Num(val value: Double) : Expr {
        override fun toString() = "[数值 $value]"
    }
    class Str(val value: String) : Expr {
        override fun toString() = "[字符串 $value]"
    }
    class Player(val name: String) : Expr {
        override fun toString() = "[玩家 $name]"
    }
    class Pos(val x: Double, val y: Double, val z: Double) : Expr {
        override fun toString() = "[位置 $x, $y, $z]"
    }
}

// Parser

/**
 * 解析整个文件的 Parser
 */
class Parser(val input: TokenStream, val rules: List<Rule>) {
    fun get(): ScriptFile = parseFile()

    private fun parseFile(): ScriptFile {
        val groups = mutableListOf<ScriptGroup>()
        while(input.hasNext()) groups += parseGroup()
        return ScriptFile(groups)
    }

    private fun parseGroup(): ScriptGroup {
        input.match(Token.Type.KW_GROUP)
        val name = input.match(Token.Type.ID).value
        input.match(Token.Type.LBRACE)
        val sens = mutableListOf<Sentence>()
        while(!input.testMatch(Token.Type.RBRACE)) sens += parseSentence()
        input.match(Token.Type.RBRACE)
        return ScriptGroup(name, sens)
    }

    private fun parseSentence(): Sentence {
        val clauses = mutableListOf<Clause>()
        input.skipType(Token.Type.EOS)
        do clauses += parseClause()
        while (input.tryMatch(Token.Type.COMMA))
        input.match(Token.Type.EOS)
        return Sentence(clauses)
    }

    private fun parseClause(): Clause {
        for (rule in rules) tryRule(rule)?.let { return it }
        throw RuntimeException("No rule matched")
    }

    private fun tryRule(rule: Rule): Clause? {
        val result = mutableListOf<Word>()
        var offset = 0
        for(component in rule.components) {
            when(component) {
                is RuleComponent.MatchAndIgnore -> {
                    if(!input.testMatch(component.text, offset)) return null
                    offset++
                }
                is RuleComponent.MatchAsVerb -> {
                    result += if (input.testMatch(component.verb, offset)) Verb(input(offset).value)
                    else return null
                    offset++
                }
                is RuleComponent.MatchAsAdverb -> {
                    if(input.testMatch(Token.Type.ID, offset)) {
                        result += Adverb(component.name, Expr.Var(input(offset).value))
                        offset++
                        continue
                    }

                    when(component.type) {
                        STRING -> {
                            result += if (input.testMatch(Token.Type.STRING, offset)) Adverb(
                                component.name,
                                Expr.Str(input(offset).value)
                            )
                            else return null
                            offset++
                        }
                        NUMBER -> {
                            result += if (input.testMatch(Token.Type.NUMBER, offset)) Adverb(
                                component.name,
                                Expr.Num(input(offset).value.toDouble())
                            )
                            else return null
                            offset++
                        }
                        BOOL -> {
                            result += if (input.testMatch(Token.Type.BOOL_TRUE, offset)) Adverb(
                                component.name,
                                Expr.Bool.TRUE
                            )
                            else if (input.testMatch(Token.Type.BOOL_FALSE, offset)) Adverb(component.name, Expr.Bool.FALSE)
                            else return null
                            offset++
                        }
                        PLAYER -> {
                            result += if (input.testMatch(
                                    Token.Type.LBRACE,
                                    Token.Type.ID,
                                    Token.Type.RBRACE,
                                    offset = offset
                                )
                            )
                                Adverb(component.name, Expr.Player(input(offset + 1).value))
                            else return null
                            offset += 3
                        }
                        POSITION -> {
                            result += if (input.testMatch(
                                    Token.Type.LBRACKET,
                                    Token.Type.NUMBER,
                                    Token.Type.COMMA,
                                    Token.Type.NUMBER,
                                    Token.Type.COMMA,
                                    Token.Type.NUMBER,
                                    Token.Type.RBRACKET,
                                    offset = offset
                                )
                            )
                                Adverb(
                                    component.name, Expr.Pos(
                                        input(offset + 1).value.toDouble(),
                                        input(offset + 3).value.toDouble(), input(offset + 5).value.toDouble()
                                    )
                                )
                            else return null
                            offset += 7
                        }
                        ANY -> result += when(input(offset).type) {
                            Token.Type.ID -> {
                                Adverb(component.name, Expr.Var(input(offset).value)).also { offset++ }
                            }
                            Token.Type.NUMBER -> {
                                Adverb(component.name, Expr.Num(input(offset).value.toDouble())).also { offset++ }
                            }
                            Token.Type.STRING -> {
                                Adverb(component.name, Expr.Str(input(offset).value)).also { offset++ }
                            }
                            Token.Type.BOOL_TRUE -> {
                                Adverb(component.name, Expr.Bool.TRUE).also { offset++ }
                            }
                            Token.Type.BOOL_FALSE -> {
                                Adverb(component.name, Expr.Bool.FALSE).also { offset++ }
                            }
                            Token.Type.LBRACE -> {
                                if (input.testMatch(Token.Type.LBRACE, Token.Type.ID, Token.Type.RBRACE, offset = offset)) {
                                    offset += 3
                                    Adverb(component.name, Expr.Player(input(offset + 1).value))
                                } else return null
                            }
                            Token.Type.LBRACKET -> {
                                if(input.testMatch(Token.Type.LBRACKET, Token.Type.NUMBER, Token.Type.COMMA,
                                        Token.Type.NUMBER, Token.Type.COMMA, Token.Type.NUMBER, Token.Type.RBRACKET, offset = offset)) {
                                    offset += 7
                                    Adverb(
                                        component.name, Expr.Pos(
                                            input(offset + 1).value.toDouble(),
                                            input(offset + 3).value.toDouble(),
                                            input(offset + 5).value.toDouble()
                                        )
                                    )
                                } else return null
                            }
                            else -> return null
                        }
                    }
                }
            }
        }
        input.skip(offset)
        return Clause(result)
    }

}
