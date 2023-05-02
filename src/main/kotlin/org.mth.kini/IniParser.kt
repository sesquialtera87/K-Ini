/**
 * MIT License
 *
 * Copyright (c) [2023] [Mattia Marelli]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.mth.kini

import org.parboiled.Action
import org.parboiled.Context
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.errors.ErrorUtils
import org.parboiled.parser.BaseActions
import org.parboiled.parser.BaseParser
import org.parboiled.parser.Parboiled
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.ValueStack
import org.parboiled.transform.BaseAction
import java.text.ParseException
import java.util.*

@BuildParseTree
open class IniParser(private val inlineComments: Boolean = true) : BaseParser<String>() {

    var ini = Ini()
    private var currentSection: IniSection = ini.section(Ini.ROOT)

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

    open fun doubleQuotedString(): Rule = Sequence(
        Ch('"'),
        ZeroOrMore(
            FirstOf(
                String("\\\""),
                NoneOf("\"\n")
            )
        ),
        Ch('"'),
    )

    open fun singleQuotedString(): Rule = Sequence(
        Ch('\''),
        ZeroOrMore(
            FirstOf(
                String("\\\""),
                NoneOf("'\n")
            )
        ),
        Ch('\''),
    )

    fun valueAction(type: String): Boolean {
        val ctx = context
        val match: String = ctx.toString()
        val valueStack: ValueStack<String> = ctx.valueStack as ValueStack<String>
        System.out.println()

        if (type == "quoted")
            valueStack.push(match.substring(1, match.length - 1))
        else
            valueStack.push(match)

        System.err.println(valueStack.peek())
        return true
    }

    open fun value(): Rule =
        FirstOf(
            doubleQuotedString(),
            ACTION(valueAction("quoted")),
            singleQuotedString(),
            ACTION(valueAction("quoted")),
            ZeroOrMore(NoneOf(";#\n")),
            ACTION(valueAction("default"))
        )

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
        // already pushed in value rule
        Action { ctx ->
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
    val sample = readSample("sampleWithQuotes.ini")
    var millis = System.currentTimeMillis()
    val ini1 = IniParser.parse(sample)
    println(ini1.section(Ini.ROOT))
    millis = System.currentTimeMillis() - millis
    println(millis)


//    ini1.store(Path.of("C:\\Users\\matti\\OneDrive\\Desktop\\test.ini"))

}