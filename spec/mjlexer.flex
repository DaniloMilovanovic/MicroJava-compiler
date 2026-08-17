
package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}

%}

%cup
%line
%column

%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

" " 	{ }
"\b" 	{ }
"\t" 	{ }
"\r\n" 	{ }
"\f" 	{ }

"program"   { return new_symbol(sym.PROG, yytext()); }
"break"     { return new_symbol(sym.BREAK, yytext()); }
"else"      { return new_symbol(sym.ELSE, yytext()); }
"const"     { return new_symbol(sym.CONST, yytext()); }
"if"        { return new_symbol(sym.IF, yytext()); }
"new"       { return new_symbol(sym.NEW, yytext()); }
"print" 	{ return new_symbol(sym.PRINT, yytext()); }
"read"      { return new_symbol(sym.READ, yytext()); }
"return" 	{ return new_symbol(sym.RETURN, yytext()); }
"void" 		{ return new_symbol(sym.VOID, yytext()); }
"continue"  { return new_symbol(sym.CONTINUE, yytext()); }
"for"       { return new_symbol(sym.FOR, yytext()); }
"length"    { return new_symbol(sym.LEN, yytext()); }
"findAny"   { return new_symbol(sym.FINDANY, yytext()); }
"map"       { return new_symbol(sym.MAP, yytext()); }

"true"      { return new_symbol(sym.TRUE, yytext()); }
"false"      { return new_symbol(sym.FALSE, yytext()); }

"+" 		{ return new_symbol(sym.PLUS, yytext()); }
"-" 		{ return new_symbol(sym.MINUS, yytext()); }
"*" 		{ return new_symbol(sym.STAR, yytext()); }
"/" 		{ return new_symbol(sym.DIV, yytext()); }
"%" 		{ return new_symbol(sym.MOD, yytext()); }
"=>"        { return new_symbol(sym.ARROW, yytext()); }
"=" 		{ return new_symbol(sym.EQUAL, yytext()); }
"<" 		{ return new_symbol(sym.LESS, yytext()); }
">" 		{ return new_symbol(sym.GREATER, yytext()); }
":" 		{ return new_symbol(sym.COLON, yytext()); }
";" 		{ return new_symbol(sym.SEMI, yytext()); }
"," 		{ return new_symbol(sym.COMMA, yytext()); }
"."			{ return new_symbol(sym.PERIOD, yytext()); }
"?" 		{ return new_symbol(sym.QMARK, yytext()); }
"!" 		{ return new_symbol(sym.EMARK, yytext()); }
"(" 		{ return new_symbol(sym.LPAREN, yytext()); }
")" 		{ return new_symbol(sym.RPAREN, yytext()); }
"{" 		{ return new_symbol(sym.LBRACE, yytext()); }
"}"			{ return new_symbol(sym.RBRACE, yytext()); }
"[" 		{ return new_symbol(sym.LBRACK, yytext()); }
"]"			{ return new_symbol(sym.RBRACK, yytext()); }
"&" 		{ return new_symbol(sym.AMPER, yytext()); }
"|" 		{ return new_symbol(sym.VLINE, yytext()); }
"++" 		{ return new_symbol(sym.PLUSPLUS, yytext()); }
"--" 		{ return new_symbol(sym.MINUSMINUS, yytext()); }
"==" 		{ return new_symbol(sym.EQUALEQUAL, yytext()); }
"!=" 		{ return new_symbol(sym.NOTEQUAL, yytext()); }
">=" 		{ return new_symbol(sym.GREATEREQUAL, yytext()); }
"<=" 		{ return new_symbol(sym.LESSEQUAL, yytext()); }
"&&" 		{ return new_symbol(sym.AMPERAMPER, yytext()); }
"||" 		{ return new_symbol(sym.VLINEVLINE, yytext()); }
'[ -~]'     { return new_symbol(sym.CHARCONST, yytext().charAt(1)); }


"//" {yybegin(COMMENT);}
<COMMENT> . {yybegin(COMMENT);}
<COMMENT> "\r\n" { yybegin(YYINITIAL); }

[0-9]+  { return new_symbol(sym.NUMBER, new Integer (yytext())); }
([a-zA-Z])[a-zA-Z0-9_]* 	{return new_symbol (sym.IDENT, yytext()); }

. { System.err.println("Leksicka greska ("+yytext()+") u liniji "+(yyline+1)); }
