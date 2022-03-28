package miniJava.SyntacticAnalyzer;

public class Token {
	public TokenType tokenType;
	public String name;
	public SourcePosition posn;
	public Token (TokenType tokenType, String name, SourcePosition posn) {
		this.tokenType = tokenType;
		this.name = name;
		if (tokenType == TokenType.IDENTIFIER) {
			for (int i = 0; i < spellings.length; i++) {
				if (name.equals(spellings[i])) {
					this.tokenType = TokenType.values()[i];
				}
			}
		}
		this.posn = posn;
	}
	
	private final static String[] spellings = {
		"<identifier>", "<integer-literal>", "<unop>", "<binop>",
		"int", "boolean", "class", "void", "null", "public", "private",
		"static", "this", "return", "if", "else", "while", "new",
		"true", "false", "-", "=", ";", ",", ".", "(", ")", "[", "]", "{", "}",
		"<eot>"
	};
}
