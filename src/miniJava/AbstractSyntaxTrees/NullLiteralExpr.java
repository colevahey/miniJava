package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class NullLiteralExpr extends Expression {
	public NullLiteralExpr(SourcePosition posn) {
		super(posn);
	}

	public <A,R> R visit(Visitor<A,R> v, A o) {
	      return v.visitNullLiteralExpr(this, o);
	  }
}