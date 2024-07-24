/*
 * MIT License
 *
 * Copyright (c) 2023 Mattia Marelli
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

package org.mth.kini;


@SuppressWarnings("fallthrough")
public class IniScanner {

    /**
     * This character denotes the end of file.
     */
    public static final int YYEOF = -1;

    /**
     * Initial size of the lookahead buffer.
     */
    private static final int ZZ_BUFFERSIZE = 16384;

    // Lexical states.
    public static final int YYINITIAL = 0;
    public static final int VALUE = 2;
    public static final int SECTION = 4;
    public static final int PROPERTY_NAME = 6;
    public static final int PROPERTY_VALUE = 8;
    public static final int COMMENT = 10;
    public static final int STRING = 12;
    public static final int STRING_SINGLE = 14;

    /**
     * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
     * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
     * at the beginning of a line
     * l is of the form l = 2*k, k a non negative integer
     */
    private static final int ZZ_LEXSTATE[] = {
            0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7
    };

    /**
     * Top-level table for translating characters to character classes
     */
    private static final int[] ZZ_CMAP_TOP = zzUnpackcmap_top();

    private static final String ZZ_CMAP_TOP_PACKED_0 =
            "\1\0\u10ff\u0100";

    private static int[] zzUnpackcmap_top() {
        int[] result = new int[4352];
        int offset = 0;
        offset = zzUnpackcmap_top(ZZ_CMAP_TOP_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackcmap_top(String packed, int offset, int[] result) {
        int i = 0;       /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            do result[j++] = value; while (--count > 0);
        }
        return j;
    }


    /**
     * Second-level tables for translating characters to character classes
     */
    private static final int[] ZZ_CMAP_BLOCKS = zzUnpackcmap_blocks();

    private static final String ZZ_CMAP_BLOCKS_PACKED_0 =
            "\11\0\1\1\1\2\2\0\1\3\22\0\1\1\1\0" +
                    "\1\4\1\5\3\0\1\6\10\0\1\7\11\0\1\10" +
                    "\1\5\1\0\1\10\35\0\1\11\1\12\1\13\10\0" +
                    "\1\7\7\0\1\7\3\0\1\7\1\0\1\7\u018b\0";

    private static int[] zzUnpackcmap_blocks() {
        int[] result = new int[512];
        int offset = 0;
        offset = zzUnpackcmap_blocks(ZZ_CMAP_BLOCKS_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackcmap_blocks(String packed, int offset, int[] result) {
        int i = 0;       /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            do result[j++] = value; while (--count > 0);
        }
        return j;
    }

    /**
     * Translates DFA states to action switch labels.
     */
    private static final int[] ZZ_ACTION = zzUnpackAction();

    private static final String ZZ_ACTION_PACKED_0 =
            "\10\0\2\1\2\2\1\3\1\4\1\5\1\6\1\7" +
                    "\1\10\2\11\2\12\1\13\1\2\1\6\2\14\1\15" +
                    "\1\11\2\16\2\0\1\17\1\20\1\21";

    private static int[] zzUnpackAction() {
        int[] result = new int[36];
        int offset = 0;
        offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackAction(String packed, int offset, int[] result) {
        int i = 0;       /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            do result[j++] = value; while (--count > 0);
        }
        return j;
    }


    /**
     * Translates a state to a row index in the transition table
     */
    private static final int[] ZZ_ROWMAP = zzUnpackRowMap();

    private static final String ZZ_ROWMAP_PACKED_0 =
            "\0\0\0\14\0\30\0\44\0\60\0\74\0\110\0\124" +
                    "\0\14\0\140\0\14\0\154\0\14\0\14\0\170\0\14" +
                    "\0\204\0\14\0\14\0\220\0\14\0\234\0\14\0\250" +
                    "\0\264\0\14\0\300\0\14\0\314\0\14\0\330\0\140" +
                    "\0\220\0\14\0\14\0\14";

    private static int[] zzUnpackRowMap() {
        int[] result = new int[36];
        int offset = 0;
        offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackRowMap(String packed, int offset, int[] result) {
        int i = 0;  /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length() - 1;
        while (i < l) {
            int high = packed.charAt(i++) << 16;
            result[j++] = high | packed.charAt(i++);
        }
        return j;
    }

    /**
     * The transition table of the DFA
     */
    private static final int[] ZZ_TRANS = zzUnpacktrans();

    private static final String ZZ_TRANS_PACKED_0 =
            "\1\11\1\12\1\13\1\14\1\11\1\15\3\11\1\16" +
                    "\2\11\14\0\13\17\1\20\10\21\1\22\3\21\1\23" +
                    "\1\24\1\25\1\26\1\23\1\27\6\23\2\30\1\20" +
                    "\1\31\10\30\2\23\1\32\1\33\1\34\5\23\1\35" +
                    "\3\23\1\36\1\37\2\23\1\34\3\23\1\35\1\23" +
                    "\1\0\1\40\3\0\1\15\10\0\1\13\11\0\13\17" +
                    "\1\0\10\21\1\0\3\21\1\0\1\41\2\0\1\42" +
                    "\1\0\1\43\7\0\1\25\11\0\2\30\2\0\10\30" +
                    "\2\0\1\20\13\0\1\32\16\0\3\44\2\0\1\44" +
                    "\3\0\1\36\11\0";

    private static int[] zzUnpacktrans() {
        int[] result = new int[228];
        int offset = 0;
        offset = zzUnpacktrans(ZZ_TRANS_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpacktrans(String packed, int offset, int[] result) {
        int i = 0;       /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            value--;
            do result[j++] = value; while (--count > 0);
        }
        return j;
    }


    /**
     * Error code for "Unknown internal scanner error".
     */
    private static final int ZZ_UNKNOWN_ERROR = 0;
    /**
     * Error code for "could not match input".
     */
    private static final int ZZ_NO_MATCH = 1;
    /**
     * Error code for "pushback value was too large".
     */
    private static final int ZZ_PUSHBACK_2BIG = 2;

    /**
     * Error messages for {@link #ZZ_UNKNOWN_ERROR}, {@link #ZZ_NO_MATCH}, and
     * {@link #ZZ_PUSHBACK_2BIG} respectively.
     */
    private static final String ZZ_ERROR_MSG[] = {
            "Unknown internal scanner error",
            "Error: could not match input",
            "Error: pushback value was too large"
    };

    /**
     * ZZ_ATTRIBUTE[aState] contains the attributes of state {@code aState}
     */
    private static final int[] ZZ_ATTRIBUTE = zzUnpackAttribute();

    private static final String ZZ_ATTRIBUTE_PACKED_0 =
            "\1\0\1\10\6\0\1\11\1\1\1\11\1\1\2\11" +
                    "\1\1\1\11\1\1\2\11\1\1\1\11\1\1\1\11" +
                    "\2\1\1\11\1\1\1\11\1\1\1\11\1\1\2\0" +
                    "\3\11";

    private static int[] zzUnpackAttribute() {
        int[] result = new int[36];
        int offset = 0;
        offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
        return result;
    }

    private static int zzUnpackAttribute(String packed, int offset, int[] result) {
        int i = 0;       /* index in packed string  */
        int j = offset;  /* index in unpacked array */
        int l = packed.length();
        while (i < l) {
            int count = packed.charAt(i++);
            int value = packed.charAt(i++);
            do result[j++] = value; while (--count > 0);
        }
        return j;
    }

    /**
     * Input device.
     */
    private java.io.Reader zzReader;

    /**
     * Current state of the DFA.
     */
    private int zzState;

    /**
     * Current lexical state.
     */
    private int zzLexicalState = YYINITIAL;

    /**
     * This buffer contains the current text to be matched and is the source of the {@link #yytext()}
     * string.
     */
    private char zzBuffer[] = new char[Math.min(ZZ_BUFFERSIZE, zzMaxBufferLen())];

    /**
     * Text position at the last accepting state.
     */
    private int zzMarkedPos;

    /**
     * Current text position in the buffer.
     */
    private int zzCurrentPos;

    /**
     * Marks the beginning of the {@link #yytext()} string in the buffer.
     */
    private int zzStartRead;

    /**
     * Marks the last character in the buffer, that has been read from input.
     */
    private int zzEndRead;

    /**
     * Whether the scanner is at the end of file.
     *
     * @see #yyatEOF
     */
    private boolean zzAtEOF;

    /**
     * The number of occupied positions in {@link #zzBuffer} beyond {@link #zzEndRead}.
     *
     * <p>When a lead/high surrogate has been read from the input stream into the final
     * {@link #zzBuffer} position, this will have a value of 1; otherwise, it will have a value of 0.
     */
    private int zzFinalHighSurrogate = 0;

    /**
     * Number of newlines encountered up to the start of the matched text.
     */
    private int yyline;

    /**
     * Number of characters from the last newline up to the start of the matched text.
     */
    @SuppressWarnings("unused")
    private int yycolumn;

    /**
     * Number of characters up to the start of the matched text.
     */
    @SuppressWarnings("unused")
    private long yychar;

    /**
     * Whether the scanner is currently at the beginning of a line.
     */
    @SuppressWarnings("unused")
    private boolean zzAtBOL = true;

    /**
     * Whether the user-EOF-code has already been executed.
     */
    @SuppressWarnings("unused")
    private boolean zzEOFDone;

    /* user code: */
    Ini ini = new Ini();
    IniSection currentSection = ini.section(Ini.ROOT);
    String[] property = new String[2];
    StringBuilder propertyValue;
    boolean quotedValue = false;

    void newProperty(String name) {
        property[0] = name.trim();
        property[1] = "";
        propertyValue = new StringBuilder();
    }

    void addProperty() {
        String value;

        if (quotedValue)
            value = propertyValue.toString().stripLeading();
        else
            value = propertyValue.toString().trim();

        currentSection.set(property[0], value);
        quotedValue = false;
    }

    void malformed(char c) {
        if (propertyValue.charAt(0) != c)
            propertyValue.insert(0, c);
    }


    /**
     * Creates a new scanner
     *
     * @param in the java.io.Reader to read input from.
     */
    public IniScanner(java.io.Reader in) {
        this.zzReader = in;
    }


    /**
     * Returns the maximum size of the scanner buffer, which limits the size of tokens.
     */
    private int zzMaxBufferLen() {
        return Integer.MAX_VALUE;
    }

    /**
     * Whether the scanner buffer can grow to accommodate a larger token.
     */
    private boolean zzCanGrow() {
        return true;
    }

    /**
     * Translates raw input code points to DFA table row
     */
    private static int zzCMap(int input) {
        int offset = input & 255;
        return offset == input ? ZZ_CMAP_BLOCKS[offset] : ZZ_CMAP_BLOCKS[ZZ_CMAP_TOP[input >> 8] | offset];
    }

    /**
     * Refills the input buffer.
     *
     * @return {@code false} iff there was new input.
     * @throws java.io.IOException if any I/O-Error occurs
     */
    private boolean zzRefill() throws java.io.IOException {

        /* first: make room (if you can) */
        if (zzStartRead > 0) {
            zzEndRead += zzFinalHighSurrogate;
            zzFinalHighSurrogate = 0;
            System.arraycopy(zzBuffer, zzStartRead,
                    zzBuffer, 0,
                    zzEndRead - zzStartRead);

            /* translate stored positions */
            zzEndRead -= zzStartRead;
            zzCurrentPos -= zzStartRead;
            zzMarkedPos -= zzStartRead;
            zzStartRead = 0;
        }

        /* is the buffer big enough? */
        if (zzCurrentPos >= zzBuffer.length - zzFinalHighSurrogate && zzCanGrow()) {
            /* if not, and it can grow: blow it up */
            char newBuffer[] = new char[Math.min(zzBuffer.length * 2, zzMaxBufferLen())];
            System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
            zzBuffer = newBuffer;
            zzEndRead += zzFinalHighSurrogate;
            zzFinalHighSurrogate = 0;
        }

        /* fill the buffer with new input */
        int requested = zzBuffer.length - zzEndRead;
        int numRead = zzReader.read(zzBuffer, zzEndRead, requested);

        /* not supposed to occur according to specification of java.io.Reader */
        if (numRead == 0) {
            if (requested == 0) {
                throw new java.io.EOFException("Scan buffer limit reached [" + zzBuffer.length + "]");
            } else {
                throw new java.io.IOException(
                        "Reader returned 0 characters. See JFlex examples/zero-reader for a workaround.");
            }
        }
        if (numRead > 0) {
            zzEndRead += numRead;
            if (Character.isHighSurrogate(zzBuffer[zzEndRead - 1])) {
                if (numRead == requested) { // We requested too few chars to encode a full Unicode character
                    --zzEndRead;
                    zzFinalHighSurrogate = 1;
                } else {                    // There is room in the buffer for at least one more char
                    int c = zzReader.read();  // Expecting to read a paired low surrogate char
                    if (c == -1) {
                        return true;
                    } else {
                        zzBuffer[zzEndRead++] = (char) c;
                    }
                }
            }
            /* potentially more input available */
            return false;
        }

        /* numRead < 0 ==> end of stream */
        return true;
    }


    /**
     * Closes the input reader.
     *
     * @throws java.io.IOException if the reader could not be closed.
     */
    public final void yyclose() throws java.io.IOException {
        zzAtEOF = true; // indicate end of file
        zzEndRead = zzStartRead; // invalidate buffer

        if (zzReader != null) {
            zzReader.close();
        }
    }


    /**
     * Resets the scanner to read from a new input stream.
     *
     * <p>Does not close the old reader.
     *
     * <p>All internal variables are reset, the old input stream <b>cannot</b> be reused (internal
     * buffer is discarded and lost). Lexical state is set to {@code ZZ_INITIAL}.
     *
     * <p>Internal scan buffer is resized down to its initial length, if it has grown.
     *
     * @param reader The new input stream.
     */
    public final void yyreset(java.io.Reader reader) {
        zzReader = reader;
        zzEOFDone = false;
        yyResetPosition();
        zzLexicalState = YYINITIAL;
        int initBufferSize = Math.min(ZZ_BUFFERSIZE, zzMaxBufferLen());
        if (zzBuffer.length > initBufferSize) {
            zzBuffer = new char[initBufferSize];
        }
    }

    /**
     * Resets the input position.
     */
    private final void yyResetPosition() {
        zzAtBOL = true;
        zzAtEOF = false;
        zzCurrentPos = 0;
        zzMarkedPos = 0;
        zzStartRead = 0;
        zzEndRead = 0;
        zzFinalHighSurrogate = 0;
        yyline = 0;
        yycolumn = 0;
        yychar = 0L;
    }


    /**
     * Returns whether the scanner has reached the end of the reader it reads from.
     *
     * @return whether the scanner has reached EOF.
     */
    public final boolean yyatEOF() {
        return zzAtEOF;
    }


    /**
     * Returns the current lexical state.
     *
     * @return the current lexical state.
     */
    public final int yystate() {
        return zzLexicalState;
    }


    /**
     * Enters a new lexical state.
     *
     * @param newState the new lexical state
     */
    public final void yybegin(int newState) {
        zzLexicalState = newState;
    }


    /**
     * Returns the text matched by the current regular expression.
     *
     * @return the matched text.
     */
    public final String yytext() {
        return new String(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
    }


    /**
     * Returns the character at the given position from the matched text.
     *
     * <p>It is equivalent to {@code yytext().charAt(pos)}, but faster.
     *
     * @param position the position of the character to fetch. A value from 0 to {@code yylength()-1}.
     * @return the character at {@code position}.
     */
    public final char yycharat(int position) {
        return zzBuffer[zzStartRead + position];
    }


    /**
     * How many characters were matched.
     *
     * @return the length of the matched text region.
     */
    public final int yylength() {
        return zzMarkedPos - zzStartRead;
    }


    /**
     * Reports an error that occurred while scanning.
     *
     * <p>In a well-formed scanner (no or only correct usage of {@code yypushback(int)} and a
     * match-all fallback rule) this method will only be called with things that
     * "Can't Possibly Happen".
     *
     * <p>If this method is called, something is seriously wrong (e.g. a JFlex bug producing a faulty
     * scanner etc.).
     *
     * <p>Usual syntax/scanner level error handling should be done in error fallback rules.
     *
     * @param errorCode the code of the error message to display.
     */
    private static void zzScanError(int errorCode) {
        String message;
        try {
            message = ZZ_ERROR_MSG[errorCode];
        } catch (ArrayIndexOutOfBoundsException e) {
            message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
        }

        throw new Error(message);
    }


    /**
     * Pushes the specified amount of characters back into the input stream.
     *
     * <p>They will be read again by then next call of the scanning method.
     *
     * @param number the number of characters to be read again. This number must not be greater than
     *               {@link #yylength()}.
     */
    public void yypushback(int number) {
        if (number > yylength())
            zzScanError(ZZ_PUSHBACK_2BIG);

        zzMarkedPos -= number;
    }


    /**
     * Resumes scanning until the next regular expression is matched, the end of input is encountered
     * or an I/O-Error occurs.
     *
     * @return the next token.
     * @throws java.io.IOException if any I/O-Error occurs.
     */
    public int yylex() throws java.io.IOException {
        int zzInput;
        int zzAction;

        // cached fields:
        int zzCurrentPosL;
        int zzMarkedPosL;
        int zzEndReadL = zzEndRead;
        char[] zzBufferL = zzBuffer;

        int[] zzTransL = ZZ_TRANS;
        int[] zzRowMapL = ZZ_ROWMAP;
        int[] zzAttrL = ZZ_ATTRIBUTE;

        while (true) {
            zzMarkedPosL = zzMarkedPos;

            boolean zzR = false;
            int zzCh;
            int zzCharCount;
            for (zzCurrentPosL = zzStartRead;
                 zzCurrentPosL < zzMarkedPosL;
                 zzCurrentPosL += zzCharCount) {
                zzCh = Character.codePointAt(zzBufferL, zzCurrentPosL, zzMarkedPosL);
                zzCharCount = Character.charCount(zzCh);
                switch (zzCh) {
                    case '\u000B':  // fall through
                    case '\u000C':  // fall through
                    case '\u0085':  // fall through
                    case '\u2028':  // fall through
                    case '\u2029':
                        yyline++;
                        zzR = false;
                        break;
                    case '\r':
                        yyline++;
                        zzR = true;
                        break;
                    case '\n':
                        if (zzR)
                            zzR = false;
                        else {
                            yyline++;
                        }
                        break;
                    default:
                        zzR = false;
                }
            }

            if (zzR) {
                // peek one character ahead if it is
                // (if we have counted one line too much)
                boolean zzPeek;
                if (zzMarkedPosL < zzEndReadL)
                    zzPeek = zzBufferL[zzMarkedPosL] == '\n';
                else if (zzAtEOF)
                    zzPeek = false;
                else {
                    boolean eof = zzRefill();
                    zzEndReadL = zzEndRead;
                    zzMarkedPosL = zzMarkedPos;
                    zzBufferL = zzBuffer;
                    if (eof)
                        zzPeek = false;
                    else
                        zzPeek = zzBufferL[zzMarkedPosL] == '\n';
                }
                if (zzPeek) yyline--;
            }
            zzAction = -1;

            zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

            zzState = ZZ_LEXSTATE[zzLexicalState];

            // set up zzAction for empty match case:
            int zzAttributes = zzAttrL[zzState];
            if ((zzAttributes & 1) == 1) {
                zzAction = zzState;
            }


            zzForAction:
            {
                while (true) {

                    if (zzCurrentPosL < zzEndReadL) {
                        zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
                        zzCurrentPosL += Character.charCount(zzInput);
                    } else if (zzAtEOF) {
                        zzInput = YYEOF;
                        break zzForAction;
                    } else {
                        // store back cached positions
                        zzCurrentPos = zzCurrentPosL;
                        zzMarkedPos = zzMarkedPosL;
                        boolean eof = zzRefill();
                        // get translated positions and possibly new buffer
                        zzCurrentPosL = zzCurrentPos;
                        zzMarkedPosL = zzMarkedPos;
                        zzBufferL = zzBuffer;
                        zzEndReadL = zzEndRead;
                        if (eof) {
                            zzInput = YYEOF;
                            break zzForAction;
                        } else {
                            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
                            zzCurrentPosL += Character.charCount(zzInput);
                        }
                    }
                    int zzNext = zzTransL[zzRowMapL[zzState] + zzCMap(zzInput)];
                    if (zzNext == -1) break zzForAction;
                    zzState = zzNext;

                    zzAttributes = zzAttrL[zzState];
                    if ((zzAttributes & 1) == 1) {
                        zzAction = zzState;
                        zzMarkedPosL = zzCurrentPosL;
                        if ((zzAttributes & 8) == 8) break zzForAction;
                    }

                }
            }

            // store back cached position
            zzMarkedPos = zzMarkedPosL;

            if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
                zzAtEOF = true;
                switch (zzLexicalState) {
                    case YYINITIAL: {
                        return 0;
                    }  // fall though
                    case 37:
                        break;
                    case SECTION: {
                        throw new IniParseException("Malformed input", yyline);
                    }  // fall though
                    case 38:
                        break;
                    case PROPERTY_NAME: {
                        throw new IniParseException("Expecting property value", yyline);
                    }  // fall though
                    case 39:
                        break;
                    case PROPERTY_VALUE: {
                        yybegin(YYINITIAL);
                        addProperty();
                    }  // fall though
                    case 40:
                        break;
                    case COMMENT: {
                        return 0;
                    }  // fall though
                    case 41:
                        break;
                    case STRING: {
                        malformed('"');
                        addProperty();
                        return 0;
                    }  // fall though
                    case 42:
                        break;
                    case STRING_SINGLE: {
                        malformed('\'');
                        addProperty();
                        return 0;
                    }  // fall though
                    case 43:
                        break;
                    default:
                        return YYEOF;
                }
            } else {
                switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
                    case 1: {
                        yypushback(1);
                        yybegin(PROPERTY_NAME);
                    }
                    // fall through
                    case 18:
                        break;
                    case 2: {
                    }
                    // fall through
                    case 19:
                        break;
                    case 3: {
                        yybegin(COMMENT);
                    }
                    // fall through
                    case 20:
                        break;
                    case 4: {
                        yybegin(SECTION);
                    }
                    // fall through
                    case 21:
                        break;
                    case 5: {
                        currentSection = ini.section(yytext());
                    }
                    // fall through
                    case 22:
                        break;
                    case 6: {
                        yybegin(YYINITIAL);
                    }
                    // fall through
                    case 23:
                        break;
                    case 7: {
                        newProperty(yytext());
                    }
                    // fall through
                    case 24:
                        break;
                    case 8: {
                        yybegin(PROPERTY_VALUE);
                    }
                    // fall through
                    case 25:
                        break;
                    case 9: {
                        propertyValue.append(zzBuffer[zzMarkedPos - 1]);
                    }
                    // fall through
                    case 26:
                        break;
                    case 10: {
                        yybegin(YYINITIAL);
                        addProperty();
                    }
                    // fall through
                    case 27:
                        break;
                    case 11: {
                        yybegin(COMMENT);
                        addProperty();
                    }
                    // fall through
                    case 28:
                        break;
                    case 12: {
                        malformed('"');
                        addProperty();
                        yybegin(YYINITIAL);
                    }
                    // fall through
                    case 29:
                        break;
                    case 13: {
                        quotedValue = true;
                        addProperty();
                        yybegin(YYINITIAL);
                    }
                    // fall through
                    case 30:
                        break;
                    case 14: {
                        malformed('\'');
                        addProperty();
                        yybegin(YYINITIAL);
                    }
                    // fall through
                    case 31:
                        break;
                    case 15: {
                        yybegin(STRING);
                    }
                    // fall through
                    case 32:
                        break;
                    case 16: {
                        yybegin(STRING_SINGLE);
                    }
                    // fall through
                    case 33:
                        break;
                    case 17: {
                        propertyValue.append(yytext());
                    }
                    // fall through
                    case 34:
                        break;
                    default:
                        zzScanError(ZZ_NO_MATCH);
                }
            }
        }
    }
}
