package miniJava.SyntacticAnalyzer;

public class SourcePosition {
	public final int line;
	public final int offset;
	public SourcePosition(int line, int offset) {
		this.line = line;
		this.offset = offset;
	}
}
