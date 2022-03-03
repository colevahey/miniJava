package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

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
		System.out.println("Syntactic analysis ... ");

		try {
			AST ast = parser.parse();
			System.out.print("Syntactic analysis complete:  ");
			System.out.println("Valid miniJava Program:");
			new ASTDisplay().showTree(ast);
			System.exit(0);
		} catch (SyntaxError e) {
			System.out.print("Syntactic analysis complete:  ");
			System.out.println("Invalid miniJava Program");
			// e.status();
			// e.printStackTrace();
			System.exit(4);
		}
	}
}
