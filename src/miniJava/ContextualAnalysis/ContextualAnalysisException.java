package miniJava.ContextualAnalysis;

public class ContextualAnalysisException extends RuntimeException {
	private String errorMessage;
	public ContextualAnalysisException(String message) {
		this.errorMessage = message;
	}
	
	public void status() {
		if (errorMessage != null) {
			System.out.println(errorMessage);
		}
	}
}
