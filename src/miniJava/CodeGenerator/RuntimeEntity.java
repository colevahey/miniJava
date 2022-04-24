package miniJava.CodeGenerator;

import miniJava.mJAM.Machine;

public class RuntimeEntity {
	public int displacement;
	public int size;
	public Machine.Reg pointer;
	
	public RuntimeEntity(Machine.Reg reg, int displacement) {
		this.displacement = displacement;
		this.size = 0;
		this.pointer = reg;
	}
}
