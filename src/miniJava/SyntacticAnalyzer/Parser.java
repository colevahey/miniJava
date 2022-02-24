package miniJava.SyntacticAnalyzer;

import miniJava.SyntaxError;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

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
	
	// DONE
	private AST parseProgram() {
		ClassDeclList c = new ClassDeclList();
		while (currentToken.tokenType.equals(TokenType.CLASS)) {
			c.add(parseClassDeclaration());
		}
		Package p = new Package(c, null);
		return p;
	}
	
	// DONE
	private ClassDecl parseClassDeclaration() {
		autoAccept();													// class
		String className = parseIdentifier().name;						// id
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		
		accept(TokenType.LCBRACKET);									// {
		while (!currentToken.tokenType.equals(TokenType.RCBRACKET)) {
			Boolean isPrivate = parseVisibility();						// Visibility
			Boolean isStatic = parseAccess();							// Access
			String memberName;
			TypeDenoter type;
			if (currentToken.tokenType.equals(TokenType.VOID)) {
				autoAccept();											// void
				type = new BaseType(TypeKind.VOID, null);
				memberName = parseIdentifier().name;						// id
			} else {
				type = parseType();									// Type
				memberName = parseIdentifier().name;							// id
				if (currentToken.tokenType.equals(TokenType.SEMICOLON)) {
					autoAccept();										// ;
					fdl.add(new FieldDecl(isPrivate, isStatic, type, memberName, null));
					continue;											// END OF FIELD DECLARATION
				}
			}
			switch (currentToken.tokenType) {
			case LPAREN:
				autoAccept();											// ( -> METHOD DECLARATION
				ParameterDeclList pdl = new ParameterDeclList();
				switch (currentToken.tokenType) {
				case RPAREN:
					autoAccept();										// )
					break;
				default:
					pdl = parseParameterList();							// ParameterList?
					accept(TokenType.RPAREN);							// )
					break;
				}
				accept(TokenType.LCBRACKET);							// {
				StatementList sl = new StatementList();
				while (!currentToken.tokenType.equals(TokenType.RCBRACKET)) {
					sl.add(parseStatement());							// Statement*
				}
				autoAccept();											// }
				mdl.add(new MethodDecl(new FieldDecl(isPrivate, isStatic, type, memberName, null), pdl, sl, null));
				break;
			default:
				throw new SyntaxError("Syntax Error: Method or Field Declaration Expected");
			}
		}
		accept(TokenType.RCBRACKET);									// }
		return new ClassDecl(className, fdl, mdl, null);
	}
	
	// DONE
	private Boolean parseVisibility() {
		if (currentToken.tokenType.equals(TokenType.PUBLIC) 
				|| currentToken.tokenType.equals(TokenType.PRIVATE)) {
			Boolean isPrivate = currentToken.tokenType.equals(TokenType.PRIVATE) ? true : false;
			autoAccept();	// public or private
			return isPrivate;
		}
		return false;
	}
	
	// DONE
	private Boolean parseAccess() {
		if (currentToken.tokenType.equals(TokenType.STATIC)) {
			autoAccept();	// static
			return true;
		}
		return false;
	}
	
	// DONE
	private TypeDenoter parseType() {
		switch (currentToken.tokenType) {
		case INT:
			autoAccept();						// int
			switch (currentToken.tokenType) {
			case LBRACKET:
				autoAccept();
				accept(TokenType.RBRACKET);
				return new ArrayType(new BaseType(TypeKind.INT, null), null);
			default:
				return new BaseType(TypeKind.INT, null);
			}
		case IDENTIFIER:
			Identifier i = parseIdentifier();
			switch (currentToken.tokenType) {
			case LBRACKET:
				autoAccept();					// [
				accept(TokenType.RBRACKET);		// ]
				return new ArrayType(new ClassType(i, null), null);
			default:
				return new ClassType(i, null);
			}
		case BOOLEAN:
			autoAccept();						// boolean
			return new BaseType(TypeKind.BOOLEAN, null);
		default:
			throw new SyntaxError("Syntax Error: Invalid Type");
		}
	}
	
	// DONE
	private ParameterDeclList parseParameterList() {
		ParameterDeclList pdl = new ParameterDeclList();
		TypeDenoter type = parseType();					// Type
		String paramName = parseIdentifier().name;		// id
		ParameterDecl pd = new ParameterDecl(type, paramName, null);
		pdl.add(pd);
		
		while (currentToken.tokenType.equals(TokenType.COMMA)) {
			autoAccept();						// ,
			TypeDenoter t = parseType();		// Type
			String pN = parseIdentifier().name;	// id
			ParameterDecl pD = new ParameterDecl(t, pN, null);
			pdl.add(pD);
		}
		
		return pdl;
	}
	
	// TODO
	private void parseArgumentList() {
		parseExpression();		// Expression
		while (currentToken.tokenType.equals(TokenType.COMMA)) {
			autoAccept();		// ,
			parseExpression();	// Expression
		}
	}
	
	// DONE
	private Reference parseReference() {
		switch (currentToken.tokenType) {
		case IDENTIFIER:
			IdRef iR = new IdRef(parseIdentifier(), null); 				// id
			if (currentToken.tokenType.equals(TokenType.DOT)) {
				autoAccept();											// .
				QualRef q = new QualRef(iR, parseIdentifier(), null); 	// id
				while (currentToken.tokenType.equals(TokenType.DOT)) {
					autoAccept();										// .
					q = new QualRef(q, parseIdentifier(), null); 		// id
				}
				return q;
			} else {
				return iR;
			}
		case THIS:
			autoAccept();						// this
			ThisRef tR = new ThisRef(null);
			if (currentToken.tokenType.equals(TokenType.DOT)) {
				autoAccept();											// .
				QualRef q = new QualRef(tR, parseIdentifier(), null); 	// id
				while (currentToken.tokenType.equals(TokenType.DOT)) {
					autoAccept();										// .
					q = new QualRef(q, parseIdentifier(), null); 		// id
				}
				return q;
			} else {
				return tR;
			}
		default:
			throw new SyntaxError("Syntax Error");
		}
	}

	// DONE
	private Statement parseStatement() {
		switch (currentToken.tokenType) {
		case LCBRACKET:
			autoAccept();						// {
			StatementList sl = new StatementList();
			while (!currentToken.tokenType.equals(TokenType.RCBRACKET)) {
				sl.add(parseStatement());		// Statement*
			}
			autoAccept();						// }
			return new BlockStmt(sl, new SourcePosition(0,0));
		case INT:
		case BOOLEAN:
			TypeDenoter tD = parseType();			// Type
			String idName = parseIdentifier().name;	// id
			accept(TokenType.ASSIGNMENT);			// =
			Expression e = parseExpression();		// Expression
			accept(TokenType.SEMICOLON);			// ;
			return new VarDeclStmt(new VarDecl(tD, idName, null), e, null);
		case IDENTIFIER:
			Identifier id = parseIdentifier();	// id
			switch (currentToken.tokenType) {
			case IDENTIFIER:
				tD = new ClassType(id, null);				// Type
				idName = parseIdentifier().name;			// id
				accept(TokenType.ASSIGNMENT);				// =
				e = parseExpression();						// Expression
				accept(TokenType.SEMICOLON);				// ;
				return new VarDeclStmt(new VarDecl(tD, idName, null), e, null);
			case LBRACKET:
				autoAccept();								// [
				switch (currentToken.tokenType) {
				case RBRACKET:
					autoAccept();							// ]
					tD = new ArrayType(new ClassType(id, null), null); // ArrayType
					idName = parseIdentifier().name;		// id
					accept(TokenType.ASSIGNMENT);			// =
					e = parseExpression();					// Expression
					accept(TokenType.SEMICOLON);			// ;
					return new VarDeclStmt(new VarDecl(tD, idName, null), e, null);
				default:
					IdRef iDR = new IdRef(id, null);
					Expression eOuter = parseExpression();	// Expression
					accept(TokenType.RBRACKET);				// ]
					accept(TokenType.ASSIGNMENT);			// =
					e = parseExpression();					// Expression
					accept(TokenType.SEMICOLON);			// ;
					return new IxAssignStmt(iDR, eOuter, e, null);
				}
			case ASSIGNMENT:
				autoAccept();					// =
				e = parseExpression();			// Expression
				accept(TokenType.SEMICOLON);	// ;
				return new AssignStmt(new IdRef(id, null), e, null);
			case LPAREN:
				IdRef iDR = new IdRef(id, null);
				ExprList eL = new ExprList();
				autoAccept();					// (
				switch (currentToken.tokenType) {
				case RPAREN:
					autoAccept();				// )
					break;
				default:
					eL.add(parseExpression());		// Expression
					while (currentToken.tokenType.equals(TokenType.COMMA)) {
						autoAccept();		// ,
						eL.add(parseExpression());	// Expression
					}
					accept(TokenType.RPAREN);	// )
					break;
				}
				accept(TokenType.SEMICOLON);	// ;
				return new CallStmt(iDR, eL, null);
			case DOT:
				autoAccept();											// .
				iDR = new IdRef(id, null);
				QualRef q = new QualRef(iDR, parseIdentifier(), null); 	// id
				while (currentToken.tokenType.equals(TokenType.DOT)) {
					autoAccept();										// .
					q = new QualRef(q, parseIdentifier(), null); 		// id
				}
				switch (currentToken.tokenType) {
				case LBRACKET:
					autoAccept();							// [
					Expression eOuter = parseExpression();	// Expression
					accept(TokenType.RBRACKET);				// ]
					accept(TokenType.ASSIGNMENT);			// =
					e = parseExpression();					// Expression
					accept(TokenType.SEMICOLON);			// ;
					return new IxAssignStmt(q, eOuter, e, null);
				case ASSIGNMENT:
					autoAccept();					// =
					e = parseExpression();				// Expression
					accept(TokenType.SEMICOLON);	// ;
					return new AssignStmt(q, e, null);
				case LPAREN:
					eL = new ExprList();
					autoAccept();					// (
					switch (currentToken.tokenType) {
					case RPAREN:
						autoAccept();				// )
						break;
					default:
						eL.add(parseExpression());		// Expression
						while (currentToken.tokenType.equals(TokenType.COMMA)) {
							autoAccept();		// ,
							eL.add(parseExpression());	// Expression
						}
						accept(TokenType.RPAREN);	// )
						break;
					}
					accept(TokenType.SEMICOLON);	// ;
					return new CallStmt(q, eL, null);
				default:
					throw new SyntaxError("Syntax Error");
				}
			default:
				throw new SyntaxError("Syntax Error");
			}
		case THIS:
			Reference r = parseReference();
			switch (currentToken.tokenType) {
			case ASSIGNMENT:
				autoAccept();					// =
				e = parseExpression();			// Expression
				accept(TokenType.SEMICOLON);	// ;
				return new AssignStmt(r, e, null);
			case LBRACKET:
				autoAccept();							// [
				Expression eOuter = parseExpression();	// Expression
				accept(TokenType.RBRACKET);				// ]
				accept(TokenType.ASSIGNMENT);			// =
				e = parseExpression();					// Expression
				accept(TokenType.SEMICOLON);			// ;
				return new IxAssignStmt(r, eOuter, e, null);
			case LPAREN:
				ExprList eL = new ExprList();
				autoAccept();					// (
				switch (currentToken.tokenType) {
				case RPAREN:
					autoAccept();				// )
					break;
				default:
					eL.add(parseExpression());		// Expression
					while (currentToken.tokenType.equals(TokenType.COMMA)) {
						autoAccept();		// ,
						eL.add(parseExpression());	// Expression
					}
					accept(TokenType.RPAREN);		// )
					break;
				}
				accept(TokenType.SEMICOLON);		// ;
				return new CallStmt(r, eL, null);
			default:
				throw new SyntaxError("SyntaxError");
			}
		case RETURN:
			autoAccept();							// return
			switch (currentToken.tokenType) {
			case SEMICOLON:
				autoAccept();						// ;
				return new ReturnStmt(null, null);
			default:
				e = parseExpression();				// Expression
				accept(TokenType.SEMICOLON);		// ;
				return new ReturnStmt(e, null);
			}
		case IF:
			autoAccept();							// if
			accept(TokenType.LPAREN);				// (
			e = parseExpression();					// Expression
			accept(TokenType.RPAREN);				// )
			Statement s = parseStatement();			// Statement
			switch (currentToken.tokenType) {
			case ELSE:
				autoAccept();						// else
				Statement eS = parseStatement();	// Statement
				return new IfStmt(e, s, eS, null);	// IfStmt with an else
			default:
				return new IfStmt(e, s, null);		// No else
			}
		case WHILE:
			autoAccept();							// while
			accept(TokenType.LPAREN);				// (
			e = parseExpression();					// Expression
			accept(TokenType.RPAREN);				// )
			s = parseStatement();					// Statement
			return new WhileStmt(e, s, null);
		default:
			throw new SyntaxError("Syntax Error");
		}
	}
	
	// TODO
	private Expression parseExpression() {
		switch (currentToken.tokenType) {
		case IDENTIFIER:
		case THIS:
			Reference r = parseReference();
			switch (currentToken.tokenType) {
			case LBRACKET:
				autoAccept();							// [
				Expression e = parseExpression();		// Expression
				accept(TokenType.RBRACKET);				// ]
				return new IxExpr(r, e, null);
			case LPAREN:
				ExprList eL = new ExprList();
				autoAccept();					// (
				switch (currentToken.tokenType) {
				case RPAREN:
					autoAccept();				// )
					break;
				default:
					eL.add(parseExpression());		// Expression
					while (currentToken.tokenType.equals(TokenType.COMMA)) {
						autoAccept();				// ,
						eL.add(parseExpression());	// Expression
					}
					accept(TokenType.RPAREN);		// )
					break;
				}
				accept(TokenType.SEMICOLON);		// ;
				return new CallExpr(r, eL, null);
			default:
				return new RefExpr(r, null);
			}
		
		// TODO: USED STRATIFIED GRAMMAR TO CHEF DIS UP
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
	
	// DONE
	private Identifier parseIdentifier() {
		if (currentToken.tokenType.equals(TokenType.IDENTIFIER)) {
			Identifier id = new Identifier(currentToken);
			currentToken = scanner.scan();	// currentToken++
			return id;
		} else {
			throw new SyntaxError("Syntax Error: Invalid Identifier");
		}
	}
	
	// DONE
	private IntLiteral parseIntLiteral() {
		if (currentToken.tokenType.equals(TokenType.INTLITERAL)) {
			IntLiteral i = new IntLiteral(currentToken);
			currentToken = scanner.scan();	// currentToken++
			return i;
		} else {
			throw new SyntaxError("Syntax Error: Invalid Integer Literal");
		}
	}
	
	// DONE
	private BooleanLiteral parseBoolLiteral() {
		if (currentToken.tokenType.equals(TokenType.TRUE)) {
			BooleanLiteral b = new BooleanLiteral(currentToken);
			currentToken = scanner.scan();	// currentToken++
			return b;
		} else if (currentToken.tokenType.equals(TokenType.FALSE)) {
			BooleanLiteral b = new BooleanLiteral(currentToken);
			currentToken = scanner.scan();
			return b;
		}
		else {
			throw new SyntaxError("Syntax Error: Invalid Integer Literal");
		}
	}
	
	// TODO
	private void parseUnop() {
		if (currentToken.tokenType.equals(TokenType.UNOP)
				|| currentToken.tokenType.equals(TokenType.MINUS)) {
			currentToken = scanner.scan();	// currentToken++
		} else {
			throw new SyntaxError("Syntax Error: Invalid Unary Operator");
		}
	}
	
	// TODO
	private void parseBinop() {
		if (currentToken.tokenType.equals(TokenType.BINOP)
				|| currentToken.tokenType.equals(TokenType.MINUS)) {
			currentToken = scanner.scan();	// currentToken++
		} else {
			throw new SyntaxError("Syntax Error: Invalid Binary Operator");
		}
	}
	
	// TODO
	public void parse() {
		currentToken = scanner.scan();
		parseProgram();
		if (!currentToken.tokenType.equals(TokenType.EOT)) {
			throw new SyntaxError("Syntax Error: Expected EOT");
		}
	}
	
	// TODO PARSE BOOL
}
