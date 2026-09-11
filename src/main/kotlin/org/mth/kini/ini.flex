package org.mth.kini;

import java.util.InputMismatchException;

%%

%public
%class IniScanner
%unicode
%line
%int
//%standalone
//%debug

%{
    Ini ini = new Ini();
    IniSection currentSection = ini.section(Ini.ROOT);
    String[] property = new String[2];
    StringBuilder propertyValue;
    boolean quotedValue = false;

    private void newProperty(CharSequence name) {
        property[0] = name.toString().trim();
        property[1] = "";
        propertyValue = new StringBuilder();
    }

    void addProperty() {
        String value;

        if(quotedValue) {
            value = propertyValue.toString().stripLeading();
        } else {
            value = propertyValue.toString().trim();
        }

        currentSection.set(property[0], value);
        quotedValue = false;
    }

    private char unescape(char c) {
        switch (c) {
            case 't': return '\t';
            case 'n': return '\n';
            case 'r': return '\r';
            case 'f': return '\f';
            case '0': return '\u0000';
            default: return c; // ; # ' " \\  restano se stessi
        }
    }

    void malformed(char c) {
        if (propertyValue.charAt(0) != c){
            propertyValue.insert(0, c);
        }
    }
%}

Assign				= [=:]
Escaped             = "\\"[tnrf0;#\"'\\]
Whitespace			= ([ \t]+)
Comment				= ({Whitespace}*[#;])
Eol                 = \r|\n|\r\n

%state VALUE
%state SECTION
%state PROPERTY_NAME
%state PROPERTY_VALUE
%state COMMENT
%state STRING
%state STRING_SINGLE

%%

<YYINITIAL> {
    {Comment}           { yybegin(COMMENT); }
    {Eol}               { }
    "["                 { yybegin(SECTION); }
    [^\[]               { yypushback(1); yybegin(PROPERTY_NAME); }
	<<EOF>>				{ return 0; }
}

<STRING_SINGLE> {
    {Escaped}               { propertyValue.append(unescape(yycharat(1))); }
    {Eol}                   { malformed('\''); addProperty(); yybegin(YYINITIAL); }
    [']                     { quotedValue = true; addProperty(); yybegin(YYINITIAL); }
    <<EOF>>                 { malformed('\''); addProperty(); return 0; }
    [^]                     { propertyValue.append(zzBuffer.charAt(zzMarkedPos-1)); }
}

<STRING> {
    {Escaped}               { propertyValue.append(unescape(yycharat(1))); }
    {Eol}                   { malformed('"'); addProperty(); yybegin(YYINITIAL); }
    [\"]                    { quotedValue = true; addProperty(); yybegin(YYINITIAL); }
    <<EOF>>                 { malformed('"'); addProperty(); return 0; }
    [^]                     { propertyValue.append(zzBuffer.charAt(zzMarkedPos-1)); }
}

<COMMENT> {
    {Eol}               { yybegin(YYINITIAL); }
    [^\r\n]+            { }
    <<EOF>>				{ return 0; }
}

<PROPERTY_NAME> {
    {Assign}            { yybegin(PROPERTY_VALUE); }
    [^=:]+              { newProperty(yytext()); }
    <<EOF>>				{ throw new InputMismatchException("Expecting property value [line: " + yyline + "]"); }
}

<PROPERTY_VALUE> {
    {Whitespace}?[\"]   { yybegin(STRING); }
    {Whitespace}?[']    { yybegin(STRING_SINGLE); }
    {Eol}               { yybegin(YYINITIAL); addProperty(); }
    [#;]                { yybegin(COMMENT); addProperty(); }
    [^\r\n]             { propertyValue.append(zzBuffer.charAt(zzMarkedPos-1)); }
    <<EOF>>             { yybegin(YYINITIAL); addProperty(); }
}

<SECTION> {
    "]"                 { yybegin(YYINITIAL); }
    [^\]]+              { currentSection = ini.section(yytext().toString().trim()); }
	<<EOF>>				{ throw new InputMismatchException("Malformed input [line: " + yyline + "]"); }
}