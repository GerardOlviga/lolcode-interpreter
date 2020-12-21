package semanticanalyzer;

import lexicalanalyzer.Lexeme;
import lexicalanalyzer.TokenType;
import parser.Parser;
import symboltable.Symbol;
import symboltable.SymbolTable;

public final class SemanticAnalyzer {

	//parser is given to this class in order for the semantic analyzer to know the current situation in the parser.
	//since there are methods that span multiple lexemes and be able to move the token stream.

	public static String removeStrDelimiters(String quotedString) {
		return quotedString.substring(1, quotedString.length() - 1);
	}

	//assign variable  - add/overwrite variables.
	//returns true if the action is valid, returns false if action has semantic error
	public static boolean assignVariable(String varName,Lexeme lexeme,SymbolTable st,Parser parser){

		//add a symbol to create
		Symbol<Object> assignedSymbol = null;

			//symbol from expression
			if(lexeme.isOperationSymbol()){
				assignedSymbol = parser.analyzeExpression(st);
			//symbol  from literal / variable
			}else if(lexeme.isLiteral() || lexeme.getClassifier() == TokenType.VAR_IDENTIFIER){
				assignedSymbol = getSymbolFromLiteralOrVariable(lexeme,st);
			}

			//add the assigned symbol to the semantic analyzer
			if(assignedSymbol != null){
				st.add(varName, new Symbol<Object>(assignedSymbol.getValue(),assignedSymbol.getDatatype()));
				return true;
			}
			else{
				return false;
			}
	}

	public static Symbol<Object> getSymbolFromLiteralOrVariable(Lexeme lexeme, SymbolTable st) {

		//as variable.
		if(lexeme.getClassifier() == TokenType.VAR_IDENTIFIER){
			return SemanticAnalyzer.getSymbolFromSymbolTable(lexeme.getValue(),st,lexeme.getLineNo());
		}

		//as Int
		else if(lexeme.getClassifier() == TokenType.INT_LITERAL){
			int intValue = Integer.parseInt(lexeme.getValue());
			return new Symbol<Object>(intValue,TokenType.DATATYPE_INT);

		//as Float
		}else if(lexeme.getClassifier() == TokenType.FLOAT_LITERAL){
			double floatValue = Double.parseDouble(lexeme.getValue());
			return new Symbol<Object>(floatValue,TokenType.DATATYPE_FLOAT);

		// as Boolean
		}else if(lexeme.getClassifier() == TokenType.BOOL_TRUE || lexeme.getClassifier() == TokenType.BOOL_FALSE){
			boolean boolValue = true;
			if(lexeme.getClassifier() == TokenType.BOOL_FALSE){
				boolValue = false;
			}
			return new Symbol<Object>(boolValue,TokenType.DATATYPE_BOOLEAN);

		// as String
		}else if(lexeme.getClassifier() == TokenType.STR_LITERAL){
			String strValue = SemanticAnalyzer.removeStrDelimiters(lexeme.getValue());
			return new Symbol<Object>(strValue,TokenType.DATATYPE_STRING);
		}else{
			Parser.printErrorMsg(lexeme.getLineNo(),"expecting a value for declared variable.");
			return null;
		}

	}

	//declare unititalized variable
	public static void declareUnitializedVariable(String varName,SymbolTable st){
		st.add(varName, new Symbol<Object>("unitialized",TokenType.DATATYPE_NONE));
	}

	//checks if variable is in symbol table or not
	public static boolean variableInSymbolTable(String variableName, SymbolTable st) {
		return st.inSymbolTable(variableName);
	}

	//grabs gets specified symbol
	public static Symbol<Object> getSymbolFromSymbolTable(String variableName, SymbolTable st, int lineNo) {
		if(st.inSymbolTable(variableName)){
				Symbol<Object> retrievedSymbol = st.get(variableName);
				if(retrievedSymbol.getDatatype() != TokenType.DATATYPE_NONE){
					return retrievedSymbol;
				}else{
					Parser.printErrorMsg(lineNo,"Variable '" +variableName +"' not initialized.");
					return null;
				}
		}else{
			Parser.printErrorMsg(lineNo,"Variable '" +variableName +"' unknown.");
			return null;
		}

	}

