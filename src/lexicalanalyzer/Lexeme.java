package lexicalanalyzer;

import parser.Abstraction;

public class Lexeme {
	private String value;			//value/key of the lexeme
	private TokenType classifier;	//token classification
	private Abstraction abstraction;//abstraction type of token
	private int lineNo;				//the line where the lexeme is found

	public Lexeme(String value,TokenType classifier,Abstraction abstraction,int lineNo) {
		this.value = value;
		this.classifier = classifier;
		this.abstraction = abstraction;
		this.lineNo = lineNo;
		//System.out.println( "'"+ this.value+ "'" + " has been classified as " + this.classifier.name());
	}

	public String getValue(){
		return value;
	}

	public int getLineNo(){
		return lineNo;
	}


	public TokenType getClassifier(){
		return classifier;
	}

	public Abstraction getAbstraction(){
		return abstraction;
	}

	//check state of lexeme
	//check if lexeme is start of a statement
		public boolean isStatementStarter(Lexeme next){

			if(this.abstraction == Abstraction.STATEMENT_STARTER){
				return true;
			//special case for Assignment statement
			}else if(this.classifier == TokenType.VAR_IDENTIFIER && next.classifier == TokenType.ASSIGNMENT){
				return true;
			//special case for expression statement
			}else if(this.isOperationSymbol()){
				return true;
			}
			return false;
		}


		//checks if lexeme is a literal or not
		public boolean isLiteral(){


			if(this.abstraction == Abstraction.LITERAL){
				return true;
			}
			return false;

		}

		//checks if lexeme is a variable or not
		public boolean isVariable(){

			if(this.abstraction == Abstraction.VARIABLE){
				return true;
			}
			return false;

		}


		//checks if lexeme is an operation symbol
		public boolean isOperationSymbol(){
			if(this.abstraction == Abstraction.ARITHMETIC_OPERATOR ||
				this.abstraction == Abstraction.COMPARISON_OPERATOR ||
				this.abstraction == Abstraction.LOGIC_OPERATOR){
				return true;
			}
			return false;
		}

		//checks if lexeme is an Arithmetic symbol
		public boolean isArithmeticOperator(){
			if(this.abstraction == Abstraction.ARITHMETIC_OPERATOR){
				return true;
			}
			return false;
		}


		//checks if lexeme is an operation delimiter (AN)
		public boolean isOperationSeparator(){

			if(this.classifier == TokenType.EXPR_OP_SEPARATOR){
				return true;
			}
			return false;
		}

		//check if previous lexeme and next lexeme are in the same line
		public boolean lexemesAreInSameLine(Lexeme other){
			if(this.getLineNo() == other.getLineNo()){
				return true;
			}
			return false;
		}

}
