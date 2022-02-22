package miniJava.SyntacticAnalyzer;

import miniJava.SyntaxError;

public class Parser {
	private Token currentToken;
	private Scanner scanner;
	
	public Parser(Scanner scanner) {
		this.scanner = scanner;
	}
	
	private void accept(TokenType expected) {
		if (currentToken.tokenType.equals(expected)) {
			currentToken = scanner.scan();					// currentToken++
		} else {
			throw new SyntaxError("Syntax Error: Unexpected Token");
		}
	}
	
	private void autoAccept() {
		currentToken = scanner.scan();						// currentToken++
	}
	
	private void parseProgram() {
		while (currentToken.tokenType.equals(TokenType.CLASS)) {
			parseClassDeclaration();
		}
	}
	
	private void parseClassDeclaration() {
		autoAccept();													// class
		parseIdentifier();												// id
		accept(TokenType.LCBRACKET);									// {
		while (!currentToken.tokenType.equals(TokenType.RCBRACKET)) {
			parseVisibility();											// Visibility
			parseAccess();												// Access
			if (currentToken.tokenType.equals(TokenType.VOID)) {
				autoAccept();											// void
				parseIdentifier();										// id
			} else {
				parseType();											// Type
				parseIdentifier();										// id
				if (currentToken.tokenType.equals(TokenType.SEMICOLON)) {
					autoAccept();										// ;
					continue;											// END OF FIELD DECLARATION
				}
			}
			switch (currentToken.tokenType) {
			case LPAREN:
				autoAccept();											// ( -> METHOD DECLARATION
				switch (currentToken.tokenType) {
				case RPAREN:
					autoAccept();										// )
					break;
				default:
					parseParameterList();								// ParameterList?
					accept(TokenType.RPAREN);							// )
					break;
				}
				accept(TokenType.LCBRACKET);							// {
				while (!currentToken.tokenType.equals(TokenType.RCBRACKET)) {
					parseStatement();									// Statement*
				}
				autoAccept();											// }
				break;
			default:
				throw new SyntaxError("Syntax Error: Method or Field Declaration Expected");
			}
		}
		accept(TokenType.RCBRACKET);									// }
	}
	
	private void parseVisibility() {
		if (currentToken.tokenType.equals(TokenType.PUBLIC) 
				|| currentToken.tokenType.equals(TokenType.PRIVATE)) {
			autoAccept();	// public or private
		}
	}
	
	private void parseAccess() {
		if (currentToken.tokenType.equals(TokenType.STATIC)) {
			autoAccept();	// static
		}
	}
	
	private void parseType() {
		switch (currentToken.tokenType) {
		case INT:
		case IDENTIFIER:
			autoAccept();						// int or id
			switch (currentToken.tokenType) {
			case LBRACKET:
				autoAccept();					// [
				accept(TokenType.RBRACKET);		// ]
				break;
			default:
				break;
			}
			break;
		case BOOLEAN:
			autoAccept();						// boolean
			break;
		default:
			throw new SyntaxError("Syntax Error: Invalid Type");
		}
	}
	
	private void parseParameterList() {
		parseType();			// Type
		parseIdentifier();		// id
		while (currentToken.tokenType.equals(TokenType.COMMA)) {
			autoAccept();		// ,
			parseType();		// Type
			parseIdentifier();	// id
		}
	}
	
	private void parseArgumentList() {
		parseExpression();		// Expression
		while (currentToken.tokenType.equals(TokenType.COMMA)) {
			autoAccept();		// ,
			parseExpression();	// Expression
		}
	}
	
	private void parseReference() {
		switch (currentToken.tokenType) {
		case IDENTIFIER:
			parseIdentifier();		// id
			break;
		case THIS:
			autoAccept();			// this
			break;
		default:
			throw new SyntaxError("Syntax Error");
		}
		while (currentToken.tokenType.equals(TokenType.DOT)) {
			autoAccept();			// .
			parseIdentifier();		// id
		}
	}

