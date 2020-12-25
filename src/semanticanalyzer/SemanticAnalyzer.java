package semanticanalyzer;

import java.util.ArrayList;
import java.util.Scanner;

import lexicalanalyzer.Lexeme;
import lexicalanalyzer.TokenType;
import parser.Parser;
import symboltable.Symbol;
import symboltable.SymbolTable;

public final class SemanticAnalyzer {

	private static Scanner sc;

	//parser is given to this class in order for the semantic analyzer to know the current situation in the parser.
	//since there are methods that span multiple lexemes and be able to move the token stream.

	public static String removeStrDelimiters(String quotedString) {
		return quotedString.substring(1, quotedString.length() - 1);
	}

	public static String removeTrailingZeros(String floatString) {

		//determine index of decimal point
		int decPointIndex = floatString.indexOf('.');

		//return a no decimals version of the intstring
		if(decPointIndex == 0){
			return "0";
		}

		return floatString.substring(0, decPointIndex);

	}

	//assign variable  - add/overwrite variables.
	//returns true if the action is valid, returns false if action has semantic error
	public static boolean assignVariable(String varName,Lexeme lexeme,SymbolTable st,Parser parser){

		//add a symbol to create
		Symbol<Object> assignedSymbol = null;

			//symbol from expression
			if(lexeme.isOperationSymbol()){
				assignedSymbol = parser.analyzeExpression(st);
			}

			//symbol  from literal / variable
			else if(lexeme.isLiteral() || lexeme.isVariable()){
				assignedSymbol = getSymbolFromLiteralOrVariable(lexeme,st);
			}

			//add the assigned symbol to the semantic analyzer
			if(assignedSymbol != null){

				st.assignValue(varName, new Symbol<Object>(assignedSymbol.getValue(),assignedSymbol.getDatatype()));
				return true;
			}

			return false;

	}
	//declare variable
	public static boolean declareVariable(String varName, Lexeme lexeme, SymbolTable st, Parser parser) {
		//add a symbol to create
		//TODO turn to method
		Symbol<Object> declaredSymbol = null;

			//symbol from expression
			if(lexeme.isOperationSymbol()){
				declaredSymbol = parser.analyzeExpression(st);
			}

			//symbol  from literal / variable
			else if(lexeme.isLiteral() || lexeme.isVariable()){
				declaredSymbol = getSymbolFromLiteralOrVariable(lexeme,st);
			}

			//add the declared symbol to the semantic analyzer
			if(declaredSymbol != null){

				st.declareValue(varName, new Symbol<Object>(declaredSymbol.getValue(),declaredSymbol.getDatatype()));
				return true;
			}

			return false;
	}

	//declare unititalized variable
	public static void declareUnitializedVariable(String varName,SymbolTable st){
		st.declareValue(varName, new Symbol<Object>("unitialized",TokenType.DATATYPE_NONE));
	}

	//grabs gets specified symbol
	public static Symbol<Object> getSymbolFromSymbolTable(String variableName, SymbolTable st, int lineNo) {
		if(st.inSymbolTable(variableName)){

				Symbol<Object> retrievedSymbol = st.get(variableName);

				if(retrievedSymbol.getDatatype() != TokenType.DATATYPE_NONE){
					return retrievedSymbol;
				}

				printErrorMsg(lineNo,"Variable '" +variableName +"' not initialized.");
				return null;

		}

		printErrorMsg(lineNo,"Variable '" +variableName +"' unknown!.");
		return null;

	}

