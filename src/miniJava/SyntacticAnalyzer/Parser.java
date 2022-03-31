package miniJava.SyntacticAnalyzer;

import miniJava.SyntaxError;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser {
	private Token currentToken;
	private Scanner scanner;
	private SourcePosition start;
	
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
	
	// private AST parseProgram()
	private Package parseProgram() {
		start = currentToken.posn;
		ClassDeclList c = new ClassDeclList();
		while (currentToken.tokenType.equals(TokenType.CLASS)) {
			c.add(parseClassDeclaration());
		}
		Package p = new Package(c, start);
		return p;
	}
	
	private ClassDecl parseClassDeclaration() {
		start = currentToken.posn;
		autoAccept();													// class
		String className = parseIdentifier().name;						// id
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		
		accept(TokenType.LCBRACKET);									// {
		while (!currentToken.tokenType.equals(TokenType.RCBRACKET)) {
			start = currentToken.posn;
			Boolean isPrivate = parseVisibility();						// Visibility
			Boolean isStatic = parseAccess();							// Access
			String memberName;
			TypeDenoter type;
			if (currentToken.tokenType.equals(TokenType.VOID)) {
				autoAccept();											// void
				type = new BaseType(TypeKind.VOID, start);
				memberName = parseIdentifier().name;						// id
			} else {
				type = parseType();									// Type
				memberName = parseIdentifier().name;							// id
				if (currentToken.tokenType.equals(TokenType.SEMICOLON)) {
					autoAccept();										// ;
					fdl.add(new FieldDecl(isPrivate, isStatic, type, memberName, start));
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
				mdl.add(new MethodDecl(new FieldDecl(isPrivate, isStatic, type, memberName, start), pdl, sl, start));
				break;
			default:
				throw new SyntaxError("Syntax Error: Method or Field Declaration Expected");
			}
		}
		accept(TokenType.RCBRACKET);									// }
		return new ClassDecl(className, fdl, mdl, start);
	}
	
	private Boolean parseVisibility() {
		start = currentToken.posn;
		if (currentToken.tokenType.equals(TokenType.PUBLIC) 
				|| currentToken.tokenType.equals(TokenType.PRIVATE)) {
			Boolean isPrivate = currentToken.tokenType.equals(TokenType.PRIVATE) ? true : false;
			autoAccept();	// public or private
			return isPrivate;
		}
		return false;
	}
	
	private Boolean parseAccess() {
		start = currentToken.posn;
		if (currentToken.tokenType.equals(TokenType.STATIC)) {
			autoAccept();	// static
			return true;
		}
		return false;
	}
	
	private TypeDenoter parseType() {
		start = currentToken.posn;
		switch (currentToken.tokenType) {
		case INT:
			autoAccept();						// int
			switch (currentToken.tokenType) {
			case LBRACKET:
				autoAccept();
				accept(TokenType.RBRACKET);
				return new ArrayType(new BaseType(TypeKind.INT, start), start);
			default:
				return new BaseType(TypeKind.INT, start);
			}
		case IDENTIFIER:
			Identifier i = parseIdentifier();
			switch (currentToken.tokenType) {
			case LBRACKET:
				autoAccept();					// [
				accept(TokenType.RBRACKET);		// ]
				return new ArrayType(new ClassType(i, start), start);
			default:
				return new ClassType(i, start);
			}
		case BOOLEAN:
			autoAccept();						// boolean
			return new BaseType(TypeKind.BOOLEAN, start);
		default:
			throw new SyntaxError("Syntax Error: Invalid Type");
		}
	}
	
	private ParameterDeclList parseParameterList() {
		start = currentToken.posn;
		ParameterDeclList pdl = new ParameterDeclList();
		TypeDenoter type = parseType();					// Type
		String paramName = parseIdentifier().name;		// id
		ParameterDecl pd = new ParameterDecl(type, paramName, start);
		pdl.add(pd);
		
		while (currentToken.tokenType.equals(TokenType.COMMA)) {
			autoAccept();						// ,
			TypeDenoter t = parseType();		// Type
			String pN = parseIdentifier().name;	// id
			ParameterDecl pD = new ParameterDecl(t, pN, start);
			pdl.add(pD);
		}
		
		return pdl;
	}
	
	private Reference parseReference() {
		start = currentToken.posn;
		switch (currentToken.tokenType) {
		case IDENTIFIER:
			IdRef iR = new IdRef(parseIdentifier(), start); 				// id
			if (currentToken.tokenType.equals(TokenType.DOT)) {
				autoAccept();											// .
				QualRef q = new QualRef(iR, parseIdentifier(), start); 	// id
				while (currentToken.tokenType.equals(TokenType.DOT)) {
					autoAccept();										// .
					q = new QualRef(q, parseIdentifier(), start); 		// id
				}
				return q;
			} else {
				return iR;
			}
		case THIS:
			autoAccept();						// this
			ThisRef tR = new ThisRef(start);
			if (currentToken.tokenType.equals(TokenType.DOT)) {
				autoAccept();											// .
				QualRef q = new QualRef(tR, parseIdentifier(), start); 	// id
				while (currentToken.tokenType.equals(TokenType.DOT)) {
					autoAccept();										// .
					q = new QualRef(q, parseIdentifier(), start); 		// id
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
		start = currentToken.posn;
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
			return new VarDeclStmt(new VarDecl(tD, idName, start), e, start);
		case IDENTIFIER:
			Identifier id = parseIdentifier();	// id
			switch (currentToken.tokenType) {
			case IDENTIFIER:
				tD = new ClassType(id, start);				// Type
				idName = parseIdentifier().name;			// id
				accept(TokenType.ASSIGNMENT);				// =
				e = parseExpression();						// Expression
				accept(TokenType.SEMICOLON);				// ;
				return new VarDeclStmt(new VarDecl(tD, idName, start), e, start);
			case LBRACKET:
				autoAccept();								// [
				switch (currentToken.tokenType) {
				case RBRACKET:
					autoAccept();							// ]
					tD = new ArrayType(new ClassType(id, start), start); // ArrayType
					idName = parseIdentifier().name;		// id
					accept(TokenType.ASSIGNMENT);			// =
					e = parseExpression();					// Expression
					accept(TokenType.SEMICOLON);			// ;
					return new VarDeclStmt(new VarDecl(tD, idName, start), e, start);
				default:
					IdRef iDR = new IdRef(id, start);
					Expression eOuter = parseExpression();	// Expression
					accept(TokenType.RBRACKET);				// ]
					accept(TokenType.ASSIGNMENT);			// =
					e = parseExpression();					// Expression
					accept(TokenType.SEMICOLON);			// ;
					return new IxAssignStmt(iDR, eOuter, e, start);
				}
			case ASSIGNMENT:
				autoAccept();					// =
				e = parseExpression();			// Expression
				accept(TokenType.SEMICOLON);	// ;
				return new AssignStmt(new IdRef(id, start), e, start);
			case LPAREN:
				IdRef iDR = new IdRef(id, start);
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
				return new CallStmt(iDR, eL, start);
			case DOT:
				autoAccept();											// .
				iDR = new IdRef(id, null);
				QualRef q = new QualRef(iDR, parseIdentifier(), start); 	// id
				while (currentToken.tokenType.equals(TokenType.DOT)) {
					autoAccept();										// .
					q = new QualRef(q, parseIdentifier(), start); 		// id
				}
				switch (currentToken.tokenType) {
				case LBRACKET:
					autoAccept();							// [
					Expression eOuter = parseExpression();	// Expression
					accept(TokenType.RBRACKET);				// ]
					accept(TokenType.ASSIGNMENT);			// =
					e = parseExpression();					// Expression
					accept(TokenType.SEMICOLON);			// ;
					return new IxAssignStmt(q, eOuter, e, start);
				case ASSIGNMENT:
					autoAccept();					// =
					e = parseExpression();				// Expression
					accept(TokenType.SEMICOLON);	// ;
					return new AssignStmt(q, e, start);
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
					return new CallStmt(q, eL, start);
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
				return new AssignStmt(r, e, start);
			case LBRACKET:
				autoAccept();							// [
				Expression eOuter = parseExpression();	// Expression
				accept(TokenType.RBRACKET);				// ]
				accept(TokenType.ASSIGNMENT);			// =
				e = parseExpression();					// Expression
				accept(TokenType.SEMICOLON);			// ;
				return new IxAssignStmt(r, eOuter, e, start);
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
				return new CallStmt(r, eL, start);
			default:
				throw new SyntaxError("SyntaxError");
			}
		case RETURN:
			autoAccept();							// return
			switch (currentToken.tokenType) {
			case SEMICOLON:
				autoAccept();						// ;
				return new ReturnStmt(null, start);
			default:
				e = parseExpression();				// Expression
				accept(TokenType.SEMICOLON);		// ;
				return new ReturnStmt(e, start);
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
				return new IfStmt(e, s, eS, start);	// IfStmt with an else
			default:
				return new IfStmt(e, s, start);		// No else
			}
		case WHILE:
			autoAccept();							// while
			accept(TokenType.LPAREN);				// (
			e = parseExpression();					// Expression
			accept(TokenType.RPAREN);				// )
			s = parseStatement();					// Statement
			return new WhileStmt(e, s, start);
		default:
			throw new SyntaxError("Syntax Error");
		}
	}
	
	private Expression parseExpression() {
		start = currentToken.posn;
		Expression e = parseE();
		return e;
	}
	
	private Expression parseE() {
		start = currentToken.posn;
		Expression e1 = parseD();
		while (currentToken.name.equals("||")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseD();
			e1 = new BinaryExpr(op, e1, e2, start);
		}
		return e1;
	}
	
	private Expression parseD() {
		start = currentToken.posn;
		Expression e1 = parseC();
		while (currentToken.name.equals("&&")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseC();
			e1 = new BinaryExpr(op, e1, e2, start);
		}
		return e1;
	}
	
	private Expression parseC() {
		start = currentToken.posn;
		Expression e1 = parseF();
		while (currentToken.name.equals("==") || currentToken.name.equals("!=")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseF();
			e1 = new BinaryExpr(op, e1, e2, start);
		}
		return e1;
	}
	
	private Expression parseF() {
		start = currentToken.posn;
		Expression e1 = parseR();
		while (currentToken.name.equals("<=") || currentToken.name.equals("<") ||
				currentToken.name.equals(">=") || currentToken.name.equals(">")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseR();
			e1 = new BinaryExpr(op, e1, e2, start);
		}
		return e1;
	}
	
	private Expression parseR() {
		start = currentToken.posn;
		Expression e1 = parseA();
		while (currentToken.name.equals("+") || currentToken.name.equals("-")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseA();
			e1 = new BinaryExpr(op, e1, e2, start);
		}
		return e1;
	}
	
	private Expression parseA() {
		start = currentToken.posn;
		Expression e1 = parseM();
		while (currentToken.name.equals("*") || currentToken.name.equals("/")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			Expression e2 = parseM();
			e1 = new BinaryExpr(op, e1, e2, start);
		}
		return e1;
	}
	
	private Expression parseM() {
		start = currentToken.posn;
		if (currentToken.name.equals("!") || currentToken.name.equals("-")) {
			Operator op = new Operator(currentToken);
			autoAccept();
			return new UnaryExpr(op, parseM(), start);
		} else {
			return parseBaseExpression();
		}
	}
	
	private Expression parseBaseExpression() {
		start = currentToken.posn;
		switch (currentToken.tokenType) {
		case IDENTIFIER:
		case THIS:
			Reference r = parseReference();
			switch (currentToken.tokenType) {
			case LBRACKET:
				autoAccept();							// [
				Expression e = parseExpression();		// Expression
				accept(TokenType.RBRACKET);				// ]
				return new IxExpr(r, e, start);
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
				return new CallExpr(r, eL, start);
			default:
				return new RefExpr(r, start);
			}
		case LPAREN:
			autoAccept();						// (
			Expression e = parseExpression();	// Expression
			accept(TokenType.RPAREN);			// )
			return e;
		case INTLITERAL:
			IntLiteral i = parseIntLiteral();	// intlit
			return new LiteralExpr(i, start);
		case TRUE:
		case FALSE:
			BooleanLiteral b = parseBoolLiteral();	// boollit
			return new LiteralExpr(b, start);		// true or false
		case NEW:
			autoAccept();						// new
			switch (currentToken.tokenType) {
			case IDENTIFIER:
				Identifier id = parseIdentifier();	// id
				switch (currentToken.tokenType) {
				case LPAREN:
					autoAccept();				// (
					accept(TokenType.RPAREN);	// )
					return new NewObjectExpr(new ClassType(id, start), start);
				case LBRACKET:
					autoAccept();				// [
					e = parseExpression();		// Expression
					accept(TokenType.RBRACKET);	// ]
					return new NewArrayExpr(new ClassType(id, start), e, start);
				default:
					throw new SyntaxError("Syntax Error: Invalid Declaration");
				}
			case INT:
				autoAccept();					// int
				accept(TokenType.LBRACKET);		// [
				e = parseExpression();			// Expression
				accept(TokenType.RBRACKET);		// ]
				return new NewArrayExpr(new BaseType(TypeKind.INT, start), e, start);
			default:
				throw new SyntaxError("Syntax Error: Invalid Declaration");
			}
		case NULL:
			autoAccept();						// null
			return new NullLiteralExpr(start);
		default:
			throw new SyntaxError("Syntax Error: Invalid Expression");
		}
	}
	
	private Identifier parseIdentifier() {
		start = currentToken.posn;
		if (currentToken.tokenType.equals(TokenType.IDENTIFIER)) {
			Identifier id = new Identifier(currentToken);
			currentToken = scanner.scan();	// currentToken++
			return id;
		} else {
			throw new SyntaxError("Syntax Error: Invalid Identifier");
		}
	}
	
	private IntLiteral parseIntLiteral() {
		start = currentToken.posn;
		if (currentToken.tokenType.equals(TokenType.INTLITERAL)) {
			IntLiteral i = new IntLiteral(currentToken);
			currentToken = scanner.scan();	// currentToken++
			return i;
		} else {
			throw new SyntaxError("Syntax Error: Invalid Integer Literal");
		}
	}
	
	private BooleanLiteral parseBoolLiteral() {
		start = currentToken.posn;
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
	
	// AST parse() and AST p
	public Package parse() {
		currentToken = scanner.scan();
		Package p = parseProgram();
		if (!currentToken.tokenType.equals(TokenType.EOT)) {
			throw new SyntaxError("Syntax Error: Expected EOT");
		}
		return p;
	}
}
