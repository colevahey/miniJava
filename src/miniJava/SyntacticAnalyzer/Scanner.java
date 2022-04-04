package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

import miniJava.SyntaxError;

public class Scanner {
	private InputStream inputStream;
	private char currentChar;						// FIRST SOURCE CHARACTER
	private TokenType currentTokenType;
	private int currentLine;
	private StringBuffer currentName = new StringBuffer("");
	
	public Scanner(InputStream inputStream) {
		this.currentLine = 1;
		this.inputStream = inputStream;
		getChar();
	}
	
	private void getChar() {
		try {
			currentChar = (char) inputStream.read();
		} catch (IOException e) {
			throw new SyntaxError("File Input Error");
		}
	}
	
	private void takeIt() {
		currentName.append(currentChar);
		getChar();		// NEXT CHARACTER
	}
	
	private boolean isDigit(char c) {
		return '0' <= c && c <= '9';
	}
	
	private boolean isLetter(char c) {
		return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
	}
	
	private boolean isGraphic(char c) {
		return (32 <= c && c <= 126);
	}
	
	private TokenType scanToken() {
		if (('a' <= currentChar && currentChar <= 'z') || ('A' <= currentChar && currentChar <= 'Z')) {
			takeIt();				// Letter
			while (isLetter(currentChar) || isDigit(currentChar) || currentChar == '_') {
				takeIt();			// Letter or Digit *
			}
			return TokenType.IDENTIFIER;
		} else if ('0' <= currentChar && currentChar <= '9') {
			takeIt();				// Digit
			while (isDigit(currentChar)) {
				takeIt();			// Digit*
			}
			return TokenType.INTLITERAL;
		} else if (currentChar == '=') {
			takeIt();
			if (currentChar == '=') {
				takeIt();
				return TokenType.BINOP;
			}
			return TokenType.ASSIGNMENT;
		} else if (currentChar == '+' || currentChar == '*' || currentChar == '/') {
			takeIt();
			return TokenType.BINOP;
		} else if (currentChar == '-') {
			takeIt();
			return TokenType.MINUS;
		} else if (currentChar == '!') {
			takeIt();
			if (currentChar == '=') {
				takeIt();
				return TokenType.BINOP;
			}
			return TokenType.UNOP;
		} else if (currentChar == '>' || currentChar == '<') {
			takeIt();
			if (currentChar == '=') {
				takeIt();
			}
			return TokenType.BINOP;
		} else if (currentChar == '&') {
			takeIt();
			if (currentChar == '&') {
				takeIt();
				return TokenType.BINOP;
			}
			throw new SyntaxError("Lexical Error");
		} else if (currentChar == '|') {
			takeIt();
			if (currentChar == '|') {
				takeIt();
				return TokenType.BINOP;
			}
			throw new SyntaxError("Lexical Error");
		} else if (currentChar == ';') {
			takeIt();
			return TokenType.SEMICOLON;
		} else if (currentChar == '(') {
			takeIt();
			return TokenType.LPAREN;
		} else if (currentChar == ')') {
			takeIt();
			return TokenType.RPAREN;
		} else if (currentChar == '[') {
			takeIt();
			return TokenType.LBRACKET;
		} else if (currentChar == ']') {
			takeIt();
			return TokenType.RBRACKET;
		} else if (currentChar == '{') {
			takeIt();
			return TokenType.LCBRACKET;
		} else if (currentChar == '}') {
			takeIt();
			return TokenType.RCBRACKET;
		} else if (currentChar == ',') {
			takeIt();
			return TokenType.COMMA;
		} else if (currentChar == '.') {
			takeIt();
			return TokenType.DOT;
		} else if (currentChar == 65535) {					// Code for eot scan (textbook has \000)
			return TokenType.EOT;
		} else {
			System.out.println(currentChar);
			throw new SyntaxError("Lexical Error");
		}
	}
	
	private void scanSeparator() {
		switch (currentChar) {
		case '/':												// Following a /
			takeIt();
			while (isGraphic(currentChar)) {
				takeIt();
			}
			if (currentChar == '\n' || currentChar == '\r') {	// If a newline or just carriage return
				takeIt();
				currentLine++;
			}
			break;
		case '*':												// Following a /
			takeIt();
			while (true) {
				if (currentChar == 65535) {
					throw new SyntaxError("Lexical Error");
				} else if (currentChar == '*') {
					takeIt();
					if (currentChar == '/') {
						takeIt();
						break;
					}
				} else {
					if (currentChar == '\n' || currentChar == '\r') {
						currentLine++;
					}
					takeIt();
				}
			}
			break;
		case '\n':
		case '\r':
			currentLine++;
		case ' ':
		case '\t':
			takeIt();
			break;
		}
	}
	
	public Token scan() {
		while (currentChar == ' ' || currentChar == '\n' || currentChar == '\t' || currentChar == '\r') {
			scanSeparator();
		}
		if (currentChar == '/') {
			takeIt();
			if (currentChar == '/' || currentChar == '*') {
				scanSeparator();
				return scan();
			} else {
				currentName = new StringBuffer("/");
				currentTokenType = TokenType.BINOP;
			}
		} else {
			currentName = new StringBuffer("");
			currentTokenType = scanToken();
		}
		return new Token(currentTokenType, currentName.toString(), new SourcePosition(currentLine, 0));
	}
}