	private void parseStatement() {
		switch (currentToken.tokenType) {
		case LCBRACKET:
			autoAccept();						// {
			while (!currentToken.tokenType.equals(TokenType.RCBRACKET)) {
				parseStatement();				// Statement*
			}
			autoAccept();						// }
			break;
		case INT:
		case BOOLEAN:
			parseType();						// Type
			parseIdentifier();					// id
			accept(TokenType.ASSIGNMENT);		// =
			parseExpression();					// Expression
			accept(TokenType.SEMICOLON);		// ;
			break;
		case IDENTIFIER:
			parseIdentifier();					// id
			switch (currentToken.tokenType) {
			case IDENTIFIER:
				parseIdentifier();				// id
				accept(TokenType.ASSIGNMENT);	// =
				parseExpression();				// Expression
				accept(TokenType.SEMICOLON);	// ;
				break;
			case LBRACKET:
				autoAccept();						// [
				switch (currentToken.tokenType) {
				case RBRACKET:
					autoAccept();					// ]
					parseIdentifier();				// id
					accept(TokenType.ASSIGNMENT);	// =
					parseExpression();				// Expression
					accept(TokenType.SEMICOLON);	// ;
					break;
				default:
					parseExpression();				// Expression
					accept(TokenType.RBRACKET);		// ]
					accept(TokenType.ASSIGNMENT);	// =
					parseExpression();				// Expression
					accept(TokenType.SEMICOLON);	// ;
					break;
				}
				break;
			case ASSIGNMENT:
				autoAccept();					// =
				parseExpression();				// Expression
				accept(TokenType.SEMICOLON);	// ;
				break;
			case LPAREN:
				autoAccept();					// (
				switch (currentToken.tokenType) {
				case RPAREN:
					autoAccept();				// )
					break;
				default:
					parseArgumentList();		// ArgumentList?
					accept(TokenType.RPAREN);	// )
					break;
				}
				accept(TokenType.SEMICOLON);	// ;
				break;
			case DOT:
				while (currentToken.tokenType.equals(TokenType.DOT)) {
					autoAccept();					// DOT
					parseIdentifier();				// id
				}
				switch (currentToken.tokenType) {
				case LBRACKET:
					autoAccept();					// [
					parseExpression();				// Expression
					accept(TokenType.RBRACKET);		// ]
					accept(TokenType.ASSIGNMENT);	// =
					parseExpression();				// Expression
					accept(TokenType.SEMICOLON);	// ;
					break;
				case ASSIGNMENT:
					autoAccept();					// =
					parseExpression();				// Expression
					accept(TokenType.SEMICOLON);	// ;
					break;
				case LPAREN:
					autoAccept();					// (
					switch (currentToken.tokenType) {
					case RPAREN:
						autoAccept();				// )
						break;
					default:
						parseArgumentList();		// ArgumentList?
						accept(TokenType.RPAREN);	// )
						break;
					}
					accept(TokenType.SEMICOLON);	// ;
					break;
				default:
					throw new SyntaxError("Syntax Error");
				}
				break;
			default:
				throw new SyntaxError("Syntax Error");
			}
			break;
		case THIS:
			parseReference();
			switch (currentToken.tokenType) {
			case ASSIGNMENT:
				autoAccept();					// =
				parseExpression();				// Expression
				accept(TokenType.SEMICOLON);	// ;
				break;
			case LBRACKET:
				autoAccept();					// [
				parseExpression();				// Expression
				accept(TokenType.RBRACKET);		// ]
				accept(TokenType.ASSIGNMENT);	// =
				parseExpression();				// Expression
				accept(TokenType.SEMICOLON);	// ;
				break;
			case LPAREN:
				autoAccept();					// (
				switch (currentToken.tokenType) {
				case RPAREN:
					autoAccept();				// )
					break;
				default:
					parseArgumentList();		// ArgumentList()
					accept(TokenType.RPAREN);	// )
					break;
				}
				accept(TokenType.SEMICOLON);	// ;
				break;
			default:
				throw new SyntaxError("SyntaxError");
			}
			break;
		case RETURN:
			autoAccept();						// return
			switch (currentToken.tokenType) {
			case SEMICOLON:
				autoAccept();					// ;
				break;
			default:
				parseExpression();				// Expression
				accept(TokenType.SEMICOLON);	// ;
				break;
			}
			break;
		case IF:
			autoAccept();						// if
			accept(TokenType.LPAREN);			// (
			parseExpression();					// Expression
			accept(TokenType.RPAREN);			// )
			parseStatement();					// Statement
			switch (currentToken.tokenType) {
			case ELSE:
				autoAccept();					// else
				parseStatement();				// Statement
				break;
			default:
				break;							// NO ELSE
			}
			break;
		case WHILE:
			autoAccept();						// while
			accept(TokenType.LPAREN);			// (
			parseExpression();					// Expression
			accept(TokenType.RPAREN);			// )
			parseStatement();					// Statement
			break;
		default:
			throw new SyntaxError("Syntax Error");
		}
	}
	
