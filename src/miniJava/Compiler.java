package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.ContextualAnalysis.ContextualAnalysisException;
import miniJava.ContextualAnalysis.Identification;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.mJAM.Disassembler;
import miniJava.mJAM.Interpreter;
import miniJava.mJAM.ObjectFile;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGenerator.Generator;

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
		System.out.println("Analyzing ... ");
		try {
			Package parsed = parser.parse();
			// ASTDisplay a = new ASTDisplay();
			// a.showTree(parsed);
			
			new Identification(parsed);
			System.out.println("Valid miniJava Program");
			
			new Generator(parsed);
			// String objectCodeFileName = args[0].replace(".java", ".mJAM");
			String objectCodeFileName = "out.mJAM";
			ObjectFile objF = new ObjectFile(objectCodeFileName);
			System.out.print("Writing object code file " + objectCodeFileName + " ... ");
			if (objF.write()) {
				System.out.println("FAILED!");
				System.exit(4);
			} else {
				System.out.println("SUCCEEDED");
			}
			String asmCodeFileName = objectCodeFileName.replace(".mJAM",".asm");
	        System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
	        Disassembler d = new Disassembler(objectCodeFileName);
	        if (d.disassemble()) {
	                System.out.println("FAILED!");
	                System.exit(4);
	        } else {
	                System.out.println("SUCCEEDED");
	        }
	        
	        System.out.println("Running Code");
	        Interpreter.interpret(objectCodeFileName);
	        
	        // System.out.println("Running code in debugger ... ");
	        // Interpreter.debug(objectCodeFileName, asmCodeFileName);
	        
			System.exit(0);
		} catch (SyntaxError e) {
			e.status();
			// e.printStackTrace();
			System.exit(4);
		} catch (ContextualAnalysisException e) {
			e.status();
			// e.printStackTrace();
			System.exit(4);
		}
	}
}