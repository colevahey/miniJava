package miniJava;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.*;

import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

/*public class Compiler {
	public static void main(String[] args) {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(args[0]);
		} catch (FileNotFoundException e) {
			System.out.println("Input file " + args[0] + " not found");
			System.exit(1);
		}
		
		Scanner scanner = new Scanner(inputStream);
		Parser parser = new Parser(scanner);
		System.out.println("Syntactic Analysis ... ");
		try {
			System.out.println("Valid miniJava Program");
			ASTDisplay a = new ASTDisplay();
			a.showTree(parser.parse());
			System.exit(0);
		} catch (SyntaxError e) {
			System.out.println("Invalid miniJava Program");
			e.status();
			// e.printStackTrace();
			System.exit(4);
		}
	}
}*/

public class Compiler {
	public static void main(String[] args) {
		File folder = new File("/users/colev/testing/pa1_tests");
		/*try {
			System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("/users/colev/testing/text.txt"))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}*/
		File[] listOfFiles = folder.listFiles();
		int numFiles = 0;
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				if (listOfFiles[i].getName().indexOf("pass") == 0) {
					System.out.println("(" + (++numFiles) + ") + Testing: " + listOfFiles[i].getName());
					InputStream inputStream = null;
					try {
						inputStream = new FileInputStream(listOfFiles[i].getAbsolutePath());
					} catch (FileNotFoundException e) {
						System.out.println("Input file " + listOfFiles[i] + " not found");
						System.exit(1);
					}
					Scanner scanner = new Scanner(inputStream);
					Parser parser = new Parser(scanner);
					new ASTDisplay().showTree(parser.parse());
					System.out.println("\tSuccess");
				}
			}
		}
	}
}