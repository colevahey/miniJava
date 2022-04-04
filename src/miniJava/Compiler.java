/*package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.ContextualAnalysis.ContextualAnalysisException;
import miniJava.ContextualAnalysis.Identification;
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
		System.out.println("Syntactic Analysis ... ");
		try {
			miniJava.AbstractSyntaxTrees.Package parsed = parser.parse();
			new Identification(parsed);
			System.out.println("Valid miniJava Program");
			ASTDisplay a = new ASTDisplay();
			a.showTree(parsed);
			System.exit(0);
		} catch (SyntaxError e) {
			System.out.println("Invalid miniJava Program");
			e.status();
			e.printStackTrace();
			System.exit(4);
		} catch (ContextualAnalysisException e) {
			System.out.println("Contextual Analysis Error");
			e.status();
			// e.printStackTrace();
			System.exit(4);
		}
	}
}
*/

package miniJava;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalysis.ContextualAnalysisException;
import miniJava.ContextualAnalysis.Identification;
import miniJava.ContextualAnalysis.TypeChecking;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

import java.io.*;

public class Compiler {
	public static <InputReader> void main(String[] args) throws SyntaxError, FileNotFoundException {
		File folder = new File("/Users/colev/testing/pa2_tests");
		File[] listOfFiles = folder.listFiles();
		int numFiles = 0;
		int failCount = 0;
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				if (listOfFiles[i].getName().indexOf("s.") == 0) {
					System.out.println("(" + (++numFiles) + ") + Testing: " + listOfFiles[i].getName());
					Package ast = new Parser(new Scanner(new FileInputStream(listOfFiles[i]))).parse();
					try {
						new Identification(ast);
						new TypeChecking(ast);
						System.out.println("PARSED");
					} catch (ContextualAnalysisException e) {
						e.status();
						System.out.println("PARSED");
					} catch (Exception e) {
						failCount++;
						e.printStackTrace();
						System.out.println("PROBLEMO");
					}
				}
			}
		}
		System.out.println("FAIL COUNT: " + failCount);
	}
}