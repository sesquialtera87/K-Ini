package org.mth.kini;

import java.text.ParseException;

import static java.lang.Character.isWhitespace;
import static org.mth.kini.IniParserKt.readSample;

public class SimpleParser {

    static char DOUBLE_QUOTES = '"';
    static char SINGLE_QUOTE = '\'';
    static char WHITESPACE = ' ';
    static char EQUAL = '=';

    String input;
    int position = 0;

    char ch;

    Ini ini;
    IniSection currentSection;


    void consumeWhitespaces() {
        while (!EOF()) {
            ch = peek();

            if (ch == WHITESPACE || ch == '\t')
                consume();
            else break;
        }
    }

    char peek() {
        return input.charAt(position);
    }

    void consume() {
        position++;
    }

    boolean EOF() {
        return position >= input.length();
    }

    char readNext() {
        return input.charAt(position++);
    }

    boolean startComment() {
        if (!EOF()) {
            ch = peek();
            return (ch == ';' || ch == '#');
        } else
            return false;
    }

    boolean parse(String content) throws ParseException {
        input = content;
        position = 0;
        ini = new Ini();
        currentSection = ini.section(Ini.ROOT);

        while (!EOF()) {
            ch = peek();

            if (ch == '[') {
                consume();
                if (!section())
                    throw new ParseException("Section", 0);
            } else if (startComment()) {
                consume();
                comment();
            } else if (isWhitespace(ch)) {
                emptyLine();
            } else {
                assignment();
            }
        }

        return true;
    }

    boolean end(boolean consume) {
        if (EOF()) return true;

        if (peek() == '\n' || peek() == '\r') {
            if (consume)
                consume();
            return true;
        }

        return false;
    }

    boolean end() {
        if (EOF()) return true;

        return peek() == '\n' || peek() == '\r';
    }

    void emptyLine() {
        ch = peek();

        while (!end()) {
            if (!isWhitespace(ch))
                return;

            consume();
        }

        consume();
    }

    void comment() {
        StringBuilder b = new StringBuilder();

        while (!EOF()) {
            ch = peek();

            if (end()) {
                System.out.println("Comment -> " + b);
                consume();
                return;
            } else {
                b.append(ch);
                consume();
            }
        }

    }

    boolean section() {
        StringBuilder b = new StringBuilder();

        while (!EOF()) {
            ch = peek();

            if (ch == ']') {
                consume();
                consumeWhitespaces();

                if (end()) {
                    consume();
                    System.out.println("Section: " + b);
                    currentSection = ini.section(b.toString());
                    return true;
                } else return false;
            } else if (end()) {
                consume();
                return false;
            } else {
                b.append(ch);
                consume();
            }
        }

        return true;
    }

    String key() throws ParseException {
        StringBuilder b = new StringBuilder();

        while (!EOF()) {
            ch = peek();

            if (end()) {
                if (b.length() == 0) {
                    throw new RuntimeException("Strange...");
                } else throw new ParseException("Invalid assignment", 0);
            } else if (isWhitespace(ch)) {
                consumeWhitespaces();

                if (peek() != EQUAL)
                    throw new ParseException("Spaces not allowed in keys", position);
            } else if (ch != EQUAL) {
                b.append(ch);
                consume();
            } else {
                System.out.println("key [" + b + "]");
                return b.toString();
            }
        }

        return null;
    }

    boolean match(char ch) {
        if (peek() == ch) {
            consume();
            return true;
        } else return false;
    }

    String doubleQuoteEscape() throws ParseException {
        boolean stringClosed = false;
        StringBuilder b = new StringBuilder();

        if (!match(DOUBLE_QUOTES)) return null;

        while (!EOF()) {
            ch = peek();

            if (end() && !stringClosed) {
                throw new ParseException("Unclosed string value", 0);
            } else if (ch != DOUBLE_QUOTES) {
                b.append(ch);
                consume();
            } else if (match(DOUBLE_QUOTES)) {
                stringClosed = true;
                consumeWhitespaces();

                if (end(true)) {
                    System.out.println("Quoted Value [" + b + "]");
                    break;
                } else throw new ParseException("Invalid character after escaped string", 0);
            }
        }

        return b.toString();
    }

    String singleQuoteEscape() throws ParseException {
        boolean stringClosed = false;
        StringBuilder b = new StringBuilder();

        if (!match(SINGLE_QUOTE)) return null;

        while (!EOF()) {
            ch = peek();

            if (end() && !stringClosed) {
                throw new ParseException("Unclosed string value", 0);
            } else if (ch != SINGLE_QUOTE) {
                b.append(ch);
                consume();
            } else if (match(SINGLE_QUOTE)) {
                stringClosed = true;
                consumeWhitespaces();

                if (end(true)) {
                    System.out.println("Quoted Value [" + b + "]");
                    break;
                } else throw new ParseException("Invalid character after escaped string", 0);
            }
        }

        return b.toString();
    }

    String value() throws ParseException {
        StringBuilder b = new StringBuilder();

        while (!EOF()) {
            ch = peek();

            if (ch == DOUBLE_QUOTES) {
                b.append(doubleQuoteEscape());
                break;
            } else if (ch == SINGLE_QUOTE) {
                b.append(singleQuoteEscape());
                break;
            }

            if (ch == WHITESPACE) {
                consumeWhitespaces();

                if (startComment()) {
                    consume();
                    comment();
                } else if (end(true))
                    break;
                else
                    throw new ParseException("Whitespace in non escaped value", 0);
            } else if (!end()) {
                b.append(ch);
                consume();
            } else if (end(true)) {
                System.out.println("Value [" + b + "]");
                break;
            }
        }

        return b.toString();
    }

    void assignment() throws ParseException {
        String key = key();

        if (!match(EQUAL)) throw new RuntimeException("Strange...");

        consumeWhitespaces();

        String value = value();

        currentSection.set(key, value);
    }

    public static void main(String[] args) throws ParseException {
        String content = readSample("startsWithExample.ini");
        long millis = System.currentTimeMillis();
        SimpleParser parser = new SimpleParser();
        System.out.println(parser.parse(content));
        System.out.println(System.currentTimeMillis() - millis);

        System.out.println(parser.ini.section(Ini.ROOT));
    }
}
