package gl.ky.adeyaka.script

import gl.ky.adeyaka.script.GetAs.Type.*
import java.util.*
import java.util.regex.Matcher

fun main() {
    val t = """
        特效组"adeyaka"{
            设置a为1；b为#b；c为{Kouyou};d为“d”
        }
        """.trimIndent()
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
class Parser(val input: String, val rules: List<Rule>) {
    fun get(): ScriptFile = parseFile()

    class ParseException(val pos: Int, val msg: String) : RuntimeException("At pos $pos : $msg")

    object Keywords {
        const val SCRIPT_GROUP = "脚本组"
    }

    private var offset = 0
    private var savepoint: Stack<Int> = Stack()

    private inline fun save() { savepoint.push(offset) }
    private inline fun recover() { offset = savepoint.pop() }
    private inline fun cancel() { savepoint.pop() }
    private inline fun hasMore() = offset < input.length
    private inline fun skipWhitespace() { while (input[offset].isWhitespace()) offset++ }
    private inline fun expect(str: String) {
        skipWhitespace()
        if (isNext(str)) skip(str.length)
            else throw ParseException(offset, "Expected $str")
        skipWhitespace()
    }
    private inline fun isNext(str: String): Boolean {
        skipWhitespace()
        return input.startsWith(str, offset).also { skipWhitespace() }
    }
    private inline fun eatIfPresent(str: String): Boolean {
        skipWhitespace()
        if (isNext(str)) {
            skip(str.length)
            skipWhitespace()
            return true
        }
        return false
    }
    private inline fun skip(i: Int) { offset += i }

    private fun parseFile(): ScriptFile {
        val groups = mutableListOf<ScriptGroup>()
        while(hasMore()) {
            groups.add(parseGroup())
        }
        return ScriptFile(groups)
    }

    private fun parseGroup(): ScriptGroup {
        expect("脚本组")
        val name = expectStr().value
        expect("{")
        val sens = mutableListOf<Sentence>()
        while(input[offset] != '}') {
            sens.add(parseSentence())
        }
        expect("}")
        return ScriptGroup(name, sens)
    }

    private fun parseSentence(): Sentence {
        val clauses = mutableListOf<Clause>()
        do {
            clauses.add(parseClause())
        } while (input[offset] == '，')
        return Sentence(clauses)
    }

    private fun parseClause(): Clause {
        for (rule in rules) {
            val result = tryRule(rule)
            if (result != null) return result
        }
        throw ParseException(offset, "No rule matched")
    }

    private fun tryRule(rule: Rule): Clause? {
        val words = mutableListOf<Word>()
        for (component in rule.components) {
            save()
            try {
                when(component) {
                    is VerbMatch -> {
                        expect(component.verb)
                        words.add(Verb(component.verb))
                    }
                    is Match -> {
                        expect(component.s)
                    }
                    is GetAs -> {
                        val value = when(component.type) {
                            ANY -> expectAny()
                            STRING -> expectStr()
                            NUMBER -> expectNum()
                            BOOL -> expectBool()
                            POSITION -> expectPos()
                            PLAYER -> expectPlayer()
                        }
                        words.add(Adverb(component.name, value))
                    }
                }
            } catch (e: ParseException) {
                recover()
                return null
            }
            cancel()
        }
        return Clause(words)
    }

    private fun expectAny(): Expr {
        TODO()
    }

    private fun expectStr(): Expr.Str {
        TODO()
    }
    private fun expectNum(): Expr.Num {
        assert(input[offset] == '-' || input[offset] in '0'..'9')

    }
    private fun expectBool(): Expr.Bool {
        return when {
            eatIfPresent("真") -> Expr.Bool.TRUE
            eatIfPresent("假") -> Expr.Bool.FALSE
            eatIfPresent("是") -> Expr.Bool.TRUE
            eatIfPresent("否") -> Expr.Bool.FALSE
            eatIfPresent("true") -> Expr.Bool.TRUE
            eatIfPresent("false") -> Expr.Bool.FALSE
            else -> throw ParseException(offset, "Expected bool")
        }
    }
    private fun expectPos(): Expr.Pos {
        expect("[")
        val x = expectNum().value
        expect(",")
        val y = expectNum().value
        expect(",")
        val z = expectNum().value
        expect("]")
        return Expr.Pos(x, y, z)
    }
    private fun expectPlayer(): Expr.Player {
        expect("{")
        val name = expectStr().value
        expect("}")
        return Expr.Player(name)
    }
    private fun expectVar(): Expr.Var {
        expect("#")
        val name = expectStr().value
        return Expr.Var(name)
    }
}
