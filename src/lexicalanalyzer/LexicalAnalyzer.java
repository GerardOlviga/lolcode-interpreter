package lexicalanalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import parser.Abstraction;
/*
 Lexical Analyzer

 	This Lexical Analyzer reads the contents of a .lol file and create a stream of lexemes
 (tokenStream) arranged from the first lexeme found up to the last lexeme at the end of the file.
 This tokenStream will then be fed into the parser to check its validity and to construct the symbol table.

 	The Lexical Analyzer also removes all comments so it wont be read by the parser.

 */

public class LexicalAnalyzer {

	/*
	 Token Stream
	 	-An arraylist of lexemes that holds the arranged order of lexemes to be processed by the parser.

	 Lexeme
	 	-has 3 attributes. The Lexeme itself, the Type of Token (Literal, Keyword, Identifier, etc.)
	 	and the line no the lexeme appears in.
	 */

	private ArrayList<Lexeme> tokenStream;

	/*
	 Mode

	  	Because of the differing nature of detecting lexemes in a string (Ignoring whitespaces, Strings needing
	  	to preserve whitespace. Ignoring comments, etc.), modes are implemented in order to
	  	control how lexemes are detected. Modes change based on certain keywords/special characters.

	 */

	enum Mode{
		DEFAULT,			//Default - every whitespace is used to cut the word.
		SINGLE_COMMENT,		//Single Line Comment Mode -  all subsequent text are ignored until end of line
		MULTI_LINE_COMMENT, //Multi Line Comment Mode - all subsequent text from MULTILINE OPEN to MULTILINE CLOSE are ignored.
		STRING				//String mode - from DOUBLE QUOTE to another DOUBLE QUOTE, all text are
	}


	private Mode currentMode; //holds the mode currently in effect.

	private BufferedReader br;

	//Constructor Method
	public LexicalAnalyzer(){
		this.tokenStream = new ArrayList<Lexeme>();
		this.currentMode = Mode.DEFAULT;
	}



	//Creation of the Token Stream
	public ArrayList<Lexeme> createTokenStream(File inputFile){
		try {

		//The Lexical Analyzer reads the files using Buffered Reader

			br = new BufferedReader(
					  new InputStreamReader(
					  new FileInputStream(inputFile), "ISO-8859-1"));



			String line;		//string of the contents of the line

			int lineNo = 1; 	//tracks the line number (to be used for multiple line comparison
								//and detecting the line number where error occurs)

			//read and analyze by line
			while ((line = br.readLine()) != null) {
				if(!(line.isEmpty())){


				//for each line, the contents are analyzed and detected lexemes are added into the arraylist
            	  analyzeLine(line,lineNo);


               		}
				lineNo++;
                }
	      } catch (Exception e) {
	        System.out.println("An error occurred.");
	      }

		//returns the created tokenstream
		return this.tokenStream;

	}

	//print the contents of the created table
	public void print() {
		System.out.format("%-20s%-15s%-15s\n","Type","Lexeme","Line No.");
		for(Lexeme lexeme:this.tokenStream){
			System.out.format("%-20s%-15s%-15s\n",lexeme.getClassifier(),lexeme.getValue(),lexeme.getLineNo());
		}

	}



