package symboltable;

import java.util.HashMap;

import lexicalanalyzer.TokenType;


public class SymbolTable {

	private HashMap<String,Symbol<Object>> symbolTable;

	//parent symbolTable
	private SymbolTable parent;

	public SymbolTable(SymbolTable parent) {
		this.symbolTable = new HashMap<String,Symbol<Object>>();

		//get the parent symbol table
		this.parent = parent;

		//an implicit variable IT is created
		this.symbolTable.put("IT", new Symbol<Object>("unitialized",TokenType.DATATYPE_NONE));
	}

	//assignment - variable must exist.
	public void assignValue(String key,Symbol<Object> s){

		if(this.symbolTable.containsKey(key)){
			this.symbolTable.put(key, s);
		}else if (this.parent != null){
			this.parent.assignValue(key, s);
		}

	}

	//declaration - declare variable in the current symbol table
	public void declareValue(String key,Symbol<Object> s){

		this.symbolTable.put(key, s);
	}

	public  Symbol<Object> get(String key){

		if(this.symbolTable.containsKey(key)){
			return this.symbolTable.get(key);
		}

		if(this.parent != null){
			return this.parent.get(key);
		}

		return null;
	}

	public boolean inSymbolTable(String variablekey){

		if(this.symbolTable.containsKey(variablekey)){
			return true;
		}

		if(this.parent != null){
			return this.parent.inSymbolTable(variablekey);
		}

		return false;
	}

	public void print() {
		System.out.format("%-20s%-20s%-15s\n","Name","Type","Value");
		for(String key:this.symbolTable.keySet()){


			System.out.format("%-20s%-20s%-15s\n",key,this.symbolTable.get(key).getDatatype(),this.symbolTable.get(key).getValue());

		}

	}

}
