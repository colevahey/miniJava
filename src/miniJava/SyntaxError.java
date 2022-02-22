package miniJava;

public class SyntaxError extends RuntimeException {
	private String errorMessage;
	public SyntaxError(String message) {
		this.errorMessage = message;
	}
	
	public void status() {
		System.out.println(errorMessage);
	}
}
