package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.ContextualAnalysis.ContextualAnalysisException;
import miniJava.ContextualAnalysis.Identification;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.AbstractSyntaxTrees.Package;

public class Compiler {
	public static void main(String[] args) {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(args[0]);
		} catch (FileNotFoundException e) {
			System.out.println("Input file " + args[0] + " not found");
			System.exit(3);
		}
		
		Scanner scanner = new Scanner(inputStream);
		Parser parser = new Parser(scanner);
		System.out.println("Syntactic Analysis ... ");
		try {
			Package parsed = parser.parse();
			new Identification(parsed);
			System.out.println("Valid miniJava Program");
			// ASTDisplay a = new ASTDisplay();
			// a.showTree(parsed);
			System.exit(0);
		} catch (SyntaxError e) {
			System.out.println("Syntax Error");
			e.status();
			// e.printStackTrace();
			System.exit(4);
		} catch (ContextualAnalysisException e) {
			System.out.println("Contextual Analysis Error");
			e.status();
			// e.printStackTrace();
			System.exit(4);
		}
	}
}