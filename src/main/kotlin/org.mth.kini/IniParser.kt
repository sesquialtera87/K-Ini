package org.mth.kini

import org.parboiled.Action
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.errors.ErrorUtils
import org.parboiled.parser.BaseParser
import org.parboiled.parser.Parboiled
import org.parboiled.parserunners.ReportingParseRunner
import java.text.ParseException
import java.util.*

@BuildParseTree
open class IniParser(private val inlineComments: Boolean = true) : BaseParser<String>() {

    var ini = Ini()
    private var currentSection = ini

    open fun sectionName(): Rule = OneOrMore(
        NoneOf("[]"),
    )

    open fun section(): Rule = Sequence(
        Ch('['),
        sectionName(),
        Action { ctx ->
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

        if (inlineComments) {
            // inline comment next to a section declaration
            Optional(comment())
        } else
            whitespaces()
    )

    open fun comment(): Rule = Sequence(
        whitespaces(),
        commentPrefix(),
        ZeroOrMore(NoneOf("\n")),
//        lineBreak()
    )

    open fun commentPrefix(): Rule = AnyOf(";#")

    open fun lineBreak(): Rule = OneOrMore(
        FirstOf(
            String("\n\r"), String("\n"), String("\r")
        )
    )

    open fun id(): Rule = OneOrMore(NoneOf("[="))

    open fun whitespaces(): Rule = ZeroOrMore(Ch(' '))

    open fun value(): Rule =
//        Sequence(
        ZeroOrMore(NoneOf(";#\n"))
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
//        whitespaces(),

        value(),
        Action<String> { ctx ->
            ctx.valueStack.push(ctx.match)
            true
        },

        Action<String> { ctx ->
            val value = ctx.valueStack.pop()
            val name = ctx.valueStack.pop()
            currentSection[name.trim()] = value
            true
        },

        if (inlineComments)
            Optional(comment())
        else
            whitespaces()
    )

    open fun ini(): Rule =
        Sequence(
            OneOrMore(
                FirstOf(
                    Sequence(comment(), lineBreak()),
                    Sequence(section(), lineBreak()),
                    Sequence(assignment(), lineBreak())
                )
            ),
            EOI
        )

    companion object {
        fun parse(fileContent: String): Ini {
            val parser = Parboiled.createParser(IniParser::class.java)
            val r = ReportingParseRunner<Any>(parser.ini()).run(fileContent)

            if (r.hasErrors()) {
                println(ErrorUtils.printParseError(r.parseErrors[0]))
                throw ParseException("Malformed input file", r.parseErrors[0].startIndex)
            }

            return parser.ini
        }
    }
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
//    val sample = readSample("startsWithExample.ini")
    val sample = readSample("sample.ini")
    var millis = System.currentTimeMillis()
    println(IniParser.parse(sample))
    millis = System.currentTimeMillis() - millis
    println(millis)

    millis = System.currentTimeMillis()
    val inputStream = IniParser::class.java.getResourceAsStream("samples/sample.ini")
    val ini = com.github.vincentrussell.ini.Ini()
    ini.load(inputStream)
    millis = System.currentTimeMillis() - millis
    println(millis)
}