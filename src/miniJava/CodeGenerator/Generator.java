package miniJava.CodeGenerator;

import java.util.HashMap;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.mJAM.Machine;
import miniJava.mJAM.Machine.Op;
import miniJava.mJAM.Machine.Prim;
import miniJava.mJAM.Machine.Reg;

public class Generator implements Visitor<Object, Object> {
	private Package ast;
	private HashMap<Integer, MethodDecl> methodsToPatch;
	private int staticFieldCount;
	private int instanceFieldCount;
	private int patchAddr_Call_main;
	private boolean addrFlag;
	
	public Generator(Package ast) {
		this.ast = ast;
		this.staticFieldCount = 0;
		this.instanceFieldCount = 0;
		this.addrFlag = false;
		Machine.initCodeGen();
		this.ast.visit(this, null);
	}
	
	public Object visitPackage(Package prog, Object arg) {
		
		methodsToPatch = new HashMap<Integer, MethodDecl>();
		
		for (ClassDecl cd: prog.classDeclList) {
			instanceFieldCount = 0;
			cd.address = new RuntimeEntity(null, 0);
			for (FieldDecl fd : cd.fieldDeclList) {
				if (fd.isStatic) {
					Machine.emit(Op.PUSH, 1);
					fd.address = new RuntimeEntity(Reg.SB, staticFieldCount);
					staticFieldCount++;
				} else {
					fd.address = new RuntimeEntity(Reg.OB, instanceFieldCount);
					instanceFieldCount++;
				}
			}
			cd.address.size = instanceFieldCount;
		}
		
		Machine.emit(Op.LOADL,0);            // array length 0
		Machine.emit(Prim.newarr);           // empty String array argument
		patchAddr_Call_main = Machine.nextInstrAddr();  // record instr addr where main is called                                                
		Machine.emit(Op.CALL,Reg.CB,-1);     // static call main (address to be patched)
		Machine.emit(Op.HALT,0,0,0);         // end execution
		
		for (ClassDecl cd: prog.classDeclList) {
			cd.visit(this, null);
		}
		
		methodsToPatch.forEach((callAddress, md) -> Machine.patch(callAddress,  md.address.displacement));
		
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		for (MethodDecl md: cd.methodDeclList) {
			md.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		md.address = new RuntimeEntity(Reg.CB, Machine.nextInstrAddr());
		md.address.size = 2;
		if (md.isMain) {
			Machine.patch(patchAddr_Call_main, md.address.displacement);
		}
		int param = -1;
		for (ParameterDecl pd: md.parameterDeclList) {
			pd.visit(this, param);
			param--;
		}
		
		for (Statement st: md.statementList) {
			st.visit(this, md);
		}
		
		if (md.type.typeKind.equals(TypeKind.VOID)) {
			visitReturnStmt(new ReturnStmt(null, null), md);
		}
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		pd.address = new RuntimeEntity(Reg.LB, (int) arg);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.address = new RuntimeEntity(Reg.LB, ((MethodDecl) arg).address.size + 1);
		((MethodDecl) arg).address.size++;
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		int blockSize = 0;
		for (Statement st: stmt.sl) {
			if (st instanceof VarDeclStmt) {
				blockSize++;
			}
			st.visit(this, arg);
		}
		Machine.emit(Op.POP, blockSize);
		((MethodDecl) arg).address.size -= blockSize;
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.varDecl.visit(this, arg);
		if (stmt.initExp != null) {
			stmt.initExp.visit(this, arg);
		} else {
			Machine.emit(Op.PUSH, 1); // null, just push 0s
		}
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		if (stmt.ref instanceof IdRef) {
			// IdRef
			stmt.val.visit(this, null);
			if (stmt.ref.decl instanceof LocalDecl) {
				Machine.emit(Op.STORE, Reg.LB, stmt.ref.decl.address.displacement);
			} else {
				if (((FieldDecl)stmt.ref.decl).isStatic) {
					Machine.emit(Op.STORE, Reg.SB, stmt.ref.decl.address.displacement);
				} else {
					Machine.emit(Op.STORE, Reg.OB, stmt.ref.decl.address.displacement);
				}
			}
		} else {
			// QualRef
			if (((FieldDecl) stmt.ref.decl).isStatic) {
				stmt.val.visit(this, null);
				Machine.emit(Op.STORE, Reg.SB, stmt.ref.decl.address.displacement);
			} else {
				addrFlag = true;				// need address from qualref
				stmt.ref.visit(this, null);
				stmt.val.visit(this, null);
				Machine.emit(Prim.fieldupd);
			}
		}
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);	// array ref address
		stmt.ix.visit(this, null);	// array index
		stmt.exp.visit(this, null);	// value to store at a[i]
		Machine.emit(Prim.arrayupd);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		// Visit all args and put them on the stack
		for (int i = stmt.argList.size() - 1; i > -1; i--) {
			stmt.argList.get(i).visit(this, null);
		}
		
		// Check for System.out.println()
		if (stmt.methodRef instanceof QualRef) {
			QualRef mR = (QualRef) stmt.methodRef;
			if (mR.id.decl.name.equals("println") && mR.ref instanceof QualRef) {
				mR = (QualRef) mR.ref;
				if (mR.id.decl.name.equals("out") && mR.ref instanceof IdRef) {
					if (mR.ref.decl instanceof ClassDecl) {
						if (((ClassDecl) mR.ref.decl).name.equals("System")) {
							Machine.emit(Prim.putintnl);
							return null;
						}
					}
				}
			}
		}
		
		if (!((MethodDecl) stmt.methodRef.decl).isStatic) {
			if (stmt.methodRef instanceof QualRef) {
				if (!(((QualRef) stmt.methodRef).ref instanceof ThisRef)) {
					stmt.methodRef.visit(this, null);
				} else {
					Machine.emit(Op.LOADA, Reg.OB, 0);
				}
			} else {
				Machine.emit(Op.LOADA, Reg.OB, 0);
			}
		}

		// Add method to patch hashmap so it can be correctly patched at the end
		methodsToPatch.put(Machine.nextInstrAddr(), (MethodDecl) stmt.methodRef.decl);
		
		// Complete the call
		if (((MethodDecl) stmt.methodRef.decl).isStatic) {
			Machine.emit(Op.CALL, Reg.CB, 0);
		} else {
			Machine.emit(Op.CALLI, Reg.CB, 0);
		}
		
		// Unused returned value on stack
		if (!((MethodDecl) stmt.methodRef.decl).type.typeKind.equals(TypeKind.VOID)) {
			Machine.emit(Op.POP, 1);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (!((MethodDecl) arg).type.typeKind.equals(TypeKind.VOID)) {
			// Method returns a value, put it on the stack
			stmt.returnExpr.visit(this, null);
			Machine.emit(Op.RETURN, 1, 0, ((MethodDecl) arg).parameterDeclList.size());
		} else {
			// Method should just return without putting a value on the stack
			Machine.emit(Op.RETURN, 0, 0, ((MethodDecl) arg).parameterDeclList.size());
		}
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		int conditional = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);       // Jump to else if false, patch later
		
		stmt.thenStmt.visit(this, arg);
		int thenComplete = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, 0);  			// Jump past else if true, patch later
		int afterThen = Machine.nextInstrAddr();
		
