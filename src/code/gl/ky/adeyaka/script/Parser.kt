package gl.ky.adeyaka.script

import gl.ky.adeyaka.script.GetAs.Type.*
import gl.ky.adeyaka.script.TokenStreamUtil.eatIfPresent
import gl.ky.adeyaka.script.TokenStreamUtil.expect
import gl.ky.adeyaka.script.TokenStreamUtil.isNext
import java.util.*

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

class Clause(val words: List<Word>) : ASTNode

sealed interface Word : ASTNode

/**
 * 动词，一个句中要执行的动作
 */
class Verb(val name: String) : Word

/**
 * 副词，为一个句中的动作提供参数
 */
class Adverb(val name: String, val value: Expr) : Word

sealed interface Expr : ASTNode {
    class Var(val name: String) : Expr
    class Bool private constructor(val value: Boolean) : Expr {
        companion object {
            @JvmStatic
            val TRUE = Bool(true)
            @JvmStatic
            val FALSE = Bool(false)
        }
    }
    class Num(val value: Double) : Expr
    class Str(val value: String) : Expr
    class Player(val name: String) : Expr
    class Pos(val x: Double, val y: Double, val z: Double) : Expr
}

// Parser

/**
 * 解析整个文件的 Parser
 */
class Parser(val input: TokenStream, val rules: List<Rule>) {
    fun get(): ScriptFile = parseFile()

    private fun parseFile(): ScriptFile {
        val groups = mutableListOf<ScriptGroup>()
        while(input.hasNext()) {
            groups += parseGroup()
        }
        return ScriptFile(groups)
    }

    private fun parseGroup(): ScriptGroup {
        input.expect(Token.Type.KW_GROUP)
        val name = input.expect(Token.Type.ID).value
        input.expect(Token.Type.LBRACE)
        val sens = mutableListOf<Sentence>()
        while(!input.isNext(Token.Type.RBRACE)) {
            sens += parseSentence()
        }
        input.expect(Token.Type.RBRACE)
        return ScriptGroup(name, sens)
    }

    private fun parseSentence(): Sentence {
        val clauses = mutableListOf<Clause>()
        do {
            clauses += parseClause()
        } while (input.isNext(Token.Type.COMMA))
        input.expect(Token.Type.EOS)
        return Sentence(clauses)
    }

    private fun parseClause(): Clause {
        for (rule in rules) {
            tryRule(rule)?.let { return it }
        }
        throw RuntimeException("No rule matched")
    }

    private fun tryRule(rule: Rule): Clause? {
        val result = mutableListOf<Word>()
        input.save()
        for (com in rule.components) {
            when(com) {
                is VerbMatch -> {
                    if(!input.eatIfPresent(com.verb)) { input.restore(); return null }
                    result += Verb(input.expect(Token.Type.ID).value)
                }
                is Match -> {
                    if(!input.eatIfPresent(com.s)) { input.restore(); return null }
                }
                is GetAs -> {
                    if(input.isNext(Token.Type.ID)) result += Adverb(com.name, Expr.Var(input.next().value))
                    when(com.type) {
                        BOOL -> {
                            if(input.isNext(Token.Type.BOOL_TRUE)) result += Adverb(com.name, Expr.Bool.TRUE)
                            else if(input.isNext(Token.Type.BOOL_FALSE)) result += Adverb(com.name, Expr.Bool.FALSE)
                            else { input.restore(); return null }
                        }
                        NUMBER -> {
                            if(input.isNext(Token.Type.NUMBER)) result += Adverb(com.name, Expr.Num(input.next().value.toDouble()))
                            else { input.restore(); return null }
                        }
                        STRING -> {
                            if(input.isNext(Token.Type.STRING)) result += Adverb(com.name, Expr.Str(input.next().value))
                            else { input.restore(); return null }
                        }
                    }
                }
            })
        }
        input.cancel()
        return Clause(result)
    }

}
