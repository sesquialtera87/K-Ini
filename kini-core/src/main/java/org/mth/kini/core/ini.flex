package org.mth.kini;

import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

%%

%public
%class IniScanner
%unicode
%line
%int

%{
    public static final String DEFAULT_SECTION = "§§§§§";

    public Map<String, List<String[]>> ini = new HashMap<>();
    String currentSection = DEFAULT_SECTION;
    String[] property = new String[2];
    StringBuilder propertyValue;
    boolean quotedValue = false;

    List<String[]> section(String name) {
        if (!ini.containsKey(name)) {
            ini.put(name, new ArrayList<>());
        }

        return ini.get(name);
    }

    void newProperty(String name) {
        property = new String[2];
        property[0] = name.trim();
        property[1] = "";
        propertyValue = new StringBuilder();
    }

    void addProperty() {
        String value;

        if(quotedValue)
            value = propertyValue.toString().stripLeading();
        else
            value = propertyValue.toString().trim();

        property[1] = value;
        section(currentSection).add(property);
        quotedValue = false;
    }

    void malformed(char c) {
        if(propertyValue.charAt(0)!=c)
            propertyValue.insert(0, c);
    }
%}

Assign				= [=:]
Escaped             = "\\"[tnrf0;#'\\]
Whitespace			= ([ \t]+)
Comment				= ({Whitespace}*[#;])
Eol                 = \r|\n|\r\n

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
    {Escaped}               { propertyValue.append(yytext()); }
    {Eol}                   { malformed('\''); addProperty(); yybegin(YYINITIAL); }
    [']                     { quotedValue = true; addProperty(); yybegin(YYINITIAL); }
    <<EOF>>                 { malformed('\''); addProperty(); return 0; }
    [^]                     { propertyValue.append(zzBuffer[zzMarkedPos-1]); }
}

<STRING> {
    {Escaped}               { propertyValue.append(yytext()); }
    {Eol}                   { malformed('"'); addProperty(); yybegin(YYINITIAL); }
    [\"]                    { quotedValue = true; addProperty(); yybegin(YYINITIAL); }
    <<EOF>>                 { malformed('"'); addProperty(); return 0; }
    [^]                     { propertyValue.append(zzBuffer[zzMarkedPos-1]); }
}

<COMMENT> {
    {Eol}               { yybegin(YYINITIAL); }
    [^\r\n]+            { }
    <<EOF>>				{ return 0; }
}

<PROPERTY_NAME> {
    {Assign}            { yybegin(PROPERTY_VALUE); }
    [^=:]+              { newProperty(yytext()); }
    <<EOF>>				{ throw new MalformedInputException(yyline); }
}

<PROPERTY_VALUE> {
    {Whitespace}[\"]    { yybegin(STRING); }
    {Whitespace}[']     { yybegin(STRING_SINGLE); }
    {Eol}               { yybegin(YYINITIAL); addProperty(); }
    [#;]                { yybegin(COMMENT); addProperty(); }
    [^\r\n]             { propertyValue.append(zzBuffer[zzMarkedPos-1]); }
    <<EOF>>             { yybegin(YYINITIAL); addProperty(); }
}

<SECTION> {
    "]"                 { yybegin(YYINITIAL); }
    [^\]]+              { currentSection = String.valueOf(yytext()).trim(); }
	<<EOF>>				{ throw new MalformedInputException(yyline); }
}