	//
	public static String getPrintString(Lexeme lexeme, SymbolTable st,Parser parser) {
		//remove quotations from str literal values
		if(lexeme.getClassifier() == TokenType.STR_LITERAL){
			return removeStrDelimiters(lexeme.getValue());

		}else if(lexeme.getClassifier() == TokenType.INT_LITERAL || lexeme.getClassifier() == TokenType.FLOAT_LITERAL){
			return lexeme.getValue();
		}
		//print true or false on boolean literals
		else if(lexeme.getClassifier() == TokenType.BOOL_TRUE){
			return "true";
		}else if(lexeme.getClassifier() == TokenType.BOOL_FALSE){
			return "false";
		//print the value of the variable.
		}else if(lexeme.getClassifier() == TokenType.VAR_IDENTIFIER){
			Symbol<Object>  assignedSymbol = SemanticAnalyzer.getSymbolFromSymbolTable(lexeme.getValue(),st,lexeme.getLineNo());
			if(assignedSymbol != null){
				return assignedSymbol.getValue().toString();
			}else{
				return null;
			}
		//print the value of the expression
		}else if(lexeme.isOperationSymbol()){
			Symbol<Object>  resultSymbol = parser.analyzeExpression(st);
			if(resultSymbol != null){
				return resultSymbol.getValue().toString();
			}else{
				return null;
			}
		//print
		}
		else{
			return null;
		}

	}

