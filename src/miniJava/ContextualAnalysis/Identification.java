package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class Identification implements Visitor<Object, Object> {
	public IDTable table;
	private Package ast;
	private ClassDecl currentClass;
	private boolean currentStatic;
	private boolean baseLevel;
	private boolean mainDeclared;
	
	public Identification(Package ast) {
		table = new IDTable();
		this.ast = ast;
		this.currentStatic = false;
		this.baseLevel = false;
		this.mainDeclared = false;
		this.ast.visit(this, null);
		new TypeChecking(this.ast);
	}
	
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
			if (table.getCurrentLevel().containsKey(cd.name)) {
				throw new ContextualAnalysisException("*** line " + cd.posn.line + ": (Identification) Cannot redeclare class");
			}
			table.enter(cd.name, cd);
		}
		for (ClassDecl cd: prog.classDeclList) {
			currentClass = cd;
			cd.visit(this, null);
		}
		if (!mainDeclared) {
			throw new ContextualAnalysisException("*** line " + prog.posn.line + ": (Identification) Main method never declared");
		}	
		table.closeScope();
		return null;
	}
	
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		table.openScope();
		for (FieldDecl fd: cd.fieldDeclList) {
			if (table.getCurrentLevel().containsKey(fd.name)) {
				throw new ContextualAnalysisException("*** line " + fd.posn.line + ": (Identification) Cannot redeclare field in scope");
			}
			table.enter(fd.name, fd);
		}
		for (MethodDecl md: cd.methodDeclList) {
			if (table.getCurrentLevel().containsKey(md.name)) {
				throw new ContextualAnalysisException("*** line " + md.posn.line + ": (Identification) Cannot redeclare method in scope");
			}
			table.enter(md.name, md);
		}
		
		for (FieldDecl fd: cd.fieldDeclList) {
			currentStatic = fd.isStatic;
			fd.visit(this, null);
		}
		for (MethodDecl md: cd.methodDeclList) {
			currentStatic = md.isStatic;
			md.visit(this, null);
			if (md.name.equals("main")) {
				if (!md.isPrivate && md.isStatic && md.type.typeKind.equals(TypeKind.VOID) && md.parameterDeclList.size() == 1) {
					if (md.parameterDeclList.get(0).type instanceof ArrayType) {
						if (((ArrayType) md.parameterDeclList.get(0).type).eltType instanceof ClassType) {
							if ((((ClassType) ((ArrayType) md.parameterDeclList.get(0).type).eltType)).className.name.equals("String")) {
								if (!mainDeclared) {
									mainDeclared = true;
									md.isMain = true;
								} else {
									throw new ContextualAnalysisException("*** line " + md.posn.line + ": (Identification) Main method has already been declared");
								}
							}
						}
					}
				}
			}
		}
		table.closeScope();
		
		return null;
	}
	
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		fd.type.visit(this, null);
		return null;
	}
	
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

	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		pd.type.visit(this, null);
		if (table.getCurrentLevel().containsKey(pd.name)) {
			throw new ContextualAnalysisException("*** line " + pd.posn.line + ": (Identification) Cannot redeclare parameter in scope");
		}
		table.enter(pd.name, pd);
		return null;
	}

	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.type.visit(this, null);
		if (table.getCurrentLevel().containsKey(decl.name) || table.getLocals().containsKey(decl.name)) {
			throw new ContextualAnalysisException("*** line " + decl.posn.line + ": (Identification) Cannot redeclare variable in scope");
		} else {
			table.enter(decl.name, decl);
		}
		return null;
	}

	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	public Object visitClassType(ClassType type, Object arg) {
		if (!table.getClasses().containsKey(type.className.name)) {
			throw new ContextualAnalysisException("*** line " + type.posn.line + ": (Identification) Invalid class type");
		}
		type.className.decl = table.getClasses().get(type.className.name);
		return null;
	}

	public Object visitArrayType(ArrayType type, Object arg) {
		type.eltType.visit(this, null);
		return null;
	}
	
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		table.openScope();
		for (Statement s: stmt.sl) {
			s.visit(this, null);
		}
		table.closeScope();
		return null;
	}

	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.initExp.visit(this, null);
		stmt.varDecl.visit(this, null);
		if (stmt.initExp instanceof RefExpr) {
			if (((RefExpr) stmt.initExp).ref.decl instanceof MethodDecl) {
				throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": (Identification) Cannot assign a method declaration to a field");
			}
		}
		return null;
	}

	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		stmt.val.visit(this, null);
		if (stmt.val instanceof RefExpr) {
			if (((RefExpr) stmt.val).ref.decl instanceof MethodDecl) {
				throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": (Identification) Cannot assign a method declaration to a field");
			}
		}
		return null;
	}

	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		stmt.ix.visit(this, null);
		stmt.exp.visit(this, null);
		return null;
	}

	public Object visitCallStmt(CallStmt stmt, Object arg) {
		stmt.methodRef.visit(this, null);
		if (!(stmt.methodRef.decl instanceof MethodDecl)) {
			throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": (Identification) Cannot utilize a method call on a variable");
		}
		for (Expression e: stmt.argList) {
			e.visit(this, null);
		}
		return null;
	}

	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, null);
		}
		return null;
	}

	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		table.openScope();
		stmt.thenStmt.visit(this, null);
		table.closeScope();
		if (stmt.elseStmt != null) {
			table.openScope();
			stmt.elseStmt.visit(this, null);
			table.closeScope();
		}
		return null;
	}

	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		table.openScope();
		stmt.body.visit(this, null);
		table.closeScope();
		return null;
	}
	
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.operator.visit(this, null);
		expr.expr.visit(this, null);
		return null;
	}

	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		expr.left.visit(this, null);
		expr.operator.visit(this, null);
		expr.right.visit(this, null);
		return null;
	}

	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, null);
		return null;
	}

	public Object visitIxExpr(IxExpr expr, Object arg) {
		expr.ref.visit(this, null);
		expr.ixExpr.visit(this, null);
		return null;
	}
	
	public Object visitCallExpr(CallExpr expr, Object arg) {
		expr.functionRef.visit(this, null);
		if (!(expr.functionRef.decl instanceof MethodDecl)) {
			throw new ContextualAnalysisException("*** line " + expr.posn.line + ": (Identification) Cannot utilize a method call on a variable");
		}
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

	public Object visitThisRef(ThisRef ref, Object arg) {
		if (!currentStatic) {
			ref.decl = currentClass;
		} else {
			throw new ContextualAnalysisException("*** line" + ref.posn.line + ": (Identification) Cannot access instance in static context");
		}
		return null;
	}

	public Object visitIdRef(IdRef ref, Object arg) {
		if (arg != null) {
			if (table.getClasses().containsKey(ref.id.name)) {
				ref.decl = table.getClasses().get(ref.id.name);
				ref.id.decl = table.getClasses().get(ref.id.name);
			} else {
				ref.decl = table.retrieve(ref.id.name);
				ref.id.decl = table.retrieve(ref.id.name);
			}
		} else {
			Declaration decl = table.retrieve(ref.id.name);
			if (decl instanceof MemberDecl) {
				if (currentStatic) {
					if (!((MemberDecl) decl).isStatic) {
						throw new ContextualAnalysisException("*** line " + ref.posn.line + ": (Identification) Cannot reference instance members in static context");
					} else {
						ref.decl = decl;
						ref.id.decl = decl;
					}
				} else {
					ref.decl = decl;
					ref.id.decl = decl;
				}
			} else if (decl instanceof ClassDecl) {
				throw new ContextualAnalysisException("*** line " + ref.posn.line + ": (Identification) IdRef cannot be a class declaration");
			} else if (decl == null) {
				throw new ContextualAnalysisException("*** line " + ref.posn.line + ": (Identification) Reference cannot be found in scope");
			} else {
				ref.decl = decl;
				ref.id.decl = decl;
			}
		}
		return null;
	}
	
	public Object visitQRef(QualRef ref, Object arg) {
		ref.ref.visit(this, ref.id);
		if (ref.ref.decl == null) {
			throw new ContextualAnalysisException("*** line " + ref.posn.line + ": (Identification) Cannot access reference member " + ref.id.name);
		}
		FieldDeclList outerFdl;
		if ((ref.ref instanceof IdRef || ref.ref instanceof ThisRef) && ref.ref.decl instanceof ClassDecl) {
			outerFdl = ((ClassDecl) ref.ref.decl).fieldDeclList;
		} else {
			if (ref.ref.decl.type instanceof ArrayType) {
				if (!ref.id.name.equals("length")) {
					throw new ContextualAnalysisException("*** line " + ref.posn.line + ": (Identification) Cannot dereference variable " + ref.ref.decl.name);
				}
				return null;
			}
			if (!(ref.ref.decl.type instanceof ClassType)) {
				throw new ContextualAnalysisException("*** line " + ref.posn.line + ": (Identification) Cannot dereference variable " + ref.ref.decl.name);
			}
			outerFdl = ((ClassDecl) table.getClasses().get(((ClassType)(ref.ref.decl).type).className.name)).fieldDeclList;
		}
		
		boolean found = false;
		for (FieldDecl fd : outerFdl) {
			if (ref.id.name.equals(fd.name)) {
				ref.decl = fd;
				found = true;
			}
		}
		if (arg == null && !found) {
			MethodDeclList mdl;
			if (ref.ref.decl instanceof ClassDecl) {
				mdl = ((ClassDecl) ref.ref.decl).methodDeclList;
			} else {
				mdl = ((ClassDecl) table.getClasses().get(((ClassType)(ref.ref.decl).type).className.name)).methodDeclList;
			}
			for (MethodDecl md : mdl) {
				if (ref.id.name.equals(md.name)) {
					ref.decl = md;
					found = true;
				}
			}
		}
		if (!found) {
			throw new ContextualAnalysisException("*** line " + ref.posn.line + ": (Identification) Reference does not exist in scope");
		}
		baseLevel = arg == null ? true : false;
		ref.id.visit(this, ref);
		return null;
	}

	public Object visitIdentifier(Identifier id, Object arg) {
		ClassDecl cd;
		boolean accessPrivate = false;
		boolean isStatic = false;
		if (((QualRef) arg).ref instanceof ThisRef) {
			cd = currentClass;
		} else if (((QualRef) arg).ref instanceof IdRef && ((QualRef) arg).ref.decl instanceof ClassDecl) {
			cd = (ClassDecl) ((QualRef) arg).ref.decl;
			isStatic = true;
		} else {
			cd = (ClassDecl) table.getClasses().get(((ClassType)((QualRef) arg).ref.decl.type).className.name);
		}
		if (cd.equals(currentClass)) {
			accessPrivate = true;
		}
		FieldDeclList fdl = cd.fieldDeclList;
		if (baseLevel) {
			MethodDeclList mdl = cd.methodDeclList;
			if (isStatic && !accessPrivate) {
				// Static method that is public in another class
				for (MethodDecl md : mdl) {
					if (md.isStatic && !md.isPrivate) { 
						if (id.name.equals(md.name)) {
							id.decl = md;
						}
					}
				}
			} else if (!isStatic && !accessPrivate) {
				// Non-Static method of a different class
				for (MethodDecl md : mdl) {
					if (!md.isPrivate) {
						if (id.name.equals(md.name)) {
							id.decl = md;
						}
					}
				}
			} else if (isStatic && accessPrivate) {
				// Static method of this class
				for (MethodDecl md : mdl) {
					if (md.isStatic) {
						if (id.name.equals(md.name)) {
							id.decl = md;
						}
					}
				}
			} else {
				// Non-static method in this class
				for (MethodDecl md : mdl) {
					if (id.name.equals(md.name)) {
						id.decl = md;
					}
				}
			}
		}
		if (isStatic && !accessPrivate) {
			// Static field that is public
			for (FieldDecl fd : fdl) {
				if (fd.isStatic && !fd.isPrivate) {
					if (id.name.equals(fd.name)) { 
						id.decl = fd;
					}
				}
			}
		} else if (!isStatic && !accessPrivate) {
			// Static or Non-Static field of a different class
			for (FieldDecl fd : fdl) {
				if (!fd.isPrivate) {
					if (id.name.equals(fd.name)) { 
						id.decl = fd;
					}
				}
			}
		} else if (isStatic && accessPrivate) {
			// Static field of this class
			for (FieldDecl fd : fdl) {
				if (fd.isStatic) {
					if (id.name.equals(fd.name)) {
						id.decl = fd;
					}
				}
			}
		} else {
			// Non-static field in this class
			for (FieldDecl fd : fdl) {
				if (id.name.equals(fd.name)) {
					id.decl = fd;
				}
			}
		}
		if (id.decl == null) {
			throw new ContextualAnalysisException("*** line " + id.posn.line + ": (Identification) Method or field not found in scope");
		}
		return null;
	}
	
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	public Object visitIntLiteral(IntLiteral num, Object arg) {
		return null;
	}

	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return null;
	}
}