	//get a symbol
	public static Symbol<Object> getSymbolFromLiteralOrVariable(Lexeme lexeme, SymbolTable st) {

		//on literal
		if(lexeme.isLiteral()){
				switch(lexeme.getClassifier()){

					//on str literal
					case STR_LITERAL:
						String strValue = SemanticAnalyzer.removeStrDelimiters(lexeme.getValue());
						return getSymbolFromString(strValue);

					//on int
					case INT_LITERAL:
						int intValue = Integer.parseInt(lexeme.getValue());
						return new Symbol<Object>(intValue,TokenType.DATATYPE_INT);

					case FLOAT_LITERAL:
						//check if float can be typecast to int
						if (lexeme.getValue().matches("^-?\\d*\\.0+")){
							int trailingIntValue = Integer.parseInt(removeTrailingZeros(lexeme.getValue()));
							return new Symbol<Object>(trailingIntValue,TokenType.DATATYPE_INT);
						}

						double floatValue = Double.parseDouble(lexeme.getValue());
						return new Symbol<Object>(floatValue,TokenType.DATATYPE_FLOAT);

					//on boolean literals
					case BOOL_TRUE:
						return new Symbol<Object>(true,TokenType.DATATYPE_BOOLEAN);
					case BOOL_FALSE:
						return new Symbol<Object>(false,TokenType.DATATYPE_BOOLEAN);
					default:

				}
		}
		//on variable
		if(lexeme.isVariable()){
			return SemanticAnalyzer.getSymbolFromSymbolTable(lexeme.getValue(),st,lexeme.getLineNo());
		}

		printErrorMsg(lexeme.getLineNo(),"expecting a value for declared variable.");
		return null;
	}

	//check patterns on the string value and determine whether they can be automatically typecasted into int or double
	private static Symbol<Object> getSymbolFromString(String strValue) {

		//strValue is an int
		if(strValue.matches("^-?\\d+$")){
			return new Symbol<Object> (Integer.parseInt(strValue),TokenType.DATATYPE_INT);
		}

		//strValue is a double
		if(strValue.matches("^-?\\d*\\.\\d+$")){

			//if double can be typecast to int
			if (strValue.matches("-?\\d*\\.0+$")){
				return new Symbol<Object> (Integer.parseInt(removeTrailingZeros(strValue)),TokenType.DATATYPE_INT);
			}

			return new Symbol<Object> (Double.parseDouble(strValue),TokenType.DATATYPE_FLOAT);
		}

		//if does not fit any int or double it is transformed into string.
		return new Symbol<Object> (strValue,TokenType.DATATYPE_STRING);
	}

	//get user input
	public static boolean getUserInput(String varName, Lexeme lexeme, SymbolTable st) {

		//get input string
		sc = new Scanner(System.in);
		String inputString = sc.next();

		//get input datatype
		Symbol<Object>userInput = getSymbolFromString(inputString);

		//put
		if(userInput != null){
			st.assignValue(varName,userInput);
			return true;
		}

		printErrorMsg(lexeme.getLineNo(),"Input Error.");
		return false;

	}


	public static String getPrintString(Lexeme lexeme, SymbolTable st,Parser parser) {

		//on literal
		if(lexeme.isLiteral()){

			switch(lexeme.getClassifier()){

				//on str literal
				case STR_LITERAL:
					return removeStrDelimiters(lexeme.getValue());

				//on int or float literal
				case INT_LITERAL:
				case FLOAT_LITERAL:
					return lexeme.getValue();

				//on boolean literals
				case BOOL_TRUE:
					return "WIN";
				case BOOL_FALSE:
					return "FAIL";
				default:
					return null;
			}
		}

		//on variable
		if(lexeme.isVariable()){
			Symbol<Object>  assignedSymbol = SemanticAnalyzer.getSymbolFromSymbolTable(lexeme.getValue(),st,lexeme.getLineNo());
			if(assignedSymbol != null){
				return getStringFromSymbol(assignedSymbol);
			}
			return null;
		}

		//on expression
		if(lexeme.isOperationSymbol()){
			Symbol<Object>  resultSymbol = parser.analyzeExpression(st);
			if(resultSymbol != null){
				return getStringFromSymbol(resultSymbol);
			}
			return null;
		}

		return null;
	}

	//get the string version of the symbol value
	private static String getStringFromSymbol(Symbol<Object> symbol) {

		String printString = symbol.getValue().toString();

		//if boolean change "true" and "false" to their lolcode counterparts
		if(symbol.getDatatype() == TokenType.DATATYPE_BOOLEAN){
			if(printString == "true"){
				return "WIN";
			}
			return "FAIL";
		}
		return printString;
	}

	//get boolean from symbol value
	public static boolean getBooleanFromSymbol(Symbol<Object> symbol) {

		if(symbol.getDatatype() != TokenType.DATATYPE_BOOLEAN){
			return true;
		}
		return (boolean)symbol.getValue();
	}

