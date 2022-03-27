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
	
	private AST parseProgram() {
		ClassDeclList c = new ClassDeclList();
		while (currentToken.tokenType.equals(TokenType.CLASS)) {
			c.add(parseClassDeclaration());
		}
		Package p = new Package(c, null);
		return p;
	}
	
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
	
	private Boolean parseVisibility() {
		if (currentToken.tokenType.equals(TokenType.PUBLIC) 
				|| currentToken.tokenType.equals(TokenType.PRIVATE)) {
			Boolean isPrivate = currentToken.tokenType.equals(TokenType.PRIVATE) ? true : false;
			autoAccept();	// public or private
			return isPrivate;
		}
		return false;
	}
	
	private Boolean parseAccess() {
		if (currentToken.tokenType.equals(TokenType.STATIC)) {
			autoAccept();	// static
			return true;
		}
		return false;
	}
	
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
	
	private Expression parseExpression() {
		Expression e = parseE();
		return e;
	}
	
	private Expression parseE() {
		Expression e1 = parseD();
		while (currentToken.name.equals("||")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseD();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}
	
	private Expression parseD() {
		Expression e1 = parseC();
		while (currentToken.name.equals("&&")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseC();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}
	
	private Expression parseC() {
		Expression e1 = parseF();
		while (currentToken.name.equals("==") || currentToken.name.equals("!=")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseF();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}
	
	private Expression parseF() {
		Expression e1 = parseR();
		while (currentToken.name.equals("<=") || currentToken.name.equals("<") ||
				currentToken.name.equals(">=") || currentToken.name.equals(">")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseR();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}
	
	private Expression parseR() {
		Expression e1 = parseA();
		while (currentToken.name.equals("+") || currentToken.name.equals("-")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseA();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}
	
	private Expression parseA() {
		Expression e1 = parseM();
		while (currentToken.name.equals("*") || currentToken.name.equals("/")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseM();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}
	
	private Expression parseM() {
		if (currentToken.name.equals("!") || currentToken.name.equals("-")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			return new UnaryExpr(op, parseM(), null);
		} else {
			return parseBaseExpression();
		}
	}
	
	private Expression parseBaseExpression() {
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
				return new CallExpr(r, eL, null);
			default:
				return new RefExpr(r, null);
			}
		case LPAREN:
			autoAccept();						// (
			Expression e = parseExpression();	// Expression
			accept(TokenType.RPAREN);			// )
			return e;
		case INTLITERAL:
			IntLiteral i = parseIntLiteral();	// intlit
			return new LiteralExpr(i, null);
		case TRUE:
		case FALSE:
			BooleanLiteral b = parseBoolLiteral();	// boollit
			return new LiteralExpr(b, null);		// true or false
		case NEW:
			autoAccept();						// new
			switch (currentToken.tokenType) {
			case IDENTIFIER:
				Identifier id = parseIdentifier();	// id
				switch (currentToken.tokenType) {
				case LPAREN:
					autoAccept();				// (
					accept(TokenType.RPAREN);	// )
					return new NewObjectExpr(new ClassType(id, null), null);
				case LBRACKET:
					autoAccept();				// [
					e = parseExpression();		// Expression
					accept(TokenType.RBRACKET);	// ]
					return new NewArrayExpr(new ClassType(id, null), e, null);
				default:
					throw new SyntaxError("Syntax Error: Invalid Declaration");
				}
			case INT:
				autoAccept();					// int
				accept(TokenType.LBRACKET);		// [
				e = parseExpression();			// Expression
				accept(TokenType.RBRACKET);		// ]
				return new NewArrayExpr(new BaseType(TypeKind.INT, null), e, null);
			default:
				throw new SyntaxError("Syntax Error: Invalid Declaration");
			}
		case NULL:
			autoAccept();						// null
			return new NullLiteralExpr(null);
		default:
			throw new SyntaxError("Syntax Error: Invalid Expression");
		}
	}
	
	private Identifier parseIdentifier() {
		if (currentToken.tokenType.equals(TokenType.IDENTIFIER)) {
			Identifier id = new Identifier(currentToken);
			currentToken = scanner.scan();	// currentToken++
			return id;
		} else {
			throw new SyntaxError("Syntax Error: Invalid Identifier");
		}
	}
	
	private IntLiteral parseIntLiteral() {
		if (currentToken.tokenType.equals(TokenType.INTLITERAL)) {
			IntLiteral i = new IntLiteral(currentToken);
			currentToken = scanner.scan();	// currentToken++
			return i;
		} else {
			throw new SyntaxError("Syntax Error: Invalid Integer Literal");
		}
	}
	
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
	
	public AST parse() {
		currentToken = scanner.scan();
		AST p = parseProgram();
		if (!currentToken.tokenType.equals(TokenType.EOT)) {
			throw new SyntaxError("Syntax Error: Expected EOT");
		}
		return p;
	}
}
