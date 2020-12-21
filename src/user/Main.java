package user;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import lexicalanalyzer.Lexeme;
import lexicalanalyzer.LexicalAnalyzer;
import parser.Parser;

public class Main {


	public static String DATASET = "user/input.lol";
	public static String OUTPUT = "src/user/output.txt";
	//DEBUG: check if debug is on or not
	public static boolean debugMode = true;


	public static void main(String[] args) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL URL = cl.getResource(Main.DATASET);
		File inputFile = new File(URL.getPath());
		File outputFile = new File(Main.OUTPUT);



		LexicalAnalyzer la = new LexicalAnalyzer();
		//get the token stream
		 ArrayList<Lexeme> tokenStream = la.createTokenStream(inputFile);

		 if(debugMode){
			 System.out.println("  Lexical Analyzer");
			 la.print();
		 }

		 Parser p = new Parser(tokenStream);
		 //start parsing
		 p.start();
	}

}
