package parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import lexicalanalyzer.Lexeme;
import lexicalanalyzer.TokenType;
import semanticanalyzer.SemanticAnalyzer;
import symboltable.Symbol;
import symboltable.SymbolTable;
import user.Main;

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

	//DEBUG MODE
	private boolean debugMode;

	public Parser(ArrayList<Lexeme> tokenStream) {

		//the token stream from the lexical analyzer is passed here.
		this.tokenStream = tokenStream;

		//create symbol table
		this.global = new SymbolTable();

		//global symbol table always have the implicit variable
		SemanticAnalyzer.declareUnitializedVariable("IT", this.global);


		//assume that a blank file is valid.
		this.valid= true;

		//start iteration
		this.iter = this.tokenStream.iterator();

		//DEBUG:
		this.debugMode = Main.debugMode;

		this.current = null;
		this.next = null;

	}

	public void start(){
		//start syntax analysis of the program.
		this.valid = analyzeProgram();

		if(debugMode){
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
				printTokenStreamTrace("PROGRAM");

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

		printTokenStreamTrace("STATEMENT");

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
		else if(this.current.isVariable() && this.next.getClassifier() == TokenType.ASSIGNMENT){
			analyzeVarAssign(st);
		}

		//--Statement is an input statement
		else if(this.current.getClassifier() == TokenType.USER_INPUT){
			analyzeInput(st);
		}

		//statement is an implicit variable expression.
		else if(this.current.isOperationSymbol()){
			analyzeImplicitAssignment(st);
		}
		//unknown statement starter
		else{
			printErrorMsg(this.current.getLineNo(),"action '" + this.current.getValue() + "' not found");
			this.valid = false;
		}
		//check if program is still valid.
		if(this.valid){
			this.current = this.next;
			//after declaration move to next state
			if(iter.hasNext()){
				this.next = this.iter.next();
				printTokenStreamTrace("NEXT-STATEMENT");
				if(this.current.isStatementStarter(this.next)){
					analyzeStatement(st);
				}
			}
		}


	}

	//<imp_assign> ::= <expression>
	private void analyzeImplicitAssignment(SymbolTable st) {
		printTokenStreamTrace("ExpressionImplicit");

		//semantically analyze variable declaration
		this.valid = SemanticAnalyzer.assignVariable("IT",this.current,st,this);
	}

	//<input> ::= GIMME varident
	private void analyzeInput(SymbolTable st) {
		try{
			printTokenStreamTrace("USER INPUT");

			//move one lexeme
			this.current = this.next;
			this.next = this.iter.next();

			printTokenStreamTrace("VAR INPUT");
			if(this.current.isVariable()){

				//define the variable name
				String varName = this.current.getValue();

				//semantically analyze getting user input
				if(SemanticAnalyzer.variableInSymbolTable(varName, st)){
					this.valid = SemanticAnalyzer.getUserInput(varName,this.current,st);
				}else{
					//declare error
					printErrorMsg(this.next.getLineNo(),"Variable '" +varName +"' undeclared.");
					this.valid = false;
				}

			}else{
				printErrorMsg(this.current.getLineNo(),"expecting a value for declared variable.");
				this.valid = false;
			}


		}catch(NoSuchElementException e){
			printErrorMsg(this.current.getLineNo(),"user input ends abruptly.");
			this.valid = false;
		}

	}

	/*
	 <var_assign> ::= varident R <value>
	 <value> ::= varident |  <expr> |  <literal>
	*/

	private void analyzeVarAssign(SymbolTable st) {
		try{

			if (this.lexemesAreInSameLine()){
				printTokenStreamTrace("ASSIGNMENT");

				//get Variable Name
				String varName = this.current.getValue();

				//move lexemes
				this.moveToNextLexeme();
				printTokenStreamTrace("VAR_ASSIGN");

				//check if next is literal variable or expression ,and next and current are on different lines
				if ((this.next.isLiteral() || this.next.isOperationSymbol() || this.next.isVariable()) && this.lexemesAreInSameLine()){

					this.moveToNextLexeme();
					printTokenStreamTrace("VAR_ASSIGN");

					//semantically analyze variable assignment
					if(SemanticAnalyzer.variableInSymbolTable(varName, st)){
						this.valid = SemanticAnalyzer.assignVariable(varName,this.current,st,this);
					}else{
						//declare error
						printErrorMsg(this.current.getLineNo(),"Variable '" +varName +"' undeclared.");
						this.valid = false;
					}


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
		if ( this.next.isVariable() && this.lexemesAreInSameLine()){
			printTokenStreamTrace("VAR_DECLARE");
			//VarDeclare is valid

			//point current to the next lexeme
			this.moveToNextLexeme();

			//get Variable Name
			String varName = this.current.getValue();

			//check if next is variable initialization keyword and if next and current are on same line
			if (this.next.getClassifier() == TokenType.VAR_INITIALIZE && this.lexemesAreInSameLine()){
				printTokenStreamTrace("VAR_INITIALIZE");

				//point current to the next lexeme
				this.current = this.next;
				this.next = this.iter.next();

				printTokenStreamTrace("VAR_ASSIGN");

				//check if next is literal variable or expression ,and next and current are on different lines
				if ((this.next.isLiteral() || this.next.isOperationSymbol() || this.next.isVariable()) && this.lexemesAreInSameLine()){

					//move to next lexeme after this.
					this.moveToNextLexeme();

					printTokenStreamTrace("VAR_ASSIGN");



					//semantically analyze variable declaration
					this.valid = SemanticAnalyzer.assignVariable(varName,this.current,st,this);

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
		//TODO: Make Sure that only current lexeme is passed to semantic Analysis. next only serves as lookahead
		printTokenStreamTrace("PRINT");
		//check if next is literal or variable and if next and current are on different lines
		if ((this.next.isLiteral() || this.next.isOperationSymbol() || this.next.isVariable()) && this.lexemesAreInSameLine()){

			//Print is valid
			printTokenStreamTrace("PRINT_START");

			moveToNextLexeme();


			String printValues = "";
			//repeat until current is a non identifier
			while (this.current.isLiteral() || this.current.isOperationSymbol() || this.current.isVariable()){



				printTokenStreamTrace("PRINT_VALUE");


				//get Values to print
				String currentValue = SemanticAnalyzer.getPrintString(this.current,st,this);
				printTokenStreamTrace("PAFTER CURRVALUE");

				if(currentValue != null){
					printValues = printValues + currentValue;
				}else{
					this.valid = false;
					break;
				}

				printTokenStreamTrace("AFTER ADD VALUE");
				//only move to next lexeme if lexemes are on the same line or lexemes are a valid print value
				if(this.lexemesAreInSameLine() && (this.next.isLiteral() || this.next.isOperationSymbol() || this.next.isVariable())){
					moveToNextLexeme();
				}else{
					break;
				}


			}
			//print values
			if(this.valid){
				printTokenStreamTrace("PRINT_END");
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


	*/
	public Symbol<Object> analyzeExpression(SymbolTable st) {

	Symbol<Object> resultSymbol = null;

		printTokenStreamTrace("EXPRESSION");

		//arithmetic expression
		if(this.current.isArithmeticOperator()){
			resultSymbol = getArithmeticAnswer(st);
		}

		//check for result symbol results
		if(resultSymbol != null){

			//move to next lexeme
			//this.current = this.next;
			//this.next = this.iter.next();

			return resultSymbol;
		}

		//printErrorMsg(this.current.getLineNo()," '" + this.current.getValue() + "' not an expression operator");
		return null;
	}
	/*
	<arith_operation> ::=  <arith_operator> <operand> AN <operand>
	<operand> ::=  <arith_operation> | <literal> | <variable>
	<arith_operator> ::= SUM OF | DIFF OF ..
	*/
	//get Answer from arithmetic
	private Symbol<Object> getArithmeticAnswer(SymbolTable st) {
		printTokenStreamTrace("ARITH EXPRESSION");


		TokenType operator = this.current.getClassifier();
		Symbol<Object> operand1 = null;
		Symbol<Object> operand2 = null;

		this.moveToNextLexeme();
		printTokenStreamTrace("ARITH OP1");
		//if op1 is an expression
		if(this.current.isArithmeticOperator()){
			//move to next lexeme
			operand1 = getArithmeticAnswer(st);
		//if op1 is a integer or variable
		}else if(this.current.isLiteral() || this.current.isVariable()){
			operand1 = SemanticAnalyzer.getSymbolFromLiteralOrVariable(this.current, st);
		}else{
			printErrorMsg(this.current.getLineNo(),"'" + this.current.getValue() + "' is not a valid operand.");
			return null;
		}
		//if operand 1 == null
		if(operand1 == null){
			return null;
		}

		//move to next lexeme
		this.moveToNextLexeme();
		printTokenStreamTrace("ARITH SEP");

		// if next value is not AN
		if(this.current.getClassifier() != TokenType.EXPR_OP_SEPARATOR){
			printErrorMsg(this.current.getLineNo(),"separator AN not found.");
			return null;
		}
		//move to next lexeme
		this.moveToNextLexeme();
		// if next
		printTokenStreamTrace("ARITH OP2");
		//if op2 is an expression
		if(this.current.isArithmeticOperator()){
			operand2 = getArithmeticAnswer(st);
		//if op2 is a integer or variable
		}else if(this.current.isLiteral() || this.current.isVariable()){
			operand2 = SemanticAnalyzer.getSymbolFromLiteralOrVariable(this.current, st);
		}else{
			printErrorMsg(this.current.getLineNo(),"'" + this.current.getValue() + "' is not a valid operand.");
			return null;
		}

		//if operand 2 == null
		if(operand2 == null){
			return null;
		}

		printTokenStreamTrace("ARITH EXPRESSION END");

		//perform operation
		return	SemanticAnalyzer.performArithmeticOperation(operator,operand1,operand2,this.current.getLineNo());
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

	//move to next lexeme
	public void moveToNextLexeme(){
		if(this.iter.hasNext()){
			this.current = this.next;
			this.next = this.iter.next();
		}
	}

	//debug
	private void printTokenStreamTrace(String label){
			if(debugMode){
				System.out.println( label +": Current: "+ this.current.getValue() + " Next: " + this.next.getValue());
			}
		}



}
