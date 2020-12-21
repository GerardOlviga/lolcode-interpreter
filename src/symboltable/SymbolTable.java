package symboltable;

import java.util.HashMap;


public class SymbolTable {

	private HashMap<String,Symbol<Object>> symbolTable;

	public SymbolTable() {
		this.symbolTable = new HashMap<String,Symbol<Object>>();
	}

	public void add(String key,Symbol<Object> s){
		this.symbolTable.put(key, s);
	}

	public  Symbol<Object> get(String key){
		return this.symbolTable.get(key);
	}

	public boolean inSymbolTable(String variablekey){
		if(this.symbolTable.containsKey(variablekey)){
			return true;
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
