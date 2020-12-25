package parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

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
	private boolean errorDetected;			//detects if an error has been detected. (halts printing of other errors due to the parser's stack based nature.
	private Iterator<Lexeme> iter;			//iterator
	private Lexeme current;					//current lexeme being checked
	private Lexeme next;					//next lexeme being checked

	private SymbolTable global;				//symbol table that holds global variables.
	private int depth;						//depth of code blocks. 0 - global. 1 - local to global, 2 - local to local to global , and so on

	//DEBUG MODE
	private boolean debugMode;

	public Parser(ArrayList<Lexeme> tokenStream) {

		//the token stream from the lexical analyzer is passed here.
		this.tokenStream = tokenStream;

		//create symbol table
		this.global = new SymbolTable(null);

		//set depth to 0
		this.depth = 0;


		//assume that a blank file is valid.
		this.valid= true;

		//start iteration
		this.iter = this.tokenStream.iterator();

		//DEBUG:
		this.debugMode = Main.debugMode;

		//error checking
		this.errorDetected = false;

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
		}

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
		if (this.iter.hasNext()){
				//access the next lexeme
				this.next = this.iter.next();
		}

				//Determine if statement is called
				//Print
				printTokenStreamTrace("PROGRAM");

				//skip HAI
				if(this.current.getClassifier() == TokenType.PROGRAM_START){
					this.moveToNextLexeme();
					//skip Version Number
					if(this.current.getClassifier() == TokenType.FLOAT_LITERAL){
						this.moveToNextLexeme();
					}
				}


				//Lexeme is a Statement Starter
				if(this.current.isStatementStarter(this.next)){
					boolean statementValid = analyzeStatement(this.global,0);
					if(statementValid){
						this.printTokenStreamTrace("ALL STATEMENT END");
					}
				}
				//not a statement starter
				else{
					printErrorMsg(this.current.getLineNo(),"action '" + this.current.getValue() + "' not found");
					return false;

				}



		//check if program ends with KTHXBYE
		if(this.current.getClassifier() != TokenType.PROGRAM_END){
			printErrorMsg(this.current.getLineNo(),"must end with KTHXBYE.");
			return false;
		}
		return true;
	}

	//<statement> ::= <vardeclare> | <varassign> | <expr> |<ifthen> | <switch> | <print> | <scan>|<concat>
	private boolean analyzeStatement(SymbolTable st,int depth) {

		printTokenStreamTrace("STATEMENT");

		//local boolean flag that checks if statement is still valid
		boolean statementValid = true;

		//--Statement is Assignment
		if(this.current.isVariable() && this.next.getClassifier() == TokenType.ASSIGNMENT){
			statementValid = analyzeVarAssign(st);
		}
		//Statement is an implicit variable assignment.
		else if(this.current.isOperationSymbol() ||  this.current.isLiteral() || this.current.isVariable()){
			statementValid = analyzeImplicitAssignment(st);
		}
		else{
			//other statements
			switch(this.current.getClassifier()){
			case PRINT:
				statementValid = analyzePrint(st);
				break;
			case VAR_DECLARE:
				statementValid = analyzeVarDeclare(st);
				break;
			case USER_INPUT:
				statementValid = analyzeInput(st);
				break;
			case CTRL_IF_THEN:
				statementValid = analyzeIfThen(st,depth);	//since if then is a ctrl statement. depth is incremented by 1
				//this.depth --;								//remove depth
				break;
			default:
				//do nothing
			}
		}

		//check if program is still valid.
		if(this.valid && statementValid){
			moveToNextLexeme();
			printTokenStreamTrace("NEXT-STATEMENT");
			//if next statement is not a statement starter
			if(this.current.isStatementStarter(this.next)){
				return analyzeStatement(st,depth);
			}


			//if current lexeme is the program ending or jump keyword
			if(this.current.getClassifier() == TokenType.PROGRAM_END ||
					this.current.getAbstraction() == Abstraction.JUMP){
				return true;
			}

			//if next lexeme is the EOF
			if(this.next.getClassifier() == TokenType.END_OF_FILE){
				printErrorMsg(this.current.getLineNo(),"KTHXBYE is missing.");
				return false;
			}


			printErrorMsg(this.current.getLineNo(),"action '" + this.current.getValue() + "' not found");
			return false;

		}
		return false;
	}

	//control flow if then
	//<if_then> ::= O RLY? YA RLY <statement> <else> | O RLY? YA RLY <else>
	//<else> ::= MEBBE <condition> <statement> <else> | MEBBE <condition> <else>
	//			| NO WAI <statement> OIC | NO WAI OIC
	private boolean analyzeIfThen(SymbolTable st, int depth) {
		//update depth
		int localdepth = depth + 1;
		printTokenStreamTrace("CTRL IF THEN");

		//create a local symbol table
		SymbolTable local = new SymbolTable(st);

		//get IT
		Symbol<Object> implicitVar = SemanticAnalyzer.getSymbolFromSymbolTable("IT", st, this.current.getLineNo());

		//supress future error messages and declare error
		if(implicitVar == null){
			this.errorDetected = true;
			return false;
		}

		//get Boolean value of IT
		boolean conditionResult = SemanticAnalyzer.getBooleanFromSymbol(implicitVar);
		boolean ctrlFlowValid = false;

		moveToNextLexeme();
		printTokenStreamTrace("CTRL IF");

		//determine if 'if statement' or not
		if(this.current.getClassifier() != TokenType.CTRL_IF){
			this.printErrorMsg(this.current.getLineNo(),"expecting YA RLY.");
		}
		moveToNextLexeme();
		printTokenStreamTrace("IF STATEMENT");

		//determine if segment starter
		if (this.current.isStatementStarter(next)){


			//condition check
			if(conditionResult){
				ctrlFlowValid = analyzeStatement(local,localdepth);
			}else{
				ctrlFlowValid = ignoreLexemesUntilNextIfElseBlock(localdepth);
				//what is ctrl flow statement here?
			}

			//check if valid or not
			if(!(ctrlFlowValid)){
				return false;
			}

		}
		printTokenStreamTrace("END OF IF STATEMENT");
		//check if 'else statement'
		if(this.current.getClassifier() == TokenType.CTRL_ELSE){
			moveToNextLexeme();
			printTokenStreamTrace("ELSE STATEMENT");
			//determine if segment starter
			if (this.current.isStatementStarter(next)){



				//condition check
				if(!(conditionResult)){
					ctrlFlowValid = analyzeStatement(local,depth);
				}else{
					ctrlFlowValid = ignoreLexemesUntilNextIfElseBlock(depth);
				}

				//check if valid or not
				if(!(ctrlFlowValid)){
					return false;
				}

			}
			printTokenStreamTrace("END OF ELSE STATEMENT");
		}

		//check if end of ctrl
		if(this.current.getClassifier() == TokenType.CTRL_END){
			printTokenStreamTrace("END OF CTRL FLOW STATEMENT");
			return true;
		}
		this.printErrorMsg(this.current.getLineNo(),"expecting OIC.");
		return false;
	}

	//ignore all the lexemes in this statement code block until another ctrl flow keyword is found (if then)
	private boolean ignoreLexemesUntilNextIfElseBlock(int depth) {
		int localdepth = depth;
		while(true){

			printTokenStreamTrace("ELSE STATEMENTS");
			this.moveToNextLexeme();
//			try {
//				TimeUnit.SECONDS.sleep(2);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			//a ctrl statement is found
			if(this.current.getClassifier() == TokenType.CTRL_IF_THEN){
				localdepth++;
			}

			if(localdepth == depth && (this.current.getClassifier() == TokenType.CTRL_ELSE ||
				this.current.getClassifier() == TokenType.CTRL_END)){
				return true;
			}

			//ctrl statement ended
			if(this.current.getClassifier() == TokenType.CTRL_END){
				localdepth--;
			}

			if(this.next.getClassifier() == TokenType.END_OF_FILE){
				printErrorMsg(this.current.getLineNo(),"control flow statement reached EOF");
				return false;
			}
		}
	}

	//<imp_assign> ::= <expression>
	private boolean analyzeImplicitAssignment(SymbolTable st) {
		printTokenStreamTrace("Implicit");

		//semantically analyze variable declaration
		boolean assignValid = SemanticAnalyzer.assignVariable("IT",this.current,st,this);

		//supress other error messages if a semantic error has been found
		if(!(assignValid)){
			this.valid = false;
			this.errorDetected = true;
		}

		return assignValid;
	}

	//<input> ::= GIMME varident
	private boolean analyzeInput(SymbolTable st) {
		try{
			printTokenStreamTrace("USER INPUT");

			//move lexeme
			this.moveToNextLexeme();

			printTokenStreamTrace("VAR INPUT");
			if(this.current.isVariable()){

				//define the variable name
				String varName = this.current.getValue();

				//semantically analyze getting user input
				if(SemanticAnalyzer.variableInSymbolTable(varName, st)){
					boolean userInputValid = SemanticAnalyzer.getUserInput(varName,this.current,st);

					//supress other error messages if a semantic error has been found
					if(!(userInputValid)){
						this.errorDetected = true;
					}

					return userInputValid;
				}

				printErrorMsg(this.current.getLineNo(),"Variable '" +varName +"' undeclared.");
				return false;

			}
			printErrorMsg(this.current.getLineNo(),"expecting a value for declared variable.");
			return false;

		}catch(NoSuchElementException e){
			printErrorMsg(this.current.getLineNo(),"user input ends abruptly.");
			return false;
		}

	}

	/*
	 <var_assign> ::= varident R <value>
	 <value> ::= varident |  <expr> |  <literal>
	*/

	private boolean analyzeVarAssign(SymbolTable st) {
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
						boolean assignValid = SemanticAnalyzer.assignVariable(varName,this.current,st,this);

						//supress other error messages if a semantic error has been found
						if(!(assignValid)){
							this.errorDetected = true;
						}

						return assignValid;

					}

					printErrorMsg(this.current.getLineNo(),"Variable '" +varName +"' undeclared.");
					return false;

				}
				printErrorMsg(this.current.getLineNo(),"expecting a value for declared variable.");
				return false;

			}
			printErrorMsg(this.current.getLineNo(),"expecting an assignment operator R.");
			return false;

		}catch(NoSuchElementException e){
			//e.printStackTrace();
			printErrorMsg(this.current.getLineNo(),"assignment ends abruptly.");
			return false;
		}
	}

	/*
	 <var_declare> ::= I HAS A varident | I HAS A varident ITZ <value>
	 <value> ::= varident |  <expr> |  <literal>
	 */
	private boolean analyzeVarDeclare(SymbolTable st){
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
					moveToNextLexeme();

					printTokenStreamTrace("VAR_ASSIGN");



					//semantically analyze variable declaration
					boolean assignValid = SemanticAnalyzer.declareVariable(varName,this.current,st,this);

					//supress other error messages if a semantic error has been found
					if(!(assignValid)){
						this.errorDetected = true;
					}

					return assignValid;

				}
				printErrorMsg(this.current.getLineNo(),"expecting a value for declared variable.");
				return false;

			}

			//unitialized variable
			SemanticAnalyzer.declareUnitializedVariable(varName,st);
			return true;
		}

		printErrorMsg(this.current.getLineNo(),"expecting a variable identifier.");
		return false;

	}catch(NoSuchElementException e){
		//e.printStackTrace();
		printErrorMsg(this.current.getLineNo(),"variable declaration ends abruptly.");
		return false;
	}


	}


	/*
	 <print> ::= VISIBLE <print_values>
	 <print_values> :: <print_string> <print_values> | <value>
	 <value> ::= varident |  <expr> |  <literal>
	 */
	private boolean analyzePrint(SymbolTable st) {
		//TODO: Make Sure that only current lexeme is passed to semantic Analysis. next only serves as lookahead

		//flag that states whether print statement is valid or not
		boolean printValid = true;

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
					this.errorDetected = true;
					printValid = false;
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
			if(printValid){
				printTokenStreamTrace("PRINT_END");
				System.out.println(printValues);
				return true;
			}


			//exiting function and be analyzed by next statement

		}
		printErrorMsg(this.current.getLineNo(),"expecting a value to print.");
		return false;


	}

	/*
	<expression> ::= <arith_operation> | <comp_operation> | <logic_operation>


	*/
	public Symbol<Object> analyzeExpression(SymbolTable st) {

	Symbol<Object> resultSymbol = null;

		printTokenStreamTrace("EXPRESSION");

		switch(this.current.getAbstraction()){
			//arithmetic expression
			case ARITHMETIC_OPERATOR:
				resultSymbol = getArithmeticAnswer(st);
				break;
			//comparison expression
			case COMPARISON_OPERATOR:
				resultSymbol = getComparisonAnswer(st);
				break;
			//boolean expression
			case BOOLEAN_OPERATOR:
				resultSymbol = getBooleanAnswer(st);
				break;
			//bool inf expression
			case BOOL_INF_OPERATOR:
				resultSymbol = getBoolInfAnswer(st);
				break;
			default:
				//do nothing
		}
		//check for result symbol results
		if(resultSymbol != null){
			return resultSymbol;
		}
		printErrorMsg(this.current.getLineNo()," '" + this.current.getValue() + "' not an expression operator");
		return null;

	}

	/*
	<bool_inf_operation> ::= <bool_inf_operator> <bool_inf_value>
	<bool_inf_value> ::= <bool_inf_operand> AN <bool_inf_value> | <bool_inf_operand> MKAY
	<bool_inf_operand> ::=  <comp_operation> | <bool_operation> | <arith_operation> | <literal> | <variable>
	<bool_inf_operator> ::= ANY OF | ALL OF ..
	*/
	private Symbol<Object> getBoolInfAnswer(SymbolTable st) {
		printTokenStreamTrace("BOOL INF EXPRESSION");

		TokenType operator = this.current.getClassifier();
		ArrayList<Symbol<Object>> operands = new ArrayList<>();

		moveToNextLexeme();

		//first operand is required, else print an error
		printTokenStreamTrace("BOOL INF OPERAND1");
		//get boolInfOoerand
		Symbol<Object> operand1 = this.getBoolInfOperand(this.current, st);

		//if operand 1 == null
		if(operand1 == null){
			return null;
		}
		//add first operand to the list of operands
		operands.add(operand1);

		moveToNextLexeme();

		//while an
		while(this.current.getClassifier() == TokenType.EXPR_OP_SEPARATOR){
			printTokenStreamTrace("BOOL INF SEPARATOR");

			moveToNextLexeme();

			printTokenStreamTrace("BOOL INF OPERAND");

			Symbol<Object> operand = this.getBoolInfOperand(this.current, st);

			//if operand == null
			if(operand == null){
				return null;
			}
			//add operand to the list of operands
			operands.add(operand);

			moveToNextLexeme();

		}

		//check if end of bool inf
		if(this.current.getClassifier() == TokenType.BOOL_INF_END){
			printTokenStreamTrace("BOOL INF END");
			return SemanticAnalyzer.performBoolInfOperation(operator,operands);
		}else{
			printErrorMsg(this.current.getLineNo(),"Invalid Operation.");
			return null;
		}

	}

	private Symbol<Object> getBoolInfOperand(Lexeme lexeme, SymbolTable st) {
		switch(lexeme.getAbstraction()){

		case ARITHMETIC_OPERATOR:
		case COMPARISON_OPERATOR:
		case BOOLEAN_OPERATOR:
			return analyzeExpression(st);

		case LITERAL:
		case VARIABLE:
			Symbol<Object> symbolFound = SemanticAnalyzer.getSymbolFromLiteralOrVariable(lexeme, st);

			//supress future error msgs
			if(symbolFound == null){
				this.errorDetected = true;
			}
			return symbolFound;

		default:
			//error detection
			//there is an attempt to
			if(this.current.getAbstraction() == Abstraction.BOOL_INF_OPERATOR){
				printErrorMsg(lexeme.getLineNo(),"ALL OF and ANY OF cannot be nested.");
				return null;
			}

			if(this.current.getClassifier() == TokenType.BOOL_INF_END){
				printErrorMsg(lexeme.getLineNo(),"missing operand before MKAY.");
				return null;
			}

			printErrorMsg(lexeme.getLineNo(),"'" + this.current.getValue() + "' is not a valid operand.");
			return null;
	}
	}

	/*
	<bool_operation> ::=  <bool_operator> <bool_operand> AN <bool_operand>
	<bool_operand> ::=  <comp_operation> | <bool_operation> | <arith_operation> | <literal> | <variable>
	<bool_operator> ::= BOTH OF | EITHER OF ..
	*/
	private Symbol<Object> getBooleanAnswer(SymbolTable st) {
				printTokenStreamTrace("BOOL EXPRESSION");


				TokenType operator = this.current.getClassifier();
				Symbol<Object> operand1 = null;
				Symbol<Object> operand2 = null;

				moveToNextLexeme();
				printTokenStreamTrace("BOOL OP1");
				//if op1 is an boolean expression //TODO get bool operand method
				operand1 = this.getBoolOperand(this.current, st);

				//if operand 1 == null
				if(operand1 == null){
					return null;
				}

				//if unary operation
				if(operator == TokenType.BOOL_NOT){
					printTokenStreamTrace("BOOL EXPRESSION END");
					return SemanticAnalyzer.performBooleanOperation(operator, operand1);
				}

				//move to next lexeme
				moveToNextLexeme();
				printTokenStreamTrace("BOOL SEP?");

				// if next value is not AN
				if(this.current.getClassifier() != TokenType.EXPR_OP_SEPARATOR){
					printErrorMsg(this.current.getLineNo(),"separator AN not found.");
					return null;
				}

				//move to next lexeme
				moveToNextLexeme();
				// if next
				printTokenStreamTrace("BOOL OP2");
				//if op1 is an boolean expression
				operand2 = this.getBoolOperand(this.current, st);
				//if operand 2 == null
				if(operand2 == null){
					return null;
				}

			printTokenStreamTrace("BOOL EXPRESSION END");

			//perform operation
			return	SemanticAnalyzer.performBooleanOperation(operator,operand1,operand2);
	}

	private Symbol<Object> getBoolOperand(Lexeme lexeme,SymbolTable st){
		switch(lexeme.getAbstraction()){

			case BOOLEAN_OPERATOR:
				return getBooleanAnswer(st);

			case ARITHMETIC_OPERATOR:
			case COMPARISON_OPERATOR:
			case BOOL_INF_OPERATOR:
				return analyzeExpression(st);

			case LITERAL:
			case VARIABLE:
				Symbol<Object> symbolFound = SemanticAnalyzer.getSymbolFromLiteralOrVariable(lexeme, st);

				//supress future error msgs
				if(symbolFound == null){
					this.errorDetected = true;
				}

				return symbolFound;
			default:
				//error detection

				printErrorMsg(lexeme.getLineNo(),"'" + this.current.getValue() + "' is not a valid operand.");
				return null;
		}
	}

	/*
	<comp_operation> ::=  <comp_operator> <comp_operand> AN <comp_operand>
	<comp_operand> ::=  <comp_operation> | <bool_operation> | <arith_operation> | <literal> | <variable>
	<comp_operator> ::= BOTH OF | DIFFRINT ..
	*/
	private Symbol<Object> getComparisonAnswer(SymbolTable st) {
		printTokenStreamTrace("COMP EXPRESSION");


		TokenType operator = this.current.getClassifier();
		Symbol<Object> operand1 = null;
		Symbol<Object> operand2 = null;

		this.moveToNextLexeme();
		printTokenStreamTrace("COMP OP1");
		//op1
		operand1 = this.getCompOperand(this.current,st);
		//if operand 1 == null
		if(operand1 == null){
			return null;
		}

		//move to next lexeme
		this.moveToNextLexeme();
		printTokenStreamTrace("COMP SEP");

		// if next value is not AN
		if(this.current.getClassifier() != TokenType.EXPR_OP_SEPARATOR){
			printErrorMsg(this.current.getLineNo(),"separator AN not found.");
			return null;
		}
		//move to next lexeme
		this.moveToNextLexeme();
		// if next
		printTokenStreamTrace("COMP OP2");

		//if op2
		operand2 = this.getCompOperand(this.current,st);

		//if operand 2 == null
		if(operand2 == null){
			return null;
		}

		printTokenStreamTrace("COMP EXPRESSION END");

		//perform operation
		return	SemanticAnalyzer.performComparisonOperation(operator,operand1,operand2);
	}

	private Symbol<Object> getCompOperand(Lexeme lexeme,SymbolTable st){
		switch(lexeme.getAbstraction()){

			case COMPARISON_OPERATOR:
				return getComparisonAnswer(st);

			case ARITHMETIC_OPERATOR:
			case BOOLEAN_OPERATOR:
			case BOOL_INF_OPERATOR:
				return analyzeExpression(st);

			case LITERAL:
			case VARIABLE:
				Symbol<Object> symbolFound = SemanticAnalyzer.getSymbolFromLiteralOrVariable(lexeme, st);

				//supress future error msgs
				if(symbolFound == null){
					this.errorDetected = true;
				}

				return symbolFound;
			default:
				printErrorMsg(lexeme.getLineNo(),"'" + this.current.getValue() + "' is not a valid operand.");
				return null;
		}
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
		//op1
		operand1 = getArithOperand(this.current, st);
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

		printTokenStreamTrace("ARITH OP2");
		//op2
		operand2 = getArithOperand(this.current, st);

		//if operand 2 == null
		if(operand2 == null){
			return null;
		}

		printTokenStreamTrace("ARITH EXPRESSION END");

		//perform operation
		Symbol<Object> symbolFound =SemanticAnalyzer.performArithmeticOperation(operator,operand1,operand2,this.current.getLineNo());

		//supress future error msgs
		if(symbolFound == null){
			this.errorDetected = true;
		}

		return symbolFound;

	}

	private Symbol<Object> getArithOperand(Lexeme lexeme,SymbolTable st){
		switch(lexeme.getAbstraction()){

			case ARITHMETIC_OPERATOR:
				return getArithmeticAnswer(st);

			case LITERAL:
			case VARIABLE:
				Symbol<Object> symbolFound = SemanticAnalyzer.getSymbolFromLiteralOrVariable(lexeme, st);

				//supress future error msgs
				if(symbolFound == null){
					this.errorDetected = true;
				}

				return symbolFound;

			default:
				printErrorMsg(lexeme.getLineNo(),"'" + this.current.getValue() + "' is not a valid operand.");
				return null;
		}
	}


	//check if previous lexeme and next lexeme are in the same line
	private boolean lexemesAreInSameLine(){
		if(this.current.getLineNo() == this.next.getLineNo()){
			return true;
		}
		return false;
	}

	public void printErrorMsg(int lineNo, String msg) {
		if(!(errorDetected)){
			System.out.println("Error at Line " + lineNo + " : " + msg);
			errorDetected = true;
		}

	}

	//move to next lexeme
	public void moveToNextLexeme(){
		if(this.iter.hasNext()){
			this.current = this.next;
			this.next = this.iter.next();
			//if current lexeme is an unknown keyword.
			if(this.next.getClassifier() == TokenType.UNKNOWN_KEYWORD){
				this.printErrorMsg(this.next.getLineNo(),"'" + this.next.getValue() + "' unknown.");
				this.valid = false;
			}
		}

	}

	//debug
	private void printTokenStreamTrace(String label){
			if(debugMode){
				System.out.println( label +": Current: "+ this.current.getValue() + " Next: " + this.next.getValue());
			}
		}



}
