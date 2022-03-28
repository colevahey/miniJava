package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class Identification implements Visitor<Object, Object> {
	public IDTable table;
	public Identification(Package ast) {
		table = new IDTable();
		ast.visit(this, null);
	}
	
	
	// Program
	
	// **DONE**
	public Object visitPackage(Package prog, Object arg) {
		table.openScope();
		FieldDeclList fdl = new FieldDeclList();
		fdl.add(new FieldDecl(false, true, new ClassType(new Identifier(new Token(TokenType.IDENTIFIER, "_PrintStream", null)), null), "out", null));
		MethodDeclList mdl = new MethodDeclList();
		ParameterDeclList pdl = new ParameterDeclList();
		pdl.add(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null));
		mdl.add(new MethodDecl(new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), "println", null), pdl, new StatementList(), null));
		table.enter(
				"System", 
				new ClassDecl(
						"System", 
						fdl, 
						new MethodDeclList(), 
						null
						)
				);
		table.enter(
				"_PrintStream", 
				new ClassDecl(
						"_PrintStream", 
						new FieldDeclList(), 
						mdl, 
						null
						)
				);
		table.enter(
				"String", 
				new ClassDecl(
						"String", 
						new FieldDeclList(), 
						new MethodDeclList(), 
						null
						)
				);
		
		
		for (ClassDecl cd: prog.classDeclList) {
			table.enter(cd.name, cd);
		}
		for (ClassDecl cd: prog.classDeclList) {
			cd.visit(this, null);
		}
		table.closeScope();
		return null;
	}
	
	//**DONE**
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		table.openScope();
		for (FieldDecl fd: cd.fieldDeclList) {
			table.enter(fd.name, fd);
		}
		for (MethodDecl md: cd.methodDeclList) {
			table.enter(md.name, md);
		}
		
		for (FieldDecl fd: cd.fieldDeclList) {
			fd.visit(this, null);
		}
		for (MethodDecl md: cd.methodDeclList) {
			md.visit(this, null);
		}
		table.closeScope();
		
		return null;
	}
	
	// **DONE**
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		fd.type.visit(this, null);
		return null;
	}
	
	// **DONE**
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		md.type.visit(this, null);
		
		table.openScope();
		for (ParameterDecl pd: md.parameterDeclList) {
			pd.visit(this, null);
		}
		
		table.openScope();
		for (Statement st: md.statementList) {
			st.visit(this, null);
		}
		table.closeScope();
		
		table.closeScope();
		return null;
	}

	// **DONE**
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		pd.type.visit(this, null);
		table.enter(pd.name, pd);
		return null;
	}

	// **DONE**
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.type.visit(this, null);
		table.enter(decl.name, decl);
		return null;
	}

	// **DONE**
	public Object visitBaseType(BaseType type, Object arg) {
		if (type.typeKind.equals(TypeKind.INT)) {
			type.type = TypeKind.INT;
		} else if (type.typeKind.equals(TypeKind.BOOLEAN)) {
			type.type = TypeKind.BOOLEAN;
		}
		return type.type;
	}

	// **DONE**
	public Object visitClassType(ClassType type, Object arg) {
		Declaration d = (Declaration) type.className.visit(this, null);
		if (d != null) {
			return d;
		} else {
			throw new RuntimeException("Invalid type");
		}
	}

	public Object visitArrayType(ArrayType type, Object arg) {
		type.eltType.visit(this, null);
		return null;
	}

	
	// Statements
	
	//**DONE**
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		table.openScope();
		for (Statement s: stmt.sl) {
			s.visit(this, null);
		}
		table.closeScope();
		return null;
	}

	//**DONE**
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		TypeKind vType = (TypeKind) stmt.varDecl.visit(this, null);
		TypeKind eType = (TypeKind) stmt.initExp.visit(this, null);
		if (!vType.equals(eType)) {
			throw new RuntimeException("Type inequality in a variable instantiation statment!!");
		}
		return null;
	}

	//**DONE**
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		TypeKind vType = (TypeKind) stmt.ref.visit(this, null);
		TypeKind eType = (TypeKind) stmt.val.visit(this, null);
		if (!vType.equals(eType)) {
			throw new RuntimeException("Type inequality in an assignment statment!!");
		}
		return null;
	}

	// **DONE** - WRONG: vType will be ArrayType instead of the elType needed
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		TypeKind vType = (TypeKind) stmt.ref.visit(this, null);
		TypeKind index = (TypeKind) stmt.ix.visit(this, null);
		TypeKind eType = (TypeKind) stmt.exp.visit(this, null);
		if (!index.equals(TypeKind.INT)) {
			throw new RuntimeException("Invalid Index Type");
		}
		if (!vType.equals(eType)) {
			throw new RuntimeException("Type inequality in array element assignment");
		}
		return null;
	}

	public Object visitCallStmt(CallStmt stmt, Object arg) {
		// Need to check types here? Matching param types
		stmt.methodRef.visit(this, null);
		for (Expression e: stmt.argList) {
			e.visit(this, null);
		}
		return null;
	}

	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		MethodDecl m = (MethodDecl) arg;
		TypeKind t = (TypeKind) stmt.returnExpr.visit(this, null);
		if (!m.type.type.equals(t)) {
			throw new RuntimeException("Return type invalid");
		}
		return null;
	}

	public Object visitIfStmt(IfStmt stmt, Object arg) {
		TypeKind eType = (TypeKind) stmt.cond.visit(this, null);
		if (!eType.equals(TypeKind.BOOLEAN)) {
			throw new RuntimeException("Invalid conditional");
		}
		stmt.thenStmt.visit(this, null);
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, null);
		}
		return null;
	}

	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		TypeKind eType = (TypeKind) stmt.cond.visit(this, null);
		if (!eType.equals(TypeKind.BOOLEAN)) {
			throw new RuntimeException("Invalid conditional");
		}
		stmt.body.visit(this, null);
		return null;
	}
	
	
	// Expressions
	
	// **DONE**
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		Operator uOp = (Operator) expr.operator.visit(this, null);
		TypeKind e = (TypeKind) expr.expr.visit(this, null);
		if (uOp.name.equals("!")) {
			if (e.equals(TypeKind.INT)) {
				throw new RuntimeException("Binary Expression Expected");
			}
			expr.type = TypeKind.BOOLEAN;
		} else {
			if (e.equals(TypeKind.BOOLEAN)) {
				throw new RuntimeException("Binary Expression Expected");
			}
			expr.type = TypeKind.INT;
		}
		return expr.type;
	}

	// **DONE**
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		TypeKind leftType = (TypeKind) expr.left.visit(this, null);
		Operator bOp = (Operator) expr.operator.visit(this, null);
		TypeKind rightType = (TypeKind) expr.right.visit(this, null);
		if (bOp.name.equals("||") || bOp.name.equals("&&")) {
			if (!leftType.equals(TypeKind.BOOLEAN) || !rightType.equals(TypeKind.BOOLEAN)) {
				throw new RuntimeException("Boolean Expression Expected");
			}
			expr.type = TypeKind.BOOLEAN;
		} else if (!bOp.name.equals("==") && !bOp.name.equals("!=")) {
			if (!leftType.equals(TypeKind.INT) || !rightType.equals(TypeKind.INT)) {
				throw new RuntimeException("Integer Expression Expected");
			}
			expr.type = TypeKind.INT;
		} else {
			if (!leftType.equals(rightType)) {
				throw new RuntimeException("Mismatched types in binary operation");
			}
			expr.type = leftType;
		}
		return expr.type;
	}

	// **DONE**
	public Object visitRefExpr(RefExpr expr, Object arg) {
		TypeKind r = (TypeKind) expr.ref.visit(this, null);
		expr.type = r;
		return expr.type;
	}

	public Object visitIxExpr(IxExpr expr, Object arg) {
		TypeKind r = (TypeKind) expr.ref.visit(this, null);
		TypeKind i = (TypeKind) expr.ixExpr.visit(this, null);
		if (!r.equals(TypeKind.ARRAY)) {
			throw new RuntimeException("Cannot index a non-array");
		}
		if (!i.equals(TypeKind.INT)) {
			throw new RuntimeException("Index must be an integer");
		}
		// NEED A TYPE HERE... ElTYPE but how to access it?????
		return null;
	}
	
	public Object visitCallExpr(CallExpr expr, Object arg) {
		expr.functionRef.visit(this, null);
		for (Expression e: expr.argList) {
			e.visit(this, null);
		}
		return null;
	}

	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		expr.lit.visit(this, null);
		return null;
	}

	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		expr.classtype.visit(this, null);
		return null;
	}

	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.eltType.visit(this, null);
		expr.sizeExpr.visit(this, null);
		return null;
	}

	public Object visitNullLiteralExpr(NullLiteralExpr expr, Object arg) {
		return null;
	}

	
	// Values and Names
	
	
	public Object visitThisRef(ThisRef ref, Object arg) {
		
		return null;
	}

	public Object visitIdRef(IdRef ref, Object arg) {
		return null;
	}
	
	public Object visitQRef(QualRef ref, Object arg) {
		Declaration iD = (Declaration) ref.id.visit(this, null);
		if (iD.type instanceof ClassType)
		
		
		
		return null;
	}

	
	// Identifiers
	
	// **DONE**
	public Object visitIdentifier(Identifier id, Object arg) {
		id.decl = table.retrieve(id.name);
		return id.decl;
	}

	
	// Operators
	
	// **DONE**
	public Object visitOperator(Operator op, Object arg) {
		return op;
	}

	// **DONE**?
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		return TokenType.INT;
	}

	// **DONE**?
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return TokenType.BOOLEAN;
	}
}
