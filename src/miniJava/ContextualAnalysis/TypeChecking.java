package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class TypeChecking implements Visitor<Object, Object> {
	private Package ast;
	private int typeCheckingErrors;
	
	public TypeChecking(Package ast) {
		this.ast = ast;
		this.typeCheckingErrors = 0;
		this.ast.visit(this, null);
		if (this.typeCheckingErrors > 0) {
			throw new ContextualAnalysisException(null);
		}
	}
	
	public void catchError(String e) {
		System.out.println(e);
		typeCheckingErrors++;
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
			Object sR = st.visit(this, md.type);
			if (sR != null) {
				returned = (boolean) sR || returned;
			}
		}
		if (ret != null) {
			if (md.statementList.size() == 0) {
				catchError("*** line " + md.posn.line + ": (Type Checking) Missing return statement of type " + ret);
			} else if (!(md.statementList.get(md.statementList.size() - 1) instanceof ReturnStmt)) {
				catchError("*** line " + md.posn.line + ": (Type Checking) Missing final return statement of type " + ret);
			} else if (!returned) {
				catchError("*** line " + md.posn.line + ": (Type Checking) Return statement expected for this method");
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
			s.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		TypeKind vType = (TypeKind) stmt.varDecl.visit(this, null);
		TypeKind eType = (TypeKind) stmt.initExp.visit(this, stmt.varDecl);
		if (!vType.equals(eType)) {
			if (!eType.equals(TypeKind.NULL) || vType.equals(TypeKind.INT) || vType.equals(TypeKind.BOOLEAN)) { 
				catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched types in declaration");
			}
		} else {
			if (vType.equals(TypeKind.CLASS)) {
				if (stmt.initExp instanceof NewObjectExpr) {
					if (!(((ClassType) stmt.varDecl.type).className.name.equals(((NewObjectExpr) stmt.initExp).classtype.className.name))) {
						catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in declaration");
					}
				} else if (stmt.initExp instanceof CallExpr) {
					if (!((ClassType) stmt.varDecl.type).className.name.equals(((ClassType) ((CallExpr) stmt.initExp).functionRef.decl.type).className.name)) {
						catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in declaration");
					}
				} else if (stmt.initExp instanceof IxExpr) {
					if (!((ClassType) stmt.varDecl.type).className.name.equals(((ClassType) ((ArrayType) ((IxExpr) stmt.initExp).ref.decl.type).eltType).className.name)) {
						catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in declaration");
					}
				} else {
					if (!(((ClassType) stmt.varDecl.type).className.name.equals(((ClassType) ((RefExpr) stmt.initExp).ref.decl.type).className.name))) {
						catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in declaration");
					}
				}
			} else if (vType.equals(TypeKind.ARRAY)) {
				TypeKind aT = ((ArrayType) stmt.varDecl.type).eltType.typeKind;
				TypeDenoter rT;
				if (stmt.initExp instanceof NewArrayExpr) {
					rT = ((NewArrayExpr) stmt.initExp).eltType;
				} else if (stmt.initExp instanceof CallExpr) {
					rT = ((ArrayType) ((CallExpr) stmt.initExp).functionRef.decl.type).eltType;
				} else if ((stmt.initExp instanceof IxExpr)) {
					rT = ((ArrayType) ((IxExpr) stmt.initExp).ref.decl.type).eltType;
				} else {
					rT = ((ArrayType) ((RefExpr)stmt.initExp).ref.decl.type).eltType;
				}
				if (!aT.equals(rT.typeKind)) {
					catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched array types in declaration");
				}
				if (aT.equals(TypeKind.CLASS)) {
					if (!(((ClassType) ((ArrayType) stmt.varDecl.type).eltType).className.name.equals(((ClassType) rT).className.name))) {
						catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched array types in declaration");
					}
				}
			}
		}
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		TypeKind vType = (TypeKind) stmt.ref.visit(this, null);
		TypeKind eType = null;
		if (stmt.ref.decl != null) {
			eType = (TypeKind) stmt.val.visit(this, stmt.ref.decl.type);
		} else {
			throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": (Type Checking) Cannot assign a read-only attribute");
		}
		if (!vType.equals(eType)) {
			if (!eType.equals(TypeKind.NULL) || vType.equals(TypeKind.INT) || vType.equals(TypeKind.BOOLEAN)) { 
				catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched types in assignment");
			}
		} else {
			if (vType.equals(TypeKind.CLASS)) {
				if (stmt.val instanceof NewObjectExpr) {
					if (!(((ClassType) stmt.ref.decl.type).className.name.equals(((NewObjectExpr) stmt.val).classtype.className.name))) {
						catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in assignment");
					}
				} else if (stmt.val instanceof CallExpr) {
					if (!((ClassType) stmt.ref.decl.type).className.name.equals(((ClassType) ((CallExpr) stmt.val).functionRef.decl.type).className.name)) {
						catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in assignment");
					}
				} else if (stmt.val instanceof IxExpr) {
					if (!((ClassType) stmt.ref.decl.type).className.name.equals(((ClassType) ((ArrayType) ((IxExpr) stmt.val).ref.decl.type).eltType).className.name)) {
						catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in assignmet");
					}
				} else {
					if (!(((ClassType) stmt.ref.decl.type).className.name.equals(((ClassType) ((RefExpr) stmt.val).ref.decl.type).className.name))) {
						catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in assignment");
					}
				}
			} else if (vType.equals(TypeKind.ARRAY)) {
				TypeKind aT = ((ArrayType) stmt.ref.decl.type).eltType.typeKind;
				TypeDenoter rT;
				if (stmt.val instanceof NewArrayExpr) {
					rT = ((NewArrayExpr) stmt.val).eltType;
				} else if (stmt.val instanceof CallExpr) {
					rT = ((ArrayType) ((CallExpr) stmt.val).functionRef.decl.type).eltType;
				} else if (stmt.val instanceof IxExpr) {
					rT = ((ArrayType) ((IxExpr) stmt.val).ref.decl.type).eltType;
				} else {
					rT = ((ArrayType) ((RefExpr)stmt.val).ref.decl.type).eltType;
				}
				if (!aT.equals(rT.typeKind)) {
					catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched array types in assignment");
				}
				if (aT.equals(TypeKind.CLASS)) {
					if (!(((ClassType) ((ArrayType) stmt.ref.decl.type).eltType).className.name.equals(((ClassType) rT).className.name))) {
						catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched array types in assignment");
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
		TypeKind eType = (TypeKind) stmt.exp.visit(this, ((ArrayType) stmt.ref.decl.type).eltType);
		if (!index.equals(TypeKind.INT)) {
			catchError("*** line " + stmt.posn.line + ": (Type Checking) Invalid index");
		}
		if (vType.equals(TypeKind.ARRAY)) {
			if (!((ArrayType)stmt.ref.decl.type).eltType.typeKind.equals(eType)) {
				if (!eType.equals(TypeKind.NULL) || stmt.ref.decl.type.typeKind.equals(TypeKind.INT) || stmt.ref.decl.type.typeKind.equals(TypeKind.BOOLEAN)) {
					catchError("*** line " + stmt.posn.line + ": (Type Checking) Type inequality in array element assignment");
				}
			}
		} else {
			catchError("*** line " + stmt.posn.line + ": (Type Checking) Cannot access index of non-array type");
		}
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		stmt.methodRef.visit(this, null);
		if (stmt.argList.size() > ((MethodDecl) stmt.methodRef.decl).parameterDeclList.size()) {
			catchError("*** line " + stmt.posn.line + ": (Type Checking) Too many arguments in method call");
		} else if (stmt.argList.size() < ((MethodDecl) stmt.methodRef.decl).parameterDeclList.size()) {
			catchError("*** line " + stmt.posn.line + ": (Type Checking) Missing arguments in method call");
		} else {
			for (int i = 0; i < stmt.argList.size(); i++) {
				TypeDenoter expected = ((MethodDecl) stmt.methodRef.decl).parameterDeclList.get(i).type;
				TypeKind t = (TypeKind) stmt.argList.get(i).visit(this, null);
				if (!t.equals(expected.typeKind)) {
					catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched argument types");
				} else {
					if (expected.typeKind.equals(TypeKind.CLASS)) {
						if (stmt.argList.get(i) instanceof NewObjectExpr) {
							if (!(((ClassType) expected).className.name.equals(((NewObjectExpr) stmt.argList.get(i)).classtype.className.name))) {
								catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in method call arguments");
							}
						} else if (stmt.argList.get(i) instanceof CallExpr) {
							if (!((ClassType) expected).className.name.equals(((ClassType) ((CallExpr) stmt.argList.get(i)).functionRef.decl.type).className.name)) {
								catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in method call arguments");
							}
						} else if (stmt.argList.get(i) instanceof IxExpr) {
							if (!((ClassType) expected).className.name.equals(((ClassType) ((ArrayType) ((IxExpr) stmt.argList.get(i)).ref.decl.type).eltType).className.name)) {
								catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in method call arguments");
							}
						} else {
							if (!(((ClassType) expected).className.name.equals(((ClassType) ((RefExpr) stmt.argList.get(i)).ref.decl.type).className.name))) {
								catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in method call arguments");
							}
						}
					} else if (expected.typeKind.equals(TypeKind.ARRAY)) {
						TypeKind aT = ((ArrayType) expected).eltType.typeKind;
						TypeDenoter rT;
						if (stmt.argList.get(i) instanceof NewArrayExpr) {
							rT = ((NewArrayExpr) stmt.argList.get(i)).eltType;
						} else if (stmt.argList.get(i) instanceof CallExpr) {
							rT = ((ArrayType) ((CallExpr) stmt.argList.get(i)).functionRef.decl.type).eltType;
						} else if (stmt.argList.get(i) instanceof IxExpr) {
							rT = ((ArrayType) ((IxExpr) stmt.argList.get(i)).ref.decl.type).eltType;
						}else {
							rT = ((ArrayType) ((RefExpr)stmt.argList.get(i)).ref.decl.type).eltType;
						}
						if (!aT.equals(rT.typeKind)) {
							catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched array types in method call arguments");
						} else if (aT.equals(TypeKind.CLASS)) {
							if (!(((ClassType) ((ArrayType) expected).eltType).className.name.equals(((ClassType) rT).className.name))) {
								catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched array types in method call arguments");
							}
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr != null) {
			TypeKind ret = (TypeKind) stmt.returnExpr.visit(this, (TypeDenoter) arg);
			TypeKind argType = ((TypeDenoter) arg).typeKind;
			if (!ret.equals(argType)) {
				if (!ret.equals(TypeKind.NULL)) {
					catchError("*** line " + stmt.posn.line + ": (Type Checking) Invalid return type");
					return true;
				} else if (!argType.equals(TypeKind.ARRAY) && !argType.equals(TypeKind.CLASS)) {
					catchError("*** line " + stmt.posn.line + ": (Type Checking) Cannot return null for base type");
					return true;
				}
			} else {
				if (argType.equals(TypeKind.CLASS)) {
					if (stmt.returnExpr instanceof NewObjectExpr) {
						if (!(((ClassType) ((TypeDenoter) arg)).className.name.equals(((NewObjectExpr) stmt.returnExpr).classtype.className.name))) {
							catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in return statement");
							return true;
						}
					} else if (stmt.returnExpr instanceof CallExpr) {
						if (!((ClassType) ((TypeDenoter) arg)).className.name.equals(((ClassType) ((CallExpr) stmt.returnExpr).functionRef.decl.type).className.name)) {
							catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in return statement");
						}
						return true;
					} else if (stmt.returnExpr instanceof IxExpr) {
						if (!((ClassType) ((TypeDenoter) arg)).className.name.equals(((ClassType) ((ArrayType) ((IxExpr) stmt.returnExpr).ref.decl.type).eltType).className.name)) {
							catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in return statement");
						}
						return true;
					} else {
						if (!(((ClassType) ((TypeDenoter) arg)).className.name.equals(((ClassType) ((RefExpr) stmt.returnExpr).ref.decl.type).className.name))) {
							catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched class types in return statement");
							return true;
						}
					}
				} else if (argType.equals(TypeKind.ARRAY)) {
					TypeKind aT = ((ArrayType) ((TypeDenoter) arg)).eltType.typeKind;
					TypeDenoter rT;
					if (stmt.returnExpr instanceof NewArrayExpr) {
						rT = ((NewArrayExpr) stmt.returnExpr).eltType;
					} else if (stmt.returnExpr instanceof CallExpr) {
						rT = ((ArrayType) ((CallExpr) stmt.returnExpr).functionRef.decl.type).eltType;
					} else if (stmt.returnExpr instanceof IxExpr) {
						rT = ((ArrayType) ((IxExpr) stmt.returnExpr).ref.decl.type).eltType;
					} else {
						rT = ((ArrayType) ((RefExpr)stmt.returnExpr).ref.decl.type).eltType;
					}
					if (!aT.equals(rT.typeKind)) {
						catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched array types in return statement");
						return true;
					} else if (aT.equals(TypeKind.CLASS)) {
						if (!(((ClassType) ((ArrayType) ((TypeDenoter) arg)).eltType).className.name.equals(((ClassType) rT).className.name))) {
							catchError("*** line " + stmt.posn.line + ": (Type Checking) Mismatched array types in return statement");
							return true;
						}
					}
				}
			}
		} else if (arg != null && !((TypeDenoter) arg).typeKind.equals(TypeKind.VOID)) {
			catchError("*** line " + stmt.posn.line + ": (Type Checking) Missing argument in return statement");
			return true;
		}
		return true;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		TypeKind eType = (TypeKind) stmt.cond.visit(this, new BaseType(TypeKind.BOOLEAN, null));
		if (!eType.equals(TypeKind.BOOLEAN)) {
			if (!eType.equals(TypeKind.NULL)) { 
				catchError("*** line " + stmt.posn.line + ": (Type Checking) Invalid conditional statement");
			}
		}
		
		stmt.thenStmt.visit(this, arg);
		if (stmt.thenStmt instanceof VarDeclStmt) {
			catchError("*** line " + stmt.thenStmt.posn.line + ": (Type Checking) Solitary variable declaration statement not permitted here");
		}
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, arg);
			if (stmt.elseStmt instanceof VarDeclStmt) {
				catchError("*** line " + stmt.elseStmt.posn.line + ": (Type Checking) Solitary variable declaration statement not permitted here");
			}
		}
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		TypeKind eType = (TypeKind) stmt.cond.visit(this, new BaseType(TypeKind.BOOLEAN, null));
		if (!eType.equals(TypeKind.BOOLEAN)) {
			catchError("*** line " + stmt.posn.line + ": (Type Checking) Invalid conditional");
		}
		stmt.body.visit(this, arg);
		if (stmt.body instanceof VarDeclStmt) {
			catchError("*** line " + stmt.body.posn.line + ": (Type Checking) Solitary variable declaration statement not permitted here");
		}
		return null;
	}
	
	public Object visitForStmt(ForStmt stmt, Object arg) {
		if (stmt.init != null) stmt.init.visit(this, null);
		if (stmt.cond != null) {
			TypeKind eType = (TypeKind) stmt.cond.visit(this, new BaseType(TypeKind.BOOLEAN, null));
			if (!eType.equals(TypeKind.BOOLEAN)) {
				catchError("*** line " + stmt.posn.line + ": (Type Checking) Invalid conditional");
			}
		}
		if (stmt.assignment != null) stmt.assignment.visit(this, null);
		stmt.body.visit(this, arg);
		if (stmt.body.sl.size() == 1 && stmt.body.sl.get(0) instanceof VarDeclStmt) {
			catchError("*** line " + stmt.body.sl.get(0).posn.line + ": (Type Checking) Solitary variable declaration statement not permitted here");
		}
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		Operator uOp = (Operator) expr.operator.visit(this, null);
		TypeKind e = (TypeKind) expr.expr.visit(this, arg);
		if (uOp.name.equals("!")) {
			if (e.equals(TypeKind.INT)) {
				catchError("*** line " + expr.posn.line + ": (Type Checking) Boolean expression expected");
			}
			expr.type = TypeKind.BOOLEAN;
		} else {
			if (e.equals(TypeKind.BOOLEAN)) {
				catchError("*** line " + expr.posn.line + ": (Type Checking) Integer Expression Expected");
			}
			expr.type = TypeKind.INT;
		}
		return expr.type;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		TypeKind leftType = (TypeKind) expr.left.visit(this, arg);
		Operator bOp = (Operator) expr.operator.visit(this, null);
		TypeKind rightType = (TypeKind) expr.right.visit(this, arg);
		if (bOp.name.equals("||") || bOp.name.equals("&&")) {
			if (!leftType.equals(TypeKind.BOOLEAN) || !rightType.equals(TypeKind.BOOLEAN)) {
				catchError("*** line " + expr.posn.line + ": (Type Checking) Boolean Expression Expected");
			}
			expr.type = TypeKind.BOOLEAN;
		} else if (bOp.name.equals(">") || bOp.name.equals("<") || bOp.name.equals(">=") || bOp.name.equals("<=")) {
			if (!leftType.equals(TypeKind.INT) || !rightType.equals(TypeKind.INT)) {
				catchError("*** line " + expr.posn.line + ": (Type Checking) Integer Expression Expected");
			}
			expr.type = TypeKind.BOOLEAN;
		} else if (!bOp.name.equals("==") && !bOp.name.equals("!=")) {
			// +, -, *, /
			if (!leftType.equals(TypeKind.INT) || !rightType.equals(TypeKind.INT)) {
				catchError("*** line " + expr.posn.line + ": (Type Checking) Integer Expression Expected");
			}
			expr.type = TypeKind.INT;
		} else {
			// ==, !=
			if (!leftType.equals(rightType)) {
				if ((!rightType.equals(TypeKind.NULL) && !leftType.equals(TypeKind.NULL)) || leftType.equals(TypeKind.INT) || leftType.equals(TypeKind.BOOLEAN)) {
					catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched types in binary operation");
				}
				expr.type = TypeKind.BOOLEAN;
			} else if (leftType.equals(TypeKind.CLASS)) {
				ClassType leftClassType;
				ClassType rightClassType;
				if (expr.left instanceof NewObjectExpr) {
					leftClassType = ((NewObjectExpr) expr.left).classtype;
				} else if (expr.left instanceof CallExpr) {
					leftClassType = (ClassType) ((CallExpr) expr.left).functionRef.decl.type;
				} else if (expr.left instanceof IxExpr) {
					leftClassType = (ClassType) ((ArrayType) ((IxExpr) expr.left).ref.decl.type).eltType;
				} else {
					leftClassType = (ClassType) ((RefExpr) expr.left).ref.decl.type;
				}
				if (expr.right instanceof NewObjectExpr) {
					rightClassType = ((NewObjectExpr) expr.right).classtype;
				} else if (expr.right instanceof CallExpr) {
					rightClassType = (ClassType) ((CallExpr) expr.right).functionRef.decl.type;
				} else if (expr.right instanceof IxExpr) {
					rightClassType = (ClassType) ((ArrayType) ((IxExpr) expr.right).ref.decl.type).eltType;
				} else {
					rightClassType = (ClassType) ((RefExpr) expr.right).ref.decl.type;
				}
				if (!leftClassType.className.name.equals(rightClassType.className.name)) {
					catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched class types in binary operation");
				}
				expr.type = TypeKind.BOOLEAN;
			} else if (leftType.equals(TypeKind.ARRAY)) {
				TypeDenoter left = ((ArrayType) (((RefExpr) expr.left).ref.decl.type)).eltType;
				TypeDenoter right = ((ArrayType) ((RefExpr) expr.right).ref.decl.type).eltType;
				if (left.typeKind.equals(TypeKind.CLASS)) {
					if (!(((ClassType) left).className.name.equals(((ClassType) right).className.name))) {
						catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched array class types in binary operation");
					}
				} else if (left.typeKind.equals(TypeKind.ARRAY)) {
					TypeKind aT = ((ArrayType) left).eltType.typeKind;
					TypeKind rT = ((ArrayType) right).eltType.typeKind;
					if (!aT.equals(rT)) {
						catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched array types in binary operation");
					} else if (aT.equals(TypeKind.CLASS)) {
						if (!(((ClassType) ((ArrayType) left).eltType).className.name.equals(((ClassType) ((ArrayType) right).eltType).className.name))) {
							catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched array types in binary operation");
						}
					}
				}
				expr.type = TypeKind.BOOLEAN;
			} else {
				expr.type = TypeKind.BOOLEAN;
			}
		}
		return expr.type;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		TypeKind r = (TypeKind) expr.ref.visit(this, arg);
		expr.type = r;
		return expr.type;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		TypeKind r = (TypeKind) expr.ref.visit(this, arg);
		TypeKind i = (TypeKind) expr.ixExpr.visit(this, arg);
		if (!r.equals(TypeKind.ARRAY)) {
			throw new ContextualAnalysisException("*** line " + expr.posn.line + ": (Type Checking) Cannot index a non-array");
			// expr.type = ((TypeDenoter) arg).typeKind;
		} else if (!i.equals(TypeKind.INT)) {
			throw new ContextualAnalysisException("*** line " + expr.posn.line + ": (Type Checking) Index must be an integer");
			// expr.type = ((TypeDenoter) arg).typeKind;
		} else {
			expr.type = ((ArrayType) expr.ref.decl.type).eltType.typeKind;
		}
		return expr.type;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		expr.functionRef.visit(this, arg);
		if (expr.argList.size() > ((MethodDecl) expr.functionRef.decl).parameterDeclList.size()) {
			catchError("*** line " + expr.posn.line + ": (Type Checking) Too many arguments in method call");
		} else if (expr.argList.size() < ((MethodDecl) expr.functionRef.decl).parameterDeclList.size()) {
			catchError("*** line " + expr.posn.line + ": (Type Checking) Missing arguments in method call");
		} else {
			for (int i = 0; i < expr.argList.size(); i++) {
				TypeDenoter expected = ((MethodDecl) expr.functionRef.decl).parameterDeclList.get(i).type;
				TypeKind t = (TypeKind) expr.argList.get(i).visit(this, arg);
				if (!t.equals(expected.typeKind)) {
					catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched argument types");
				} else {
					if (expected.typeKind.equals(TypeKind.CLASS)) {
						if (expr.argList.get(i) instanceof NewObjectExpr) {
							if (!(((ClassType) expected).className.name.equals(((NewObjectExpr) expr.argList.get(i)).classtype.className.name))) {
								catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched class types in method call arguments");
							}
						} else if (expr.argList.get(i) instanceof CallExpr) {
							if (!((ClassType) expected).className.name.equals(((ClassType) ((CallExpr) expr.argList.get(i)).functionRef.decl.type).className.name)) {
								catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched class types in method call arguments");
							}
						} else if (expr.argList.get(i) instanceof IxExpr) {
							if (!((ClassType) expected).className.name.equals(((ClassType) ((ArrayType) ((IxExpr) expr.argList.get(i)).ref.decl.type).eltType).className.name)) {
								catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched class types in method call arguments");
							}
						} else {
							if (!(((ClassType) expected).className.name.equals(((ClassType) ((RefExpr) expr.argList.get(i)).ref.decl.type).className.name))) {
								catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched class types in method call arguments");
							}
						}
					} else if (expected.typeKind.equals(TypeKind.ARRAY)) {
						TypeKind aT = ((ArrayType) expected).eltType.typeKind;
						TypeDenoter rT;
						if (expr.argList.get(i) instanceof NewArrayExpr) {
							rT = ((NewArrayExpr) expr.argList.get(i)).eltType;
						} else if (expr.argList.get(i) instanceof CallExpr) {
							rT = ((ArrayType) ((CallExpr) expr.argList.get(i)).functionRef.decl.type).eltType;
						} else if (expr.argList.get(i) instanceof IxExpr) {
							rT = ((ArrayType) ((IxExpr) expr.argList.get(i)).ref.decl.type).eltType;
						} else {
							rT = ((ArrayType) ((RefExpr) expr.argList.get(i)).ref.decl.type).eltType;
						}
						if (!aT.equals(rT.typeKind)) {
							catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched array types in method call arguments");
						} else if (aT.equals(TypeKind.CLASS)) {
							if (!(((ClassType) ((ArrayType) expected).eltType).className.name.equals(((ClassType) rT).className.name))) {
								catchError("*** line " + expr.posn.line + ": (Type Checking) Mismatched array types in method call arguments");
							}
						}
					}
				}
			}
		}
		return expr.functionRef.decl.type.typeKind;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		TypeKind e = (TypeKind) expr.lit.visit(this, arg);
		return e;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		TypeKind e = (TypeKind) expr.classtype.visit(this, arg);
		expr.type = e;
		return expr.type;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.eltType.visit(this, arg);
		TypeKind s = (TypeKind) expr.sizeExpr.visit(this, arg);
		if (!s.equals(TypeKind.INT)) {
			catchError("*** line " + expr.posn.line + ": (Type Checking) Invalid array size");
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
		if (arg instanceof VarDecl) {
			if (((VarDecl) arg).name.equals(ref.id.name)) {
				catchError("*** line " + ((VarDecl) arg).posn.line + ": (Type Checking) Cannot reference variable in its own definition");
			}
		}
		return ref.decl.type.typeKind;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		if (ref.id.name.equals("length") && ref.ref.decl.type.typeKind.equals(TypeKind.ARRAY)) {
			return TypeKind.INT;
		}
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