	//get resultant datatype
	private static TokenType getResultDatatype(Symbol<Object> operand1, Symbol<Object> operand2, int lineNo) {

		if(operand1.getDatatype() == TokenType.DATATYPE_INT && operand2.getDatatype() == TokenType.DATATYPE_INT){
			return TokenType.DATATYPE_INT;
		}

		if(operand1.getDatatype() == TokenType.DATATYPE_INT && operand2.getDatatype() == TokenType.DATATYPE_FLOAT ||
				operand1.getDatatype() == TokenType.DATATYPE_FLOAT && operand2.getDatatype() == TokenType.DATATYPE_INT ||
				operand1.getDatatype() == TokenType.DATATYPE_FLOAT && operand2.getDatatype() == TokenType.DATATYPE_FLOAT){
			return TokenType.DATATYPE_FLOAT;
		}

		printErrorMsg(lineNo,"invalid datatype.");
		return TokenType.DATATYPE_NONE;

	}

	//checks if variable is in symbol table or not
	public static boolean variableInSymbolTable(String variableName, SymbolTable st) {
		return st.inSymbolTable(variableName);
	}

	//perform arithmetic operation
	public static Symbol<Object> performArithmeticOperation(TokenType operator, Symbol<Object> operand1,
			Symbol<Object> operand2, int lineNo) {


		//determine the datatype of the result (int or double).
		TokenType resultDatatype = getResultDatatype(operand1,operand2,lineNo);


		if(resultDatatype != TokenType.DATATYPE_NONE){
			switch(operator){
			//addition
			case EXPR_ADD:
				return performAddition(resultDatatype,operand1,operand2);
			//subtraction
			case EXPR_SUB:
				return performSubtraction(resultDatatype,operand1,operand2);
			//multiplication
			case EXPR_MUL:
				return performMultiplication(resultDatatype,operand1,operand2);
			//division
			case EXPR_DIV:
				return performDivision(resultDatatype,operand1,operand2,lineNo);
			//modulo
			case EXPR_MOD:
				return performModulo(resultDatatype,operand1,operand2,lineNo);
			//max
			case EXPR_MAX:
				return performMax(resultDatatype,operand1,operand2);
			//min
			case EXPR_MIN:
				return performMin(resultDatatype,operand1,operand2);

			default:
				break;
			}
		}

		return null;

	}

	private static Symbol<Object> performAddition(TokenType resultDatatype, Symbol<Object> operand1, Symbol<Object> operand2) {

		//both operands are int
		if(resultDatatype == TokenType.DATATYPE_INT){
			return  new Symbol<Object>((int)operand1.getValue() + (int)operand2.getValue(),resultDatatype);
		}
		//only op 1 is int
		if(operand1.getDatatype() == TokenType.DATATYPE_INT){
			return new Symbol<Object>((int)operand1.getValue() + (double)operand2.getValue(),resultDatatype);
		}
		//only op 2 is int
		if(operand2.getDatatype() == TokenType.DATATYPE_INT){
			return new Symbol<Object>((double)operand1.getValue() + (int)operand2.getValue(),resultDatatype);
		}
		//both operands are double
		return new Symbol<Object>((double)operand1.getValue() + (double)operand2.getValue(),resultDatatype);

	}

	private static Symbol<Object> performSubtraction(TokenType resultDatatype, Symbol<Object> operand1,
			Symbol<Object> operand2) {
		//both operands are int
		if(resultDatatype == TokenType.DATATYPE_INT){
			return  new Symbol<Object>((int)operand1.getValue() - (int)operand2.getValue(),resultDatatype);
		}
		//only op 1 is int
		if(operand1.getDatatype() == TokenType.DATATYPE_INT){
			return new Symbol<Object>((int)operand1.getValue() - (double)operand2.getValue(),resultDatatype);
		}
		//only op 2 is int
		if(operand2.getDatatype() == TokenType.DATATYPE_INT){
			return new Symbol<Object>((double)operand1.getValue() - (int)operand2.getValue(),resultDatatype);
		}
		//both operands are double
		return new Symbol<Object>((double)operand1.getValue() - (double)operand2.getValue(),resultDatatype);
	}

