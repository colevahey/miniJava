package miniJava.ContextualAnalysis;

import java.util.ArrayList;
import java.util.HashMap;

import miniJava.AbstractSyntaxTrees.Declaration;

public class IDTable {
	private ArrayList<HashMap<String, Declaration>> table;
	private int level;
	
	public IDTable() {
		this.table = new ArrayList<HashMap<String, Declaration>>();
		this.level = -1;
	}
	
	public void enter(String s, Declaration decl) {
		table.get(level).put(s, decl);
	}
	
	public Declaration retrieve(String s) {
		int checkLevel = level;
		while (checkLevel >= 0) {
			if (table.get(checkLevel).containsKey(s)) {
				return table.get(checkLevel).get(s);
			} else {
				checkLevel--;
			}
		}
		return null;
	}
	
	public Declaration getClass(String s) {
		return table.get(0).get(s);
	}
	
	public HashMap<String, Declaration> getClasses() {
		return table.get(0);
	}
	
	public HashMap<String, Declaration> getLocals() {
		HashMap<String, Declaration> hm = new HashMap<String, Declaration>();
		for (int l = 2; l < level; l++) {
			hm.putAll(table.get(l));
		}
		return hm;
	}
	
	public HashMap<String, Declaration> getCurrentLevel() {
		return table.get(level);
	}
	
	public void openScope() {
		table.add(new HashMap<String, Declaration>());
		level++;
	}
	
	public void closeScope() {
		table.remove(level);
		level--;
	}
}
