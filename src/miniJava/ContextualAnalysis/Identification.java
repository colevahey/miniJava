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
	
	public Identification(Package ast) {
		table = new IDTable();
		this.ast = ast;
		this.currentStatic = false;
		this.baseLevel = false;
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
			table.enter(cd.name, cd);
		}
		for (ClassDecl cd: prog.classDeclList) {
			currentClass = cd;
			cd.visit(this, null);
		}
		table.closeScope();
		return null;
	}
	
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		table.openScope();
		for (FieldDecl fd: cd.fieldDeclList) {
			table.enter(fd.name, fd);
		}
		for (MethodDecl md: cd.methodDeclList) {
			table.enter(md.name, md);
		}
		
		for (FieldDecl fd: cd.fieldDeclList) {
			currentStatic = fd.isStatic;
			fd.visit(this, null);
		}
		for (MethodDecl md: cd.methodDeclList) {
			currentStatic = md.isStatic;
			md.visit(this, null);
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
		table.enter(pd.name, pd);
		return null;
	}

	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.type.visit(this, null);
		table.enter(decl.name, decl);
		return null;
	}

	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	public Object visitClassType(ClassType type, Object arg) {
		if (!table.getClasses().containsKey(type.className.name)) {
			throw new ContextualAnalysisException("*** line " + type.posn.line + ": Class name expected");
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
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		return null;
	}

	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		stmt.val.visit(this, null);
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
			throw new ContextualAnalysisException("*** line " + stmt.posn.line + ": Cannot utilize a method call on a variable");
		}
		for (Expression e: stmt.argList) {
			e.visit(this, null);
		}
		return null;
	}

	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		stmt.returnExpr.visit(this, null);
		return null;
	}

	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		table.openScope();
		stmt.thenStmt.visit(this, null);
		table.closeScope();
		table.openScope();
		stmt.elseStmt.visit(this, null);
		table.closeScope();
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
		if (!(expr.functionRef.decl instanceof MethodDecl)) {
			throw new ContextualAnalysisException("*** line " + expr.posn.line + ": Cannot utilize a method call on a variable");
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
			throw new ContextualAnalysisException("*** line" + ref.posn.line + ": Cannot access instance in static context");
		}
		return null;
	}

	// Not quite working correctly... needs work
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
						throw new ContextualAnalysisException("*** line " + ref.posn.line + ": Cannot reference instance members in static context");
					} else {
						ref.decl = decl;
						ref.id.decl = decl;
					}
				} else {
					ref.decl = decl;
					ref.id.decl = decl;
				}
			} else if (decl instanceof ClassDecl) {
				throw new ContextualAnalysisException("*** line " + ref.posn.line + ": IdRef cannot be a class declaration");
			} else {
				ref.decl = decl;
				ref.id.decl = decl;
			}
		}
		return null;
	}
	
	public Object visitQRef(QualRef ref, Object arg) {
		ref.ref.visit(this, ref.id);
		FieldDeclList outerFdl;
		if ((ref.ref instanceof IdRef || ref.ref instanceof ThisRef) && ref.ref.decl instanceof ClassDecl) {
			outerFdl = ((ClassDecl) ref.ref.decl).fieldDeclList;
		} else {
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
			MethodDeclList mdl = ((ClassDecl) table.getClasses().get(((ClassType)(ref.ref.decl).type).className.name)).methodDeclList;
			for (MethodDecl md : mdl) {
				if (ref.id.name.equals(md.name)) {
					ref.decl = md;
					found = true;
				}
			}
		}
		if (!found) {
			throw new ContextualAnalysisException("*** line " + ref.posn.line + ": Reference does not exist in scope");
		}
		baseLevel = arg == null ? true : false;
		ref.id.visit(this, ref);
		return null;
	}

	public Object visitIdentifier(Identifier id, Object arg) {
		ClassDecl cd;
		boolean accessPrivate = false;
		boolean isStatic = false;
		boolean found = false;
		if (((QualRef) arg).ref instanceof ThisRef) {
			cd = currentClass;
			accessPrivate = true;
		} else if (((QualRef) arg).ref instanceof IdRef && ((QualRef) arg).ref.decl instanceof ClassDecl) {
			cd = (ClassDecl) ((QualRef) arg).ref.decl;
			isStatic = true;
		} else {
			cd = (ClassDecl) table.getClasses().get(((ClassType)((QualRef) arg).ref.decl.type).className.name);
		}
		FieldDeclList fdl = cd.fieldDeclList;
		if (baseLevel) {
			MethodDeclList mdl = cd.methodDeclList;
			if (!isStatic && !accessPrivate) {
				// Non static method that is public in another class
				for (MethodDecl md : mdl) {
					if (!md.isStatic && !md.isPrivate) { 
						if (id.name.equals(md.name)) {
							found = true;
						}
					}
				} 
			} else if (!accessPrivate) {
				// Static method of a different class
				for (MethodDecl md : mdl) {
					if (isStatic && !md.isPrivate) {
						if (id.name.equals(md.name)) {
							found = true;
						}
					}
				}
			} else {
				// Method in this class static or non-static
				for (MethodDecl md : mdl) {
					if (id.name.equals(md.name)) {
						found = true;
					}
				}
			}
		}
		if (!isStatic && !accessPrivate) {
			// Non static field that is public
			for (FieldDecl fd : fdl) {
				if (!fd.isPrivate && !fd.isStatic) {
					if (id.name.equals(fd.name)) { 
						found = true;
					}
				}
			}
		} else if (!accessPrivate) {
			// Static field of a different class
			for (FieldDecl fd : fdl) {
				if (fd.isStatic && !fd.isPrivate) {
					if (id.name.equals(fd.name)) { 
						found = true;
					}
				}
			}
		} else {
			// Field in this class (private and static/non-static access)
			for (FieldDecl fd : fdl) {
				if (id.name.equals(fd.name)) {
					found = true;
				}
			}
		}
		if (!found) {
			throw new ContextualAnalysisException("*** line " + id.posn.line + ": Method or field not found");
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