	/*
	 Analyze Line

	 For each line, detect lexemes to be added into the tokenStream.

	 */
	private void analyzeLine(String line,int lineNo) {

		/*
		 Word

		  		A word is a string that is used to determine a lexeme.
		  	Keywords, Identifiers, Literals are detected as words first.

		  		It will start as an empty string. Over time, as single characters are detected
		  	they are concatenated to the word one by one. In default mode and multi comment mode, once the character
		  	detected is a whitespace, the word is considered finished and is sent to match()
		  	with patterns that determine the token type it has. If successful, a lexeme, with its type
		  	and line no noted, will be created and added into the array list. the word is cleared
		  	and the next character is added to it,until it finishes another word, and the cycle repeats until
		  	the line has ended.

		  		LOLCODE has several keywords with multiple words on it. To detect multiple words,
		  	match() sends back words that are 'lead ins' to the multiple words, hoping that the next word will match
		  	to the keyword detected. For example if 'BIGGR' is detected, instead of classifying it as a variable identifier
		  	it is sent back to analysis to add the next word, if the next detected word is 'OF', it will go to
		  	match() as 'BIGGR OF' ,now detected as a keyword.

		  		In String mode, all subsequent characters are read (even whitespace) until
		  	mode returns to default or line ends. This fills up the word string and once default mode has returned,
		  	the word is automatically classified as string literal.

		  */
		String word = "";

		//add a whitespace at the end of line so the last word will be detected as a word
		//(whitespace is the indicator of the end of the word)
		line = line + ' ';

		//loop through every character in the line.
		//each character will be used to determine if a word has been created
		for(int sym = 0; sym < line.length(); sym++ ){

			//get the character at the specified index
			char foundChar = line.charAt(sym);

			/*
			 Check Mode
				The character can detect what mode is currently in use and it changes how words are counted.
			 two of these modes can be detected by the current character.

			 */
			checkMode(foundChar);


			//default and multiline comment mode: each whitespace marks end of word
			if(this.currentMode == Mode.DEFAULT||this.currentMode == Mode.MULTI_LINE_COMMENT){
				//defines end of word.
				if(String.valueOf(foundChar).matches("\\s")){
					word = match(word,lineNo);
				}else{
					word = word + foundChar;
				}
			//string mode: whitespace is ignored
			//when it returns to default mode, the word created will be matched as string literal.
			}else if(this.currentMode == Mode.STRING){
				word = word + foundChar;
			}

		}
		//turn to single comment mode to default mode after end of line.
		if(this.currentMode == Mode.SINGLE_COMMENT){
			this.currentMode = Mode.DEFAULT;
		}





	}


	/*
	 Check Mode

	 string mode: this mode starts when the character detected is a double quote, and on default mode
	default mode: this mode starts when the character detected is a double quote, and on string mode
	 */
	private void checkMode(char sym){
		String checkerChar = String.valueOf(sym);
		if(checkerChar.matches("\"") && this.currentMode == Mode.DEFAULT){
			this.currentMode = Mode.STRING;
		}else if(checkerChar.matches("\"") && this.currentMode == Mode.STRING){
			this.currentMode = Mode.DEFAULT;
		}
	}

	/*
	 Match

	 Detected words are matched into specific patterns that classify what kind of lexemes
	 will be made.

	 Has 2 modes:

	 	 Default Mode - each detected word is match to each and every pattern and once
	 	 finding a match a lexeme is created. No Match would not create a lexeme (this error will be detected in parsing)

	 	 Multi Line Comment Mode -  No lexemes are created in this mode.
	 	 		It ends once detected word matches to 'TLDR' which signifies the end of the comment

	*/

