package gl.ky.adeyaka.core

interface CaughtValueActionParser : ActionParser {
    /**
     * try to use these values to make an action
     * @return the action if successful, null otherwise, if failed, tokens won't be really consumed.
     */
    fun parse(input: List<*>): Action?
}

/**
 * Rule example: 游戏模式切换为 @mode=string
 * generate:
 * LexRule: '游戏模式切换为' -> WORD
 * ActionParser: { match('游戏模式切换为'); result.add(parseString()); return cvap.parse(result) }
 */
class Rule(val text: String, val extraHandler: CaughtValueActionParser? = null) {}

class RuleEngine {
    val extraLexRules = arrayListOf<LexRule>()
    val extraLp = arrayListOf<LiteralParser<*>>()

    fun addExtraLexRule(rule: LexRule) : RuleEngine { return this }
    fun addRule(rule: Rule) : RuleEngine { return this }

    fun compile() : RuleEngine { return this }
    lateinit var lexer: Lexer
    lateinit var parser: Parser
}