	//perform arithmetic operation
	public static Symbol<Object> performArithmeticOperation(TokenType operator, Symbol<Object> operand1,
			Symbol<Object> operand2, int lineNo) {


		//determine the datatype of the result (int or double).
		TokenType resultDatatype = getResultDatatype(operand1,operand2,lineNo);

		if(resultDatatype != TokenType.DATATYPE_NONE){
			//Addition
			if(operator == TokenType.EXPR_ADD){

				if(resultDatatype == TokenType.DATATYPE_INT){
					return  new Symbol<Object>((int)operand1.getValue() + (int)operand2.getValue(),resultDatatype);
				}else{
					if(operand1.getDatatype() == TokenType.DATATYPE_INT){
						return new Symbol<Object>((int)operand1.getValue() + (double)operand2.getValue(),resultDatatype);
					}else if(operand2.getDatatype() == TokenType.DATATYPE_INT){
						return new Symbol<Object>((double)operand1.getValue() + (int)operand2.getValue(),resultDatatype);
					}else{
						return new Symbol<Object>((double)operand1.getValue() + (double)operand2.getValue(),resultDatatype);
					}

				}
			}
			//Subtraction
			else if(operator == TokenType.EXPR_SUB){
				if(resultDatatype == TokenType.DATATYPE_INT){
					return  new Symbol<Object>((int)operand1.getValue() - (int)operand2.getValue(),resultDatatype);
				}else{
					if(operand1.getDatatype() == TokenType.DATATYPE_INT){
						return new Symbol<Object>((int)operand1.getValue() - (double)operand2.getValue(),resultDatatype);
					}else if(operand2.getDatatype() == TokenType.DATATYPE_INT){
						return new Symbol<Object>((double)operand1.getValue() - (int)operand2.getValue(),resultDatatype);
					}else{
						return new Symbol<Object>((double)operand1.getValue() - (double)operand2.getValue(),resultDatatype);
					}

				}
			}
			//Multiplication
			else if(operator == TokenType.EXPR_MUL){
				if(resultDatatype == TokenType.DATATYPE_INT){
					return  new Symbol<Object>((int)operand1.getValue() * (int)operand2.getValue(),resultDatatype);
				}else{
					if(operand1.getDatatype() == TokenType.DATATYPE_INT){
						return new Symbol<Object>((int)operand1.getValue() * (double)operand2.getValue(),resultDatatype);
					}else if(operand2.getDatatype() == TokenType.DATATYPE_INT){
						return new Symbol<Object>((double)operand1.getValue() * (int)operand2.getValue(),resultDatatype);
					}else{
						return new Symbol<Object>((double)operand1.getValue() * (double)operand2.getValue(),resultDatatype);
					}


				}
			}
			//Division
			else if(operator == TokenType.EXPR_DIV){

				try{
					if(resultDatatype == TokenType.DATATYPE_INT){
						return  new Symbol<Object>((int)operand1.getValue() / (int)operand2.getValue(),resultDatatype);
					}else{
						if(operand1.getDatatype() == TokenType.DATATYPE_INT){
							return new Symbol<Object>((int)operand1.getValue() / (double)operand2.getValue(),resultDatatype);
						}else if(operand2.getDatatype() == TokenType.DATATYPE_INT){
							return new Symbol<Object>((double)operand1.getValue() / (int)operand2.getValue(),resultDatatype);
						}else{
							return new Symbol<Object>((double)operand1.getValue() / (double)operand2.getValue(),resultDatatype);
						}


					}
				}catch(ArithmeticException e){
					Parser.printErrorMsg(lineNo,"Zero Division.");
					return null;
				}

			}
			//Modulo
			else if(operator == TokenType.EXPR_MOD){
				try{
					if(resultDatatype == TokenType.DATATYPE_INT){
						return  new Symbol<Object>((int)operand1.getValue() % (int)operand2.getValue(),resultDatatype);
					}else{
						if(operand1.getDatatype() == TokenType.DATATYPE_INT){
							return new Symbol<Object>((int)operand1.getValue() % (double)operand2.getValue(),resultDatatype);
						}else if(operand2.getDatatype() == TokenType.DATATYPE_INT){
							return new Symbol<Object>((double)operand1.getValue() % (int)operand2.getValue(),resultDatatype);
						}else{
							return new Symbol<Object>((double)operand1.getValue() % (double)operand2.getValue(),resultDatatype);
						}


					}
				}catch(ArithmeticException e){
					Parser.printErrorMsg(lineNo,"Zero Division.");
					return null;
				}

			}
			//Max
			else if(operator == TokenType.EXPR_MAX){

				if(resultDatatype == TokenType.DATATYPE_INT){
					return new Symbol<Object>(Math.max((int)operand1.getValue(),(int)operand2.getValue()),resultDatatype);
				}else{
					double typecastedValue = 0;
					if(operand1.getDatatype() == TokenType.DATATYPE_INT){
						typecastedValue = Double.parseDouble(operand1.getValue().toString());
						return new Symbol<Object>(Math.max(typecastedValue,(double)operand2.getValue()),resultDatatype);
					}else if(operand2.getDatatype() == TokenType.DATATYPE_INT){
						typecastedValue = Double.parseDouble(operand2.getValue().toString());
						return new Symbol<Object>(Math.max((double)operand1.getValue(),typecastedValue),resultDatatype);
					}else{
						return new Symbol<Object>(Math.max((double)operand1.getValue(),(double)operand2.getValue()),resultDatatype);
					}

				}
			}

			//Min
			else if(operator == TokenType.EXPR_MIN){

				if(resultDatatype == TokenType.DATATYPE_INT){
					return new Symbol<Object>(Math.min((int)operand1.getValue(),(int)operand2.getValue()),resultDatatype);
				}else{
					double typecastedValue = 0;
					if(operand1.getDatatype() == TokenType.DATATYPE_INT){
						typecastedValue = Double.parseDouble(operand1.getValue().toString());
						return new Symbol<Object>(Math.min(typecastedValue,(double)operand2.getValue()),resultDatatype);
					}else if(operand2.getDatatype() == TokenType.DATATYPE_INT){
						typecastedValue = Double.parseDouble(operand2.getValue().toString());
						return new Symbol<Object>(Math.min((double)operand1.getValue(),typecastedValue),resultDatatype);
					}else{
						return new Symbol<Object>(Math.min((double)operand1.getValue(),(double)operand2.getValue()),resultDatatype);
					}

				}
			}
		}
		return null;
	}

	//get resultant datatype
	private static TokenType getResultDatatype(Symbol<Object> operand1, Symbol<Object> operand2, int lineNo) {
		if(operand1.getDatatype() == TokenType.DATATYPE_INT && operand2.getDatatype() == TokenType.DATATYPE_INT){
			return TokenType.DATATYPE_INT;
		}else if(operand1.getDatatype() == TokenType.DATATYPE_INT && operand2.getDatatype() == TokenType.DATATYPE_FLOAT ||
				operand1.getDatatype() == TokenType.DATATYPE_FLOAT && operand2.getDatatype() == TokenType.DATATYPE_INT ||
				operand1.getDatatype() == TokenType.DATATYPE_FLOAT && operand2.getDatatype() == TokenType.DATATYPE_FLOAT){
			return TokenType.DATATYPE_FLOAT;
		}else{
			Parser.printErrorMsg(lineNo,"invalid datatype.");
			return TokenType.DATATYPE_NONE;
		}

	}

}