	private static Symbol<Object> performMultiplication(TokenType resultDatatype, Symbol<Object> operand1,
			Symbol<Object> operand2) {

		//both operands are int
		if(resultDatatype == TokenType.DATATYPE_INT){
			return  new Symbol<Object>((int)operand1.getValue() * (int)operand2.getValue(),resultDatatype);
		}
		//only op 1 is int
		if(operand1.getDatatype() == TokenType.DATATYPE_INT){
			return new Symbol<Object>((int)operand1.getValue() * (double)operand2.getValue(),resultDatatype);
		}
		//only op 2 is int
		if(operand2.getDatatype() == TokenType.DATATYPE_INT){
			return new Symbol<Object>((double)operand1.getValue() * (int)operand2.getValue(),resultDatatype);
		}
		//both operands are double
		return new Symbol<Object>((double)operand1.getValue() * (double)operand2.getValue(),resultDatatype);

	}

	private static Symbol<Object> performDivision(TokenType resultDatatype, Symbol<Object> operand1,
			Symbol<Object> operand2, int lineNo) {
		try{
			//both operands are int
			if(resultDatatype == TokenType.DATATYPE_INT){
				return  new Symbol<Object>((int)operand1.getValue() / (int)operand2.getValue(),resultDatatype);
			}
			//only op 1 is int
			if(operand1.getDatatype() == TokenType.DATATYPE_INT){
				return new Symbol<Object>((int)operand1.getValue() / (double)operand2.getValue(),resultDatatype);
			}
			//only op 2 is int
			if(operand2.getDatatype() == TokenType.DATATYPE_INT){
				return new Symbol<Object>((double)operand1.getValue() / (int)operand2.getValue(),resultDatatype);
			}
			//both operands are double
			return new Symbol<Object>((double)operand1.getValue() / (double)operand2.getValue(),resultDatatype);


		}catch(ArithmeticException e){
			printErrorMsg(lineNo,"Zero Division.");
			return null;
		}
	}

	private static Symbol<Object> performModulo(TokenType resultDatatype, Symbol<Object> operand1,
			Symbol<Object> operand2,int lineNo) {

		try{
			//both operands are int
			if(resultDatatype == TokenType.DATATYPE_INT){
				return  new Symbol<Object>((int)operand1.getValue() % (int)operand2.getValue(),resultDatatype);
			}
			//only op 1 is int
			if(operand1.getDatatype() == TokenType.DATATYPE_INT){
				return new Symbol<Object>((int)operand1.getValue() % (double)operand2.getValue(),resultDatatype);
			}
			//only op 2 is int
			if(operand2.getDatatype() == TokenType.DATATYPE_INT){
				return new Symbol<Object>((double)operand1.getValue() % (int)operand2.getValue(),resultDatatype);
			}
			//both operands are double
			return new Symbol<Object>((double)operand1.getValue() % (double)operand2.getValue(),resultDatatype);


		}catch(ArithmeticException e){
			printErrorMsg(lineNo,"Zero Division.");
			return null;
		}
	}

	private static Symbol<Object> performMax(TokenType resultDatatype, Symbol<Object> operand1,
			Symbol<Object> operand2) {
		//both operands are int
		if(resultDatatype == TokenType.DATATYPE_INT){
			return new Symbol<Object>(Math.max((int)operand1.getValue(),(int)operand2.getValue()),resultDatatype);
		}

		double typecastedValue = 0;

		//only op 1 is int
		if(operand1.getDatatype() == TokenType.DATATYPE_INT){
			typecastedValue = Double.parseDouble(operand1.getValue().toString());
			return new Symbol<Object>(Math.max(typecastedValue,(double)operand2.getValue()),resultDatatype);
		}
		//only op 2 is int
		if(operand2.getDatatype() == TokenType.DATATYPE_INT){
			typecastedValue = Double.parseDouble(operand2.getValue().toString());
			return new Symbol<Object>(Math.max((double)operand1.getValue(),typecastedValue),resultDatatype);
		}
		//both operands are double
		return new Symbol<Object>(Math.max((double)operand1.getValue(),(double)operand2.getValue()),resultDatatype);

	}