		Machine.patch(conditional, afterThen);
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, arg);
		}
		
		int afterElse = Machine.nextInstrAddr();
		Machine.patch(thenComplete, afterElse);
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		int jConditional = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, 0);				// Jump to conditional, patch later
		
		int bodyStart = Machine.nextInstrAddr();
		stmt.body.visit(this, arg);
		
		int conditionalStart = Machine.nextInstrAddr();
		Machine.patch(jConditional, conditionalStart);
		stmt.cond.visit(this, null);
		Machine.emit(Op.JUMPIF, 1, Reg.CB, bodyStart);	// Jump back to body start if conditional
		return null;
	}
	
	// EXTRA CREDIT FOR LOOPS
	public Object visitForStmt(ForStmt stmt, Object arg) {
		// Do init statement
		boolean decl = false;
		
		if (stmt.init != null) {
			if (stmt.init instanceof VarDeclStmt) {
				decl = true;
				stmt.init.visit(this, arg);
			} else {
				stmt.init.visit(this, null);
			}
		}
		
		// Jump to conditional
		int jConditional = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, 0);
		
		// Run body
		int bodyStart = Machine.nextInstrAddr();
		stmt.body.visit(this, arg);
		
		// Visit assignment
		if (stmt.assignment != null) stmt.assignment.visit(this, null);
		
		// Run conditional
		int conditionalStart = Machine.nextInstrAddr();
		Machine.patch(jConditional, conditionalStart);
		if (stmt.cond != null) {
			stmt.cond.visit(this, null);
		} else {
			Machine.emit(Op.LOADL, 1);
		}
		
		// Jump back to body start if conditional
		Machine.emit(Op.JUMPIF, 1, Reg.CB, bodyStart);
		
		if (decl) {
			Machine.emit(Op.POP, 1);
		}
		
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, null);
		Machine.emit(expr.operator.name.equals("-") ? Machine.Prim.neg : Machine.Prim.not);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		Operator bOp = expr.operator;
		expr.left.visit(this, null);
		if (bOp.name.equals("||")) {
			int jRight = Machine.nextInstrAddr();
			Machine.emit(Op.JUMPIF, 1, Reg.CB, 0);      // Skip right visit if true || ..., patch later
			Machine.emit(Op.LOADL, 0);					// Put 0 back on stack since it was consumed
			expr.right.visit(this, null);				// Visit right
			Machine.emit(Prim.or);						// Call or
			int normalRight = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, 0);			// Jump to end
			int afterRight = Machine.nextInstrAddr();
			Machine.patch(jRight, afterRight);
			Machine.emit(Op.LOADL, 1);					// Put true back on the stack since it was consumed for JUMPIF
			int afterDone = Machine.nextInstrAddr();
			Machine.patch(normalRight, afterDone);
			return null;
		} else if (bOp.name.equals("&&")) {
			int jRight = Machine.nextInstrAddr();
			Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);       // Skip right visit if false && ..., patch later
			Machine.emit(Op.LOADL, 1);
			expr.right.visit(this, null);
			Machine.emit(Prim.and);
			int normalRight = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, 0);
			int afterRight = Machine.nextInstrAddr();
			Machine.patch(jRight, afterRight);
			Machine.emit(Op.LOADL, 0);					// Put false back on the stack since it was consumed for JUMPIF
			int afterDone = Machine.nextInstrAddr();
			Machine.patch(normalRight, afterDone);
			return null;
		}
		expr.right.visit(this, null);
		if (bOp.name.equals(">")) {
			Machine.emit(Prim.gt);
		} else if (bOp.name.equals("<")) {
			Machine.emit(Prim.lt);
		} else if (bOp.name.equals(">=")) {
			Machine.emit(Prim.ge);
		} else if (bOp.name.equals("<=")) {
			Machine.emit(Prim.le);
		} else if (bOp.name.equals("+")) {
			Machine.emit(Prim.add);
		} else if (bOp.name.equals("-")) {
			Machine.emit(Prim.sub);
		} else if (bOp.name.equals("*")) {
			Machine.emit(Prim.mult);
		} else if (bOp.name.equals("/")) {
			Machine.emit(Prim.div);
		} else if (bOp.name.equals("==")) {
			Machine.emit(Prim.eq);
		} else {
			Machine.emit(Prim.ne);
		}
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		expr.ref.visit(this, null);
		expr.ixExpr.visit(this, null);
		Machine.emit(Prim.arrayref);
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		for (int i = expr.argList.size() - 1; i > -1; i--) {
			expr.argList.get(i).visit(this, null);
		}
		
		if (!((MethodDecl) expr.functionRef.decl).isStatic) {
			if (expr.functionRef instanceof QualRef) {
				if (!(((QualRef) expr.functionRef).ref instanceof ThisRef)) {
					expr.functionRef.visit(this, null);
				} else {
					Machine.emit(Op.LOADA, Reg.OB, 0);
				}
			} else {
				Machine.emit(Op.LOADA, Reg.OB, 0);
			}
		}

		methodsToPatch.put(Machine.nextInstrAddr(), (MethodDecl) expr.functionRef.decl);
		if (((MethodDecl) expr.functionRef.decl).isStatic) {
			Machine.emit(Op.CALL, Reg.CB, 0);
		} else {
			Machine.emit(Op.CALLI, Reg.CB, 0);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		expr.lit.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		Machine.emit(Op.LOADL, -1);
		Machine.emit(Op.LOADL, expr.classtype.className.decl.address.size);
		Machine.emit(Prim.newobj);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.sizeExpr.visit(this, null);
		Machine.emit(Prim.newarr);
		return null;
	}

	@Override
	public Object visitNullLiteralExpr(NullLiteralExpr expr, Object arg) {
		Machine.emit(Op.LOADL, Machine.nullRep);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		Machine.emit(Op.LOADA, Reg.OB, 0);
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		if (ref.decl instanceof LocalDecl) {
			Machine.emit(Op.LOAD, Reg.LB, ref.decl.address.displacement);
		} else if (ref.decl instanceof FieldDecl) {
			if (((FieldDecl) ref.decl).isStatic) {
				Machine.emit(Op.LOAD, Reg.SB, ref.decl.address.displacement);
			} else {
				Machine.emit(Op.LOAD, Reg.OB, ref.decl.address.displacement);
			}
		}
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		if (addrFlag) {
			addrFlag = false;
			ref.ref.visit(this, null);
			Machine.emit(Op.LOADL, ref.id.decl.address.displacement);
		} else {
			if (ref.ref.decl instanceof LocalDecl || ref.ref.decl instanceof MemberDecl) {
				if (ref.ref.decl.type.typeKind.equals(TypeKind.ARRAY) && ref.id.name.equals("length")) {
					ref.ref.visit(this, null);
					Machine.emit(Prim.arraylen);
				} else {
					if (ref.id.decl instanceof FieldDecl) {
						if (((FieldDecl) ref.id.decl).isStatic) {
							Machine.emit(Op.LOAD, Reg.SB, ref.id.decl.address.displacement);
						} else {
							ref.ref.visit(this, null);
							Machine.emit(Op.LOADL, ref.decl.address.displacement);
							Machine.emit(Prim.fieldref);
						}
					} else {
						// MethodDecl
						ref.ref.visit(this, null);
					}
				}
			} else if (ref.ref.decl instanceof ClassDecl) {
				if (ref.id.decl instanceof FieldDecl) {
					if (((FieldDecl) ref.id.decl).isStatic) {
						Machine.emit(Op.LOAD, Reg.SB, ref.id.decl.address.displacement);
					} else {
						Machine.emit(Op.LOAD, Reg.OB, ref.id.decl.address.displacement);
					}
				}
			}
		}
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		Machine.emit(Machine.Op.LOADL, Integer.parseInt(num.name));
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		Machine.emit(Machine.Op.LOADL, Boolean.parseBoolean(bool.name) ? 1 : 0);
		return null;
	}

}
