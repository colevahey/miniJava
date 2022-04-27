package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ForStmt extends Statement {
    public ForStmt(Statement initialization, Expression e, AssignStmt a, BlockStmt b, SourcePosition posn){
        super(posn);
        cond = e;
        init = initialization;
        assignment = a;
        body = b;
    }
        
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitForStmt(this, o);
    }
    
    public Statement init;
    public Expression cond;
    public AssignStmt assignment;
    public BlockStmt body;
}