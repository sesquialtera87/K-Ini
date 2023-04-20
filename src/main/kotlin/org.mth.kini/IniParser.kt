package org.mth.kini

import org.parboiled.Action
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.errors.ErrorUtils
import org.parboiled.parser.BaseParser
import org.parboiled.parser.Parboiled
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.ParseTreeUtils
import java.util.*

@BuildParseTree
open class IniParser : BaseParser<String>() {

    var ini = Ini()
    private var currentSection = ini

    open fun sectionName(): Rule = OneOrMore(
        NoneOf("[]"),
    )

    open fun section(): Rule = Sequence(
        Ch('['),
        sectionName(),
        Action<String> { ctx ->
            println("Section -> " + ctx?.match)
            ctx.valueStack.push(ctx.match)
            true
        },
        Ch(']'),

        Action<String> { ctx ->
            val name = ctx.valueStack.pop()
            currentSection = ini.section(name)
            true
        },

        whitespaces(),
    )

    open fun comment(): Rule = Sequence(
        whitespaces(),
        Ch(';'),
        ZeroOrMore(NoneOf("\n")),
        Action<String> { ctx ->
            println("Comment -> " + ctx?.match)
            true
        },
        lineBreak()
    )

    open fun lineBreak(): Rule = FirstOf(String("\n\r"), String("\n"), String("\r"))

    open fun id(): Rule = OneOrMore(NoneOf("[="))

    open fun whitespaces(): Rule = ZeroOrMore(Ch(' '))

    open fun value(): Rule =
//        Sequence(
        ZeroOrMore(NoneOf("\n"))
//        lineBreak()
//    )

    open fun assignment(): Rule = Sequence(
//        whitespaces(),
        id(),
        Action<String> { ctx ->
            ctx.valueStack.push(ctx.match)
            true
        },
        whitespaces(),
        Ch('='),
        whitespaces(),

        value(),
        Action<String> { ctx ->
            ctx.valueStack.push(ctx.match)
            true
        },

        Action<String> { ctx ->
            val value = ctx.valueStack.pop()
            val name = ctx.valueStack.pop()
            currentSection.addProperty(name.trim(), value)
            true
        },
        Action<String> { ctx ->
            println("\nStack -> " + ctx?.match)
            ctx.valueStack.forEach {
                println(it)
            }
            true
        },
    )

    open fun ini(): Rule =
        Sequence(
            OneOrMore(
                FirstOf(
                    comment(),
                    Sequence(section(), lineBreak()),
                    Sequence(assignment(), lineBreak())
                )
            ),
            EOI
        )
}

fun readSample(sampleName: String): String {
    val inputStream = IniParser::class.java.getResourceAsStream("samples/$sampleName")
    val builder = StringBuilder()
    val scanner = Scanner(inputStream)

    while (scanner.hasNext())
        builder.append(scanner.nextLine()).append("\n")

    return builder.toString()
}

fun main() {
    val sample = readSample("startsWithExample.ini")
    println(sample)
    val parser = Parboiled.createParser(IniParser::class.java)
    val r = ReportingParseRunner<Any>(parser.ini()).run(sample)

    if (r.parseErrors.isNotEmpty())
        println(ErrorUtils.printParseError(r.parseErrors[0]));
    else {
//        println(ParseTreeUtils.printNodeTree(r))
    }

    println(parser.ini.section("Sample").properties)


}