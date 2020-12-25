package lexicalanalyzer;

public enum TokenType {
	//identifiers
	VAR_IDENTIFIER,
	FUNC_IDENTIFIER,
	LOOP_IDENTIFIER,

	//literals
	INT_LITERAL,
	FLOAT_LITERAL,
	STR_LITERAL,
	BOOL_TRUE,
	BOOL_FALSE,
	TYPE_LITERAL,

	//Keywords
	PROGRAM_START, 			// HAI start keyword
	PROGRAM_END,			// KTHXBYE end keyword

	SINGLE_COMMENT, 		// BTW single line comment
	MULTI_COMMENT_START,	// OBTW multi line comment start
	MULTI_COMMENT_END,		//TLDR multiline comment end

	VAR_DECLARE,			//I HAS A declare a variable
	VAR_INITIALIZE,			//ITZ define recently declared variable
	VAR_IMPLICIT,			//IT Implicit variable

	DATATYPE_NONE,			//Datatypes
	DATATYPE_INT,
	DATATYPE_FLOAT,
	DATATYPE_STRING,
	DATATYPE_BOOLEAN,

	ASSIGNMENT,				//R	assignment operator
	PRINT,					//VISIBLE print statement
	USER_INPUT,				//GIMME user input

	EXPR_OP_SEPARATOR,		//AN Operand Separator

	EXPR_ADD,				//Arithmetic Operations
	EXPR_SUB,
	EXPR_MUL,
	EXPR_DIV,
	EXPR_MOD,
	EXPR_MAX,
	EXPR_MIN,

	BOOL_AND,				//Logic Operations
	BOOL_OR,
	BOOL_XOR,
	BOOL_NOT,
	BOOL_INF_AND, 			//Infinite Arity AND
	BOOL_INF_OR,			//Infinite Arity OR
	BOOL_INF_END,			//MKAY end infinite arity operation

	COMP_EQUAL,				//Comparison Operations
	COMP_NOT_EQUAL,

	CTRL_IF_THEN,			//If-Then
	CTRL_IF,
	CTRL_ELSEIF,
	CTRL_ELSE,


	CTRL_SWITCH,			//Switch Case
	CTRL_CASE,
	BREAK,
	CTRL_CASE_DEFAULT,
	CTRL_END,


	TYPECAST,				//MAEK
	TYPECAST_SEPARATOR, 	//A
	STR_CONCAT,

	//Unidentified Keyword
	UNKNOWN_KEYWORD,

	//EOF token that detemines end of file
	END_OF_FILE

}
