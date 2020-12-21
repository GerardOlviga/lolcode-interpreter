package parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import lexicalanalyzer.Lexeme;
import lexicalanalyzer.TokenType;
import semanticanalyzer.SemanticAnalyzer;
import symboltable.Symbol;
import symboltable.SymbolTable;

/*
 Parser

		This Parser takes the token stream and does 3 things.
	(1) Check if the the token stream is syntactically correct.
	(2) Construct and Populate the Symbol Table
	(3) Facilitate Semantic Analysis by calling methods from semantic analyzer in order to conduct both syntactic
	and semantic analysis at the same time (thereby immediately executing commands once certain statements are
	considered valid).


*/

public class Parser {
	private ArrayList<Lexeme> tokenStream; 	//tokens from lexical analyzer
	private boolean valid;					//dictates whether the token stream is still valid or not.
	private Iterator<Lexeme> iter;			//iterator
	private Lexeme current;					//current lexeme being checked
	private Lexeme next;					//next lexeme being checked

	private SymbolTable global;					//symbol table that holds global variables.


	public Parser(ArrayList<Lexeme> tokenStream) {

		//the token stream from the lexical analyzer is passed here.
		this.tokenStream = tokenStream;

		//create symbol table
		this.global = new SymbolTable();


		//assume that a blank file is valid.
		this.valid= true;

		//start iteration
		this.iter = this.tokenStream.iterator();


		this.current = null;
		this.next = null;

	}