	private void parseExpression() {
		switch (currentToken.tokenType) {
		case IDENTIFIER:
		case THIS:
			parseReference();
			switch (currentToken.tokenType) {
			case LBRACKET:
				autoAccept();					// [
				parseExpression();				// Expression
				accept(TokenType.RBRACKET);		// ]
				break;
			case LPAREN:
				autoAccept();					// (
				switch (currentToken.tokenType) {
				case RPAREN:
					autoAccept();				// )
					break;
				default:
					parseArgumentList();		// ArgumentList?
					accept(TokenType.RPAREN);	// )
					break;
				}
				break;
			default:
				break;
			}
			break;
		case MINUS:
		case UNOP:
			parseUnop();						// unop
			parseExpression();					// Expression
			break;
		case LPAREN:
			autoAccept();						// (
			parseExpression();					// Expression
			accept(TokenType.RPAREN);			// )
			break;
		case INTLITERAL:
			parseIntLiteral();					// intlit
			break;
		case TRUE:
		case FALSE:
			autoAccept();						// true or false
			break;
		case NEW:
			autoAccept();						// new
			switch (currentToken.tokenType) {
			case IDENTIFIER:
				parseIdentifier();				// id
				switch (currentToken.tokenType) {
				case LPAREN:
					autoAccept();				// (
					accept(TokenType.RPAREN);	// )
					break;
				case LBRACKET:
					autoAccept();				// [
					parseExpression();			// Expression
					accept(TokenType.RBRACKET);	// ]
					break;
				default:
					throw new SyntaxError("Syntax Error: Invalid Declaration");
				}
				break;
			case INT:
				autoAccept();					// int
				accept(TokenType.LBRACKET);		// [
				parseExpression();				// Expression
				accept(TokenType.RBRACKET);		// ]
				break;
			default:
				throw new SyntaxError("Syntax Error: Invalid Declaration");
			}
			break;
		default:
			throw new SyntaxError("Syntax Error: Invalid Expression");
		}
		if (currentToken.tokenType.equals(TokenType.BINOP) || currentToken.tokenType.equals(TokenType.MINUS)) {
			parseBinop();						// binop
			parseExpression();					// Expression
		}
	}
	
	private void parseIdentifier() {
		if (currentToken.tokenType.equals(TokenType.IDENTIFIER)) {
			currentToken = scanner.scan();	// currentToken++
		} else {
			throw new SyntaxError("Syntax Error: Invalid Identifier");
		}
	}
	
	private void parseIntLiteral() {
		if (currentToken.tokenType.equals(TokenType.INTLITERAL)) {
			currentToken = scanner.scan();	// currentToken++
		} else {
			throw new SyntaxError("Syntax Error: Invalid Integer Literal");
		}
	}
	
	private void parseUnop() {
		if (currentToken.tokenType.equals(TokenType.UNOP)
				|| currentToken.tokenType.equals(TokenType.MINUS)) {
			currentToken = scanner.scan();	// currentToken++
		} else {
			throw new SyntaxError("Syntax Error: Invalid Unary Operator");
		}
	}
	
	private void parseBinop() {
		if (currentToken.tokenType.equals(TokenType.BINOP)
				|| currentToken.tokenType.equals(TokenType.MINUS)) {
			currentToken = scanner.scan();	// currentToken++
		} else {
			throw new SyntaxError("Syntax Error: Invalid Binary Operator");
		}
	}
	
	public void parse() {
		currentToken = scanner.scan();
		parseProgram();
		if (!currentToken.tokenType.equals(TokenType.EOT)) {
			throw new SyntaxError("Syntax Error: Expected EOT");
		}
	}
}