	private static Symbol<Object> performMin(TokenType resultDatatype, Symbol<Object> operand1,
			Symbol<Object> operand2) {
		//both operands are int
		if(resultDatatype == TokenType.DATATYPE_INT){
			return new Symbol<Object>(Math.min((int)operand1.getValue(),(int)operand2.getValue()),resultDatatype);
		}

		double typecastedValue = 0;

		//only op 1 is int
		if(operand1.getDatatype() == TokenType.DATATYPE_INT){
			typecastedValue = Double.parseDouble(operand1.getValue().toString());
			return new Symbol<Object>(Math.min(typecastedValue,(double)operand2.getValue()),resultDatatype);
		}
		//only op 2 is int
		if(operand2.getDatatype() == TokenType.DATATYPE_INT){
			typecastedValue = Double.parseDouble(operand2.getValue().toString());
			return new Symbol<Object>(Math.min((double)operand1.getValue(),typecastedValue),resultDatatype);
		}
		//both operands are double
		return new Symbol<Object>(Math.min((double)operand1.getValue(),(double)operand2.getValue()),resultDatatype);

	}

	//comparison operation
	public static Symbol<Object> performComparisonOperation(TokenType operator, Symbol<Object> operand1,
			Symbol<Object> operand2) {

		//check for equality
		boolean resultBoolean = operand1.getValue().equals(operand2.getValue());

		//if not equal, flip the boolean result
		if(operator == TokenType.COMP_NOT_EQUAL){
			resultBoolean = !(resultBoolean);
		}

		return new Symbol<Object>(resultBoolean,TokenType.DATATYPE_BOOLEAN);
	}

	//boolean operation (binary operands)
	public static Symbol<Object> performBooleanOperation(TokenType operator, Symbol<Object> operand1,
			Symbol<Object> operand2) {

		//holds the boolean of the answer
		boolean resultBoolean = false;
		//holds boolean 1 and boolean2
		boolean bool1 = getBooleanFromSymbol(operand1);
		boolean bool2 = getBooleanFromSymbol(operand2);

		//check for boolean operation
		switch(operator){
		case BOOL_AND:
			resultBoolean = bool1 && bool2;
			break;
		case BOOL_OR:
			resultBoolean = bool1 || bool2;
			break;
		case BOOL_XOR:
			resultBoolean = (bool1 || bool2) && (!(bool1) || !(bool2));
			break;
		default:
		}

		return new Symbol<Object>(resultBoolean,TokenType.DATATYPE_BOOLEAN);
	}

	//boolean operation (unary operands)
	public static Symbol<Object> performBooleanOperation(TokenType operator, Symbol<Object> operand1) {

		//holds the boolean of the answer
		boolean resultBoolean = false;
		//holds boolean 1 and boolean2
		boolean bool1 = getBooleanFromSymbol(operand1);

		//check for boolean operation
		//not
		if(operator == TokenType.BOOL_NOT){
			resultBoolean = !(bool1);
		}

		return new Symbol<Object>(resultBoolean,TokenType.DATATYPE_BOOLEAN);
	}

	public static Symbol<Object> performBoolInfOperation(TokenType operator, ArrayList<Symbol<Object>> operands) {

		//holds the boolean of the answer
		boolean resultBoolean = false;

		//for repeated AND, change start to true
		if(operator == TokenType.BOOL_INF_AND){
			resultBoolean = true;
		}

		for(Symbol<Object> operand: operands){
			//if repeated AND is performed and one operand is false
			if(operator == TokenType.BOOL_INF_AND && getBooleanFromSymbol(operand) == false){
				resultBoolean = false;
				break;
				//if repeated OR is performed and one operand is true
			}else if(operator == TokenType.BOOL_INF_OR && getBooleanFromSymbol(operand) == true){
				resultBoolean = true;
				break;
			}
		}

		return new Symbol<Object>(resultBoolean,TokenType.DATATYPE_BOOLEAN);
	}

	public static void printErrorMsg(int lineNo, String msg) {

		System.out.println("Error at Line " + lineNo + " : " + msg);

	}

}