	public void start(){
		//start syntax analysis of the program.
		this.valid = analyzeProgram();

		//debug
		System.out.println("\n Final Global Symbol Table");
		global.print();

		System.out.print("The program is ");
		if(this.valid){
			System.out.print("valid");
		}else{
			System.out.print("not valid");
		}

	}
	//determine if the program is valid
	/*
	<program> ::= HAI <statement> KTHXBYE
	*/
	private boolean analyzeProgram(){

		//access the first lexeme of the token stream
		if(this.iter.hasNext()){
			this.current = this.iter.next();
		}

		//check if program starts with HAI
		if(this.current.getClassifier() != TokenType.PROGRAM_START){
			printErrorMsg(this.current.getLineNo(),"must start with HAI.");
			return false;
		}

		//program traversal
		while (this.iter.hasNext()){
				//access the next lexeme
				this.next = this.iter.next();


				//Determine if statement is called
				//Print
				System.out.println("PROGRAM: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());

				//skip HAI
				if(this.current.getClassifier() == TokenType.PROGRAM_START){
					this.current = this.next;
					this.next = this.iter.next();
					//skip Version Number
					if(this.current.getClassifier() == TokenType.FLOAT_LITERAL){
						this.current = this.next;
						this.next = this.iter.next();
					}
				}


				//Lexeme is a Statement Starter
				if(this.current.isStatementStarter(this.next)){
					analyzeStatement(this.global);
				}
				//not a statement starter
				else{
					printErrorMsg(this.current.getLineNo(),"variable '" + this.current.getValue() + "' not found");
					return false;
					//the next becomes current
					//this.current = this.next;
				}

				//if found invalid from here
				if(!(this.valid)){
					return false;
				}

			}

		//check if program ends with KTHXBYE
		if(this.next.getClassifier() != TokenType.PROGRAM_END){
			printErrorMsg(this.next.getLineNo(),"must end with KTHXBYE.");
			return false;
		}
		return true;
	}

	//<statement> ::= <vardeclare> | <varassign> | <expr> |<ifthen> | <switch> | <print> | <scan>|<concat>
	private void analyzeStatement(SymbolTable st) {
		//the next becomes current
		System.out.println("STATEMENT: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());

		//--Statement is Print Statement
		if(this.current.getClassifier() == TokenType.PRINT){
			analyzePrint(st);
		}
//
//		//--Statement is Variable Declaration
		else if(this.current.getClassifier() == TokenType.VAR_DECLARE){
			analyzeVarDeclare(st);
		}

		//--Statement is Assignment
		else if(this.current.getClassifier() == TokenType.VAR_IDENTIFIER && this.current.getClassifier() == TokenType.VAR_IDENTIFIER){
			analyzeVarAssign(st);
		}

		//unknown statement starter
		else{
			printErrorMsg(this.current.getLineNo(),"variable '" + this.current.getValue() + "' not found");
			this.valid = false;
		}
		//check if program is still valid.
		if(this.valid){
			this.current = this.next;
			//after declaration move to next state
			if(iter.hasNext()){
				this.next = this.iter.next();
				System.out.println("AFTER-STATEMENT: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());
				if(this.current.isStatementStarter(this.next)){
					analyzeStatement(st);
				}
			}
		}


	}
	/*
	 <var_assign> ::= varident R <value>
	 <value> ::= varident |  <expr> |  <literal>
	 */

	private void analyzeVarAssign(SymbolTable st) {
		try{

			if (this.lexemesAreInSameLine()){
				System.out.println("ASSIGNMENT: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());

				//get Variable Name
				String varName = this.current.getValue();

				//move lexemes
				this.current = this.next;
				this.next = this.iter.next();
				System.out.println("VAR ASSIGN: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());
				//check if next is literal variable or expression ,and next and current are on different lines
				if ((this.next.isLiteral() || this.next.isOperationSymbol() || this.next.getClassifier() == TokenType.VAR_IDENTIFIER) && this.lexemesAreInSameLine()){

					System.out.println("VAR ASSIGN: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());



					//semantically analyze variable assignment
					if(SemanticAnalyzer.variableInSymbolTable(varName, st)){
						this.valid = SemanticAnalyzer.assignVariable(varName,this.next,st,this);
					}else{
						//declare error
						printErrorMsg(this.next.getLineNo(),"Variable '" +varName +"' undeclared.");
						this.valid = false;
					}
					//move to next lexeme after this.
					this.current = this.next;
					this.next = this.iter.next();

				}else{
					printErrorMsg(this.current.getLineNo(),"expecting a value for declared variable.");
					this.valid = false;
				}

			}else{
				printErrorMsg(this.current.getLineNo(),"expecting an assignment operator R.");
				this.valid = false;
			}
		}catch(NoSuchElementException e){
			//e.printStackTrace();
			printErrorMsg(this.current.getLineNo(),"assignment ends abruptly.");
			this.valid = false;
		}
	}

	/*
	 <var_declare> ::= I HAS A varident | I HAS A varident ITZ <value>
	 <value> ::= varident |  <expr> |  <literal>
	 */
	private void analyzeVarDeclare(SymbolTable st){
	try{
			//check if next is variable identifier and if next and current are on same line
		if ( this.next.getClassifier() == TokenType.VAR_IDENTIFIER && this.lexemesAreInSameLine()){
			System.out.println("VAR DECLARE: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());
			//VarDeclare is valid

			//point current to the next lexeme
			this.current = this.next;

			//get Variable Name
			String varName = this.current.getValue();

			//point next to the next iteration
			this.next = this.iter.next();
			//check if next is variable initialization keyword and if next and current are on same line
			if (this.next.getClassifier() == TokenType.VAR_INITIALIZE && this.lexemesAreInSameLine()){
				System.out.println("VAR INITIALIZE: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());

				//point current to the next lexeme
				this.current = this.next;

				//point next to the next iteration
				this.next = this.iter.next();
				System.out.println("VAR ASSIGN: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());

				//check if next is literal variable or expression ,and next and current are on different lines
				if ((this.next.isLiteral() || this.next.isOperationSymbol() || this.next.getClassifier() == TokenType.VAR_IDENTIFIER) && this.lexemesAreInSameLine()){

					System.out.println("VAR ASSIGN: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());



					//semantically analyze variable declaration
					this.valid = SemanticAnalyzer.assignVariable(varName,this.next,st,this);



					//move to next lexeme after this.
					this.current = this.next;
					this.next = this.iter.next();

				}else{
					printErrorMsg(this.current.getLineNo(),"expecting a value for declared variable.");
					this.valid = false;
				}

			}else{
				//unitialized variable
				SemanticAnalyzer.declareUnitializedVariable(varName,st);

			}

		}else{
			printErrorMsg(this.current.getLineNo(),"expecting a variable identifier.");
			this.valid = false;
		}
	}catch(NoSuchElementException e){
		//e.printStackTrace();
		printErrorMsg(this.current.getLineNo(),"variable declaration ends abruptly.");
		this.valid = false;
	}


	}


	/*
	 <print> ::= VISIBLE <print_values>
	 <print_values> :: <print_string> <print_values> | <value>
	 <value> ::= varident |  <expr> |  <literal>
	 */
	private void analyzePrint(SymbolTable st) {
		System.out.println("PRINT: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());
		//check if next is literal or variable and if next and current are on different lines
		if ((this.next.isLiteral() || this.next.isOperationSymbol() || this.next.getClassifier() == TokenType.VAR_IDENTIFIER) && this.lexemesAreInSameLine()){

			//Print is valid

			String printValues = "";
			//repeat until current is a non identifier
			while (iter.hasNext() && (this.next.isLiteral() || this.next.isOperationSymbol() || this.next.getClassifier() == TokenType.VAR_IDENTIFIER)){

				System.out.println("PRINTVALUE: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());

				//special case identifier to not include var identifier from possible assignment
				if(this.next.getClassifier() == TokenType.VAR_IDENTIFIER && !(lexemesAreInSameLine())){
					break;
				}
				//get Values to print
				String currentValue = SemanticAnalyzer.getPrintString(this.next,st,this);
				if(currentValue != null){
					printValues = printValues + currentValue;
				}else{
					this.valid = false;
					break;
				}


				//go to next lexeme
				this.current = this.next;
				this.next = this.iter.next();

			}
			//print values
			if(this.valid){
				System.out.println(printValues);
			}


			//exiting function and be analyzed by next statement

		}else{
			printErrorMsg(this.current.getLineNo(),"expecting a value to print.");
			this.valid = false;
		}

	}

	/*
	<expression> ::= <arith_operation> | <comp_operation> | <logic_operation>
	<arith_operation> ::=  <arith_operator> <operand> AN <operand>
	<operand> ::=  <arith_operation> | <literal> | <variable>
	<arith_operator> ::= SUM OF | DIFF OF ..

	*/
	public Symbol<Object> analyzeExpression(SymbolTable st) {

		//move to next lexeme
		this.current = this.next;
		this.next = this.iter.next();

		System.out.println("EXPRESSION: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());

		//arithmetic expression
		if(this.current.isArithmeticOperator()){
			return getArithmeticAnswer(st);
		}

		printErrorMsg(this.current.getLineNo()," '" + this.current.getValue() + "' not an expression operator");
		return null;
	}
	//get Answer from arithmetic
	private Symbol<Object> getArithmeticAnswer(SymbolTable st) {
		System.out.println("ARITH EXPRESSION: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());


		TokenType operator = this.current.getClassifier();
		Symbol<Object> operand1 = null;
		Symbol<Object> operand2 = null;

		System.out.println("ARITH OP1: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());
		//if op1 is an expression
		if(this.next.isArithmeticOperator()){
			//move to next lexeme
			this.current = this.next;
			this.next = this.iter.next();
			operand1 = getArithmeticAnswer(st);
		//if op1 is a integer or variable
		}else if(this.next.isLiteral() || this.next.getClassifier() == TokenType.VAR_IDENTIFIER){
			operand1 = SemanticAnalyzer.getSymbolFromLiteralOrVariable(this.next, st);
		}else{
			printErrorMsg(this.next.getLineNo(),"'" + this.next.getValue() + "' is not a valid operand.");
			return null;
		}
		//if operand 1 == null
		if(operand1 == null){
			return null;
		}

		//move to next lexeme
		this.current = this.next;
		this.next = this.iter.next();

		System.out.println("ARITH SEP: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());

		// if next value is not AN
		if(this.next.getClassifier() != TokenType.EXPR_OP_SEPARATOR){
			printErrorMsg(this.next.getLineNo(),"separator AN not found.");
			return null;
		}
		//move to next lexeme
		this.current = this.next;
		this.next = this.iter.next();
		// if next
		System.out.println("ARITH OP2: Current: "+ this.current.getValue() + " Next: " + this.next.getValue());
		//if op2 is an expression
		if(this.next.isArithmeticOperator()){
			//move to next lexeme
			this.current = this.next;
			this.next = this.iter.next();
			operand2 = getArithmeticAnswer(st);
		//if op2 is a integer or variable
		}else if(this.next.isLiteral() || this.next.getClassifier() == TokenType.VAR_IDENTIFIER){
			operand2 = SemanticAnalyzer.getSymbolFromLiteralOrVariable(this.next, st);
		}else{
			printErrorMsg(this.next.getLineNo(),"'" + this.next.getValue() + "' is not a valid operand.");
			return null;
		}

		//if operand 2 == null
		if(operand2 == null){
			return null;
		}

		//perform operation
		return	SemanticAnalyzer.performArithmeticOperation(operator,operand1,operand2,this.next.getLineNo());
	}


	//check if previous lexeme and next lexeme are in the same line
	private boolean lexemesAreInSameLine(){
		if(this.current.getLineNo() == this.next.getLineNo()){
			return true;
		}
		return false;
	}

	public static void printErrorMsg(int lineNo, String msg) {
		System.out.println("Error at Line " + lineNo + " : " + msg);

	}



}