	private String match(String word,int lineNo) {
		//On default mode
		if(this.currentMode == Mode.DEFAULT){


			//Handle Strings.
			if(word.matches("^\".*\"$")){
			this.tokenStream.add(new Lexeme(word,TokenType.STR_LITERAL,Abstraction.LITERAL,lineNo));
			}

			//Handle Comments
			else if(word.matches("^BTW$")) this.currentMode = Mode.SINGLE_COMMENT;
			else if(word.matches("^OBTW$")) this.currentMode = Mode.MULTI_LINE_COMMENT;

			//Handle Numbers.
			else if(word.matches("^-?\\d+$")) this.tokenStream.add(new Lexeme(word,TokenType.INT_LITERAL,Abstraction.LITERAL,lineNo));
			else if(word.matches("^-?\\d*\\.\\d+$")) this.tokenStream.add(new Lexeme(word,TokenType.FLOAT_LITERAL,Abstraction.LITERAL,lineNo));

			//Handle Boolean
			else if(word.matches("^WIN$")) this.tokenStream.add(new Lexeme(word,TokenType.BOOL_TRUE,Abstraction.LITERAL,lineNo));
			else if(word.matches("^FAIL$")) this.tokenStream.add(new Lexeme(word,TokenType.BOOL_FALSE,Abstraction.LITERAL,lineNo));


			//single line keywords

			//program definition
			else if(word.matches("^HAI$"))this.tokenStream.add(new Lexeme(word,TokenType.PROGRAM_START,Abstraction.PROGRAM,lineNo));
			else if(word.matches("^KTHXBYE$")) this.tokenStream.add(new Lexeme(word,TokenType.PROGRAM_END,Abstraction.PROGRAM,lineNo));
			//I/O
			else if(word.matches("^VISIBLE$")) this.tokenStream.add(new Lexeme(word,TokenType.PRINT,Abstraction.STATEMENT_STARTER,lineNo));
			else if(word.matches("^GIMMEH$")) this.tokenStream.add(new Lexeme(word,TokenType.USER_INPUT,Abstraction.STATEMENT_STARTER,lineNo));
			//Variables
			else if(word.matches("^ITZ$")) this.tokenStream.add(new Lexeme(word,TokenType.VAR_INITIALIZE,Abstraction.KEYWORD,lineNo));
			else if(word.matches("^IT$")) this.tokenStream.add(new Lexeme(word,TokenType.VAR_IMPLICIT,Abstraction.VARIABLE,lineNo));
			else if(word.matches("^R$")) this.tokenStream.add(new Lexeme(word,TokenType.ASSIGNMENT,Abstraction.KEYWORD,lineNo));
			//Data Types
			else if(word.matches("^NOOB$")) this.tokenStream.add(new Lexeme(word,TokenType.DATATYPE_NONE,Abstraction.DATATYPE,lineNo));
			else if(word.matches("^NUMBR$")) this.tokenStream.add(new Lexeme(word,TokenType.DATATYPE_INT,Abstraction.DATATYPE,lineNo));
			else if(word.matches("^NUMBAR$")) this.tokenStream.add(new Lexeme(word,TokenType.DATATYPE_FLOAT,Abstraction.DATATYPE,lineNo));
			else if(word.matches("^YARN$")) this.tokenStream.add(new Lexeme(word,TokenType.DATATYPE_STRING,Abstraction.DATATYPE,lineNo));
			else if(word.matches("^TROOF$")) this.tokenStream.add(new Lexeme(word,TokenType.DATATYPE_BOOLEAN,Abstraction.DATATYPE,lineNo));
			//Operations
			else if(word.matches("^AN$")) this.tokenStream.add(new Lexeme(word,TokenType.EXPR_OP_SEPARATOR,Abstraction.KEYWORD,lineNo));
			else if(word.matches("^NOT$")) this.tokenStream.add(new Lexeme(word,TokenType.BOOL_NOT,Abstraction.LOGIC_OPERATOR,lineNo));
			else if(word.matches("^MKAY$")) this.tokenStream.add(new Lexeme(word,TokenType.BOOL_INF_END,Abstraction.LOGIC_OPERATOR,lineNo));
			else if(word.matches("^SMOOSH$")) this.tokenStream.add(new Lexeme(word,TokenType.STR_CONCAT,Abstraction.KEYWORD,lineNo));
			else if(word.matches("^DIFFRINT$")) this.tokenStream.add(new Lexeme(word,TokenType.COMP_NOT_EQUAL,Abstraction.COMPARISON_OPERATOR,lineNo));
			//Control Flow
			else if(word.matches("^MEBBE$")) this.tokenStream.add(new Lexeme(word,TokenType.CTRL_ELSE,Abstraction.JUMP,lineNo));
			else if(word.matches("^WTF\\?$")) this.tokenStream.add(new Lexeme(word,TokenType.CTRL_SWITCH,Abstraction.JUMP,lineNo));
			else if(word.matches("^OMG$")) this.tokenStream.add(new Lexeme(word,TokenType.CTRL_CASE,Abstraction.JUMP,lineNo));
			else if(word.matches("^OMGWTF$")) this.tokenStream.add(new Lexeme(word,TokenType.CTRL_CASE_DEFAULT,Abstraction.JUMP,lineNo));
			else if(word.matches("^GTFO$")) this.tokenStream.add(new Lexeme(word,TokenType.BREAK,Abstraction.JUMP,lineNo));
			else if(word.matches("^OIC$")) this.tokenStream.add(new Lexeme(word,TokenType.CTRL_END,Abstraction.JUMP,lineNo));
			//TypeCasting
			else if(word.matches("^MAEK$"))this.tokenStream.add(new Lexeme(word,TokenType.TYPECAST,Abstraction.KEYWORD,lineNo));
			else if(word.matches("^A$"))this.tokenStream.add(new Lexeme(word,TokenType.TYPECAST_SEPARATOR,Abstraction.KEYWORD,lineNo));
			//the current word is on the way of being a keyword and is returned to pick up the next word.
			else if (word.matches("^I\\s*$|^I\\s+HAS\\s*$")) return word + " ";
			else if(word.matches("^SUM\\s*$|^DIFF\\s*$|^PRODUKT\\s*$|^QUOSHUNT\\s*$|^MOD\\s*$|^BIGGR\\s*$|^SMALLR\\s*$")) return word + " ";
			else if(word.matches("^BOTH\\s*$|^EITHER\\s*$|^WON\\s*$|^ALL\\s*$|^ANY\\s*$|^O\\s*$|^YA\\s*$|^NO\\s*$")) return word + " ";
			//multi line keywords

			//Variables
			else if(word.matches("^I\\s+HAS\\s+A\\s*$"))this.tokenStream.add(new Lexeme(word,TokenType.VAR_DECLARE,Abstraction.STATEMENT_STARTER,lineNo));
			//Operations
			else if(word.matches("^SUM\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.EXPR_ADD,Abstraction.ARITHMETIC_OPERATOR,lineNo));
			else if(word.matches("^DIFF\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.EXPR_SUB,Abstraction.ARITHMETIC_OPERATOR,lineNo));
			else if(word.matches("^PRODUKT\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.EXPR_MUL,Abstraction.ARITHMETIC_OPERATOR,lineNo));
			else if(word.matches("^QUOSHUNT\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.EXPR_DIV,Abstraction.ARITHMETIC_OPERATOR,lineNo));
			else if(word.matches("^MOD\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.EXPR_MOD,Abstraction.ARITHMETIC_OPERATOR,lineNo));
			else if(word.matches("^BIGGR\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.EXPR_MAX,Abstraction.ARITHMETIC_OPERATOR,lineNo));
			else if(word.matches("^SMALLR\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.EXPR_MIN,Abstraction.ARITHMETIC_OPERATOR,lineNo));
			else if(word.matches("^BOTH\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.BOOL_AND,Abstraction.LOGIC_OPERATOR,lineNo));
			else if(word.matches("^EITHER\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.BOOL_OR,Abstraction.LOGIC_OPERATOR,lineNo));
			else if(word.matches("^WON\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.BOOL_XOR,Abstraction.LOGIC_OPERATOR,lineNo));
			else if(word.matches("^ALL\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.BOOL_INF_AND,Abstraction.LOGIC_OPERATOR,lineNo));
			else if(word.matches("^ANY\\s+OF$"))this.tokenStream.add(new Lexeme(word,TokenType.BOOL_INF_OR,Abstraction.LOGIC_OPERATOR,lineNo));
			else if(word.matches("^BOTH\\s+SAEM$"))this.tokenStream.add(new Lexeme(word,TokenType.COMP_EQUAL,Abstraction.COMPARISON_OPERATOR,lineNo));
			//Control Flow
			else if(word.matches("^O\\s+RLY\\?$"))this.tokenStream.add(new Lexeme(word,TokenType.CTRL_IF_THEN,Abstraction.JUMP,lineNo));
			else if(word.matches("^YA\\s+RLY$"))this.tokenStream.add(new Lexeme(word,TokenType.CTRL_IF,Abstraction.JUMP,lineNo));
			else if(word.matches("^NO\\s+WAI$"))this.tokenStream.add(new Lexeme(word,TokenType.CTRL_ELSE,Abstraction.JUMP,lineNo));

			//variable(Starts with a letter)
			else if(word.matches("^[a-zA-Z_]+[a-zA-Z0-9_]*$")) this.tokenStream.add(new Lexeme(word,TokenType.VAR_IDENTIFIER,Abstraction.VARIABLE,lineNo));

		//on multiline comment mode.
		}else if(this.currentMode == Mode.MULTI_LINE_COMMENT){
			//handle comment
			if(word.matches("^TLDR$")) this.currentMode = Mode.DEFAULT;
		}
		//return empty string after successful use of match
		return "";
	}
}
