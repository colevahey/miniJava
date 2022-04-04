package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class TypeChecking implements Visitor<Object, Object> {
	private Package ast;
	
	public TypeChecking(Package ast) {
		this.ast = ast;
		this.ast.visit(this, null);
	}
	
	public Object visitPackage(Package prog, Object arg) {
		for (ClassDecl cd : prog.classDeclList) {
			cd.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		for (FieldDecl fd : cd.fieldDeclList) {
			fd.visit(this, null);
		}
		for (MethodDecl md : cd.methodDeclList) {
			md.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		fd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, null);
		}
		TypeKind ret = (TypeKind) md.type.visit(this, null);
		boolean returned = false;
		for (Statement st : md.statementList) {
			Object sR = st.visit(this, ret);
			if (sR != null) {
				returned = (boolean) sR || returned;
			}
		}
		if (ret != null) {
			if (!(md.statementList.get(md.statementList.size() - 1) instanceof ReturnStmt)) {
				throw new ContextualAnalysisException("*** line " + md.posn.line + ": Missing final return statement of type " + ret);
			} else if (!returned) {
				throw new ContextualAnalysisException("*** line " + md.posn.line + ": Return statement expected for this method");
			}
		}
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		pd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.type.visit(this, null);
		return decl.type.typeKind;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		if (type.typeKind.equals(TypeKind.INT) || type.typeKind.equals(TypeKind.BOOLEAN) || type.typeKind.equals(TypeKind.NULL)) {
			return type.typeKind;
		}
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		type.className.visit(this, null);
		return TypeKind.CLASS;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		type.eltType.visit(this, null);
		return TypeKind.ARRAY;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		for (Statement s : stmt.sl) {
			s.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		TypeKind vType = (TypeKind) stmt.varDecl.visit(this, null);
		TypeKind eType = (TypeKind) stmt.initExp.visit(this, null);
		if (!vType.equals(eType)) {
			if (!eType.equals(TypeKind.NULL) || vType.equals(TypeKind.INT) || vType.equals(TypeKind.BOOLEAN)) { 
				throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Mismatched types in declaration");
			}
		} else {
			if (vType.equals(TypeKind.CLASS)) {
				if (stmt.initExp instanceof NewObjectExpr) {
					if (!(((ClassType) stmt.varDecl.type).className.name.equals(((NewObjectExpr) stmt.initExp).classtype.className.name))) {
						throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Mismatched class types in declaration");
					}
				} else {
					if (!(((ClassType) stmt.varDecl.type).className.name.equals(((ClassType) ((RefExpr) stmt.initExp).ref.decl.type).className.name))) {
						throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Mismatched class types in declaration");
					}
				}
			} else if (vType.equals(TypeKind.ARRAY)) {
				TypeKind aT = ((ArrayType) stmt.varDecl.type).eltType.typeKind;
				TypeDenoter rT;
				if (stmt.initExp instanceof NewArrayExpr) {
					rT = ((NewArrayExpr) stmt.initExp).eltType;
				} else {
					rT = ((ArrayType) ((RefExpr)stmt.initExp).ref.decl.type).eltType;
				}
				if (!aT.equals(rT.typeKind)) {
					throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Mismatched array types in declaration");
				}
				if (aT.equals(TypeKind.CLASS)) {
					if (!(((ClassType) ((ArrayType) stmt.varDecl.type).eltType).className.name.equals(((ClassType) rT).className.name))) {
						throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Mismatched array types in declaration");
					}
				}
			}
		}
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		TypeKind vType = (TypeKind) stmt.ref.visit(this, null);
		TypeKind eType = (TypeKind) stmt.val.visit(this, null);
		if (!vType.equals(eType)) {
			if (!eType.equals(TypeKind.NULL) || vType.equals(TypeKind.INT) || vType.equals(TypeKind.BOOLEAN)) { 
				throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Mismatched types in assignment");
			}
		} else {
			if (vType.equals(TypeKind.CLASS)) {
				if (stmt.val instanceof NewObjectExpr) {
					if (!(((ClassType) stmt.ref.decl.type).className.name.equals(((NewObjectExpr) stmt.val).classtype.className.name))) {
						throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Mismatched class types in assignment");
					}
				} else {
					if (!(((ClassType) stmt.ref.decl.type).className.name.equals(((ClassType) ((RefExpr) stmt.val).ref.decl.type).className.name))) {
						throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Mismatched class types in assignment");
					}
				}
			} else if (vType.equals(TypeKind.ARRAY)) {
				TypeKind aT = ((ArrayType) stmt.ref.decl.type).eltType.typeKind;
				TypeDenoter rT;
				if (stmt.val instanceof NewArrayExpr) {
					rT = ((NewArrayExpr) stmt.val).eltType;
				} else {
					rT = ((ArrayType) ((RefExpr)stmt.val).ref.decl.type).eltType;
				}
				if (!aT.equals(rT.typeKind)) {
					throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Mismatched array types in assignment");
				}
				if (aT.equals(TypeKind.CLASS)) {
					if (!(((ClassType) ((ArrayType) stmt.ref.decl.type).eltType).className.name.equals(((ClassType) rT).className.name))) {
						throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Mismatched array types in assignment");
					}
				}
			}
		}
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		TypeKind vType = (TypeKind) stmt.ref.visit(this, null);
		TypeKind index = (TypeKind) stmt.ix.visit(this, null);
		TypeKind eType = (TypeKind) stmt.exp.visit(this, null);
		if (!index.equals(TypeKind.INT)) {
			throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Invalid index");
		}
		if (vType.equals(TypeKind.ARRAY)) {
			if (!((ArrayType)stmt.ref.decl.type).eltType.typeKind.equals(eType)) {
				if (!eType.equals(TypeKind.NULL) || stmt.ref.decl.type.typeKind.equals(TypeKind.INT) || stmt.ref.decl.type.typeKind.equals(TypeKind.BOOLEAN)) {
					throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Type inequality in array element assignment");
				}
			}
		} else {
			throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Cannot access index of non-array type");
		}
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		stmt.methodRef.visit(this, null);
		if (stmt.argList.size() > ((MethodDecl) stmt.methodRef.decl).parameterDeclList.size()) {
			throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Too many arguments in method call");
		} else if (stmt.argList.size() < ((MethodDecl) stmt.methodRef.decl).parameterDeclList.size()) {
			throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Missing arguments in method call");
		} else {
			for (int i = 0; i < stmt.argList.size(); i++) {
				TypeKind t = (TypeKind) stmt.argList.get(i).visit(this, null);
				if (!t.equals(((MethodDecl) stmt.methodRef.decl).parameterDeclList.get(i).type.typeKind)) {
					throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Mismatched argument type");
				}
			}
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr != null) {
			TypeKind ret = (TypeKind) stmt.returnExpr.visit(this, null);
			if (!ret.equals(arg) && !ret.equals(TypeKind.NULL)) {
				throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Invalid return type");
			}
		} else {
			if (arg != null) {
				throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Missing argument in return statement");
			}
		}
		return true;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		TypeKind eType = (TypeKind) stmt.cond.visit(this, null);
		if (!eType.equals(TypeKind.BOOLEAN)) {
			if (!eType.equals(TypeKind.NULL)) { 
				throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Invalid conditional statement");
			}
		}
		stmt.thenStmt.visit(this, null);
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		TypeKind eType = (TypeKind) stmt.cond.visit(this, null);
		if (!eType.equals(TypeKind.BOOLEAN)) {
			throw new ContextualAnalysisException("Invalid conditional");
		}
		stmt.body.visit(this, null);
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		Operator uOp = (Operator) expr.operator.visit(this, null);
		TypeKind e = (TypeKind) expr.expr.visit(this, null);
		if (uOp.name.equals("!")) {
			if (e.equals(TypeKind.INT)) {
				throw new ContextualAnalysisException("Binary Expression Expected");
			}
			expr.type = TypeKind.BOOLEAN;
		} else {
			if (e.equals(TypeKind.BOOLEAN)) {
				throw new ContextualAnalysisException("Binary Expression Expected");
			}
			expr.type = TypeKind.INT;
		}
		return expr.type;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		TypeKind leftType = (TypeKind) expr.left.visit(this, null);
		Operator bOp = (Operator) expr.operator.visit(this, null);
		TypeKind rightType = (TypeKind) expr.right.visit(this, null);
		if (bOp.name.equals("||") || bOp.name.equals("&&")) {
			if (!leftType.equals(TypeKind.BOOLEAN) || !rightType.equals(TypeKind.BOOLEAN)) {
				throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Boolean Expression Expected");
			}
			expr.type = TypeKind.BOOLEAN;
		} else if (bOp.name.equals(">") || bOp.name.equals("<") || bOp.name.equals(">=") || bOp.name.equals("<=")) {
			if (!leftType.equals(TypeKind.INT) || !rightType.equals(TypeKind.INT)) {
				throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Integer Expression Expected");
			}
			expr.type = TypeKind.BOOLEAN;
		} else if (!bOp.name.equals("==") && !bOp.name.equals("!=")) {
			if (!leftType.equals(TypeKind.INT) || !rightType.equals(TypeKind.INT)) {
				throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Integer Expression Expected");
			}
			expr.type = TypeKind.INT;
		} else {
			if (!leftType.equals(rightType)) {
				if (!rightType.equals(TypeKind.NULL)) {
					throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Mismatched types in binary operation");
				} else {
					expr.type = TypeKind.NULL;
				}
			} else if (leftType.equals(TypeKind.CLASS)) {
				if (!rightType.equals(TypeKind.NULL)) { 
					if (!((ClassType) ((RefExpr) expr.left).ref.decl.type).className.name.equals(((ClassType) ((RefExpr) expr.right).ref.decl.type).className.name)) {
						throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Mismatched class types in binary operation");
					} else {
						expr.type = TypeKind.NULL;
					}
				}
			// else if (leftType.equals(TypeKind.ARRAY))
			} else {
				expr.type = leftType;
			}
		}
		return expr.type;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		TypeKind r = (TypeKind) expr.ref.visit(this, null);
		expr.type = r;
		return expr.type;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		TypeKind r = (TypeKind) expr.ref.visit(this, null);
		TypeKind i = (TypeKind) expr.ixExpr.visit(this, null);
		if (!r.equals(TypeKind.ARRAY)) {
			throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Cannot index a non-array");
		}
		if (!i.equals(TypeKind.INT)) {
			throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Index must be an integer");
		}
		expr.type = ((ArrayType) expr.ref.decl.type).eltType.typeKind;
		return expr.type;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		expr.functionRef.visit(this, null);
		if (expr.argList.size() > ((MethodDecl) expr.functionRef.decl).parameterDeclList.size()) {
			throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Too many arguments in method call");
		} else if (expr.argList.size() < ((MethodDecl) expr.functionRef.decl).parameterDeclList.size()) {
			throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Missing arguments in method call");
		} else {
			for (int i = 0; i < expr.argList.size(); i++) {
				TypeKind t = (TypeKind) expr.argList.get(i).visit(this, null);
				if (!t.equals(((MethodDecl) expr.functionRef.decl).parameterDeclList.get(i).type.typeKind)) {
					throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Mismatched argument type");
				}
			}
		}
		return expr.functionRef.decl.type.typeKind;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		TypeKind e = (TypeKind) expr.lit.visit(this, null);
		return e;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		TypeKind e = (TypeKind) expr.classtype.visit(this, null);
		expr.type = e;
		return expr.type;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.eltType.visit(this, null);
		TypeKind s = (TypeKind) expr.sizeExpr.visit(this, null);
		if (!s.equals(TypeKind.INT)) {
			throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Invalid array size");
		}
		expr.type = TypeKind.ARRAY;
		return expr.type;
	}

	@Override
	public Object visitNullLiteralExpr(NullLiteralExpr expr, Object arg) {
		return TypeKind.NULL;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		return ref.decl.type.typeKind;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		return ref.decl.type.typeKind;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		return ref.decl.type.typeKind;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		if (id.decl instanceof ClassDecl) {
			return TypeKind.CLASS;
		} else {
			return id.decl.type.typeKind;
		}
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return op;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		return TypeKind.INT;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return TypeKind.BOOLEAN;
	}

}
