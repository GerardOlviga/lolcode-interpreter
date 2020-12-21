package parser;

//abstractions - chunks that make up the parse tree (statements, expressions,etc)
public enum Abstraction {
	PROGRAM,					//PROGRAM START END Keywords
	STATEMENT_STARTER,			//START OF STATEMENTS Keywords
	ARITHMETIC_OPERATOR,		//EXPRESSION Keywords
	LOGIC_OPERATOR,
	COMPARISON_OPERATOR,
	JUMP,						//IF ELSE, SWITCH CASE, LOOP, FUNCTION Keywords
	DATATYPE,					//Datatypes
	VARIABLE,
	LITERAL,
	KEYWORD 					//other Keywords not represented yet
}
