package symboltable;

import lexicalanalyzer.TokenType;

public class Symbol<T> {
	//TODO: Have the value attribute handle primitive datatypes
	private T value;
	private TokenType datatype;

	public static String UNINITIALIZED = "unitialized";


	public Symbol(T value,TokenType datatype) {
		this.value = value;
		this.datatype = datatype;
	}

	public T getValue(){
		return this.value;
	}
	public void setValue(T value){
		this.value = value;
	}


	public TokenType getDatatype(){
		return this.datatype;
	}

	public void setDatatype(TokenType datatype){
		this.datatype = datatype;
	}

}
